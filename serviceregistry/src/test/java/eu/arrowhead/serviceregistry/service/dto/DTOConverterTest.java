package eu.arrowhead.serviceregistry.service.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.type.TypeReference;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.DeviceListResponseDTO;
import eu.arrowhead.dto.DeviceResponseDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.jpa.entity.Device;
import eu.arrowhead.serviceregistry.jpa.entity.DeviceAddress;

public class DTOConverterTest {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testConvertDeviceAndDeviceAddressEntriesToDTOWithAddresses() {

		final DTOConverter converter = new DTOConverter();

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
}
