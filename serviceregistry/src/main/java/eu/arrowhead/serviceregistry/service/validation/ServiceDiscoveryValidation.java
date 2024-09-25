package eu.arrowhead.serviceregistry.service.validation;

import java.time.DateTimeException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.ServiceInstanceInterfaceRequestDTO;
import eu.arrowhead.dto.ServiceInstanceRequestDTO;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;
import eu.arrowhead.serviceregistry.ServiceRegistryConstants;
import eu.arrowhead.serviceregistry.service.normalization.ServiceDiscoveryNormalization;
import eu.arrowhead.serviceregistry.service.validation.version.VersionValidator;

@Service
public class ServiceDiscoveryValidation {

	//=================================================================================================
	// members

	@Autowired
	private ServiceDiscoveryNormalization normalizer;

	@Autowired
	private VersionValidator versionValidator;

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

		// TODO validate service definition naming convention

		if (!Utilities.isEmpty(dto.expiresAt())) {
			try {
				Utilities.parseUTCStringToZonedDateTime(dto.expiresAt());
			} catch (final DateTimeException ex) {
				throw new InvalidParameterException("Expiration time has an invalide time format", origin);
			}
		}

		if (Utilities.isEmpty(dto.interfaces())) {
			throw new InvalidParameterException("Service interface list is empty", origin);
		}

		for (final ServiceInstanceInterfaceRequestDTO interfaceDTO : dto.interfaces()) {
			if (Utilities.isEmpty(interfaceDTO.templateName())) {
				throw new InvalidParameterException("Interface template name is missing", origin);
			}
			if (Utilities.isEmpty(interfaceDTO.protocol())) {
				throw new InvalidParameterException("Interface protocol is missing", origin);
			}
			if (Utilities.isEmpty(interfaceDTO.policy())) {
				throw new InvalidParameterException("Interface policy is missing", origin);
			}
			if (!Utilities.isEnumValue(interfaceDTO.policy(), ServiceInterfacePolicy.class)) {
				throw new InvalidParameterException("Invalid inteface policy", origin);
			}
			if (Utilities.isEmpty(interfaceDTO.properties())) {
				throw new InvalidParameterException("Interface properties are missing", origin);
			}
		}
	}

	// VALIDATION AND NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public ServiceInstanceRequestDTO validateAndNormalizeRegisterService(final ServiceInstanceRequestDTO dto, final String origin) {
		logger.debug("validateRegisterService started");

		validateRegisterService(dto, origin);

		final ServiceInstanceRequestDTO normalized = normalizer.normalizeServiceInstanceRequestDTO(dto);

		try {
			// TODO validate normalized interface DTOs
			versionValidator.validateNormalizedVersion(normalized.version());
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}
}
