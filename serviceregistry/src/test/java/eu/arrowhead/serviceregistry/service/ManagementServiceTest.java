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
package eu.arrowhead.serviceregistry.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.exception.LockedException;
import eu.arrowhead.common.service.PageService;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.DeviceListRequestDTO;
import eu.arrowhead.dto.DeviceListResponseDTO;
import eu.arrowhead.dto.DeviceQueryRequestDTO;
import eu.arrowhead.dto.DeviceRequestDTO;
import eu.arrowhead.dto.DeviceResponseDTO;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.dto.ServiceDefinitionListRequestDTO;
import eu.arrowhead.dto.ServiceDefinitionListResponseDTO;
import eu.arrowhead.dto.ServiceDefinitionResponseDTO;
import eu.arrowhead.dto.SystemListRequestDTO;
import eu.arrowhead.dto.SystemListResponseDTO;
import eu.arrowhead.dto.SystemQueryRequestDTO;
import eu.arrowhead.dto.SystemRequestDTO;
import eu.arrowhead.dto.SystemResponseDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.jpa.entity.Device;
import eu.arrowhead.serviceregistry.jpa.entity.DeviceAddress;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceDefinition;
import eu.arrowhead.serviceregistry.jpa.entity.System;
import eu.arrowhead.serviceregistry.jpa.entity.SystemAddress;
import eu.arrowhead.serviceregistry.jpa.service.DeviceDbService;
import eu.arrowhead.serviceregistry.jpa.service.ServiceDefinitionDbService;
import eu.arrowhead.serviceregistry.jpa.service.ServiceInstanceDbService;
import eu.arrowhead.serviceregistry.jpa.service.ServiceInterfaceTemplateDbService;
import eu.arrowhead.serviceregistry.jpa.service.SystemDbService;
import eu.arrowhead.serviceregistry.service.dto.DTOConverter;
import eu.arrowhead.serviceregistry.service.dto.NormalizedDeviceRequestDTO;
import eu.arrowhead.serviceregistry.service.dto.NormalizedSystemRequestDTO;
import eu.arrowhead.serviceregistry.service.validation.ManagementValidation;

