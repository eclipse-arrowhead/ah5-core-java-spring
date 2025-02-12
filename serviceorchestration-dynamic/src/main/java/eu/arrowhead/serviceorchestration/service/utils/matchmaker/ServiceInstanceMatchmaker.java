package eu.arrowhead.serviceorchestration.service.utils.matchmaker;

import java.util.List;

import eu.arrowhead.serviceorchestration.service.model.OrchestrationCandidate;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationForm;

public interface ServiceInstanceMatchmaker {

	OrchestrationCandidate doMatchmaking(final OrchestrationForm form, List<OrchestrationCandidate> candidates);
}
