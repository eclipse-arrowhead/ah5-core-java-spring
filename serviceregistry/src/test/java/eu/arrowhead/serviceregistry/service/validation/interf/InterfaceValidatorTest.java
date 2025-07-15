package eu.arrowhead.serviceregistry.service.validation.interf;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.jpa.service.ServiceInterfaceTemplateDbService;

@TestPropertySource(properties = {
	    "service.address.alias=address,addresses"
	})
@ExtendWith(MockitoExtension.class)
public class InterfaceValidatorTest {

	//=================================================================================================
	// members

	@InjectMocks
	private InterfaceValidator intfValidator;

	@Mock
	private InterfaceTemplateNameValidator interfaceTemplateNameValidator;

	@Mock
	private AddressNormalizer addressNormalizer;

	@Mock
	private AddressValidator addressValidator;

    @Mock
    private ServiceInterfaceTemplateDbService serviceInterfaceTemplateDbService;

    @Mock
    private PropertyValidators propertyValidators;

    @Mock
    private ServiceInterfaceAddressPropertyProcessor interfaceAddressPropertyProcessor;

    // Expected error messages
    private static final String EMPTY_TEMPLATE_LIST = "Interface template list is empty";
    private static final String TOO_LONG_PROTOCOL_NAME = "Interface protocol is too long";
    private static final String TOO_LONG_INTF_PROPERTY_NAME = "Interface property name is too long";
    private static final String UNKNOWN_VALIDATOR_PREFIX = "Unknown property validator: ";
    private static final String EMPTY_INSTANCE_LIST = "Interface instance list is empty";
    private static final String MISSING_PROTOCOL = "Interface protocol is missing";


	//=================================================================================================
	// methods

    // INTERFACE TEMPLATES

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

