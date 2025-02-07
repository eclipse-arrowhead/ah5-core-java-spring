package eu.arrowhead.serviceorchestration.service.model.validation;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.NameNormalizer;
import eu.arrowhead.common.service.validation.name.NameValidator;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationPushTrigger;

@Service
public class OrchestrationPushTriggerValidation {

	//=================================================================================================
	// members

	@Autowired
	private NameValidator nameValidator;

	@Autowired
	private NameNormalizer nameNormalizer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public void validateOrchestrationPushTrigger(final OrchestrationPushTrigger trigger, final String origin) {
		logger.debug("validateOrchestrationPushTrigger started...");

		if (trigger == null) {
			throw new InvalidParameterException("Request payload is missing.", origin);
		}

		if (Utilities.isEmpty(trigger.getRequesterSystem())) {
			throw new InvalidParameterException("Requester system is missing.", origin);
		}

		if (!Utilities.isEmpty(trigger.getTartgetSystems()) && Utilities.containsNullOrEmpty(trigger.getTartgetSystems())) {
			throw new InvalidParameterException("Target system list contains empty element.", origin);
		}

		if (!Utilities.isEmpty(trigger.getSubscriptionIds()) && Utilities.containsNullOrEmpty(trigger.getSubscriptionIds())) {
			throw new InvalidParameterException("Subscription id list contains empty element.", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void validateAndNormalizeOrchestrationPushTrigger(final OrchestrationPushTrigger trigger, final String origin) {
		logger.debug("validateAndNormalizeOrchestrationPushTrigger started...");

		validateOrchestrationPushTrigger(trigger, origin);

		trigger.setRequesterSystem(nameNormalizer.normalize(trigger.getRequesterSystem()));

		if (!Utilities.isEmpty(trigger.getTartgetSystems())) {
			trigger.setTartgetSystems(trigger.getTartgetSystems().stream().map(sys -> {
				final String normalized = nameNormalizer.normalize(sys);
				nameValidator.validateName(normalized);
				return normalized;
			}).toList());
		}

		if (!Utilities.isEmpty(trigger.getSubscriptionIds())) {
			trigger.setSubscriptionIds(trigger.getSubscriptionIds().stream().map(id -> {
				try {
					final String normalized = UUID.fromString(id.trim()).toString();
					return normalized;
				} catch (final IllegalArgumentException ex) {
					throw new InvalidParameterException("Invalid subscription id: " + id, origin);
				}
			}).toList());
		}
	}
}
