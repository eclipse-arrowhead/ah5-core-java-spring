package eu.arrowhead.authorization.service;

import java.security.PublicKey;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import eu.arrowhead.authorization.AuthorizationSystemInfo;
import eu.arrowhead.authorization.jpa.entity.EncryptionKey;
import eu.arrowhead.authorization.jpa.service.EncryptionKeyDbService;
import eu.arrowhead.authorization.service.dto.DTOConverter;
import eu.arrowhead.authorization.service.dto.NormalizedVerifyRequest;
import eu.arrowhead.authorization.service.model.EncryptionKeyModel;
import eu.arrowhead.authorization.service.model.TokenModel;
import eu.arrowhead.authorization.service.utils.SecretCryptographer;
import eu.arrowhead.authorization.service.utils.TokenEngine;
import eu.arrowhead.authorization.service.validation.AuthorizationTokenValidation;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.dto.AuthorizationEncryptionKeyRegistrationRequestDTO;
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
	private TokenEngine tokenEngine;

	@Autowired
	private SecretCryptographer secretCryptographer;

	@Autowired
	private EncryptionKeyDbService encryptionKeyDbService;

	@Autowired
	private AuthorizationTokenValidation validator;

	@Autowired
	private DTOConverter dtoConverter;

	@Resource(name = Constants.ARROWHEAD_CONTEXT)
	private Map<String, Object> arrowheadContext;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Pair<AuthorizationTokenGenerationResponseDTO, Boolean> generate(final String requesterSystem, final AuthorizationTokenGenerationRequestDTO dto, final String origin) {
		logger.debug("generate started...");

		final String normalizedRequester = validator.validateAndNormalizeSystemName(requesterSystem, origin);
		final AuthorizationTokenGenerationRequestDTO normalizedDTO = validator.validateAndNormalizeGenerateRequest(dto, origin);
		final ServiceInterfacePolicy tokenType = ServiceInterfacePolicy.valueOf(normalizedDTO.tokenType());

		// Check permission
		final boolean isAuthorized = policyEngine.isAccessGranted(new NormalizedVerifyRequest(
				normalizedDTO.provider(),
				normalizedRequester,
				Defaults.DEFAULT_CLOUD,
				AuthorizationTargetType.valueOf(normalizedDTO.targetType()),
				normalizedDTO.target(),
				dto.scope()));

		if (!isAuthorized) {
			throw new ForbiddenException("Requester has no permisson to the service instance and/or operation.", origin);
		}

		// Generate token
		final Pair<TokenModel, Boolean> tokenResult = tokenEngine.produce(normalizedRequester, normalizedRequester, tokenType, normalizedDTO.provider(), AuthorizationTargetType.valueOf(normalizedDTO.targetType()), normalizedDTO.target(),
				Utilities.isEmpty(normalizedDTO.scope()) ? Defaults.DEFAULT_AUTHORIZATION_SCOPE : normalizedDTO.scope(), origin);

		// Encrypt token if required
		final AuthorizationTokenType authorizationTokenType = AuthorizationTokenType.fromServiceInterfacePolicy(ServiceInterfacePolicy.valueOf(normalizedDTO.tokenType()));
		if (authorizationTokenType == AuthorizationTokenType.SELF_CONTAINED_TOKEN) {
			final Optional<EncryptionKey> encryptionKeyRecordOpt = encryptionKeyDbService.get(normalizedDTO.provider());
			try {
				if (encryptionKeyRecordOpt.isPresent()) {
					final EncryptionKey encryptionKeyRecord = encryptionKeyRecordOpt.get();
					final String plainEncriptionKey = secretCryptographer.decryptAESCBCPKCS5P_IV(encryptionKeyRecord.getKeyValue(), encryptionKeyRecord.getInternalAuxiliary().getAuxiliary(), sysInfo.getSecretCryptographerKey());

					if (encryptionKeyRecord.getAlgorithm().equalsIgnoreCase(SecretCryptographer.AES_ECB_ALOGRITHM)) {
						tokenResult.getFirst().setEnrcyptedToken(secretCryptographer.encryptAESECBPKCS5P(tokenResult.getFirst().getRawToken(), plainEncriptionKey));

					} else if (encryptionKeyRecord.getAlgorithm().equalsIgnoreCase(SecretCryptographer.AES_CBC_ALOGRITHM_IV_BASED)) {
						tokenResult.getFirst().setEnrcyptedToken(secretCryptographer.encryptAESCBCPKCS5P_IV(tokenResult.getFirst().getRawToken(), plainEncriptionKey, encryptionKeyRecord.getExternalAuxiliary().getAuxiliary()).getFirst());

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

		return Pair.of(dtoConverter.convertTokenModelToResponse(tokenResult.getFirst()), tokenResult.getSecond());
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationTokenVerifyResponseDTO verify(final String requesterSystem, final String token, final String origin) {
		logger.debug("verify started...");

		final String normalizedRequester = validator.validateAndNormalizeSystemName(requesterSystem, origin);
		final String normalizedToken = validator.validateAndNormalizeToken(token, origin);

		final Pair<Boolean, Optional<TokenModel>> result = tokenEngine.verify(normalizedRequester, normalizedToken, origin);
		return dtoConverter.convertTokenVerificationResultToResponse(result);
	}

	//-------------------------------------------------------------------------------------------------
	public String getPublicKey(final String origin) {
		logger.debug("registerEncryptionKey started...");

		final Optional<Object> pubKeyOpt = Optional.ofNullable(arrowheadContext.get(Constants.SERVER_PUBLIC_KEY));
		if (pubKeyOpt.isEmpty()) {
			throw new DataNotFoundException("Public key is not available", origin);
		}

		final PublicKey pubKey = (PublicKey) pubKeyOpt.get();
		return Base64.getEncoder().encodeToString(pubKey.getEncoded());
	}

	//-------------------------------------------------------------------------------------------------
	public Pair<String, Boolean> registerEncryptionKey(final String requesterSystem, final AuthorizationEncryptionKeyRegistrationRequestDTO dto, final String origin) {
		logger.debug("registerEncryptionKey started...");

		final String normalizedRequester = validator.validateAndNormalizeSystemName(requesterSystem, origin);
		final AuthorizationEncryptionKeyRegistrationRequestDTO normalizedDTO = validator.normalizeAndValidateRegisterEncryptionKeyRequest(dto, origin);

		String externalAuxiliary = "";
		if (normalizedDTO.algorithm().equalsIgnoreCase(SecretCryptographer.AES_CBC_ALOGRITHM_IV_BASED)) {
			externalAuxiliary = secretCryptographer.generateInitializationVectorBase64();
		}

		Pair<String, String> encryptedToSave = null;
		try {
			encryptedToSave = secretCryptographer.encryptAESCBCPKCS5P_IV(normalizedDTO.key(), sysInfo.getSecretCryptographerKey());
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Secret encryption failed!", origin);
		}

		final Pair<EncryptionKey, Boolean> result = encryptionKeyDbService.save(new EncryptionKeyModel(normalizedRequester, normalizedDTO.key(), encryptedToSave.getFirst(), normalizedDTO.algorithm(), encryptedToSave.getFirst(), externalAuxiliary));
		return Pair.of(externalAuxiliary, result.getSecond());
	}

	//-------------------------------------------------------------------------------------------------
	public boolean unregisterEncryptionKey(final String requesterSystem, final String origin) {
		logger.debug("unregisterEncryptionKey started...");

		final String normalizedRequester = validator.validateAndNormalizeSystemName(requesterSystem, origin);
		return encryptionKeyDbService.delete(normalizedRequester);
	}
}