		verify(interfaceTemplateNameValidator, times(2)).validateInterfaceTemplateName(anyString());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:MethodName")
	public void validateNormalizedInterfaceTemplatesTest_emptyTemplateList() {

		// check exception type
		final Exception ex = assertThrows(IllegalArgumentException.class, () -> intfValidator.validateNormalizedInterfaceTemplates(List.of()));

		// check error message
		assertEquals(EMPTY_TEMPLATE_LIST, ex.getMessage());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:MethodName")
	public void validateNormalizedInterfaceTemplatesTest_validateTemplateName() {

		intfValidator.validateNormalizedInterfaceTemplates(List.of(new ServiceInterfaceTemplateRequestDTO("generic_mqtt", "http", List.of())));
		// check if it validates the interface template name
		verify(interfaceTemplateNameValidator, times(1)).validateInterfaceTemplateName(eq("generic_mqtt"));

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:MethodName")
	public void validateNormalizedInterfaceTemplatesTest_tooLongProtocolName() {

		// check exception type
		final Exception ex = assertThrows(InvalidParameterException.class, () -> intfValidator.validateNormalizedInterfaceTemplates(
				List.of(new ServiceInterfaceTemplateRequestDTO(
						"generic_mqtt",
						"very very very very very very very very very very very long mqtt",
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
		final Exception ex = assertThrows(InvalidParameterException.class, () -> intfValidator.validateNormalizedInterfaceTemplates(
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
		final Exception ex = assertThrows(InvalidParameterException.class, () -> intfValidator.validateNormalizedInterfaceTemplates(
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
		final Exception ex = assertThrows(
				IllegalArgumentException.class,
				() -> intfValidator.validateNormalizedInterfaceInstancesWithPropsNormalization(List.of()));

		// check error message
		assertEquals(EMPTY_INSTANCE_LIST, ex.getMessage());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:MethodName")
	public void validateNormalizedInterfaceInstancesTest_missingIntfProtocol() {

		// no existing template in the DB
		when(serviceInterfaceTemplateDbService.getByName(anyString())).thenReturn(Optional.empty());


		// check exception type
		final Exception ex = assertThrows(
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
		final Exception ex = assertThrows(
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
		final Exception ex = assertThrows(
				InvalidParameterException.class,
				() -> intfValidator.validateNormalizedInterfaceInstancesWithPropsNormalization(
						List.of(new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", Map.of("tooooooooooooooooooooooooooooooooooooooooooooo long property key", "value")))));

		// check error message
		assertEquals(TOO_LONG_INTF_PROPERTY_NAME, ex.getMessage());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:MethodName")
	public void validateNormalizedInterfaceInstancesTest_discoverAddress() {

		final String address = "192.168.0.3\n";
		final String normalizedAddress = "192.168.0.3";
		final String addressKey = "address";

		// no existing template in the DB
		when(serviceInterfaceTemplateDbService.getByName(anyString())).thenReturn(Optional.empty());
		// mock validation and normalization
		when(addressNormalizer.normalize(address)).thenReturn(normalizedAddress);
		when(addressValidator.detectType(normalizedAddress)).thenReturn(AddressType.IPV4);

		// create properties to test
		final Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(
				"operations",
				Map.of("query-temperature", Map.of(HttpOperationModel.PROP_NAME_PATH, "/query", HttpOperationModel.PROP_NAME_METHOD, "GET"),
						"set-temperature", Map.of(HttpOperationModel.PROP_NAME_PATH, "/set", HttpOperationModel.PROP_NAME_METHOD, "PUT")));
		properties.put(addressKey, address);

		// mock address preprocessor
		when(interfaceAddressPropertyProcessor.findAddresses(properties)).thenReturn(new AddressData(List.of(address), addressKey, false));

		final ServiceInstanceInterfaceRequestDTO normalizedInstance = intfValidator.validateNormalizedInterfaceInstancesWithPropsNormalization(
						List.of(new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", properties))).get(0);

		// check if the address was validated and normalized
		verify(addressValidator, times(1)).validateNormalizedAddress(any(AddressType.class), anyString());
		verify(addressNormalizer, times(1)).normalize(anyString());
		assertTrue(normalizedInstance.properties().containsKey(addressKey));
		assertEquals(String.class, normalizedInstance.properties().get(addressKey).getClass());
		assertEquals(normalizedInstance.properties().get(addressKey), normalizedAddress);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:MethodName")
	public void validateNormalizedInterfaceInstancesTest_discoverAddressList() {

		// addresses to normalize
		final String address1 = "192.168.0.3\n";
		final String address2 = "GREENHOUSE.EU";
		// normalized addresses
		final String normalizedAddress1 = "192.168.0.3";
		final String normalizedAddress2 = "greenhouse.eu";
		// address properties
		final String addressListKey = "addresses";
		final List<String> addressListToNormalize = List.of(address1, address2);

		// no existing template in the DB
		when(serviceInterfaceTemplateDbService.getByName(anyString())).thenReturn(Optional.empty());
		// mock validation and normalization
		when(addressNormalizer.normalize(address1)).thenReturn(normalizedAddress1);
		when(addressNormalizer.normalize(address2)).thenReturn(normalizedAddress2);
		when(addressValidator.detectType(normalizedAddress1)).thenReturn(AddressType.IPV4);
		when(addressValidator.detectType(normalizedAddress2)).thenReturn(AddressType.HOSTNAME);

		// create properties to test
		final Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(
				"operations",
				Map.of("query-temperature", Map.of(HttpOperationModel.PROP_NAME_PATH, "/query", HttpOperationModel.PROP_NAME_METHOD, "GET"),
						"set-temperature", Map.of(HttpOperationModel.PROP_NAME_PATH, "/set", HttpOperationModel.PROP_NAME_METHOD, "PUT")));
		properties.put(addressListKey, addressListToNormalize);

		// mock address preprocessor
		when(interfaceAddressPropertyProcessor.findAddresses(properties)).thenReturn(new AddressData(addressListToNormalize, addressListKey, true));

		final ServiceInstanceInterfaceRequestDTO normalizedInstance = intfValidator.validateNormalizedInterfaceInstancesWithPropsNormalization(
						List.of(new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", properties))).get(0);

		// check if the addresses were validated and normalized
		verify(addressValidator, times(2)).validateNormalizedAddress(any(AddressType.class), anyString());
		verify(addressNormalizer, times(2)).normalize(anyString());
		assertTrue(normalizedInstance.properties().containsKey(addressListKey));
		assertEquals(java.util.ArrayList.class, normalizedInstance.properties().get(addressListKey).getClass());
		assertEquals(List.of("192.168.0.3", "greenhouse.eu"), normalizedInstance.properties().get(addressListKey));
	}

	// no existing template, address validation and normalization
	// existing template, invalid protocol for template name
	// existing template, instance property is missing
	// existing template, present validator, validate prop
	// no problem -> check returned value!

}
