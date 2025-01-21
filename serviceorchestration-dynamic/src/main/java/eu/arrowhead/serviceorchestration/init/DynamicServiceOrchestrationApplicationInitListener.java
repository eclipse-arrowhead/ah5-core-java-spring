package eu.arrowhead.serviceorchestration.init;

import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.init.ApplicationInitListener;

@Component
public class DynamicServiceOrchestrationApplicationInitListener extends ApplicationInitListener {

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void customInit(final ContextRefreshedEvent event) throws InterruptedException {
		logger.debug("customInit started...");

		if (standaloneMode) {
			return;
		}

		// TODO implement

		logger.info("System {} published {} service(s).", sysInfo.getSystemName(), registeredServices.size());
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void customDestroy() {
		logger.debug("customDestroy started...");

		if (standaloneMode) {
			return;
		}

		try {
			// TODO implement

			logger.info("Core system {} revoked {} service(s).", sysInfo, registeredServices.size());
		} catch (final Throwable t) {
			logger.error(t.getMessage());
			logger.debug(t);
		}
	}
}
