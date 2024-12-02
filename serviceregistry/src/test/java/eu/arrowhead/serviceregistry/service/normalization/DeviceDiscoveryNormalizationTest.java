package eu.arrowhead.serviceregistry.service.normalization;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

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
		final DeviceRequestDTO toNormalize = new DeviceRequestDTO(
				// name
				"\n \tdevice-NAME-1\r \n", 
				// metadata
				Map.of("key1", "value1", "key2", "value2"), 
				// addresses
				Arrays.asList(" 1.DEVICE.COM\n", " 1A-2B-3C-4D-5E-6F\n"));
		
		final NormalizedDeviceRequestDTO normalized = normalizator.normalizeDeviceRequestDTO(toNormalize);
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
		final List<MetadataRequirementDTO> metadataRequirements = new ArrayList<>(1);
		metadataRequirements.add((MetadataRequirementDTO) new MetadataRequirementDTO().put("key1", Map.of("op", "EQUALS", "value", "value1")));
		
		final DeviceLookupRequestDTO toNormalize = new DeviceLookupRequestDTO(
				// names
				List.of("\n \tdevice-NAME-1\r \n", "\n \tdevice-NAME-2\r \n"), 
				// addresses
				Arrays.asList(" 1.DEVICE.COM\n", " 2.DEVICE.ORG\n"), 
				// address type
				"hostname \n",
				// metadata requirements
				metadataRequirements);
		
		final DeviceLookupRequestDTO normalized = normalizator.normalizeDeviceLookupRequestDTO(toNormalize);
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
		assertThrows(java.lang.IllegalArgumentException.class, () -> {normalizator.normalizeDeviceName(null);});
		
		// name is empty
		assertThrows(java.lang.IllegalArgumentException.class, () -> { normalizator.normalizeDeviceName(""); });
		
		// normalize name
		assertEquals("device-name-1", normalizator.normalizeDeviceName("\n \tdevice-NAME-1\r \n"));
	}

}
