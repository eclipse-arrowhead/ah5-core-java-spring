package eu.arrowhead.serviceorchestration.service;

import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.dto.OrchestrationResponseDTO;
import eu.arrowhead.dto.enums.OrchestrationFlag;
import eu.arrowhead.serviceorchestration.jpa.entity.Subscription;
import eu.arrowhead.serviceorchestration.jpa.service.SubscriptionDbService;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationForm;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationSubscription;
import eu.arrowhead.serviceorchestration.service.utils.InterCloudServiceOrchestration;
import eu.arrowhead.serviceorchestration.service.utils.LocalServiceOrchestration;
import eu.arrowhead.serviceorchestration.service.validation.OrchestrationValidation;

@Service
public class OrchestrationService {

	//=================================================================================================
	// members

	@Autowired
	private LocalServiceOrchestration localOrch;

	@Autowired
	private InterCloudServiceOrchestration interCloudOrch;

	@Autowired
	private SubscriptionDbService subscriptionDbService;

	@Autowired
	private OrchestrationValidation validator;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationResponseDTO pull(final OrchestrationForm form, final String origin) {
		logger.debug("pull started...");

		validator.validateAndNormalizePullService(form, origin);
		validator.validateNormalizedOrchestrationFormContext(form, origin);

		return orchestrate(form);
	}

	//-------------------------------------------------------------------------------------------------
	public Pair<Boolean, String> pushSubscribe(final OrchestrationSubscription subscription, final String origin) {
		logger.debug("pushSubscribe started...");

		validator.validateAndNormalizePushSubscribeService(subscription, origin);
		validator.validateNormalizedOrchestrationFormContext(subscription.getOrchestrationForm(), origin);

		final Optional<Subscription> recordOpt = subscriptionDbService.get(
				subscription.getOrchestrationForm().getRequesterSystemName(),
				subscription.getOrchestrationForm().getTargetSystemName(),
				subscription.getOrchestrationForm().getServiceDefinition());

		boolean isOverride = false;
		if (recordOpt.isPresent()) {
			subscriptionDbService.deleteById(recordOpt.get().getId());
			isOverride = true;
		}

		return Pair.of(isOverride, subscriptionDbService.create(subscription).toString());
	}

	//-------------------------------------------------------------------------------------------------
	public void pushUnsubscribe(final String requesterSystem, final String subscriptionId, final String origin) {
		// TODO make sure that the requester sys is the same who made the subscription
	}

	//=================================================================================================
	// assistant methods

	private OrchestrationResponseDTO orchestrate(final OrchestrationForm form) {
		logger.debug("orchestrate started...");

		if (form.hasFlag(OrchestrationFlag.ONLY_INTERCLOUD)) {
			return interCloudOrch.doInterCloudServiceOrchestration(form);
		}

		return localOrch.doLocalServiceOrchestration(form);
	}
}
