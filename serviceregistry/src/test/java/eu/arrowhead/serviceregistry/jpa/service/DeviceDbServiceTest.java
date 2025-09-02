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
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import eu.arrowhead.common.exception.LockedException;
import eu.arrowhead.common.service.validation.MetadataRequirementsMatcher;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.jpa.entity.Device;
import eu.arrowhead.serviceregistry.jpa.entity.DeviceAddress;
import eu.arrowhead.serviceregistry.jpa.repository.DeviceAddressRepository;
import eu.arrowhead.serviceregistry.jpa.repository.DeviceRepository;
import eu.arrowhead.serviceregistry.jpa.repository.DeviceSystemConnectorRepository;

@ExtendWith(MockitoExtension.class)
public class DeviceDbServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private DeviceDbService service;

	@Mock
	private DeviceRepository deviceRepo;

	@Mock
	private DeviceAddressRepository addressRepo;

	@Mock
	private DeviceSystemConnectorRepository connectorRepo;

	private static final String DB_ERROR_MSG = "Database operation error";

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByFilters() {
		// TODO
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageWithoutFilterParameters() {

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");
		when(deviceRepo.findAll(pageRequest)).thenReturn(new PageImpl<>(List.of()));

		service.getPage(pageRequest);
		verify(deviceRepo).findAll(pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageShouldReturnMatchingAddressesToo() {

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");
		when(deviceRepo.findAll(pageRequest)).thenReturn(new PageImpl<>(List.of()));

		final Device device = new Device("DEVICE", "{\r\n  \"indoor\" : true\r\n}");
		when(deviceRepo.findAll(pageRequest)).thenReturn(new PageImpl<>(List.of(device)));

		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "3a:7f:12:b4:9c:e1");
		when(addressRepo.findAllByDevice(device)).thenReturn(List.of(deviceAddress));

		final Page<Entry<Device, List<DeviceAddress>>> expected = new PageImpl<Entry<Device, List<DeviceAddress>>>(List.of(Map.entry(device, List.of(deviceAddress))), pageRequest, 1);

		final Page<Entry<Device, List<DeviceAddress>>> actual = service.getPage(pageRequest, List.of(), List.of(), null, List.of());
		verify(addressRepo).findAllByDevice(device);
		assertEquals(expected, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageNotEmptyFilters() {

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");

		final Device device = new Device("DEVICE", "{\r\n  \"indoor\" : true\r\n}");
		when(deviceRepo.findAllByNameIn(any())).thenReturn(List.of(device));

		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "3a:7f:12:b4:9c:e1");
		when(addressRepo.findAllByDeviceAndAddressIn(any(), any())).thenReturn(List.of(deviceAddress));

		when(deviceRepo.findAllByNameIn(any(), any())).thenReturn(new PageImpl<>(List.of(device)));
		when(addressRepo.findAllByDevice(any())).thenReturn(List.of(deviceAddress));

		service.getPage(pageRequest, List.of("DEVICE"), List.of("3a:7f:12:b4:9c:e1"), null, List.of());
		verify(deviceRepo).findAllByNameIn(List.of("DEVICE"), pageRequest);

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageEmptyFiltersOnly() {

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");
		when(deviceRepo.findAll(pageRequest)).thenReturn(new PageImpl<>(List.of()));

		try (MockedStatic<Utilities> utilitiesMock = mockStatic(Utilities.class)) {
			utilitiesMock.when(() -> Utilities.isEmpty(List.of())).thenReturn(true);

			service.getPage(pageRequest, List.of(), List.of(), null, List.of());

			utilitiesMock.verify(() -> Utilities.isEmpty(List.of()), times(3));
		}
		verify(deviceRepo).findAll(pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageFilterByName() {

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");
		when(deviceRepo.findAllByNameIn(any())).thenReturn(List.of());
		when(deviceRepo.findAllByNameIn(any(), any())).thenReturn(new PageImpl<>(List.of()));

		service.getPage(pageRequest, List.of("DEVICE"), List.of(), null, List.of());
		verify(deviceRepo).findAllByNameIn(List.of("DEVICE"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageFilterByAddressButNoMatch() {

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");

		final Device device = new Device("DEVICE", "{\r\n  \"indoor\" : true\r\n}");
		when(deviceRepo.findAll()).thenReturn(List.of(device));
		when(addressRepo.findAllByDeviceAndAddressIn(any(), any())).thenReturn(List.of());
		when(deviceRepo.findAllByNameIn(any(), any())).thenReturn(new PageImpl<>(List.of()));

		service.getPage(pageRequest, List.of(), List.of("3a:7f:12:b4:9c:e1"), null, List.of());
		verify(addressRepo).findAllByDeviceAndAddressIn(device, List.of("3a:7f:12:b4:9c:e1"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageFilterByAddressAndMatch() {

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");

		final Device device = new Device("DEVICE", "{\r\n  \"indoor\" : true\r\n}");
		when(deviceRepo.findAll()).thenReturn(List.of(device));

		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "3a:7f:12:b4:9c:e1");
		when(addressRepo.findAllByDeviceAndAddressIn(any(), any())).thenReturn(List.of(deviceAddress));

		when(deviceRepo.findAllByNameIn(any(), any())).thenReturn(new PageImpl<>(List.of(device)));
		when(addressRepo.findAllByDevice(any())).thenReturn(List.of(deviceAddress));

		final Page<Entry<Device, List<DeviceAddress>>> expected = new PageImpl<Entry<Device, List<DeviceAddress>>>(List.of(Map.entry(device, List.of(deviceAddress))), pageRequest, 1);

		final Page<Entry<Device, List<DeviceAddress>>> actual = service.getPage(pageRequest, List.of(), List.of("3a:7f:12:b4:9c:e1"), null, List.of());
		assertEquals(expected, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageFilterByAddressTypeButNoMatch() {

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");

		final Device device = new Device("DEVICE", "{\r\n  \"indoor\" : true\r\n}");
		when(deviceRepo.findAll()).thenReturn(List.of(device));
		when(addressRepo.findAllByDeviceAndAddressType(any(), any())).thenReturn(List.of());
		when(deviceRepo.findAllByNameIn(any(), any())).thenReturn(new PageImpl<>(List.of()));

		service.getPage(pageRequest, List.of(), List.of(), AddressType.MAC, List.of());
		verify(addressRepo).findAllByDeviceAndAddressType(device, AddressType.MAC);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageFilterByAddressTypeAndMatch() {

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");

		final Device device = new Device("DEVICE", "{\r\n  \"indoor\" : true\r\n}");
		when(deviceRepo.findAll()).thenReturn(List.of(device));

		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "3a:7f:12:b4:9c:e1");
		when(addressRepo.findAllByDeviceAndAddressType(any(), any())).thenReturn(List.of(deviceAddress));

		when(deviceRepo.findAllByNameIn(any(), any())).thenReturn(new PageImpl<>(List.of(device)));
		when(addressRepo.findAllByDevice(any())).thenReturn(List.of(deviceAddress));

		final Page<Entry<Device, List<DeviceAddress>>> expected = new PageImpl<Entry<Device, List<DeviceAddress>>>(List.of(Map.entry(device, List.of(deviceAddress))), pageRequest, 1);

		final Page<Entry<Device, List<DeviceAddress>>> actual = service.getPage(pageRequest, List.of(), List.of(), AddressType.MAC, List.of());
		assertEquals(expected, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageFilterByMetadataAndMatch() {

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");

		final Device device = new Device("DEVICE", "{\r\n  \"indoor\" : true\r\n}");
		when(deviceRepo.findAll()).thenReturn(List.of(device));

		when(deviceRepo.findAllByNameIn(any(), any())).thenReturn(new PageImpl<>(List.of(device)));

		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "3a:7f:12:b4:9c:e1");
		when(addressRepo.findAllByDevice(any())).thenReturn(List.of(deviceAddress));

		final Page<Entry<Device, List<DeviceAddress>>> expected = new PageImpl<Entry<Device, List<DeviceAddress>>>(List.of(Map.entry(device, List.of(deviceAddress))), pageRequest, 1);

		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("indoor", true);
		final Page<Entry<Device, List<DeviceAddress>>> actual = service.getPage(pageRequest, List.of(), List.of(), null, List.of(requirement));
		assertEquals(expected, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageFilterByMetadataButNoMatch() {

		final Device device = new Device("DEVICE", "{\r\n  \"indoor\" : false\r\n}");
		when(deviceRepo.findAll()).thenReturn(List.of(device));
		when(deviceRepo.findAllByNameIn(any(), any())).thenReturn(new PageImpl<>(List.of()));

		try (MockedStatic<MetadataRequirementsMatcher> metaMatcherMock = mockStatic(MetadataRequirementsMatcher.class)) {
			metaMatcherMock.when(() -> MetadataRequirementsMatcher.isMetadataMatch(any(), any())).thenReturn(false);

			final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");
			final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
			requirement.put("indoor", true);

			service.getPage(pageRequest, List.of(), List.of(), null, List.of(requirement));
			metaMatcherMock.verify(() -> MetadataRequirementsMatcher.isMetadataMatch(Map.of("indoor", false), requirement));
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageThrowsInternalServerError() {

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");
		when(deviceRepo.findAll(pageRequest)).thenThrow(new LockedException("error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.getPage(pageRequest, List.of(), List.of(), null, List.of()));
		assertEquals(DB_ERROR_MSG, ex.getMessage());
	}
}
