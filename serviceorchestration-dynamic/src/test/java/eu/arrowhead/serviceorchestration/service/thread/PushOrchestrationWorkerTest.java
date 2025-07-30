package eu.arrowhead.serviceorchestration.service.thread;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.web.util.UriComponents;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.http.HttpService;
import eu.arrowhead.common.mqtt.MqttService;
import eu.arrowhead.dto.OrchestrationRequestDTO;
import eu.arrowhead.dto.OrchestrationResponseDTO;
import eu.arrowhead.dto.OrchestrationServiceRequirementDTO;
import eu.arrowhead.dto.enums.OrchestrationFlag;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationSystemInfo;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationJob;
import eu.arrowhead.serviceorchestration.jpa.entity.Subscription;
import eu.arrowhead.serviceorchestration.jpa.service.OrchestrationJobDbService;
import eu.arrowhead.serviceorchestration.jpa.service.SubscriptionDbService;
import eu.arrowhead.serviceorchestration.service.enums.NotifyProtocol;
import eu.arrowhead.serviceorchestration.service.enums.OrchestrationJobStatus;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationForm;
import eu.arrowhead.serviceorchestration.service.utils.InterCloudServiceOrchestration;
import eu.arrowhead.serviceorchestration.service.utils.LocalServiceOrchestration;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("checkstyle:MagicNumber")
public class PushOrchestrationWorkerTest {

	//=================================================================================================
	// members

	@Mock
	private OrchestrationJobDbService orchJobDbService;

	@Mock
	private SubscriptionDbService subscriptionDbService;

	@Spy
	private ObjectMapper mapper;

	@Mock
	private LocalServiceOrchestration localOrch;

	@Mock
	private InterCloudServiceOrchestration interCloudOrch;

	@Mock
	private HttpService httpService;

	@Mock
	private MqttService mqttService;

	@Mock
	private DynamicServiceOrchestrationSystemInfo sysInfo;

	private final UUID jobId = UUID.randomUUID();
	private final UUID subscriptionId = UUID.randomUUID();

	@InjectMocks
	private PushOrchestrationWorker worker = new PushOrchestrationWorker(jobId);

	@Captor
	private ArgumentCaptor<OrchestrationForm> orchestrationFormCaptor;

	@Captor
	private ArgumentCaptor<OrchestrationResponseDTO> orchestrationResponseCator;

	@Captor
	private ArgumentCaptor<UriComponents> uriCaptor;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRunLocalCloudWithoutIssueHttpNotify() {
		final OrchestrationJob job = new OrchestrationJob();
		job.setId(jobId);
		job.setSubscriptionId(subscriptionId.toString());

		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO("testService", null, null, null, null, null, null, null, null, null), null, null, null);

		Subscription subscription = new Subscription();
		subscription.setId(subscriptionId);
		subscription.setOrchestrationRequest(Utilities.toJson(orchestrationRequestDTO));
		subscription.setNotifyProtocol(NotifyProtocol.HTTP.name());
		subscription.setNotifyProperties(Utilities.toJson(Map.of("address", "127.0.0.1", "port", "8941", "method", "post", "path", "/notify")));

		final OrchestrationResponseDTO orchestrationResponseDTO = new OrchestrationResponseDTO(List.of(), List.of("test warning"));

		when(orchJobDbService.getById(eq(jobId))).thenReturn(Optional.of(job));
		when(orchJobDbService.setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull())).thenAnswer(invocation -> {
			job.setStatus(OrchestrationJobStatus.IN_PROGRESS);
			return job;
		});
		when(subscriptionDbService.get(eq(subscriptionId))).thenReturn(Optional.of(subscription));
		when(localOrch.doLocalServiceOrchestration(eq(jobId), any(OrchestrationForm.class))).thenReturn(orchestrationResponseDTO);

		assertDoesNotThrow(() -> worker.run());

		verify(orchJobDbService).getById(eq(jobId));
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(subscriptionDbService).get(eq(subscriptionId));
		verify(localOrch).doLocalServiceOrchestration(eq(jobId), orchestrationFormCaptor.capture());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(httpService).sendRequest(uriCaptor.capture(), eq(HttpMethod.POST), eq(Void.class), eq(orchestrationResponseDTO));
		verify(orchJobDbService, never()).setStatus(eq(jobId), eq(OrchestrationJobStatus.ERROR), anyString());

		assertEquals(orchestrationRequestDTO.serviceRequirement().serviceDefinition(), orchestrationFormCaptor.getValue().getServiceDefinition());
		assertEquals("127.0.0.1", uriCaptor.getValue().getHost());
		assertEquals(8941, uriCaptor.getValue().getPort());
		assertEquals("/notify", uriCaptor.getValue().getPath());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRunInterCloudWithoutIssueHttpsNotify() {
		final OrchestrationJob job = new OrchestrationJob();
		job.setId(jobId);
		job.setSubscriptionId(subscriptionId.toString());

		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO("testService", null, null, null, null, null, null, null, null, null),
				Map.of(OrchestrationFlag.ONLY_INTERCLOUD.name(), true), null, null);

		Subscription subscription = new Subscription();
		subscription.setId(subscriptionId);
		subscription.setOrchestrationRequest(Utilities.toJson(orchestrationRequestDTO));
		subscription.setNotifyProtocol(NotifyProtocol.HTTPS.name());
		subscription.setNotifyProperties(Utilities.toJson(Map.of("address", "127.0.0.1", "port", "8941", "method", "post", "path", "/notify")));

		final OrchestrationResponseDTO orchestrationResponseDTO = new OrchestrationResponseDTO(List.of(), List.of("test warning"));

		when(orchJobDbService.getById(eq(jobId))).thenReturn(Optional.of(job));
		when(orchJobDbService.setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull())).thenAnswer(invocation -> {
			job.setStatus(OrchestrationJobStatus.IN_PROGRESS);
			return job;
		});
		when(subscriptionDbService.get(eq(subscriptionId))).thenReturn(Optional.of(subscription));
		when(interCloudOrch.doInterCloudServiceOrchestration(eq(jobId), any(OrchestrationForm.class))).thenReturn(orchestrationResponseDTO);

		assertDoesNotThrow(() -> worker.run());

		verify(orchJobDbService).getById(eq(jobId));
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(subscriptionDbService).get(eq(subscriptionId));
		verify(interCloudOrch).doInterCloudServiceOrchestration(eq(jobId), orchestrationFormCaptor.capture());
		verify(localOrch, never()).doLocalServiceOrchestration(any(), any());
		verify(httpService).sendRequest(uriCaptor.capture(), eq(HttpMethod.POST), eq(Void.class), eq(orchestrationResponseDTO));
		verify(orchJobDbService, never()).setStatus(eq(jobId), eq(OrchestrationJobStatus.ERROR), anyString());

		assertEquals(orchestrationRequestDTO.serviceRequirement().serviceDefinition(), orchestrationFormCaptor.getValue().getServiceDefinition());
		assertEquals("127.0.0.1", uriCaptor.getValue().getHost());
		assertEquals(8941, uriCaptor.getValue().getPort());
		assertEquals("/notify", uriCaptor.getValue().getPath());
	}
}
