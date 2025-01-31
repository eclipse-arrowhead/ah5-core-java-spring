package eu.arrowhead.serviceregistry.service.normalization;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.validation.address.AddressNormalizer;
import eu.arrowhead.common.service.validation.address.AddressValidator;
import eu.arrowhead.common.service.validation.name.NameNormalizer;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.DeviceQueryRequestDTO;
import eu.arrowhead.dto.DeviceRequestDTO;
import eu.arrowhead.dto.ServiceDefinitionListRequestDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplateListRequestDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplateQueryRequestDTO;
import eu.arrowhead.dto.SystemListRequestDTO;
import eu.arrowhead.dto.SystemQueryRequestDTO;
import eu.arrowhead.dto.SystemRequestDTO;
import eu.arrowhead.serviceregistry.service.dto.NormalizedDeviceRequestDTO;
import eu.arrowhead.serviceregistry.service.dto.NormalizedSystemRequestDTO;
import eu.arrowhead.serviceregistry.service.validation.interf.InterfaceNormalizer;
import eu.arrowhead.serviceregistry.service.validation.version.VersionNormalizer;
import eu.arrowhead.dto.ServiceInstanceCreateListRequestDTO;
import eu.arrowhead.dto.ServiceInstanceQueryRequestDTO;
import eu.arrowhead.dto.ServiceInstanceRequestDTO;
import eu.arrowhead.dto.ServiceInstanceUpdateListRequestDTO;
import eu.arrowhead.dto.ServiceInstanceUpdateRequestDTO;

@Service
public class ManagementNormalization {
	//=================================================================================================
	// members

	@Autowired
	private AddressValidator addressValidator;

	@Autowired
	private AddressNormalizer addressNormalizer;

	@Autowired
	private VersionNormalizer versionNormalizer;

	@Autowired
	private InterfaceNormalizer interfaceNormalizer;

	@Autowired
	private NameNormalizer nameNormalizer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	// SYSTEMS

