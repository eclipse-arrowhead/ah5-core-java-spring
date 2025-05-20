package eu.arrowhead.authorization.service.validation;

import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authorization.AuthorizationConstants;
import eu.arrowhead.authorization.jpa.entity.AuthMgmtPolicyHeader;
import eu.arrowhead.authorization.jpa.entity.AuthProviderPolicyHeader;
import eu.arrowhead.authorization.jpa.entity.TokenHeader;
import eu.arrowhead.authorization.service.dto.NormalizedAuthorizationPolicyRequest;
import eu.arrowhead.authorization.service.dto.NormalizedGrantRequest;
import eu.arrowhead.authorization.service.dto.NormalizedQueryRequest;
import eu.arrowhead.authorization.service.dto.NormalizedVerifyRequest;
import eu.arrowhead.authorization.service.normalization.AuthorizationPolicyRequestNormalizer;
import eu.arrowhead.authorization.service.normalization.AuthorizationTokenNormalizer;
import eu.arrowhead.authorization.service.utils.SecretCryptographer;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.PageValidator;
import eu.arrowhead.common.service.validation.cloud.CloudIdentifierNormalizer;
import eu.arrowhead.common.service.validation.cloud.CloudIdentifierValidator;
import eu.arrowhead.common.service.validation.name.NameNormalizer;
import eu.arrowhead.common.service.validation.name.NameValidator;
import eu.arrowhead.dto.AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO;
import eu.arrowhead.dto.AuthorizationMgmtEncryptionKeyRegistrationRequestDTO;
import eu.arrowhead.dto.AuthorizationMgmtGrantListRequestDTO;
import eu.arrowhead.dto.AuthorizationMgmtGrantRequestDTO;
import eu.arrowhead.dto.AuthorizationPolicyRequestDTO;
import eu.arrowhead.dto.AuthorizationQueryRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationMgmtListRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationMgmtRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenQueryRequestDTO;
import eu.arrowhead.dto.AuthorizationVerifyListRequestDTO;
import eu.arrowhead.dto.AuthorizationVerifyRequestDTO;
import eu.arrowhead.dto.DTODefaults;
import eu.arrowhead.dto.enums.AuthorizationLevel;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import eu.arrowhead.dto.enums.AuthorizationTokenType;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;

@Service
public class AuthorizationManagementValidation {

	//=================================================================================================
	// members

	@Autowired
	private NameValidator nameValidator;

	@Autowired
	private NameNormalizer nameNormalizer;

	@Autowired
	private CloudIdentifierValidator cloudIdentifierValidator;

	@Autowired
	private CloudIdentifierNormalizer cloudIdentifierNormalizer;

	@Autowired
	private AuthorizationPolicyRequestValidator policyRequestValidator;

	@Autowired
	private AuthorizationPolicyRequestNormalizer policyRequestNormalizer;

	@Autowired
	private AuthorizationTokenNormalizer tokenNormalizer;

	@Autowired
	private PageValidator pageValidator;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	// VALIDATION

