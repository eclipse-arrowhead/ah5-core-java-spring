package eu.arrowhead.serviceregistry.service.normalization;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.DeviceLookupRequestDTO;
import eu.arrowhead.dto.DeviceRequestDTO;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.service.dto.NormalizedDeviceRequestDTO;

import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest
public class DeviceDiscoveryNormalizationTest {
	
	//=================================================================================================
	// members
	
	@Autowired
	private DeviceDiscoveryNormalization normalizator;
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void normalizeDeviceRequestDTOTest() {
		// dto is null
		assertThrows(java.lang.IllegalArgumentException.class, () -> {normalizator.normalizeDeviceRequestDTO(null);});
		
		// dto name is empty
		assertThrows(java.lang.IllegalArgumentException.class, () -> {normalizator.normalizeDeviceRequestDTO(new DeviceRequestDTO(null, null, null));});
		assertThrows(java.lang.IllegalArgumentException.class, () -> {normalizator.normalizeDeviceRequestDTO(new DeviceRequestDTO("", null, null));});
		
		// normalize dto
		DeviceRequestDTO toNormalize = new DeviceRequestDTO(
				"\n \tdevice-NAME-1\r \n", 
				Map.of("key1", "value1", "key2", "value2"), 
				Arrays.asList(" 1.DEVICE.COM\n", " 1A-2B-3C-4D-5E-6F\n"));
		
		NormalizedDeviceRequestDTO normalized = normalizator.normalizeDeviceRequestDTO(toNormalize);
		assertAll("normalize DeviceRequestDTO",
				// name
				() -> assertEquals("device-name-1", normalized.name()), 
				// metadata
				() -> assertEquals(Map.of("key1", "value1", "key2", "value2"), normalized.metadata()),
				// addresses
				() -> assertEquals(List.of(
						new AddressDTO(String.valueOf(AddressType.HOSTNAME), "1.device.com"),
						new AddressDTO(String.valueOf(AddressType.MAC), "1a:2b:3c:4d:5e:6f")), normalized.addresses()));
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void normalizeDeviceLookupRequestDTOTest() {
		// dto is null
		assertThrows(java.lang.IllegalArgumentException.class, () -> {normalizator.normalizeDeviceLookupRequestDTO(null);});
		
		// normalize dto
		List<MetadataRequirementDTO> metadataRequirements = new ArrayList<>(1);
		metadataRequirements.add((MetadataRequirementDTO) new MetadataRequirementDTO().put("key.subkey", Map.of("op", "LESS_THAN", "value", 10)));
		
		DeviceLookupRequestDTO toNormalize = new DeviceLookupRequestDTO(
				List.of("\n \tdevice-NAME-1\r \n", "\n \tdevice-NAME-2\r \n"), 
				Arrays.asList(" 1.DEVICE.COM\n", " 2.DEVICE.ORG\n"), 
				"hostname \n",
				metadataRequirements);
		
		DeviceLookupRequestDTO normalized = normalizator.normalizeDeviceLookupRequestDTO(toNormalize);
		assertAll("normalize DeviceLookupRequestDTO",
				// names
				() -> assertEquals(List.of("device-name-1", "device-name-2"), normalized.deviceNames()),
				// addresses
				() -> assertEquals(List.of("1.device.com", "2.device.org"), normalized.addresses()),
				// address type
				() -> assertEquals("HOSTNAME", normalized.addressType()),
				// requirements (sould stay the same)
				() -> assertEquals(metadataRequirements, normalized.metadataRequirementList())
				);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void normalizeDeviceNameTest() {
		// name is null
		assertThrows(java.lang.IllegalArgumentException.class, () -> {normalizator.normalizeDeviceLookupRequestDTO(null);});
		
		// normalize name
		assertEquals("device-name-1", normalizator.normalizeDeviceName("\n \tdevice-NAME-1\r \n"));
		assertThrows(java.lang.IllegalArgumentException.class, () -> { normalizator.normalizeDeviceName(""); });
	}

}
