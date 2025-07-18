package eu.arrowhead.serviceorchestration.service.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.common.service.util.ServiceInterfaceAddressPropertyProcessor;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.OrchestrationRequestDTO;
import eu.arrowhead.dto.OrchestrationResponseDTO;
import eu.arrowhead.dto.OrchestrationServiceRequirementDTO;
import eu.arrowhead.dto.ServiceDefinitionResponseDTO;
import eu.arrowhead.dto.ServiceInstanceListResponseDTO;
import eu.arrowhead.dto.ServiceInstanceLookupRequestDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.dto.SystemResponseDTO;
import eu.arrowhead.dto.enums.OrchestrationFlag;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationConstants;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationSystemInfo;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationLock;
import eu.arrowhead.serviceorchestration.jpa.service.OrchestrationJobDbService;
import eu.arrowhead.serviceorchestration.jpa.service.OrchestrationLockDbService;
import eu.arrowhead.serviceorchestration.service.enums.OrchestrationJobStatus;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationForm;
import eu.arrowhead.serviceorchestration.service.utils.matchmaker.ServiceInstanceMatchmaker;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("checkstyle:magicnumber")
public class LocalServiceOrchestrationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private LocalServiceOrchestration orchestration;

	@Mock
	private DynamicServiceOrchestrationSystemInfo sysInfo;

	@Mock
	private InterCloudServiceOrchestration interCloudOrch;

	@Mock
	private OrchestrationJobDbService orchJobDbService;

	@Mock
	private OrchestrationLockDbService orchLockDbService;

	@Mock
	private ArrowheadHttpService ahHttpService;

	@Mock(name = DynamicServiceOrchestrationConstants.SERVICE_INSTANCE_MATCHMAKER)
	private ServiceInstanceMatchmaker matchmaker;

	@Mock
	private ServiceInterfaceAddressPropertyProcessor interfaceAddressPropertyProcessor;

	@Captor
	private ArgumentCaptor<String> stringCaptor;
	
	@Captor
	private ArgumentCaptor<Set<Long>> longSetCaptor;

	@Captor
	private ArgumentCaptor<List<String>> stringListCaptor;

	@Captor
	private ArgumentCaptor<ServiceInstanceLookupRequestDTO> serviceInstanceLookupRequestCaptor;

	private static final String testSerfviceDef = "testService";

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationResultlessServiceDiscoveryIntercloudNotEnabledAndNotAllowed() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, null, null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(), 0));

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				serviceInstanceLookupRequestCaptor.capture(), any());
		verify(orchLockDbService, never()).getByServiceInstanceId(anyList());
		verify(orchLockDbService, never()).deleteInBatch(anyList());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		assertTrue(serviceInstanceLookupRequestCaptor.getValue().serviceDefinitionNames().size() == 1);
		assertEquals(testSerfviceDef, serviceInstanceLookupRequestCaptor.getValue().serviceDefinitionNames().get(0));
		assertEquals("No results were found", stringCaptor.getValue());
		assertTrue(result.results().size() == 0);
		assertTrue(result.warnings().size() == 0);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationResultlessServiceDiscoveryIntercloudIsEnabledButNotAllowed() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, null, null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(), 0));
		when(sysInfo.isIntercloudEnabled()).thenReturn(true);

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				serviceInstanceLookupRequestCaptor.capture(), any());
		verify(orchLockDbService, never()).getByServiceInstanceId(anyList());
		verify(orchLockDbService, never()).deleteInBatch(anyList());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		assertTrue(serviceInstanceLookupRequestCaptor.getValue().serviceDefinitionNames().size() == 1);
		assertEquals(testSerfviceDef, serviceInstanceLookupRequestCaptor.getValue().serviceDefinitionNames().get(0));
		assertEquals("No results were found", stringCaptor.getValue());
		assertTrue(result.results().size() == 0);
		assertTrue(result.warnings().size() == 0);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationResultlessServiceDiscoveryIntercloudIsEnabledAndAllowed() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, Map.of(OrchestrationFlag.ALLOW_INTERCLOUD.name(), true), null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(), 0));
		when(sysInfo.isIntercloudEnabled()).thenReturn(true);
		when(interCloudOrch.doInterCloudServiceOrchestration(eq(jobId), eq(form))).thenReturn(new OrchestrationResponseDTO(List.of(), List.of()));

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				serviceInstanceLookupRequestCaptor.capture(), any());
		verify(orchLockDbService, never()).getByServiceInstanceId(anyList());
		verify(orchLockDbService, never()).deleteInBatch(anyList());
		verify(interCloudOrch).doInterCloudServiceOrchestration(eq(jobId), eq(form));
		verify(orchJobDbService, never()).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), anyString());

		assertTrue(serviceInstanceLookupRequestCaptor.getValue().serviceDefinitionNames().size() == 1);
		assertEquals(testSerfviceDef, serviceInstanceLookupRequestCaptor.getValue().serviceDefinitionNames().get(0));
		assertTrue(result.results().size() == 0);
		assertTrue(result.warnings().size() == 0);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationServiceDiscoveryWithoutInterfacesBecauseTranslationIsAllowedAndEnabled() {
		final UUID jobId = UUID.randomUUID();
		final MetadataRequirementDTO interfacePropReq = new MetadataRequirementDTO();
		interfacePropReq.put("basePath", "/kelvin");
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, List.of("generic_http"), List.of("IPV4"), List.of(interfacePropReq), null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, Map.of(OrchestrationFlag.ALLOW_TRANSLATION.name(), true), null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(), 0));
		when(sysInfo.isTranslationEnabled()).thenReturn(true);

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				serviceInstanceLookupRequestCaptor.capture(), any());
		verify(orchLockDbService, never()).getByServiceInstanceId(anyList());
		verify(orchLockDbService, never()).deleteInBatch(anyList());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		final ServiceInstanceLookupRequestDTO lookupRequestDTO = serviceInstanceLookupRequestCaptor.getValue();
		assertTrue(lookupRequestDTO.serviceDefinitionNames().size() == 1);
		assertEquals(testSerfviceDef, lookupRequestDTO.serviceDefinitionNames().get(0));
		assertTrue(lookupRequestDTO.interfaceTemplateNames() == null);
		assertTrue(lookupRequestDTO.addressTypes() == null);
		assertTrue(lookupRequestDTO.interfacePropertyRequirementsList() == null);
		assertEquals("No results were found", stringCaptor.getValue());
		assertTrue(result.results().size() == 0);
		assertTrue(result.warnings().size() == 0);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationServiceDiscoveryWithInterfacesBecauseTranslationIsAllowedAndButNotEnabled() {
		final UUID jobId = UUID.randomUUID();
		final MetadataRequirementDTO interfacePropReq = new MetadataRequirementDTO();
		interfacePropReq.put("basePath", "/kelvin");
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, List.of("generic_http"), List.of("IPV4"), List.of(interfacePropReq), null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, Map.of(OrchestrationFlag.ALLOW_TRANSLATION.name(), true), null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(), 0));
		when(sysInfo.isTranslationEnabled()).thenReturn(false);

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				serviceInstanceLookupRequestCaptor.capture(), any());
		verify(orchLockDbService, never()).getByServiceInstanceId(anyList());
		verify(orchLockDbService, never()).deleteInBatch(anyList());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		final ServiceInstanceLookupRequestDTO lookupRequestDTO = serviceInstanceLookupRequestCaptor.getValue();
		assertTrue(lookupRequestDTO.serviceDefinitionNames().size() == 1);
		assertEquals(testSerfviceDef, lookupRequestDTO.serviceDefinitionNames().get(0));
		assertTrue(lookupRequestDTO.interfaceTemplateNames().size() == 1);
		assertEquals("generic_http", lookupRequestDTO.interfaceTemplateNames().get(0));
		assertTrue(lookupRequestDTO.addressTypes().size() == 1);
		assertEquals("IPV4", lookupRequestDTO.addressTypes().get(0));
		assertTrue(lookupRequestDTO.interfacePropertyRequirementsList().size() == 1);
		assertEquals("/kelvin", lookupRequestDTO.interfacePropertyRequirementsList().get(0).get("basePath"));
		assertEquals("No results were found", stringCaptor.getValue());
		assertTrue(result.results().size() == 0);
		assertTrue(result.warnings().size() == 0);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationServiceDiscoveryWithOnlyPreferredProviders() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, null, null, null, List.of("PreferredProvider"));
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, Map.of(OrchestrationFlag.ONLY_PREFERRED.name(), true), null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(), 0));

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				serviceInstanceLookupRequestCaptor.capture(), any());
		verify(orchLockDbService, never()).getByServiceInstanceId(anyList());
		verify(orchLockDbService, never()).deleteInBatch(anyList());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		final ServiceInstanceLookupRequestDTO lookupRequestDTO = serviceInstanceLookupRequestCaptor.getValue();
		assertTrue(lookupRequestDTO.serviceDefinitionNames().size() == 1);
		assertEquals(testSerfviceDef, lookupRequestDTO.serviceDefinitionNames().get(0));
		assertTrue(lookupRequestDTO.providerNames().size() == 1);
		assertEquals("PreferredProvider", lookupRequestDTO.providerNames().get(0));
		assertEquals("No results were found", stringCaptor.getValue());
		assertTrue(result.results().size() == 0);
		assertTrue(result.warnings().size() == 0);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationServiceDiscoveryWithPreferredProvidersButNotOnly() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, null, null, null, List.of("PreferredProvider"));
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, null, null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(), 0));

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				serviceInstanceLookupRequestCaptor.capture(), any());
		verify(orchLockDbService, never()).getByServiceInstanceId(anyList());
		verify(orchLockDbService, never()).deleteInBatch(anyList());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		final ServiceInstanceLookupRequestDTO lookupRequestDTO = serviceInstanceLookupRequestCaptor.getValue();
		assertTrue(lookupRequestDTO.serviceDefinitionNames().size() == 1);
		assertEquals(testSerfviceDef, lookupRequestDTO.serviceDefinitionNames().get(0));
		assertTrue(lookupRequestDTO.providerNames() == null);
		assertEquals("No results were found", stringCaptor.getValue());
		assertTrue(result.results().size() == 0);
		assertTrue(result.warnings().size() == 0);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationOneOfTheCandidateIsLocked() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, null, null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceResponseDTO lockedCandidate = serviceInstanceResponseDTO("TestProvider1");
		final ServiceInstanceResponseDTO notLockedCandidate = serviceInstanceResponseDTO("TestProvider2");

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(lockedCandidate, notLockedCandidate), 2));
		when(orchLockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of(new OrchestrationLock(UUID.randomUUID().toString(), lockedCandidate.instanceId(), null, Utilities.utcNow().plusMinutes(10), false)));

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				serviceInstanceLookupRequestCaptor.capture(), any());
		verify(orchLockDbService, times(2)).getByServiceInstanceId(stringListCaptor.capture());
		verify(orchLockDbService, never()).deleteInBatch(anyList());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		final ServiceInstanceLookupRequestDTO lookupRequestDTO = serviceInstanceLookupRequestCaptor.getValue();
		final List<String> lockCheckIdsFirst = stringListCaptor.getAllValues().get(0);
		final List<String> lockCheckIdsSecond = stringListCaptor.getAllValues().get(1);

		assertTrue(lookupRequestDTO.serviceDefinitionNames().size() == 1);
		assertEquals(testSerfviceDef, lookupRequestDTO.serviceDefinitionNames().get(0));
		assertTrue(lockCheckIdsFirst.size() == 2);
		assertTrue(lockCheckIdsFirst.contains(lockedCandidate.instanceId()));
		assertTrue(lockCheckIdsFirst.contains(notLockedCandidate.instanceId()));
		assertTrue(lockCheckIdsSecond.size() == 1);
		assertTrue(lockCheckIdsSecond.contains(notLockedCandidate.instanceId()));
		assertEquals("1 local result", stringCaptor.getValue());
		assertTrue(result.results().size() == 1);
		assertEquals(notLockedCandidate.instanceId(), result.results().get(0).serviceInstanceId());
		assertTrue(result.warnings().size() == 0);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationOneOfTheCandidateIsLockedButTheLockIsExpired() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, null, null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceResponseDTO lockExpiredCandidate = serviceInstanceResponseDTO("TestProvider1");
		final ServiceInstanceResponseDTO notLockedCandidate = serviceInstanceResponseDTO("TestProvider2");
		final OrchestrationLock lockRecord = new OrchestrationLock(UUID.randomUUID().toString(), lockExpiredCandidate.instanceId(), null, Utilities.utcNow().minusMinutes(1), false);
		lockRecord.setId(78);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(lockExpiredCandidate, notLockedCandidate), 2));
		when(orchLockDbService.getByServiceInstanceId(anyList()))
				.thenReturn(List.of(lockRecord))
				.thenReturn(List.of());

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				serviceInstanceLookupRequestCaptor.capture(), any());
		verify(orchLockDbService, times(2)).getByServiceInstanceId(stringListCaptor.capture());
		verify(orchLockDbService).deleteInBatch(longSetCaptor.capture());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		final ServiceInstanceLookupRequestDTO lookupRequestDTO = serviceInstanceLookupRequestCaptor.getValue();
		final List<String> lockCheckIdsFirst = stringListCaptor.getAllValues().get(0);
		final List<String> lockCheckIdsSecond = stringListCaptor.getAllValues().get(1);
		final Set<Long> deleteLockIds = longSetCaptor.getValue();

		assertTrue(lookupRequestDTO.serviceDefinitionNames().size() == 1);
		assertEquals(testSerfviceDef, lookupRequestDTO.serviceDefinitionNames().get(0));
		assertTrue(lockCheckIdsFirst.size() == 2);
		assertTrue(lockCheckIdsFirst.contains(lockExpiredCandidate.instanceId()));
		assertTrue(lockCheckIdsFirst.contains(notLockedCandidate.instanceId()));
		assertTrue(deleteLockIds.size() == 1);
		assertTrue(deleteLockIds.contains(78L));
		assertTrue(lockCheckIdsSecond.size() == 2);
		assertTrue(lockCheckIdsSecond.contains(lockExpiredCandidate.instanceId()));
		assertTrue(lockCheckIdsSecond.contains(notLockedCandidate.instanceId()));
		assertEquals("2 local result", stringCaptor.getValue());
		assertTrue(result.results().size() == 2);
		assertEquals(lockExpiredCandidate.instanceId(), result.results().get(0).serviceInstanceId());
		assertEquals(notLockedCandidate.instanceId(), result.results().get(1).serviceInstanceId());
		assertTrue(result.warnings().size() == 0);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private ServiceInstanceResponseDTO serviceInstanceResponseDTO(final String sysName) {
		return serviceInstanceResponseDTO(sysName, null);
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceInstanceResponseDTO serviceInstanceResponseDTO(final String sysName, final Integer exclusivity) {
		return new ServiceInstanceResponseDTO(
				sysName + "|" + testSerfviceDef + "|1.0.0",
				new SystemResponseDTO(sysName, null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO(testSerfviceDef, null, null),
				null,
				null,
				exclusivity == null ? null : Map.of(Constants.METADATA_KEY_ALLOW_EXCLUSIVITY, exclusivity),
				new ArrayList<>(),
				null,
				null);
	}
}
