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

	private Set<String> instanceIds = new HashSet<>();
	private Set<String> providerNames = new HashSet<>();
	private Set<String> serviceDefinitionNames = new HashSet<>();
	private Set<String> versions = new HashSet<>();
	private ZonedDateTime alivesAt = null;
	private List<MetadataRequirementDTO> metadataRequirementsList = new ArrayList<>();
	private Set<String> interfaceTemplateNames = new HashSet<>();
	private List<MetadataRequirementDTO> interfacePropertyRequirementsList = new ArrayList<>();
	private Set<ServiceInterfacePolicy> policies = new HashSet<>();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public ServiceLookupFilterModel(final ServiceInstanceLookupRequestDTO dto) {
		Assert.notNull(dto, "ServiceInstanceLookupRequestDTO is null");

		instanceIds = Utilities.isEmpty(dto.instanceIds()) ? new HashSet<>() : dto.instanceIds().stream().map(id -> id).collect(Collectors.toSet());
		providerNames = Utilities.isEmpty(dto.providerNames()) ? new HashSet<>() : dto.providerNames().stream().map(pn -> pn).collect(Collectors.toSet());
		serviceDefinitionNames = Utilities.isEmpty(dto.serviceDefinitionNames()) ? new HashSet<>() : dto.serviceDefinitionNames().stream().map(sd -> sd).collect(Collectors.toSet());
		versions = Utilities.isEmpty(dto.versions()) ? new HashSet<>() : dto.versions().stream().map(v -> v).collect(Collectors.toSet());
		alivesAt = Utilities.isEmpty(dto.alivesAt()) ? null : Utilities.parseUTCStringToZonedDateTime(dto.alivesAt());
		metadataRequirementsList = Utilities.isEmpty(dto.metadataRequirementsList()) ? new ArrayList<>() : dto.metadataRequirementsList().stream().map(mr -> mr).collect(Collectors.toList());
		interfaceTemplateNames = Utilities.isEmpty(dto.interfaceTemplateNames()) ? new HashSet<>() : dto.interfaceTemplateNames().stream().map(it -> it).collect(Collectors.toSet());
		interfacePropertyRequirementsList = Utilities.isEmpty(dto.interfacePropertyRequirementsList()) ? new ArrayList<>() : dto.interfacePropertyRequirementsList().stream().map(ipr -> ipr).collect(Collectors.toList());
		policies = Utilities.isEmpty(dto.policies()) ? new HashSet<>() : dto.policies().stream().map(p -> ServiceInterfacePolicy.valueOf(p)).collect(Collectors.toSet());
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
	public void setInstanceIds(final Set<String> instanceIds) {
		this.instanceIds = instanceIds;
	}

	//-------------------------------------------------------------------------------------------------
	public Set<String> getProviderNames() {
		return providerNames;
	}

	//-------------------------------------------------------------------------------------------------
	public void setProviderNames(final Set<String> providerNames) {
		this.providerNames = providerNames;
	}

	//-------------------------------------------------------------------------------------------------
	public Set<String> getServiceDefinitionNames() {
		return serviceDefinitionNames;
	}

	//-------------------------------------------------------------------------------------------------
	public void setServiceDefinitionNames(final Set<String> serviceDefinitionNames) {
		this.serviceDefinitionNames = serviceDefinitionNames;
	}

	//-------------------------------------------------------------------------------------------------
	public Set<String> getVersions() {
		return versions;
	}

	//-------------------------------------------------------------------------------------------------
	public void setVersions(final Set<String> versions) {
		this.versions = versions;
	}

	//-------------------------------------------------------------------------------------------------
	public ZonedDateTime getAlivesAt() {
		return alivesAt;
	}

	//-------------------------------------------------------------------------------------------------
	public void setAlivesAt(final ZonedDateTime alivesAt) {
		this.alivesAt = alivesAt;
	}

	//-------------------------------------------------------------------------------------------------
	public List<MetadataRequirementDTO> getMetadataRequirementsList() {
		return metadataRequirementsList;
	}

	//-------------------------------------------------------------------------------------------------
	public void setMetadataRequirementsList(final List<MetadataRequirementDTO> metadataRequirementsList) {
		this.metadataRequirementsList = metadataRequirementsList;
	}

	//-------------------------------------------------------------------------------------------------
	public Set<String> getInterfaceTemplateNames() {
		return interfaceTemplateNames;
	}

	//-------------------------------------------------------------------------------------------------
	public void setInterfaceTemplateNames(final Set<String> interfaceTemplateNames) {
		this.interfaceTemplateNames = interfaceTemplateNames;
	}

	//-------------------------------------------------------------------------------------------------
	public List<MetadataRequirementDTO> getInterfacePropertyRequirementsList() {
		return interfacePropertyRequirementsList;
	}

	//-------------------------------------------------------------------------------------------------
	public void setInterfacePropertyRequirementsList(final List<MetadataRequirementDTO> interfacePropertyRequirementsList) {
		this.interfacePropertyRequirementsList = interfacePropertyRequirementsList;
	}

	//-------------------------------------------------------------------------------------------------
	public Set<ServiceInterfacePolicy> getPolicies() {
		return policies;
	}

	//-------------------------------------------------------------------------------------------------
	public void setPolicies(final Set<ServiceInterfacePolicy> policies) {
		this.policies = policies;
	}
}
