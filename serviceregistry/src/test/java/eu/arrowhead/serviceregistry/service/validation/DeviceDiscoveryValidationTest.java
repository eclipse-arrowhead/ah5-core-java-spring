package eu.arrowhead.serviceregistry.service.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.MetadataValidation;
import eu.arrowhead.common.service.validation.address.AddressValidator;
import eu.arrowhead.common.service.validation.name.DeviceNameValidator;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.DeviceRequestDTO;
import eu.arrowhead.serviceregistry.service.dto.NormalizedDeviceRequestDTO;
import eu.arrowhead.serviceregistry.service.normalization.DeviceDiscoveryNormalization;

@ExtendWith(MockitoExtension.class)
public class DeviceDiscoveryValidationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private DeviceDiscoveryValidation validator;

	@Mock
	private AddressValidator addressValidator;

	@Mock
	private DeviceNameValidator deviceNameValidator;

	@Mock
	private DeviceDiscoveryNormalization normalizer;

	private static MockedStatic<Utilities> utilitiesMock;

	private static final String EMPTY = "\n ";

	private final NormalizedDeviceRequestDTO testNormalizedDto = new NormalizedDeviceRequestDTO(
			"DUMMY_DEVICE",
			Map.of(),
			List.of(new AddressDTO("MAC", "02:00:5e:00:53:af")));

	// expected error messages
	private static final String MISSING_PAYLOAD = "Request payload is missing";
	private static final String EMPTY_NAME = "Device name is empty";
	private static final String NO_ADDRESS = "At least one device address is needed";
	private static final String MISSING_ADDRESS = "Address is missing";

	//=================================================================================================
	// methods

	// REGISTER DEVICE

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterDeviceMissingPayload() {
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRegisterDevice(null, "test origin"));
		assertEquals(MISSING_PAYLOAD, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterDeviceMissingDeviceName() {
		final DeviceRequestDTO dto = new DeviceRequestDTO(EMPTY, Map.of(), List.of("02:00:5e:00:53:af"));
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRegisterDevice(dto, "test origin"));
		assertEquals(EMPTY_NAME, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.isEmpty(EMPTY), atLeastOnce());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterDeviceNoAddress() {
		final DeviceRequestDTO dto = new DeviceRequestDTO("TEST_DEVICE", Map.of(), List.of());
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRegisterDevice(dto, "test origin"));
		assertEquals(NO_ADDRESS, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.isEmpty(List.of()), atLeastOnce());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterDeviceMissingAddress() {
		final List<String> addresses = new ArrayList<String>();
		addresses.add("02:00:5e:00:53:af");
		addresses.add(EMPTY);
		final DeviceRequestDTO dto = new DeviceRequestDTO("TEST_DEVICE", Map.of(), addresses);
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRegisterDevice(dto, "test origin"));
		assertEquals(MISSING_ADDRESS, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.isEmpty(EMPTY), atLeastOnce());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterDeviceInvalidMetadata() {
		final DeviceRequestDTO dto = new DeviceRequestDTO("TEST_DEVICE", Map.of("indoor", true), List.of("02:00:5e:00:53:af"));
		final MockedStatic<MetadataValidation> metadataValidationMock = mockStatic(MetadataValidation.class);
		metadataValidationMock.when(() -> MetadataValidation.validateMetadataKey(Map.of("indoor", true))).thenThrow(new InvalidParameterException("Validation error"));
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRegisterDevice(dto, "test origin"));
		assertEquals("Validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		metadataValidationMock.verify(() -> MetadataValidation.validateMetadataKey(Map.of("indoor", true)));
		metadataValidationMock.close();
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
    }
}
