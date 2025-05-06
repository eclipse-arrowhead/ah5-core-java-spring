package eu.arrowhead.authorization.service;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.ZonedDateTime;
import java.util.Base64;
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
import eu.arrowhead.authorization.jpa.entity.TokenHeader;
import eu.arrowhead.authorization.jpa.entity.UsageLimitedToken;
import eu.arrowhead.authorization.jpa.service.EncryptionKeyDbService;
import eu.arrowhead.authorization.jpa.service.SelfContainedTokenDbService;
import eu.arrowhead.authorization.jpa.service.TimeLimitedTokenDbService;
import eu.arrowhead.authorization.jpa.service.TokenHeaderDbService;
import eu.arrowhead.authorization.jpa.service.UsageLimitedTokenDbService;
import eu.arrowhead.authorization.service.dto.NormalizedVerifyRequest;
import eu.arrowhead.authorization.service.model.SelfContainedTokenPayload;
import eu.arrowhead.authorization.service.utils.SecretCryptographer;
import eu.arrowhead.authorization.service.utils.TokenGenerator;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.util.ServiceInstanceIdParts;
import eu.arrowhead.common.service.util.ServiceInstanceIdUtils;
import eu.arrowhead.dto.AuthorizationEncryptionKeyRegistrationRequest;
import eu.arrowhead.dto.AuthorizationTokenGenerationRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationResponseDTO;
import eu.arrowhead.dto.AuthorizationTokenVerifyResponseDTO;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import eu.arrowhead.dto.enums.AuthorizationTokenType;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;
import jakarta.annotation.Resource;

@Service
public class AuthorizationTokenService {

	//=================================================================================================
	// members

	@Autowired
	private AuthorizationSystemInfo sysInfo;
	
	@Autowired
	private AuthorizationPolicyEngine policyEngine;

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

	@Autowired
	private TokenHeaderDbService tokenHeaderDbService;

