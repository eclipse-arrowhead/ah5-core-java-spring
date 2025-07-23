package eu.arrowhead.serviceregistry.service.validation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import eu.arrowhead.common.service.validation.MetadataValidation;
import eu.arrowhead.common.service.validation.address.AddressValidator;
import eu.arrowhead.common.service.validation.name.DeviceNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.common.service.validation.version.VersionValidator;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.SystemRequestDTO;
import eu.arrowhead.dto.enums.AddressType;
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

    private final NormalizedSystemRequestDTO testNormalizedDto = new NormalizedSystemRequestDTO(
    		"DummyProvider",
    		Map.of("dummy", true),
    		"1.0.0",
    		List.of(new AddressDTO("HOSTNAME", "dummy.eu")),
    		"DUMMY_DEVICE"); // the actual values don't matter

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterSystemMissingPalyoad() {
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> {
			validator.validateAndNormalizeRegisterSystem(null, "test origin");
		});
		assertEquals(MISSING_PAYLOAD, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
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

			// dto with empty list
			() -> {

				final List<String> emptyList = List.of();
				final SystemRequestDTO dtoWithEmptyList = new SystemRequestDTO("TemperatureProvider", Map.of("indoor", true), "1.0.0", emptyList, "TEST_DEVICE");

				when(normalizer.normalizeSystemRequestDTO(dtoWithEmptyList)).thenReturn(testNormalizedDto);
				assertDoesNotThrow(() -> {
					validator.validateAndNormalizeRegisterSystem(dtoWithEmptyList, "test origin");
				});
				utilitiesMock.verify(() -> Utilities.isEmpty(List.of()), atLeastOnce());
				resetUtilitiesMock();
			},

			// dto with empty element containing list
			() -> {

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

				// nothing is empty
				() -> {

					// mock emptiness check
					utilitiesMock.when(() -> Utilities.isEmpty(List.of("192.168.0.1"))).thenReturn(false);
					utilitiesMock.when(() -> Utilities.isEmpty("TEST_DEVICE")).thenReturn(false);

					final SystemRequestDTO testDto = new SystemRequestDTO("TemperatureProvider", Map.of(), "1.0.0", List.of("192.168.0.1"), "TEST_DEVICE");
					when(normalizer.normalizeSystemRequestDTO(testDto)).thenReturn(testNormalizedDto);
					assertDoesNotThrow(() -> {
						validator.validateAndNormalizeRegisterSystem(testDto, "test origin");
					});

					assertTrue(isEmptyCalledAtLeastOnce("TEST_DEVICE", List.of("192.168.0.1")));

					resetUtilitiesMock();
				},

				// device name is empty
				() -> {

					// mock emptiness check
					utilitiesMock.when(() -> Utilities.isEmpty(List.of("192.168.0.1"))).thenReturn(false);

					final SystemRequestDTO testDto = new SystemRequestDTO("TemperatureProvider", Map.of(), "1.0.0", List.of("192.168.0.1"), EMPTY);
					when(normalizer.normalizeSystemRequestDTO(testDto)).thenReturn(testNormalizedDto);
					assertDoesNotThrow(() -> {
						validator.validateAndNormalizeRegisterSystem(testDto, "test origin");
					});

					assertTrue(isEmptyCalledAtLeastOnce(EMPTY, List.of("192.168.0.1")));

					resetUtilitiesMock();
				},

				// address list is empty
				() -> {

					// mock emptiness check
					utilitiesMock.when(() -> Utilities.isEmpty("TEST_DEVICE")).thenReturn(false);

					final SystemRequestDTO testDto = new SystemRequestDTO("TemperatureProvider", Map.of(), "1.0.0", List.of(), "TEST_DEVICE");
					when(normalizer.normalizeSystemRequestDTO(testDto)).thenReturn(testNormalizedDto);
					assertDoesNotThrow(() -> {
						validator.validateAndNormalizeRegisterSystem(testDto, "test origin");
					});

					assertTrue(isEmptyCalledAtLeastOnce("TEST_DEVICE", List.of()));

					resetUtilitiesMock();
				},

				// everything is empty
				() -> {
					final SystemRequestDTO testDto = new SystemRequestDTO("TemperatureProvider", Map.of(), "1.0.0", List.of(), EMPTY);
					final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> {
						validator.validateAndNormalizeRegisterSystem(testDto, "test origin");
					});
					assertEquals(AT_LEAST_ONE_ADDRESS_NEEDED, ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
					assertTrue(isEmptyCalledAtLeastOnce(EMPTY, List.of()));

					resetUtilitiesMock();
				}
			);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterSystemValidateMetadata() {

		assertAll("Validate metadata",

			// without metadata
			() -> {

				// mock emptiness check
				utilitiesMock.when(() -> Utilities.isEmpty(Map.of())).thenReturn(true);

				// mock metadata validation
				MockedStatic<MetadataValidation> metadataValidationMock = mockStatic(MetadataValidation.class);

				final SystemRequestDTO dtoWithoutMetadata = new SystemRequestDTO("TemperatureProvider", Map.of(), "1.0.0", List.of("greenhouse.com"), "TEST_DEVICE");
				when(normalizer.normalizeSystemRequestDTO(dtoWithoutMetadata)).thenReturn(testNormalizedDto);

				assertDoesNotThrow(() -> {
					validator.validateAndNormalizeRegisterSystem(dtoWithoutMetadata, "test origin");
				});

				utilitiesMock.verify(() -> Utilities.isEmpty(Map.of()), atLeastOnce());
				metadataValidationMock.verify(() -> MetadataValidation.validateMetadataKey(Map.of()), never());

				// reset mocks
				metadataValidationMock.close();
				resetUtilitiesMock();
			},

			// with metadata
			() -> {

				// mock emptiness check
				utilitiesMock.when(() -> Utilities.isEmpty(Map.of("indoor", true))).thenReturn(false);

				// mock metadata validation
				MockedStatic<MetadataValidation> metadataValidationMock = mockStatic(MetadataValidation.class);

				final SystemRequestDTO dtoWithMetadata = new SystemRequestDTO("TemperatureProvider", Map.of("indoor", true), "1.0.0", List.of("greenhouse.com"), "TEST_DEVICE");
				when(normalizer.normalizeSystemRequestDTO(dtoWithMetadata)).thenReturn(testNormalizedDto);

				validator.validateAndNormalizeRegisterSystem(dtoWithMetadata, "test origin");

				utilitiesMock.verify(() -> Utilities.isEmpty(Map.of("indoor", true)), atLeastOnce());
				metadataValidationMock.verify(() -> MetadataValidation.validateMetadataKey(Map.of("indoor", true)), times(1));

				// reset mocks
				metadataValidationMock.close();
				resetUtilitiesMock();
			}
		);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRegisterSystemOk() {

		final SystemRequestDTO dto = new SystemRequestDTO("TemperatureProvider", Map.of("indoor", true), "1.0.0", List.of("greenhouse.com"), "TEST_DEVICE");

		utilitiesMock.when(() -> Utilities.isEmpty("TEST_DEVICE")).thenReturn(false);
		when(normalizer.normalizeSystemRequestDTO(dto)).thenReturn(
				new NormalizedSystemRequestDTO(
						"TemperatureProvider",
						Map.of("indoor", true),
						"1.0.0",
						List.of(new AddressDTO("HOSTNAME", "greenhouse.com")),
						"TEST_DEVICE")
				);

		assertDoesNotThrow(() -> validator.validateAndNormalizeRegisterSystem(dto, "test origin"));
		verify(normalizer, times(1)).normalizeSystemRequestDTO(dto);
		verify(systemNameValidator, times(1)).validateSystemName("TemperatureProvider");
		verify(versionValidator, times(1)).validateNormalizedVersion("1.0.0");
		verify(addressValidator, times(1)).validateNormalizedAddress(AddressType.HOSTNAME, "greenhouse.com");
		verify(deviceNameValidator, times(1)).validateDeviceName("TEST_DEVICE");

		resetUtilitiesMock();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRegisterSystemEmptyDeviceName() {

		final SystemRequestDTO dto = new SystemRequestDTO("TemperatureProvider", Map.of("indoor", true), "1.0.0", List.of("greenhouse.com"), EMPTY);

		when(normalizer.normalizeSystemRequestDTO(dto)).thenReturn(
				new NormalizedSystemRequestDTO(
						"TemperatureProvider",
						Map.of("indoor", true),
						"1.0.0",
						List.of(new AddressDTO("HOSTNAME", "greenhouse.com")),
						EMPTY)
				);

		assertDoesNotThrow(() -> validator.validateAndNormalizeRegisterSystem(dto, "test origin"));
		verify(deviceNameValidator, never()).validateDeviceName("TEST_DEVICE");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRegisterSystemThrowsExeption() {

		final SystemRequestDTO dto = new SystemRequestDTO("TemperatureProvider", Map.of("indoor", true), "1.0.0", List.of("greenhouse.com"), EMPTY);

		when(normalizer.normalizeSystemRequestDTO(dto)).thenReturn(testNormalizedDto);
		doThrow(new InvalidParameterException("Validation error")).when(systemNameValidator).validateSystemName(anyString());

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> {
			validator.validateAndNormalizeRegisterSystem(dto, "test origin");
		});

		assertEquals("Validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
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
    	utilitiesMock.when(() -> Utilities.isEmpty(EMPTY)).thenReturn(true);
    	utilitiesMock.when(() -> Utilities.isEmpty(List.of())).thenReturn(true);
    }

	//-------------------------------------------------------------------------------------------------
    private boolean isEmptyCalledAtLeastOnce(final String argString, final List<String> argStringList) {

    	// checks if the Utilities.isEmpty function is called with at least one of the arguments
		boolean calledAtLeastOnce = false;

		try {
			utilitiesMock.verify(() -> Utilities.isEmpty(argString), atLeastOnce());
			calledAtLeastOnce = true;
		} catch (Exception ex) {
			// intentionally do nothing
		}
		try {
			utilitiesMock.verify(() -> Utilities.isEmpty(argStringList), atLeastOnce());
			calledAtLeastOnce = true;
		} catch (Exception ex) {
			// intentionally do nothing
		}

		return calledAtLeastOnce;
    }
}