	//-------------------------------------------------------------------------------------------------
	public List<NormalizedSystemRequestDTO> normalizeSystemRequestDTOs(final SystemListRequestDTO dtoList) {
		logger.debug("normalizeSystemRequestDTOs started");
		Assert.notNull(dtoList, "SystemListRequestDTO is null");
		Assert.notNull(dtoList.systems(), "system list is null");

		final List<NormalizedSystemRequestDTO> normalized = new ArrayList<>(dtoList.systems().size());
		for (final SystemRequestDTO system : dtoList.systems()) {
			normalized.add(new NormalizedSystemRequestDTO(
					nameNormalizer.normalize(system.name()),
					system.metadata(),
					versionNormalizer.normalize(system.version()),
					Utilities.isEmpty(system.addresses()) ? new ArrayList<>()
							: system.addresses().stream()
									.map(a -> addressNormalizer.normalize(a))
									.map(na -> new AddressDTO(addressValidator.detectType(na).name(), na))
									.collect(Collectors.toList()),
					Utilities.isEmpty(system.deviceName()) ? null : nameNormalizer.normalize(system.deviceName())));
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public SystemQueryRequestDTO normalizeSystemQueryRequestDTO(final SystemQueryRequestDTO dto) {
		logger.debug("normalizeSystemQueryRequestDTO started");

		if (dto == null) {
			return new SystemQueryRequestDTO(null, null, null, null, null, null, null);
		}

		return new SystemQueryRequestDTO(
				dto.pagination(), // no need to normalize, because it will happen in the getPageRequest method
				Utilities.isEmpty(dto.systemNames()) ? null
						: dto.systemNames().stream().map(n -> nameNormalizer.normalize(n)).collect(Collectors.toList()),
				Utilities.isEmpty(dto.addresses()) ? null
						: dto.addresses().stream().map(n -> n.trim()).collect(Collectors.toList()),
				Utilities.isEmpty(dto.addressType()) ? null
						: dto.addressType().trim(),
				dto.metadataRequirementList(),
				Utilities.isEmpty(dto.versions()) ? null
						: dto.versions().stream()
								.map(v -> versionNormalizer.normalize(v))
								.collect(Collectors.toList()),
				Utilities.isEmpty(dto.deviceNames()) ? null
						: dto.deviceNames().stream().map(n -> nameNormalizer.normalize(n)).collect(Collectors.toList()));
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> normalizeRemoveSystemNames(final List<String> originalNames) {
		logger.debug("normalizeRemoveSystemNames started");
		Assert.notNull(originalNames, "name list is null");

		return originalNames.stream()
				.filter(n -> !Utilities.isEmpty(n))
				.map(n -> nameNormalizer.normalize(n))
				.collect(Collectors.toList());
	}

	// DEVICES

	//-------------------------------------------------------------------------------------------------
	public List<NormalizedDeviceRequestDTO> normalizeDeviceRequestDTOList(final List<DeviceRequestDTO> dtoList) {
		logger.debug("normalizeDeviceRequestDTOs started");
		Assert.notNull(dtoList, "DeviceRequestDTO list is null");

		final List<NormalizedDeviceRequestDTO> normalized = new ArrayList<>(dtoList.size());
		for (final DeviceRequestDTO device : dtoList) {
			Assert.isTrue(!Utilities.isEmpty(device.name()), "Device name is empty");
			normalized.add(new NormalizedDeviceRequestDTO(
					nameNormalizer.normalize(device.name()),
					device.metadata(),
					Utilities.isEmpty(device.addresses()) ? new ArrayList<>()
							: device.addresses().stream()
									.map(a -> addressNormalizer.normalize(a))
									.map(na -> new AddressDTO(addressValidator.detectType(na).name(), na))
									.collect(Collectors.toList())));
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public DeviceQueryRequestDTO normalizeDeviceQueryRequestDTO(final DeviceQueryRequestDTO dto) {
		logger.debug("normalizeDeviceQueryRequestDTO started");
		Assert.notNull(dto, "DeviceQueryRequestDTO list is null");

		return new DeviceQueryRequestDTO(
				dto.pagination(),
				Utilities.isEmpty(dto.deviceNames()) ? null : dto.deviceNames().stream().map(n -> nameNormalizer.normalize(n)).collect(Collectors.toList()),
				Utilities.isEmpty(dto.addresses()) ? null : dto.addresses().stream().map(a -> addressNormalizer.normalize(a)).collect(Collectors.toList()),
				Utilities.isEmpty(dto.addressType()) ? null : dto.addressType().trim().toUpperCase(),
				dto.metadataRequirementList());
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> normalizeDeviceNames(final List<String> originalNames) {
		logger.debug("normalizeDeviceNames started");
		Assert.notNull(originalNames, "name list is null");

		return originalNames.stream()
				.filter(n -> !Utilities.isEmpty(n))
				.map(n -> nameNormalizer.normalize(n))
				.collect(Collectors.toList());
	}

	// SERVICE DEFINITIONS

	//-------------------------------------------------------------------------------------------------
	public List<String> normalizeCreateServiceDefinitions(final ServiceDefinitionListRequestDTO dto) {
		logger.debug("normalizeCreateServiceDefinitions started");
		Assert.notNull(dto, "dto is null");
		Assert.notNull(dto.serviceDefinitionNames(), "service definition name list is null");

		return dto.serviceDefinitionNames()
				.stream()
				.map(n -> nameNormalizer.normalize(n))
				.collect(Collectors.toList());
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> normalizeRemoveServiceDefinitions(final List<String> names) {
		logger.debug("normalizeRemoveServiceDefinitions started");
		Assert.notNull(names, "name list is null");

		return names
				.stream()
				.map(n -> nameNormalizer.normalize(n))
				.collect(Collectors.toList());
	}

	// SERVICE INSTANCES

	//-------------------------------------------------------------------------------------------------
	public List<ServiceInstanceRequestDTO> normalizeCreateServiceInstances(final ServiceInstanceCreateListRequestDTO dto) {
		logger.debug("normalizeCreateServiceInstances started");
		Assert.notNull(dto, "ServiceInstanceCreateListRequestDTO is null");
		Assert.notNull(dto.instances(), "instance list is null");

		return dto.instances().stream().map(i -> normalizeServiceInstanceRequestDTO(i)).collect(Collectors.toList());
	}

	//-------------------------------------------------------------------------------------------------
	public List<ServiceInstanceUpdateRequestDTO> normalizeUpdateServiceInstances(final ServiceInstanceUpdateListRequestDTO dto) {
		logger.debug("normalizeUpdateServiceInstances started");
		Assert.notNull(dto, "ServiceInstanceUpdateListRequestDTO is null");
		Assert.notNull(dto.instances(), "instance list is null");

		return dto.instances().stream().map(i -> normalizeServiceInstanceUpdateRequestDTO(i)).collect(Collectors.toList());
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> normalizeRemoveServiceInstances(final List<String> instanceIds) {
		logger.debug("normalizeRemoveServiceInstances started");
		Assert.notNull(instanceIds, "instanceId list is null");

		return instanceIds.stream().map(i -> nameNormalizer.normalize(i)).collect(Collectors.toList());
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceInstanceQueryRequestDTO normalizeQueryServiceInstances(final ServiceInstanceQueryRequestDTO dto) {
		logger.debug("normalizeQueryServiceInstances started");
		Assert.notNull(dto, "ServiceInstanceQueryRequestDTO is null");

		return new ServiceInstanceQueryRequestDTO(
				dto.pagination(),
				Utilities.isEmpty(dto.instanceIds()) ? new ArrayList<>() : dto.instanceIds().stream().map(id -> normalizeServiceInstanceId(id)).toList(),
				Utilities.isEmpty(dto.providerNames()) ? new ArrayList<>() : dto.providerNames().stream().map(n -> normalizeSystemName(n)).toList(),
				Utilities.isEmpty(dto.serviceDefinitionNames()) ? new ArrayList<>() : dto.serviceDefinitionNames().stream().map(sd -> nameNormalizer.normalize(sd)).toList(),
				Utilities.isEmpty(dto.versions()) ? new ArrayList<>() : dto.versions().stream().map(v -> versionNormalizer.normalize(v)).toList(),
				Utilities.isEmpty(dto.alivesAt()) ? "" : dto.alivesAt().trim(),
				Utilities.isEmpty(dto.metadataRequirementsList()) ? new ArrayList<>() : dto.metadataRequirementsList(),
				Utilities.isEmpty(dto.interfaceTemplateNames()) ? new ArrayList<>() : dto.interfaceTemplateNames().stream().map(i -> nameNormalizer.normalize(i)).toList(),
				Utilities.isEmpty(dto.interfacePropertyRequirementsList()) ? new ArrayList<>() : dto.interfacePropertyRequirementsList(),
				Utilities.isEmpty(dto.policies()) ? new ArrayList<>() : dto.policies().stream().map(p -> p.trim().toUpperCase()).toList());
	}

	// INTERFACE TEMPLATES

	//-------------------------------------------------------------------------------------------------
	public ServiceInterfaceTemplateListRequestDTO normalizeServiceInterfaceTemplateListRequestDTO(final ServiceInterfaceTemplateListRequestDTO dto) {
		logger.debug("normalizeServiceInterfaceTemplateListRequestDTO started");
		Assert.notNull(dto, "ServiceInterfaceTemplateListRequestDTO is null");
		Assert.notNull(dto.interfaceTemplates(), "interface template list is null");

		return new ServiceInterfaceTemplateListRequestDTO(
				dto.interfaceTemplates().stream()
						.map(t -> interfaceNormalizer.normalizeTemplateDTO(t))
						.toList());
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceInterfaceTemplateQueryRequestDTO normalizeServiceInterfaceTemplateQueryRequestDTO(final ServiceInterfaceTemplateQueryRequestDTO dto) {
		logger.debug("normalizeServiceInterfaceTemplateQueryRequestDTO started");

		if (dto == null) {
			return new ServiceInterfaceTemplateQueryRequestDTO(null, new ArrayList<>(), new ArrayList<>());
		}

		return new ServiceInterfaceTemplateQueryRequestDTO(
				dto.pagination(), // no need to normalize, because it will happen in the getPageRequest method
				Utilities.isEmpty(dto.templateNames()) ? new ArrayList<>() : dto.templateNames().stream().map(n -> nameNormalizer.normalize(n)).toList(),
				Utilities.isEmpty(dto.protocols()) ? new ArrayList<>() : dto.protocols().stream().map(p -> p.trim().toLowerCase()).toList());
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> normalizeRemoveInterfaceTemplates(final List<String> names) {
		logger.debug("normalizeRemoveInterfaceTemplates started");
		Assert.notNull(names, "Interface template name list is null");

		return names
				.stream()
				.map(n -> nameNormalizer.normalize(n))
				.collect(Collectors.toList());
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private ServiceInstanceRequestDTO normalizeServiceInstanceRequestDTO(final ServiceInstanceRequestDTO dto) {
		logger.debug("normalizeServiceInstanceRequestDTO started...");

		return new ServiceInstanceRequestDTO(
				// system name
				nameNormalizer.normalize(dto.systemName()),

				// service definition name
				nameNormalizer.normalize(dto.serviceDefinitionName()),

				// version
				versionNormalizer.normalize(dto.version()),

				// expires at
				Utilities.isEmpty(dto.expiresAt()) ? "" : dto.expiresAt().trim(),

				// metadata
				dto.metadata(),

				// interfaces
				dto.interfaces()
						.stream()
						.map(i -> interfaceNormalizer.normalizeInterfaceDTO(i))
						.toList());
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceInstanceUpdateRequestDTO normalizeServiceInstanceUpdateRequestDTO(final ServiceInstanceUpdateRequestDTO dto) {
		logger.debug("normalizeServiceInstanceUpdateRequestDTO started...");

		return new ServiceInstanceUpdateRequestDTO(
				// instance id
				nameNormalizer.normalize(dto.instanceId()),

				// expires at
				Utilities.isEmpty(dto.expiresAt()) ? "" : dto.expiresAt().trim(),

				// metadata
				dto.metadata(),

				// interfaces
				dto.interfaces()
						.stream()
						.map(i -> interfaceNormalizer.normalizeInterfaceDTO(i))
						.toList());
	}

	//-------------------------------------------------------------------------------------------------
	private String normalizeSystemName(final String systemName) {
		logger.debug("normalizeSystemName started");
		Assert.isTrue(!Utilities.isEmpty(systemName), "systemName is empty");

		return nameNormalizer.normalize(systemName);
	}

	//-------------------------------------------------------------------------------------------------
	private String normalizeServiceInstanceId(final String instanceId) {
		logger.debug("normalizeServiceInstanceId started");
		Assert.isTrue(!Utilities.isEmpty(instanceId), "Service instance id is empty");

		return nameNormalizer.normalize(instanceId);
	}
}