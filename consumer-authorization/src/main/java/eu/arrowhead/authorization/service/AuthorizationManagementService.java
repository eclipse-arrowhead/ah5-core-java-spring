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
import eu.arrowhead.authorization.jpa.entity.AuthMgmtPolicyHeader;
import eu.arrowhead.authorization.jpa.entity.AuthPolicy;
import eu.arrowhead.authorization.jpa.entity.AuthPolicyHeader;
import eu.arrowhead.authorization.jpa.entity.AuthProviderPolicyHeader;
import eu.arrowhead.authorization.jpa.entity.EncryptionKey;
import eu.arrowhead.authorization.jpa.service.AuthorizationPolicyDbService;
import eu.arrowhead.authorization.jpa.service.EncryptionKeyDbService;
import eu.arrowhead.authorization.service.dto.DTOConverter;
import eu.arrowhead.authorization.service.dto.NormalizedGrantRequest;
import eu.arrowhead.authorization.service.dto.NormalizedQueryRequest;
import eu.arrowhead.authorization.service.dto.NormalizedVerifyRequest;
import eu.arrowhead.authorization.service.utils.SecretCryptographer;
import eu.arrowhead.authorization.service.utils.TokenEngine;
import eu.arrowhead.authorization.service.validation.AuthorizationManagementValidation;
import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.PageService;
import eu.arrowhead.dto.AuthorizationMgmtGrantListRequestDTO;
import eu.arrowhead.dto.AuthorizationPolicyListResponseDTO;
import eu.arrowhead.dto.AuthorizationQueryRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationMgmtListRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationMgmtRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenMgmtListResponseDTO;
import eu.arrowhead.dto.AuthorizationTokenResponseDTO;
import eu.arrowhead.dto.AuthorizationVerifyListRequestDTO;
import eu.arrowhead.dto.AuthorizationVerifyListResponseDTO;
import eu.arrowhead.dto.enums.AuthorizationLevel;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import eu.arrowhead.dto.enums.AuthorizationTokenType;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;

@Service
public class AuthorizationManagementService {

	//=================================================================================================
	// members

	@Autowired
	private AuthorizationSystemInfo sysInfo;

	@Autowired
	private AuthorizationManagementValidation validator;

