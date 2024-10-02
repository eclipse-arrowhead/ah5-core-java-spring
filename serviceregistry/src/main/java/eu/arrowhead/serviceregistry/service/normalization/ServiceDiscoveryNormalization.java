package eu.arrowhead.serviceregistry.service.normalization;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.ServiceInstanceInterfaceRequestDTO;
import eu.arrowhead.dto.ServiceInstanceLookupRequestDTO;
import eu.arrowhead.dto.ServiceInstanceRequestDTO;
import eu.arrowhead.serviceregistry.service.validation.version.VersionNormalizer;

@Service
public class ServiceDiscoveryNormalization {

	//=================================================================================================
	// members

	@Autowired
	private VersionNormalizer versionNormalizer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public ServiceInstanceRequestDTO normalizeServiceInstanceRequestDTO(final ServiceInstanceRequestDTO dto) {
		logger.debug("normalizeServiceInstanceRequestDTO started");
		Assert.notNull(dto, "ServiceInstanceRequestDTO is null");

		return new ServiceInstanceRequestDTO(
				dto.systemName().trim(),
				dto.serviceDefinitionName().trim().toLowerCase(),
				versionNormalizer.normalize(dto.version()),
				Utilities.isEmpty(dto.expiresAt()) ? "" : dto.expiresAt().trim(),
				dto.metadata(),
				Utilities.isEmpty(dto.interfaces()) ? new ArrayList<>()
						: dto.interfaces()
								.stream()
								.map(i -> new ServiceInstanceInterfaceRequestDTO(
										i.templateName().trim().toUpperCase(),
										Utilities.isEmpty(i.protocol()) ? "" : i.protocol().trim().toLowerCase(),
										i.policy().trim().toUpperCase(),
										i.properties()))
								.toList());
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceInstanceLookupRequestDTO normalizeServiceInstanceLookupRequestDTO(final ServiceInstanceLookupRequestDTO dto) {
		logger.debug("normalizeServiceInstanceLookupRequestDTO started");
		Assert.notNull(dto, "ServiceInstanceLookupRequestDTO is null");

		return new ServiceInstanceLookupRequestDTO(
				Utilities.isEmpty(dto.instanceIds()) ? new ArrayList<>() : dto.instanceIds().stream().map(id -> id.trim()).toList(),
				Utilities.isEmpty(dto.providerNames()) ? new ArrayList<>() : dto.providerNames().stream().map(n -> n.trim()).toList(),
				Utilities.isEmpty(dto.serviceDefinitionNames()) ? new ArrayList<>() : dto.serviceDefinitionNames().stream().map(sd -> sd.trim()).toList(),
				Utilities.isEmpty(dto.versions()) ? new ArrayList<>() : dto.versions().stream().map(v -> versionNormalizer.normalize(v)).toList(),
				Utilities.isEmpty(dto.alivesAt()) ? "" : dto.alivesAt().trim(),
				Utilities.isEmpty(dto.metadataRequirementsList()) ? new ArrayList<>() : dto.metadataRequirementsList(),
				Utilities.isEmpty(dto.interfaceTemplateNames()) ? new ArrayList<>() : dto.interfaceTemplateNames().stream().map(i -> i.trim().toUpperCase()).toList(),
				Utilities.isEmpty(dto.interfacePropertyRequirementsList()) ? new ArrayList<>() : dto.interfacePropertyRequirementsList(),
				Utilities.isEmpty(dto.policies()) ? new ArrayList<>() : dto.policies().stream().map(p -> p.trim().toUpperCase()).toList()
		);
	}

	//-------------------------------------------------------------------------------------------------
	public String normalizeServiceInstanceId(final String instanceId) {
		logger.debug("normalizeServiceInstanceRequestDTO started");
		Assert.isTrue(!Utilities.isEmpty(instanceId), "Service instance id is empty");

		return instanceId.trim().toLowerCase();
	}
}
