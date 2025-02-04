package eu.arrowhead.serviceorchestration.jpa.service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.serviceorchestration.jpa.entity.Subscription;
import eu.arrowhead.serviceorchestration.jpa.repository.SubscriptionRepository;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationSubscription;

@Service
public class SubscriptionDbService {

	//=================================================================================================
	// members

	@Autowired
	private SubscriptionRepository subscriptionRepo;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public List<Subscription> create(final List<OrchestrationSubscription> candidates) {
		logger.debug("create started..");
		Assert.notNull(candidates, "subscription candidate list is null");

		try {
			final List<UUID> toRemove = new ArrayList<>();
			final List<Subscription> toSave = new ArrayList<>(candidates.size());
			for (final OrchestrationSubscription candidate : candidates) {
				final Optional<Subscription> optional = subscriptionRepo.findByOwnerSystemAndTargetSystemAndServiceDefinition(
						candidate.getOrchestrationForm().getRequesterSystemName(),
						candidate.getOrchestrationForm().getTargetSystemName(),
						candidate.getOrchestrationForm().getServiceDefinition());

				if (optional.isPresent()) {
					toRemove.add(optional.get().getId());
				}

				final ZonedDateTime expireAt = candidate.getDuration() == null ? null : Utilities.utcNow().plusSeconds(candidate.getDuration());
				toSave.add(new Subscription(
						UUID.randomUUID(),
						candidate.getOrchestrationForm().getRequesterSystemName(),
						candidate.getOrchestrationForm().getTargetSystemName(),
						expireAt,
						candidate.getNotifyProtocol(),
						Utilities.toJson(candidate.getNotifyProperties()),
						Utilities.toJson(candidate.getOrchestrationForm().extractOrchestrationRequestDTO())));
			}

			subscriptionRepo.deleteAllById(toRemove);
			final List<Subscription> saved = subscriptionRepo.saveAll(toSave);
			subscriptionRepo.flush();
			return saved;

		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public Optional<Subscription> get(final UUID id) {
		logger.debug("get started..");
		Assert.notNull(id, "subscription id is null");

		try {
			return subscriptionRepo.findById(id);

		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public Optional<Subscription> get(final String ownerSystem, final String targetSystem, final String serviceDefinition) {
		logger.debug("get started..");
		Assert.isTrue(!Utilities.isEmpty(ownerSystem), "ownerSystem is empty");
		Assert.isTrue(!Utilities.isEmpty(targetSystem), "targetSystem is empty");

		try {
			return subscriptionRepo.findByOwnerSystemAndTargetSystemAndServiceDefinition(ownerSystem, targetSystem, serviceDefinition);

		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public boolean deleteById(final UUID id) {
		logger.debug("deleteById started..");
		Assert.notNull(id, "subscription id is null");

		try {
			if (subscriptionRepo.existsById(id)) {
				subscriptionRepo.deleteById(id);
				return true;
			}
			return false;

		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

}