	@Autowired
	private AuthorizationPolicyDbService dbService;

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
	public AuthorizationPolicyListResponseDTO grantPoliciesOperation(final String requester, final AuthorizationMgmtGrantListRequestDTO dto, final String origin) {
		logger.debug("grantPoliciesOperation started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final String normalizedRequester = validator.validateAndNormalizeSystemName(requester, origin);
		final List<NormalizedGrantRequest> normalizedList = validator.validateAndNormalizeGrantListRequest(dto, origin);

		try {
			final List<Pair<AuthMgmtPolicyHeader, List<AuthPolicy>>> result = dbService.createMgmtLevelPoliciesInBulk(normalizedRequester, normalizedList);

			return dtoConverter.convertMgmtLevelPolicyListToResponse(result);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void revokePoliciesOperation(final List<String> instanceIds, final String origin) {
		logger.debug("revokePoliciesOperation started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final List<String> normalizedList = validator.validateAndNormalizeRevokePoliciesInput(instanceIds, origin);

		try {
			dbService.deletePoliciesByInstanceIds(normalizedList);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationPolicyListResponseDTO queryPoliciesOperation(final AuthorizationQueryRequestDTO dto, final String origin) {
		logger.debug("queryPoliciesOperation started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final NormalizedQueryRequest normalized = validator.validateAndNormalizeQueryRequest(dto, origin);
		final PageRequest pageRequest = pageService.getPageRequest(
				dto.pagination(),
				Direction.ASC,
				normalized.level() == AuthorizationLevel.MGMT ? AuthMgmtPolicyHeader.SORTABLE_FIELDS_BY : AuthProviderPolicyHeader.SORTABLE_FIELDS_BY,
				AuthPolicyHeader.DEFAULT_SORT_FIELD,
				origin);

		try {
			if (normalized.level() == AuthorizationLevel.MGMT) {
				final Page<Pair<AuthMgmtPolicyHeader, List<AuthPolicy>>> result = dbService.getMgmtLevelPoliciesByFilters(pageRequest, normalized);

				return dtoConverter.convertMgmtLevelPolicyPageToResponse(result);
			} else {
				final Page<Pair<AuthProviderPolicyHeader, List<AuthPolicy>>> result = dbService.getProviderLevelPoliciesByFilters(
						pageRequest,
						normalized);

				return dtoConverter.convertProviderLevelPolicyPageToResponse(result);
			}
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationVerifyListResponseDTO checkPoliciesOperation(final AuthorizationVerifyListRequestDTO dto, final String origin) {
		logger.debug("checkPoliciesOperation started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final List<NormalizedVerifyRequest> normalized = validator.validateAndNormalizeVerifyListRequest(dto, origin);

		try {
			final List<Pair<NormalizedVerifyRequest, Boolean>> result = policyEngine.checkAccess(normalized);

			return dtoConverter.convertCheckResultListToResponse(result);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationTokenMgmtListResponseDTO generateTokensOperation(final String requester, final AuthorizationTokenGenerationMgmtListRequestDTO dto, final String origin) {
		logger.debug("generateTokensOperation started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final String normalizedRequester = validator.validateAndNormalizeSystemName(requester, origin);
		final AuthorizationTokenGenerationMgmtListRequestDTO normalizedDTO = dto; // TODO
		List<AuthorizationTokenGenerationMgmtRequestDTO> authorizedRequests = normalizedDTO.list();

		if (!sysInfo.hasSystemUnboundedTokenGenerationRight(normalizedRequester)) {
			// Check permission
			authorizedRequests = normalizedDTO.list().stream()
					.filter((request) -> policyEngine.isAccessGranted(
							new NormalizedVerifyRequest(request.provider(), request.consumer(), request.consumerCloud(), AuthorizationTargetType.SERVICE_DEF, request.serviceDefinition(), request.serviceOperation())))
					.toList();

			if (Utilities.isEmpty(authorizedRequests)) {
				return new AuthorizationTokenMgmtListResponseDTO(List.of(), 0);
			}
		}

		// Generate Tokens
		final List<AuthorizationTokenResponseDTO> tokenResults = new ArrayList<AuthorizationTokenResponseDTO>(authorizedRequests.size());
		for (final AuthorizationTokenGenerationMgmtRequestDTO request : authorizedRequests) {
			tokenResults.add(
					tokenEngine.produce(requester, request.consumer(), request.consumerCloud(), ServiceInterfacePolicy.valueOf(request.tokenType()), request.provider(), request.serviceDefinition(),
							Utilities.isEmpty(request.serviceOperation()) ? Defaults.DEFAULT_AUTHORIZATION_SCOPE : request.serviceOperation(), request.usageLimit(), Utilities.parseUTCStringToZonedDateTime(request.expireAt()), origin).getLeft());
		}
		
		// Encrypt token if required
		final List<AuthorizationTokenResponseDTO> finalResults = new ArrayList<AuthorizationTokenResponseDTO>(tokenResults.size());
		for (final AuthorizationTokenResponseDTO tokenResult : tokenResults) {
			if (tokenResult.tokenType() != AuthorizationTokenType.SELF_CONTAINED_TOKEN) {
				finalResults.add(tokenResult);
				
			} else {
				final Optional<EncryptionKey> encryptionKeyRecordOpt = encryptionKeyDbService.get(tokenResult.provider());
				if (encryptionKeyRecordOpt.isEmpty()) {
					finalResults.add(tokenResult);
					
				} else {
					try {
						final EncryptionKey encryptionKeyRecord = encryptionKeyRecordOpt.get();
						final String plainEncriptionKey = secretCryptographer.decryptAESCBCPKCS5P(encryptionKeyRecord.getKeyValue(), encryptionKeyRecord.getInternalAuxiliary().getAuxiliary(), sysInfo.getSecretCryptographerKey());					
						String tokenString = tokenResult.token();
						
						if (encryptionKeyRecord.getAlgorithm().equalsIgnoreCase(SecretCryptographer.HMAC_ALGORITHM)) {
							tokenString = secretCryptographer.encryptHMACSHA256(tokenString, plainEncriptionKey);
							
						} else if (encryptionKeyRecord.getAlgorithm().equalsIgnoreCase(SecretCryptographer.AES_ALOGRITHM)) {
							tokenString = secretCryptographer.encryptAESCBCPKCS5P(tokenString, plainEncriptionKey, encryptionKeyRecord.getExternalAuxiliary().getAuxiliary()).getLeft();
							
						} else {
							throw new IllegalArgumentException("Unhandled token encryption algorithm: " + encryptionKeyRecord.getAlgorithm());
						}					
						
						finalResults.add(new AuthorizationTokenResponseDTO(tokenResult.tokenType(), tokenString, tokenResult.requester(), tokenResult.consumerCloud(), tokenResult.consumer(), tokenResult.provider(),
								tokenResult.serviceDefinition(), tokenResult.serviceOperation(), tokenResult.createdAt(), tokenResult.usageLimit(), tokenResult.usageLeft(), tokenResult.expiresAt()));
						
					} catch (final Exception ex) {
						logger.error(ex.getMessage());
						logger.debug(ex);
						throw new InternalServerError("Token encryption failed!", origin);
					}
				}
			}
		}

		return new AuthorizationTokenMgmtListResponseDTO(finalResults, finalResults.size());
	}
}