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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.util.UriComponents;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.http.HttpService;
import eu.arrowhead.common.mqtt.MqttService;
import eu.arrowhead.dto.OrchestrationResponseDTO;
import eu.arrowhead.dto.enums.OrchestrationType;
import eu.arrowhead.serviceorchestration.SimpleStoreServiceOrchestrationSystemInfo;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationJob;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationStore;
import eu.arrowhead.serviceorchestration.jpa.entity.Subscription;
import eu.arrowhead.serviceorchestration.jpa.service.OrchestrationJobDbService;
import eu.arrowhead.serviceorchestration.jpa.service.SubscriptionDbService;
import eu.arrowhead.serviceorchestration.service.dto.DTOConverter;
import eu.arrowhead.serviceorchestration.service.enums.OrchestrationJobStatus;
import eu.arrowhead.serviceorchestration.service.utils.ServiceOrchestration;

@ExtendWith(MockitoExtension.class)
public class PushOrchestrationWorkerTest {

	//=================================================================================================
	// members

	@Mock
	private OrchestrationJobDbService orchJobDbService;

	@Mock
	private SubscriptionDbService subscriptionDbService;

	@Spy
	private ObjectMapper mapper = new ObjectMapper();

	@Mock
	private ServiceOrchestration serviceOrchestration;

	@Mock
	private DTOConverter dtoConverter;

	@Mock
	private HttpService httpService;

	@Mock
	private MqttService mqttService;

	@Mock
	private SimpleStoreServiceOrchestrationSystemInfo sysInfo;

	private final UUID jobId = UUID.randomUUID();

