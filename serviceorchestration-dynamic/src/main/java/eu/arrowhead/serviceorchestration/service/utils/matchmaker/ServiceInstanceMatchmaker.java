package eu.arrowhead.serviceorchestration.service.utils.matchmaker;

import java.util.List;

import eu.arrowhead.serviceorchestration.service.model.OrchestrationCandidate;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationForm;

public interface ServiceInstanceMatchmaker {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationCandidate doMatchmaking(final OrchestrationForm form, final List<OrchestrationCandidate> candidates);
}