package eu.arrowhead.serviceregistry.service.normalization;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import eu.arrowhead.common.service.validation.meta.MetaOps;
import eu.arrowhead.common.service.validation.meta.MetadataRequirementTokenizer;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.DeviceQueryRequestDTO;
import eu.arrowhead.dto.DeviceRequestDTO;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.dto.ServiceDefinitionListRequestDTO;
import eu.arrowhead.dto.SystemListRequestDTO;
import eu.arrowhead.dto.SystemQueryRequestDTO;
import eu.arrowhead.dto.SystemRequestDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.service.dto.NormalizedDeviceRequestDTO;
import eu.arrowhead.serviceregistry.service.dto.NormalizedSystemRequestDTO;

@SpringBootTest
public class ManagementNormalizationTest {
	
	//=================================================================================================
	// members
	
	@Autowired
	private ManagementNormalization normalizator;
	
	//=================================================================================================
	// methods
	
	// SYSTEMS
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void normalizeSystemRequestDTOsTest1() {
		// create dtos
		final SystemRequestDTO toNormalize1 = new SystemRequestDTO(
				" \tsysTEM-namE1 \n ",
				// metadata
				Map.of("key", "value"),
				// version
				"1.3\r\r",
				// addresses
				List.of("2001:db8:85a3::8a2e:0370:7334\r\n", " 192.168.1.1\n \t"),
				// device name
				"\ndevice-nAME1\n");
		
		final SystemRequestDTO toNormalize2 = new SystemRequestDTO(
				" \tsysTEM-namE2 \n ",
				// metadata
				Map.of("key", "value"),
				// version
				"1.3\r\r",
				// addresses
				List.of("2001:db8:85a3::8a2e:0370:7335\r\n", " 192.168.1.2\n \t"),
				// device name
				"\ndevice-nAME2\n");
		
		// normalize dto list
		final List<NormalizedSystemRequestDTO> normalized = normalizator.normalizeSystemRequestDTOs(new SystemListRequestDTO(List.of(toNormalize1, toNormalize2)));
		
		assertAll("normalize SystemRequestDTOs 1",
				() -> assertEquals(2, normalized.size()),
				// name
				() -> assertEquals("system-name1", normalized.get(0).name()),
				() -> assertEquals("system-name2", normalized.get(1).name()),
				// metadata (should not change)
				() -> assertEquals(Map.of("key", "value"), normalized.get(0).metadata()),
				() -> assertEquals(Map.of("key", "value"), normalized.get(1).metadata()),
				// version
				() -> assertEquals("1.3.0", normalized.get(0).version()),
				() -> assertEquals("1.3.0", normalized.get(1).version()),
				// addresses
				() -> assertEquals(List.of(new AddressDTO("IPV6", "2001:0db8:85a3:0000:0000:8a2e:0370:7334"), new AddressDTO("IPV4", "192.168.1.1")), normalized.get(0).addresses()),
				() -> assertEquals(List.of(new AddressDTO("IPV6", "2001:0db8:85a3:0000:0000:8a2e:0370:7335"), new AddressDTO("IPV4", "192.168.1.2")), normalized.get(1).addresses()),
				// device name
				() -> assertEquals("device-name1", normalized.get(0).deviceName()),
				() -> assertEquals("device-name2", normalized.get(1).deviceName()));
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void normalizeSystemRequestDTOsTest2_NullCases() {
		// dto is null
		assertThrows(java.lang.IllegalArgumentException.class, () -> {normalizator.normalizeSystemRequestDTOs(null);});
		
		// system list is null
		assertThrows(java.lang.IllegalArgumentException.class, () -> {normalizator.normalizeSystemRequestDTOs(new SystemListRequestDTO(null));});
		
		// dto contains null members (version, addresses)
		final SystemRequestDTO toNormalize1 = new SystemRequestDTO("system-name", Map.of("key", "value"), null, null, "device-name");
		final SystemRequestDTO toNormalize2 = new SystemRequestDTO("system-name", Map.of("key", "value"), null, null, "device-name");
		final List<NormalizedSystemRequestDTO> normalized = normalizator.normalizeSystemRequestDTOs(new SystemListRequestDTO(List.of(toNormalize1, toNormalize2)));
	
		assertAll("normalize SystemRequestDTOs 2",
				() -> assertEquals(2, normalized.size()),
				// version
				() -> assertEquals("1.0.0", normalized.get(0).version()),
				() -> assertEquals("1.0.0", normalized.get(1).version()),
				// addresses
				() -> assertEquals(new ArrayList<>(), normalized.get(0).addresses()),
				() -> assertEquals(new ArrayList<>(), normalized.get(1).addresses()));
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void normalizeSystemQueryRequestDTOTest() {
		// dto is null
		assertEquals(new SystemQueryRequestDTO(null, null, null, null, null, null, null), normalizator.normalizeSystemQueryRequestDTO(null));
		
		// normalize dto
		
		// create metadata requirements
		final MetadataRequirementDTO metadataRequirement1 = new MetadataRequirementDTO();
		metadataRequirement1.put("priority", Map.of(MetadataRequirementTokenizer.OP, MetaOps.LESS_THAN, MetadataRequirementTokenizer.VALUE, 100));
		final MetadataRequirementDTO metadataRequirement2 = new MetadataRequirementDTO();
		metadataRequirement1.put("priority", Map.of(MetadataRequirementTokenizer.OP, MetaOps.NOT_EQUALS, MetadataRequirementTokenizer.VALUE, 50));
		
		final SystemQueryRequestDTO toNormalize = new SystemQueryRequestDTO(
				// pagination
				new PageDTO(0, 10, "asc", "id"), 
				// system names
				List.of(" system-namE1 ", " system-namE2 "), 
				// addresses
				List.of(" 192.168.1.2\n \t", " 192.168.1.1\n \t"), 
				// address type
				" ipv4 \n",
				// metadata requirement list
				List.of(metadataRequirement1, metadataRequirement2), 
				// versions
				List.of(" 1 ", " 2 "), 
				// device names
				List.of(" device-namE1 ", " device-namE2 "));
		
		final SystemQueryRequestDTO normalized = normalizator.normalizeSystemQueryRequestDTO(toNormalize);
		
		assertAll("normalize SystemQueryRequestDTO",
				// paginations -> should not change
				() -> assertEquals(new PageDTO(0, 10, "asc", "id"), normalized.pagination()),
				// system names
				() -> assertEquals(List.of("system-name1", "system-name2"), normalized.systemNames()),
				// addresses
				() -> assertEquals(List.of("192.168.1.2", "192.168.1.1"), normalized.addresses()),
				// address type
				() -> assertEquals("ipv4", normalized.addressType()),
				// metadata requirement list -> should not change
				() -> assertEquals(List.of(metadataRequirement1, metadataRequirement2), normalized.metadataRequirementList()),
				// versions
				() -> assertEquals(List.of("1.0.0", "2.0.0"), normalized.versions()),
				// device names
				() -> assertEquals(List.of("device-name1", "device-name2"), normalized.deviceNames()));
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void normalizeRemoveSystemNamesTest() {
		// dto is null
		assertThrows(java.lang.IllegalArgumentException.class, () -> {normalizator.normalizeRemoveSystemNames(null);});
		
		// normalize list
		final List<String> toNormalize = List.of("namE1 ", "namE2", "", "\n \t");
		final List<String> normalized = normalizator.normalizeRemoveSystemNames(toNormalize);
		assertEquals(List.of("name1", "name2"), normalized);
	}
	
	// DEVICES
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void normalizeDeviceRequestDTOListTest1() {
		final DeviceRequestDTO toNormalize1 = new DeviceRequestDTO(
				// name
				"\n \tdevice-NAME-1\r \n", 
				// metadata
				Map.of("key1", "value1", "key2", "value2"), 
				// addresses
				Arrays.asList(" 1.DEVICE.COM\n", " 1A-2B-3C-4D-5E-6F\n"));
		
		final DeviceRequestDTO toNormalize2 = new DeviceRequestDTO(
				// name
				"\n \tdevice-NAME-2\r \n", 
				// metadata
				Map.of("key1", "value1", "key2", "value2"), 
				// addresses
				Arrays.asList(" 2.DEVICE.COM\n", " 1A-2B-3C-4D-5E-70\n"));
		
		final List<NormalizedDeviceRequestDTO> normalized = normalizator.normalizeDeviceRequestDTOList(List.of(toNormalize1, toNormalize2));
		
		assertAll("normalize DeviceRequestDTOList 1",
				() -> assertEquals(2, normalized.size()),
				// name
				() -> assertEquals("device-name-1", normalized.get(0).name()), 
				() -> assertEquals("device-name-2", normalized.get(1).name()), 
				// metadata -> should not change
				() -> assertEquals(Map.of("key1", "value1", "key2", "value2"), normalized.get(0).metadata()),
				() -> assertEquals(Map.of("key1", "value1", "key2", "value2"), normalized.get(0).metadata()),
				// addresses
				() -> assertEquals(List.of(
						new AddressDTO(String.valueOf(AddressType.HOSTNAME), "1.device.com"),
						new AddressDTO(String.valueOf(AddressType.MAC), "1a:2b:3c:4d:5e:6f")), normalized.get(0).addresses()),
				() -> assertEquals(List.of(
						new AddressDTO(String.valueOf(AddressType.HOSTNAME), "2.device.com"),
						new AddressDTO(String.valueOf(AddressType.MAC), "1a:2b:3c:4d:5e:70")), normalized.get(1).addresses()));
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void normalizeDeviceRequestDTOListTest2_NullCases() {
		// list is null
		assertThrows(java.lang.IllegalArgumentException.class, () -> {normalizator.normalizeDeviceRequestDTOList(null);});
		
		// name is null
		assertThrows(java.lang.IllegalArgumentException.class, () -> {normalizator.normalizeDeviceRequestDTOList(List.of(new DeviceRequestDTO(null, Map.of("key", "value"), List.of("device.com"))));});
		
		// addresses are null
		final List<NormalizedDeviceRequestDTO> normalized = normalizator.normalizeDeviceRequestDTOList(List.of(new DeviceRequestDTO("name", Map.of("key", "value"), null)));
		assertEquals(1, normalized.size());
		assertEquals(new ArrayList<>(), normalized.get(0).addresses());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void normalizeDeviceQueryRequestDTOTest() {
		// dto is null
		assertThrows(java.lang.IllegalArgumentException.class, () -> {normalizator.normalizeDeviceQueryRequestDTO(null);});
		
		// normalize dto
		
		final List<MetadataRequirementDTO> metadataRequirements = new ArrayList<>(1);
		metadataRequirements.add((MetadataRequirementDTO) new MetadataRequirementDTO().put("key1", Map.of("op", "EQUALS", "value", "value1")));
		
		final DeviceQueryRequestDTO toNormalize = new DeviceQueryRequestDTO(
				// pagination
				new PageDTO(0, 10, "asc", "id"),
				// device names
				List.of("\tdevice-NAME-1\r", "\tdevice-NAME-2\r"),
				// addresses
				List.of("1A:2b:3c:4d:5e:70 ", "1A:2b:3c:4d:5e:71 "),
				// address type
				"ipv6 \n",
				// metadata requirement list
				metadataRequirements);
		
		final DeviceQueryRequestDTO normalized = normalizator.normalizeDeviceQueryRequestDTO(toNormalize);
		
		assertAll("normalize DeviceQueryRequestDTO",
				// names
				() -> assertEquals(List.of("device-name-1", "device-name-2"), normalized.deviceNames()),
				// addresses
				() -> assertEquals(List.of("1a:2b:3c:4d:5e:70", "1a:2b:3c:4d:5e:71"), normalized.addresses()),
				// address type
				() -> assertEquals("IPV6", normalized.addressType()),
				// requirements (sould stay the same)
				() -> assertEquals(metadataRequirements, normalized.metadataRequirementList())
				);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void normalizeDeviceNamesTest() {
		// dto is null
		assertThrows(java.lang.IllegalArgumentException.class, () -> {normalizator.normalizeDeviceNames(null);});
		
		// normalize list
		final List<String> toNormalize = List.of("namE1 ", "namE2", "", "\n \t");
		final List<String> normalized = normalizator.normalizeDeviceNames(toNormalize);
		assertEquals(List.of("name1", "name2"), normalized);
	}
	
	// SERVICE DEFINITIONS
	
	//-------------------------------------------------------------------------------------------------
	@Test	
	public void normalizeCreateServiceDefinitionsTest() {
		// dto is null
		assertThrows(java.lang.IllegalArgumentException.class, () -> {normalizator.normalizeCreateServiceDefinitions(null);});
		
		// list is null
		assertThrows(java.lang.IllegalArgumentException.class, () -> {normalizator.normalizeCreateServiceDefinitions(new ServiceDefinitionListRequestDTO(null));});
		
		// normalize dto
		final ServiceDefinitionListRequestDTO toNormalize = new ServiceDefinitionListRequestDTO(List.of("dEf1 ", "dEf2 "));
		final List<String> normalized = normalizator.normalizeCreateServiceDefinitions(toNormalize);
		
		assertEquals(2, normalized.size());
		assertEquals("def1", normalized.get(0));
		assertEquals("def2", normalized.get(1));
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void normalizeRemoveServiceDefinitionsTest() {
		// list is null
		assertThrows(java.lang.IllegalArgumentException.class, () -> {normalizator.normalizeRemoveServiceDefinitions(null);});
		
		// normalize list
		final List<String> toNormalize = List.of("dEf1 ", "dEf2 ");
		final List<String> normalized = normalizator.normalizeRemoveServiceDefinitions(toNormalize);
		
		assertEquals(2, normalized.size());
		assertEquals("def1", normalized.get(0));
		assertEquals("def2", normalized.get(1));
	}
	
	// SERVICE INSTANCES
	
	// INTERFACE TEMPLATES
}