	@InjectMocks
	private final PushOrchestrationWorker worker = new PushOrchestrationWorker(jobId);

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@BeforeEach
	public void setup() {
		ReflectionTestUtils.setField(worker, "jobId", jobId);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRunWithNonExistentJobDoesNothing() {
		when(orchJobDbService.getById(jobId)).thenReturn(Optional.empty());

		worker.run();

		verify(orchJobDbService).getById(jobId);
		verify(orchJobDbService, never()).setStatus(any(), any(), any());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRunSetsJobStatusToInProgress() {
		final OrchestrationJob job = createOrchestrationJob(jobId, "subscriptionId");

		when(orchJobDbService.getById(jobId)).thenReturn(Optional.of(job));
		when(orchJobDbService.setStatus(jobId, OrchestrationJobStatus.IN_PROGRESS, null)).thenReturn(job);

		worker.run();

		verify(orchJobDbService).setStatus(jobId, OrchestrationJobStatus.IN_PROGRESS, null);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRunWithNullSubscriptionIdSetsErrorStatus() {
		final OrchestrationJob job = createOrchestrationJob(jobId, null);

		when(orchJobDbService.getById(jobId)).thenReturn(Optional.of(job));
		when(orchJobDbService.setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), any())).thenReturn(job);

		worker.run();

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.ERROR), anyString());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRunWithNonExistentSubscriptionSetsErrorStatus() {
		final UUID subscriptionId = UUID.randomUUID();
		final OrchestrationJob job = createOrchestrationJob(jobId, subscriptionId.toString());

		when(orchJobDbService.getById(jobId)).thenReturn(Optional.of(job));
		when(orchJobDbService.setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), any())).thenReturn(job);
		when(subscriptionDbService.get(subscriptionId)).thenReturn(Optional.empty());

		worker.run();

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.ERROR), anyString());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRunWithHttpNotificationCallsHttpService() {
		final UUID subscriptionId = UUID.randomUUID();
		final OrchestrationJob job = createOrchestrationJob(jobId, subscriptionId.toString());
		final String orchestrationRequest = "{\"serviceDefinition\":\"testService\"}";
		final String notifyProperties = "{\"address\":\"localhost\",\"port\":\"8080\",\"method\":\"POST\",\"path\":\"/notify\"}";
		final Subscription subscription = createSubscription(subscriptionId, "HTTP", notifyProperties, orchestrationRequest);
		final OrchestrationResponseDTO responseDTO = new OrchestrationResponseDTO(List.of(), null);

		when(orchJobDbService.getById(jobId)).thenReturn(Optional.of(job));
		when(orchJobDbService.setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), any())).thenReturn(job);
		when(subscriptionDbService.get(subscriptionId)).thenReturn(Optional.of(subscription));
		when(serviceOrchestration.orchestrate(eq(jobId), anyString(), any())).thenReturn(List.of());
		when(dtoConverter.convertStoreEntitiesToOrchestrationResponseDTO(any(), any())).thenReturn(responseDTO);

		worker.run();

		verify(httpService).sendRequest(any(), eq(HttpMethod.POST), eq(Void.class), eq(responseDTO));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRunCallsServiceOrchestration() {
		final UUID subscriptionId = UUID.randomUUID();
		final OrchestrationJob job = createOrchestrationJob(jobId, subscriptionId.toString());
		final String orchestrationRequest = "{\"serviceDefinition\":\"testService\"}";
		final String notifyProperties = "{\"address\":\"localhost\",\"port\":\"8080\",\"method\":\"POST\",\"path\":\"/notify\"}";
		final Subscription subscription = createSubscription(subscriptionId, "HTTP", notifyProperties, orchestrationRequest);
		final OrchestrationResponseDTO responseDTO = new OrchestrationResponseDTO(List.of(), null);

		when(orchJobDbService.getById(jobId)).thenReturn(Optional.of(job));
		when(orchJobDbService.setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), any())).thenReturn(job);
		when(subscriptionDbService.get(subscriptionId)).thenReturn(Optional.of(subscription));
		when(serviceOrchestration.orchestrate(eq(jobId), eq("targetSystem"), any())).thenReturn(List.of());
		when(dtoConverter.convertStoreEntitiesToOrchestrationResponseDTO(any(), any())).thenReturn(responseDTO);

		worker.run();

		verify(serviceOrchestration).orchestrate(eq(jobId), eq("targetSystem"), any());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRunCallsDtoConverter() {
		final UUID subscriptionId = UUID.randomUUID();
		final OrchestrationJob job = createOrchestrationJob(jobId, subscriptionId.toString());
		final String orchestrationRequest = "{\"serviceDefinition\":\"testService\"}";
		final String notifyProperties = "{\"address\":\"localhost\",\"port\":\"8080\",\"method\":\"POST\",\"path\":\"/notify\"}";
		final Subscription subscription = createSubscription(subscriptionId, "HTTP", notifyProperties, orchestrationRequest);
		final List<OrchestrationStore> orchResult = List.of();
		final OrchestrationResponseDTO responseDTO = new OrchestrationResponseDTO(List.of(), null);

		when(orchJobDbService.getById(jobId)).thenReturn(Optional.of(job));
		when(orchJobDbService.setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), any())).thenReturn(job);
		when(subscriptionDbService.get(subscriptionId)).thenReturn(Optional.of(subscription));
		when(serviceOrchestration.orchestrate(any(), any(), any())).thenReturn(orchResult);
		when(dtoConverter.convertStoreEntitiesToOrchestrationResponseDTO(eq(orchResult), any())).thenReturn(responseDTO);

		worker.run();

		verify(dtoConverter).convertStoreEntitiesToOrchestrationResponseDTO(eq(orchResult), any());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRunWithMqttNotificationCallsMqttService() throws MqttPersistenceException, MqttException {
		final UUID subscriptionId = UUID.randomUUID();
		final OrchestrationJob job = createOrchestrationJob(jobId, subscriptionId.toString());
		final String orchestrationRequest = "{\"serviceDefinition\":\"testService\"}";
		final String notifyProperties = "{\"topic\":\"orchestration/notify\"}";
		final Subscription subscription = createSubscription(subscriptionId, "MQTT", notifyProperties, orchestrationRequest);
		final OrchestrationResponseDTO responseDTO = new OrchestrationResponseDTO(List.of(), null);
		final MqttClient mqttClient = mock(MqttClient.class);

		when(orchJobDbService.getById(jobId)).thenReturn(Optional.of(job));
		when(orchJobDbService.setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), any())).thenReturn(job);
		when(subscriptionDbService.get(subscriptionId)).thenReturn(Optional.of(subscription));
		when(serviceOrchestration.orchestrate(eq(jobId), anyString(), any())).thenReturn(List.of());
		when(dtoConverter.convertStoreEntitiesToOrchestrationResponseDTO(any(), any())).thenReturn(responseDTO);
		when(sysInfo.isMqttApiEnabled()).thenReturn(true);
		when(sysInfo.getSystemName()).thenReturn("Orchestrator");
		when(mqttService.client(any())).thenReturn(mqttClient);

		worker.run();

		verify(mqttService).client(any());
		verify(mqttClient).publish(eq("orchestration/notify"), any(MqttMessage.class));

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRunWithMqttNotificationWhenMqttDisabledSetsErrorStatus() {
		final UUID subscriptionId = UUID.randomUUID();
		final OrchestrationJob job = createOrchestrationJob(jobId, subscriptionId.toString());
		final String orchestrationRequest = "{\"serviceDefinition\":\"testService\"}";
		final String notifyProperties = "{\"topic\":\"orchestration/notify\"}";
		final Subscription subscription = createSubscription(subscriptionId, "MQTT", notifyProperties, orchestrationRequest);
		final OrchestrationResponseDTO responseDTO = new OrchestrationResponseDTO(List.of(), null);

		when(orchJobDbService.getById(jobId)).thenReturn(Optional.of(job));
		when(orchJobDbService.setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), any())).thenReturn(job);
		when(subscriptionDbService.get(subscriptionId)).thenReturn(Optional.of(subscription));
		when(serviceOrchestration.orchestrate(eq(jobId), anyString(), any())).thenReturn(List.of());
		when(dtoConverter.convertStoreEntitiesToOrchestrationResponseDTO(any(), any())).thenReturn(responseDTO);
		when(sysInfo.isMqttApiEnabled()).thenReturn(false);

		worker.run();

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.ERROR), anyString());
		verify(mqttService, never()).client(any());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRunWithUnsupportedProtocolSetsErrorStatus() {
		final UUID subscriptionId = UUID.randomUUID();
		final OrchestrationJob job = createOrchestrationJob(jobId, subscriptionId.toString());
		final String orchestrationRequest = "{\"serviceDefinition\":\"testService\"}";
		final String notifyProperties = "{\"address\":\"localhost\",\"port\":\"8080\"}";
		final Subscription subscription = createSubscription(subscriptionId, "UNKNOWN", notifyProperties, orchestrationRequest);
		final OrchestrationResponseDTO responseDTO = new OrchestrationResponseDTO(List.of(), null);

		when(orchJobDbService.getById(jobId)).thenReturn(Optional.of(job));
		when(orchJobDbService.setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), any())).thenReturn(job);
		when(subscriptionDbService.get(subscriptionId)).thenReturn(Optional.of(subscription));
		when(serviceOrchestration.orchestrate(any(), any(), any())).thenReturn(List.of());
		when(dtoConverter.convertStoreEntitiesToOrchestrationResponseDTO(any(), any())).thenReturn(responseDTO);

		worker.run();

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.ERROR), anyString());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRunWithExceptionSetsErrorStatus() {
		when(orchJobDbService.getById(jobId)).thenThrow(new InternalServerError("DB error"));

		worker.run();

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.ERROR), eq("DB error"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRunWithHttpNotificationExceptionSetsErrorStatus() {
		final UUID subscriptionId = UUID.randomUUID();
		final OrchestrationJob job = createOrchestrationJob(jobId, subscriptionId.toString());
		final String orchestrationRequest = "{\"serviceDefinition\":\"testService\"}";
		final String notifyProperties = "{\"address\":\"localhost\",\"port\":\"8080\",\"method\":\"POST\",\"path\":\"/notify\"}";
		final Subscription subscription = createSubscription(subscriptionId, "HTTP", notifyProperties, orchestrationRequest);
		final OrchestrationResponseDTO responseDTO = new OrchestrationResponseDTO(List.of(), null);

		when(orchJobDbService.getById(jobId)).thenReturn(Optional.of(job));
		when(orchJobDbService.setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), any())).thenReturn(job);
		when(subscriptionDbService.get(subscriptionId)).thenReturn(Optional.of(subscription));
		when(serviceOrchestration.orchestrate(eq(jobId), anyString(), any())).thenReturn(List.of());
		when(dtoConverter.convertStoreEntitiesToOrchestrationResponseDTO(any(), any())).thenReturn(responseDTO);
		doThrow(new RuntimeException("Connection refused")).when(httpService).sendRequest(
				any(UriComponents.class),
		        any(HttpMethod.class),
		        eq(Void.class),
		        any(Object.class));
		worker.run();

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.ERROR), anyString());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRunWithMqttNotificationExceptionSetsErrorStatus() throws MqttPersistenceException, MqttException {
		final UUID subscriptionId = UUID.randomUUID();
		final OrchestrationJob job = createOrchestrationJob(jobId, subscriptionId.toString());
		final String orchestrationRequest = "{\"serviceDefinition\":\"testService\"}";
		final String notifyProperties = "{\"topic\":\"orchestration/notify\"}";
		final Subscription subscription = createSubscription(subscriptionId, "MQTT", notifyProperties, orchestrationRequest);
		final OrchestrationResponseDTO responseDTO = new OrchestrationResponseDTO(List.of(), null);
		final MqttClient mqttClient = mock(MqttClient.class);

		when(orchJobDbService.getById(jobId)).thenReturn(Optional.of(job));
		when(orchJobDbService.setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), any())).thenReturn(job);
		when(subscriptionDbService.get(subscriptionId)).thenReturn(Optional.of(subscription));
		when(serviceOrchestration.orchestrate(eq(jobId), anyString(), any())).thenReturn(List.of());
		when(dtoConverter.convertStoreEntitiesToOrchestrationResponseDTO(any(), any())).thenReturn(responseDTO);
		when(sysInfo.isMqttApiEnabled()).thenReturn(true);
		when(sysInfo.getSystemName()).thenReturn("Orchestrator");
		when(mqttService.client(any())).thenReturn(mqttClient);
		doThrow(new MqttException(MqttException.REASON_CODE_CLIENT_NOT_CONNECTED)).when(mqttClient).publish(anyString(), any(MqttMessage.class));

		worker.run();

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.ERROR), anyString());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRunWithInvalidNotifyPropertiesJsonSetsErrorStatus() {
		final UUID subscriptionId = UUID.randomUUID();
		final OrchestrationJob job = createOrchestrationJob(jobId, subscriptionId.toString());
		final String orchestrationRequest = "{\"serviceDefinition\":\"testService\"}";
		final String notifyProperties = "invalid-json-not-parseable";
		final Subscription subscription = createSubscription(subscriptionId, "HTTP", notifyProperties, orchestrationRequest);
		final OrchestrationResponseDTO responseDTO = new OrchestrationResponseDTO(List.of(), null);

		when(orchJobDbService.getById(jobId)).thenReturn(Optional.of(job));
		when(orchJobDbService.setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), any())).thenReturn(job);
		when(subscriptionDbService.get(subscriptionId)).thenReturn(Optional.of(subscription));
		when(serviceOrchestration.orchestrate(eq(jobId), anyString(), any())).thenReturn(List.of());
		when(dtoConverter.convertStoreEntitiesToOrchestrationResponseDTO(any(), any())).thenReturn(responseDTO);

		worker.run();

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.ERROR), anyString());
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private OrchestrationJob createOrchestrationJob(final UUID id, final String subscriptionId) {
		final OrchestrationJob job = new OrchestrationJob(OrchestrationType.PUSH, "requesterSystem", "targetSystem", "serviceDef", subscriptionId);
		setJobId(job, id);
		return job;
	}

	//-------------------------------------------------------------------------------------------------
	private Subscription createSubscription(final UUID id, final String protocol, final String notifyProperties, final String orchestrationRequest) {
		final Subscription subscription = new Subscription("ownerSystem", "targetSystem", "serviceDef", null, protocol, notifyProperties, orchestrationRequest);
		subscription.setId(id);
		return subscription;
	}

	//-------------------------------------------------------------------------------------------------
	private void setJobId(final OrchestrationJob job, final UUID id) {
		try {
			final java.lang.reflect.Field idField = OrchestrationJob.class.getDeclaredField("id");
			idField.setAccessible(true);
			idField.set(job, id);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}
}
