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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.OrchestrationPushJobListResponseDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionListRequestDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionListResponseDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionRequestDTO;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationConstants;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationJob;
import eu.arrowhead.serviceorchestration.jpa.entity.Subscription;
import eu.arrowhead.serviceorchestration.jpa.service.OrchestrationJobDbService;
import eu.arrowhead.serviceorchestration.jpa.service.SubscriptionDbService;
import eu.arrowhead.serviceorchestration.service.dto.DTOConverter;
import eu.arrowhead.serviceorchestration.service.enums.OrchestrationJobStatus;
import eu.arrowhead.serviceorchestration.service.enums.OrchestrationType;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationJobFilter;
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

		List<Subscription> subscriptions;
		if (Utilities.isEmpty(trigger.getSubscriptionIds()) && Utilities.isEmpty(trigger.getTartgetSystems())) {
			subscriptions = subscriptionDbService.query(List.of(trigger.getRequesterSystem()), List.of(), List.of());
		} else if (!Utilities.isEmpty(trigger.getSubscriptionIds())) {
			subscriptions = subscriptionDbService.get(trigger.getSubscriptionIds().stream().map(id -> UUID.fromString(id)).toList());
		} else {
			subscriptions = subscriptionDbService.query(List.of(), trigger.getTartgetSystems(), List.of());
		}

		final List<OrchestrationJob> existingJobs = new ArrayList<>();
		final List<OrchestrationJob> newJobs = new ArrayList<>();
		for (final Subscription subscription : subscriptions) {
			final List<OrchestrationJob> possiblySameJob = orchJobDbService.query(new OrchestrationJobFilter(
					List.of(),
					List.of(OrchestrationJobStatus.PENDING, OrchestrationJobStatus.IN_PROGRESS),
					OrchestrationType.PUSH,
					List.of(),
					List.of(),
					null,
					List.of(subscription.getId().toString())),
					PageRequest.of(0, Integer.MAX_VALUE, Direction.DESC, OrchestrationJob.DEFAULT_SORT_FIELD)).toList();

			if (!Utilities.isEmpty(possiblySameJob)) {
				existingJobs.addAll(possiblySameJob);
			} else {
				newJobs.add(new OrchestrationJob(OrchestrationType.PUSH, trigger.getRequesterSystem(), subscription.getTargetSystem(), subscription.getServiceDefinition(), subscription.getId().toString()));
			}
		}

		final List<OrchestrationJob> saved = orchJobDbService.create(newJobs);
		existingJobs.addAll(saved);

		return dtoConverter.convertOrchestrationJobListToDTO(existingJobs);
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