	@Resource(name = Constants.ARROWHEAD_CONTEXT)
	protected Map<String, Object> arrowheadContext;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Pair<AuthorizationTokenGenerationResponseDTO, Boolean> generate(final String requesterSystem, final AuthorizationTokenGenerationRequestDTO dto, final String origin) {
		logger.debug("generate started...");

		final AuthorizationTokenGenerationRequestDTO normalized = dto; // TODO validate and normalize
		final ServiceInterfacePolicy tokenType = ServiceInterfacePolicy.valueOf(normalized.tokenType());
		final ServiceInstanceIdParts serviceInstanceIdParts = ServiceInstanceIdUtils.breakDownInstanceId(normalized.serviceInstanceId());
		
		// Check permission
		boolean isAuthorized = policyEngine.isAccessGranted(new NormalizedVerifyRequest(
				serviceInstanceIdParts.systemName(),
				requesterSystem, Defaults.DEFAULT_CLOUD,
				AuthorizationTargetType.SERVICE_DEF,
				serviceInstanceIdParts.serviceDefinition(),
				dto.serviceOperation()));
		
		if (!isAuthorized) {
			throw new ForbiddenException("Requester has no permisson to the service instance and/or operation.", origin);
		}

		// Generate token
		Pair<AuthorizationTokenGenerationResponseDTO, Boolean> tokenResult = generateToken(requesterSystem, tokenType, serviceInstanceIdParts,
				Utilities.isEmpty(normalized.serviceOperation()) ? Defaults.DEFAULT_AUTHORIZATION_SCOPE : normalized.serviceOperation(), origin);

		// Encrypt token if required
		final AuthorizationTokenType authorizationTokenType = AuthorizationTokenType.fromServiceInterfacePolicy(ServiceInterfacePolicy.valueOf(normalized.tokenType()));
		if (authorizationTokenType == AuthorizationTokenType.SELF_CONTAINED_TOKEN) {
			final Optional<EncryptionKey> encryptionKeyRecordOpt = encryptionKeyDbService.get(serviceInstanceIdParts.systemName());
			try {
				if (encryptionKeyRecordOpt.isPresent()) {
					final EncryptionKey encryptionKeyRecord = encryptionKeyRecordOpt.get();
					final String plainEncriptionKey = secretCryptographer.decryptAESCBCPKCS5P(encryptionKeyRecord.getKey(), encryptionKeyRecord.getInternalAuxiliary().getAuxiliary(), sysInfo.getSecretCryptographerKey());
					final String plainToken = tokenResult.getLeft().token();

					if (encryptionKeyRecord.getAlgorithm().equalsIgnoreCase(SecretCryptographer.HMAC_ALGORITHM)) {
						final String hmacEncryptedToken = secretCryptographer.encryptHMACSHA256(plainToken, plainEncriptionKey);
						tokenResult = Pair.of(new AuthorizationTokenGenerationResponseDTO(tokenResult.getLeft().tokenType(), hmacEncryptedToken, null, tokenResult.getLeft().expiresAt()), tokenResult.getRight());

					} else if (encryptionKeyRecord.getAlgorithm().equalsIgnoreCase(SecretCryptographer.AES_ALOGRITHM)) {
						final String aesEncryptedToken = secretCryptographer.encryptAESCBCPKCS5P(plainToken, plainEncriptionKey, encryptionKeyRecord.getExternalAuxiliary().getAuxiliary()).getLeft();
						tokenResult = Pair.of(new AuthorizationTokenGenerationResponseDTO(tokenResult.getLeft().tokenType(), aesEncryptedToken, null, tokenResult.getLeft().expiresAt()), tokenResult.getRight());

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

	//-------------------------------------------------------------------------------------------------
	public AuthorizationTokenVerifyResponseDTO verify(final String requesterSystem, final String token, final String origin) {
		logger.debug("verify started...");

		final String normalizedRequester = requesterSystem; // TODO validate and normalize

		String tokenAsSaved = null;
		try {
			// Only the not self contained token can be verified this way and those are encrypted with HMAC (to not to have auxiliary, otherwise we could not find it)
			tokenAsSaved = secretCryptographer.encryptHMACSHA256(token, sysInfo.getSecretCryptographerKey());
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Token verification failed.", origin);
		}

		final Optional<TokenHeader> optional = tokenHeaderDbService.find(normalizedRequester, tokenAsSaved);
		if (optional.isEmpty()) {
			return new AuthorizationTokenVerifyResponseDTO(false, null, null, null, null);
		}
		final TokenHeader tokenHeader = optional.get();

		if (tokenHeader.getTokenType() == AuthorizationTokenType.SELF_CONTAINED_TOKEN) {
			// Cannot happen, but who knows...
			throw new InvalidParameterException("Self contained tokens can't be verified this way.", origin);
		}

		// USAGE LIMITED TOKEN
		if (tokenHeader.getTokenType() == AuthorizationTokenType.USAGE_LIMITED_TOKEN) {
			final UsageLimitedToken usageLimitedToken = usageLimitedTokenDbService.getByHeader(tokenHeader).get();
			final boolean verified = usageLimitedToken.getUsageLeft() > 0;
			if (verified) {
				usageLimitedTokenDbService.decrease(usageLimitedToken.getId());
			}
			return new AuthorizationTokenVerifyResponseDTO(verified, tokenHeader.getConsumerCloud(), tokenHeader.getConsumer(), tokenHeader.getServiceDefinition(), tokenHeader.getServiceOperation());
		}

		// TIME LIMITED TOKEN
		if (tokenHeader.getTokenType() == AuthorizationTokenType.TIME_LIMITED_TOKEN) {
			final TimeLimitedToken timeLimitedToken = timeLimitedTokenDbService.getByHeader(tokenHeader).get();
			final boolean verified = timeLimitedToken.getExpiresAt().isAfter(Utilities.utcNow());
			return new AuthorizationTokenVerifyResponseDTO(verified, tokenHeader.getConsumerCloud(), tokenHeader.getConsumer(), tokenHeader.getServiceDefinition(), tokenHeader.getServiceOperation());
		}

		throw new InternalServerError("Unhandled token type: " + tokenHeader.getTokenType(), origin);
	}

	//-------------------------------------------------------------------------------------------------
	public String publicKey(final String origin) {
		logger.debug("registerEncryptionKey started...");
		
		final Optional<Object> pubKeyOpt = Optional.ofNullable(arrowheadContext.get(Constants.SERVER_PUBLIC_KEY));
		if (pubKeyOpt.isEmpty()) {
			throw new DataNotFoundException("Public key is not available", origin);
		}
		
		final PublicKey pubKey = (PublicKey) pubKeyOpt.get();
		return Base64.getEncoder().encodeToString(pubKey.getEncoded());
	}

	//-------------------------------------------------------------------------------------------------
	public Pair<String, Boolean> registerEncryptionKey(final String requesterSystem, final AuthorizationEncryptionKeyRegistrationRequest dto, final String origin) {
		logger.debug("registerEncryptionKey started...");

		final AuthorizationEncryptionKeyRegistrationRequest normalized = dto; // TODO validate and normalize

		String externalAuxiliary = "";
		if (normalized.algorithm().equalsIgnoreCase(SecretCryptographer.AES_ALOGRITHM)) {
			externalAuxiliary = secretCryptographer.generateInitializationVectorBase64();
		}

		Pair<String, String> encryptedToSave = null;
		try {
			encryptedToSave = secretCryptographer.encryptAESCBCPKCS5P(normalized.key(), sysInfo.getSecretCryptographerKey());
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Secret encryption failed!", origin);
		}

		final Pair<EncryptionKey, Boolean> result = encryptionKeyDbService.save(requesterSystem, encryptedToSave.getLeft(), normalized.algorithm(), encryptedToSave.getRight(), externalAuxiliary);
		return Pair.of(externalAuxiliary, result.getRight());
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private Pair<AuthorizationTokenGenerationResponseDTO, Boolean> generateToken(final String requesterSystem, final ServiceInterfacePolicy tokenType, final ServiceInstanceIdParts serviceInstanceIdParts, final String operation, final String origin) {
		logger.debug("generateToken started...");

		try {
			String token = null;
			String encryptedToSaveHMAC = null;

			// SIMMPLE USAGE LIMITED TOKEN

			if (tokenType == ServiceInterfacePolicy.USAGE_LIMITED_TOKEN_AUTH) {
				token = tokenGenerator.generateSimpleToken(sysInfo.getSimpleTokenByteSize());
				encryptedToSaveHMAC = secretCryptographer.encryptHMACSHA256(token, sysInfo.getSecretCryptographerKey());
				final Pair<UsageLimitedToken, Boolean> usageLimitTokenResult = usageLimitedTokenDbService.save(
						AuthorizationTokenType.fromServiceInterfacePolicy(tokenType),
						encryptedToSaveHMAC,
						requesterSystem,
						Defaults.DEFAULT_CLOUD,
						requesterSystem,
						serviceInstanceIdParts.systemName(),
						serviceInstanceIdParts.serviceDefinition(),
						operation,
						sysInfo.getSimpleTokenUsageLimit());
				return Pair.of(new AuthorizationTokenGenerationResponseDTO(AuthorizationTokenType.fromServiceInterfacePolicy(tokenType), token, sysInfo.getSimpleTokenUsageLimit(), null), usageLimitTokenResult.getRight());
			}

			// SIMPLE TIME LIMITED TOKEN

			final Integer durationSec = sysInfo.getTokenTimeLimit();

			if (tokenType == ServiceInterfacePolicy.TIME_LIMITED_TOKEN_AUTH) {
				token = tokenGenerator.generateSimpleToken(sysInfo.getSimpleTokenByteSize());
				encryptedToSaveHMAC = secretCryptographer.encryptHMACSHA256(token, sysInfo.getSecretCryptographerKey());
				final ZonedDateTime expiresAt = Utilities.utcNow().plusSeconds(durationSec);
				final Pair<TimeLimitedToken, Boolean> timeLimitTokenResult = timeLimitedTokenDbService.save(
						AuthorizationTokenType.fromServiceInterfacePolicy(tokenType),
						encryptedToSaveHMAC,
						requesterSystem,
						Defaults.DEFAULT_CLOUD,
						requesterSystem,
						serviceInstanceIdParts.systemName(),
						serviceInstanceIdParts.serviceDefinition(),
						operation,
						expiresAt);
				return Pair.of(new AuthorizationTokenGenerationResponseDTO(AuthorizationTokenType.fromServiceInterfacePolicy(tokenType), token, null, Utilities.convertZonedDateTimeToUTCString(expiresAt)), timeLimitTokenResult.getRight());
			}

			// SELF CONTAINED TOKENS

			Pair<String, String> encryptedToSaveAES = null;

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

			encryptedToSaveAES = secretCryptographer.encryptAESCBCPKCS5P(token, sysInfo.getSecretCryptographerKey());
			final ZonedDateTime expiresAt = Utilities.utcNow().plusSeconds(durationSec);
			final Pair<SelfContainedToken, Boolean> selfContainedTokenResult = selfContainedTokenDbService.save(
					AuthorizationTokenType.fromServiceInterfacePolicy(tokenType),
					encryptedToSaveAES.getLeft(),
					encryptedToSaveAES.getRight(),
					requesterSystem,
					Defaults.DEFAULT_CLOUD,
					requesterSystem,
					serviceInstanceIdParts.systemName(),
					serviceInstanceIdParts.serviceDefinition(),
					operation,
					tokenType.name(),
					Utilities.utcNow().plusSeconds(durationSec));
			return Pair.of(new AuthorizationTokenGenerationResponseDTO(AuthorizationTokenType.fromServiceInterfacePolicy(tokenType), token, null, Utilities.convertZonedDateTimeToUTCString(expiresAt)), selfContainedTokenResult.getRight());

		} catch (final InternalServerError ex) {
			throw ex;

		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Token generation failed!", origin);
		}
	}
}
