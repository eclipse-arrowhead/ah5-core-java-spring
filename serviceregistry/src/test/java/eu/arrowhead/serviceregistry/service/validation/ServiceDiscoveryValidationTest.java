package eu.arrowhead.serviceregistry.service.validation;

import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.MetadataValidation;
import eu.arrowhead.common.service.validation.name.InterfaceTemplateNameValidator;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.common.service.validation.serviceinstance.ServiceInstanceIdentifierValidator;
import eu.arrowhead.common.service.validation.version.VersionValidator;
import eu.arrowhead.dto.ServiceInstanceInterfaceRequestDTO;
import eu.arrowhead.dto.ServiceInstanceRequestDTO;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;
import eu.arrowhead.serviceregistry.service.normalization.ServiceDiscoveryNormalization;
import eu.arrowhead.serviceregistry.service.validation.interf.InterfaceValidator;

@ExtendWith(MockitoExtension.class)
public class ServiceDiscoveryValidationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private ServiceDiscoveryValidation validator;

	@Mock
	private ServiceDiscoveryNormalization normalizer;

	@Mock
	private VersionValidator versionValidator;

	@Mock
	private InterfaceValidator interfaceValidator;

	@Mock
	private InterfaceTemplateNameValidator interfaceTemplateNameValidator;

	@Mock
	private ServiceDefinitionNameValidator serviceDefNameValidator;

	@Mock
	private SystemNameValidator systemNameValidator;

	@Mock
	private ServiceInstanceIdentifierValidator serviceInstanceIdentifierValidator;

	private static MockedStatic<Utilities> utilitiesMock;

	private static final String EMPTY = "\n ";
	private static final String testExpirationDate = "2025-11-04T01:53:02Z";
	final ServiceInstanceInterfaceRequestDTO testIntf = new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", Map.of("accessPort", 8080));

    // expected error messages
    private static final String MISSING_PAYLOAD = "Request payload is missing";
    private static final String MISSING_SYSTEM_NAME = "System name is empty";
    private static final String MISSING_SERVICE_DEFINITION = "Service definition name is empty";
    private static final String INVALID_FORMAT = "Expiration time has an invalid time format";
    private static final String INVALID_DATE = "Expiration time is in the past";
    private static final String MISSING_INTF_LIST = "Service interface list is empty";
    private static final String MISSING_TEMPLATE_NAME = "Interface template name is missing";
    private static final String MISSING_POLICY = "Interface policy is missing";
    private static final String INVALID_POLICY = "Invalid interface policy";
    private static final String MISSING_PROPERTIES = "Interface properties are missing";
    private static final String EXCLUSIVITY_VALUE_ERROR = "allowExclusivity metadata must have the string representation of an integer value.";

	//=================================================================================================
	// methods

	// REGISTER SERVICE

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterServiceMissingPayload() {
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRegisterService(null, "test origin"));
		assertEquals(MISSING_PAYLOAD, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterServiceEmptySystemName() {
		final ServiceInstanceRequestDTO dto = new ServiceInstanceRequestDTO(EMPTY, "alertService", "1.0.0", testExpirationDate, Map.of(), List.of(testIntf));
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRegisterService(dto, "test origin"));
		assertEquals(MISSING_SYSTEM_NAME, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterServiceEmptyServiceDefinition() {
		final ServiceInstanceRequestDTO dto = new ServiceInstanceRequestDTO("TemperatureProvider", EMPTY, "1.0.0", testExpirationDate, Map.of(), List.of(testIntf));
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRegisterService(dto, "test origin"));
		assertEquals(MISSING_SERVICE_DEFINITION, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterServiceValidateExpiration() {

		assertAll("Validate expiration",

			// dto with empty expiration
			() -> {
				final ServiceInstanceRequestDTO dtoEmptyExpiration = new ServiceInstanceRequestDTO("TemperatureProvider", "temperatureInfo", "1.0.0", EMPTY, Map.of(), List.of(testIntf));
				final ServiceInstanceRequestDTO expected = new ServiceInstanceRequestDTO("TemperatureProvider", "temperatureInfo", "1.0.0", "", Map.of(), List.of());
				when(normalizer.normalizeServiceInstanceRequestDTO(dtoEmptyExpiration)).thenReturn(expected);

				final ServiceInstanceRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeRegisterService(dtoEmptyExpiration, "test origin"));
				assertEquals(expected, normalized);
			},

			// invalid expiration format
			() -> {
				final ServiceInstanceRequestDTO dtoInvalidFormat = new ServiceInstanceRequestDTO("TemperatureProvider", "temperatureInfo", "1.0.0", "2030-11-04", Map.of(), List.of(testIntf));
				utilitiesMock.when(() -> Utilities.parseUTCStringToZonedDateTime(dtoInvalidFormat.expiresAt())).thenThrow(DateTimeException.class);

				final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRegisterService(dtoInvalidFormat, "test origin"));
				assertEquals(INVALID_FORMAT, ex.getMessage());
				assertEquals("test origin", ex.getOrigin());
				utilitiesMock.verify(() -> Utilities.parseUTCStringToZonedDateTime(dtoInvalidFormat.expiresAt()));
			},

			// invalid date
			() -> {
				final ServiceInstanceRequestDTO dtoInvalidDate = new ServiceInstanceRequestDTO("TemperatureProvider", "temperatureInfo", "1.0.0", "1999-11-04T01:53:02Z", Map.of(), List.of(testIntf));
				resetUtilitiesMock();
				utilitiesMock.when(() -> Utilities.parseUTCStringToZonedDateTime(dtoInvalidDate.expiresAt())).thenReturn(ZonedDateTime.of(1999, 11, 4, 1, 53, 2, 0, ZoneId.of("UTC")));

				final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRegisterService(dtoInvalidDate, "test origin"));
				assertEquals(INVALID_DATE, ex.getMessage());
				assertEquals("test origin", ex.getOrigin());
				utilitiesMock.verify(() -> Utilities.parseUTCStringToZonedDateTime(dtoInvalidDate.expiresAt()));

			}
		);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterServiceInvalidMetadata() {
		final ServiceInstanceRequestDTO dto = new ServiceInstanceRequestDTO("TemperatureProvider", "temperatureInfo", "1.0.0", testExpirationDate, Map.of("in.door", true), List.of(testIntf));
		final MockedStatic<MetadataValidation> metadataValidationMock = mockStatic(MetadataValidation.class);
		metadataValidationMock.when(() -> MetadataValidation.validateMetadataKey(dto.metadata())).thenThrow(new InvalidParameterException("Metadata validation error"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRegisterService(dto, "test origin"));
		assertEquals("Metadata validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		metadataValidationMock.verify(() -> MetadataValidation.validateMetadataKey(dto.metadata()));
		metadataValidationMock.close();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterServiceMissingInterfaceList() {
		final ServiceInstanceRequestDTO dto = new ServiceInstanceRequestDTO("TemperatureProvider", "temperatureInfo", "1.0.0", testExpirationDate, Map.of(), List.of());

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRegisterService(dto, "test origin"));
		assertEquals(MISSING_INTF_LIST, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterServiceInvalidInterface() {
		assertAll("Validate interface list",

			// template name is missing
			() -> {
				final ServiceInstanceInterfaceRequestDTO invalidIntf = new ServiceInstanceInterfaceRequestDTO(EMPTY, "http", "NONE", Map.of("accessPort", 8080));
				final ServiceInstanceRequestDTO dto = new ServiceInstanceRequestDTO("TemperatureProvider", "temperatureInfo", "1.0.0", testExpirationDate, Map.of(), List.of(invalidIntf));

				final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRegisterService(dto, "test origin"));
				assertEquals(MISSING_TEMPLATE_NAME, ex.getMessage());
				assertEquals("test origin", ex.getOrigin());
				utilitiesMock.verify(() -> Utilities.isEmpty(EMPTY), atLeastOnce());
			},

			// protocol is empty
			() -> {
				final ServiceInstanceInterfaceRequestDTO invalidIntf = new ServiceInstanceInterfaceRequestDTO("generic_http", EMPTY, "NONE", Map.of("accessPort", 8080));
				final ServiceInstanceRequestDTO dto = new ServiceInstanceRequestDTO("TemperatureProvider", "temperatureInfo", "1.0.0", testExpirationDate, Map.of(), List.of(invalidIntf));

				final ServiceInstanceInterfaceRequestDTO normalizedIntf = new ServiceInstanceInterfaceRequestDTO("generic_http", "", "NONE", Map.of("port", 8080));
				final ServiceInstanceRequestDTO expected = new ServiceInstanceRequestDTO("TemperatureProvider", "temperatureInfo", "1.0.0", testExpirationDate, Map.of(), List.of(normalizedIntf));

				when(normalizer.normalizeServiceInstanceRequestDTO(dto)).thenReturn(expected);
				when(interfaceValidator.validateNormalizedInterfaceInstancesWithPropsNormalization(expected.interfaces())).thenReturn(expected.interfaces());

				final ServiceInstanceRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeRegisterService(dto, "test origin"));
				assertEquals(expected, normalized);
			},

			// policy is missing
			() -> {
				resetUtilitiesMock();
				final ServiceInstanceInterfaceRequestDTO invalidIntf = new ServiceInstanceInterfaceRequestDTO("generic_http", "http", EMPTY, Map.of("accessPort", 8080));
				final ServiceInstanceRequestDTO dto = new ServiceInstanceRequestDTO("TemperatureProvider", "temperatureInfo", "1.0.0", testExpirationDate, Map.of(), List.of(invalidIntf));

				final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRegisterService(dto, "test origin"));
				assertEquals(MISSING_POLICY, ex.getMessage());
				assertEquals("test origin", ex.getOrigin());
				utilitiesMock.verify(() -> Utilities.isEmpty(EMPTY), atLeastOnce());
			},

			// invalid policy
			() -> {
				resetUtilitiesMock();
				final ServiceInstanceInterfaceRequestDTO invalidIntf = new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "MAGIC_TOKEN_AUTH", Map.of("accessPort", 8080));
				final ServiceInstanceRequestDTO dto = new ServiceInstanceRequestDTO("TemperatureProvider", "temperatureInfo", "1.0.0", testExpirationDate, Map.of(), List.of(invalidIntf));

				final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRegisterService(dto, "test origin"));
				assertEquals(INVALID_POLICY, ex.getMessage());
				assertEquals("test origin", ex.getOrigin());
				utilitiesMock.verify(() -> Utilities.isEnumValue("MAGIC_TOKEN_AUTH", ServiceInterfacePolicy.class));
			},

			// missing properties
			() -> {
				resetUtilitiesMock();
				final ServiceInstanceInterfaceRequestDTO invalidIntf = new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", Map.of());
				final ServiceInstanceRequestDTO dto = new ServiceInstanceRequestDTO("TemperatureProvider", "temperatureInfo", "1.0.0", testExpirationDate, Map.of(), List.of(invalidIntf));

				final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRegisterService(dto, "test origin"));
				assertEquals(MISSING_PROPERTIES, ex.getMessage());
				assertEquals("test origin", ex.getOrigin());
				utilitiesMock.verify(() -> Utilities.isEmpty(Map.of()), atLeastOnce());
			},
			// invalid properties
			() -> {
				resetUtilitiesMock();
				final ServiceInstanceInterfaceRequestDTO invalidIntf = new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", Map.of("intf.port", 8080));
				final ServiceInstanceRequestDTO dto = new ServiceInstanceRequestDTO("TemperatureProvider", "temperatureInfo", "1.0.0", testExpirationDate, Map.of(), List.of(invalidIntf));
				final MockedStatic<MetadataValidation> metadataValidationMock = mockStatic(MetadataValidation.class);
				metadataValidationMock.when(() -> MetadataValidation.validateMetadataKey(Map.of("intf.port", 8080))).thenThrow(new InvalidParameterException("Metadata validation error"));

				final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRegisterService(dto, "test origin"));
				assertEquals("Metadata validation error", ex.getMessage());
				assertEquals("test origin", ex.getOrigin());
				metadataValidationMock.verify(() -> MetadataValidation.validateMetadataKey(Map.of("intf.port", 8080)));
				metadataValidationMock.close();
			}
		);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterServiceExclusivityValueValidation() {

		assertAll(

			// invalid value
			() -> {
				final ServiceInstanceRequestDTO dtoInvalidValue = new ServiceInstanceRequestDTO("TemperatureProvider", "temperatureInfo", "1.0.0", testExpirationDate, Map.of("allowExclusivity", true), List.of(testIntf));
				when(normalizer.normalizeServiceInstanceRequestDTO(dtoInvalidValue)).thenReturn(dtoInvalidValue);
				when(interfaceValidator.validateNormalizedInterfaceInstancesWithPropsNormalization(dtoInvalidValue.interfaces())).thenReturn(dtoInvalidValue.interfaces());

				final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRegisterService(dtoInvalidValue, "test origin"));
				assertEquals(EXCLUSIVITY_VALUE_ERROR, ex.getMessage());
				assertEquals("test origin", ex.getOrigin());
			},

			// value is not represented as a string
			() -> {
				final ServiceInstanceRequestDTO dtoValueNotString = new ServiceInstanceRequestDTO("TemperatureProvider", "temperatureInfo", "1.0.0", testExpirationDate, Map.of("allowExclusivity", 100), List.of(testIntf));
				when(normalizer.normalizeServiceInstanceRequestDTO(dtoValueNotString)).thenReturn(dtoValueNotString);
				when(interfaceValidator.validateNormalizedInterfaceInstancesWithPropsNormalization(dtoValueNotString.interfaces())).thenReturn(dtoValueNotString.interfaces());

				final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRegisterService(dtoValueNotString, "test origin"));
				assertEquals(EXCLUSIVITY_VALUE_ERROR, ex.getMessage());
				assertEquals("test origin", ex.getOrigin());
			},

			// valid value
			() -> {
				final ServiceInstanceRequestDTO dtoOk = new ServiceInstanceRequestDTO("TemperatureProvider", "temperatureInfo", "1.0.0", testExpirationDate, Map.of("allowExclusivity", "100"), List.of(testIntf));
				when(normalizer.normalizeServiceInstanceRequestDTO(dtoOk)).thenReturn(dtoOk);
				when(interfaceValidator.validateNormalizedInterfaceInstancesWithPropsNormalization(dtoOk.interfaces())).thenReturn(dtoOk.interfaces());

				final ServiceInstanceRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeRegisterService(dtoOk, "test origin"));
				assertEquals(dtoOk, normalized);
			}
		);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterServiceOk() {
		final ServiceInstanceRequestDTO dto = new ServiceInstanceRequestDTO("TemperatureProvider", "temperatureInfo", "1.0.0", testExpirationDate, Map.of("frequency", "400"), List.of(testIntf));
		when(normalizer.normalizeServiceInstanceRequestDTO(dto)).thenReturn(dto);
		when(interfaceValidator.validateNormalizedInterfaceInstancesWithPropsNormalization(dto.interfaces())).thenReturn(dto.interfaces());

		final ServiceInstanceRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeRegisterService(dto, "test origin"));
		verify(normalizer).normalizeServiceInstanceRequestDTO(dto);
		verify(systemNameValidator, times(1)).validateSystemName("TemperatureProvider");
		verify(serviceDefNameValidator, times(1)).validateServiceDefinitionName("temperatureInfo");
		verify(versionValidator, times(1)).validateNormalizedVersion("1.0.0");
		verify(interfaceValidator, times(1)).validateNormalizedInterfaceInstancesWithPropsNormalization(List.of(testIntf));
		assertEquals(normalized, dto);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
    @BeforeAll
    private static void initializeUtilitiesMock() {
    	createUtilitiesMock();
    }

	//-------------------------------------------------------------------------------------------------
    @BeforeEach
    private void resetUtilitiesMockBeforeEach() {
    	resetUtilitiesMock();
    }

	//-------------------------------------------------------------------------------------------------
    private void resetUtilitiesMock() {
    	if (utilitiesMock != null) {
    		utilitiesMock.close();
    	}
    	createUtilitiesMock();
    }

	//-------------------------------------------------------------------------------------------------
    private static void createUtilitiesMock() {
    	utilitiesMock = mockStatic(Utilities.class);

    	// mock common cases
    	utilitiesMock.when(() -> Utilities.isEmpty(EMPTY)).thenReturn(true);
    	utilitiesMock.when(() -> Utilities.isEmpty(List.of())).thenReturn(true);
    	utilitiesMock.when(() -> Utilities.isEmpty(Map.of())).thenReturn(true);
		utilitiesMock.when(() -> Utilities.parseUTCStringToZonedDateTime(testExpirationDate)).thenReturn(ZonedDateTime.of(2025, 11, 4, 1, 53, 2, 0, ZoneId.of("UTC")));
    	utilitiesMock.when(() -> Utilities.utcNow()).thenReturn(ZonedDateTime.of(2025, 8, 4, 1, 53, 2, 0, ZoneId.of("UTC"))); // fictive date of testing
    	utilitiesMock.when(() -> Utilities.isEnumValue("NONE", ServiceInterfacePolicy.class)).thenReturn(true);
    }
}
