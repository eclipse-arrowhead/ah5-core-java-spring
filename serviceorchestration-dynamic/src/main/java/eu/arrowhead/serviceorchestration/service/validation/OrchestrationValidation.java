package eu.arrowhead.serviceorchestration.service.validation;

import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationForm;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationSubscription;
import eu.arrowhead.serviceorchestration.service.model.validation.OrchestrationFormValidation;
import eu.arrowhead.serviceorchestration.service.model.validation.OrchestrationSubscriptionValidation;
import eu.arrowhead.serviceorchestration.service.normalization.OrchestrationServiceNormalization;

@Service
public class OrchestrationValidation {

	//=================================================================================================
	// members

	@Autowired
	private OrchestrationServiceNormalization normalization;

	@Autowired
	private OrchestrationFormValidation orchFormValidator;

	@Autowired
	private OrchestrationSubscriptionValidation orchSubsValidator;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	// VALIDATION

	//-------------------------------------------------------------------------------------------------
	public void validatePullService(final OrchestrationForm form, final String origin) {
		logger.debug("validatePull started...");

		orchFormValidator.validateOrchestrationForm(form, origin);
	}

	//-------------------------------------------------------------------------------------------------
	public void validatePushSubscribeService(final OrchestrationSubscription subscription, final String origin) {
		logger.debug("validatePushSubscribeService started...");

		orchSubsValidator.validateOrchestrationSubscription(subscription, origin);
	}

	//-------------------------------------------------------------------------------------------------
	public void validatePushUnsubscribeService(final String requesterSystem, final String subscriptionId, final String origin) {
		logger.debug("validatePushUnsubscribeService started...");

		if (Utilities.isEmpty(requesterSystem)) {
			throw new InvalidParameterException("Requester system is missing.", origin);
		}

		if (Utilities.isEmpty(subscriptionId)) {
			throw new InvalidParameterException("Subscription id system is missing.", origin);
		}
	}

	// VALIDATION AND NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public void validateAndNormalizePullService(final OrchestrationForm form, final String origin) {
		logger.debug("validatePull started...");

		orchFormValidator.validateAndNormalizeOrchestrationForm(form, origin);
	}

	//-------------------------------------------------------------------------------------------------
	public void validateAndNormalizePushSubscribeService(final OrchestrationSubscription subscription, final String origin) {
		logger.debug("validateAndNormalizePushSubscribeService started...");

		orchSubsValidator.validateAndNormalizeOrchestrationSubscription(subscription, origin);
	}

	//-------------------------------------------------------------------------------------------------
	public Pair<String, UUID> validateAndNormalizePushUnsubscribeService(final String requesterSystem, final String subscriptionId, final String origin) {
		logger.debug("validateAndNormalizePushUnsubscribeService started...");

		validatePushUnsubscribeService(requesterSystem, subscriptionId, origin);
		final Pair<String, String> normalized = normalization.normalizePushUnsubscribe(requesterSystem, subscriptionId);

		if (!Utilities.isUUID(normalized.getRight())) {
			throw new InvalidParameterException("Invalid subscription id.", origin);
		}

		return Pair.of(normalized.getLeft(), UUID.fromString(normalized.getRight()));

	}
}
