package eu.arrowhead.serviceorchestration.service.utils;

import java.util.UUID;

import org.springframework.stereotype.Service;

import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.dto.OrchestrationResponseDTO;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationForm;

@Service
public class InterCloudServiceOrchestration {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationResponseDTO doInterCloudServiceOrchestration(final UUID jobId, final OrchestrationForm form) {
		// form.addFlag(OrchestrationFlag.MATCHMAKING);
		throw new ArrowheadException("Inter-cloud orchestration is not implemented");
	}

}