	//-------------------------------------------------------------------------------------------------
	public void validateSystemName(final String systemName, final String origin) {
		logger.debug("validateSystemName started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		if (Utilities.isEmpty(systemName)) {
			throw new InvalidParameterException("System name is empty", origin);
		}

		if (systemName.length() > Constants.SYSTEM_NAME_MAX_LENGTH) {
			throw new InvalidParameterException("System name is too long: " + systemName, origin);
		}

		try {
			nameValidator.validateName(systemName);
		} catch (final InvalidParameterException ex) {
			if (Utilities.isEmpty(ex.getOrigin())) {
				throw new InvalidParameterException(ex.getMessage(), origin);
			}

			throw ex;
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void validateGrantListRequest(final AuthorizationMgmtGrantListRequestDTO dto, final String origin) {
		logger.debug("validateSystemName started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		if (dto == null || Utilities.isEmpty(dto.list())) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		for (final AuthorizationMgmtGrantRequestDTO grantDTO : dto.list()) {
			validateGrantRequest(grantDTO, origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void validateRevokePoliciesInput(final List<String> instanceIds, final String origin) {
		logger.debug("validateRevokePoliciesInput started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		if (Utilities.isEmpty(instanceIds)) {
			throw new InvalidParameterException("Instance id list is missing", origin);
		}

		if (Utilities.containsNullOrEmpty(instanceIds)) {
			throw new InvalidParameterException("Instance id list contains null or empty element", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void validateQueryRequest(final AuthorizationQueryRequestDTO dto, final String origin) {
		logger.debug("validateQueryRequest started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		// level
		if (Utilities.isEmpty(dto.level())) {
			throw new InvalidParameterException("Level is missing", origin);
		}

		final String levelName = dto.level().trim().toUpperCase();

		if (!Utilities.isEnumValue(levelName, AuthorizationLevel.class)) {
			throw new InvalidParameterException("Level is invalid: " + levelName, origin);
		}

		// page
		try {
			pageValidator.validatePageParameter(
					dto.pagination(),
					levelName.equals(AuthorizationLevel.MGMT.name()) ? AuthMgmtPolicyHeader.SORTABLE_FIELDS_BY : AuthProviderPolicyHeader.SORTABLE_FIELDS_BY,
					origin);
		} catch (final InvalidParameterException ex) {
			if (Utilities.isEmpty(ex.getOrigin())) {
				throw new InvalidParameterException(ex.getMessage(), origin);
			}

			throw ex;
		}

		// providers
		if (!Utilities.isEmpty(dto.providers()) && Utilities.containsNullOrEmpty(dto.providers())) {
			throw new InvalidParameterException("Provider list contains null or empty element", origin);
		}

		// instance ids
		if (!Utilities.isEmpty(dto.instanceIds()) && Utilities.containsNullOrEmpty(dto.instanceIds())) {
			throw new InvalidParameterException("Instance id list contains null or empty element", origin);
		}

		// target names
		if (!Utilities.isEmpty(dto.targetNames()) && Utilities.containsNullOrEmpty(dto.targetNames())) {
			throw new InvalidParameterException("Target names list contains null or empty element", origin);
		}

		// cloud identifiers
		if (!Utilities.isEmpty(dto.cloudIdentifiers()) && Utilities.containsNullOrEmpty(dto.cloudIdentifiers())) {
			throw new InvalidParameterException("Cloud identifiers list contains null or empty element", origin);
		}

		// target type
		if (!Utilities.isEmpty(dto.targetType())) {
			final String targetTypeName = dto.targetType().trim().toUpperCase();

			if (!Utilities.isEnumValue(targetTypeName, AuthorizationTargetType.class)) {
				throw new InvalidParameterException("Target type is invalid: " + targetTypeName, origin);
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void validateVerifyListRequest(final AuthorizationVerifyListRequestDTO dto, final String origin) {
		logger.debug("validateVerifyListRequest started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		if (dto == null || Utilities.isEmpty(dto.list())) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		for (final AuthorizationVerifyRequestDTO request : dto.list()) {
			validateVerifyRequest(request, origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void validateGenerateTokenRequets(final AuthorizationTokenGenerationMgmtListRequestDTO dto, final String origin) {
		logger.debug("validateGenerateTokenRequets started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		if (dto == null || Utilities.isEmpty(dto.list())) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		if (Utilities.containsNull(dto.list())) {
			throw new InvalidParameterException("Request payload list contains null element", origin);
		}

		for (final AuthorizationTokenGenerationMgmtRequestDTO request : dto.list()) {
			if (Utilities.isEmpty(request.tokenType())) {
				throw new InvalidParameterException("Token type is missing", origin);
			}
			if (Utilities.isEmpty(request.consumer())) {
				throw new InvalidParameterException("Consumer system name is missing", origin);
			}
			if (Utilities.isEmpty(request.provider())) {
				throw new InvalidParameterException("Provider system name is missing", origin);
			}
			if (Utilities.isEmpty(request.serviceDefinition())) {
				throw new InvalidParameterException("Service definition is missing", origin);
			}
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	public void validateQueryTokensRequest(final AuthorizationTokenQueryRequestDTO dto, final String origin) {
		logger.debug("validateQueryTokensRequest started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");
		
		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}
		
		pageValidator.validatePageParameter(dto.pagination(), TokenHeader.SORTABLE_FIELDS_BY, origin);
	}
	
	//-------------------------------------------------------------------------------------------------
	public void validateAddEncryptionKeysRequest(final AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO dto, final String origin) {
		logger.debug("validateAddEncryptionKeysRequest started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");
		
		if (dto == null || Utilities.isEmpty(dto.list())) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}
		
		if (Utilities.containsNull(dto.list())) {
			throw new InvalidParameterException("Request payload list contains null element", origin);
		}
		
		for (final AuthorizationMgmtEncryptionKeyRegistrationRequestDTO request : dto.list()) {
			if (Utilities.isEmpty(request.systemName())) {
				throw new InvalidParameterException("System name is missing", origin);
			}
			if (Utilities.isEmpty(request.algorithm())) {
				throw new InvalidParameterException("Algorithm is missing.", origin);
			}			
			if (Utilities.isEmpty(request.key())) {
				throw new InvalidParameterException("Key is missing.", origin);
			}
		}
	}
	
	// VALIDATION AND NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public String validateAndNormalizeSystemName(final String systemName, final String origin) {
		logger.debug("validateAndNormalizeSystemName started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateSystemName(systemName, origin);
		return nameNormalizer.normalize(systemName);
	}

	//-------------------------------------------------------------------------------------------------
	public List<NormalizedGrantRequest> validateAndNormalizeGrantListRequest(final AuthorizationMgmtGrantListRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeGrantListRequest started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateGrantListRequest(dto, origin);

		return normalizedGrantListRequest(dto);
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> validateAndNormalizeRevokePoliciesInput(final List<String> instanceIds, final String origin) {
		logger.debug("validateAndNormalizeRevokePoliciesInput started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateRevokePoliciesInput(instanceIds, origin);

		return instanceIds
				.stream()
				.map(id -> id.trim())
				.toList();
	}

	//-------------------------------------------------------------------------------------------------
	public NormalizedQueryRequest validateAndNormalizeQueryRequest(final AuthorizationQueryRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeQueryRequest started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateQueryRequest(dto, origin);

		return normalizeQueryRequest(dto);
	}

	//-------------------------------------------------------------------------------------------------
	public List<NormalizedVerifyRequest> validateAndNormalizeVerifyListRequest(final AuthorizationVerifyListRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeVerifyListRequest started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateVerifyListRequest(dto, origin);

		return normalizeVerifyListRequest(dto);
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationTokenGenerationMgmtListRequestDTO validateAndNormalizeGenerateTokenRequets(final AuthorizationTokenGenerationMgmtListRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeGenerateTokenRequets started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateGenerateTokenRequets(dto, origin);

		final AuthorizationTokenGenerationMgmtListRequestDTO normalized = tokenNormalizer.normalizeAuthorizationTokenGenerationMgmtListRequestDTO(dto);

		for (final AuthorizationTokenGenerationMgmtRequestDTO request : normalized.list()) {
			if (!Utilities.isEnumValue(request.tokenType(), ServiceInterfacePolicy.class)
					|| !request.tokenType().endsWith(AuthorizationConstants.TOKEN_TYPE_AUTH_SUFFIX)) {
				throw new InvalidParameterException("Invalid token type: " + request.tokenType(), origin);
			}

			try {
				nameValidator.validateName(request.consumerCloud());
				nameValidator.validateName(request.consumer());
				nameValidator.validateName(request.provider());
				nameValidator.validateName(request.serviceDefinition());

				if (!request.serviceOperation().equals(Defaults.DEFAULT_AUTHORIZATION_SCOPE)) {
					nameValidator.validateName(request.serviceOperation());
				}

			} catch (final InvalidParameterException ex) {
				throw new InvalidParameterException(ex.getMessage(), origin);
			}

			if (!Utilities.isEmpty(request.expireAt())) {
				try {
					Utilities.parseUTCStringToZonedDateTime(request.expireAt());
				} catch (final DateTimeParseException ex) {
					throw new InvalidParameterException("Invalid expires at: " + request.expireAt(), origin);
				}
			}
		}

		return normalized;
	}
	
	//-------------------------------------------------------------------------------------------------
	public AuthorizationTokenQueryRequestDTO validateAndNormalizedQueryTokensRequest(final AuthorizationTokenQueryRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizedQueryTokensRequest started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");
		
		validateQueryTokensRequest(dto, origin);
		final AuthorizationTokenQueryRequestDTO normalized = tokenNormalizer.normalizeAuthorizationTokenQueryRequestDTO(dto);
		
		try {
			if (!Utilities.isEmpty(normalized.requester())) {
				nameValidator.validateName(normalized.requester());
			}
			if (!Utilities.isEmpty(normalized.consumerCloud())) {
				nameValidator.validateName(normalized.consumerCloud());
			}
			if (!Utilities.isEmpty(normalized.consumer())) {
				nameValidator.validateName(normalized.consumer());
			}
			if (!Utilities.isEmpty(normalized.provider())) {
				nameValidator.validateName(normalized.provider());
			}
			if (!Utilities.isEmpty(normalized.serviceDefinition())) {
				nameValidator.validateName(normalized.serviceDefinition());
			}
			
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}
		
		if (!Utilities.isEmpty(dto.tokenType())
				&& !Utilities.isEnumValue(dto.tokenType(), AuthorizationTokenType.class)) {
			throw new InvalidParameterException("Invalid token type: " + dto.tokenType(), origin);
		}
		
		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO validateAndNormalizeAddEncryptionKeysRequest(final AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeAddEncryptionKeysRequest started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");
		
		validateAddEncryptionKeysRequest(dto, origin);
		
		final AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO normalized = tokenNormalizer.normalizeAuthorizationMgmtEncryptionKeyRegistrationListRequestDTO(dto);
		
		final Set<String> sysNames = new HashSet<>(normalized.list().size());
		for (final AuthorizationMgmtEncryptionKeyRegistrationRequestDTO request : normalized.list()) {
			try {
				nameValidator.validateName(request.systemName());
			} catch (final InvalidParameterException ex) {
				throw new InvalidParameterException(ex.getMessage(), origin);
			}			
			if (sysNames.contains(request.systemName())) {
				throw new InvalidParameterException("Duplicate system name: " + request.systemName(), origin);
			}
			sysNames.add(request.systemName());
			
			if (!(request.algorithm().equalsIgnoreCase(SecretCryptographer.HMAC_ALGORITHM) || request.algorithm().equalsIgnoreCase(SecretCryptographer.AES_ALOGRITHM))) {
				throw new InvalidParameterException("Unsupported algorithm", origin);
			}
			if (request.algorithm().equalsIgnoreCase(SecretCryptographer.AES_ALOGRITHM)
					&& request.key().getBytes().length != SecretCryptographer.AES_KEY_SIZE) {
				throw new InvalidParameterException("Key size is not " + SecretCryptographer.AES_KEY_SIZE + " byte long for system: " + request.systemName());
			}
		}
		
		return normalized;
	}
	
	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void validateGrantRequest(final AuthorizationMgmtGrantRequestDTO dto, final String origin) {
		logger.debug("validateGrantRequest started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		try {
			// cloud
			if (!Utilities.isEmpty(dto.cloud())) {
				if (dto.cloud().length() > Constants.CLOUD_IDENTIFIER_MAX_LENGTH) {
					throw new InvalidParameterException("Cloud identifier is too long: " + dto.cloud(), origin);
				}

				cloudIdentifierValidator.validateCloudIdentifier(dto.cloud());
			}

			// provider
			validateSystemName(dto.provider(), origin);

			// target type
			if (Utilities.isEmpty(dto.targetType())) {
				throw new InvalidParameterException("Target type is missing", origin);
			}

			final String targetTypeName = dto.targetType().trim().toUpperCase();

			if (!Utilities.isEnumValue(targetTypeName, AuthorizationTargetType.class)) {
				throw new InvalidParameterException("Target type is invalid: " + targetTypeName, origin);
			}

			// target
			if (Utilities.isEmpty(dto.target())) {
				throw new InvalidParameterException("Target is missing", origin);
			}

			final int threshold = AuthorizationTargetType.SERVICE_DEF.name().equals(targetTypeName) ? Constants.SERVICE_DEFINITION_NAME_MAX_LENGTH : Constants.EVENT_TYPE_NAME_MAX_LENGTH;
			if (dto.target().length() > threshold) {
				throw new InvalidParameterException("Target is too long: " + dto.cloud(), origin);
			}

			nameValidator.validateName(dto.target());

			// default policy
			policyRequestValidator.validateAuthorizationPolicy(dto.defaultPolicy(), true, origin);

			// scoped policies
			if (dto.scopedPolicies() != null) {
				dto.scopedPolicies().entrySet().forEach(e -> {
					if (e.getKey().length() > Constants.SCOPE_MAX_LENGTH) {
						throw new InvalidParameterException("Scope is too long: " + e.getKey(), origin);
					}

					nameValidator.validateName(e.getKey());
					policyRequestValidator.validateAuthorizationPolicy(e.getValue(), false, origin);
				});
			}
		} catch (final InvalidParameterException ex) {
			if (Utilities.isEmpty(ex.getOrigin())) {
				throw new InvalidParameterException(ex.getMessage(), origin);
			}

			throw ex;
		}
	}

	//-------------------------------------------------------------------------------------------------
	private List<NormalizedGrantRequest> normalizedGrantListRequest(final AuthorizationMgmtGrantListRequestDTO dto) {
		logger.debug("normalizedGrantListRequest started...");

		return dto
				.list()
				.stream()
				.map(grantDTO -> normalizeGrantRequest(grantDTO))
				.toList();
	}

	//-------------------------------------------------------------------------------------------------
	private NormalizedGrantRequest normalizeGrantRequest(final AuthorizationMgmtGrantRequestDTO dto) {
		logger.debug("normalizeGrantRequest started...");

		final NormalizedGrantRequest result = new NormalizedGrantRequest(AuthorizationLevel.MGMT);
		result.setCloud(Utilities.isEmpty(dto.cloud()) ? Defaults.DEFAULT_CLOUD : cloudIdentifierNormalizer.normalize(dto.cloud()));
		result.setProvider(nameNormalizer.normalize(dto.provider()));
		result.setTargetType(AuthorizationTargetType.valueOf(dto.targetType().trim().toUpperCase()));
		result.setTarget(nameNormalizer.normalize(dto.target()));
		result.setDescription(dto.description());

		if (!Utilities.isEmpty(dto.scopedPolicies())) {
			for (final Entry<String, AuthorizationPolicyRequestDTO> entry : dto.scopedPolicies().entrySet()) {
				final String scope = nameNormalizer.normalize(entry.getKey());
				final NormalizedAuthorizationPolicyRequest normalized = policyRequestNormalizer.normalize(entry.getValue());
				result.addPolicy(scope, normalized);
			}
		}

		result.addPolicy(Defaults.DEFAULT_AUTHORIZATION_SCOPE, policyRequestNormalizer.normalize(dto.defaultPolicy()));

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	private NormalizedQueryRequest normalizeQueryRequest(final AuthorizationQueryRequestDTO dto) {
		logger.debug("normalizeQueryRequest started...");

		final NormalizedQueryRequest result = new NormalizedQueryRequest();
		result.setLevel(AuthorizationLevel.valueOf(dto.level().trim().toUpperCase()));

		result.setProviders(Utilities.isEmpty(dto.providers())
				? null
				: dto.providers()
						.stream()
						.map(p -> nameNormalizer.normalize(p))
						.toList());

		result.setInstanceIds(Utilities.isEmpty(dto.instanceIds())
				? null
				: dto.instanceIds()
						.stream()
						.map(String::trim)
						.toList());

		result.setCloudIdentifiers(Utilities.isEmpty(dto.cloudIdentifiers())
				? null
				: dto.cloudIdentifiers()
						.stream()
						.map(cId -> cloudIdentifierNormalizer.normalize(cId))
						.toList());

		result.setTargetNames(Utilities.isEmpty(dto.targetNames())
				? null
				: dto.targetNames()
						.stream()
						.map(target -> nameNormalizer.normalize(target))
						.toList());

		result.setTargetType(Utilities.isEmpty(dto.targetType()) ? null : AuthorizationTargetType.valueOf(dto.targetType().trim().toUpperCase()));

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	public void validateVerifyRequest(final AuthorizationVerifyRequestDTO dto, final String origin) {
		logger.debug("validateVerifyRequest started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		try {

			// provider
			if (Utilities.isEmpty(dto.provider())) {
				throw new InvalidParameterException("Provider is missing", origin);
			}
			nameValidator.validateName(dto.provider());

			// consumer
			if (Utilities.isEmpty(dto.consumer())) {
				throw new InvalidParameterException("Consumer is missing", origin);
			}
			nameValidator.validateName(dto.consumer());

			// cloud
			if (!Utilities.isEmpty(dto.cloud())) {
				cloudIdentifierValidator.validateCloudIdentifier(dto.cloud());
			}

			// target type
			if (Utilities.isEmpty(dto.targetType())) {
				throw new InvalidParameterException("Target type is missing", origin);
			}

			final String targetTypeName = dto.targetType().trim().toUpperCase();

			if (!Utilities.isEnumValue(targetTypeName, AuthorizationTargetType.class)) {
				throw new InvalidParameterException("Target type is invalid: " + targetTypeName, origin);
			}

			// target
			if (Utilities.isEmpty(dto.target())) {
				throw new InvalidParameterException("Target is missing", origin);
			}

			nameValidator.validateName(dto.target());

			// scope
			if (!Utilities.isEmpty(dto.scope())) {
				nameValidator.validateName(dto.scope());
			}
		} catch (final InvalidParameterException ex) {
			if (Utilities.isEmpty(ex.getOrigin())) {
				throw new InvalidParameterException(ex.getMessage(), origin);
			}

			throw ex;
		}
	}

	//-------------------------------------------------------------------------------------------------
	private List<NormalizedVerifyRequest> normalizeVerifyListRequest(final AuthorizationVerifyListRequestDTO request) {
		logger.debug("normalizeVerifyListRequest started...");

		return request
				.list()
				.stream()
				.map(dto -> normalizeVerifyRequest(dto))
				.toList();
	}

	//-------------------------------------------------------------------------------------------------
	private NormalizedVerifyRequest normalizeVerifyRequest(final AuthorizationVerifyRequestDTO dto) {
		logger.debug("normalizeVerifyRequest started...");

		return new NormalizedVerifyRequest(
				nameNormalizer.normalize(dto.provider()),
				nameNormalizer.normalize(dto.consumer()),
				Utilities.isEmpty(dto.cloud()) ? DTODefaults.DEFAULT_CLOUD : cloudIdentifierNormalizer.normalize(dto.cloud()),
				AuthorizationTargetType.valueOf(dto.targetType().trim().toUpperCase()),
				nameNormalizer.normalize(dto.target()),
				Utilities.isEmpty(dto.scope()) ? null : nameNormalizer.normalize(dto.scope()));
	}
}