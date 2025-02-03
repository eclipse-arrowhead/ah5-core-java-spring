package eu.arrowhead.serviceorchestration.service;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseBody;

import eu.arrowhead.dto.OrchestrationSubscriptionListRequestDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionListResponseDTO;
import eu.arrowhead.serviceorchestration.jpa.entity.Subscription;
import eu.arrowhead.serviceorchestration.jpa.service.SubscriptionDbService;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationPushTrigger;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationSubscription;
import eu.arrowhead.serviceorchestration.service.validation.OrchestrationFromContextValidation;

@Service
public class OrchestrationPushManagementService {

	//=================================================================================================
	// members

	@Autowired
	private OrchestrationFromContextValidation formContextValidator;

	@Autowired
	private SubscriptionDbService subscriptionDbService;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public @ResponseBody OrchestrationSubscriptionListResponseDTO pushSubscribe(final String requesterSystem, final OrchestrationSubscriptionListRequestDTO dto, final String origin) {
		logger.debug("pushSubscribe started...");

		// TODO validate (check duplicates too) and normalize
		final Pair<String, List<OrchestrationSubscriptionListRequestDTO>> normalized = null;
		final List<OrchestrationSubscription> subscriptions = dto.subscriptions().stream().map(subscriptionDTO -> new OrchestrationSubscription(requesterSystem, subscriptionDTO)).toList();
		subscriptions.forEach(s -> formContextValidator.validate(s.getOrchestrationForm(), origin));

		// TODO delete existing

		final List<Subscription> result = subscriptionDbService.create(subscriptions);

		// TODO return converted to DTO
		return null;
	}

	//-------------------------------------------------------------------------------------------------
	public void pushTrigger(final OrchestrationPushTrigger trigger, final String origin) {
		// TODO
	}
}
