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
package eu.arrowhead.serviceorchestration.service.thread;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.serviceorchestration.SimpleStoreServiceOrchestrationSystemInfo;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("checkstyle:MagicNumber")
public class PushOrchestrationThreadTest {

	//=================================================================================================
	// members

	private PushOrchestrationThread pushOrchestrationThread;

	@Mock
	private SimpleStoreServiceOrchestrationSystemInfo sysInfo;

	@Mock
	private Function<UUID, PushOrchestrationWorker> pushOrchestrationWorkerFactory;

	@Mock
	private PushOrchestrationWorker worker;

	private BlockingQueue<UUID> pushOrchJobQueue;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@BeforeEach
	public void setup() {
		pushOrchestrationThread = new PushOrchestrationThread();
		pushOrchJobQueue = new LinkedBlockingQueue<>();

		ReflectionTestUtils.setField(pushOrchestrationThread, "pushOrchJobQueue", pushOrchJobQueue);
		ReflectionTestUtils.setField(pushOrchestrationThread, "sysInfo", sysInfo);
		ReflectionTestUtils.setField(pushOrchestrationThread, "pushOrchestrationWorkerFactory", pushOrchestrationWorkerFactory);

		when(sysInfo.getPushOrchestrationMaxThread()).thenReturn(5);
		// Call init to create the threadpool
		ReflectionTestUtils.invokeMethod(pushOrchestrationThread, "init");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRunProcessesJobFromQueue() throws InterruptedException {
		final UUID jobId = UUID.randomUUID();

		when(pushOrchestrationWorkerFactory.apply(jobId)).thenReturn(worker);
		pushOrchestrationThread.start();
		pushOrchJobQueue.put(jobId);
		Thread.sleep(100);
		pushOrchestrationThread.interrupt();

		verify(pushOrchestrationWorkerFactory).apply(jobId);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRunProcessesMultipleJobsFromQueue() throws InterruptedException {
		final UUID jobId1 = UUID.randomUUID();
		final UUID jobId2 = UUID.randomUUID();

		when(pushOrchestrationWorkerFactory.apply(any(UUID.class))).thenReturn(worker);

		pushOrchestrationThread.start();
		pushOrchJobQueue.put(jobId1);
		pushOrchJobQueue.put(jobId2);
		Thread.sleep(200);
		pushOrchestrationThread.interrupt();

		verify(pushOrchestrationWorkerFactory).apply(jobId1);
		verify(pushOrchestrationWorkerFactory).apply(jobId2);
	}

	//=================================================================================================
	// Tests for interrupt

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInterruptStopsThread() throws InterruptedException {

		pushOrchestrationThread.start();
		pushOrchestrationThread.interrupt();
		pushOrchestrationThread.join(1000);

		assertFalse(pushOrchestrationThread.isAlive() && !pushOrchestrationThread.isInterrupted());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInterruptSetsDoWorkToFalse() throws InterruptedException {

		pushOrchestrationThread.start();
		pushOrchestrationThread.interrupt();

		Thread.sleep(100);
		final Boolean doWork = (Boolean) ReflectionTestUtils.getField(pushOrchestrationThread, "doWork");
		assertFalse(doWork);
	}
}
