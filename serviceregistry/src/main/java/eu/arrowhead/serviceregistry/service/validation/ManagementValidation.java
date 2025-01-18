package eu.arrowhead.serviceregistry.service.validation;

import java.time.DateTimeException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
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
import eu.arrowhead.common.service.validation.MetadataValidation;
import eu.arrowhead.common.service.validation.PageValidator;
import eu.arrowhead.common.service.validation.address.AddressValidator;
import eu.arrowhead.common.service.validation.name.NameNormalizer;
import eu.arrowhead.common.service.validation.name.NameValidator;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.DeviceListRequestDTO;
import eu.arrowhead.dto.DeviceQueryRequestDTO;
import eu.arrowhead.dto.DeviceRequestDTO;
import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.dto.ServiceDefinitionListRequestDTO;
import eu.arrowhead.dto.ServiceInstanceCreateListRequestDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceRequestDTO;
import eu.arrowhead.dto.ServiceInstanceQueryRequestDTO;
import eu.arrowhead.dto.ServiceInstanceRequestDTO;
import eu.arrowhead.dto.ServiceInstanceUpdateListRequestDTO;
import eu.arrowhead.dto.ServiceInstanceUpdateRequestDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplateListRequestDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplatePropertyDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplateQueryRequestDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplateRequestDTO;
import eu.arrowhead.dto.SystemListRequestDTO;
import eu.arrowhead.dto.SystemQueryRequestDTO;
import eu.arrowhead.dto.SystemRequestDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;
import eu.arrowhead.serviceregistry.ServiceRegistryConstants;
import eu.arrowhead.serviceregistry.jpa.entity.Device;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceDefinition;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInterfaceTemplate;
import eu.arrowhead.serviceregistry.jpa.entity.System;
import eu.arrowhead.serviceregistry.service.dto.NormalizedDeviceRequestDTO;
import eu.arrowhead.serviceregistry.service.dto.NormalizedSystemRequestDTO;
import eu.arrowhead.serviceregistry.service.normalization.ManagementNormalization;
import eu.arrowhead.serviceregistry.service.validation.interf.InterfaceValidator;
import eu.arrowhead.serviceregistry.service.validation.version.VersionValidator;

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
	private InterfaceValidator interfaceValidator;

	@Autowired
	private NameValidator nameValidator;

	@Autowired
	private NameNormalizer nameNormalizer; //for checking duplications

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

			if (names.contains(nameNormalizer.normalize(device.name()))) {
				throw new InvalidParameterException("Duplicate device name: " + device.name(), origin);
			}

			names.add(nameNormalizer.normalize(device.name()));

			if (Utilities.isEmpty(device.addresses())) {
				throw new InvalidParameterException("At least one device address is needed for every device", origin);
			}

			for (final String address : device.addresses()) {
				if (Utilities.isEmpty(address)) {
					throw new InvalidParameterException("Address is missing", origin);
				}

				if (address.trim().length() > ServiceRegistryConstants.ADDRESS_LENGTH) {
					throw new InvalidParameterException("Address is too long", origin);
				}
			}

			if (!Utilities.isEmpty(device.metadata())) {
				MetadataValidation.validateMetadataKey(device.metadata());
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

			if (!Utilities.isEmpty(dto.addressType()) && !Utilities.isEnumValue(dto.addressType().toUpperCase(), AddressType.class)) {
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
	public List<NormalizedDeviceRequestDTO> validateAndNormalizeCreateDevices(final DeviceListRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeCreateDevices started");

		validateCreateDevices(dto, origin);

		final List<NormalizedDeviceRequestDTO> normalized = normalizer.normalizeDeviceRequestDTOList(dto.devices());
		normalized.forEach(n -> validateNormalizedName(n.name(), origin));
		normalized.forEach(n -> n.addresses().forEach(address -> validateNormalizedAddress(address, origin)));

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public List<NormalizedDeviceRequestDTO> validateAndNormalizeUpdateDevices(final DeviceListRequestDTO dto, final String origin) {

		validateUpdateDevices(dto, origin);

		final List<NormalizedDeviceRequestDTO> normalized = normalizer.normalizeDeviceRequestDTOList(dto.devices());
		normalized.forEach(n -> validateNormalizedName(n.name(), origin));
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
	public void validateQueryServiceDefinitions(final PageDTO dto, final String origin) {
		logger.debug("validateQueryServiceDefinitions started");

		if (dto != null) {
			pageValidator.validatePageParameter(dto, ServiceDefinition.SORTABLE_FIELDS_BY, origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void validateCreateServiceDefinitions(final ServiceDefinitionListRequestDTO dto, final String origin) {
		logger.debug("validateCreateServiceDefinition started");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		if (Utilities.isEmpty(dto.serviceDefinitionNames())) {
			throw new InvalidParameterException("Service definition name list is empty", origin);
		}

		if (Utilities.containsNullOrEmpty(dto.serviceDefinitionNames())) {
			throw new InvalidParameterException("Service definition name list contains null or empty element", origin);
		}

		final List<String> names = new ArrayList<>(dto.serviceDefinitionNames().size());

		for (final String name : dto.serviceDefinitionNames()) {

			if (names.contains(nameNormalizer.normalize(name))) {
				throw new InvalidParameterException("Duplicated service defitition name: " + name, origin);
			}

			if (name.length() > ServiceRegistryConstants.SERVICE_DEFINITION_NAME_LENGTH) {
				throw new InvalidParameterException("Service definition name is too long: " + name, origin);
			}

			names.add(nameNormalizer.normalize(name));

		}

	}

	//-------------------------------------------------------------------------------------------------
	public void validateRemoveServiceDefinitions(final List<String> names, final String origin) {
		logger.debug("validateRemoveServiceDefinitions started");

		if (Utilities.isEmpty(names)) {
			throw new InvalidParameterException("Service definition name list is missing or empty", origin);
		}

		if (Utilities.containsNullOrEmpty(names)) {
			throw new InvalidParameterException("Service definition name list contains null or empty element", origin);
		}
	}

	// SERVICE DEFINITION VALIDATION AND NORMALIZATION
	//-------------------------------------------------------------------------------------------------
	public List<String> validateAndNormalizeCreateServiceDefinitions(final ServiceDefinitionListRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeCreateServiceDefinitions started");

		validateCreateServiceDefinitions(dto, origin);

		final List<String> normalized = normalizer.normalizeCreateServiceDefinitions(dto);
		normalized.forEach(n -> validateNormalizedName(n, origin));

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> validateAndNormalizeRemoveServiceDefinitions(final List<String> names, final String origin) {
		logger.debug("validateAndNormalizeRemoveServiceDefinitions started");

		try {
			validateRemoveServiceDefinitions(names, origin);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalizer.normalizeRemoveServiceDefinitions(names);
	}

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

			if (names.contains(nameNormalizer.normalize(system.name()))) {
				throw new InvalidParameterException("Duplicated system name: " + system.name(), origin);
			}

			if (system.name().length() > ServiceRegistryConstants.SYSTEM_NAME_LENGTH) {
				throw new InvalidParameterException("System name is too long: " + system.name(), origin);
			}

			names.add(nameNormalizer.normalize(system.name()));

			if (!Utilities.isEmpty(system.addresses())) {
				for (final String address : system.addresses()) {
					if (Utilities.isEmpty(address)) {
						throw new InvalidParameterException("Address value is missing", origin);
					}

					if (address.trim().length() > ServiceRegistryConstants.ADDRESS_LENGTH) {
						throw new InvalidParameterException("Address is too long", origin);
					}
				}
			}

			if (Utilities.isEmpty(system.addresses()) && Utilities.isEmpty(system.deviceName())) {
				throw new InvalidParameterException("At least one system address is needed for every system");
			}

			if (!Utilities.isEmpty(system.metadata())) {
				MetadataValidation.validateMetadataKey(system.metadata());
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void validateUpdateSystems(final SystemListRequestDTO dto, final String origin) {
		logger.debug("validateUpdateSystems started");

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
	public void validateRemoveSystems(final List<String> originalNames, final String origin) {
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
	public List<NormalizedSystemRequestDTO> validateAndNormalizeCreateSystems(final SystemListRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeCreateSystems started");

		validateCreateSystems(dto, origin);

		final List<NormalizedSystemRequestDTO> normalized = normalizer.normalizeSystemRequestDTOs(dto);

		normalized.forEach(n -> validateNormalizedName(n.name(), origin));
		normalized.forEach(n -> validateNormalizedVersion(n.version(), origin));
		normalized.forEach(n -> n.addresses().forEach(a -> validateNormalizedAddress(a, origin)));

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public List<NormalizedSystemRequestDTO> validateAndNormalizeUpdateSystems(final SystemListRequestDTO dto, final String origin) {
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

		return normalizer.normalizeRemoveSystemNames(originalNames);
	}

	// SERVICE INSTANCE VALIDATION

	//-------------------------------------------------------------------------------------------------
	public void validateCreateServiceInstances(final ServiceInstanceCreateListRequestDTO dto, final String origin) {
		logger.debug("validateCreateServiceInstances started");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		if (Utilities.isEmpty(dto.instances())) {
			throw new InvalidParameterException("Request payload is empty", origin);
		}

		for (final ServiceInstanceRequestDTO instance : dto.instances()) {

			// system name
			if (Utilities.isEmpty(instance.systemName())) {
				throw new InvalidParameterException("System name is empty", origin);
			}

			if (instance.systemName().length() > ServiceRegistryConstants.SYSTEM_NAME_LENGTH) {
				throw new InvalidParameterException("System name is too long: " + instance.systemName(), origin);
			}

			// service definition name
			if (Utilities.isEmpty(instance.serviceDefinitionName())) {
				throw new InvalidParameterException("Service definition name is empty", origin);
			}

			if (instance.serviceDefinitionName().length() > ServiceRegistryConstants.SERVICE_DEFINITION_NAME_LENGTH) {
				throw new InvalidParameterException("Service definition name is too long: " + instance.serviceDefinitionName(), origin);
			}

			// version -> can be empty (default will be set at normalization)

			// expires at
			if (!Utilities.isEmpty(instance.expiresAt())) {
				ZonedDateTime expiresAt = null;
				try {
					expiresAt = Utilities.parseUTCStringToZonedDateTime(instance.expiresAt());
				} catch (final DateTimeException ex) {
					throw new InvalidParameterException("Expiration time has an invalid time format, UTC string expected (example: 2024-10-11T14:30:00Z)", origin);
				}
				if (Utilities.utcNow().isAfter(expiresAt)) {
					throw new InvalidParameterException("Expiration time is in the past", origin);
				}
			}

			// metadata
			if (!Utilities.isEmpty(instance.metadata())) {
				MetadataValidation.validateMetadataKey(instance.metadata());
			}

			// interfaces
			if (Utilities.isEmpty(instance.interfaces())) {
				throw new InvalidParameterException("Service interface list is empty", origin);
			}

			for (final ServiceInstanceInterfaceRequestDTO interfaceDTO : instance.interfaces()) {
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
	}

	//-------------------------------------------------------------------------------------------------
	public void validateUpdateServiceInstances(final ServiceInstanceUpdateListRequestDTO dto, final String origin) {
		logger.debug("ServiceInstanceUpdateListRequestDTO started");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		if (Utilities.isEmpty(dto.instances())) {
			throw new InvalidParameterException("Request payload is empty", origin);
		}

		final Set<String> instanceIds = new HashSet<String>();
		for (final ServiceInstanceUpdateRequestDTO instance : dto.instances()) {

			// instance id
			if (Utilities.isEmpty(instance.instanceId())) {
				throw new InvalidParameterException("Instance id is empty");
			}

			if (instanceIds.contains(nameNormalizer.normalize(instance.instanceId()))) {
				throw new InvalidParameterException("Duplicated instance id: " + instance.instanceId());
			}

			instanceIds.add(nameNormalizer.normalize(instance.instanceId()));

			// expires at
			if (!Utilities.isEmpty(instance.expiresAt())) {
				ZonedDateTime expiresAt = null;
				try {
					expiresAt = Utilities.parseUTCStringToZonedDateTime(instance.expiresAt());
				} catch (final DateTimeException ex) {
					throw new InvalidParameterException("Expiration time has an invalid time format, UTC string expected (example: 2024-10-11T14:30:00Z)", origin);
				}
				if (Utilities.utcNow().isAfter(expiresAt)) {
					throw new InvalidParameterException("Expiration time is in the past", origin);
				}
			}

			// metadata
			if (!Utilities.isEmpty(instance.metadata())) {
				MetadataValidation.validateMetadataKey(instance.metadata());
			}

			// interfaces
			if (Utilities.isEmpty(instance.interfaces())) {
				throw new InvalidParameterException("Service interface list is empty", origin);
			}

			for (final ServiceInstanceInterfaceRequestDTO interfaceDTO : instance.interfaces()) {
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
	}

	//-------------------------------------------------------------------------------------------------
	public void validateRemoveServiceInstances(final List<String> instanceIds, final String origin) {
		if (Utilities.isEmpty(instanceIds)) {
			throw new InvalidParameterException("Instance id list is empty", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void validateQueryServiceInstances(final ServiceInstanceQueryRequestDTO dto, final String origin) {
		logger.debug("validateQueryServiceInstances started");

		if (dto != null) {
			// pagination
			pageValidator.validatePageParameter(dto.pagination(), Device.SORTABLE_FIELDS_BY, origin);

			// check if instanceIds, providerNames and serviceDefinitionNames are all empty
			if (Utilities.isEmpty(dto.instanceIds()) && Utilities.isEmpty(dto.providerNames()) && Utilities.isEmpty(dto.serviceDefinitionNames())) {
				throw new InvalidParameterException("One of the following filters must be used: 'instanceIds', 'providerNames', 'serviceDefinitionNames'", origin);
			}

			// instanceIds
			if (!Utilities.isEmpty(dto.instanceIds()) && Utilities.containsNullOrEmpty(dto.instanceIds())) {
				throw new InvalidParameterException("Instance id list contains null or empty element", origin);
			}

			// providerNames
			if (!Utilities.isEmpty(dto.providerNames()) && Utilities.containsNullOrEmpty(dto.providerNames())) {
				throw new InvalidParameterException("Provider name list contains null or empty element", origin);
			}

			// serviceDefinitionNames
			if (!Utilities.isEmpty(dto.serviceDefinitionNames()) && Utilities.containsNullOrEmpty(dto.serviceDefinitionNames())) {
				throw new InvalidParameterException("Service definition name list contains null or empty element", origin);
			}

			// versions
			if (!Utilities.isEmpty(dto.versions()) && Utilities.containsNullOrEmpty(dto.versions())) {
				throw new InvalidParameterException("Version list contains null or empty element", origin);
			}

			// alivesAt
			if (!Utilities.isEmpty(dto.alivesAt())) {
				try {
					Utilities.parseUTCStringToZonedDateTime(dto.alivesAt());
				} catch (final DateTimeException ex) {
					throw new InvalidParameterException("Alive time has an invalid time format", origin);
				}
			}

			// metadataRequirementsList
			if (!Utilities.isEmpty(dto.metadataRequirementsList()) && Utilities.containsNull(dto.metadataRequirementsList())) {
				throw new InvalidParameterException("Metadata requirements list contains null element", origin);
			}

			// interfaceTemplateNames
			if (!Utilities.isEmpty(dto.interfaceTemplateNames()) && Utilities.containsNullOrEmpty(dto.interfaceTemplateNames())) {
				throw new InvalidParameterException("Interface template list contains null or empty element", origin);
			}

			// interfacePropertyRequirementsList
			if (!Utilities.isEmpty(dto.interfacePropertyRequirementsList()) && Utilities.containsNull(dto.interfacePropertyRequirementsList())) {
				throw new InvalidParameterException("Interface property requirements list contains null element", origin);
			}

			// policies
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
	}

	// SERVICE INSTANCE VALIDATION AND NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public List<ServiceInstanceRequestDTO> validateAndNormalizeCreateServiceInstances(final ServiceInstanceCreateListRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeCreateServiceInstances started");

		validateCreateServiceInstances(dto, origin);

		final List<ServiceInstanceRequestDTO> normalized = normalizer.normalizeCreateServiceInstances(dto);

		normalized.forEach(n -> {
			nameValidator.validateName(n.systemName());
			nameValidator.validateName(n.serviceDefinitionName());
			versionValidator.validateNormalizedVersion(n.version());
			interfaceValidator.validateNormalizedInterfaceInstancesWithPropsNormalization(n.interfaces());
		});

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public List<ServiceInstanceUpdateRequestDTO> validateAndNormalizeUpdateServiceInstances(final ServiceInstanceUpdateListRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeUpdateServiceInstances started");

		validateUpdateServiceInstances(dto, origin);

		final List<ServiceInstanceUpdateRequestDTO> normalized = normalizer.normalizeUpdateServiceInstances(dto);

		normalized.forEach(n -> interfaceValidator.validateNormalizedInterfaceInstancesWithPropsNormalization(n.interfaces()));

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> validateAndNormalizeRemoveServiceInstances(final List<String> instanceIds, final String origin) {
		logger.debug("validateAndNormalizeRevokeServiceInstances started");

		validateRemoveServiceInstances(instanceIds, origin);
		return normalizer.normalizeRemoveServiceInstances(instanceIds);
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceInstanceQueryRequestDTO validateAndNormalizeQueryServiceInstances(final ServiceInstanceQueryRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeQueryServiceInstances");

		validateQueryServiceInstances(dto, origin);

		return normalizer.normalizeQueryServiceInstances(dto);
	}

	// INTERFACE VALIDATION

	//-------------------------------------------------------------------------------------------------
	public void validateCreateInterfaceTemplates(final ServiceInterfaceTemplateListRequestDTO dto, final String origin) {
		logger.debug("validateCreateInterfaceTemplates started");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		if (Utilities.isEmpty(dto.interfaceTemplates())) {
			throw new InvalidParameterException("Request payload is empty", origin);
		}

		final Set<String> templateNames = new HashSet<>();
		for (final ServiceInterfaceTemplateRequestDTO templateDTO : dto.interfaceTemplates()) {
			if (templateDTO == null) {
				throw new InvalidParameterException("Interface template list contains null element", origin);
			}
			if (Utilities.isEmpty(templateDTO.name())) {
				throw new InvalidParameterException("Interface template name is empty", origin);
			}
			if (templateNames.contains(templateDTO.name().trim().toLowerCase())) {
				throw new InvalidParameterException("Duplicate interface template name: " + templateDTO.name(), origin);
			}
			templateNames.add(templateDTO.name().trim().toLowerCase());
			if (Utilities.isEmpty(templateDTO.protocol())) {
				throw new InvalidParameterException("Interface template protocol is empty", origin);
			}
			if (!Utilities.isEmpty(templateDTO.propertyRequirements())) {
				final Set<String> propertyNames = new HashSet<>();
				for (final ServiceInterfaceTemplatePropertyDTO propertyDTO : templateDTO.propertyRequirements()) {
					if (propertyDTO == null) {
						throw new InvalidParameterException("Interface template contains null property", origin);
					}
					if (Utilities.isEmpty(propertyDTO.name())) {
						throw new InvalidParameterException("Interface template property name is empty", origin);
					}
					if (propertyNames.contains(propertyDTO.name().trim().toLowerCase())) {
						throw new InvalidParameterException("Duplicate interface template property name: " + templateDTO.name() + "." + propertyDTO.name(), origin);
					}
					propertyNames.add(propertyDTO.name().trim().toLowerCase());
					if (!Utilities.isEmpty(propertyDTO.validatorParams())) {
						if (Utilities.isEmpty(propertyDTO.validator())) {
							throw new InvalidParameterException("Interface template property validator is empty while validator params are defined", origin);
						}
						if (Utilities.containsNullOrEmpty(propertyDTO.validatorParams())) {
							throw new InvalidParameterException("Interface template property validator parameter list contains empty element", origin);
						}
					}
				}
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void validateQueryInterfaceTemplates(final ServiceInterfaceTemplateQueryRequestDTO dto, final String origin) {
		logger.debug("validateQueryInterfaceTemplates started");

		if (dto != null) {

			pageValidator.validatePageParameter(dto.pagination(), ServiceInterfaceTemplate.SORTABLE_FIELDS_BY, origin);

			if (!Utilities.isEmpty(dto.templateNames()) && Utilities.containsNullOrEmpty(dto.templateNames())) {
				throw new InvalidParameterException("Interface template name list contains empty element", origin);
			}

			if (!Utilities.isEmpty(dto.protocols()) && Utilities.containsNullOrEmpty(dto.protocols())) {
				throw new InvalidParameterException("Interface template protocol list contains empty element", origin);
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void validateRemoveInterfaceTemplates(final List<String> originalNames, final String origin) {
		logger.debug("validateRemoveInterfaceTemplate started");

		if (Utilities.isEmpty(originalNames)) {
			throw new InvalidParameterException("Interface template name list is missing or empty", origin);
		}

		if (Utilities.containsNullOrEmpty(originalNames)) {
			throw new InvalidParameterException("Interface templpate name list contains null or empty element", origin);
		}
	}

	// INTERFACE VALIDATION AND NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public ServiceInterfaceTemplateListRequestDTO validateAndNormalizeCreateInterfaceTemplates(final ServiceInterfaceTemplateListRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeCreateInterfaceTemplates started");

		validateCreateInterfaceTemplates(dto, origin);
		final ServiceInterfaceTemplateListRequestDTO normalized = normalizer.normalizeServiceInterfaceTemplateListRequestDTO(dto);

		try {
			interfaceValidator.validateNormalizedInterfaceTemplates(normalized.interfaceTemplates());
			return normalized;

		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceInterfaceTemplateQueryRequestDTO validateAndNormalizeQueryInterfaceTemplates(final ServiceInterfaceTemplateQueryRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeQueryInterfaceTemplates started");

		validateQueryInterfaceTemplates(dto, origin);
		return normalizer.normalizeServiceInterfaceTemplateQueryRequestDTO(dto);
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> validateAndNormalizeRemoveInterfaceTemplates(final List<String> originalNames, final String origin) {
		logger.debug("validateAndNormalizeRemoveInterfaceTemplates started");

		validateRemoveInterfaceTemplates(originalNames, origin);

		return normalizer.normalizeRemoveInterfaceTemplates(originalNames);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void validateNormalizedName(final String name, final String origin) {
		logger.debug("validateNormalizedName started");

		try {
			nameValidator.validateName(name);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}
	}

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

		if (dto.address().trim().length() > ServiceRegistryConstants.ADDRESS_LENGTH) {
			throw new InvalidParameterException("Address is too long", origin);
		}

		try {
			addressTypeValidator.validateNormalizedAddress(AddressType.valueOf(dto.type()), dto.address());
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}
	}
}