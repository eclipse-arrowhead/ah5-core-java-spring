package eu.arrowhead.authorization.service.validation;

import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authorization.service.dto.NormalizedAuthorizationPolicyRequest;
import eu.arrowhead.authorization.service.dto.NormalizedGrantRequest;
import eu.arrowhead.authorization.service.dto.NormalizedLookupRequest;
import eu.arrowhead.authorization.service.dto.NormalizedVerifyRequest;
import eu.arrowhead.authorization.service.normalization.AuthorizationPolicyRequestNormalizer;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.cloud.CloudIdentifierNormalizer;
import eu.arrowhead.common.service.validation.cloud.CloudIdentifierValidator;
import eu.arrowhead.common.service.validation.name.NameNormalizer;
import eu.arrowhead.common.service.validation.name.NameValidator;
import eu.arrowhead.dto.AuthorizationGrantRequestDTO;
import eu.arrowhead.dto.AuthorizationLookupRequestDTO;
import eu.arrowhead.dto.AuthorizationPolicyRequestDTO;
import eu.arrowhead.dto.AuthorizationVerifyRequestDTO;
import eu.arrowhead.dto.DTODefaults;
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
	private CloudIdentifierValidator cloudIdentifierValidator;

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
	public void validateRevokeInput(final String systemName, final String instanceId, final String origin) {
		logger.debug("validateRevokeInput started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateSystemName(systemName, origin);

		if (Utilities.isEmpty(instanceId)) {
			throw new InvalidParameterException("Instance id is missing", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void validateLookupRequest(final AuthorizationLookupRequestDTO dto, final String origin) {
		logger.debug("validateLookupRequest started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		// one of the filters is mandatory
		if (Utilities.isEmpty(dto.instanceIds()) && Utilities.isEmpty(dto.targetNames()) && Utilities.isEmpty(dto.cloudIdentifiers())) {
			throw new InvalidParameterException("One of the following filters must be used: 'instanceIds', 'targetNames', 'cloudIdentifiers'", origin);
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
	public void validateVerifyRequest(final AuthorizationVerifyRequestDTO dto, final String origin) {
		logger.debug("validateVerifyRequest started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		try {

			// provider or consumer must be not null
			if (Utilities.isEmpty(dto.provider()) && Utilities.isEmpty(dto.consumer())) {
				throw new InvalidParameterException("Either provider or consumer must be specified", origin);
			}

			// provider
			if (!Utilities.isEmpty(dto.provider())) {
				nameValidator.validateName(dto.provider());
			}

			// consumer
			if (!Utilities.isEmpty(dto.consumer())) {
				nameValidator.validateName(dto.consumer());
			}

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

	//-------------------------------------------------------------------------------------------------
	public Pair<String, String> validateAndNormalizeRevokeInput(final String systemName, final String instanceId, final String origin) {
		logger.debug("validateAndNormalizeRevokeInput started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateRevokeInput(systemName, instanceId, origin);

		return Pair.of(nameNormalizer.normalize(systemName), instanceId.trim());
	}

	//-------------------------------------------------------------------------------------------------
	public NormalizedLookupRequest validateAndNormalizeLookupRequest(final String normalizedProvider, final AuthorizationLookupRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeLookupRequest started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateLookupRequest(dto, origin);

		final NormalizedLookupRequest result = new NormalizedLookupRequest();
		result.setProvider(normalizedProvider);
		normalizeLookupRequest(dto, result);

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	public NormalizedVerifyRequest validateAndNormalizeVerifyRequest(final String normalizedRequester, final AuthorizationVerifyRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeVerifyRequest started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateVerifyRequest(dto, origin);

		return normalizeVerifyRequest(normalizedRequester, dto, origin);
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

	//-------------------------------------------------------------------------------------------------
	private void normalizeLookupRequest(final AuthorizationLookupRequestDTO dto, final NormalizedLookupRequest result) {
		logger.debug("normalizeLookupRequest started...");

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
	}

	//-------------------------------------------------------------------------------------------------
	private NormalizedVerifyRequest normalizeVerifyRequest(final String normalizedRequester, final AuthorizationVerifyRequestDTO dto, final String origin) {
		logger.debug("normalizeVerifyRequest started...");

		final String normalizedProvider = Utilities.isEmpty(dto.provider()) ? normalizedRequester : nameNormalizer.normalize(dto.provider());
		final String normalizedConsumer = Utilities.isEmpty(dto.consumer()) ? normalizedRequester : nameNormalizer.normalize(dto.consumer());

		if (!normalizedProvider.equals(normalizedRequester) && !normalizedConsumer.equals(normalizedRequester)) {
			throw new ForbiddenException("Only the related provider or consumer can use this operaiton", origin);
		}

		return new NormalizedVerifyRequest(
				normalizedProvider,
				normalizedConsumer,
				Utilities.isEmpty(dto.cloud()) ? DTODefaults.DEFAULT_CLOUD : cloudIdentifierNormalizer.normalize(dto.cloud()),
				AuthorizationTargetType.valueOf(dto.targetType()),
				nameNormalizer.normalize(dto.target()),
				Utilities.isEmpty(dto.scope()) ? null : nameNormalizer.normalize(dto.scope()));
	}
}