package eu.arrowhead.serviceregistry.service.normalization;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.validation.name.NameNormalizer;
import eu.arrowhead.dto.ServiceInstanceLookupRequestDTO;
import eu.arrowhead.dto.ServiceInstanceRequestDTO;
import eu.arrowhead.serviceregistry.service.validation.interf.InterfaceNormalizer;
import eu.arrowhead.serviceregistry.service.validation.version.VersionNormalizer;

@Service
public class ServiceDiscoveryNormalization {

	//=================================================================================================
	// members

	@Autowired
	private NameNormalizer nameNormalizer;

	@Autowired
	private VersionNormalizer versionNormalizer;

	@Autowired
	private InterfaceNormalizer interfaceNormalizer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public ServiceInstanceRequestDTO normalizeServiceInstanceRequestDTO(final ServiceInstanceRequestDTO dto) {
		logger.debug("normalizeServiceInstanceRequestDTO started");
		Assert.notNull(dto, "ServiceInstanceRequestDTO is null");

		return new ServiceInstanceRequestDTO(
				normalizeSystemName(dto.systemName()),
				nameNormalizer.normalize(dto.serviceDefinitionName()),
				versionNormalizer.normalize(dto.version()),
				Utilities.isEmpty(dto.expiresAt()) ? "" : dto.expiresAt().trim(),
				dto.metadata(),
				Utilities.isEmpty(dto.interfaces()) ? new ArrayList<>()
						: dto.interfaces()
								.stream()
								.map(i -> interfaceNormalizer.normalizeInterfaceDTO(i))
								.toList());
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceInstanceLookupRequestDTO normalizeServiceInstanceLookupRequestDTO(final ServiceInstanceLookupRequestDTO dto) {
		logger.debug("normalizeServiceInstanceLookupRequestDTO started");
		Assert.notNull(dto, "ServiceInstanceLookupRequestDTO is null");

		return new ServiceInstanceLookupRequestDTO(
				Utilities.isEmpty(dto.instanceIds()) ? new ArrayList<>() : dto.instanceIds().stream().map(id -> normalizeServiceInstanceId(id)).toList(),
				Utilities.isEmpty(dto.providerNames()) ? new ArrayList<>() : dto.providerNames().stream().map(n -> normalizeSystemName(n)).toList(),
				Utilities.isEmpty(dto.serviceDefinitionNames()) ? new ArrayList<>() : dto.serviceDefinitionNames().stream().map(sd -> nameNormalizer.normalize(sd)).toList(),
				Utilities.isEmpty(dto.versions()) ? new ArrayList<>() : dto.versions().stream().map(v -> versionNormalizer.normalize(v)).toList(),
				Utilities.isEmpty(dto.alivesAt()) ? "" : dto.alivesAt().trim(),
				Utilities.isEmpty(dto.metadataRequirementsList()) ? new ArrayList<>() : dto.metadataRequirementsList(),
				Utilities.isEmpty(dto.addressTypes()) ? new ArrayList<>() : dto.addressTypes().stream().map(a -> a.trim().toUpperCase()).toList(),
				Utilities.isEmpty(dto.interfaceTemplateNames()) ? new ArrayList<>() : dto.interfaceTemplateNames().stream().map(i -> nameNormalizer.normalize(i)).toList(),
				Utilities.isEmpty(dto.interfacePropertyRequirementsList()) ? new ArrayList<>() : dto.interfacePropertyRequirementsList(),
				Utilities.isEmpty(dto.policies()) ? new ArrayList<>() : dto.policies().stream().map(p -> p.trim().toUpperCase()).toList()
		);
	}

	//-------------------------------------------------------------------------------------------------
	public String normalizeSystemName(final String systemName) {
		logger.debug("normalizeSystemName started");
		Assert.isTrue(!Utilities.isEmpty(systemName), "systemName is empty");

		return nameNormalizer.normalize(systemName);
	}

	//-------------------------------------------------------------------------------------------------
	public String normalizeServiceInstanceId(final String instanceId) {
		logger.debug("normalizeServiceInstanceId started");
		Assert.isTrue(!Utilities.isEmpty(instanceId), "Service instance id is empty");

		return nameNormalizer.normalize(instanceId);
	}
}