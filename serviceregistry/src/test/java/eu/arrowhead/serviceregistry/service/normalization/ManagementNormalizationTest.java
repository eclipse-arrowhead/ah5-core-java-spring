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

import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.mqtt.model.MqttInterfaceModel;
import eu.arrowhead.common.service.validation.meta.MetaOps;
import eu.arrowhead.common.service.validation.meta.MetadataRequirementTokenizer;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.DeviceQueryRequestDTO;
import eu.arrowhead.dto.DeviceRequestDTO;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.dto.ServiceDefinitionListRequestDTO;
import eu.arrowhead.dto.ServiceInstanceCreateListRequestDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceRequestDTO;
import eu.arrowhead.dto.ServiceInstanceQueryRequestDTO;
import eu.arrowhead.dto.ServiceInstanceRequestDTO;
import eu.arrowhead.dto.ServiceInstanceUpdateListRequestDTO;
import eu.arrowhead.dto.ServiceInstanceUpdateRequestDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplateListRequestDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplatePropertyDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplateQueryRequestDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplateRequestDTO;
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
	public void normalizeSystemRequestDTOsTest2NullCases() {
		// dto is null
		assertThrows(java.lang.IllegalArgumentException.class, () -> {
			normalizator.normalizeSystemRequestDTOs(null);
			});

		// system list is null
		assertThrows(java.lang.IllegalArgumentException.class, () -> {
			normalizator.normalizeSystemRequestDTOs(new SystemListRequestDTO(null));
			});

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
	@SuppressWarnings("checkstyle:magicnumber")
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
		assertThrows(java.lang.IllegalArgumentException.class, () -> {
			normalizator.normalizeRemoveSystemNames(null);
			});

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
	public void normalizeDeviceRequestDTOListTest2NullCases() {
		// list is null
		assertThrows(java.lang.IllegalArgumentException.class, () -> {
			normalizator.normalizeDeviceRequestDTOList(null);
			});

		// name is null
		assertThrows(java.lang.IllegalArgumentException.class, () -> {
			normalizator.normalizeDeviceRequestDTOList(List.of(new DeviceRequestDTO(null, Map.of("key", "value"), List.of("device.com"))));
			});

		// addresses are null
		final List<NormalizedDeviceRequestDTO> normalized = normalizator.normalizeDeviceRequestDTOList(List.of(new DeviceRequestDTO("name", Map.of("key", "value"), null)));
		assertEquals(1, normalized.size());
		assertEquals(new ArrayList<>(), normalized.get(0).addresses());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void normalizeDeviceQueryRequestDTOTest() {
		// dto is null
		assertThrows(java.lang.IllegalArgumentException.class, () -> {
			normalizator.normalizeDeviceQueryRequestDTO(null);
			});

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
		assertThrows(java.lang.IllegalArgumentException.class, () -> {
			normalizator.normalizeDeviceNames(null);
			});

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
		assertThrows(java.lang.IllegalArgumentException.class, () -> {
			normalizator.normalizeCreateServiceDefinitions(null);
			});

		// list is null
		assertThrows(java.lang.IllegalArgumentException.class, () -> {
			normalizator.normalizeCreateServiceDefinitions(new ServiceDefinitionListRequestDTO(null));
			});

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
		assertThrows(java.lang.IllegalArgumentException.class, () -> {
			normalizator.normalizeRemoveServiceDefinitions(null);
			});

		// normalize list
		final List<String> toNormalize = List.of("dEf1 ", "dEf2 ");
		final List<String> normalized = normalizator.normalizeRemoveServiceDefinitions(toNormalize);

		assertEquals(2, normalized.size());
		assertEquals("def1", normalized.get(0));
		assertEquals("def2", normalized.get(1));
	}

	// SERVICE INSTANCES

	//-------------------------------------------------------------------------------------------------
	@Test
	public void normalizeCreateServiceInstancesTest1() {

		// create ServiceInstanceInterfaceRequestDTO list for dto
		final Map<String, Object> properties = 	Map.of(
				MqttInterfaceModel.PROP_NAME_ACCESS_ADDRESSES, List.of("192.168.56.116"),
				MqttInterfaceModel.PROP_NAME_ACCESS_PORT, 8080,
				MqttInterfaceModel.PROP_NAME_TOPIC, "hello");

		final ServiceInstanceInterfaceRequestDTO interface1 = new ServiceInstanceInterfaceRequestDTO("\n generic-mqtt", "\n tcp", "\n none", properties);
		final ServiceInstanceInterfaceRequestDTO interface2 = new ServiceInstanceInterfaceRequestDTO("\n generic-mqtts", "\n ssl", "\n token_auth", properties);

		// create list elements
		final ServiceInstanceRequestDTO toNormalize1 = new ServiceInstanceRequestDTO(
				// system name
				"\n systeM-name-1",
				// service definition name
				"\n servicE-def-1",
				// version
				"1.1",
				// expires at
				"\n 2026-01-31T12:00:00Z ",
				// metadata
				Map.of("key", "value"),
				// interfaces
				List.of(interface1, interface2)
				);

		final ServiceInstanceRequestDTO toNormalize2 = new ServiceInstanceRequestDTO(
				// system name
				"\n systeM-name-2",
				// service definition name
				"\n servicE-def-2",
				// version
				"1.2",
				// expires at
				"\n 2027-01-31T12:00:00Z",
				// metadata
				Map.of("key", "value"),
				// interfaces
				List.of(interface1, interface2)
				);

		final List<ServiceInstanceRequestDTO> normalized = normalizator.normalizeCreateServiceInstances(new ServiceInstanceCreateListRequestDTO(List.of(toNormalize1, toNormalize2)));
		assertEquals(2, normalized.size());
		final List<ServiceInstanceInterfaceRequestDTO> nInterfaces1 = normalized.get(0).interfaces();
		final List<ServiceInstanceInterfaceRequestDTO> nInterfaces2 = normalized.get(1).interfaces();

		assertAll("normlaize CreateServiceInstances 1",
				// system name
				() -> assertEquals("system-name-1", normalized.get(0).systemName()),
				() -> assertEquals("system-name-2", normalized.get(1).systemName()),
				// service definition name
				() -> assertEquals("service-def-1", normalized.get(0).serviceDefinitionName()),
				() -> assertEquals("service-def-2", normalized.get(1).serviceDefinitionName()),
				// version
				() -> assertEquals("1.1.0", normalized.get(0).version()),
				() -> assertEquals("1.2.0", normalized.get(1).version()),
				// expires at
				() -> assertEquals("2026-01-31T12:00:00Z", normalized.get(0).expiresAt()),
				() -> assertEquals("2027-01-31T12:00:00Z", normalized.get(1).expiresAt()),
				// metadata -> should not change
				() -> assertEquals(Map.of("key", "value"), normalized.get(0).metadata()),
				() -> assertEquals(Map.of("key", "value"), normalized.get(1).metadata()),
				// interfaces
				() -> assertEquals(2, nInterfaces1.size()),
				() -> assertEquals(2, nInterfaces2.size()),
					// template name
				() -> assertEquals("generic-mqtt", nInterfaces1.get(0).templateName()),
				() -> assertEquals("generic-mqtts", nInterfaces1.get(1).templateName()),
				() -> assertEquals("generic-mqtt", nInterfaces2.get(0).templateName()),
				() -> assertEquals("generic-mqtts", nInterfaces2.get(1).templateName()),
					// protocol
				() -> assertEquals("tcp", nInterfaces1.get(0).protocol()),
				() -> assertEquals("ssl", nInterfaces1.get(1).protocol()),
				() -> assertEquals("tcp", nInterfaces2.get(0).protocol()),
				() -> assertEquals("ssl", nInterfaces2.get(1).protocol()),
					// policy
				() -> assertEquals("NONE", nInterfaces1.get(0).policy()),
				() -> assertEquals("TOKEN_AUTH", nInterfaces1.get(1).policy()),
					// properties (should not change, these will be normalized in the interface validator)
				() -> assertEquals(properties,  nInterfaces1.get(0).properties()),
				() -> assertEquals(properties,  nInterfaces1.get(1).properties()),
				() -> assertEquals(properties,  nInterfaces2.get(0).properties()),
				() -> assertEquals(properties,  nInterfaces2.get(1).properties()));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void normalizeCreateServiceInstancesTest2NullCases() {
		// dto is null
		assertThrows(java.lang.IllegalArgumentException.class, () -> {
			normalizator.normalizeCreateServiceInstances(null);
			});

		// instance list is null
		assertThrows(java.lang.IllegalArgumentException.class, () -> {
			normalizator.normalizeCreateServiceInstances(new ServiceInstanceCreateListRequestDTO(null));
			});

		// dto contain null members (version, expires at)
		final ServiceInstanceRequestDTO toNormalize1 = new ServiceInstanceRequestDTO("system-name-1", "service-def-1",
				// version
				null,
				"2027-01-31T12:00:00Z", Map.of("key", "value"), List.of());

		final ServiceInstanceRequestDTO toNormalize2 = new ServiceInstanceRequestDTO("system-name-2", "service-def-2", "1.0.0",
				// expires at
				null,
				Map.of("key", "value"), List.of());

		final List<ServiceInstanceRequestDTO> normalized = normalizator.normalizeCreateServiceInstances(new ServiceInstanceCreateListRequestDTO(List.of(toNormalize1, toNormalize2)));
		assertEquals(2, normalized.size());
		assertEquals("1.0.0", normalized.get(0).version());
		assertEquals("", normalized.get(1).expiresAt());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void normalizeUpdateServiceInstancesTest1() {
		// create ServiceInstanceInterfaceRequestDTO list for dto
		final Map<String, Object> properties = 	Map.of(
				MqttInterfaceModel.PROP_NAME_ACCESS_ADDRESSES, List.of("192.168.56.116"),
				MqttInterfaceModel.PROP_NAME_ACCESS_PORT, 8080,
				MqttInterfaceModel.PROP_NAME_TOPIC, "hello");

		final ServiceInstanceInterfaceRequestDTO interface1 = new ServiceInstanceInterfaceRequestDTO("\n generic-mqtt", "\n tcp", "\n none", properties);
		final ServiceInstanceInterfaceRequestDTO interface2 = new ServiceInstanceInterfaceRequestDTO("\n generic-mqtts", "\n ssl", "\n token_auth", properties);

		final ServiceInstanceUpdateRequestDTO toNormalize1 = new ServiceInstanceUpdateRequestDTO(
				// instance id
				"\n system-NAME::service-definition::1.0.0 ",
				// expires at
				"\n 2026-01-31T12:00:00Z \n",
				// metadata
				Map.of("key", "value"),
				// interfaces
				List.of(interface1, interface2));

		final ServiceInstanceUpdateRequestDTO toNormalize2 = new ServiceInstanceUpdateRequestDTO(
				// instance id
				"\n system-NAME::service-definition::1.0.1 ",
				// expires at
				"\n 2027-01-31T12:00:00Z \n",
				// metadata
				Map.of("key", "value"),
				// interfaces
				List.of(interface1, interface2));

		final List<ServiceInstanceUpdateRequestDTO> normalized = normalizator.normalizeUpdateServiceInstances(new ServiceInstanceUpdateListRequestDTO(List.of(toNormalize1, toNormalize2)));
		assertEquals(2, normalized.size());
		final List<ServiceInstanceInterfaceRequestDTO> nInterfaces1 = normalized.get(0).interfaces();
		final List<ServiceInstanceInterfaceRequestDTO> nInterfaces2 = normalized.get(1).interfaces();

		assertAll("normlaize UpdateServiceInstances 1",
				// instance id
				() -> assertEquals("system-name::service-definition::1.0.0", normalized.get(0).instanceId()),
				() -> assertEquals("system-name::service-definition::1.0.1", normalized.get(1).instanceId()),
				// expires at
				() -> assertEquals("2026-01-31T12:00:00Z", normalized.get(0).expiresAt()),
				() -> assertEquals("2027-01-31T12:00:00Z", normalized.get(1).expiresAt()),
				// metadata -> should not change
				() -> assertEquals(Map.of("key", "value"), normalized.get(0).metadata()),
				() -> assertEquals(Map.of("key", "value"), normalized.get(1).metadata()),
				// interfaces
				() -> assertEquals(2, nInterfaces1.size()),
				() -> assertEquals(2, nInterfaces2.size()),
					// template name
				() -> assertEquals("generic-mqtt", nInterfaces1.get(0).templateName()),
				() -> assertEquals("generic-mqtts", nInterfaces1.get(1).templateName()),
				() -> assertEquals("generic-mqtt", nInterfaces2.get(0).templateName()),
				() -> assertEquals("generic-mqtts", nInterfaces2.get(1).templateName()),
					// protocol
				() -> assertEquals("tcp", nInterfaces1.get(0).protocol()),
				() -> assertEquals("ssl", nInterfaces1.get(1).protocol()),
				() -> assertEquals("tcp", nInterfaces2.get(0).protocol()),
				() -> assertEquals("ssl", nInterfaces2.get(1).protocol()),
					// policy
				() -> assertEquals("NONE", nInterfaces1.get(0).policy()),
				() -> assertEquals("TOKEN_AUTH", nInterfaces1.get(1).policy()),
					// properties (should not change, these will be normalized in the interface validator)
				() -> assertEquals(properties,  nInterfaces1.get(0).properties()),
				() -> assertEquals(properties,  nInterfaces1.get(1).properties()),
				() -> assertEquals(properties,  nInterfaces2.get(0).properties()),
				() -> assertEquals(properties,  nInterfaces2.get(1).properties()));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void normalizeUpdateServiceInstancesTest2NullCases() {
		// dto is null
		assertThrows(java.lang.IllegalArgumentException.class, () -> {
			normalizator.normalizeUpdateServiceInstances(null);
			});

		// instance list is null
		assertThrows(java.lang.IllegalArgumentException.class, () -> {
			normalizator.normalizeUpdateServiceInstances(new ServiceInstanceUpdateListRequestDTO(null));
			});

		// dto contains null member (expires at)
		final ServiceInstanceUpdateRequestDTO toNormalize = new ServiceInstanceUpdateRequestDTO("system-name::service-definition::1.0.0",
				// expires at
				null,
				Map.of("key", "value"), List.of());
		final List<ServiceInstanceUpdateRequestDTO> normalized = normalizator.normalizeUpdateServiceInstances(new ServiceInstanceUpdateListRequestDTO(List.of(toNormalize)));
		assertEquals(1, normalized.size());
		assertEquals("", normalized.get(0).expiresAt());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void normalizeRemoveServiceInstancesTest() {
		// instance list is null
		assertThrows(java.lang.IllegalArgumentException.class, () -> {
			normalizator.normalizeRemoveServiceInstances(null);
			});

		// normalize list
		final List<String> toNormalize = List.of("\tsystem-NAME::service-definition::1.0.0 ", "\tsystem-NAME::service-definition::1.0.1 ");
		final List<String> normalized = normalizator.normalizeRemoveInterfaceTemplates(toNormalize);
		assertEquals("system-name::service-definition::1.0.0", normalized.get(0));
		assertEquals("system-name::service-definition::1.0.1", normalized.get(1));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void normalizeQueryServiceInstancesTest1() {
		// create metadata requirements
		final MetadataRequirementDTO metadataRequirement = new MetadataRequirementDTO();
		metadataRequirement.put("priority", Map.of(MetadataRequirementTokenizer.OP, MetaOps.LESS_THAN, MetadataRequirementTokenizer.VALUE, 100));

		// create interface property requirements
		final MetadataRequirementDTO interfacePropertyRequirement = new MetadataRequirementDTO();
		interfacePropertyRequirement.put(HttpInterfaceModel.PROP_NAME_BASE_PATH, Map.of(MetadataRequirementTokenizer.OP, MetaOps.CONTAINS, MetadataRequirementTokenizer.VALUE, "path"));

		// create dto
		final ServiceInstanceQueryRequestDTO toNormalize = new ServiceInstanceQueryRequestDTO(
				// pagination
				new PageDTO(0, 10, "asc", "id"),
				// instance ids
				List.of(" system-NAME::service-definition::1.0.0 ", " system-NAME::service-definition::1.0.1 "),
				// provides names
				List.of(" provider-NAME-1 ", " provider-NAME-2 "),
				// service definition names
				List.of(" service-DEF-name1 ", " service-DEF-name2 "),
				// versions
				List.of("1", "2"),
				// alives at
				"\n 2025-01-31T12:00:00Z \n",
				// metadata requirement list
				List.of(metadataRequirement),
				// interface template namesk
				List.of(" generic-MQTT ", " generic-MQTTS "),
				// interface property requirements list
				List.of(interfacePropertyRequirement),
				// policies
				List.of(" none ", " cert_auth "));

		// normalize dto
		final ServiceInstanceQueryRequestDTO normalized = normalizator.normalizeQueryServiceInstances(toNormalize);

		assertAll("normalize QueryServiceInstances 1",
				// pagination -> should not change
				() -> assertEquals(new PageDTO(0, 10, "asc", "id"), normalized.pagination()),
				// instance ids
				() -> assertEquals(List.of("system-name::service-definition::1.0.0", "system-name::service-definition::1.0.1"), normalized.instanceIds()),
				// provicer names
				() -> assertEquals(List.of("provider-name-1", "provider-name-2"), normalized.providerNames()),
				// service definition names
				() -> assertEquals(List.of("service-def-name1", "service-def-name2"), normalized.serviceDefinitionNames()),
				// versions
				() -> assertEquals(List.of("1.0.0", "2.0.0"), normalized.versions()),
				// alives at
				() -> assertEquals("2025-01-31T12:00:00Z", normalized.alivesAt()),
				// metadata requirement list -> should not change
				() -> assertEquals(List.of(metadataRequirement), normalized.metadataRequirementsList()),
				// interface template names
				() -> assertEquals(List.of("generic-mqtt", "generic-mqtts"), normalized.interfaceTemplateNames()),
				// interface property requirements list -> should not change
				() -> assertEquals(List.of(interfacePropertyRequirement), normalized.interfacePropertyRequirementsList()),
				// policies
				() -> assertEquals(List.of("NONE", "CERT_AUTH"), normalized.policies()));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void normalizeQueryServiceInstancesTest2NullCases() {
		// dto is null
		assertThrows(java.lang.IllegalArgumentException.class, () -> {
			normalizator.normalizeQueryServiceInstances(null);
			});

		// dto conatins null members
		final ServiceInstanceQueryRequestDTO toNormalize = new ServiceInstanceQueryRequestDTO(new PageDTO(0, 10, "asc", "id"), null, null, null, null, null, null, null, null, null);
		final ServiceInstanceQueryRequestDTO normalized = normalizator.normalizeQueryServiceInstances(toNormalize);
		assertAll("normalize eQueryServiceInstances 2",
				// instance ids
				() -> assertEquals(new ArrayList<>(), normalized.instanceIds()),
				// provicer names
				() -> assertEquals(new ArrayList<>(), normalized.providerNames()),
				// service definition names
				() -> assertEquals(new ArrayList<>(), normalized.serviceDefinitionNames()),
				// versions
				() -> assertEquals(new ArrayList<>(), normalized.versions()),
				// alives at
				() -> assertEquals("", normalized.alivesAt()),
				// metadata requirement list -> should not change
				() -> assertEquals(new ArrayList<>(), normalized.metadataRequirementsList()),
				// interface template names
				() -> assertEquals(new ArrayList<>(), normalized.interfaceTemplateNames()),
				// interface property requirements list -> should not change
				() -> assertEquals(new ArrayList<>(), normalized.interfacePropertyRequirementsList()),
				// policies
				() -> assertEquals(new ArrayList<>(), normalized.policies()));
	}

	// INTERFACE TEMPLATES

	//-------------------------------------------------------------------------------------------------
	@Test
	public void normalizeServiceInterfaceTemplateListRequestDTOTest() {
		// dto is null
		assertThrows(java.lang.IllegalArgumentException.class, () -> {
			normalizator.normalizeServiceInterfaceTemplateListRequestDTO(null);
			});

		// template list is null
		assertThrows(java.lang.IllegalArgumentException.class, () -> {
			normalizator.normalizeServiceInterfaceTemplateListRequestDTO(new ServiceInterfaceTemplateListRequestDTO(null));
			});

		// normalize dto
		final ServiceInterfaceTemplateRequestDTO toNormalize1 = new ServiceInterfaceTemplateRequestDTO(
				// name
				"\t generic-hTTp \t\n",
				// protocol
				" hTTp  ",
				// property requirements
				List.of(new ServiceInterfaceTemplatePropertyDTO(
						// name
						"\n ipAddress \t ",
						// mandatory
						false,
						// validator
						"\n minmax \n",
						// validator params
						List.of("192.168.0.0 \n", "192.168.255.255 \n")))
				);

		final ServiceInterfaceTemplateRequestDTO toNormalize2 = new ServiceInterfaceTemplateRequestDTO(
				// name
				"\t generic-hTTps \t\n",
				// protocol
				" hTTps  ",
				// property requirements
				List.of(new ServiceInterfaceTemplatePropertyDTO(
						// name
						"\n ipAddress \t ",
						// mandatory
						false,
						// validator
						"\n minmax \n",
						// validator params
						List.of("192.166.0.0 \n", "192.166.255.255 \n"))));

		final ServiceInterfaceTemplateListRequestDTO normalized = normalizator.normalizeServiceInterfaceTemplateListRequestDTO(new ServiceInterfaceTemplateListRequestDTO(List.of(toNormalize1, toNormalize2)));

		assertAll("normalize ServiceInterfaceTemplateListRequestDTO 1",
				() -> assertEquals(2, normalized.interfaceTemplates().size()),
				// name
				() -> assertEquals("generic-http", normalized.interfaceTemplates().get(0).name()),
				() -> assertEquals("generic-https", normalized.interfaceTemplates().get(1).name()),
				// protocol
				() -> assertEquals("http", normalized.interfaceTemplates().get(0).protocol()),
				() -> assertEquals("https", normalized.interfaceTemplates().get(1).protocol()),
				// property requirements
				() -> assertEquals(List.of(new ServiceInterfaceTemplatePropertyDTO("ipAddress", false, "MINMAX", List.of("192.168.0.0", "192.168.255.255"))), normalized.interfaceTemplates().get(0).propertyRequirements()),
				() -> assertEquals(List.of(new ServiceInterfaceTemplatePropertyDTO("ipAddress", false, "MINMAX", List.of("192.166.0.0", "192.166.255.255"))), normalized.interfaceTemplates().get(1).propertyRequirements()));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void normalizeServiceInterfaceTemplateQueryRequestDTOTest() {
		// dto is null
		assertEquals(new ServiceInterfaceTemplateQueryRequestDTO(null, new ArrayList<>(), new ArrayList<>()),
				normalizator.normalizeServiceInterfaceTemplateQueryRequestDTO(null));

		// dto has null members (template names, protocols)
		assertEquals(new ServiceInterfaceTemplateQueryRequestDTO(null, new ArrayList<>(), new ArrayList<>()),
				normalizator.normalizeServiceInterfaceTemplateQueryRequestDTO(new ServiceInterfaceTemplateQueryRequestDTO(null, null, null)));

		// normalize dto
		final ServiceInterfaceTemplateQueryRequestDTO toNormalize = new ServiceInterfaceTemplateQueryRequestDTO(
				// pagination
				new PageDTO(0, 10, "asc", "id"),
				// template names
				List.of("generic-MQTT \n", "generic-MQTTS \n"),
				// protocols
				List.of("TCP \n", "SSL \n"));

		final ServiceInterfaceTemplateQueryRequestDTO normalized = normalizator.normalizeServiceInterfaceTemplateQueryRequestDTO(toNormalize);
		assertAll("normalize ServiceInterfaceTemplateQueryRequestDTO",
				// pagination -> should not change
				() -> assertEquals(new PageDTO(0, 10, "asc", "id"), normalized.pagination()),
				// template names
				() -> assertEquals(List.of("generic-mqtt", "generic-mqtts"), normalized.templateNames()),
				// protocols
				() -> assertEquals(List.of("tcp", "ssl"), normalized.protocols()));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void normalizeRemoveInterfaceTemplatesTest() {
		// name list is null
		assertThrows(java.lang.IllegalArgumentException.class, () -> {
			normalizator.normalizeRemoveInterfaceTemplates(null);
			});

		// normalize list
		final List<String> toNormalize = List.of("generic-MQTT \n", "generic-MQTTS \n");
		final List<String> normalized = normalizator.normalizeRemoveInterfaceTemplates(toNormalize);
		assertEquals("generic-mqtt", normalized.get(0));
		assertEquals("generic-mqtts", normalized.get(1));
	}
}
