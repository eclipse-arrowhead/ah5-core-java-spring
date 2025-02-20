package eu.arrowhead.serviceorchestration.quartz;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationSystemInfo;
import eu.arrowhead.serviceorchestration.jpa.service.OrchestrationJobDbService;
import eu.arrowhead.serviceorchestration.jpa.service.OrchestrationLockDbService;
import eu.arrowhead.serviceorchestration.jpa.service.SubscriptionDbService;
import eu.arrowhead.serviceorchestration.service.enums.OrchestrationJobStatus;

@Component
@DisallowConcurrentExecution
public class CleanerJob implements Job {

	//=================================================================================================
	// members

	@Autowired
	private DynamicServiceOrchestrationSystemInfo sysInfo;

	@Autowired
	private SubscriptionDbService subscriptionDbService;

	@Autowired
	private OrchestrationLockDbService orchestrationLockDbService;

	@Autowired
	private OrchestrationJobDbService orchestrationJobDbService;

	private static final Object LOCK = new Object();

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public void execute(final JobExecutionContext context) throws JobExecutionException {
		logger.debug("execute started...");
		System.out.println("cleaning...");
		try {
			removeExpiredSubscriptions();
			removeExpiredOrchestrationLocks();
			removeOldOrchestrationJobs();

		} catch (final Exception ex) {
			logger.debug(ex);
			logger.error("Cleaner job error: " + ex.getMessage());
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void removeExpiredSubscriptions() {
		logger.debug("removeExpiredSubscriptions started..");

		synchronized (LOCK) {
			final ZonedDateTime now = Utilities.utcNow();
			final List<UUID> toRemove = new ArrayList<>();
			subscriptionDbService.getAll().forEach(subscription -> {
				if (subscription.getExpiresAt() != null && subscription.getExpiresAt().isAfter(now)) {
					toRemove.add(subscription.getId());
				}
			});
			if (!Utilities.isEmpty(toRemove)) {
				subscriptionDbService.deleteInBatch(toRemove);
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void removeExpiredOrchestrationLocks() {
		logger.debug("removeExpiredOrchestrationLocks started...");

		synchronized (logger) {
			final ZonedDateTime now = Utilities.utcNow();
			final List<Long> toRemove = new ArrayList<>();
			orchestrationLockDbService.getAll().forEach(lock -> {
				if (lock.getExpiresAt() != null && lock.getExpiresAt().isAfter(now)) {
					toRemove.add(lock.getId());
				}
			});
			if (!Utilities.isEmpty(toRemove)) {
				orchestrationLockDbService.deleteInBatch(toRemove);
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void removeOldOrchestrationJobs() {
		logger.debug("removeOldOrchestrationJobs started...");

		synchronized (LOCK) {
			final ZonedDateTime now = Utilities.utcNow();
			final List<UUID> toRemove = new ArrayList<>();
			orchestrationJobDbService.getAllByStatusIn(List.of(OrchestrationJobStatus.DONE, OrchestrationJobStatus.ERROR)).forEach(job -> {
				final ZonedDateTime expirationTime = job.getFinishedAt().plusDays(sysInfo.getOrchestrationHistoryMaxAge());
				if (expirationTime.isBefore(now)) {
					toRemove.add(job.getId());
				}
			});
			if (!Utilities.isEmpty(toRemove)) {
				orchestrationJobDbService.deleteInBatch(toRemove);
			}
		}
	}
}
