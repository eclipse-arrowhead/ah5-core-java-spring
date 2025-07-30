package eu.arrowhead.serviceregistry.service.validation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;

import java.util.ArrayList;
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
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.MetadataValidation;
import eu.arrowhead.common.service.validation.PageValidator;
import eu.arrowhead.common.service.validation.address.AddressValidator;
import eu.arrowhead.common.service.validation.name.DeviceNameNormalizer;
import eu.arrowhead.common.service.validation.name.DeviceNameValidator;
import eu.arrowhead.common.service.validation.name.InterfaceTemplateNameNormalizer;
import eu.arrowhead.common.service.validation.name.InterfaceTemplateNameValidator;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.common.service.validation.serviceinstance.ServiceInstanceIdentifierNormalizer;
import eu.arrowhead.common.service.validation.serviceinstance.ServiceInstanceIdentifierValidator;
import eu.arrowhead.common.service.validation.version.VersionNormalizer;
import eu.arrowhead.common.service.validation.version.VersionValidator;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.DeviceListRequestDTO;
import eu.arrowhead.dto.DeviceRequestDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.service.dto.NormalizedDeviceRequestDTO;
import eu.arrowhead.serviceregistry.service.normalization.ManagementNormalization;
import eu.arrowhead.serviceregistry.service.validation.interf.InterfaceValidator;

