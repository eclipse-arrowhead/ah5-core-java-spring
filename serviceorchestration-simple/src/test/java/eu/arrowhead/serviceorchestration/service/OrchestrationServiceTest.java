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

package eu.arrowhead.serviceorchestration.service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.dto.*;
import eu.arrowhead.dto.enums.OrchestrationType;
import eu.arrowhead.serviceorchestration.SimpleStoreServiceOrchestrationConstants;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationJob;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationStore;
import eu.arrowhead.serviceorchestration.jpa.entity.Subscription;
import eu.arrowhead.serviceorchestration.jpa.service.OrchestrationJobDbService;
import eu.arrowhead.serviceorchestration.jpa.service.SubscriptionDbService;
import eu.arrowhead.serviceorchestration.service.dto.DTOConverter;
import eu.arrowhead.serviceorchestration.service.model.SimpleOrchestrationRequest;
import eu.arrowhead.serviceorchestration.service.model.SimpleOrchestrationSubscriptionRequest;
import eu.arrowhead.serviceorchestration.service.utils.ServiceOrchestration;
import eu.arrowhead.serviceorchestration.service.validation.OrchestrationServiceValidation;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrchestrationServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private OrchestrationService service;

	@Mock
	private OrchestrationServiceValidation validator;

	@Mock
	private OrchestrationJobDbService orchJobDbService;

	@Mock
	private SubscriptionDbService subscriptionDbService;

	@Mock
	private ServiceOrchestration serviceOrchestration;

	@Mock(name = SimpleStoreServiceOrchestrationConstants.JOB_QUEUE_PUSH_ORCHESTRATION)
	private BlockingQueue<UUID> pushOrchJobQueue;

	@Mock
	private DTOConverter dtoConverter;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPullOk() {
		AtomicReference<UUID> jobID = new AtomicReference<>();

		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO("service1", null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO dto = new OrchestrationRequestDTO(requirementDTO, Map.of("MATCHMAKING", true), List.of(), null);
		final SimpleOrchestrationRequest normalized = new SimpleOrchestrationRequest("service1", List.of(), Map.of("MATCHMAKING", true), Set.of());
		final OrchestrationStore result = new OrchestrationStore("Consumer1", "service1", "Provider1|service1|1.0.0", 1, null);
		final OrchestrationResultDTO resultDTO = new OrchestrationResultDTO("Provider1|service1|1.0.0", "LOCAL", "Provider1", "service1", "1.0.0", null, null, null, null, null);
		OrchestrationResponseDTO responseDTO = new OrchestrationResponseDTO(List.of(resultDTO), List.of());

		when(validator.validateAndNormalizeRequester("Consumer1", "test origin")).thenReturn("Consumer1");
		when(validator.validateAndNormalizePull(dto, "test origin")).thenReturn(normalized);
		when(orchJobDbService.create(any())).thenAnswer(Invocation -> {
			final List<OrchestrationJob> jobs = Invocation.getArgument(0);
			final OrchestrationJob job = jobs.getFirst();
			jobID.set(job.getId());
			return List.of(job);
		});
		when(serviceOrchestration.orchestrate(any(), eq("Consumer1"), eq(normalized))).thenReturn(List.of(result));
		when(dtoConverter.convertStoreEntitiesToOrchestrationResponseDTO(List.of(result), Set.of())).thenReturn(responseDTO);

		final OrchestrationResponseDTO actual = service.pull("Consumer1", dto, "test origin");
		assertEquals(responseDTO, actual);
		verify(orchJobDbService).create(argThat(jobList ->
						jobList.size() == 1
						&& jobList.getFirst().getType() == OrchestrationType.PULL
						&& Objects.equals(jobList.getFirst().getRequesterSystem(), "Consumer1")
						&& Objects.equals(jobList.getFirst().getTargetSystem(), "Consumer1")
						&& Objects.equals(jobList.getFirst().getServiceDefinition(), "service1")));
		verify(serviceOrchestration).orchestrate(jobID.get(), "Consumer1", normalized);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPullThrowsInternalServerError() {

		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO("service1", null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO dto = new OrchestrationRequestDTO(requirementDTO, Map.of("MATCHMAKING", true), List.of(), null);
		final SimpleOrchestrationRequest normalized = new SimpleOrchestrationRequest("service1", List.of(), Map.of("MATCHMAKING", true), Set.of());

		when(validator.validateAndNormalizeRequester("Consumer1", "test origin")).thenReturn("Consumer1");
		when(validator.validateAndNormalizePull(dto, "test origin")).thenReturn(normalized);
		when(orchJobDbService.create(any())).thenThrow(new InternalServerError("Internal error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.pull("Consumer1", dto, "test origin"));
		assertEquals(InternalServerError.class, ex.getClass());
		assertEquals("Internal error", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPushSubscribeOverrideExistingRecordNoTrigger() {

		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO("service1", null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, Map.of("MATCHMAKING", true), List.of(), null);
		final OrchestrationNotifyInterfaceDTO intfDTO = new OrchestrationNotifyInterfaceDTO("GENERIC_HTTP", Map.of("port", "4040"));
		final OrchestrationNotifyInterfaceDTO normalizedIntfDTO = new OrchestrationNotifyInterfaceDTO("generic_http", Map.of("port", "4040"));
		final OrchestrationSubscriptionRequestDTO dto = new OrchestrationSubscriptionRequestDTO("Consumer1", requestDTO, normalizedIntfDTO, 30L);
		final SimpleOrchestrationRequest normalizedOrchRequest = new SimpleOrchestrationRequest("service1", List.of(), Map.of("MATCHMAKING", true), Set.of());
		final SimpleOrchestrationSubscriptionRequest simpleOrchRequest = new SimpleOrchestrationSubscriptionRequest("Consumer1", normalizedOrchRequest, intfDTO, 30L);
		final Subscription existingSubscription = new Subscription(
				"Provider1",
				"Consumer1",
				"service1",
				ZonedDateTime.parse("2026-01-27T01:53:02Z"),
				"generic_http",
				Utilities.toJson(normalizedIntfDTO),
				Utilities.toJson(normalizedOrchRequest));
		final Subscription createdSubscription = new Subscription(
				"Provider1",
				"Consumer1",
				"service1",
				ZonedDateTime.parse("2026-01-30T01:53:02Z"),
				"generic_http",
				Utilities.toJson(normalizedIntfDTO),
				Utilities.toJson(normalizedOrchRequest));
		createdSubscription.setId(UUID.fromString("9f3c2a7e-4b61-4f8a-b1f2-6e8a9c4d5b27"));

		when(validator.validateAndNormalizePushSubscribe(dto, "Consumer1", "test origin")).thenReturn(simpleOrchRequest);
		when(subscriptionDbService.get("Consumer1", "Consumer1", "service1")).thenReturn(Optional.of(existingSubscription));
		when(subscriptionDbService.create(List.of(simpleOrchRequest), "Consumer1")).thenReturn(List.of(createdSubscription));

		final Pair<Boolean, String> expected = Pair.of(true, "9f3c2a7e-4b61-4f8a-b1f2-6e8a9c4d5b27");
		final Pair<Boolean, String> actual = service.pushSubscribe("Consumer1", dto, false, "test origin");
		assertEquals(expected, actual);

		verify(subscriptionDbService).get("Consumer1", "Consumer1", "service1");
		verify(subscriptionDbService).create(List.of(simpleOrchRequest), "Consumer1");
		verify(orchJobDbService, never()).create(any());
	}
}
