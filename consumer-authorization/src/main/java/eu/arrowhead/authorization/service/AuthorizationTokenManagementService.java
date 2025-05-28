package eu.arrowhead.authorization.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authorization.AuthorizationSystemInfo;
import eu.arrowhead.authorization.jpa.entity.EncryptionKey;
import eu.arrowhead.authorization.jpa.entity.TokenHeader;
import eu.arrowhead.authorization.jpa.service.EncryptionKeyDbService;
import eu.arrowhead.authorization.service.dto.DTOConverter;
import eu.arrowhead.authorization.service.dto.NormalizedVerifyRequest;
import eu.arrowhead.authorization.service.engine.AuthorizationPolicyEngine;
import eu.arrowhead.authorization.service.model.EncryptionKeyModel;
import eu.arrowhead.authorization.service.model.TokenModel;
import eu.arrowhead.authorization.service.utils.SecretCryptographer;
import eu.arrowhead.authorization.service.utils.TokenEngine;
import eu.arrowhead.authorization.service.validation.AuthorizationTokenManagementValidation;
import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.service.PageService;
import eu.arrowhead.dto.AuthorizationMgmtEncryptionKeyListResponseDTO;
import eu.arrowhead.dto.AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO;
import eu.arrowhead.dto.AuthorizationMgmtEncryptionKeyRegistrationRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationMgmtListRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationMgmtRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenMgmtListResponseDTO;
import eu.arrowhead.dto.AuthorizationTokenQueryRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenResponseDTO;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import eu.arrowhead.dto.enums.AuthorizationTokenType;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;

@Service
public class AuthorizationTokenManagementService {

	//=================================================================================================
	// members

	@Autowired
	private AuthorizationSystemInfo sysInfo;

	@Autowired
	private AuthorizationTokenManagementValidation validator;

	@Autowired
	private AuthorizationPolicyEngine policyEngine;

	@Autowired
	private TokenEngine tokenEngine;

	@Autowired
	private EncryptionKeyDbService encryptionKeyDbService;

	@Autowired
	private SecretCryptographer secretCryptographer;

	@Autowired
	private DTOConverter dtoConverter;

