package eu.arrowhead.serviceorchestration.service.validation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.enums.OrchestrationFlag;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationSystemInfo;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationForm;

@Service
public class OrchestrationFromContextValidation {

	//=================================================================================================
	// members

	@Autowired
	private DynamicServiceOrchestrationSystemInfo sysInfo;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//-------------------------------------------------------------------------------------------------
	// methods

	//-------------------------------------------------------------------------------------------------
	public void validate(final OrchestrationForm form, final String origin) {
		logger.debug("validateNormalizedOrchestrationFormContext started...");

		if (form.hasFlag(OrchestrationFlag.ONLY_INTERCLOUD) && !sysInfo.isInterCloudEnabled()) {
			throw new InvalidParameterException("ONLY_INTERCLOUD flag is present, but intercloud orchestration is not enabled.", origin);
		}

		if (form.hasFlag(OrchestrationFlag.ONLY_INTERCLOUD) && form.hasFlag(OrchestrationFlag.ALLOW_TRANSLATION)) {
			// Inter-cloud translation is not supported
			throw new InvalidParameterException("ONLY_INTERCLOUD and ALLOW_TRANSLATION flags cannot be present at the same time.", origin);
		}

		if (form.hasFlag(OrchestrationFlag.ONLY_INTERCLOUD) && !Utilities.isEmpty(form.getOperations())) {
			// The creation of inter-cloud bridges is limited to the actual operations that the requester wants to use.
			throw new InvalidParameterException("Operations must be defined, when only inter-cloud orchestration is required.", origin);
		}

		if (form.hasFlag(OrchestrationFlag.ALLOW_INTERCLOUD) && !Utilities.isEmpty(form.getOperations())) {
			// The creation of inter-cloud bridges is limited to the actual operations that the requester wants to use.
			throw new InvalidParameterException("Operations must be defined, when inter-cloud orchestration is allowed.", origin);
		}

		if (form.hasFlag(OrchestrationFlag.ALLOW_TRANSLATION) && !Utilities.isEmpty(form.getOperations())) {
			// The creation of translation bridges is limited to the actual operations that the requester wants to use.
			throw new InvalidParameterException("Operations must be defined, when translation is allowed", origin);
		}

		if (form.hasFlag(OrchestrationFlag.ONLY_PREFERRED) && !form.hasPreferredProviders()) {
			throw new InvalidParameterException("ONLY_PREFERRED falg is present, but no preferred provider is defined.", origin);
		}

		if (form.hasQoSRequirements() && !sysInfo.isQoSEnabled()) {
			throw new InvalidParameterException("QoS requirements are present, but QoS support is not enabled.", origin);
		}
	}
}
