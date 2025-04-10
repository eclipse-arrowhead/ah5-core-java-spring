package eu.arrowhead.authorization.service.validation;

import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authorization.service.dto.NormalizedAuthorizationPolicyRequest;
import eu.arrowhead.authorization.service.dto.NormalizedGrantRequest;
import eu.arrowhead.authorization.service.normalization.AuthorizationPolicyRequestNormalizer;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.cloud.CloudIdentifierNormalizer;
import eu.arrowhead.common.service.validation.cloud.CloudIdentifierValidator;
import eu.arrowhead.common.service.validation.name.NameNormalizer;
import eu.arrowhead.common.service.validation.name.NameValidator;
import eu.arrowhead.dto.AuthorizationGrantRequestDTO;
import eu.arrowhead.dto.AuthorizationPolicyRequestDTO;
import eu.arrowhead.dto.enums.AuthorizationLevel;
import eu.arrowhead.dto.enums.AuthorizationTargetType;

@Service
public class AuthorizationValidation {

	//=================================================================================================
	// members

	@Autowired
	private NameValidator nameValidator;

	@Autowired
	private NameNormalizer nameNormalizer;

	@Autowired
	private CloudIdentifierValidator cloudIdentitiferValidator;

	@Autowired
	private CloudIdentifierNormalizer cloudIdentifierNormalizer;

	@Autowired
	private AuthorizationPolicyRequestValidator policyRequestValidator;

	@Autowired
	private AuthorizationPolicyRequestNormalizer policyRequestNormalizer;

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
	public void validateGrantRequest(final AuthorizationGrantRequestDTO dto, final String origin) {
		logger.debug("validateGrantRequest started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		try {
			// cloud
			if (!Utilities.isEmpty(dto.cloud())) {
				if (dto.cloud().length() > Constants.CLOUD_IDENTIFIER_MAX_LENGTH) {
					throw new InvalidParameterException("Cloud identifier is too long: " + dto.cloud(), origin);
				}

				cloudIdentitiferValidator.validateCloudIdentifier(dto.cloud());
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

	// VALIDATION AND NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public String validateAndNormalizeSystemName(final String systemName, final String origin) {
		logger.debug("validateAndNormalizeSystemName started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateSystemName(systemName, origin);
		return nameNormalizer.normalize(systemName);
	}

	//-------------------------------------------------------------------------------------------------
	public NormalizedGrantRequest validateAndNormalizeGrantRequest(final String normalizedProvider, final AuthorizationGrantRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeGrantRequest started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateGrantRequest(dto, origin);

		final NormalizedGrantRequest result = new NormalizedGrantRequest(AuthorizationLevel.PROVIDER);
		result.setProvider(normalizedProvider);
		normalizeGrantRequest(dto, result);

		return result;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void normalizeGrantRequest(final AuthorizationGrantRequestDTO dto, final NormalizedGrantRequest result) {
		logger.debug("normalizeGrantRequest started...");

		result.setCloud(Utilities.isEmpty(dto.cloud()) ? Defaults.DEFAULT_CLOUD : cloudIdentifierNormalizer.normalize(dto.cloud()));
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
	}
}