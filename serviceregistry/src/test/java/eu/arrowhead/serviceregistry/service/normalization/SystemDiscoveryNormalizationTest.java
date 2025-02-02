package eu.arrowhead.serviceregistry.service.normalization;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.SystemLookupRequestDTO;
import eu.arrowhead.dto.SystemRequestDTO;
import eu.arrowhead.serviceregistry.service.dto.NormalizedSystemRequestDTO;

@SpringBootTest
public class SystemDiscoveryNormalizationTest {
	
	//=================================================================================================
	// members
	
	@Autowired
	private SystemDiscoveryNormalization normalizator;
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void normalizeSystemRequestDTOTest1() {
		final SystemRequestDTO toNormalize = new SystemRequestDTO(
				// name
				" \tsysTEM-namE \n ",
				// metadata
				Map.of("key", "value"),
				// version
				"1.3\r\r",
				// addresses
				List.of("2001:db8:85a3::8a2e:0370:7334\r\n", " 192.168.1.1\n \t"),
				// device name
				"\ndevice-nAME\n");
		
		final NormalizedSystemRequestDTO normalized = normalizator.normalizeSystemRequestDTO(toNormalize);
		
		assertAll("normalize SystemRequestDTO",
				// name
				() -> assertEquals("system-name", normalized.name()),
				// metadata (should not change)
				() -> assertEquals(Map.of("key", "value"), normalized.metadata()),
				// version
				() -> assertEquals("1.3.0", normalized.version()),
				// addresses
				() -> assertEquals(List.of(new AddressDTO("IPV6", "2001:0db8:85a3:0000:0000:8a2e:0370:7334"), new AddressDTO("IPV4", "192.168.1.1")), normalized.addresses()),
				// device name
				() -> assertEquals("device-name", normalized.deviceName()));
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void normalizeSystemRequestDTOTest2_NullCases() {
		// dto is null
		assertThrows(java.lang.IllegalArgumentException.class, () -> {normalizator.normalizeSystemRequestDTO(null);});
		
		// dto contains null members (expires at, interfaces)
		final SystemRequestDTO toNormalize = new SystemRequestDTO(
				// name
				"system-name",
				// metadata
				Map.of("key", "value"),
				// version
				null,
				// addresses
				null,
				// device name
				"device-name");
		
		final NormalizedSystemRequestDTO normalized = normalizator.normalizeSystemRequestDTO(toNormalize);
		
		assertAll("normalize SystemRequestDTO",
				// version
				() -> assertEquals("1.0.0", normalized.version()),
				// addresses
				() -> assertEquals(new ArrayList<>(), normalized.addresses()));
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void normalizeSystemLookupRequestDTO() {
		// dto is null
		assertDoesNotThrow(() -> normalizator.normalizeSystemLookupRequestDTO(null));
		
		
		final List<MetadataRequirementDTO> metadataRequirements = new ArrayList<>(1);
		metadataRequirements.add((MetadataRequirementDTO) new MetadataRequirementDTO().put("key1", Map.of("op", "EQUALS", "value", "value1")));
	
		// normalize dto
		final SystemLookupRequestDTO toNormalize = new SystemLookupRequestDTO(
				// names
				List.of(" system-NAME-1\r\n", " system-NAME-2\r\n"), 
				// addresses
				List.of("2001:db8:85a3::8a2e:370:7334\r\n", "    2001:db8:85a3::8a2e:370:1\r\n"), 
				// address type
				" ipv6\r\n", 
				// metadata requirements
				metadataRequirements, 
				// versions
				List.of("", " 1 ", "1.1\r\n"), 
				// device names
				List.of(" device-NAME-1\r\n", " device-NAME-2\r\n"));
		
		final SystemLookupRequestDTO normalized = normalizator.normalizeSystemLookupRequestDTO(toNormalize);
		
		assertAll("normalize SystemLookupRequestDTO",
				// names
				() -> assertEquals(List.of("system-name-1", "system-name-2"), normalized.systemNames()),
				// addresses
				() -> assertEquals(List.of("2001:0db8:85a3:0000:0000:8a2e:0370:7334", "2001:0db8:85a3:0000:0000:8a2e:0370:0001"), normalized.addresses()),
				// address type
				() -> assertEquals("IPV6", normalized.addressType()),
				// metadata requirements (should not change during normaization)
				() -> assertEquals(metadataRequirements, normalized.metadataRequirementList()),
				// versions
				() -> assertEquals(List.of("1.0.0", "1.0.0", "1.1.0"), normalized.versions()),
				// device names
				() -> assertEquals(List.of("device-name-1", "device-name-2"), normalized.deviceNames()));
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void normalizeRevokeSystemNameTest() {
		// name is null
		assertThrows(java.lang.IllegalArgumentException.class, () -> {normalizator.normalizeRevokeSystemName(null);});
		
		// name is empty
		assertThrows(java.lang.IllegalArgumentException.class, () -> { normalizator.normalizeRevokeSystemName("");});
		
		// normalize name
		assertEquals("system-name-1", normalizator.normalizeRevokeSystemName("\n \tsystem-NAME-1\r \n"));
	}
}
