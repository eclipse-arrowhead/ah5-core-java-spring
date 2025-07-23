package eu.arrowhead.serviceregistry.service.validation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.address.AddressValidator;
import eu.arrowhead.common.service.validation.name.DeviceNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.common.service.validation.version.VersionValidator;
import eu.arrowhead.dto.SystemRequestDTO;
import eu.arrowhead.serviceregistry.service.dto.NormalizedSystemRequestDTO;
import eu.arrowhead.serviceregistry.service.normalization.SystemDiscoveryNormalization;

@ExtendWith(MockitoExtension.class)
public class SystemDiscoveryValidationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private SystemDiscoveryValidation validator;

	@Mock
	private SystemDiscoveryNormalization normalizer;

	@Mock
	private AddressValidator addressValidator;

	@Mock
	private SystemNameValidator systemNameValidator;

	@Mock
	private DeviceNameValidator deviceNameValidator;

	@Mock
	private VersionValidator versionValidator;

	private static MockedStatic<Utilities> utilitiesMock;

	private static final String EMPTY = "\n ";

    // expected error messages
    private static final String MISSING_PAYLOAD = "Request payload is missing";
    private static final String MISSING_SYSTEM_NAME = "System name is empty";
    private static final String MISSING_ADDRESS = "Address is missing";
    private static final String AT_LEAST_ONE_ADDRESS_NEEDED = "At least one system address is needed for every system";

    private final NormalizedSystemRequestDTO testNormalizedDto = new NormalizedSystemRequestDTO("TemperatureProvider", Map.of("indoor", true), "1.0.0", List.of(), "TEST_DEVICE");

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterSystemMissingPalyoad() {
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> {
			validator.validateAndNormalizeRegisterSystem(null, "test origin");
		});
		assertEquals(MISSING_PAYLOAD, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());;
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterSystemMissingSystemName() {

		// mock emptiness check
		utilitiesMock.when(() -> Utilities.isEmpty("TemperatureProvider")).thenReturn(false);

		// create test dtos
		final SystemRequestDTO dtoWithMissingName = new SystemRequestDTO(EMPTY, Map.of("indoor", true), "1.0.0", List.of(), "TEST_DEVICE");
		final SystemRequestDTO dtoOk = new SystemRequestDTO("TemperatureProvider", Map.of("indoor", true), "1.0.0", List.of(), "TEST_DEVICE");

		assertAll("Validate system name",
				() -> {
					final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> {
						validator.validateAndNormalizeRegisterSystem(dtoWithMissingName, "test origin");
					});
					assertEquals(MISSING_SYSTEM_NAME, ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
					utilitiesMock.verify(() -> Utilities.isEmpty(EMPTY));
				},
				() -> {
					when(normalizer.normalizeSystemRequestDTO(dtoOk)).thenReturn(testNormalizedDto);
					assertDoesNotThrow(() -> {
						validator.validateAndNormalizeRegisterSystem(dtoOk, "test origin");
					});
					utilitiesMock.verify(() -> Utilities.isEmpty("TemperatureProvider"));
				}
		);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterSystemValidateAddresses() {

		assertAll("Validate addresses",
			() -> {
				// dto with empty list
				final List<String> emptyList = List.of();
				final SystemRequestDTO dtoWithEmptyList = new SystemRequestDTO("TemperatureProvider", Map.of("indoor", true), "1.0.0", emptyList, "TEST_DEVICE");

				when(normalizer.normalizeSystemRequestDTO(dtoWithEmptyList)).thenReturn(testNormalizedDto);
				assertDoesNotThrow(() -> {
					validator.validateAndNormalizeRegisterSystem(dtoWithEmptyList, "test origin");
				});
				utilitiesMock.verify(() -> Utilities.isEmpty(List.of()), atLeastOnce());
				resetUtilitiesMock();
			},
			() -> {
				// dto with empty element containing list
				final List<String> emptyElementList = List.of("192.168.0.1", "\t\n ");
				final SystemRequestDTO dtoWithEmptyElement = new SystemRequestDTO("TemperatureProvider", Map.of("indoor", true), "1.0.0", emptyElementList, "TEST_DEVICE");

				// mock emptiness check
				utilitiesMock.when(() -> Utilities.isEmpty("192.168.0.1")).thenReturn(false);
				utilitiesMock.when(() -> Utilities.isEmpty("\t\n ")).thenReturn(true);

				final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> {
					validator.validateAndNormalizeRegisterSystem(dtoWithEmptyElement, "test origin");
				});
				assertEquals(MISSING_ADDRESS, ex.getMessage());
				assertEquals("test origin", ex.getOrigin());
				utilitiesMock.verify(() -> Utilities.isEmpty("192.168.0.1"));
				utilitiesMock.verify(() -> Utilities.isEmpty("\t\n "));
				resetUtilitiesMock();
			}
		);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterSystemValidateAtLeastOneAddress() {

		assertAll("At least one address is present",
				() -> {
					// mock emptiness check
					utilitiesMock.when(() -> Utilities.isEmpty(List.of("192.168.0.1"))).thenReturn(false);
					utilitiesMock.when(() -> Utilities.isEmpty("TEST_DEVICE")).thenReturn(false);

					// nothing is empty
					final SystemRequestDTO testDto = new SystemRequestDTO("TemperatureProvider", Map.of(), "1.0.0", List.of("192.168.0.1"), "TEST_DEVICE");
					when(normalizer.normalizeSystemRequestDTO(testDto)).thenReturn(testNormalizedDto);
					assertDoesNotThrow(() -> {
						validator.validateAndNormalizeRegisterSystem(testDto, "test origin");
					});
					utilitiesMock.verify(() -> Utilities.isEmpty(List.of("192.168.0.1")), atLeastOnce());
					utilitiesMock.verify(() -> Utilities.isEmpty("TEST_DEVICE"));

					resetUtilitiesMock();
				},
				() -> {
					// mock emptiness check
					utilitiesMock.when(() -> Utilities.isEmpty(List.of("192.168.0.1"))).thenReturn(false);
					utilitiesMock.when(() -> Utilities.isEmpty(List.of("\n "))).thenReturn(true);

					// device name is empty
					final SystemRequestDTO testDto = new SystemRequestDTO("TemperatureProvider", Map.of(), "1.0.0", List.of("192.168.0.1"), "\n ");
					when(normalizer.normalizeSystemRequestDTO(testDto)).thenReturn(testNormalizedDto);
					assertDoesNotThrow(() -> {
						validator.validateAndNormalizeRegisterSystem(testDto, "test origin");
					});

					boolean called = false;

					try {
						utilitiesMock.verify(() -> Utilities.isEmpty("\n "));
					    called = true;
					} catch (final AssertionError ex) {
						// intentionally do nothing
					}
					try {
					    utilitiesMock.verify(() -> Utilities.isEmpty(List.of("192.168.0.1")));
					    called = true;
					} catch (final AssertionError ex) {
						// intentionally do nothing
					}
					assertTrue(called);

					resetUtilitiesMock();
				}/*
				// everything is empty
				() -> {
					final SystemRequestDTO testDto = new SystemRequestDTO("TemperatureProvider", Map.of(), "1.0.0", List.of(), "");
					final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> {
						validator.validateAndNormalizeRegisterSystem(testDto, "test origin");
					});
					assertEquals(AT_LEAST_ONE_ADDRESS_NEEDED, ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
					utilitiesMock.verify(() -> Utilities.isEmpty(""));
					utilitiesMock.verify(() -> Utilities.isEmpty(List.of()), atLeastOnce());
				}*/
			);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
    @BeforeAll
    private static void initializeUtilitiesMock() {
    	utilitiesMock = mockStatic(Utilities.class);
    	utilitiesMock.when(() -> Utilities.isEmpty(EMPTY)).thenReturn(true);
    	utilitiesMock.when(() -> Utilities.isEmpty(List.of())).thenReturn(true);
    }

	//-------------------------------------------------------------------------------------------------
    @BeforeEach
    private void resetUtilitiesMock() {
    	utilitiesMock.close();
    	initializeUtilitiesMock();
    }
}
