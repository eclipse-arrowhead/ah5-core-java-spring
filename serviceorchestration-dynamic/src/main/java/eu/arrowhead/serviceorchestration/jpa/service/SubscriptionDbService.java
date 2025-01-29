package eu.arrowhead.serviceorchestration.jpa.service;

import java.time.ZonedDateTime;
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
	public UUID create(final OrchestrationSubscription candidate) {
		logger.debug("create started..");
		Assert.notNull(candidate, "subscription candidate is null");
		Assert.isTrue(!subscriptionRepo.existsById(candidate.getId()), "subscription id already exist");

		try {
			final ZonedDateTime expireAt = candidate.getDuration() == null ? null : Utilities.utcNow().plusSeconds(candidate.getDuration());

			final Subscription saved = subscriptionRepo.saveAndFlush(new Subscription(
					candidate.getId(),
					candidate.getOrchestrationForm().getRequesterSystemName(),
					candidate.getOrchestrationForm().getTargetSystemName(),
					expireAt,
					candidate.getNotifyProtocol(),
					Utilities.toJson(candidate.getNotifyProperties()),
					Utilities.toJson(candidate.getOrchestrationForm().extractOrchestrationRequestDTO())));

			return saved.getId();

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
