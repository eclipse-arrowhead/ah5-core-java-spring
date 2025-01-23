package eu.arrowhead.serviceorchestration.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.dto.OrchestrationResponseDTO;
import eu.arrowhead.dto.enums.OrchestrationFlag;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationForm;
import eu.arrowhead.serviceorchestration.service.utils.InterCloudServiceOrchestration;
import eu.arrowhead.serviceorchestration.service.utils.LocalServiceOrchestration;

@Service
public class OrchestrationService {

	//=================================================================================================
	// members

	@Autowired
	private LocalServiceOrchestration localOrch;

	@Autowired
	private InterCloudServiceOrchestration interCloudOrch;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationResponseDTO pull(final OrchestrationForm form) {
		//TODO
		// from syntax validation (naming convetion és stb is)
		// form syntax normalization
		// form context validation (ONLY_Intercloud, but it is not enabled | no reservation in intercloud -> ez nem igaz!!! | ONLY_EXCLUSIVE and ONLY_INTERCLOUD cannot be together -> ez nem igaz!! | ONLY_INTERCLOUD can't be with ALLOW_TRANSLATION |
		//                          Has QoS requirements but it is not enabled | ha translation enabled akkor legyen operation is | ha only preferred akkor legyenek is | ha az összes preferred prvider local és ONLY_PREFERRED + ONLY_INTERCLOUD)

		return orchestrate(form);
	}

	//=================================================================================================
	// assistant methods

	private OrchestrationResponseDTO orchestrate(final OrchestrationForm form) {

		if (form.hasFlag(OrchestrationFlag.ONLY_INTERCLOUD)) {
			return interCloudOrch.doInterCloudServiceOrchestration(form);
		}

		return localOrch.doLocalServiceOrchestration(form);
	}
}
