package eu.arrowhead.serviceorchestration.service.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.OrchestrationRequestDTO;
import eu.arrowhead.dto.OrchestrationServiceRequirementDTO;
import eu.arrowhead.dto.enums.OrchestrationFlag;

public class OrchestrationForm {

	//=================================================================================================
	// members

	private String requesterSystemName;
	private String targetSystemName;
	private List<String> orchestrationFlags = new ArrayList<String>();
	private Map<String, String> qosRequirements;
	private Integer exclusivityDuration;

	// Service related
	private String serviceDefinition;
	private List<String> operations;
	private List<String> versions;
	private String alivesAt;
	private List<MetadataRequirementDTO> metadataRequirements;
	private List<String> interfaceTemplateNames;
	private List<String> interfaceAddressTypes;
	private List<MetadataRequirementDTO> interfacePropertyRequirements;
	private List<String> securityPolicies;
	private List<String> prefferedProviders;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationForm(final String requesterSystemName, final OrchestrationRequestDTO dto) {
		this(requesterSystemName, requesterSystemName, dto);
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationForm(final String requesterSystemName, final String targetSystemName, final OrchestrationRequestDTO dto) {
		this.requesterSystemName = requesterSystemName;
		this.targetSystemName = targetSystemName;

		if (dto != null) {
			this.orchestrationFlags.addAll(dto.orchestrationFlags() == null ? List.of() : dto.orchestrationFlags());
			this.qosRequirements = dto.qosRequirements();
			this.exclusivityDuration = dto.exclusivityDuration();

			if (dto.serviceRequirement() != null) {
				this.serviceDefinition = dto.serviceRequirement().serviceDefinition();
				this.operations = dto.serviceRequirement().operations();
				this.versions = dto.serviceRequirement().versions();
				this.alivesAt = dto.serviceRequirement().alivesAt();
				this.metadataRequirements = dto.serviceRequirement().metadataRequirements();
				this.interfaceTemplateNames = dto.serviceRequirement().interfaceTemplateNames();
				this.interfaceAddressTypes = dto.serviceRequirement().interfaceAddressTypes();
				this.interfacePropertyRequirements = dto.serviceRequirement().interfacePropertyRequirements();
				this.securityPolicies = dto.serviceRequirement().securityPolicies();
				this.prefferedProviders = dto.serviceRequirement().prefferedProviders();
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	public boolean hasFlag(final OrchestrationFlag flag) {
		for (final String rawFlag : orchestrationFlags) {
			if (rawFlag.equalsIgnoreCase(flag.name())) {
				return true;
			}
		}

		return false;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean addFlag(final OrchestrationFlag flag) {
		if (!hasFlag(flag)) {
			orchestrationFlags.add(flag.name());
			return true;
		}
		return false;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean exclusivityIsPreferred() {
		return exclusivityDuration != null;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean hasPreferredProviders() {
		return !Utilities.isEmpty(prefferedProviders);
	}

	//-------------------------------------------------------------------------------------------------
	public boolean hasQoSRequirements() {
		return !Utilities.isEmpty(qosRequirements);
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationRequestDTO extractOrchestrationRequestDTO() {
		final OrchestrationServiceRequirementDTO serviceReq = new OrchestrationServiceRequirementDTO(serviceDefinition, operations, versions, alivesAt, metadataRequirements, interfaceTemplateNames, interfaceAddressTypes,
				interfacePropertyRequirements, securityPolicies, prefferedProviders);
		return new OrchestrationRequestDTO(serviceReq, orchestrationFlags, qosRequirements, exclusivityDuration);
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public String getRequesterSystemName() {
		return requesterSystemName;
	}

	//-------------------------------------------------------------------------------------------------
	public String getTargetSystemName() {
		return targetSystemName;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> getOrchestrationFlags() {
		return orchestrationFlags;
	}

	//-------------------------------------------------------------------------------------------------
	public Map<String, String> getQosRequirements() {
		return qosRequirements;
	}

	//-------------------------------------------------------------------------------------------------
	public String getServiceDefinition() {
		return serviceDefinition;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> getOperations() {
		return operations;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> getVersions() {
		return versions;
	}

	//-------------------------------------------------------------------------------------------------
	public String getAlivesAt() {
		return alivesAt;
	}

	//-------------------------------------------------------------------------------------------------
	public Integer getExclusivityDuration() {
		return exclusivityDuration;
	}

	//-------------------------------------------------------------------------------------------------
	public List<MetadataRequirementDTO> getMetadataRequirements() {
		return metadataRequirements;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> getInterfaceTemplateNames() {
		return interfaceTemplateNames;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> getInterfaceAddressTypes() {
		return interfaceAddressTypes;
	}

	//-------------------------------------------------------------------------------------------------
	public List<MetadataRequirementDTO> getInterfacePropertyRequirements() {
		return interfacePropertyRequirements;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> getSecurityPolicies() {
		return securityPolicies;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> getPrefferedProviders() {
		return prefferedProviders;
	}

	//-------------------------------------------------------------------------------------------------
	public void setRequesterSystemName(final String requesterSystemName) {
		this.requesterSystemName = requesterSystemName;
	}

	//-------------------------------------------------------------------------------------------------
	public void setTargetSystemName(final String targetSystemName) {
		this.targetSystemName = targetSystemName;
	}

	//-------------------------------------------------------------------------------------------------
	public void setQosRequirements(final Map<String, String> qosRequirements) {
		this.qosRequirements = qosRequirements;
	}

	//-------------------------------------------------------------------------------------------------
	public void setExclusivityDuration(final Integer exclusivityDuration) {
		this.exclusivityDuration = exclusivityDuration;
	}

	//-------------------------------------------------------------------------------------------------
	public void setServiceDefinition(final String serviceDefinition) {
		this.serviceDefinition = serviceDefinition;
	}

	//-------------------------------------------------------------------------------------------------
	public void setOperations(final List<String> operations) {
		this.operations = operations;
	}

	//-------------------------------------------------------------------------------------------------
	public void setOrchestrationFlags(final List<String> orchestrationFlags) {
		this.orchestrationFlags = orchestrationFlags;
	}

	//-------------------------------------------------------------------------------------------------
	public void setVersions(final List<String> versions) {
		this.versions = versions;
	}

	//-------------------------------------------------------------------------------------------------
	public void setAlivesAt(final String alivesAt) {
		this.alivesAt = alivesAt;
	}

	//-------------------------------------------------------------------------------------------------
	public void setMetadataRequirements(final List<MetadataRequirementDTO> metadataRequirements) {
		this.metadataRequirements = metadataRequirements;
	}

	//-------------------------------------------------------------------------------------------------
	public void setInterfaceTemplateNames(final List<String> interfaceTemplateNames) {
		this.interfaceTemplateNames = interfaceTemplateNames;
	}

	//-------------------------------------------------------------------------------------------------
	public void setInterfaceAddressTypes(final List<String> interfaceAddressTypes) {
		this.interfaceAddressTypes = interfaceAddressTypes;
	}

	//-------------------------------------------------------------------------------------------------
	public void setInterfacePropertyRequirements(final List<MetadataRequirementDTO> interfacePropertyRequirements) {
		this.interfacePropertyRequirements = interfacePropertyRequirements;
	}

	//-------------------------------------------------------------------------------------------------
	public void setSecurityPolicies(final List<String> securityPolicies) {
		this.securityPolicies = securityPolicies;
	}

	//-------------------------------------------------------------------------------------------------
	public void setPrefferedProviders(final List<String> prefferedProviders) {
		this.prefferedProviders = prefferedProviders;
	}

}
