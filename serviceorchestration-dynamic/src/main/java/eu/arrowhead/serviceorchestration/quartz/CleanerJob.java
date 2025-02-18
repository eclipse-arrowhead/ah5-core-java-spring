package eu.arrowhead.serviceorchestration.quartz;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

@Component
@DisallowConcurrentExecution
public class CleanerJob implements Job {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public void execute(final JobExecutionContext context) throws JobExecutionException {
		logger.debug("execute started...");

		try {
			removeExpiredSubscriptions();
			removeExpiredServiceLocks();
			removeOldOrchestrationHistory();

		} catch (final Exception ex) {
			logger.debug(ex);
			logger.error("Cleaner job error");
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void removeExpiredSubscriptions() {
		//TODO
	}

	//-------------------------------------------------------------------------------------------------
	private void removeExpiredServiceLocks() {
		// TODO
	}

	//-------------------------------------------------------------------------------------------------
	private void removeOldOrchestrationHistory() {
		// TODO
	}
}
