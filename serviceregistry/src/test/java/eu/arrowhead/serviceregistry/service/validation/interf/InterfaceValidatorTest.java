package eu.arrowhead.serviceregistry.service.validation.interf;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.intf.properties.PropertyValidators;
import eu.arrowhead.common.service.util.ServiceInterfaceAddressPropertyProcessor;
import eu.arrowhead.common.service.util.ServiceInterfaceAddressPropertyProcessor.AddressData;
import eu.arrowhead.common.service.validation.address.AddressNormalizer;
import eu.arrowhead.common.service.validation.address.AddressValidator;
import eu.arrowhead.common.service.validation.name.InterfaceTemplateNameValidator;
import eu.arrowhead.dto.ServiceInstanceInterfaceRequestDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplatePropertyDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplateRequestDTO;
import eu.arrowhead.serviceregistry.jpa.service.ServiceInterfaceTemplateDbService;

@SpringBootTest(classes = {
		InterfaceValidator.class,
		InterfaceTemplateNameValidator.class,
		PropertyValidators.class,
		AddressNormalizer.class,
		AddressValidator.class })
@TestPropertySource(properties = {
	    "service.address.alias=address,addresses"
	})
public class InterfaceValidatorTest {

	//=================================================================================================
	// members

	@Autowired
	private InterfaceValidator intfValidator;

    @MockBean
    private ServiceInterfaceTemplateDbService serviceInterfaceTemplateDbService;

    @MockBean
    private PropertyValidators propertyValidators;

    @MockBean
    private ServiceInterfaceAddressPropertyProcessor interfaceAddressPropertyProcessor;

    // Expected error messages
    private static final String INVALID_TEMPLATE_NAME_PREFIX = "The specified interface template name does not match the naming convention: ";
    private static final String EMPTY_TEMPLATE_LIST = "Interface template list is empty";
    private static final String TOO_LONG_PROTOCOL_NAME = "Interface protocol is too long";
    private static final String TOO_LONG_INTF_PROPERTY_NAME = "Interface property name is too long";
    private static final String UNKNOWN_VALIDATOR_PREFIX = "Unknown property validator: ";
    private static final String EMPTY_INSTANCE_LIST = "Interface instance list is empty";
    private static final String MISSING_PROTOCOL = "Interface protocol is missing";


	//=================================================================================================
	// methods

