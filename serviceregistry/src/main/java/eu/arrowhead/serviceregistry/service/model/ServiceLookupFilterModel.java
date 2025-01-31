package eu.arrowhead.serviceregistry.service.model;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.ServiceInstanceLookupRequestDTO;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;

public class ServiceLookupFilterModel {

	//=================================================================================================
	// members

	private final Set<String> instanceIds = new HashSet<>();
	private final Set<String> providerNames = new HashSet<>();
	private final Set<String> serviceDefinitionNames = new HashSet<>();
	private final Set<String> versions = new HashSet<>();
	private final ZonedDateTime alivesAt;
	private final List<MetadataRequirementDTO> metadataRequirementsList = new ArrayList<>();
	private final Set<String> interfaceTemplateNames = new HashSet<>();
	private final List<MetadataRequirementDTO> interfacePropertyRequirementsList = new ArrayList<>();
	private final Set<ServiceInterfacePolicy> policies = new HashSet<>();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public ServiceLookupFilterModel(final ServiceInstanceLookupRequestDTO dto) {
		Assert.notNull(dto, "ServiceInstanceLookupRequestDTO is null");

		if (!Utilities.isEmpty(dto.instanceIds())) {
		    instanceIds.addAll(dto.instanceIds());
		}
		
		if (!Utilities.isEmpty(dto.providerNames())) {
			providerNames.addAll(dto.providerNames());
		}
		
		if (!Utilities.isEmpty(dto.serviceDefinitionNames())) {
			serviceDefinitionNames.addAll(dto.serviceDefinitionNames());
		}
		
		if (!Utilities.isEmpty(dto.versions())) {
			versions.addAll(dto.versions());
		}
		
		alivesAt = Utilities.parseUTCStringToZonedDateTime(dto.alivesAt());
		if (!Utilities.isEmpty(dto.metadataRequirementsList())) {
			metadataRequirementsList.addAll(dto.metadataRequirementsList());
		}
		
		if (!Utilities.isEmpty(dto.interfaceTemplateNames())) {
			interfaceTemplateNames.addAll(dto.interfaceTemplateNames());
		}
		
		if (!Utilities.isEmpty(dto.interfacePropertyRequirementsList())) {
			interfacePropertyRequirementsList.addAll(dto.interfacePropertyRequirementsList());
		}
		
		if (!Utilities.isEmpty(dto.policies())) {
			policies.addAll(dto.policies().stream().map(p -> ServiceInterfacePolicy.valueOf(p)).collect(Collectors.toSet()));
		}
	}

	//-------------------------------------------------------------------------------------------------
	public boolean hasFilters() {
		return !Utilities.isEmpty(instanceIds)
				|| !Utilities.isEmpty(providerNames)
				|| !Utilities.isEmpty(serviceDefinitionNames)
				|| !Utilities.isEmpty(versions)
				|| alivesAt != null
				|| !Utilities.isEmpty(metadataRequirementsList)
				|| !Utilities.isEmpty(interfaceTemplateNames)
				|| !Utilities.isEmpty(interfacePropertyRequirementsList)
				|| !Utilities.isEmpty(policies);
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public Set<String> getInstanceIds() {
		return instanceIds;
	}

	//-------------------------------------------------------------------------------------------------
	public Set<String> getProviderNames() {
		return providerNames;
	}

	//-------------------------------------------------------------------------------------------------
	public Set<String> getServiceDefinitionNames() {
		return serviceDefinitionNames;
	}

	//-------------------------------------------------------------------------------------------------
	public Set<String> getVersions() {
		return versions;
	}

	//-------------------------------------------------------------------------------------------------
	public ZonedDateTime getAlivesAt() {
		return alivesAt;
	}

	//-------------------------------------------------------------------------------------------------
	public List<MetadataRequirementDTO> getMetadataRequirementsList() {
		return metadataRequirementsList;
	}

	//-------------------------------------------------------------------------------------------------
	public Set<String> getInterfaceTemplateNames() {
		return interfaceTemplateNames;
	}

	//-------------------------------------------------------------------------------------------------
	public List<MetadataRequirementDTO> getInterfacePropertyRequirementsList() {
		return interfacePropertyRequirementsList;
	}

	//-------------------------------------------------------------------------------------------------
	public Set<ServiceInterfacePolicy> getPolicies() {
		return policies;
	}
}