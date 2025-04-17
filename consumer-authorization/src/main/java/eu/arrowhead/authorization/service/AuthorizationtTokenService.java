package eu.arrowhead.authorization.service;

import java.security.PrivateKey;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authorization.AuthorizationSystemInfo;
import eu.arrowhead.authorization.jpa.entity.EncryptionKey;
import eu.arrowhead.authorization.jpa.entity.SelfContainedToken;
import eu.arrowhead.authorization.jpa.entity.TimeLimitedToken;
import eu.arrowhead.authorization.jpa.entity.UsageLimitedToken;
import eu.arrowhead.authorization.jpa.service.EncryptionKeyDbService;
import eu.arrowhead.authorization.jpa.service.SelfContainedTokenDbService;
import eu.arrowhead.authorization.jpa.service.TimeLimitedTokenDbService;
import eu.arrowhead.authorization.jpa.service.UsageLimitedTokenDbService;
import eu.arrowhead.authorization.service.model.SelfContainedTokenPayload;
import eu.arrowhead.authorization.service.utils.SecretCryptographer;
import eu.arrowhead.authorization.service.utils.TokenGenerator;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.service.util.ServiceInstanceIdParts;
import eu.arrowhead.common.service.util.ServiceInstanceIdUtils;
import eu.arrowhead.dto.AuthorizationTokenGenerationRequestDTO;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import eu.arrowhead.dto.enums.AuthorizationTokenType;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;
import jakarta.annotation.Resource;

@Service
public class AuthorizationtTokenService {

	//=================================================================================================
	// members

	@Autowired
	private AuthorizationSystemInfo sysInfo;

	@Autowired
	private TokenGenerator tokenGenerator;

	@Autowired
	private SecretCryptographer secretCryptographer;

	@Autowired
	private EncryptionKeyDbService encryptionKeyDbService;

	@Autowired
	private UsageLimitedTokenDbService usageLimitedTokenDbService;

	@Autowired
	private TimeLimitedTokenDbService timeLimitedTokenDbService;

	@Autowired
	private SelfContainedTokenDbService selfContainedTokenDbService;
	
