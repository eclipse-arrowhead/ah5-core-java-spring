package eu.arrowhead.serviceregistry.service.validation.interf;

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
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.dto.ServiceInstanceInterfaceRequestDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplatePropertyDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplateRequestDTO;

@SpringBootTest
public class InterfaceNormalizerTest {

	//=================================================================================================
	// members

	@Autowired
	private InterfaceNormalizer normalizer;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void normalizeInterfaceDTOTest1() {

		// create properties for dto
		final Map<String, Object> properties = 	Map.of(
				HttpInterfaceModel.PROP_NAME_ACCESS_ADDRESSES, List.of("192.168.56.116"),
				HttpInterfaceModel.PROP_NAME_ACCESS_PORT, 8080,
				HttpInterfaceModel.PROP_NAME_BASE_PATH, "/basepath",
				HttpInterfaceModel.PROP_NAME_OPERATIONS, Map.of("echo", Map.of(HttpOperationModel.PROP_NAME_PATH, "/echo", HttpOperationModel.PROP_NAME_METHOD, "get")));

		// normalize dto
		final ServiceInstanceInterfaceRequestDTO toNormalize = new ServiceInstanceInterfaceRequestDTO(
				// template name
				"generic-HTTPS \t\n",
				// protocol
				"\t  HTTPS ",
				// policy
				"\n cert_auth \t",
				// properties
				properties);

		final ServiceInstanceInterfaceRequestDTO normalized = normalizer.normalizeInterfaceDTO(toNormalize);

		assertAll("normalize InterfaceDTO 1",
				// template name
				() -> assertEquals("generic-https", normalized.templateName()),
				// protocol
				() -> assertEquals("https", normalized.protocol()),
				// policy
				() -> assertEquals("CERT_AUTH", normalized.policy()),
				// properties (should not change, these will be normalized in the interface validator)
				() -> assertEquals(properties, normalized.properties()));

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void normalizeInterfaceDTOTest2NullCases() {
		// dto is null
		assertThrows(java.lang.IllegalArgumentException.class, () -> {
			normalizer.normalizeInterfaceDTO(null);
			});

		// protocol is null
		final ServiceInstanceInterfaceRequestDTO toNormalize = new ServiceInstanceInterfaceRequestDTO(
				// template name
				"generic-http",
				// protocol -> should be changed to an empty string
				null,
				// policy
				"TOKEN_AUTH",
				// properties
				null);

		final ServiceInstanceInterfaceRequestDTO normalized = normalizer.normalizeInterfaceDTO(toNormalize);

		assertEquals("", normalized.protocol());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void normalizeTemplateDTOTest1() {
		final ServiceInterfaceTemplateRequestDTO toNormalize = new ServiceInterfaceTemplateRequestDTO(
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

		final ServiceInterfaceTemplateRequestDTO normalized = normalizer.normalizeTemplateDTO(toNormalize);

		assertAll("normalize templateDTO 1",
				// name
				() -> assertEquals("generic-http", normalized.name()),
				// protocol
				() -> assertEquals("http", normalized.protocol()),
				// property requirements
				() -> assertEquals(List.of(new ServiceInterfaceTemplatePropertyDTO("ipAddress", false, "MINMAX", List.of("192.168.0.0", "192.168.255.255"))), normalized.propertyRequirements()));

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void normalizeTemplateDTOTestNullCases() {
		// dto is null
		assertThrows(java.lang.IllegalArgumentException.class, () -> {
			normalizer.normalizeTemplateDTO(null);
			});

		// validator is null, validator params are null
		final ServiceInterfaceTemplateRequestDTO toNormalize = new ServiceInterfaceTemplateRequestDTO(
				// name
				"generic-http",
				// protocol
				"http",
				// property requirements
				List.of(new ServiceInterfaceTemplatePropertyDTO(
						// name
						"ipAddress",
						// mandatory
						false,
						// validator -> should be changed to an empty string
						null,
						// validator params -> should be changed to an empty list
						null))
				);

		final ServiceInterfaceTemplateRequestDTO normalized = normalizer.normalizeTemplateDTO(toNormalize);

		assertEquals(1, normalized.propertyRequirements().size());
		assertEquals("", normalized.propertyRequirements().get(0).validator());
		assertEquals(new ArrayList<>(), normalized.propertyRequirements().get(0).validatorParams());
	}

}