@ExtendWith(MockitoExtension.class)
public class ManagementValidationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private ManagementValidation validator;

	@Mock
	private AddressValidator addressTypeValidator;

	@Mock
	private PageValidator pageValidator;

	@Mock
	private VersionValidator versionValidator;

	@Mock
	private InterfaceValidator interfaceValidator;

	@Mock
	private DeviceNameValidator deviceNameValidator;

	@Mock
	private ServiceDefinitionNameValidator serviceDefNameValidator;

	@Mock
	private SystemNameValidator systemNameValidator;

	@Mock
	private ServiceInstanceIdentifierValidator serviceInstanceIdentifierValidator;

	@Mock
	private InterfaceTemplateNameValidator interfaceTemplateNameValidator;

	@Mock
	private DeviceNameNormalizer deviceNameNormalizer;

	@Mock
	private ServiceDefinitionNameNormalizer serviceDefNameNormalizer;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	@Mock
	private VersionNormalizer versionNormalizer;

	@Mock
	private ServiceInstanceIdentifierNormalizer serviceInstanceIdentifierNormalizer;

	@Mock
	private InterfaceTemplateNameNormalizer interfaceTemplateNameNormalizer;

	@Mock
	private ManagementNormalization normalizer;

	private static MockedStatic<Utilities> utilitiesMock;

	private static final String EMPTY = "\n ";

    // expected error messages
	private static final String MISSING_PAYLOAD = "Request payload is missing";
	private static final String EMPTY_PAYLOAD = "Request payload is empty";
	private static final String NULL_DEVICE = "Device list contains null element";
	private static final String EMPTY_DEVICE_NAME = "Device name is empty";
	private static final String DUPLICATE_DEVICE_NAME_PREFIX = "Duplicate device name: ";
	private static final String MISSING_ADDRESS_LIST = "At least one device address is needed for every device";
	private static final String MISSING_ADDRESS = "Address is missing";

	//=================================================================================================
	// methods

	// DEVICE

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateDeviceMissingPayload() {

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateDevices(null, "test origin"));
		assertEquals(MISSING_PAYLOAD, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateDeviceEmptyPayload() {

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateDevices(new DeviceListRequestDTO(List.of()), "test origin"));
		assertEquals(EMPTY_PAYLOAD, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.isEmpty(List.of()));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateDeviceNullElement() {

		final List<DeviceRequestDTO> devices = new ArrayList<DeviceRequestDTO>(2);
		devices.add(new DeviceRequestDTO("TEST_DEVICE", Map.of("indoor", true), List.of("02:00:00:00:00:01")));
		devices.add(null);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateDevices(new DeviceListRequestDTO(devices), "test origin"));
		assertEquals(NULL_DEVICE, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateDeviceEmptyName() {

		final List<DeviceRequestDTO> devices = List.of(new DeviceRequestDTO(EMPTY, Map.of("indoor", true), List.of("02:00:00:00:00:01")));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateDevices(new DeviceListRequestDTO(devices), "test origin"));
		assertEquals(EMPTY_DEVICE_NAME, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.isEmpty(EMPTY));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateDeviceDuplicateName() {

		final List<DeviceRequestDTO> devices = new ArrayList<DeviceRequestDTO>(2);
		devices.add(new DeviceRequestDTO("TEST_DEVICE", Map.of("indoor", true), List.of("02:00:00:00:00:01")));
		devices.add(new DeviceRequestDTO(" test_device ", Map.of("indoor", false), List.of("02:00:00:00:00:02")));

		when(deviceNameNormalizer.normalize("TEST_DEVICE")).thenReturn("TEST_DEVICE");
		when(deviceNameNormalizer.normalize(" test_device ")).thenReturn("TEST_DEVICE");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateDevices(new DeviceListRequestDTO(devices), "test origin"));
		assertEquals(DUPLICATE_DEVICE_NAME_PREFIX + "TEST_DEVICE", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateDeviceMissingAddresses() {

		final List<DeviceRequestDTO> devices = List.of(new DeviceRequestDTO("TEST_DEVICE", Map.of("indoor", true), List.of()));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateDevices(new DeviceListRequestDTO(devices), "test origin"));
		assertEquals(MISSING_ADDRESS_LIST, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.isEmpty(List.of()));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateDeviceMissingAddress() {

		final List<DeviceRequestDTO> devices = List.of(new DeviceRequestDTO("TEST_DEVICE", Map.of("indoor", true), List.of("02:00:00:00:00:01", EMPTY)));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateDevices(new DeviceListRequestDTO(devices), "test origin"));
		assertEquals(MISSING_ADDRESS, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.isEmpty(EMPTY));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateDeviceValidateMetadata() {

		final MockedStatic<MetadataValidation> metadataValidationMock = mockStatic(MetadataValidation.class);

		assertAll(

			// invalid metadata
			() -> {
				final List<DeviceRequestDTO> devicesInvalidMetadata = List.of(new DeviceRequestDTO("TEST_DEVICE", Map.of("in.door", true), List.of("02:00:00:00:00:01")));

				metadataValidationMock.when(() -> MetadataValidation.validateMetadataKey(Map.of("in.door", true))).thenThrow(new InvalidParameterException("Validation error"));

				final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateDevices(new DeviceListRequestDTO(devicesInvalidMetadata), "test origin"));
				assertEquals("Validation error", ex.getMessage());
				assertEquals("test origin", ex.getOrigin());
				metadataValidationMock.verify(() -> MetadataValidation.validateMetadataKey(Map.of("in.door", true)));
			},

			// empty metadata
			() -> {
				final List<DeviceRequestDTO> devicesEmptyMetadata = List.of(new DeviceRequestDTO("TEST_DEVICE", Map.of(), List.of("02:00:00:00:00:01")));
				final List<NormalizedDeviceRequestDTO> expected = List.of(new NormalizedDeviceRequestDTO("TEST_DEVICE", Map.of(), List.of(new AddressDTO("MAC", "02:00:00:00:00:01"))));
				when(normalizer.normalizeDeviceRequestDTOList(devicesEmptyMetadata)).thenReturn(expected);

				metadataValidationMock.reset();

				final List<NormalizedDeviceRequestDTO> normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeCreateDevices(new DeviceListRequestDTO(devicesEmptyMetadata), "test origin"));
				assertEquals(normalized, expected);
				metadataValidationMock.verify(() -> MetadataValidation.validateMetadataKey(Map.of("in.door", true)), never());
				metadataValidationMock.close();
			}
		);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateDevicesOk() {

		final List<DeviceRequestDTO> devices = new ArrayList<DeviceRequestDTO>(1);
		devices.add(new DeviceRequestDTO("TEST_DEVICE", Map.of("indoor", true), List.of("02:00:00:00:00:01")));

		final List<NormalizedDeviceRequestDTO> expected = List.of(new NormalizedDeviceRequestDTO("TEST_DEVICE", Map.of("indoor", true), List.of(new AddressDTO("MAC", "02:00:00:00:00:01"))));
		when(normalizer.normalizeDeviceRequestDTOList(devices)).thenReturn(expected);

		final List<NormalizedDeviceRequestDTO> normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeCreateDevices(new DeviceListRequestDTO(devices), "test origin"));
		assertEquals(expected, normalized);
		verify(normalizer, times(1)).normalizeDeviceRequestDTOList(devices);
		verify(deviceNameValidator, times(1)).validateDeviceName("TEST_DEVICE");
		verify(addressTypeValidator, times(1)).validateNormalizedAddress(AddressType.MAC, "02:00:00:00:00:01");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateDevicesThrowsException() {

		final List<DeviceRequestDTO> devices = new ArrayList<DeviceRequestDTO>(1);
		devices.add(new DeviceRequestDTO("TEST_DEVICE", Map.of("indoor", true), List.of("02:00:00:00:00:01")));

		final List<NormalizedDeviceRequestDTO> expected = List.of(new NormalizedDeviceRequestDTO("TEST_DEVICE", Map.of("indoor", true), List.of(new AddressDTO("MAC", "02:00:00:00:00:01"))));
		when(normalizer.normalizeDeviceRequestDTOList(devices)).thenReturn(expected);
		doThrow(new InvalidParameterException("Validation error")).when(deviceNameValidator).validateDeviceName("TEST_DEVICE");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateDevices(new DeviceListRequestDTO(devices), "test origin"));
		assertEquals("Validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	// SERVICE DEFINITION

	// SYSTEM

	// SERVICE INTSTANCE

	// INTERFACE

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
    	utilitiesMock.when(() -> Utilities.isEmpty(Map.of())).thenReturn(true);
    	utilitiesMock.when(() -> Utilities.isEmpty(List.of())).thenReturn(true);
    }
}
