package eu.arrowhead.serviceregistry.service.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.time.ZonedDateTime;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.fasterxml.jackson.core.type.TypeReference;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.DeviceListResponseDTO;
import eu.arrowhead.dto.DeviceResponseDTO;
import eu.arrowhead.dto.ServiceDefinitionListResponseDTO;
import eu.arrowhead.dto.ServiceDefinitionResponseDTO;
import eu.arrowhead.dto.SystemListResponseDTO;
import eu.arrowhead.dto.SystemResponseDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.jpa.entity.Device;
import eu.arrowhead.serviceregistry.jpa.entity.DeviceAddress;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceDefinition;
import eu.arrowhead.serviceregistry.jpa.entity.System;
import eu.arrowhead.serviceregistry.jpa.entity.SystemAddress;

public class DTOConverterTest {

	//=================================================================================================
	// members

	private final DTOConverter converter = new DTOConverter();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testConvertDeviceAndDeviceAddressEntriesToDTOWithAddresses() {

		// mocking Utilities to mock the creation time
		try (MockedStatic<Utilities> utilitiesMock = Mockito.mockStatic(Utilities.class)) {
			utilitiesMock.when(() -> Utilities.utcNow()).thenReturn(ZonedDateTime.parse("2024-11-04T01:53:02Z"));
			utilitiesMock.when(() -> Utilities.fromJson(eq("{ \"indoor\": true }"), any(TypeReference.class))).thenReturn(Map.of("indoor", true));
			utilitiesMock.when(() -> Utilities.convertZonedDateTimeToUTCString(ZonedDateTime.parse("2024-11-04T01:53:02Z"))).thenReturn("2024-11-04T01:53:02Z");
			utilitiesMock.when(() -> Utilities.isEmpty(eq(List.of()))).thenReturn(true);

			// device with addresses
		    final Device device1 = new Device("TEST_DEVICE", "{ \"indoor\": true }");
		    device1.onCreate();
		    final DeviceAddress address1 = new DeviceAddress(device1, AddressType.IPV4, "192.168.2.2");
		    final DeviceAddress address2 = new DeviceAddress(device1, AddressType.MAC, "00:1a:2b:3c:4d:5e");

		    // device without addresses
		    final Device device2 = new Device("ALARM", "{ \"indoor\": true }");
		    device2.onCreate();

			final DeviceResponseDTO expectedDTO1 = new DeviceResponseDTO(
					"TEST_DEVICE",
					Map.of("indoor", true),
					List.of(new AddressDTO("IPV4", "192.168.2.2"), new AddressDTO("MAC", "00:1a:2b:3c:4d:5e")),
					"2024-11-04T01:53:02Z",
					"2024-11-04T01:53:02Z");

			final DeviceResponseDTO expectedDTO2 = new DeviceResponseDTO(
					"TEST_DEVICE",
					Map.of("indoor", true),
					null,
					"2024-11-04T01:53:02Z",
					"2024-11-04T01:53:02Z");

			DeviceListResponseDTO converted = converter.convertDeviceAndDeviceAddressEntriesToDTO(List.of(Map.entry(device1, List.of(address1, address2)), Map.entry(device1, List.of())), 2);
			assertEquals(new DeviceListResponseDTO(List.of(expectedDTO1, expectedDTO2), 2), converted);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertServiceDefinitionEntityListToDTO() {

		// mocking Utilities to mock the creation time
		try (MockedStatic<Utilities> utilitiesMock = Mockito.mockStatic(Utilities.class)) {
			utilitiesMock.when(() -> Utilities.utcNow()).thenReturn(ZonedDateTime.parse("2024-11-04T01:53:02Z"));
			utilitiesMock.when(() -> Utilities.convertZonedDateTimeToUTCString(ZonedDateTime.parse("2024-11-04T01:53:02Z"))).thenReturn("2024-11-04T01:53:02Z");

			final ServiceDefinition entity = new ServiceDefinition("temperatureInfo");
			entity.onCreate();

			final ServiceDefinitionResponseDTO expectedDTO = new ServiceDefinitionResponseDTO("temperatureInfo", "2024-11-04T01:53:02Z", "2024-11-04T01:53:02Z");
			final ServiceDefinitionListResponseDTO expected = new ServiceDefinitionListResponseDTO(List.of(expectedDTO), 1);

			final ServiceDefinitionListResponseDTO converted = converter.convertServiceDefinitionEntityListToDTO(List.of(entity));
			assertEquals(expected, converted);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertServiceDefinitionEntityPageToDTO() {

		// mocking Utilities to mock the creation time
		try (MockedStatic<Utilities> utilitiesMock = Mockito.mockStatic(Utilities.class)) {
			utilitiesMock.when(() -> Utilities.utcNow()).thenReturn(ZonedDateTime.parse("2024-11-04T01:53:02Z"));
			utilitiesMock.when(() -> Utilities.convertZonedDateTimeToUTCString(ZonedDateTime.parse("2024-11-04T01:53:02Z"))).thenReturn("2024-11-04T01:53:02Z");

			final ServiceDefinition entity = new ServiceDefinition("temperatureInfo");
			entity.onCreate();

			final ServiceDefinitionResponseDTO expectedDTO = new ServiceDefinitionResponseDTO("temperatureInfo", "2024-11-04T01:53:02Z", "2024-11-04T01:53:02Z");
			final ServiceDefinitionListResponseDTO expected = new ServiceDefinitionListResponseDTO(List.of(expectedDTO), 10);

			final ServiceDefinitionListResponseDTO converted = converter.convertServiceDefinitionEntityPageToDTO(new PageImpl<>(List.of(entity), PageRequest.of(0, 1), 10));
			assertEquals(expected, converted);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testConvertSystemTripletPageToDTO() {

		// mocking Utilities to mock the creation time
		try (MockedStatic<Utilities> utilitiesMock = Mockito.mockStatic(Utilities.class)) {
			utilitiesMock.when(() -> Utilities.utcNow()).thenReturn(ZonedDateTime.parse("2024-11-04T01:53:02Z"));
			utilitiesMock.when(() -> Utilities.convertZonedDateTimeToUTCString(ZonedDateTime.parse("2024-11-04T01:53:02Z"))).thenReturn("2024-11-04T01:53:02Z");
			utilitiesMock.when(() -> Utilities.fromJson(eq("{ \"indoor\": true }"), any(TypeReference.class))).thenReturn(Map.of("indoor", true));
			utilitiesMock.when(() -> Utilities.fromJson(eq("{ \"size\": 200 }"), any(TypeReference.class))).thenReturn(Map.of("size", 200));

			// system with device that has address

			final System system1 = new System("TemperatureProvider", "{ \"size\": 200 }", "1.0.1");
			system1.onCreate();
			final SystemAddress systemAddress1 = new SystemAddress(system1, AddressType.IPV4, "192.168.100.8");

		    final Device device1 = new Device("TEST_DEVICE", "{ \"indoor\": true }");
		    device1.onCreate();
		    final DeviceAddress deviceAddress1 = new DeviceAddress(device1, AddressType.MAC, "00:1a:2b:3c:4d:5e");

		    // system with device that has no address

			final System system2 = new System("AlertProvider", "{ \"size\": 200 }", "1.0.1");
			system2.onCreate();
			final SystemAddress systemAddress2 = new SystemAddress(system2, AddressType.IPV4, "192.168.100.9");

		    final Device device2 = new Device("ALARM", "{ \"indoor\": true }");
		    device2.onCreate();

			// system without device
			final System system3 = new System("TemperatureManager", "{ \"size\": 200 }", "1.0.1");
			system3.onCreate();
			final SystemAddress systemAddress3 = new SystemAddress(system3, AddressType.IPV4, "192.168.100.10");

			final Page<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> toConvert = new PageImpl<>(List.of(
					Triple.of(system1, List.of(systemAddress1), Map.entry(device1, List.of(deviceAddress1))),
					Triple.of(system2, List.of(systemAddress2), new AbstractMap.SimpleImmutableEntry<>(device2, null)),
					Triple.of(system3, List.of(systemAddress3), null)),
					PageRequest.of(0, 3),
					10);

			final DeviceResponseDTO expectedDevice1 = new DeviceResponseDTO(
					"TEST_DEVICE",
					Map.of("indoor", true),
					List.of(new AddressDTO("MAC", "00:1a:2b:3c:4d:5e")),
					"2024-11-04T01:53:02Z",
					"2024-11-04T01:53:02Z");

			final SystemResponseDTO expectedDTO1 = new SystemResponseDTO(
					"TemperatureProvider",
					Map.of("size", 200),
					"1.0.1",
					List.of(new AddressDTO("IPV4", "192.168.100.8")),
					expectedDevice1,
					"2024-11-04T01:53:02Z",
					"2024-11-04T01:53:02Z");

			final DeviceResponseDTO expectedDevice2 = new DeviceResponseDTO(
					"ALARM",
					Map.of("indoor", true),
					null,
					"2024-11-04T01:53:02Z",
					"2024-11-04T01:53:02Z");

			final SystemResponseDTO expectedDTO2 = new SystemResponseDTO(
					"AlertProvider",
					Map.of("size", 200),
					"1.0.1",
					List.of(new AddressDTO("IPV4", "192.168.100.9")),
					expectedDevice2,
					"2024-11-04T01:53:02Z",
					"2024-11-04T01:53:02Z");

			final SystemResponseDTO expectedDTO3 = new SystemResponseDTO(
					"TemperatureManager",
					Map.of("size", 200),
					"1.0.1",
					List.of(new AddressDTO("IPV4", "192.168.100.10")),
					null,
					"2024-11-04T01:53:02Z",
					"2024-11-04T01:53:02Z");

			final SystemListResponseDTO expected = new SystemListResponseDTO(List.of(expectedDTO1, expectedDTO2, expectedDTO3), 10);

			final SystemListResponseDTO converted = converter.convertSystemTripletPageToDTO(toConvert);
			assertEquals(expected, converted);
		}
	}
}
