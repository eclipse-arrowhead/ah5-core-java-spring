package eu.arrowhead.serviceorchestration.service;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.dto.OrchestrationSubscriptionListRequestDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionListResponseDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionRequestDTO;
import eu.arrowhead.serviceorchestration.jpa.entity.Subscription;
import eu.arrowhead.serviceorchestration.jpa.service.SubscriptionDbService;
import eu.arrowhead.serviceorchestration.service.dto.DTOConverter;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationPushTrigger;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationSubscription;
import eu.arrowhead.serviceorchestration.service.validation.OrchestrationFromContextValidation;
import eu.arrowhead.serviceorchestration.service.validation.OrchestrationPushManagementValidation;

@Service
public class OrchestrationPushManagementService {

	//=================================================================================================
	// members

	@Autowired
	private OrchestrationFromContextValidation formContextValidator;

	@Autowired
	private SubscriptionDbService subscriptionDbService;

	@Autowired
	private DTOConverter dtoConverter;

	@Autowired
	private OrchestrationPushManagementValidation validation;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationSubscriptionListResponseDTO pushSubscribe(final String requesterSystem, final OrchestrationSubscriptionListRequestDTO dto, final String origin) {
		logger.debug("pushSubscribe started...");

		final Pair<String, List<OrchestrationSubscriptionRequestDTO>> normalized = validation.validateAndNormalizePushSubscribeService(requesterSystem, dto, origin);
		final List<OrchestrationSubscription> subscriptions = normalized.getRight().stream().map(subscriptionDTO -> new OrchestrationSubscription(normalized.getLeft(), subscriptionDTO)).toList();
		subscriptions.forEach(s -> formContextValidator.validate(s.getOrchestrationForm(), origin));

		final List<Subscription> result = subscriptionDbService.create(subscriptions);
		return dtoConverter.convertSubscriptionListToDTO(result);
	}

	//-------------------------------------------------------------------------------------------------
	public void pushTrigger(final OrchestrationPushTrigger trigger, final String origin) {
		// TODO
	}
}
