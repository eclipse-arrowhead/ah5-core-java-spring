package eu.arrowhead.serviceorchestration.service.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.OrchestrationRequestDTO;
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
	public OrchestrationForm(final String requesterSystemName, final String targetSystemName, final OrchestrationRequestDTO dto) {
		this.requesterSystemName = requesterSystemName;
		this.targetSystemName = targetSystemName;

		if (dto != null) {
			this.orchestrationFlags.addAll(dto.orchestrationFlags());
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
	public void addFlag(final OrchestrationFlag flag) {
		if (!hasFlag(flag)) {
			orchestrationFlags.add(flag.name());
		}
	}

	//-------------------------------------------------------------------------------------------------
	public boolean exclusivityIsPreferred() {
		return exclusivityDuration != null;
	}

	//-------------------------------------------------------------------------------------------------
	public Integer getExclusivityDuration() {
		return exclusivityDuration;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean hasPreferredProviders() {
		return !Utilities.isEmpty(prefferedProviders);
	}
}
