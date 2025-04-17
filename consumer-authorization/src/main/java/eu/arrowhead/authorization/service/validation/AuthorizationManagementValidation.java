package eu.arrowhead.authorization.service.validation;

import java.util.List;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authorization.jpa.entity.AuthMgmtPolicyHeader;
import eu.arrowhead.authorization.jpa.entity.AuthProviderPolicyHeader;
import eu.arrowhead.authorization.service.dto.NormalizedAuthorizationPolicyRequest;
import eu.arrowhead.authorization.service.dto.NormalizedGrantRequest;
import eu.arrowhead.authorization.service.dto.NormalizedQueryRequest;
import eu.arrowhead.authorization.service.normalization.AuthorizationPolicyRequestNormalizer;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.PageValidator;
import eu.arrowhead.common.service.validation.cloud.CloudIdentifierNormalizer;
import eu.arrowhead.common.service.validation.cloud.CloudIdentifierValidator;
import eu.arrowhead.common.service.validation.name.NameNormalizer;
import eu.arrowhead.common.service.validation.name.NameValidator;
import eu.arrowhead.dto.AuthorizationMgmtGrantListRequestDTO;
import eu.arrowhead.dto.AuthorizationMgmtGrantRequestDTO;
import eu.arrowhead.dto.AuthorizationPolicyRequestDTO;
import eu.arrowhead.dto.AuthorizationQueryRequestDTO;
import eu.arrowhead.dto.enums.AuthorizationLevel;
import eu.arrowhead.dto.enums.AuthorizationTargetType;

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

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	public void validateGrantRequest(final AuthorizationMgmtGrantRequestDTO dto, final String origin) {
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
}