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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.jpa.entity.Device;
import eu.arrowhead.serviceregistry.jpa.entity.DeviceAddress;
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

		//TODO
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkInheritedSystemAddress() {

		//TODO
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
	public void testCreateBulkThrowsInternalServerError() {

	}
}