    // 	INTERFACE TEMPLATES

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:MethodName")
	public void validateNormalizedInterfaceTemplatesTest() {

		// properties
		final ServiceInterfaceTemplatePropertyDTO propAddress = new ServiceInterfaceTemplatePropertyDTO(
				"accessAddresses",
				true,
				"NOT_EMPTY_ADDRESS_LIST",
				List.of());

		final ServiceInterfaceTemplatePropertyDTO propOperations = new ServiceInterfaceTemplatePropertyDTO(
				"operations",
				true,
				"HTTP_OPERATIONS",
				List.of());

		// templates
		final ServiceInterfaceTemplateRequestDTO httpTemplate = new ServiceInterfaceTemplateRequestDTO("generic_http", "http", List.of(propAddress, propOperations));

		final ServiceInterfaceTemplateRequestDTO mqttTemplate = new ServiceInterfaceTemplateRequestDTO("generic_mqtt", "mqtt", List.of(propAddress));
		assertDoesNotThrow(() -> {
			intfValidator.validateNormalizedInterfaceTemplates(List.of(httpTemplate, mqttTemplate));
		});
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:MethodName")
	public void validateNormalizedInterfaceTemplatesTest_invalidTemplateName() {

		final String invalidTemplateName = "genericHttp";

		// check exception type
		Exception ex = assertThrows(InvalidParameterException.class, () -> intfValidator.validateNormalizedInterfaceTemplates(
				List.of(new ServiceInterfaceTemplateRequestDTO(
						invalidTemplateName,
						"http",
						List.of()))));

		// check error message
		assertEquals(INVALID_TEMPLATE_NAME_PREFIX + invalidTemplateName, ex.getMessage());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:MethodName")
	public void validateNormalizedInterfaceTemplatesTest_emptyTemplateList() {

		// check exception type
		Exception ex = assertThrows(IllegalArgumentException.class, () -> intfValidator.validateNormalizedInterfaceTemplates(List.of()));

		// check error message
		assertEquals(EMPTY_TEMPLATE_LIST, ex.getMessage());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:MethodName")
	public void validateNormalizedInterfaceTemplatesTest_tooLongProtocolName() {

		// check exception type
		Exception ex = assertThrows(InvalidParameterException.class, () -> intfValidator.validateNormalizedInterfaceTemplates(
				List.of(new ServiceInterfaceTemplateRequestDTO(
						"generic_mqtt",
						"very very very very very very very very very very very long http",
						List.of()))));


		// check error message
		assertEquals(TOO_LONG_PROTOCOL_NAME, ex.getMessage());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:MethodName")
	public void validateNormalizedInterfaceTemplatesTest_tooLongIntfPropName() {

		final ServiceInterfaceTemplatePropertyDTO propWithLongName = new ServiceInterfaceTemplatePropertyDTO(
				"very very very very very very very very very very long address list",
				false,
				"NOT_EMPTY_ADDRESS_LIST",
				List.of());

		// check exception type
		Exception ex = assertThrows(InvalidParameterException.class, () -> intfValidator.validateNormalizedInterfaceTemplates(
				List.of(new ServiceInterfaceTemplateRequestDTO(
						"generic_mqtt",
						"mqtt",
						List.of(propWithLongName)))));

		// check error message
		assertEquals(TOO_LONG_INTF_PROPERTY_NAME, ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:MethodName")
	public void validateNormalizedInterfaceTemplatesTest_unknownValidator() {

		final String unknownValidator = "NOT_EMPTY_MQTT_ADDRESS_LIST";

		final ServiceInterfaceTemplatePropertyDTO propWithUnknownValidator = new ServiceInterfaceTemplatePropertyDTO(
				"mqtt addresses",
				false,
				unknownValidator,
				List.of());

		// check exception type
		Exception ex = assertThrows(InvalidParameterException.class, () -> intfValidator.validateNormalizedInterfaceTemplates(
				List.of(new ServiceInterfaceTemplateRequestDTO(
						"generic_mqtt",
						"mqtt",
						List.of(propWithUnknownValidator)))));

		// check error message
		assertEquals(UNKNOWN_VALIDATOR_PREFIX + unknownValidator, ex.getMessage());
	}

	// INTERFACE INSTANCES

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:MethodName")
	public void validateNormalizedInterfaceInstancesTest_emptyInstanceList() {

		// check exception type
		Exception ex = assertThrows(
				IllegalArgumentException.class,
				() -> intfValidator.validateNormalizedInterfaceInstancesWithPropsNormalization(List.of()));

		// check error message
		assertEquals(EMPTY_INSTANCE_LIST, ex.getMessage());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:MethodName")
	public void validateNormalizedInterfaceInstancesTest_invalidTemplateName() {

		final String invalidTemplateName = "invalid name";

		// check exception type
		Exception ex = assertThrows(
				InvalidParameterException.class,
				() -> intfValidator.validateNormalizedInterfaceInstancesWithPropsNormalization(
						List.of(new ServiceInstanceInterfaceRequestDTO(invalidTemplateName, "http", "BASE64_SELF_CONTAINED_TOKEN_AUTH", Map.of()))));

		// check error message
		assertEquals(INVALID_TEMPLATE_NAME_PREFIX + invalidTemplateName, ex.getMessage());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:MethodName")
	public void validateNormalizedInterfaceInstancesTest_missingIntfProtocol() {

		// no existing template in the DB
		when(serviceInterfaceTemplateDbService.getByName(anyString())).thenReturn(Optional.empty());


		// check exception type
		Exception ex = assertThrows(
				InvalidParameterException.class,
				() -> intfValidator.validateNormalizedInterfaceInstancesWithPropsNormalization(
						List.of(new ServiceInstanceInterfaceRequestDTO("generic_http", null, "BASE64_SELF_CONTAINED_TOKEN_AUTH", Map.of()))));

		// check error message
		assertEquals(MISSING_PROTOCOL, ex.getMessage());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:MethodName")
	public void validateNormalizedInterfaceInstancesTest_tooLongIntfProtocol() {

		// no existing template in the DB
		when(serviceInterfaceTemplateDbService.getByName(anyString())).thenReturn(Optional.empty());


		// check exception type
		Exception ex = assertThrows(
				InvalidParameterException.class,
				() -> intfValidator.validateNormalizedInterfaceInstancesWithPropsNormalization(
						List.of(new ServiceInstanceInterfaceRequestDTO("generic_http", "toooooooooooooooooooooooooooooooooooooooooooo long protocol name", "NONE", Map.of()))));

		// check error message
		assertEquals(TOO_LONG_PROTOCOL_NAME, ex.getMessage());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:MethodName")
	public void validateNormalizedInterfaceInstancesTest_tooLongPropertyName() {

		// no existing template in the DB
		when(serviceInterfaceTemplateDbService.getByName(anyString())).thenReturn(Optional.empty());


		// check exception type
		Exception ex = assertThrows(
				InvalidParameterException.class,
				() -> intfValidator.validateNormalizedInterfaceInstancesWithPropsNormalization(
						List.of(new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", Map.of("tooooooooooooooooooooooooooooooooooooooooooooo long property key", "value")))));

		// check error message
		assertEquals(TOO_LONG_INTF_PROPERTY_NAME, ex.getMessage());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:MethodName")
	public void validateNormalizedInterfaceInstancesTest_discoverAddresses() {

		final List<String> addressListToNormalize = List.of("192.168.0.0.3\n", "GREENHOUSE.EU");
		final String addressListKey = "addresses";

		// no existing template in the DB
		when(serviceInterfaceTemplateDbService.getByName(anyString())).thenReturn(Optional.empty());

		when(interfaceAddressPropertyProcessor.findAddresses(anyMap())).thenReturn(new AddressData(addressListToNormalize, addressListKey, true));

		Map<String, Object> properties = Map.of(
				"operations", Map.of(
						"query-temperature", Map.of(HttpOperationModel.PROP_NAME_PATH, "/query", HttpOperationModel.PROP_NAME_METHOD, "GET"),
						"set-temperature", Map.of(HttpOperationModel.PROP_NAME_PATH, "/set", HttpOperationModel.PROP_NAME_METHOD, "PUT")),
				addressListKey, addressListToNormalize);

		final List<ServiceInstanceInterfaceRequestDTO> normalizedInstances = intfValidator.validateNormalizedInterfaceInstancesWithPropsNormalization(
						List.of(new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", properties)));

		// check if the addresses were normalized
		assertTrue(normalizedInstances.get(0).properties().containsKey(addressListKey));
		assertEquals(normalizedInstances.get(0).properties().get(addressListKey), List.of("192.168.0.0.3", "greenhouse.eu"));
	}

	// when(interfaceAddressPropertyProcessor.findAddresses(anyMap())).thenReturn("mocked response");

	// no existing template, address validation and normalization
	// existing template, invalid protocol for template name
	// existing template, instance property is missing
	// existing template, present validator, validate prop
	// no problem -> check returned value!

}