@ExtendWith(MockitoExtension.class)
public class ManagementServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private ManagementService service;

	@Mock
	private ManagementValidation validator;

	@Mock
	private PageService pageService;

	@Mock
	private DeviceDbService deviceDbService;

	@Mock
	private ServiceDefinitionDbService serviceDefinitionDbService;

	@Mock
	private SystemDbService systemDbService;

	@Mock
	private ServiceInterfaceTemplateDbService interfaceTemplateDbService;

	@Mock
	private DTOConverter dtoConverter;

	@Mock
	private ServiceInstanceDbService instanceDbService;

	//=================================================================================================
	// methods

	// DEVICES

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateDevicesOk() {

		final DeviceListRequestDTO dto = new DeviceListRequestDTO(List.of(new DeviceRequestDTO("ALARM", Map.of("indoor", true), List.of("7c:5a:2e:d1:9b:44"))));
		final NormalizedDeviceRequestDTO normalizedDeviceRequest = new NormalizedDeviceRequestDTO("ALARM", Map.of("indoor", true), List.of(new AddressDTO("MAC", "7c:5a:2e:d1:9b:44")));
		when(validator.validateAndNormalizeCreateDevices(dto, "test origin")).thenReturn(List.of(normalizedDeviceRequest));

		final Device device = new Device("ALARM", "{\r\n  \"indoor\" : true\r\n}");
		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "7c:5a:2e:d1:9b:44");
		when(deviceDbService.createBulk(List.of(normalizedDeviceRequest))).thenReturn(List.of(Map.entry(device, List.of(deviceAddress))));

		final DeviceResponseDTO response = new DeviceResponseDTO("ALARM", Map.of("indoor", true), List.of(new AddressDTO("MAC", "7c:5a:2e:d1:9b:44")), null, null);
		final DeviceListResponseDTO expected = new DeviceListResponseDTO(List.of(response), 1);
		when(dtoConverter.convertDeviceAndDeviceAddressEntriesToDTO(List.of(Map.entry(device, List.of(deviceAddress))), 1)).thenReturn(expected);

		final DeviceListResponseDTO actual = service.createDevices(dto, "test origin");
		assertEquals(expected, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateDevicesThrowsInvalidParameterException() {

		final DeviceListRequestDTO dto = new DeviceListRequestDTO(List.of(new DeviceRequestDTO("ALARM", Map.of("indoor", true), List.of("7c:5a:2e:d1:9b:44"))));
		final NormalizedDeviceRequestDTO normalizedDeviceRequest = new NormalizedDeviceRequestDTO("ALARM", Map.of("indoor", true), List.of(new AddressDTO("MAC", "7c:5a:2e:d1:9b:44")));
		when(validator.validateAndNormalizeCreateDevices(dto, "test origin")).thenReturn(List.of(normalizedDeviceRequest));

		when(deviceDbService.createBulk(List.of(normalizedDeviceRequest))).thenThrow(new InvalidParameterException("Invalid parameter"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.createDevices(dto, "test origin"));
		assertEquals("Invalid parameter", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateDevicesThrowsInternalServerError() {

		final DeviceListRequestDTO dto = new DeviceListRequestDTO(List.of(new DeviceRequestDTO("ALARM", Map.of("indoor", true), List.of("7c:5a:2e:d1:9b:44"))));
		final NormalizedDeviceRequestDTO normalizedDeviceRequest = new NormalizedDeviceRequestDTO("ALARM", Map.of("indoor", true), List.of(new AddressDTO("MAC", "7c:5a:2e:d1:9b:44")));
		when(validator.validateAndNormalizeCreateDevices(dto, "test origin")).thenReturn(List.of(normalizedDeviceRequest));

		when(deviceDbService.createBulk(List.of(normalizedDeviceRequest))).thenThrow(new InternalServerError("Database error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.createDevices(dto, "test origin"));
		assertEquals("Database error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateDevicesOk() {

		final DeviceListRequestDTO dto = new DeviceListRequestDTO(List.of(new DeviceRequestDTO("ALARM", Map.of("indoor", true), List.of("7c:5a:2e:d1:9b:44"))));
		final NormalizedDeviceRequestDTO normalizedDeviceRequest = new NormalizedDeviceRequestDTO("ALARM", Map.of("indoor", true), List.of(new AddressDTO("MAC", "7c:5a:2e:d1:9b:44")));
		when(validator.validateAndNormalizeUpdateDevices(dto, "test origin")).thenReturn(List.of(normalizedDeviceRequest));

		final Device device = new Device("ALARM", "{\r\n  \"indoor\" : true\r\n}");
		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "7c:5a:2e:d1:9b:44");
		when(deviceDbService.updateBulk(List.of(normalizedDeviceRequest))).thenReturn(List.of(Map.entry(device, List.of(deviceAddress))));

		final DeviceResponseDTO response = new DeviceResponseDTO("ALARM", Map.of("indoor", true), List.of(new AddressDTO("MAC", "7c:5a:2e:d1:9b:44")), null, null);
		final DeviceListResponseDTO expected = new DeviceListResponseDTO(List.of(response), 1);
		when(dtoConverter.convertDeviceAndDeviceAddressEntriesToDTO(List.of(Map.entry(device, List.of(deviceAddress))), 1)).thenReturn(expected);

		final DeviceListResponseDTO actual = service.updateDevices(dto, "test origin");
		assertEquals(expected, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateDevicesThrowsInvalidParameterException() {

		final DeviceListRequestDTO dto = new DeviceListRequestDTO(List.of(new DeviceRequestDTO("ALARM", Map.of("indoor", true), List.of("7c:5a:2e:d1:9b:44"))));
		final NormalizedDeviceRequestDTO normalizedDeviceRequest = new NormalizedDeviceRequestDTO("ALARM", Map.of("indoor", true), List.of(new AddressDTO("MAC", "7c:5a:2e:d1:9b:44")));
		when(validator.validateAndNormalizeUpdateDevices(dto, "test origin")).thenReturn(List.of(normalizedDeviceRequest));

		when(deviceDbService.updateBulk(List.of(normalizedDeviceRequest))).thenThrow(new InvalidParameterException("Invalid parameter"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.updateDevices(dto, "test origin"));
		assertEquals("Invalid parameter", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateDevicesThrowsInternalServerError() {

		final DeviceListRequestDTO dto = new DeviceListRequestDTO(List.of(new DeviceRequestDTO("ALARM", Map.of("indoor", true), List.of("7c:5a:2e:d1:9b:44"))));
		final NormalizedDeviceRequestDTO normalizedDeviceRequest = new NormalizedDeviceRequestDTO("ALARM", Map.of("indoor", true), List.of(new AddressDTO("MAC", "7c:5a:2e:d1:9b:44")));
		when(validator.validateAndNormalizeUpdateDevices(dto, "test origin")).thenReturn(List.of(normalizedDeviceRequest));

		when(deviceDbService.updateBulk(List.of(normalizedDeviceRequest))).thenThrow(new InternalServerError("Database error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.updateDevices(dto, "test origin"));
		assertEquals("Database error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryDevicesAddressTypeNotEmpty() {

		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("indoor", Map.of("op", "EQUALS", "value", true));

		final DeviceQueryRequestDTO dto = new DeviceQueryRequestDTO(new PageDTO(10, 20, "ASC", "id"), List.of("ALARM"), List.of("7c:5a:2e:d1:9b:44"), "MAC", List.of(requirement));
		when(validator.validateAndNormalizeQueryDevices(dto, "test origin")).thenReturn(dto);

		final PageRequest pageRequest = PageRequest.of(10, 20, Direction.ASC, "id");
		when(pageService.getPageRequest(eq(new PageDTO(10, 20, "ASC", "id")), any(), any(), any(), eq("test origin"))).thenReturn(pageRequest);

		final Device device = new Device("ALARM", "{\r\n  \"indoor\" : true\r\n}");
		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "7c:5a:2e:d1:9b:44");
		final Page<Entry<Device, List<DeviceAddress>>> expectedPage = new PageImpl<>(List.of(Map.entry(device, List.of(deviceAddress))));
		when(deviceDbService.getPage(pageRequest, List.of("ALARM"), List.of("7c:5a:2e:d1:9b:44"), AddressType.MAC, List.of(requirement))).thenReturn(expectedPage);

		final DeviceResponseDTO response = new DeviceResponseDTO("ALARM", Map.of("indoor", true), List.of(new AddressDTO("MAC", "7c:5a:2e:d1:9b:44")), null, null);
		final DeviceListResponseDTO expected = new DeviceListResponseDTO(List.of(response), 1);
		when(dtoConverter.convertDeviceAndDeviceAddressEntriesToDTO(expectedPage, 1)).thenReturn(expected);

		final DeviceListResponseDTO actual = service.queryDevices(dto, "test origin");
		assertEquals(expected, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryDevicesAddressTypeEmpty() {

		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("indoor", Map.of("op", "EQUALS", "value", true));

		final DeviceQueryRequestDTO dto = new DeviceQueryRequestDTO(new PageDTO(10, 20, "ASC", "id"), List.of("ALARM"), List.of("7c:5a:2e:d1:9b:44"), "", List.of(requirement));
		when(validator.validateAndNormalizeQueryDevices(dto, "test origin")).thenReturn(dto);

		final PageRequest pageRequest = PageRequest.of(10, 20, Direction.ASC, "id");
		when(pageService.getPageRequest(eq(new PageDTO(10, 20, "ASC", "id")), any(), any(), any(), eq("test origin"))).thenReturn(pageRequest);

		final Device device = new Device("ALARM", "{\r\n  \"indoor\" : true\r\n}");
		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "7c:5a:2e:d1:9b:44");
		final Page<Entry<Device, List<DeviceAddress>>> expectedPage = new PageImpl<>(List.of(Map.entry(device, List.of(deviceAddress))));
		when(deviceDbService.getPage(pageRequest, List.of("ALARM"), List.of("7c:5a:2e:d1:9b:44"), null, List.of(requirement))).thenReturn(expectedPage);

		final DeviceResponseDTO response = new DeviceResponseDTO("ALARM", Map.of("indoor", true), List.of(new AddressDTO("MAC", "7c:5a:2e:d1:9b:44")), null, null);
		final DeviceListResponseDTO expected = new DeviceListResponseDTO(List.of(response), 1);
		when(dtoConverter.convertDeviceAndDeviceAddressEntriesToDTO(expectedPage, 1)).thenReturn(expected);

		final DeviceListResponseDTO actual = service.queryDevices(dto, "test origin");
		assertEquals(expected, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryDevicesThrowsInternalServerError() {

		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("indoor", Map.of("op", "EQUALS", "value", true));

		final DeviceQueryRequestDTO dto = new DeviceQueryRequestDTO(new PageDTO(10, 20, "ASC", "id"), List.of("ALARM"), List.of("7c:5a:2e:d1:9b:44"), "MAC", List.of(requirement));
		when(validator.validateAndNormalizeQueryDevices(dto, "test origin")).thenReturn(dto);
		when(pageService.getPageRequest(any(), any(), any(), any(), any())).thenReturn(PageRequest.of(10, 20, Direction.ASC, "id"));

		when(deviceDbService.getPage(any(), any(), any(), any(), any())).thenThrow(new InternalServerError("Database error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.queryDevices(dto, "test origin"));
		assertEquals("Database error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveDevicesOk() {

		final List<String> names = List.of("ALARM");
		when(validator.validateAndNormalizeRemoveDevices(names, "test origin")).thenReturn(names);

		service.removeDevices(names, "test origin");
		verify(deviceDbService).deleteByNameList(List.of("ALARM"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveDevicesThrowsInvalidParameterException() {

		final List<String> names = List.of("ALARM");
		when(validator.validateAndNormalizeRemoveDevices(names, "test origin")).thenReturn(names);
		doThrow(new InvalidParameterException("Invalid parameter")).when(deviceDbService).deleteByNameList(names);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.removeDevices(names, "test origin"));
		assertEquals("Invalid parameter", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveDevicesThrowsLockedException() {

		final List<String> names = List.of("ALARM");
		when(validator.validateAndNormalizeRemoveDevices(names, "test origin")).thenReturn(names);
		doThrow(new LockedException("Locked entity")).when(deviceDbService).deleteByNameList(names);

		final LockedException ex = assertThrows(LockedException.class, () -> service.removeDevices(names, "test origin"));
		assertEquals("Locked entity", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveDevicesThrowsInternalServerError() {

		final List<String> names = List.of("ALARM");
		when(validator.validateAndNormalizeRemoveDevices(names, "test origin")).thenReturn(names);
		doThrow(new InternalServerError("Database error")).when(deviceDbService).deleteByNameList(names);

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.removeDevices(names, "test origin"));
		assertEquals("Database error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}
	// SERVICE DEFINITIONS

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetServiceDefinitionsOk() {

		final PageDTO dto = new PageDTO(10, 20, "ASC", "id");
		final PageRequest pageRequest = PageRequest.of(10, 20, Direction.ASC, "id");
		when(pageService.getPageRequest(eq(dto), any(), any(), any(), eq("test origin"))).thenReturn(pageRequest);

		final ServiceDefinition serviceDefinition = new ServiceDefinition("alertService");
		final Page<ServiceDefinition> entitiesFromDb = new PageImpl<>(List.of(serviceDefinition));
		when(serviceDefinitionDbService.getPage(pageRequest)).thenReturn(entitiesFromDb);

		final ServiceDefinitionResponseDTO reponseEntity = new ServiceDefinitionResponseDTO("alertService", null, null);
		final ServiceDefinitionListResponseDTO expectedResponse = new ServiceDefinitionListResponseDTO(List.of(reponseEntity), 1);
		when(dtoConverter.convertServiceDefinitionEntityPageToDTO(entitiesFromDb)).thenReturn(expectedResponse);

		final ServiceDefinitionListResponseDTO actual = service.getServiceDefinitions(dto, "test origin");
		assertEquals(expectedResponse, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetServiceDefinitionsThrowsInternalServerError() {

		final PageDTO dto = new PageDTO(10, 20, "ASC", "id");
		final PageRequest pageRequest = PageRequest.of(10, 20, Direction.ASC, "id");
		when(pageService.getPageRequest(eq(dto), any(), any(), any(), eq("test origin"))).thenReturn(pageRequest);

		when(serviceDefinitionDbService.getPage(pageRequest)).thenThrow(new InternalServerError("Database error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.getServiceDefinitions(dto, "test origin"));
		assertEquals("Database error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCeateServiceDefinitionsOk() {

		final ServiceDefinitionListRequestDTO dto = new ServiceDefinitionListRequestDTO(List.of("alertService"));
		when(validator.validateAndNormalizeCreateServiceDefinitions(dto, "test origin")).thenReturn(List.of("alertService"));

		final ServiceDefinition serviceDefinition = new ServiceDefinition("alertService");
		when(serviceDefinitionDbService.createBulk(List.of("alertService"))).thenReturn(List.of(serviceDefinition));

		final ServiceDefinitionResponseDTO reponseEntity = new ServiceDefinitionResponseDTO("alertService", null, null);
		final ServiceDefinitionListResponseDTO expectedResponse = new ServiceDefinitionListResponseDTO(List.of(reponseEntity), 1);
		when(dtoConverter.convertServiceDefinitionEntityListToDTO(List.of(serviceDefinition))).thenReturn(expectedResponse);

		final ServiceDefinitionListResponseDTO actual = service.createServiceDefinitions(dto, "test origin");
		assertEquals(expectedResponse, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCeateServiceDefinitionsThrowsInvalidParameterException() {

		final ServiceDefinitionListRequestDTO dto = new ServiceDefinitionListRequestDTO(List.of("alertService"));
		when(validator.validateAndNormalizeCreateServiceDefinitions(dto, "test origin")).thenReturn(List.of("alertService"));

		when(serviceDefinitionDbService.createBulk(List.of("alertService"))).thenThrow(new InvalidParameterException("Invalid parameter"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.createServiceDefinitions(dto, "test origin"));
		assertEquals("Invalid parameter", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCeateServiceDefinitionsThrowsInternalServerError() {

		final ServiceDefinitionListRequestDTO dto = new ServiceDefinitionListRequestDTO(List.of("alertService"));
		when(validator.validateAndNormalizeCreateServiceDefinitions(dto, "test origin")).thenReturn(List.of("alertService"));

		when(serviceDefinitionDbService.createBulk(List.of("alertService"))).thenThrow(new InternalServerError("Database error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.createServiceDefinitions(dto, "test origin"));
		assertEquals("Database error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveServiceDefinitionsOk() {

		final List<String> names = List.of("alertService");
		when(validator.validateAndNormalizeRemoveServiceDefinitions(names, "test origin")).thenReturn(names);

		service.removeServiceDefinitions(names, "test origin");
		verify(serviceDefinitionDbService).removeBulk(names);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveServiceDefinitionsThrowsInternalServerError() {

		final List<String> names = List.of("alertService");
		when(validator.validateAndNormalizeRemoveServiceDefinitions(names, "test origin")).thenReturn(names);
		doThrow(new InternalServerError("Database error")).when(serviceDefinitionDbService).removeBulk(names);

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.removeServiceDefinitions(names, "test origin"));
		assertEquals("Database error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	// SYSTEMS

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateSystemsOk() {

		final SystemRequestDTO systemRequest = new SystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of("192.168.100.100"), "DEVICE1");
		final SystemListRequestDTO dto = new SystemListRequestDTO(List.of(systemRequest));

		final NormalizedSystemRequestDTO normalizedDto = new NormalizedSystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of(new AddressDTO("IPV4", "192.168.100.100")), "DEVICE1");
		when(validator.validateAndNormalizeCreateSystems(dto, "test origin")).thenReturn(List.of(normalizedDto));

		final System system = new System("TemperatureManager", "{\r\n  \"priority\" : 2\r\n}", "5.0.0");
		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.100.100");
	    final Device device = new Device("DEVICE1", "{ }");
	    final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "00:1a:2b:3c:4d:51");
	    final List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> dbResult = List.of(Triple.of(system, List.of(systemAddress), Map.entry(device, List.of(deviceAddress))));
	    when(systemDbService.createBulk(List.of(normalizedDto))).thenReturn(dbResult);

		final DeviceResponseDTO deviceResponse = new DeviceResponseDTO("DEVICE1", Map.of(), List.of(new AddressDTO("MAC", "00:1a:2b:3c:4d:51")), null, null);
		final SystemResponseDTO response = new SystemResponseDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of(new AddressDTO("IPV4", "192.168.100.100")), deviceResponse, null, null);
	    final SystemListResponseDTO result = new SystemListResponseDTO(List.of(response), 1);
	    when(dtoConverter.convertSystemTripletListToDTO(dbResult)).thenReturn(result);

	    final SystemListResponseDTO actual = service.createSystems(dto, "test origin");
	    assertEquals(result, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateSystemsThrowsInvalidParameterException() {

		final SystemRequestDTO systemRequest = new SystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of("192.168.100.100"), "DEVICE1");
		final SystemListRequestDTO dto = new SystemListRequestDTO(List.of(systemRequest));

		final NormalizedSystemRequestDTO normalizedDto = new NormalizedSystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of(new AddressDTO("IPV4", "192.168.100.100")), "DEVICE1");
		when(validator.validateAndNormalizeCreateSystems(dto, "test origin")).thenReturn(List.of(normalizedDto));

		when(systemDbService.createBulk(List.of(normalizedDto))).thenThrow(new InvalidParameterException("Invalid parameter"));

	    final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.createSystems(dto, "test origin"));
		assertEquals("Invalid parameter", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateSystemsThrowsInternalServerError() {

		final SystemRequestDTO systemRequest = new SystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of("192.168.100.100"), "DEVICE1");
		final SystemListRequestDTO dto = new SystemListRequestDTO(List.of(systemRequest));

		final NormalizedSystemRequestDTO normalizedDto = new NormalizedSystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of(new AddressDTO("IPV4", "192.168.100.100")), "DEVICE1");
		when(validator.validateAndNormalizeCreateSystems(dto, "test origin")).thenReturn(List.of(normalizedDto));

		when(systemDbService.createBulk(List.of(normalizedDto))).thenThrow(new InternalServerError("Database error"));

	    final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.createSystems(dto, "test origin"));
		assertEquals("Database error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQuerySystemsAddressTypeEmpty() {

		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("indoor", Map.of("op", "EQUALS", "value", true));

		final SystemQueryRequestDTO dto = new SystemQueryRequestDTO(new PageDTO(10, 20, "ASC", "id"), List.of("TemperatureManager"), List.of("192.168.100.100"), "", List.of(requirement), List.of("5.0.0"), List.of("DEVICE1"));

		when(validator.validateAndNormalizeQuerySystems(dto, "test origin")).thenReturn(dto);

		final PageRequest pageRequest = PageRequest.of(10, 20, Direction.ASC, "id");
		when(pageService.getPageRequest(eq(new PageDTO(10, 20, "ASC", "id")), any(), any(), any(), eq("test origin"))).thenReturn(pageRequest);

		final System system = new System("TemperatureManager", "{\r\n  \"indoor\" : true\r\n}", "5.0.0");
		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.100.100");
	    final Device device = new Device("DEVICE1", "{ }");
	    final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "7c:5a:2e:d1:9b:44");
	    final Page<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> systemTripletPage = new PageImpl<>(List.of(Triple.of(system, List.of(systemAddress), Map.entry(device, List.of(deviceAddress)))));
		when(systemDbService.getPageByFilters(eq(pageRequest), any(), any(), any(), any(), any(), any())).thenReturn(systemTripletPage);

		final DeviceResponseDTO deviceResponse = new DeviceResponseDTO("DEVICE1", Map.of("indoor", true), List.of(new AddressDTO("MAC", "7c:5a:2e:d1:9b:44")), null, null);
		final SystemResponseDTO response = new SystemResponseDTO("TemperatureManager", Map.of("indoor", true), "5.0.0", List.of(new AddressDTO("IPV4", "192.168.100.100")), deviceResponse, null, null);
		final SystemListResponseDTO result = new SystemListResponseDTO(List.of(response), 1);
		when(dtoConverter.convertSystemTripletPageToDTO(systemTripletPage)).thenReturn(result);

	    try (MockedStatic<Utilities> utilitiesMock = mockStatic(Utilities.class)) {
	    	utilitiesMock.when(() -> Utilities.isEmpty("")).thenReturn(true);

	    	service.querySystems(dto, true, "test origin");
	    	utilitiesMock.verify(() -> Utilities.isEmpty(""));
	    }
	    verify(systemDbService).getPageByFilters(pageRequest, List.of("TemperatureManager"), List.of("192.168.100.100"), null, List.of(requirement), List.of("5.0.0"), List.of("DEVICE1"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQuerySystemsAddressTypeNotEmpty() {

		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("indoor", Map.of("op", "EQUALS", "value", true));

		final SystemQueryRequestDTO dto = new SystemQueryRequestDTO(new PageDTO(10, 20, "ASC", "id"), List.of("TemperatureManager"), List.of("192.168.100.100"), "IPV4", List.of(requirement), List.of("5.0.0"), List.of("DEVICE1"));

		when(validator.validateAndNormalizeQuerySystems(dto, "test origin")).thenReturn(dto);

		final PageRequest pageRequest = PageRequest.of(10, 20, Direction.ASC, "id");
		when(pageService.getPageRequest(eq(new PageDTO(10, 20, "ASC", "id")), any(), any(), any(), eq("test origin"))).thenReturn(pageRequest);

		final System system = new System("TemperatureManager", "{\r\n  \"indoor\" : true\r\n}", "5.0.0");
		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.100.100");
	    final Device device = new Device("DEVICE1", "{ }");
	    final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "7c:5a:2e:d1:9b:44");
	    final Page<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> systemTripletPage = new PageImpl<>(List.of(Triple.of(system, List.of(systemAddress), Map.entry(device, List.of(deviceAddress)))));
		when(systemDbService.getPageByFilters(eq(pageRequest), any(), any(), any(), any(), any(), any())).thenReturn(systemTripletPage);

		final DeviceResponseDTO deviceResponse = new DeviceResponseDTO("DEVICE1", Map.of("indoor", true), List.of(new AddressDTO("MAC", "7c:5a:2e:d1:9b:44")), null, null);
		final SystemResponseDTO response = new SystemResponseDTO("TemperatureManager", Map.of("indoor", true), "5.0.0", List.of(new AddressDTO("IPV4", "192.168.100.100")), deviceResponse, null, null);
		final SystemListResponseDTO result = new SystemListResponseDTO(List.of(response), 1);
		when(dtoConverter.convertSystemTripletPageToDTO(systemTripletPage)).thenReturn(result);

	    service.querySystems(dto, true, "test origin");
	    verify(systemDbService).getPageByFilters(pageRequest, List.of("TemperatureManager"), List.of("192.168.100.100"), AddressType.IPV4, List.of(requirement), List.of("5.0.0"), List.of("DEVICE1"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQuerySystemsVerboseTrue() {

		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("indoor", Map.of("op", "EQUALS", "value", true));

		final SystemQueryRequestDTO dto = new SystemQueryRequestDTO(new PageDTO(10, 20, "ASC", "id"), List.of("TemperatureManager"), List.of("192.168.100.100"), "IPV4", List.of(requirement), List.of("5.0.0"), List.of("DEVICE1"));

		when(validator.validateAndNormalizeQuerySystems(dto, "test origin")).thenReturn(dto);

		final PageRequest pageRequest = PageRequest.of(10, 20, Direction.ASC, "id");
		when(pageService.getPageRequest(eq(new PageDTO(10, 20, "ASC", "id")), any(), any(), any(), eq("test origin"))).thenReturn(pageRequest);

		final System system = new System("TemperatureManager", "{\r\n  \"indoor\" : true\r\n}", "5.0.0");
		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.100.100");
	    final Device device = new Device("DEVICE1", "{ }");
	    final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "7c:5a:2e:d1:9b:44");
	    final Page<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> systemTripletPage = new PageImpl<>(List.of(Triple.of(system, List.of(systemAddress), Map.entry(device, List.of(deviceAddress)))));
		when(systemDbService.getPageByFilters(eq(pageRequest), any(), any(), any(), any(), any(), any())).thenReturn(systemTripletPage);

		final DeviceResponseDTO deviceResponse = new DeviceResponseDTO("DEVICE1", Map.of("indoor", true), List.of(new AddressDTO("MAC", "7c:5a:2e:d1:9b:44")), null, null);
		final SystemResponseDTO response = new SystemResponseDTO("TemperatureManager", Map.of("indoor", true), "5.0.0", List.of(new AddressDTO("IPV4", "192.168.100.100")), deviceResponse, null, null);
		final SystemListResponseDTO result = new SystemListResponseDTO(List.of(response), 1);
		when(dtoConverter.convertSystemTripletPageToDTO(systemTripletPage)).thenReturn(result);

	    final SystemListResponseDTO actual = service.querySystems(dto, true, "test origin");
	    assertEquals(result, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQuerySystemsVerboseFalse() {

		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("indoor", Map.of("op", "EQUALS", "value", true));

		final SystemQueryRequestDTO dto = new SystemQueryRequestDTO(new PageDTO(10, 20, "ASC", "id"), List.of("TemperatureManager"), List.of("192.168.100.100"), "IPV4", List.of(requirement), List.of("5.0.0"), List.of("DEVICE1"));

		when(validator.validateAndNormalizeQuerySystems(dto, "test origin")).thenReturn(dto);

		final PageRequest pageRequest = PageRequest.of(10, 20, Direction.ASC, "id");
		when(pageService.getPageRequest(eq(new PageDTO(10, 20, "ASC", "id")), any(), any(), any(), eq("test origin"))).thenReturn(pageRequest);

		final System system = new System("TemperatureManager", "{\r\n  \"indoor\" : true\r\n}", "5.0.0");
		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.100.100");
	    final Device device = new Device("DEVICE1", "{ }");
	    final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "7c:5a:2e:d1:9b:44");
	    final Page<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> systemTripletPage = new PageImpl<>(List.of(Triple.of(system, List.of(systemAddress), Map.entry(device, List.of(deviceAddress)))));
		when(systemDbService.getPageByFilters(eq(pageRequest), any(), any(), any(), any(), any(), any())).thenReturn(systemTripletPage);

		final DeviceResponseDTO deviceResponse = new DeviceResponseDTO("DEVICE1", Map.of("indoor", true), List.of(new AddressDTO("MAC", "7c:5a:2e:d1:9b:44")), null, null);
		final SystemResponseDTO response = new SystemResponseDTO("TemperatureManager", Map.of("indoor", true), "5.0.0", List.of(new AddressDTO("IPV4", "192.168.100.100")), deviceResponse, null, null);
		final SystemListResponseDTO result = new SystemListResponseDTO(List.of(response), 1);
		when(dtoConverter.convertSystemTripletPageToDTO(systemTripletPage)).thenReturn(result);

		final DeviceResponseDTO deviceTerse = new DeviceResponseDTO("DEVICE1", null, null, null, null);
		final SystemResponseDTO responseTerse = new SystemResponseDTO("TemperatureManager", Map.of("indoor", true), "5.0.0", List.of(new AddressDTO("IPV4", "192.168.100.100")), deviceTerse, null, null);
		final SystemListResponseDTO resultTerse = new SystemListResponseDTO(List.of(responseTerse), 1);
		when(dtoConverter.convertSystemListResponseDtoToTerse(result)).thenReturn(resultTerse);

	    final SystemListResponseDTO actual = service.querySystems(dto, false, "test origin");
	    assertEquals(resultTerse, actual);
	    verify(dtoConverter).convertSystemListResponseDtoToTerse(result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQuerySystemsThrowsInternalServerError() {

		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("indoor", Map.of("op", "EQUALS", "value", true));

		final SystemQueryRequestDTO dto = new SystemQueryRequestDTO(new PageDTO(10, 20, "ASC", "id"), List.of("TemperatureManager"), List.of("192.168.100.100"), "IPV4", List.of(requirement), List.of("5.0.0"), List.of("DEVICE1"));

		when(validator.validateAndNormalizeQuerySystems(dto, "test origin")).thenReturn(dto);

		final PageRequest pageRequest = PageRequest.of(10, 20, Direction.ASC, "id");
		when(pageService.getPageRequest(eq(new PageDTO(10, 20, "ASC", "id")), any(), any(), any(), eq("test origin"))).thenReturn(pageRequest);

		when(systemDbService.getPageByFilters(eq(pageRequest), any(), any(), any(), any(), any(), any())).thenThrow(new InternalServerError("Database error", "test origin"));

	    final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.querySystems(dto, true, "test origin"));
		assertEquals("Database error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateSystemsOk() {

		final SystemRequestDTO systemRequest = new SystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of("192.168.100.100"), "DEVICE1");
		final SystemListRequestDTO dto = new SystemListRequestDTO(List.of(systemRequest));

		final NormalizedSystemRequestDTO normalizedDto = new NormalizedSystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of(new AddressDTO("IPV4", "192.168.100.100")), "DEVICE1");
		when(validator.validateAndNormalizeUpdateSystems(dto, "test origin")).thenReturn(List.of(normalizedDto));

		final System system = new System("TemperatureManager", "{\r\n  \"priority\" : 2\r\n}", "5.0.0");
		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.100.100");
	    final Device device = new Device("DEVICE1", "{ }");
	    final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "00:1a:2b:3c:4d:51");
	    final List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> dbResult = List.of(Triple.of(system, List.of(systemAddress), Map.entry(device, List.of(deviceAddress))));
	    when(systemDbService.updateBulk(List.of(normalizedDto))).thenReturn(dbResult);

		final DeviceResponseDTO deviceResponse = new DeviceResponseDTO("DEVICE1", Map.of(), List.of(new AddressDTO("MAC", "00:1a:2b:3c:4d:51")), null, null);
		final SystemResponseDTO response = new SystemResponseDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of(new AddressDTO("IPV4", "192.168.100.100")), deviceResponse, null, null);
	    final SystemListResponseDTO result = new SystemListResponseDTO(List.of(response), 1);
	    when(dtoConverter.convertSystemTripletListToDTO(dbResult)).thenReturn(result);

	    final SystemListResponseDTO actual = service.updateSystems(dto, "test origin");
	    assertEquals(result, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateSystemsThrowsInvalidParameterException() {

		final SystemRequestDTO systemRequest = new SystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of("192.168.100.100"), "DEVICE1");
		final SystemListRequestDTO dto = new SystemListRequestDTO(List.of(systemRequest));

		final NormalizedSystemRequestDTO normalizedDto = new NormalizedSystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of(new AddressDTO("IPV4", "192.168.100.100")), "DEVICE1");
		when(validator.validateAndNormalizeUpdateSystems(dto, "test origin")).thenReturn(List.of(normalizedDto));

		when(systemDbService.updateBulk(List.of(normalizedDto))).thenThrow(new InvalidParameterException("Invalid parameter"));

	    final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.updateSystems(dto, "test origin"));
		assertEquals("Invalid parameter", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateSystemsThrowsInternalServerError() {

		final SystemRequestDTO systemRequest = new SystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of("192.168.100.100"), "DEVICE1");
		final SystemListRequestDTO dto = new SystemListRequestDTO(List.of(systemRequest));

		final NormalizedSystemRequestDTO normalizedDto = new NormalizedSystemRequestDTO("TemperatureManager", Map.of("priority", 1), "5.0.0", List.of(new AddressDTO("IPV4", "192.168.100.100")), "DEVICE1");
		when(validator.validateAndNormalizeUpdateSystems(dto, "test origin")).thenReturn(List.of(normalizedDto));

		when(systemDbService.updateBulk(List.of(normalizedDto))).thenThrow(new InternalServerError("Database error"));

	    final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.updateSystems(dto, "test origin"));
		assertEquals("Database error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveSystemsOk() {

		final List<String> names = List.of("TemperatureManager");
		when(validator.validateAndNormalizeRemoveSystems(names, "test origin")).thenReturn(names);

		assertDoesNotThrow(() -> service.removeSystems(names, "test origin"));
		verify(systemDbService).deleteByNameList(names);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveSystemsThrowsInternalServerError() {

		final List<String> names = List.of("TemperatureManager");
		when(validator.validateAndNormalizeRemoveSystems(names, "test origin")).thenReturn(names);
		doThrow(new InternalServerError("Database error", "test origin")).when(systemDbService).deleteByNameList(names);

	    final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.removeSystems(names, "test origin"));
		assertEquals("Database error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	// SERVICE INSTANCES
	
	// INTERFACE TEMPLATES
}
