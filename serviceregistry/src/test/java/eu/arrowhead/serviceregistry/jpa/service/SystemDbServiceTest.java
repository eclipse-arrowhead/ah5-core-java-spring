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
package eu.arrowhead.serviceregistry.jpa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.exception.LockedException;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.jpa.entity.Device;
import eu.arrowhead.serviceregistry.jpa.entity.DeviceAddress;
import eu.arrowhead.serviceregistry.jpa.entity.DeviceSystemConnector;
import eu.arrowhead.serviceregistry.jpa.entity.System;
import eu.arrowhead.serviceregistry.jpa.entity.SystemAddress;
import eu.arrowhead.serviceregistry.jpa.repository.DeviceAddressRepository;
import eu.arrowhead.serviceregistry.jpa.repository.DeviceRepository;
import eu.arrowhead.serviceregistry.jpa.repository.DeviceSystemConnectorRepository;
import eu.arrowhead.serviceregistry.jpa.repository.SystemAddressRepository;
import eu.arrowhead.serviceregistry.jpa.repository.SystemRepository;
import eu.arrowhead.serviceregistry.service.dto.NormalizedSystemRequestDTO;

@ExtendWith(MockitoExtension.class)
public class SystemDbServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private SystemDbService service;

	@Mock
	private SystemRepository systemRepo;

	@Mock
	private SystemAddressRepository systemAddressRepo;

	@Mock
	private DeviceAddressRepository deviceAddressRepo;

	@Mock
	private DeviceRepository deviceRepo;

	@Mock
	private DeviceSystemConnectorRepository deviceSystemConnectorRepo;

	private static final String DB_ERROR_MSG = "Database operation error";

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkExistingSystemNamesThowsInvalidParameterException() {

		final NormalizedSystemRequestDTO dto1 = new NormalizedSystemRequestDTO("TemperatureConsumer", Map.of("indoor", false), "1.0.0", List.of(new AddressDTO("IPV4", "192.168.4.4")), "TEST_DEVICE1");
		final NormalizedSystemRequestDTO dto2 = new NormalizedSystemRequestDTO("TemperatureManager", Map.of("indoor", false), "1.0.0", List.of(new AddressDTO("IPV4", "192.168.4.5")), "TEST_DEVICE2");

		final System system1 = new System("TemperatureConsumer", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");
		final System system2 = new System("TemperatureManager", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");

		when(systemRepo.findAllByNameIn(any())).thenReturn(List.of(system1, system2));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.createBulk(List.of(dto1, dto2)));
		assertEquals("Systems with names already exist: TemperatureConsumer, TemperatureManager", ex.getMessage());
		verify(systemRepo).findAllByNameIn(List.of("TemperatureConsumer", "TemperatureManager"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkNotExistingDeviceNamesThowsInvalidParameterException() {

		when(systemRepo.findAllByNameIn(any())).thenReturn(List.of());
		when(deviceRepo.findAllByNameIn(any())).thenReturn(List.of());

		final NormalizedSystemRequestDTO dto1 = new NormalizedSystemRequestDTO("TemperatureConsumer", Map.of("indoor", false), "1.0.0", List.of(new AddressDTO("IPV4", "192.168.4.4")), "TEST_DEVICE1");
		final NormalizedSystemRequestDTO dto2 = new NormalizedSystemRequestDTO("TemperatureManager", Map.of("indoor", false), "1.0.0", List.of(new AddressDTO("IPV4", "192.168.4.5")), "TEST_DEVICE2");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.createBulk(List.of(dto1, dto2)));
		assertEquals("Device names do not exist: TEST_DEVICE1, TEST_DEVICE2", ex.getMessage());
		verify(deviceRepo).findAllByNameIn(List.of("TEST_DEVICE1", "TEST_DEVICE2"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkNoAddressesThrowsInvalidParameterException() {

		final NormalizedSystemRequestDTO dto = new NormalizedSystemRequestDTO("TemperatureConsumer", Map.of("indoor", false), "1.0.0", List.of(), "DEVICE");

		final Device device = new Device("DEVICE", "{\r\n  \"indoor\" : true\r\n}");
		when(deviceRepo.findAllByNameIn(List.of("DEVICE"))).thenReturn(List.of(device));
		when(deviceRepo.findByName("DEVICE")).thenReturn(Optional.of(device));

		// no inheritable address
		when(deviceAddressRepo.findAllByDeviceAndAddressTypeIn(any(), any())).thenReturn(List.of());

		when(systemRepo.findAllByNameIn(any())).thenReturn(List.of());
		when(systemRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.createBulk(List.of(dto)));
		assertEquals("At least one system address is needed for every system", ex.getMessage());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkInheritedSystemAddress() {

		final NormalizedSystemRequestDTO dto = new NormalizedSystemRequestDTO("TemperatureConsumer", Map.of("indoor", false), "1.0.0", List.of(), "DEVICE");

		final Device device = new Device("DEVICE", "{\r\n  \"indoor\" : true\r\n}");
		when(deviceRepo.findAllByNameIn(List.of("DEVICE"))).thenReturn(List.of(device));
		when(deviceRepo.findByName("DEVICE")).thenReturn(Optional.of(device));

		final DeviceAddress deviceAddress1 = new DeviceAddress(device, AddressType.MAC, "3a:7f:12:b4:9c:e1");
		final DeviceAddress deviceAddress2 = new DeviceAddress(device, AddressType.IPV4, "192.168.4.4");
		when(deviceAddressRepo.findAllByDevice(device)).thenReturn(List.of(deviceAddress1, deviceAddress2));

		when(deviceAddressRepo.findAllByDeviceAndAddressTypeIn(any(), any())).thenReturn(List.of(deviceAddress2));

		when(systemRepo.findAllByNameIn(any())).thenReturn(List.of());
		when(systemRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		final System system = new System("TemperatureConsumer", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");
		when(systemRepo.findByName("TemperatureConsumer")).thenReturn(Optional.of(system));

		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.4.4");

		when(deviceSystemConnectorRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		final List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> expected = List.of(
				Triple.of(system, List.of(systemAddress), Map.entry(device, List.of(deviceAddress1, deviceAddress2))));

		final List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> actual = service.createBulk(List.of(dto));
		assertEquals(expected, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkNotInheritedSystemAddresses() {

		final NormalizedSystemRequestDTO systemWithDevice = new NormalizedSystemRequestDTO("TemperatureConsumer", Map.of("indoor", false), "1.0.0", List.of(new AddressDTO("IPV4", "192.168.4.4")), "DEVICE");
		final NormalizedSystemRequestDTO systemWithoutDevice = new NormalizedSystemRequestDTO("TemperatureManager", Map.of("indoor", false), "1.0.0", List.of(new AddressDTO("IPV4", "192.168.4.5")), null);

		final Device device = new Device("DEVICE", "{\r\n  \"indoor\" : true\r\n}");
		when(deviceRepo.findAllByNameIn(List.of("DEVICE"))).thenReturn(List.of(device));
		when(deviceRepo.findByName("DEVICE")).thenReturn(Optional.of(device));

		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "3a:7f:12:b4:9c:e1");
		when(deviceAddressRepo.findAllByDevice(device)).thenReturn(List.of(deviceAddress));

		when(systemRepo.findAllByNameIn(any())).thenReturn(List.of());
		when(systemRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		final System system1 = new System("TemperatureConsumer", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");
		final System system2 = new System("TemperatureManager", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");
		when(systemRepo.findByName("TemperatureConsumer")).thenReturn(Optional.of(system1));

		final SystemAddress systemAddress1 = new SystemAddress(system1, AddressType.IPV4, "192.168.4.4");
		final SystemAddress systemAddress2 = new SystemAddress(system2, AddressType.IPV4, "192.168.4.5");
		when(systemAddressRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		when(deviceSystemConnectorRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		final List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> expected = List.of(
				Triple.of(system1, List.of(systemAddress1), Map.entry(device, List.of(deviceAddress))),
				Triple.of(system2, List.of(systemAddress2), null));

		final List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> actual = service.createBulk(List.of(systemWithDevice, systemWithoutDevice));
		assertEquals(expected, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkNoDevice() {

		final NormalizedSystemRequestDTO dto = new NormalizedSystemRequestDTO("TemperatureConsumer", Map.of("indoor", false), "1.0.0", List.of(new AddressDTO("IPV4", "192.168.4.4")), null);

		when(systemRepo.findAllByNameIn(any())).thenReturn(List.of());
		when(systemRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		final System system = new System("TemperatureConsumer", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");

		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.4.4");
		when(systemAddressRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		final List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> expected = List.of(
				Triple.of(system, List.of(systemAddress), null));

		final List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> actual = service.createBulk(List.of(dto));
		assertEquals(expected, actual);
		verify(deviceSystemConnectorRepo, never()).saveAllAndFlush(any());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkThrowsInternalServerError() {

		final NormalizedSystemRequestDTO dto = new NormalizedSystemRequestDTO("TemperatureConsumer", Map.of("indoor", false), "1.0.0", List.of(), "DEVICE");
		when(systemRepo.findAllByNameIn(any())).thenThrow(new LockedException("error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.createBulk(List.of(dto)));
		assertEquals(DB_ERROR_MSG, ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateBulkNotExistingSystemsThrowsInvalidParameterException() {

		final NormalizedSystemRequestDTO dto1 = new NormalizedSystemRequestDTO("TemperatureConsumer1", Map.of("indoor", false), "1.0.0", List.of(new AddressDTO("IPV4", "192.168.4.4")), "TEST_DEVICE1");
		final NormalizedSystemRequestDTO dto2 = new NormalizedSystemRequestDTO("TemperatureConsumer2", Map.of("indoor", false), "1.0.0", List.of(new AddressDTO("IPV4", "192.168.4.8")), "TEST_DEVICE8");
		final NormalizedSystemRequestDTO dto3 = new NormalizedSystemRequestDTO("TemperatureManager", Map.of("indoor", false), "1.0.0", List.of(new AddressDTO("IPV4", "192.168.4.5")), "TEST_DEVICE2");

		final System system = new System("TemperatureConsumer2", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");
		when(systemRepo.findAllByNameIn(any())).thenReturn(List.of(system));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.updateBulk(List.of(dto1, dto2, dto3)));
		assertEquals("System(s) not exists: TemperatureConsumer1, TemperatureManager", ex.getMessage());
		verify(systemRepo).findAllByNameIn(List.of("TemperatureConsumer1", "TemperatureConsumer2", "TemperatureManager"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateBulkNotExistingDevicesThrowsInvalidParameterException() {

		when(deviceRepo.findAllByNameIn(any())).thenReturn(List.of());

		final NormalizedSystemRequestDTO dto1 = new NormalizedSystemRequestDTO("TemperatureConsumer", Map.of("indoor", false), "1.0.0", List.of(new AddressDTO("IPV4", "192.168.4.4")), "TEST_DEVICE1");
		final NormalizedSystemRequestDTO dto2 = new NormalizedSystemRequestDTO("TemperatureManager", Map.of("indoor", false), "1.0.0", List.of(new AddressDTO("IPV4", "192.168.4.5")), "TEST_DEVICE2");

		final System system1 = new System("TemperatureConsumer", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");
		final System system2 = new System("TemperatureManager", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");
		when(systemRepo.findAllByNameIn(any())).thenReturn(List.of(system1, system2));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.updateBulk(List.of(dto1, dto2)));
		assertEquals("Device names do not exist: TEST_DEVICE1, TEST_DEVICE2", ex.getMessage());
		verify(deviceRepo).findAllByNameIn(List.of("TEST_DEVICE1", "TEST_DEVICE2"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateBulkNoDeviceCheckNeededOperations() {

		final NormalizedSystemRequestDTO dto = new NormalizedSystemRequestDTO("TemperatureConsumer", Map.of("indoor", true), "1.0.1", List.of(new AddressDTO("IPV4", "192.168.4.5")), null);

		final System oldSystem = new System("TemperatureConsumer", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");
		when(systemRepo.findAllByNameIn(any())).thenReturn(List.of(oldSystem));
		when(systemRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		when(deviceRepo.findAllByNameIn(any())).thenReturn(List.of());

		when(systemAddressRepo.deleteAllBySystemIn(any())).thenAnswer(Invocation -> Invocation.getArgument(0));
		when(systemAddressRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		when(deviceSystemConnectorRepo.findBySystem(any())).thenReturn(Optional.empty());

		final System newSystem = new System("TemperatureConsumer", "{\r\n  \"indoor\" : true\r\n}", "1.0.1");
		final SystemAddress systemAddress = new SystemAddress(newSystem, AddressType.IPV4, "192.168.4.5");
		List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> expected = List.of(Triple.of(newSystem, List.of(systemAddress), null));

		final List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> actual = service.updateBulk(List.of(dto));
		assertEquals(expected, actual);
		verify(systemRepo).saveAllAndFlush(List.of(newSystem));
		final InOrder inOrder = Mockito.inOrder(systemAddressRepo);
		inOrder.verify(systemAddressRepo).deleteAllBySystemIn(List.of(newSystem));
		inOrder.verify(systemAddressRepo).saveAllAndFlush(List.of(systemAddress));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateBulkNoOldDeviceButNewDevice() {

		final NormalizedSystemRequestDTO dto = new NormalizedSystemRequestDTO("TemperatureConsumer", Map.of("indoor", true), "1.0.1", List.of(new AddressDTO("IPV4", "192.168.4.5")), "DEVICE");

		final System oldSystem = new System("TemperatureConsumer", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");
		when(systemRepo.findAllByNameIn(any())).thenReturn(List.of(oldSystem));
		when(systemRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		final Device device = new Device("DEVICE", "{\r\n  \"indoor\" : true\r\n}");
		when(deviceRepo.findAllByNameIn(List.of("DEVICE"))).thenReturn(List.of(device));
		when(deviceRepo.findByName("DEVICE")).thenReturn(Optional.of(device));

		when(systemAddressRepo.deleteAllBySystemIn(any())).thenAnswer(Invocation -> Invocation.getArgument(0));
		when(systemAddressRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		when(deviceSystemConnectorRepo.findBySystem(any())).thenReturn(Optional.empty());
		when(deviceSystemConnectorRepo.saveAll(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "3a:7f:12:b4:9c:e1");
		when(deviceAddressRepo.findAllByDevice(any())).thenReturn(List.of(deviceAddress));

		final System newSystem = new System("TemperatureConsumer", "{\r\n  \"indoor\" : true\r\n}", "1.0.1");
		final SystemAddress systemAddress = new SystemAddress(newSystem, AddressType.IPV4, "192.168.4.5");
		List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> expected = List.of(Triple.of(newSystem, List.of(systemAddress), Map.entry(device, List.of(deviceAddress))));

		final List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> actual = service.updateBulk(List.of(dto));
		assertEquals(expected, actual);
		final InOrder inOrder = Mockito.inOrder(deviceSystemConnectorRepo);
		inOrder.verify(deviceSystemConnectorRepo).saveAll(List.of(new DeviceSystemConnector(device, newSystem)));
		inOrder.verify(deviceSystemConnectorRepo).flush();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateBulkOldDeviceButNoNewDevice() {

		final NormalizedSystemRequestDTO dto = new NormalizedSystemRequestDTO("TemperatureConsumer", Map.of("indoor", true), "1.0.1", List.of(new AddressDTO("IPV4", "192.168.4.5")), null);

		final System oldSystem = new System("TemperatureConsumer", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");
		when(systemRepo.findAllByNameIn(any())).thenReturn(List.of(oldSystem));
		when(systemRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		when(deviceRepo.findAllByNameIn(any())).thenReturn(List.of());

		final Device device = new Device("DEVICE", "{\r\n  \"indoor\" : true\r\n}");
		final DeviceSystemConnector connection = new DeviceSystemConnector(device, oldSystem);
		when(deviceSystemConnectorRepo.findBySystem(any())).thenReturn(Optional.of(connection));

		when(systemAddressRepo.deleteAllBySystemIn(any())).thenAnswer(Invocation -> Invocation.getArgument(0));
		when(systemAddressRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		final System newSystem = new System("TemperatureConsumer", "{\r\n  \"indoor\" : true\r\n}", "1.0.1");
		final SystemAddress systemAddress = new SystemAddress(newSystem, AddressType.IPV4, "192.168.4.5");
		List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> expected = List.of(Triple.of(newSystem, List.of(systemAddress), null));

		final List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> actual = service.updateBulk(List.of(dto));
		assertEquals(expected, actual);
		final InOrder inOrder = Mockito.inOrder(deviceSystemConnectorRepo);
		inOrder.verify(deviceSystemConnectorRepo).deleteAll(List.of(connection));
		inOrder.verify(deviceSystemConnectorRepo).flush();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateBulkUpdateDevice() {

		final NormalizedSystemRequestDTO dto = new NormalizedSystemRequestDTO("TemperatureConsumer", Map.of("indoor", true), "1.0.1", List.of(new AddressDTO("IPV4", "192.168.4.5")), "DEVICE2");

		System system = new System("TemperatureConsumer", "{\r\n  \"indoor\" : false\r\n}", "1.0.0");
		when(systemRepo.findAllByNameIn(any())).thenReturn(List.of(system));
		when(systemRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		final Device device2 = new Device("DEVICE2", "{\r\n  \"indoor\" : true\r\n}");
		when(deviceRepo.findAllByNameIn(any())).thenReturn(List.of(device2));
		when(deviceRepo.findByName("DEVICE2")).thenReturn(Optional.of(device2));

		final Device device1 = new Device("DEVICE1", "{\r\n  \"indoor\" : true\r\n}");

		when(systemAddressRepo.deleteAllBySystemIn(any())).thenAnswer(Invocation -> Invocation.getArgument(0));
		when(systemAddressRepo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));

		final DeviceAddress deviceAddress = new DeviceAddress(device2, AddressType.MAC, "3a:7f:12:b4:9c:e1");
		when(deviceAddressRepo.findAllByDevice(device2)).thenReturn(List.of(deviceAddress));

		final DeviceSystemConnector oldConnection = new DeviceSystemConnector(device1, system);
		when(deviceSystemConnectorRepo.findBySystem(any())).thenReturn(Optional.of(oldConnection));

		system.setMetadata("{\r\n  \"indoor\" : true\r\n}");
		system.setVersion("1.0.1");
		final SystemAddress systemAddress = new SystemAddress(system, AddressType.IPV4, "192.168.4.5");

		List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> expected = List.of(Triple.of(system, List.of(systemAddress), Map.entry(device2, List.of(deviceAddress))));

		final List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> actual = service.updateBulk(List.of(dto));
		assertEquals(expected, actual);
		final InOrder inOrder = Mockito.inOrder(deviceSystemConnectorRepo);
		inOrder.verify(deviceSystemConnectorRepo).saveAll(List.of(new DeviceSystemConnector(device2, system)));
		inOrder.verify(deviceSystemConnectorRepo).flush();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateBulkThrowsInternalServerError() {

		final NormalizedSystemRequestDTO dto = new NormalizedSystemRequestDTO("TemperatureConsumer", Map.of("indoor", true), "1.0.1", List.of(new AddressDTO("IPV4", "192.168.4.5")), "DEVICE2");
		when(systemRepo.findAllByNameIn(any())).thenThrow(new LockedException("error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.updateBulk(List.of(dto)));
		assertEquals(DB_ERROR_MSG, ex.getMessage());
	}
}
