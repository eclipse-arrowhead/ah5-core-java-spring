package eu.arrowhead.serviceorchestration.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.OrchestrationPushJobListResponseDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionListRequestDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionListResponseDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionRequestDTO;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationConstants;
import eu.arrowhead.serviceorchestration.jpa.entity.Subscription;
import eu.arrowhead.serviceorchestration.jpa.service.OrchestrationJobDbService;
import eu.arrowhead.serviceorchestration.jpa.service.SubscriptionDbService;
import eu.arrowhead.serviceorchestration.service.dto.DTOConverter;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationPushTrigger;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationSubscription;
import eu.arrowhead.serviceorchestration.service.validation.OrchestrationFromContextValidation;
import eu.arrowhead.serviceorchestration.service.validation.OrchestrationPushManagementValidation;
import jakarta.annotation.Resource;

@Service
public class OrchestrationPushManagementService {

	//=================================================================================================
	// members

	@Autowired
	private OrchestrationFromContextValidation formContextValidator;

	@Autowired
	private SubscriptionDbService subscriptionDbService;

	@Autowired
	private OrchestrationJobDbService orchJobDbService;

	@Autowired
	private DTOConverter dtoConverter;

	@Autowired
	private OrchestrationPushManagementValidation validator;

	@Resource(name = DynamicServiceOrchestrationConstants.JOB_QUEUE_PUSH_ORCHESTRATION)
	private BlockingQueue<UUID> pushOrchJobQueue;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationSubscriptionListResponseDTO pushSubscribe(final String requesterSystem, final OrchestrationSubscriptionListRequestDTO dto, final String origin) {
		logger.debug("pushSubscribe started...");

		final List<OrchestrationSubscription> subscriptions = new ArrayList<>();
		if (dto != null && !Utilities.isEmpty(dto.subscriptions())) {
			for (final OrchestrationSubscriptionRequestDTO subsReq : dto.subscriptions()) {
				subscriptions.add(new OrchestrationSubscription(requesterSystem, subsReq));
			}
		}

		validator.validateAndNormalizePushSubscribeService(subscriptions, origin);
		checkForDuplicateSubscriptionRequest(subscriptions, origin);
		subscriptions.forEach(s -> formContextValidator.validate(s.getOrchestrationForm(), origin));

		final List<Subscription> result = subscriptionDbService.create(subscriptions);
		return dtoConverter.convertSubscriptionListToDTO(result);
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationPushJobListResponseDTO pushTrigger(final OrchestrationPushTrigger trigger, final String origin) {
		logger.debug("pushTrigger started...");

		// TODO validate and normalize
		// TODO query subscriptions
		// TODO check if there is an already running for the same and return the existing job id
		// TODO create jobs from subscriptions
		// TODO save to job DB
		// TODO convert to DTO
		return null;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void checkForDuplicateSubscriptionRequest(final List<OrchestrationSubscription> subscriptions, final String origin) {
		logger.debug("checkForDuplicateSubscriptionRequest started...");

		final Set<String> keys = new HashSet<>(subscriptions.size());

		for (final OrchestrationSubscription subsReq : subscriptions) {
			final String key = subsReq.getOrchestrationForm().getRequesterSystemName() + "-" + subsReq.getOrchestrationForm().getTargetSystemName() + "-" + subsReq.getOrchestrationForm().getServiceDefinition();
			if (keys.contains(key)) {
				throw new InvalidParameterException("Duplicate subscription request for: " + key, origin);
			}
			keys.add(key);
		}
	}
}
