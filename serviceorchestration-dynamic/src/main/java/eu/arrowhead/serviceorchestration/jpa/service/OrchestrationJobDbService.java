package eu.arrowhead.serviceorchestration.jpa.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationJob;
import eu.arrowhead.serviceorchestration.jpa.repository.OrchestrationJobRepository;
import eu.arrowhead.serviceorchestration.service.enums.BaseFilter;
import eu.arrowhead.serviceorchestration.service.enums.OrchestrationJobStatus;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationJobFilter;

@Service
public class OrchestrationJobDbService {

	//=================================================================================================
	// members

	@Autowired
	private OrchestrationJobRepository jobRepo;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// members

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public List<OrchestrationJob> create(final List<OrchestrationJob> jobs) {
		logger.debug("save started...");
		Assert.isTrue(!Utilities.isEmpty(jobs), "job list is null");

		try {
			return jobRepo.saveAllAndFlush(jobs);

		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public Optional<OrchestrationJob> getById(final UUID id) {
		logger.debug("getById started...");
		Assert.notNull(id, "id is null");

		try {
			return jobRepo.findById(id);

		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public Page<OrchestrationJob> query(final OrchestrationJobFilter filter, final PageRequest pagination) {
		logger.debug("query started...");

		try {
			List<OrchestrationJob> toFilter;
			BaseFilter baseFilter = BaseFilter.NONE;
			if (!Utilities.isEmpty(filter.getIds())) {
				baseFilter = BaseFilter.ID;
				toFilter = jobRepo.findAllById(filter.getIds());
			} else if (!Utilities.isEmpty(filter.getStatuses())) {
				baseFilter = BaseFilter.STATUS;
				toFilter = jobRepo.findAllByStatusIn(filter.getStatuses().stream().map(s -> s.name()).toList());
			} else if (!Utilities.isEmpty(filter.getRequesterSystems())) {
				baseFilter = BaseFilter.OWNER;
				toFilter = jobRepo.findAllByRequesterSystemIn(filter.getRequesterSystems());
			} else if (!Utilities.isEmpty(filter.getTargetSystems())) {
				baseFilter = BaseFilter.TARGET;
				toFilter = jobRepo.findAllByTargetSystemIn(filter.getTargetSystems());
			} else if (!Utilities.isEmpty(filter.getServiceDefinitions())) {
				baseFilter = BaseFilter.SERVICE;
				toFilter = jobRepo.findAllByServiceDefinitionIn(filter.getServiceDefinitions());
			} else {
				toFilter = jobRepo.findAll();
			}

			final List<UUID> matchingIds = new ArrayList<>();
			for (final OrchestrationJob job : toFilter) {
				boolean matching = true;

				// Match against to job ids
				if (baseFilter != BaseFilter.ID && !Utilities.isEmpty(filter.getIds()) && !filter.getIds().contains(job.getId())) {
					matching = false;
				}

				// Match against to job statuses
				if (matching && baseFilter != BaseFilter.STATUS && !Utilities.isEmpty(filter.getStatuses()) && !filter.getStatuses().contains(OrchestrationJobStatus.valueOf(job.getStatus()))) {
					matching = false;
				}

				// Match against to job type
				if (matching && filter.getType() != null && !filter.getType().name().equalsIgnoreCase(job.getType())) {
					matching = false;
				}

				// Match against to requester systems
				if (matching && baseFilter != BaseFilter.OWNER && !Utilities.isEmpty(filter.getRequesterSystems()) && !filter.getRequesterSystems().contains(job.getRequesterSystem())) {
					matching = false;
				}

				// Match against to target systems
				if (matching && baseFilter != BaseFilter.TARGET && !Utilities.isEmpty(filter.getTargetSystems()) && !filter.getTargetSystems().contains(job.getTargetSystem())) {
					matching = false;
				}

				// Match against to service definitions
				if (matching && baseFilter != BaseFilter.SERVICE && !Utilities.isEmpty(filter.getServiceDefinitions()) && !filter.getServiceDefinitions().contains(job.getServiceDefinition())) {
					matching = false;
				}

				// Match against to subscription ids
				if (matching && !Utilities.isEmpty(filter.getSubscriptionIds()) && !filter.getSubscriptionIds().contains(job.getSubscriptionId())) {
					matching = false;
				}

				if (matching) {
					matchingIds.add(job.getId());
				}
			}

			return jobRepo.findAllByIdIn(matchingIds, pagination);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public OrchestrationJob setStatus(final UUID jobId, final OrchestrationJobStatus status, final String message) {
		logger.debug("setStartedAt started...");

		try {
			final Optional<OrchestrationJob> optional = jobRepo.findById(jobId);
			Assert.isTrue(optional.isPresent(), "job does not exists: " + jobId);
			final OrchestrationJob job = optional.get();

			job.setStatus(status.name());
			if (!Utilities.isEmpty(message)) {
				job.setMessage(message);
			}

			switch (status) {
			case IN_PROGRESS:
				job.setStartedAt(Utilities.utcNow());
				break;

			case DONE:
			case ERROR:
				job.setFinishedAt(Utilities.utcNow());
				break;

			default:
				Assert.isTrue(false, "Unhandled orchestration job status: " + status);
			}

			return jobRepo.saveAndFlush(job);

		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}
}
