/*******************************************************************************
 *
 * Copyright (c) 2025 AITIA
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 *
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  	AITIA - implementation
 *  	Arrowhead Consortia - conceptualization
 *
 *******************************************************************************/

package eu.arrowhead.serviceorchestration;

import eu.arrowhead.serviceorchestration.thread.PushOrchestrationWorker;
import eu.arrowhead.serviceorchestration.thread.model.PushOrchestrationJobDetails;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;

@Configuration
public class BeanConfig {

    //=================================================================================================
    // methods

    //-------------------------------------------------------------------------------------------------
    @Bean(name = SimpleStoreServiceOrchestrationConstants.JOB_QUEUE_PUSH_ORCHESTRATION)
    BlockingQueue<PushOrchestrationJobDetails> initPushOrchestrationJobQueue() {
        return new LinkedBlockingQueue<>();
    }

    //-------------------------------------------------------------------------------------------------
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    PushOrchestrationWorker createPushOrchestrationWorker(final PushOrchestrationJobDetails jobDetails) {
        return new PushOrchestrationWorker(jobDetails);
    }

    //-------------------------------------------------------------------------------------------------
    @Bean
    Function<PushOrchestrationJobDetails, PushOrchestrationWorker> pushOrchestrationWorkerFactory() {
        return jobDetails -> createPushOrchestrationWorker(jobDetails);
    }
}
