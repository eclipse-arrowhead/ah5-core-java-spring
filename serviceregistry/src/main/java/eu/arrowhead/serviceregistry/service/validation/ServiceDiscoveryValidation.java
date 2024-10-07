package eu.arrowhead.serviceregistry.service.validation;

import java.time.DateTimeException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.intf.properties.IPropertyValidator;
import eu.arrowhead.common.intf.properties.PropertyValidatorType;
import eu.arrowhead.common.intf.properties.PropertyValidators;
import eu.arrowhead.dto.ServiceInstanceInterfaceRequestDTO;
import eu.arrowhead.dto.ServiceInstanceLookupRequestDTO;
import eu.arrowhead.dto.ServiceInstanceRequestDTO;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;
import eu.arrowhead.serviceregistry.ServiceRegistryConstants;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInterfaceTemplate;
import eu.arrowhead.serviceregistry.jpa.service.ServiceInterfaceTemplateDbService;
import eu.arrowhead.serviceregistry.service.normalization.ServiceDiscoveryNormalization;
import eu.arrowhead.serviceregistry.service.validation.name.NameValidator;
import eu.arrowhead.serviceregistry.service.validation.version.VersionValidator;

@Service
public class ServiceDiscoveryValidation {

	//=================================================================================================
	// members

	@Autowired
	private ServiceDiscoveryNormalization normalizer;

	@Autowired
	private VersionValidator versionValidator;

	@Autowired
	private PropertyValidators interfacePropertyValidator;

	@Autowired
	private NameValidator nameValidator;

	@Autowired
	private ServiceInterfaceTemplateDbService interfaceTemplateDbService;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	// VALIDATION