	@Resource(name = Constants.ARROWHEAD_CONTEXT)
	protected Map<String, Object> arrowheadContext;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Pair<String, Boolean> generate(final String requesterSystem, final AuthorizationTokenGenerationRequestDTO dto, final String origin) {
		logger.debug("generate started...");

		final AuthorizationTokenGenerationRequestDTO normalized = dto; // TODO validate and normalize
		final ServiceInterfacePolicy tokenType = ServiceInterfacePolicy.valueOf(normalized.tokenType());
		final ServiceInstanceIdParts serviceInstanceIdParts = ServiceInstanceIdUtils.breakDownInstanceId(normalized.serviceInstanceId());

		// Generate token
		Pair<String, Boolean> tokenResult = generateToken(requesterSystem, tokenType, serviceInstanceIdParts, normalized.serviceOperation(), origin);

		// Encrypt token if required
		final AuthorizationTokenType authorizationTokenType = AuthorizationTokenType.fromServiceInterfacePolicy(ServiceInterfacePolicy.valueOf(normalized.tokenType()));
		if (authorizationTokenType == AuthorizationTokenType.SELF_CONTAINED_TOKEN) {
			final Optional<EncryptionKey> encryptionKeyRecordOpt = encryptionKeyDbService.get(serviceInstanceIdParts.systemName());
			try {
				if (encryptionKeyRecordOpt.isPresent()) {
					final EncryptionKey encryptionKeyRecord = encryptionKeyRecordOpt.get();
					final String plainEncriptionKey = secretCryptographer.decryptInternal(encryptionKeyRecord.getKey(), encryptionKeyRecord.getInternalAuxiliary().getAuxiliary(), sysInfo.getSecretCryptographerKey());
					final String plainToken = tokenResult.getLeft();

					if (encryptionKeyRecord.getAlgorithm().equalsIgnoreCase(SecretCryptographer.HMAC_ALGORITHM)) {
						final String hmacEncryptedToken = secretCryptographer.encryptHMACSHA256(plainToken, plainEncriptionKey);
						tokenResult = Pair.of(hmacEncryptedToken, tokenResult.getRight());

					} else if (encryptionKeyRecord.getAlgorithm().equalsIgnoreCase(SecretCryptographer.AES_ALOGRITHM)) {
						final String aesEncryptedToken = secretCryptographer.encryptAESCBCPKCS5P(plainToken, plainEncriptionKey, encryptionKeyRecord.getExternalAuxiliary().getAuxiliary()).getLeft();
						tokenResult = Pair.of(aesEncryptedToken, tokenResult.getRight());

					} else {
						throw new IllegalArgumentException("Unhandled token encryption algorithm: " + encryptionKeyRecord.getAlgorithm());
					}
				}

			} catch (final Exception ex) {
				logger.error(ex.getMessage());
				logger.debug(ex);
				throw new InternalServerError("Token encryption failed!", origin);
			}
		}

		return tokenResult;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	public Pair<String, Boolean> generateToken(final String requesterSystem, ServiceInterfacePolicy tokenType, final ServiceInstanceIdParts serviceInstanceIdParts, final String operation, final String origin) {
		logger.debug("generateToken started...");

		try {
			String token = null;
			Pair<String, String> encryptedToSave = null;

			// SIMMPLE USAGE LIMITED TOKEN

			if (tokenType == ServiceInterfacePolicy.USAGE_LIMITED_TOKEN_AUTH) {
				token = tokenGenerator.generateSimpleToken(sysInfo.getSimpleTokenByteSize());
				encryptedToSave = secretCryptographer.encryptInternal(token, sysInfo.getSecretCryptographerKey());
				final Pair<UsageLimitedToken, Boolean> usageLimitTokenResult = usageLimitedTokenDbService.save(
						AuthorizationTokenType.fromServiceInterfacePolicy(tokenType),
						encryptedToSave.getLeft(),
						encryptedToSave.getRight(),
						requesterSystem,
						Defaults.DEFAULT_CLOUD,
						requesterSystem,
						serviceInstanceIdParts.systemName(),
						serviceInstanceIdParts.serviceDefinition(),
						operation,
						sysInfo.getSimpleTokenUsageLimit());
				return Pair.of(usageLimitTokenResult.getLeft().getHeader().getToken(), usageLimitTokenResult.getRight());
			}

			// SIMPLE TIME LIMITED TOKEN

			final Integer durationSec = sysInfo.getTokenTimeLimit();

			if (tokenType == ServiceInterfacePolicy.TIME_LIMITED_TOKEN_AUTH) {
				token = tokenGenerator.generateSimpleToken(sysInfo.getSimpleTokenByteSize());
				encryptedToSave = secretCryptographer.encryptInternal(token, sysInfo.getSecretCryptographerKey());
				final Pair<TimeLimitedToken, Boolean> timeLimitTokenResult = timeLimitedTokenDbService.save(
						AuthorizationTokenType.fromServiceInterfacePolicy(tokenType),
						encryptedToSave.getLeft(),
						encryptedToSave.getRight(),
						requesterSystem,
						Defaults.DEFAULT_CLOUD,
						requesterSystem,
						serviceInstanceIdParts.systemName(),
						serviceInstanceIdParts.serviceDefinition(),
						operation,
						Utilities.utcNow().plusSeconds(durationSec));
				return Pair.of(timeLimitTokenResult.getLeft().getHeader().getToken(), timeLimitTokenResult.getRight());
			}

			// SELF CONTAINED TOKENS
			
			// -- BASE64 SELF CONTAINED TOKEN

			final SelfContainedTokenPayload tokenPayload = new SelfContainedTokenPayload(serviceInstanceIdParts.systemName(), requesterSystem, Defaults.DEFAULT_CLOUD, AuthorizationTargetType.SERVICE_DEF, serviceInstanceIdParts.serviceDefinition(),
					operation);

			if (tokenType == ServiceInterfacePolicy.BASE64_SELF_CONTAINED_AUTH_TOKEN) {
				token = tokenGenerator.generateBas64SelfSignedToken(durationSec, tokenPayload);

			} else {
				//  -- JSON WEB TOKEN

				final PrivateKey privateKey = (PrivateKey) arrowheadContext.get(Constants.SERVER_PRIVATE_KEY);

				if (tokenType == ServiceInterfacePolicy.RSA_SHA512_JSON_WEB_TOKEN_AUTH) {
					token = tokenGenerator.generateJsonWebToken(AlgorithmIdentifiers.RSA_USING_SHA512, privateKey, durationSec, tokenPayload);
				}
				if (tokenType == ServiceInterfacePolicy.RSA_SHA256_JSON_WEB_TOKEN_AUTH) {
					token = tokenGenerator.generateJsonWebToken(AlgorithmIdentifiers.RSA_USING_SHA256, privateKey, durationSec, tokenPayload);
				}
			}

			Assert.isTrue(!Utilities.isEmpty(token), "Unhandled token type: " + tokenType);

			encryptedToSave = secretCryptographer.encryptInternal(token, sysInfo.getSecretCryptographerKey());
			final Pair<SelfContainedToken, Boolean> selfContainedTokenResult = selfContainedTokenDbService.save(
					AuthorizationTokenType.fromServiceInterfacePolicy(tokenType),
					encryptedToSave.getLeft(),
					encryptedToSave.getRight(),
					requesterSystem,
					Defaults.DEFAULT_CLOUD,
					requesterSystem,
					serviceInstanceIdParts.systemName(),
					serviceInstanceIdParts.serviceDefinition(),
					operation,
					tokenType.name(),
					Utilities.utcNow().plusSeconds(durationSec));
			return Pair.of(selfContainedTokenResult.getLeft().getHeader().getToken(), selfContainedTokenResult.getRight());

		} catch (final InternalServerError ex) {
			throw ex;

		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Token generation failed!", origin);
		}
	}
}
