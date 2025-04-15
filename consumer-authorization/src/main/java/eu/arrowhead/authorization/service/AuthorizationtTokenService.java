package eu.arrowhead.authorization.service;

import java.security.PrivateKey;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authorization.AuthorizationSystemInfo;
import eu.arrowhead.authorization.jpa.entity.EncryptionKey;
import eu.arrowhead.authorization.jpa.entity.UsageLimitedToken;
import eu.arrowhead.authorization.jpa.service.EncryptionKeyDbService;
import eu.arrowhead.authorization.jpa.service.UsageLimitedTokenDbService;
import eu.arrowhead.authorization.service.model.JWTPayload;
import eu.arrowhead.authorization.service.utils.SecretCryptographer;
import eu.arrowhead.authorization.service.utils.TokenGenerator;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.service.util.ServiceInstanceIdParts;
import eu.arrowhead.common.service.util.ServiceInstanceIdUtils;
import eu.arrowhead.dto.AuthorizationTokenGenerationRequestDTO;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import eu.arrowhead.dto.enums.AuthorizationTokenType;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;

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

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Pair<String, Boolean> generate(final String requesterSystem, final AuthorizationTokenGenerationRequestDTO dto, final String origin) {
		logger.debug("generate started...");

		final AuthorizationTokenGenerationRequestDTO normalized = dto; // TODO validate and normalize

		// Generate token
		Pair<String, Boolean> tokenResult = generateToken(requesterSystem, normalized, origin);

		// Encrypt token if required
		final AuthorizationTokenType authorizationTokenType = AuthorizationTokenType.fromServiceInterfacePolicy(ServiceInterfacePolicy.valueOf(normalized.tokenType()));
		if (authorizationTokenType == AuthorizationTokenType.JSON_WEB_TOKEN) {
			final Optional<EncryptionKey> encryptionKeyRecordOpt = encryptionKeyDbService.get(requesterSystem);
			try {
				if (encryptionKeyRecordOpt.isPresent()) {
					final EncryptionKey encryptionKeyRecord = encryptionKeyRecordOpt.get();
					final String plainEncriptionKey = secretCryptographer.decryptAESCBCPKCS5P(encryptionKeyRecord.getKey(), encryptionKeyRecord.getInternalAuxiliary().getAuxiliary(), sysInfo.getSecretCryptographerKey());
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
	public Pair<String, Boolean> generateToken(final String requesterSystem, final AuthorizationTokenGenerationRequestDTO normalized, final String origin) {
		logger.debug("generateToken started...");

		final ServiceInstanceIdParts serviceInstanceIdParts = ServiceInstanceIdUtils.breakDownInstanceId(normalized.serviceInstanceId());

		try {
			final ServiceInterfacePolicy tokenType = ServiceInterfacePolicy.valueOf(normalized.tokenType());
			String token = null;
			Pair<String, String> encryptedToSave = null;

			if (tokenType == ServiceInterfacePolicy.USAGE_LIMITED_TOKEN_AUTH) {
				token = tokenGenerator.generateSimpleToken(sysInfo.getSimpleTokenByteSize());
				encryptedToSave = secretCryptographer.encryptAESCBCPKCS5P(token, sysInfo.getSecretCryptographerKey());
				final Pair<UsageLimitedToken, Boolean> usageLimitTokenResult = usageLimitedTokenDbService.save(
						AuthorizationTokenType.fromServiceInterfacePolicy(tokenType),
						encryptedToSave.getLeft(),
						encryptedToSave.getRight(),
						requesterSystem,
						"LOCAL",
						requesterSystem,
						serviceInstanceIdParts.systemName(),
						serviceInstanceIdParts.serviceDefinition(),
						normalized.serviceOperation(),
						sysInfo.getSimpleTokenUsageLimit());
				return Pair.of(usageLimitTokenResult.getLeft().getHeader().getToken(), usageLimitTokenResult.getRight());
			}

			final Integer duration = null; // TODO config

			if (tokenType == ServiceInterfacePolicy.TIME_LIMITED_TOKEN_AUTH) {
				token = tokenGenerator.generateSimpleToken(sysInfo.getSimpleTokenByteSize());
				// TODO encode and save into DB
				return null;
			}

			final PrivateKey privateKey = null; // TODO
			final String encriptionKey = null; // TODO from DB and decode it
			final JWTPayload jwtPayload = new JWTPayload(serviceInstanceIdParts.systemName(), requesterSystem, "LOCAL", AuthorizationTargetType.SERVICE_DEF, serviceInstanceIdParts.serviceDefinition(), normalized.serviceOperation());

			if (tokenType == ServiceInterfacePolicy.RSASHA256_AES128GCM_JSON_WEB_TOKEN_AUTH) {
				token = tokenGenerator.generateJsonWebToken(AlgorithmIdentifiers.RSA_USING_SHA512, privateKey, ContentEncryptionAlgorithmIdentifiers.AES_128_GCM, encriptionKey, duration, jwtPayload);
				// TODO encode and save into DB
			}
			if (tokenType == ServiceInterfacePolicy.RSASHA256_AES256GCM_JSON_WEB_TOKEN_AUTH) {
				token = tokenGenerator.generateJsonWebToken(AlgorithmIdentifiers.RSA_USING_SHA512, privateKey, ContentEncryptionAlgorithmIdentifiers.AES_256_GCM, encriptionKey, duration, jwtPayload);
				// TODO encode and save into DB
			}

			Assert.isTrue(!Utilities.isEmpty(token), "Unhandled token type: " + tokenType);

			return null;

		} catch (final InternalServerError ex) {
			throw ex;

		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Token generation failed!", origin);
		}
	}
}