	//-------------------------------------------------------------------------------------------------
	public void validateRegisterService(final ServiceInstanceRequestDTO dto, final String origin) {
		logger.debug("validateRegisterService started");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		if (Utilities.isEmpty(dto.systemName())) {
			throw new InvalidParameterException("System name is empty", origin);
		}

		if (Utilities.isEmpty(dto.serviceDefinitionName())) {
			throw new InvalidParameterException("Service definition name is empty", origin);
		}

		if (dto.serviceDefinitionName().length() > ServiceRegistryConstants.SERVICE_DEFINITION_NAME_LENGTH) {
			throw new InvalidParameterException("Service definition name is too long", origin);
		}

		nameValidator.validateName(dto.serviceDefinitionName());

		if (!Utilities.isEmpty(dto.expiresAt())) {
			ZonedDateTime expiresAt = null;
			try {
				expiresAt = Utilities.parseUTCStringToZonedDateTime(dto.expiresAt());
			} catch (final DateTimeException ex) {
				throw new InvalidParameterException("Expiration time has an invalid time format", origin);
			}
			if (Utilities.utcNow().isAfter(expiresAt)) {
				throw new InvalidParameterException("Expiration time is in the past", origin);
			}
		}

		if (Utilities.isEmpty(dto.interfaces())) {
			throw new InvalidParameterException("Service interface list is empty", origin);
		}

		for (final ServiceInstanceInterfaceRequestDTO interfaceDTO : dto.interfaces()) {
			if (Utilities.isEmpty(interfaceDTO.templateName())) {
				throw new InvalidParameterException("Interface template name is missing", origin);
			}
			nameValidator.validateName(interfaceDTO.templateName());
			if (Utilities.isEmpty(interfaceDTO.policy())) {
				throw new InvalidParameterException("Interface policy is missing", origin);
			}
			if (!Utilities.isEnumValue(interfaceDTO.policy().toUpperCase(), ServiceInterfacePolicy.class)) {
				throw new InvalidParameterException("Invalid inteface policy", origin);
			}
			if (Utilities.isEmpty(interfaceDTO.properties())) {
				throw new InvalidParameterException("Interface properties are missing", origin);
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void validateLookupService(final ServiceInstanceLookupRequestDTO dto, final String origin) {
		logger.debug("validateLookupService started");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		if (Utilities.isEmpty(dto.instanceIds()) && Utilities.isEmpty(dto.providerNames()) && Utilities.isEmpty(dto.serviceDefinitionNames())) {
			throw new InvalidParameterException("One of the following filters must be used: 'instanceIds', 'providerNames', 'serviceDefinitionNames'", origin);
		}

		if (!Utilities.isEmpty(dto.instanceIds()) && Utilities.containsNullOrEmpty(dto.instanceIds())) {
			throw new InvalidParameterException("Instance id list contains null or empty element", origin);
		}

		if (!Utilities.isEmpty(dto.providerNames()) && Utilities.containsNullOrEmpty(dto.providerNames())) {
			throw new InvalidParameterException("Provider name list contains null or empty element", origin);
		}

		if (!Utilities.isEmpty(dto.serviceDefinitionNames()) && Utilities.containsNullOrEmpty(dto.serviceDefinitionNames())) {
			throw new InvalidParameterException("Service definition name list contains null or empty element", origin);
		}

		if (!Utilities.isEmpty(dto.versions()) && Utilities.containsNullOrEmpty(dto.versions())) {
			throw new InvalidParameterException("Version list contains null or empty element", origin);
		}

		if (!Utilities.isEmpty(dto.alivesAt())) {
			try {
				Utilities.parseUTCStringToZonedDateTime(dto.alivesAt());
			} catch (final DateTimeException ex) {
				throw new InvalidParameterException("Alive time has an invalid time format", origin);
			}
		}

		if (!Utilities.isEmpty(dto.metadataRequirementsList()) && Utilities.containsNull(dto.metadataRequirementsList())) {
			throw new InvalidParameterException("Metadata requirements list contains null element", origin);
		}

		if (!Utilities.isEmpty(dto.interfaceTemplateNames()) && Utilities.containsNullOrEmpty(dto.interfaceTemplateNames())) {
			throw new InvalidParameterException("Interface template list contains null or empty element", origin);
		}

		if (!Utilities.isEmpty(dto.interfacePropertyRequirementsList()) && Utilities.containsNull(dto.interfacePropertyRequirementsList())) {
			throw new InvalidParameterException("Interface property requirements list contains null element", origin);
		}

		if (!Utilities.isEmpty(dto.policies())) {
			for (final String policy : dto.policies()) {
				if (Utilities.isEmpty(policy)) {
					throw new InvalidParameterException("Policy list contains null or empty element", origin);
				}
				if (!Utilities.isEnumValue(policy.toUpperCase(), ServiceInterfacePolicy.class)) {
					throw new InvalidParameterException("Policy list contains invalid element: " + policy, origin);
				}
			}
		}

	}

	//-------------------------------------------------------------------------------------------------
	public void validateRevokeService(final String instanceId, final String origin) {
		logger.debug("validateRevokeService started");

		if (Utilities.isEmpty(instanceId)) {
			throw new InvalidParameterException("Service instance ID is missing", origin);
		}
	}

	// VALIDATION AND NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public ServiceInstanceRequestDTO validateAndNormalizeRegisterService(final ServiceInstanceRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeRegisterService started");

		validateRegisterService(dto, origin);

		final ServiceInstanceRequestDTO normalized = normalizer.normalizeServiceInstanceRequestDTO(dto);

		try {
			versionValidator.validateNormalizedVersion(normalized.version());

			normalized.interfaces().forEach(interfaceInstance -> {
				final Optional<ServiceInterfaceTemplate> templateOpt = interfaceTemplateDbService.getByName(interfaceInstance.templateName());
				if (templateOpt.isPresent()) {
					if (!Utilities.isEmpty(interfaceInstance.protocol()) && !interfaceInstance.protocol().equals(templateOpt.get().getProtocol())) {
						throw new InvalidParameterException(interfaceInstance.protocol() + " protocol is invalid for " + interfaceInstance.templateName());
					}

					interfaceTemplateDbService.getPropertiesByTemplateName(interfaceInstance.templateName())
							.forEach(templateProp -> {
								final Object instanceProp = interfaceInstance.properties().get(templateProp.getPropertyName());

								if (instanceProp == null && templateProp.isMandatory()) {
									throw new InvalidParameterException(templateProp.getPropertyName() + " interface property is missing for " + templateProp.getServiceInterfaceTemplate().getName());
								}

								if (!Utilities.isEmpty(templateProp.getValidator())) {
									final String[] validatorWithArgs = templateProp.getValidator().split(ServiceRegistryConstants.INTERFACE_PROPERTY_VALIDATOR_DELIMITER);
									final IPropertyValidator validator = interfacePropertyValidator.getValidator(PropertyValidatorType.valueOf(validatorWithArgs[0]));
									if (validator != null) {
										final Object normalizedProp = validator.validateNormalize(
												instanceProp,
												validatorWithArgs.length <= 1 ? new String[0] : Arrays.copyOfRange(validatorWithArgs, 1, validatorWithArgs.length));
										interfaceInstance.properties().put(templateProp.getPropertyName(), normalizedProp);
									}
								}
							});
				}
			});

		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceInstanceLookupRequestDTO validateAndNormalizeLookupService(final ServiceInstanceLookupRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeLookupService started");

		validateLookupService(dto, origin);
		return normalizer.normalizeServiceInstanceLookupRequestDTO(dto);

	}

	//-------------------------------------------------------------------------------------------------
	public String validateAndNormalizeRevokeService(final String instanceId, final String origin) {
		logger.debug("validateAndNormalizeRevokeService started");

		validateRevokeService(instanceId, origin);
		return normalizer.normalizeServiceInstanceId(instanceId);
	}
}
