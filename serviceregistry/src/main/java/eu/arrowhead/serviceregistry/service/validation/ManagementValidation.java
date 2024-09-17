package eu.arrowhead.serviceregistry.service.validation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.jpa.ArrowheadEntity;
import eu.arrowhead.common.service.validation.PageValidator;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.ServiceDefinitionListRequestDTO;
import eu.arrowhead.dto.SystemListRequestDTO;
import eu.arrowhead.dto.SystemQueryRequestDTO;
import eu.arrowhead.dto.SystemRequestDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.jpa.entity.System;
import eu.arrowhead.serviceregistry.service.normalization.ManagementNormalization;
import eu.arrowhead.serviceregistry.service.validation.address.AddressValidator;
import eu.arrowhead.serviceregistry.service.validation.version.VersionValidator;
import eu.arrowhead.dto.DeviceListRequestDTO;
import eu.arrowhead.dto.DeviceQueryRequestDTO;
import eu.arrowhead.dto.DeviceRequestDTO;
import eu.arrowhead.serviceregistry.ServiceRegistryConstants;
import eu.arrowhead.serviceregistry.jpa.entity.Device;

@Service
public class ManagementValidation {

	//=================================================================================================
	// members

	@Autowired
	private AddressValidator addressTypeValidator;

	@Autowired
	private PageValidator pageValidator;

	@Autowired
	private VersionValidator versionValidator;

	@Autowired
	private ManagementNormalization normalizer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	// DEVICE VALIDATION

