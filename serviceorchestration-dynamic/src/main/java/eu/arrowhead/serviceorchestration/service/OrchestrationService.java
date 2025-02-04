package eu.arrowhead.serviceorchestration.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.dto.OrchestrationResponseDTO;
import eu.arrowhead.dto.enums.OrchestrationFlag;
import eu.arrowhead.serviceorchestration.jpa.entity.Subscription;
import eu.arrowhead.serviceorchestration.jpa.service.SubscriptionDbService;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationForm;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationSubscription;
import eu.arrowhead.serviceorchestration.service.utils.InterCloudServiceOrchestration;
import eu.arrowhead.serviceorchestration.service.utils.LocalServiceOrchestration;
import eu.arrowhead.serviceorchestration.service.validation.OrchestrationFromContextValidation;
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

	@Autowired
	private OrchestrationFromContextValidation formContextValidator;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationResponseDTO pull(final OrchestrationForm form, final String origin) {
		logger.debug("pull started...");

		validator.validateAndNormalizePullService(form, origin);
		formContextValidator.validate(form, origin);

		return orchestrate(form);
	}

	//-------------------------------------------------------------------------------------------------
	public Pair<Boolean, String> pushSubscribe(final OrchestrationSubscription subscription, final String origin) {
		logger.debug("pushSubscribe started...");

		validator.validateAndNormalizePushSubscribeService(subscription, origin);
		formContextValidator.validate(subscription.getOrchestrationForm(), origin);

		final Optional<Subscription> recordOpt = subscriptionDbService.get(
				subscription.getOrchestrationForm().getRequesterSystemName(),
				subscription.getOrchestrationForm().getTargetSystemName(),
				subscription.getOrchestrationForm().getServiceDefinition());

		final boolean isOverride = recordOpt.isPresent();

		final List<Subscription> result = subscriptionDbService.create(List.of(subscription));
		return Pair.of(isOverride, result.getFirst().getId().toString());
	}

	//-------------------------------------------------------------------------------------------------
	public boolean pushUnsubscribe(final String requesterSystem, final String subscriptionId, final String origin) {
		logger.debug("pushUnsubscribe started...");

		final Pair<String, UUID> normalized = validator.validateAndNormalizePushUnsubscribeService(requesterSystem, subscriptionId, origin);

		final Optional<Subscription> recordOpt = subscriptionDbService.get(normalized.getRight());
		if (recordOpt.isPresent()) {
			if (!recordOpt.get().getOwnerSystem().equals(normalized.getLeft())) {
				throw new ForbiddenException(requesterSystem + " is not the subscription owner.", origin);
			}
			subscriptionDbService.deleteById(normalized.getRight());
			return true;
		}
		return false;
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
