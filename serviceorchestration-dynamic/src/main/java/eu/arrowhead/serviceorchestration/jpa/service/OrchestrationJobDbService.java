package eu.arrowhead.serviceorchestration.jpa.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationJob;
import eu.arrowhead.serviceorchestration.jpa.repository.OrchestrationJobRepository;
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
	public List<OrchestrationJob> query(final OrchestrationJobFilter filter) {
		//TODO
		return null;
	}

	//-------------------------------------------------------------------------------------------------
	public void setStatus(final UUID jobId, final OrchestrationJobStatus status, final String message) {
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

			jobRepo.saveAndFlush(job);

		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}
}
