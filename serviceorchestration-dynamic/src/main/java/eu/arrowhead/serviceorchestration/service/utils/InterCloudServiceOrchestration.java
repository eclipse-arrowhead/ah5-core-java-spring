package eu.arrowhead.serviceorchestration.service.utils;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import eu.arrowhead.dto.OrchestrationResponseDTO;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationForm;

@Service
public class InterCloudServiceOrchestration {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationResponseDTO doInterCloudServiceOrchestration(final UUID jobId, final OrchestrationForm form) {
		// form.addFlag(OrchestrationFlag.MATCHMAKING);
		// TODO implement when Gatekeeper & Gateway are ready.
		logger.warn("Inter-cloud orchestration is not supported yet");
		return new OrchestrationResponseDTO();
	}

}
