package eu.arrowhead.serviceorchestration.service.validation;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationPushTrigger;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationSubscription;
import eu.arrowhead.serviceorchestration.service.model.validation.OrchestrationPushTriggerValidation;
import eu.arrowhead.serviceorchestration.service.model.validation.OrchestrationSubscriptionValidation;

@Service
public class OrchestrationPushManagementValidation {

	//=================================================================================================
	// members

	@Autowired
	private OrchestrationSubscriptionValidation orchSubsValidator;

	@Autowired
	private OrchestrationPushTriggerValidation orchPushTriggerValidator;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//-------------------------------------------------------------------------------------------------
	// methods

	// VALIDATION

	//-------------------------------------------------------------------------------------------------
	public void validatePushSubscribeService(final String requesterSystem, final List<OrchestrationSubscription> subscription, final String origin) {
		logger.debug("validatePushSubscribeService started");

		if (Utilities.isEmpty(subscription)) {
			throw new InvalidParameterException("Subscription request list is empty.", origin);
		}

		for (final OrchestrationSubscription subs : subscription) {
			orchSubsValidator.validateOrchestrationSubscription(subs, origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void validatePushTriggerService(final OrchestrationPushTrigger trigger, final String origin) {
		logger.debug("validatePushTriggerService started");

		orchPushTriggerValidator.validateOrchestrationPushTrigger(trigger, origin);
	}

	// VALIDATION AND NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public void validateAndNormalizePushSubscribeService(final List<OrchestrationSubscription> subscription, final String origin) {
		logger.debug("validateAndNormalizePushSubscribeService started");

		if (Utilities.isEmpty(subscription)) {
			throw new InvalidParameterException("Subscription request list is empty.", origin);
		}

		for (final OrchestrationSubscription subs : subscription) {
			orchSubsValidator.validateAndNormalizeOrchestrationSubscription(subs, origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void validateAndNormalizePushTriggerService(final OrchestrationPushTrigger trigger, final String origin) {
		logger.debug("validateAndNormalizePushTriggerService started");

		orchPushTriggerValidator.validateAndNormalizeOrchestrationPushTrigger(trigger, origin);
	}
}
