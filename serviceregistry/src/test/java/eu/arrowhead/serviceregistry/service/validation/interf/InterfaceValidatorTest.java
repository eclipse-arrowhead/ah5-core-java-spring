package eu.arrowhead.serviceregistry.service.validation.interf;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.intf.properties.PropertyValidators;
import eu.arrowhead.common.service.util.ServiceInterfaceAddressPropertyProcessor;
import eu.arrowhead.common.service.validation.address.AddressNormalizer;
import eu.arrowhead.common.service.validation.address.AddressValidator;
import eu.arrowhead.common.service.validation.name.InterfaceTemplateNameValidator;
import eu.arrowhead.dto.ServiceInterfaceTemplatePropertyDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplateRequestDTO;
import eu.arrowhead.serviceregistry.jpa.service.ServiceInterfaceTemplateDbService;

@SpringBootTest(classes = {
		InterfaceValidator.class,
		InterfaceTemplateNameValidator.class,
		PropertyValidators.class,
		AddressNormalizer.class,
		AddressValidator.class })
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
    private ServiceInterfaceAddressPropertyProcessor serviceIntfAddrPropProcessor;

    // Expected error messages
    private final static String INVALID_TEMPLATE_NAME_PREFIX = "The specified interface template name does not match the naming convention: ";
    private final static String EMPTY_TEMPLATE_LIST = "Interface template list is empty";
    private final static String TOO_LONG_PROTOCOL_NAME = "Interface protocol is too long";
    private final static String TOO_LONG_INTF_PROPERTY_NAME = "Interface property name is too long";
    private final static String UNKNOWN_VALIDATOR_PREFIX = "Unknown property validator: ";

	// interface property name is too long

	// unknown property validator

	// everything is right


	//=================================================================================================
	// methods

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

}
