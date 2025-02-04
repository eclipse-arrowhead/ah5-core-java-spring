package eu.arrowhead.serviceregistry.service.normalization;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.mqtt.model.MqttInterfaceModel;
import eu.arrowhead.common.service.validation.meta.MetaOps;
import eu.arrowhead.common.service.validation.meta.MetadataRequirementTokenizer;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceRequestDTO;
import eu.arrowhead.dto.ServiceInstanceLookupRequestDTO;
import eu.arrowhead.dto.ServiceInstanceRequestDTO;

@SpringBootTest
public class ServiceDiscoveryNormalizationTest {

	//=================================================================================================
	// members

	@Autowired
	private ServiceDiscoveryNormalization normalizator;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void normalizeServiceInstanceRequestDTOTest1() {

		// create ServiceInstanceInterfaceRequestDTO list for dto
		final Map<String, Object> properties = 	Map.of(
				MqttInterfaceModel.PROP_NAME_ACCESS_ADDRESSES, List.of("192.168.56.116"),
				MqttInterfaceModel.PROP_NAME_ACCESS_PORT, 8080,
				MqttInterfaceModel.PROP_NAME_TOPIC, "hello");

		final ServiceInstanceInterfaceRequestDTO interface1 = new ServiceInstanceInterfaceRequestDTO("\n generic-mqtt", "\n tcp", "\n none", properties);
		final ServiceInstanceInterfaceRequestDTO interface2 = new ServiceInstanceInterfaceRequestDTO("\n generic-mqtts", "\n ssl", "\n token_auth", properties);

		// normalize dto
		final ServiceInstanceRequestDTO toNormalize = new ServiceInstanceRequestDTO(
				// system name
				" \tsYSTEM-NAME\n \n ",
				// service definiton name
				" \tsERVICE-DEFINITION-NAME\n \n ",
				// version
				" 1\n",
				// expires at
				"\n 2025-01-31T12:00:00Z \n",
				// metadata
				Map.of("key", "value"),
				// interfaces
				List.of(interface1, interface2));

		final ServiceInstanceRequestDTO normalized = normalizator.normalizeServiceInstanceRequestDTO(toNormalize);

		assertAll("normalize ServiceInstanceRequestDTO 1",
				// system name
				() -> assertEquals("system-name", normalized.systemName()),
				// service definition name
				() -> assertEquals("service-definition-name", normalized.serviceDefinitionName()),
				// version
				() -> assertEquals("1.0.0", normalized.version()),
				// expires at
				() -> assertEquals("2025-01-31T12:00:00Z", normalized.expiresAt()),
				// metadata
				() -> assertEquals(Map.of("key", "value"), normalized.metadata()),
				// interfaces
				() -> assertEquals(2, normalized.interfaces().size()),
				() -> assertEquals(new ServiceInstanceInterfaceRequestDTO("generic-mqtt", "tcp", "NONE", properties), normalized.interfaces().get(0)),
				() -> assertEquals(new ServiceInstanceInterfaceRequestDTO("generic-mqtts", "ssl", "TOKEN_AUTH", properties), normalized.interfaces().get(1))
 				);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void normalizeServiceInstanceRequestDTOTest2NullCases() {

		// dto is null
		assertThrows(java.lang.IllegalArgumentException.class, () -> {
			normalizator.normalizeServiceInstanceRequestDTO(null);
			});

		// system name is null
		assertThrows(java.lang.IllegalArgumentException.class, () -> {
			normalizator.normalizeServiceInstanceRequestDTO(new ServiceInstanceRequestDTO(null, "service-def", "1.0.0", "2025-01-31T12:00:00Z", Map.of("key", "value"), null));
			});

		// dto contains null members (expires at, interfaces)
		final ServiceInstanceRequestDTO toNormalize = new ServiceInstanceRequestDTO("system-name", "service-definitions-name", "1.0.0",
				// expires at -> should be changed to an empty string
				null,
				// metadata
				Map.of("key", "value"),
				// interfaces -> should be changed to an empty list
				null);

		final ServiceInstanceRequestDTO normalized = normalizator.normalizeServiceInstanceRequestDTO(toNormalize);

		assertEquals("", normalized.expiresAt());
		assertEquals(new ArrayList<>(), normalized.interfaces());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void normalizeServiceInstanceLookupRequestDTOTest1() {
		// create metadata requirements
		final MetadataRequirementDTO metadataRequirement = new MetadataRequirementDTO();
		metadataRequirement.put("priority", Map.of(MetadataRequirementTokenizer.OP, MetaOps.LESS_THAN, MetadataRequirementTokenizer.VALUE, 100));

		// create interface property requirements
		final MetadataRequirementDTO interfacePropertyRequirement = new MetadataRequirementDTO();
		interfacePropertyRequirement.put(HttpInterfaceModel.PROP_NAME_BASE_PATH, Map.of(MetadataRequirementTokenizer.OP, MetaOps.CONTAINS, MetadataRequirementTokenizer.VALUE, "path"));

		// normalize dto
		final ServiceInstanceLookupRequestDTO toNormalize = new ServiceInstanceLookupRequestDTO(
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

		final ServiceInstanceLookupRequestDTO normalized = normalizator.normalizeServiceInstanceLookupRequestDTO(toNormalize);

		assertAll("normalize ServiceInstanceLookupRequestDTO 1",
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
	public void normalizeServiceInstanceLookupRequestDTOTest2NullCases() {

		// dto is null
		assertThrows(java.lang.IllegalArgumentException.class, () -> {
			normalizator.normalizeServiceInstanceLookupRequestDTO(null);
			});

		// everything is null
		final ServiceInstanceLookupRequestDTO toNormalize = new ServiceInstanceLookupRequestDTO(null, null, null, null, null, null, null, null, null);
		final ServiceInstanceLookupRequestDTO normalized = normalizator.normalizeServiceInstanceLookupRequestDTO(toNormalize);
		assertAll("normalize ServiceInstanceLookupRequestDTO 1",
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
				() -> assertEquals(new ArrayList<>(), normalized.policies())
				);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void normalizeSystemNameTest() {
		// name is null
		assertThrows(java.lang.IllegalArgumentException.class, () -> {
			normalizator.normalizeSystemName(null);
			});

		// name is empty
		assertThrows(java.lang.IllegalArgumentException.class, () -> {
			normalizator.normalizeSystemName("");
			});

		// normalize name
		assertEquals("system-name-1", normalizator.normalizeSystemName("\n \tsystem-NAME-1\r \n"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void normalizeServiceInstanceIdTest() {
		// instance id is null
		assertThrows(java.lang.IllegalArgumentException.class, () -> {
			normalizator.normalizeServiceInstanceId(null);
			});

		// instance id is empty
		assertThrows(java.lang.IllegalArgumentException.class, () -> {
			normalizator.normalizeServiceInstanceId("");
			});

		// normalize instance id
		assertEquals("system-name::service-definition::1.0.0", normalizator.normalizeServiceInstanceId(" system-NAME::service-definition::1.0.0 "));
	}
}
