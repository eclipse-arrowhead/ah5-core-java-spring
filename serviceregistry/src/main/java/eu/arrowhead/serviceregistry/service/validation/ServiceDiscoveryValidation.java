package eu.arrowhead.serviceregistry.service.validation;

import java.time.DateTimeException;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.MetadataValidation;
import eu.arrowhead.dto.ServiceInstanceInterfaceRequestDTO;
import eu.arrowhead.dto.ServiceInstanceLookupRequestDTO;
import eu.arrowhead.dto.ServiceInstanceRequestDTO;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;
import eu.arrowhead.serviceregistry.ServiceRegistryConstants;
import eu.arrowhead.serviceregistry.service.normalization.ServiceDiscoveryNormalization;
import eu.arrowhead.serviceregistry.service.validation.interf.InterfaceValidator;
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
	private InterfaceValidator interfaceValidator;

	@Autowired
	private NameValidator nameValidator;

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

		if (!Utilities.isEmpty(dto.metadata())) {
			MetadataValidation.validateMetadataKey(dto.metadata());
		}

		if (Utilities.isEmpty(dto.interfaces())) {
			throw new InvalidParameterException("Service interface list is empty", origin);
		}

		for (final ServiceInstanceInterfaceRequestDTO interfaceDTO : dto.interfaces()) {
			if (Utilities.isEmpty(interfaceDTO.templateName())) {
				throw new InvalidParameterException("Interface template name is missing", origin);
			}
			if (Utilities.isEmpty(interfaceDTO.policy())) {
				throw new InvalidParameterException("Interface policy is missing", origin);
			}
			if (!Utilities.isEnumValue(interfaceDTO.policy().toUpperCase(), ServiceInterfacePolicy.class)) {
				throw new InvalidParameterException("Invalid inteface policy", origin);
			}
			if (Utilities.isEmpty(interfaceDTO.properties())) {
				throw new InvalidParameterException("Interface properties are missing", origin);
			} else {
				MetadataValidation.validateMetadataKey(interfaceDTO.properties());
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
	public void validateRevokeService(final String systemName, final String instanceId, final String origin) {
		logger.debug("validateRevokeService started");

		if (Utilities.isEmpty(systemName)) {
			throw new InvalidParameterException("System name is empty", origin);
		}

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
			interfaceValidator.validateNormalizedInterfaceInstances(normalized.interfaces());

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
	public Entry<String, String> validateAndNormalizeRevokeService(final String systemName, final String instanceId, final String origin) {
		logger.debug("validateAndNormalizeRevokeService started");

		validateRevokeService(systemName, instanceId, origin);
		return Map.entry(normalizer.normalizeSystemName(systemName), normalizer.normalizeServiceInstanceId(instanceId));
	}
}
