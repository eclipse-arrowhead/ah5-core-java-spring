package eu.arrowhead.serviceorchestration.service.model;

import eu.arrowhead.dto.OrchestrationRequestDTO;

public class OrchestrationFrom {

	//=================================================================================================
	// members

	private String requesterSystemName;
	private String targetSystemName;
	private String serviceDefinition;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationFrom(final String requesterSystemName, final String targetSystemName, final OrchestrationRequestDTO dto) {
		this.requesterSystemName = requesterSystemName;
		this.targetSystemName = targetSystemName;
		if (dto != null) {
			this.serviceDefinition = dto.serviceRequirement().serviceDefinition();
		}
	}
}
