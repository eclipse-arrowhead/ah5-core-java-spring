package eu.arrowhead.serviceorchestration;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import eu.arrowhead.serviceorchestration.service.utils.matchmaker.DefaultServiceInstanceMatchmaker;
import eu.arrowhead.serviceorchestration.service.utils.matchmaker.ServiceInstanceMatchmaker;

@Configuration
public class BeanConfig {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Bean(name = DynamicServiceOrchestrationConstants.JOB_QUEUE_PUSH_ORCHESTRATION)
	BlockingQueue<UUID> initPushOrchestrationJobQueue() {
		return new LinkedBlockingQueue<>();
	}

	//-------------------------------------------------------------------------------------------------
	@Bean(name = DynamicServiceOrchestrationConstants.SERVICE_INSTANCE_MATCHMAKER)
	ServiceInstanceMatchmaker initServiceInstanceMatchmaker() {
		return new DefaultServiceInstanceMatchmaker();
	}
}