	//-------------------------------------------------------------------------------------------------
	public void validateCreateDevices(final DeviceListRequestDTO dto, final String origin) {
		logger.debug("validateCreateDevice started");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		if (Utilities.isEmpty(dto.devices())) {
			throw new InvalidParameterException("Request payload is empty", origin);
		}

		final Set<String> names = new HashSet<>();
		for (final DeviceRequestDTO device : dto.devices()) {

			if (device == null) {
				throw new InvalidParameterException("Device list contains null element", origin);
			}

			if (Utilities.isEmpty(device.name())) {
				throw new InvalidParameterException("Device name is empty", origin);
			}

			if (device.name().length() > ServiceRegistryConstants.DEVICE_NAME_LENGTH) {
				throw new InvalidParameterException("Device name is too long", origin);
			}

			if (names.contains(device.name())) {
				throw new InvalidParameterException("Duplicate device name: " + device.name(), origin);
			}
			names.add(device.name());

			if (!Utilities.isEmpty(device.addresses())) {
				for (final AddressDTO address : device.addresses()) {

					if (address == null) {
						throw new InvalidParameterException("Address list contains null element", origin);
					}

					if (Utilities.isEmpty(address.type())) {
						throw new InvalidParameterException("Address type is missing", origin);
					}

					if (!Utilities.isEnumValue(address.type(), AddressType.class)) {
						throw new InvalidParameterException("Invalid address type: " + address.type(), origin);
					}

					if (Utilities.isEmpty(address.address())) {
						throw new InvalidParameterException("Address value is missing", origin);
					}

					if (address.type().length() > ServiceRegistryConstants.ADDRESS_TYPE_LENGTH) {
						throw new InvalidParameterException("Address type is too long", origin);
					}

					if (address.address().length() > ServiceRegistryConstants.ADDRESS_ADDRESS_LENGTH) {
						throw new InvalidParameterException("Address is too long", origin);
					}
				}
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void validateUpdateDevices(final DeviceListRequestDTO dto, final String origin) {
		logger.debug("validateUpdateDevice started");
		validateCreateDevices(dto, origin);
	}

	//-------------------------------------------------------------------------------------------------
	public void validateQueryDevices(final DeviceQueryRequestDTO dto, final String origin) {
		logger.debug("validateQueryDevices started");

		if (dto != null) {
			pageValidator.validatePageParameter(dto.pagination(), Device.SORTABLE_FIELDS_BY, origin);

			if (!Utilities.isEmpty(dto.deviceNames()) && Utilities.containsNullOrEmpty(dto.deviceNames())) {
				throw new InvalidParameterException("Device name list contains null or empty element", origin);
			}

			if (!Utilities.isEmpty(dto.addresses()) && Utilities.containsNullOrEmpty(dto.addresses())) {
				throw new InvalidParameterException("Address list contains null element or empty element", origin);
			}

			if (!Utilities.isEmpty(dto.addressType()) && !Utilities.isEnumValue(dto.addressType(), AddressType.class)) {
				throw new InvalidParameterException("Invalid address type: " + dto.addressType(), origin);
			}

			if (!Utilities.isEmpty(dto.metadataRequirementList()) && Utilities.containsNull(dto.metadataRequirementList())) {
				throw new InvalidParameterException("Metadata requirement list contains null element", origin);
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void validateRemoveDevices(final List<String> names, final String origin) {
		logger.debug("validateRemoveDevices started");

		if (Utilities.isEmpty(names)) {
			throw new InvalidParameterException("Device name list is missing or empty", origin);
		}

		if (Utilities.containsNullOrEmpty(names)) {
			throw new InvalidParameterException("Device name list contains null or empty element", origin);
		}
	}

	// DEVICE VALIDATION AND NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public List<DeviceRequestDTO> validateAndNormalizeCreateDevices(final DeviceListRequestDTO dto, final String origin) {

		validateCreateDevices(dto, origin);

		final List<DeviceRequestDTO> normalized = normalizer.normalizeDeviceRequestDTOList(dto.devices());
		normalized.forEach(n -> n.addresses().forEach(address -> validateNormalizedAddress(address, origin)));

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public List<DeviceRequestDTO> validateAndNormalizeUpdateDevices(final DeviceListRequestDTO dto, final String origin) {

		validateUpdateDevices(dto, origin);

		final List<DeviceRequestDTO> normalized = normalizer.normalizeDeviceRequestDTOList(dto.devices());
		normalized.forEach(n -> n.addresses().forEach(address -> validateNormalizedAddress(address, origin)));

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public DeviceQueryRequestDTO validateAndNormalizeQueryDevices(final DeviceQueryRequestDTO dto, final String origin) {

		validateQueryDevices(dto, origin);

		final DeviceQueryRequestDTO normalized = dto == null ? new DeviceQueryRequestDTO(null, null, null, null, null)
				: normalizer.normalizeDeviceQueryRequestDTO(dto);

		if (!Utilities.isEmpty(normalized.addressType()) && !Utilities.isEmpty(normalized.addresses())) {
			normalized.addresses().forEach(a -> validateNormalizedAddress(new AddressDTO(normalized.addressType(), a), origin));
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> validateAndNormalizeRemoveDevices(final List<String> names, final String origin) {
		logger.debug("validateAndNormalizeRemoveDevices started");

		try {
			validateRemoveDevices(names, origin);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalizer.normalizeDeviceNames(names);
	}

	// SERVICE DEFINITION VALIDATION

	//-------------------------------------------------------------------------------------------------
	public void validateCreateServiceDefinition(final ServiceDefinitionListRequestDTO dto, final String origin) {
		logger.debug("validateCreateServiceDefinition started");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		if (Utilities.isEmpty(dto.serviceDefinitionNames())) {
			throw new InvalidParameterException("Request payload is empty", origin);
		}

		for (final String definitionName : dto.serviceDefinitionNames()) {
			if (Utilities.isEmpty(definitionName)) {
				throw new InvalidParameterException("Service definition name is missing", origin);
			}

			// verify no duplicates in list

			// TODO: max 63 chars and naming convention!
		}
	}

	// SERVICE DEFINITION VALIDATION AND NORMALIZATION
	// TODO

	// SYSTEM VALIDATION

	//-------------------------------------------------------------------------------------------------
	public void validateCreateSystems(final SystemListRequestDTO dto, final String origin) {
		logger.debug("validateCreateSystems started");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		if (Utilities.isEmpty(dto.systems())) {
			throw new InvalidParameterException("Request payload is empty", origin);
		}

		final Set<String> names = new HashSet<>();

		for (final SystemRequestDTO system : dto.systems()) {

			if (system == null) {
				throw new InvalidParameterException("System list contains null element", origin);
			}

			if (Utilities.isEmpty(system.name())) {
				throw new InvalidParameterException("System name is empty", origin);
			}

			if (names.contains(system.name())) {
				throw new InvalidParameterException("Duplicate system name: " + system.name(), origin);
			}

			if (system.name().length() > ServiceRegistryConstants.SYSTEM_NAME_LENGTH) {
				throw new InvalidParameterException("System name is too long: " + system.name(), origin);
			}

			names.add(system.name());

			if (!Utilities.isEmpty(system.addresses())) {
				for (final AddressDTO address : system.addresses()) {

					if (address == null) {
						throw new InvalidParameterException("Address list contains null element", origin);
					}

					if (Utilities.isEmpty(address.type())) {
						throw new InvalidParameterException("Address type is missing", origin);
					}

					if (!Utilities.isEnumValue(address.type(), AddressType.class)) {
						throw new InvalidParameterException("Invalid address type: " + address.type(), origin);
					}

					if (Utilities.isEmpty(address.address())) {
						throw new InvalidParameterException("Address value is missing", origin);
					}

					if (address.type().length() > ServiceRegistryConstants.ADDRESS_TYPE_LENGTH) {
						throw new InvalidParameterException("Address type is too long", origin);
					}

					if (address.address().length() > ServiceRegistryConstants.ADDRESS_ADDRESS_LENGTH) {
						throw new InvalidParameterException("Address is too long", origin);
					}
				}
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void validateUpdateSystems(final SystemListRequestDTO dto, final String origin) {
		logger.debug("validateCreateSystems started");

		validateCreateSystems(dto, origin);
	}

	//-------------------------------------------------------------------------------------------------
	public void validateQuerySystems(final SystemQueryRequestDTO dto, final String origin) {
		logger.debug("validateQuerySystems started");

		if (dto != null) {

			pageValidator.validatePageParameter(dto.pagination(), System.SORTABLE_FIELDS_BY, origin);
	
			if (!Utilities.isEmpty(dto.systemNames()) && Utilities.containsNullOrEmpty(dto.systemNames())) {
				throw new InvalidParameterException("System name list contains null or empty element", origin);
			}
	
			if (!Utilities.isEmpty(dto.addresses()) && Utilities.containsNullOrEmpty(dto.addresses())) {
				throw new InvalidParameterException("Address list contains null or empty element", origin);
			}
	
			if (!Utilities.isEmpty(dto.addressType()) && !Utilities.isEnumValue(dto.addressType(), AddressType.class)) {
				throw new InvalidParameterException("Invalid address type: " + dto.addressType(), origin);
			}
			
			if (!Utilities.isEmpty(dto.addresses()) && Utilities.isEmpty(dto.addressType())) {
				throw new InvalidParameterException("Address list is not empty, but the address type was not specified!");
			}
	
			if (!Utilities.isEmpty(dto.metadataRequirementList()) && Utilities.containsNull(dto.metadataRequirementList())) {
				throw new InvalidParameterException("Metadata requirement list contains null element", origin);
			}
	
			if (!Utilities.isEmpty(dto.versions()) && Utilities.containsNullOrEmpty(dto.versions())) {
				throw new InvalidParameterException("Version list contains null or empty element", origin);
			}
	
			if (!Utilities.isEmpty(dto.deviceNames()) && Utilities.containsNullOrEmpty(dto.deviceNames())) {
				throw new InvalidParameterException("Device name list contains null or empty element", origin);
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void validateRemoveSystems(final List<String> originalNames, final  String origin) {
		logger.debug("validateRemoveSystems started");

		if (Utilities.isEmpty(originalNames)) {
			throw new InvalidParameterException("System name list is missing or empty", origin);
		}

		if (Utilities.containsNullOrEmpty(originalNames)) {
			throw new InvalidParameterException("System name list contains null or empty element", origin);
		}
	}

	// SYSTEM VALIDATION AND NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public List<SystemRequestDTO> validateAndNormalizeCreateSystems(final SystemListRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeCreateSystems started");
		validateCreateSystems(dto, origin);

		final List<SystemRequestDTO> normalized = normalizer.normalizeSystemRequestDTOs(dto);

		normalized.forEach(n -> n.addresses().forEach(a -> validateNormalizedAddress(a, origin)));
		normalized.forEach(n -> validateNormalizedVersion(n.version(), origin));

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public List<SystemRequestDTO> validateAndNormalizeUpdateSystems(final SystemListRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeUpdateSystems started");

		return validateAndNormalizeCreateSystems(dto, origin);
	}

	//-------------------------------------------------------------------------------------------------
	public SystemQueryRequestDTO validateAndNormalizeQuerySystems(final SystemQueryRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeQuerySystems started");

		validateQuerySystems(dto, origin);

		final SystemQueryRequestDTO normalized = normalizer.normalizeSystemQueryRequestDTO(dto);

		if (!Utilities.isEmpty(normalized.addressType()) && !Utilities.isEmpty(normalized.addresses())) {
			normalized.addresses().forEach(na -> validateNormalizedAddress(new AddressDTO(normalized.addressType(), na), origin));
		}

		if (!Utilities.isEmpty(normalized.versions())) {
			normalized.versions().forEach(nv -> validateNormalizedVersion(nv, origin));
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> validateAndNormalizeRemoveSystems(final List<String> originalNames, final String origin) {
		logger.debug("validateAndNormalizeRemoveSystems started");

		validateRemoveSystems(originalNames, origin);

		return normalizer.normalizeSystemNames(originalNames);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void validateNormalizedVersion(final String version, final String origin) {
		logger.debug("validateNormalizedVersion started");

		try {
			versionValidator.validateNormalizedVersion(version);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateNormalizedAddress(final AddressDTO dto, final String origin) {
		logger.debug("validateNormalizedAddress started");
		Assert.isTrue(Utilities.isEnumValue(dto.type(), AddressType.class), "address type is invalid");

		if (dto.type().length() > ServiceRegistryConstants.ADDRESS_TYPE_LENGTH) {
			throw new InvalidParameterException("Address type is too long", origin);
		}

		if (dto.address().length() > ServiceRegistryConstants.ADDRESS_ADDRESS_LENGTH) {
			throw new InvalidParameterException("Address is too long", origin);
		}

		try {
			addressTypeValidator.validateNormalizedAddress(AddressType.valueOf(dto.type()), dto.address());
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}
	}
}