	@Autowired
	private PageService pageService;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public AuthorizationTokenMgmtListResponseDTO generateTokensOperation(
			final String requester,
			final AuthorizationTokenGenerationMgmtListRequestDTO dto,
			final boolean unbounded,
			final String origin) {
		logger.debug("generateTokensOperation started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final String normalizedRequester = validator.validateAndNormalizeSystemName(requester, origin);
		final AuthorizationTokenGenerationMgmtListRequestDTO normalizedDTO = validator.validateAndNormalizeGenerateTokenRequests(dto, origin);
		List<AuthorizationTokenGenerationMgmtRequestDTO> authorizedRequests = normalizedDTO.list();

		// TODO: continue

		boolean skipAuth = false;
		if (unbounded) {
			skipAuth = sysInfo.hasSystemUnboundedTokenGenerationRight(normalizedRequester);
			if (!skipAuth) {
				throw new ForbiddenException(normalizedRequester + " has no permission for unbounded token generation request", origin);
			}
		}

		if (!skipAuth) {
			// Check permission
			authorizedRequests = normalizedDTO.list().stream()
					.filter((request) -> policyEngine.isAccessGranted(
							new NormalizedVerifyRequest(request.provider(), request.consumer(), request.consumerCloud(), AuthorizationTargetType.valueOf(request.targetType()), request.target(), request.scope())))
					.toList();

			if (Utilities.isEmpty(authorizedRequests)) {
				return new AuthorizationTokenMgmtListResponseDTO(List.of(), 0);
			}
		}

		// Generate Tokens
		final List<TokenModel> tokenResults = new ArrayList<>(authorizedRequests.size());
		for (final AuthorizationTokenGenerationMgmtRequestDTO request : authorizedRequests) {
			tokenResults.add(
					tokenEngine.produce(requester, request.consumer(), request.consumerCloud(), ServiceInterfacePolicy.valueOf(request.tokenType()), request.provider(), AuthorizationTargetType.valueOf(request.targetType()), request.target(),
							Utilities.isEmpty(request.scope()) ? Defaults.DEFAULT_AUTHORIZATION_SCOPE : request.scope(), request.usageLimit(), Utilities.parseUTCStringToZonedDateTime(request.expiresAt()), origin).getFirst());
		}

		// Encrypt token if required
		final List<AuthorizationTokenResponseDTO> finalResults = new ArrayList<>(tokenResults.size());
		for (final TokenModel tokenResult : tokenResults) {
			if (tokenResult.getTokenType() != AuthorizationTokenType.SELF_CONTAINED_TOKEN) {
				finalResults.add(dtoConverter.convertTokenModelToMgmtResponse(tokenResult));

			} else {
				encryptTokenIfNeeded(tokenResult, origin);
				finalResults.add(dtoConverter.convertTokenModelToMgmtResponse(tokenResult));
			}
		}

		return new AuthorizationTokenMgmtListResponseDTO(finalResults, finalResults.size());
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationTokenMgmtListResponseDTO queryTokensOperation(final AuthorizationTokenQueryRequestDTO dto, final String origin) {
		logger.debug("queryTokensOperation started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final AuthorizationTokenQueryRequestDTO normalized = validator.validateAndNormalizedQueryTokensRequest(dto, origin);

		final PageRequest pageRequest = pageService.getPageRequest(
				normalized.pagination(),
				Direction.ASC,
				TokenHeader.SORTABLE_FIELDS_BY,
				TokenHeader.DEFAULT_SORT_FIELD,
				origin);

		final Page<TokenModel> page = tokenEngine.query(pageRequest, normalized.requester(), Utilities.isEmpty(normalized.tokenType()) ? null : AuthorizationTokenType.valueOf(normalized.tokenType()), normalized.consumerCloud(), normalized.consumer(),
				normalized.provider(), Utilities.isEmpty(normalized.targetType()) ? null : AuthorizationTargetType.valueOf(normalized.targetType()), normalized.target(), origin);

		return new AuthorizationTokenMgmtListResponseDTO(page.getContent().stream().map(t -> dtoConverter.convertTokenModelToMgmtResponse(t)).toList(), page.getTotalElements());
	}

	//-------------------------------------------------------------------------------------------------
	public void revokeTokensOperation(final List<String> tokenReferences, final String origin) {
		logger.debug("revokeTokensOperation started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		tokenEngine.revoke(tokenReferences, origin);
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationMgmtEncryptionKeyListResponseDTO addEncryptionKeysOperation(final AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO dto, final String origin) {
		logger.debug("addEncryptionKeysOperation started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO normalizedDTO = validator.validateAndNormalizeAddEncryptionKeysRequest(dto, origin);

		final List<EncryptionKeyModel> models = new ArrayList<EncryptionKeyModel>(normalizedDTO.list().size());

		for (final AuthorizationMgmtEncryptionKeyRegistrationRequestDTO item : normalizedDTO.list()) {

			String externalKeyAuxiliary = null;
			if (item.algorithm().equalsIgnoreCase(SecretCryptographer.AES_CBC_ALGORITHM_IV_BASED)) {
				externalKeyAuxiliary = secretCryptographer.generateInitializationVectorBase64();
			}

			// Encrypt the key for saving into the DB
			Pair<String, String> encryptedKeyToSave = null;
			try {
				encryptedKeyToSave = secretCryptographer.encryptAESCBCPKCS5P_IV(item.key(), sysInfo.getSecretCryptographerKey());
			} catch (final Exception ex) {
				logger.error(ex.getMessage());
				logger.debug(ex);
				throw new InternalServerError("Secret encryption failed!", origin);
			}

			models.add(new EncryptionKeyModel(item.systemName(), item.key(), encryptedKeyToSave.getFirst(), item.algorithm(), encryptedKeyToSave.getSecond(), externalKeyAuxiliary));
		}

		try {
			final List<EncryptionKey> result = encryptionKeyDbService.save(models);

			// Change the keyValue from encrypted to raw
			for (final EncryptionKey encryptionKey : result) {
				final String rawKeyValue = models.stream().filter((item) -> item.getSystemName().equals(encryptionKey.getSystemName())).findFirst().get().getKeyValue();
				encryptionKey.setEncryptedKey(rawKeyValue);
			}

			return dtoConverter.convertEncryptionKeyListToResponse(result, result.size());

		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void removeEncryptionKeysOperation(final List<String> systemNames, final String origin) {
		logger.debug("removeEncriptionKeysOperation started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final List<String> normalized = systemNames.stream().map((name) -> validator.validateAndNormalizeSystemName(name, origin)).toList();

		try {
			encryptionKeyDbService.delete(normalized);

		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void encryptTokenIfNeeded(final TokenModel tokenResult, final String origin) {
		logger.debug("encryptTokenIfNeeded started...");

		final Optional<EncryptionKey> encryptionKeyRecordOpt = encryptionKeyDbService.get(tokenResult.getProvider());
		if (encryptionKeyRecordOpt.isPresent()) {
			try {
				final EncryptionKey encryptionKeyRecord = encryptionKeyRecordOpt.get();
				final String plainEncryptionKey = secretCryptographer.decryptAESCBCPKCS5P_IV(encryptionKeyRecord.getEncryptedKey(), encryptionKeyRecord.getInternalAuxiliary().getValue(), sysInfo.getSecretCryptographerKey());

				if (encryptionKeyRecord.getAlgorithm().equalsIgnoreCase(SecretCryptographer.AES_ECB_ALGORITHM)) {
					tokenResult.setEnrcyptedToken(secretCryptographer.encryptAESECBPKCS5P(tokenResult.getRawToken(), plainEncryptionKey));

				} else if (encryptionKeyRecord.getAlgorithm().equalsIgnoreCase(SecretCryptographer.AES_CBC_ALGORITHM_IV_BASED)) {
					tokenResult.setEnrcyptedToken(secretCryptographer.encryptAESCBCPKCS5P_IV(tokenResult.getRawToken(), plainEncryptionKey, encryptionKeyRecord.getExternalAuxiliary().getValue()).getFirst());

				} else {
					throw new IllegalArgumentException("Unhandled token encryption algorithm: " + encryptionKeyRecord.getAlgorithm());
				}

			} catch (final InternalServerError ex) {
				throw new InternalServerError(ex.getMessage(), origin);

			} catch (final Exception ex) {
				logger.error(ex.getMessage());
				logger.debug(ex);
				throw new InternalServerError("Token encryption failed!", origin);
			}
		}
	}
}
