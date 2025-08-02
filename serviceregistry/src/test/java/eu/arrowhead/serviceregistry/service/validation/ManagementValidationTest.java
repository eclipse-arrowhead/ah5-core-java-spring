package eu.arrowhead.serviceregistry.service.validation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
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
import eu.arrowhead.dto.DeviceQueryRequestDTO;
import eu.arrowhead.dto.DeviceRequestDTO;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.dto.ServiceDefinitionListRequestDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.jpa.entity.Device;
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
	private static final String NULL_OR_EMPTY_DEVICE_NAME = "Device name list contains null or empty element";
	private static final String NULL_OR_EMPTY_ADDRESS = "Address list contains null element or empty element";
	private static final String INVALID_ADDRESS_TYPE_PREFIX = "Invalid address type: ";
	private static final String NULL_METADATA_REQUIREMENT = "Metadata requirement list contains null element";
	private static final String MISSING_DEVICE_NAME_LIST = "Device name list is missing or empty";
	private static final String EMPTY_SERVICE_DEF_LIST = "Service definition name list is empty";
	private static final String SERVICE_DEF_NAME_LIST_NULL_OR_EMPTY_ELEMENT = "Service definition name list contains null or empty element";
	private static final String DUPLICATED_SERVICE_DEF_NAME_PREFIX = "Duplicated service defitition name: ";
	private static final String MISSING_SERVICE_DEF_NAME_LIST = "Service definition name list is missing or empty";
	
	//=================================================================================================
	// methods

	// DEVICE

	// create

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

	// update

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateDeviceMissingPayload() {

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateDevices(null, "test origin"));
		assertEquals(MISSING_PAYLOAD, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateDeviceEmptyPayload() {

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateDevices(new DeviceListRequestDTO(List.of()), "test origin"));
		assertEquals(EMPTY_PAYLOAD, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.isEmpty(List.of()));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateDeviceNullElement() {

		final List<DeviceRequestDTO> devices = new ArrayList<DeviceRequestDTO>(2);
		devices.add(new DeviceRequestDTO("TEST_DEVICE", Map.of("indoor", true), List.of("02:00:00:00:00:01")));
		devices.add(null);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateDevices(new DeviceListRequestDTO(devices), "test origin"));
		assertEquals(NULL_DEVICE, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateDeviceEmptyName() {

		final List<DeviceRequestDTO> devices = List.of(new DeviceRequestDTO(EMPTY, Map.of("indoor", true), List.of("02:00:00:00:00:01")));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateDevices(new DeviceListRequestDTO(devices), "test origin"));
		assertEquals(EMPTY_DEVICE_NAME, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.isEmpty(EMPTY));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateDeviceDuplicateName() {

		final List<DeviceRequestDTO> devices = new ArrayList<DeviceRequestDTO>(2);
		devices.add(new DeviceRequestDTO("TEST_DEVICE", Map.of("indoor", true), List.of("02:00:00:00:00:01")));
		devices.add(new DeviceRequestDTO(" test_device ", Map.of("indoor", false), List.of("02:00:00:00:00:02")));

		when(deviceNameNormalizer.normalize("TEST_DEVICE")).thenReturn("TEST_DEVICE");
		when(deviceNameNormalizer.normalize(" test_device ")).thenReturn("TEST_DEVICE");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateDevices(new DeviceListRequestDTO(devices), "test origin"));
		assertEquals(DUPLICATE_DEVICE_NAME_PREFIX + "TEST_DEVICE", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateDeviceMissingAddresses() {

		final List<DeviceRequestDTO> devices = List.of(new DeviceRequestDTO("TEST_DEVICE", Map.of("indoor", true), List.of()));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateDevices(new DeviceListRequestDTO(devices), "test origin"));
		assertEquals(MISSING_ADDRESS_LIST, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.isEmpty(List.of()));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateDeviceMissingAddress() {

		final List<DeviceRequestDTO> devices = List.of(new DeviceRequestDTO("TEST_DEVICE", Map.of("indoor", true), List.of("02:00:00:00:00:01", EMPTY)));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateDevices(new DeviceListRequestDTO(devices), "test origin"));
		assertEquals(MISSING_ADDRESS, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.isEmpty(EMPTY));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateDeviceValidateMetadata() {

		final MockedStatic<MetadataValidation> metadataValidationMock = mockStatic(MetadataValidation.class);

		assertAll(

			// invalid metadata
			() -> {
				final List<DeviceRequestDTO> devicesInvalidMetadata = List.of(new DeviceRequestDTO("TEST_DEVICE", Map.of("in.door", true), List.of("02:00:00:00:00:01")));

				metadataValidationMock.when(() -> MetadataValidation.validateMetadataKey(Map.of("in.door", true))).thenThrow(new InvalidParameterException("Validation error"));

				final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateDevices(new DeviceListRequestDTO(devicesInvalidMetadata), "test origin"));
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

				final List<NormalizedDeviceRequestDTO> normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeUpdateDevices(new DeviceListRequestDTO(devicesEmptyMetadata), "test origin"));
				assertEquals(normalized, expected);
				metadataValidationMock.verify(() -> MetadataValidation.validateMetadataKey(Map.of("in.door", true)), never());
				metadataValidationMock.close();
			}
		);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeUpdateDevicesOk() {

		final List<DeviceRequestDTO> devices = new ArrayList<DeviceRequestDTO>(1);
		devices.add(new DeviceRequestDTO("TEST_DEVICE", Map.of("indoor", true), List.of("02:00:00:00:00:01")));

		final List<NormalizedDeviceRequestDTO> expected = List.of(new NormalizedDeviceRequestDTO("TEST_DEVICE", Map.of("indoor", true), List.of(new AddressDTO("MAC", "02:00:00:00:00:01"))));
		when(normalizer.normalizeDeviceRequestDTOList(devices)).thenReturn(expected);

		final List<NormalizedDeviceRequestDTO> normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeUpdateDevices(new DeviceListRequestDTO(devices), "test origin"));
		assertEquals(expected, normalized);
		verify(normalizer, times(1)).normalizeDeviceRequestDTOList(devices);
		verify(deviceNameValidator, times(1)).validateDeviceName("TEST_DEVICE");
		verify(addressTypeValidator, times(1)).validateNormalizedAddress(AddressType.MAC, "02:00:00:00:00:01");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeUpdateDevicesThrowsException() {

		final List<DeviceRequestDTO> devices = new ArrayList<DeviceRequestDTO>(1);
		devices.add(new DeviceRequestDTO("TEST_DEVICE", Map.of("indoor", true), List.of("02:00:00:00:00:01")));

		final List<NormalizedDeviceRequestDTO> expected = List.of(new NormalizedDeviceRequestDTO("TEST_DEVICE", Map.of("indoor", true), List.of(new AddressDTO("MAC", "02:00:00:00:00:01"))));
		when(normalizer.normalizeDeviceRequestDTOList(devices)).thenReturn(expected);
		doThrow(new InvalidParameterException("Validation error")).when(deviceNameValidator).validateDeviceName("TEST_DEVICE");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateDevices(new DeviceListRequestDTO(devices), "test origin"));
		assertEquals("Validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	// query

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateQueryDevicesMissingName() {

		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final DeviceQueryRequestDTO dto = new DeviceQueryRequestDTO(
				new PageDTO(10, 20, "ASC", "id"),
				List.of(EMPTY),
				List.of("02:00:00:00:00:01"),
				"MAC",
				List.of(metadataReq));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryDevices(dto, "test origin"));
		assertEquals(NULL_OR_EMPTY_DEVICE_NAME, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateQueryDevicesMissingAddress() {

		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final DeviceQueryRequestDTO dto = new DeviceQueryRequestDTO(
				new PageDTO(10, 20, "ASC", "id"),
				List.of("TEST_DEVICE"),
				List.of(EMPTY),
				"MAC",
				List.of(metadataReq));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryDevices(dto, "test origin"));
		assertEquals(NULL_OR_EMPTY_ADDRESS, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateQueryDevicesInvalidAddressType() {

		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final DeviceQueryRequestDTO dto = new DeviceQueryRequestDTO(
				new PageDTO(10, 20, "ASC", "id"),
				List.of("TEST_DEVICE"),
				List.of("02:00:00:00:00:01"),
				"MäC",
				List.of(metadataReq));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryDevices(dto, "test origin"));
		assertEquals(INVALID_ADDRESS_TYPE_PREFIX + "MäC", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateQueryDevicesNullMetadataRequirement() {

		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final List<MetadataRequirementDTO> requirements = new ArrayList<MetadataRequirementDTO>(2);
		requirements.add(metadataReq);
		requirements.add(null);

		utilitiesMock.when(() -> Utilities.containsNull(requirements)).thenReturn(true);

		final DeviceQueryRequestDTO dto = new DeviceQueryRequestDTO(
				new PageDTO(10, 20, "ASC", "id"),
				List.of("TEST_DEVICE"),
				List.of("02:00:00:00:00:01"),
				"MAC",
				requirements);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryDevices(dto, "test origin"));
		assertEquals(NULL_METADATA_REQUIREMENT, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryDevicesOk() {

		assertAll(

			// dto is null
			() -> {

				final DeviceQueryRequestDTO expected = new DeviceQueryRequestDTO(null, null, null, null, null);
				when(normalizer.normalizeDeviceQueryRequestDTO(null)).thenReturn(expected);

				final DeviceQueryRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeQueryDevices(null, "test origin"));
				assertEquals(expected, normalized);
			},

			// everything is empty
			() -> {

				final DeviceQueryRequestDTO dto = new DeviceQueryRequestDTO(null, null, null, null, null);
				when(normalizer.normalizeDeviceQueryRequestDTO(dto)).thenReturn(dto);

				final DeviceQueryRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeQueryDevices(dto, "test origin"));
				assertEquals(dto, normalized);

			},

			// nothing is empty
			() -> {

				final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
				metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

				final DeviceQueryRequestDTO dto = new DeviceQueryRequestDTO(
						new PageDTO(10, 20, "ASC", "id"),
						List.of("TEST_DEVICE"),
						List.of("02:00:00:00:00:01"),
						"MAC",
						List.of(metadataReq));
				when(normalizer.normalizeDeviceQueryRequestDTO(dto)).thenReturn(dto);

				final DeviceQueryRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeQueryDevices(dto, "test origin"));
				assertEquals(dto, normalized);
				verify(pageValidator, times(1)).validatePageParameter(new PageDTO(10, 20, "ASC", "id"), Device.SORTABLE_FIELDS_BY, "test origin");
				verify(normalizer, times(1)).normalizeDeviceQueryRequestDTO(dto);
				verify(deviceNameValidator, times(1)).validateDeviceName("TEST_DEVICE");
				verify(addressTypeValidator, times(1)).validateNormalizedAddress(AddressType.MAC, "02:00:00:00:00:01");
			},

			// address type is present, but addresses are empty
			() -> {

				final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
				metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

				final DeviceQueryRequestDTO dto = new DeviceQueryRequestDTO(
						new PageDTO(10, 20, "ASC", "id"),
						List.of("TEST_DEVICE"),
						List.of(),
						"MAC",
						List.of(metadataReq));
				when(normalizer.normalizeDeviceQueryRequestDTO(dto)).thenReturn(dto);

				final DeviceQueryRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeQueryDevices(dto, "test origin"));
				assertEquals(dto, normalized);
			}
		);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryDevicesThrowsException() {

		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final DeviceQueryRequestDTO dto = new DeviceQueryRequestDTO(
				new PageDTO(10, 20, "ASC", "id"),
				List.of("TEST|DEVICE"),
				List.of("02:00:00:00:00:01"),
				"MAC",
				List.of(metadataReq));

		when(normalizer.normalizeDeviceQueryRequestDTO(dto)).thenReturn(dto);
		doThrow(new InvalidParameterException("Validation error")).when(deviceNameValidator).validateDeviceName("TEST|DEVICE");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryDevices(dto, "test origin"));

		assertEquals("Validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	// remove

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateRemoveDevicesMissingNameList() {

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRemoveDevices(List.of(), "test origin"));
		assertEquals(MISSING_DEVICE_NAME_LIST, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.isEmpty(List.of()));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateRemoveDevicesMissingName() {

		final List<String> deviceNames = new ArrayList<String>(2);
		deviceNames.add("TEST_DEVICE");
		deviceNames.add(EMPTY);

		utilitiesMock.when(() -> Utilities.containsNullOrEmpty(deviceNames)).thenReturn(true);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRemoveDevices(deviceNames, "test origin"));
		assertEquals(NULL_OR_EMPTY_DEVICE_NAME, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.containsNullOrEmpty(deviceNames));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRemoveDevicesOk() {

		final List<String> deviceNames = List.of("TEST_DEVICE1\n", "TEST_DEVICE2\n");
		final List<String> expected = List.of("TEST_DEVICE1", "TEST_DEVICE2");

		when(normalizer.normalizeDeviceNames(deviceNames)).thenReturn(expected);

		final List<String> normalized = validator.validateAndNormalizeRemoveDevices(deviceNames, "test origin");
		assertEquals(expected, normalized);
		verify(normalizer, times(1)).normalizeDeviceNames(deviceNames);
		verify(deviceNameValidator, times(2)).validateDeviceName(anyString());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRemoveDevicesThrowsExcetion() {

		final List<String> deviceNames = List.of("TEST_DEVICE1\n", "TEST_DEVICE2\n");
		final List<String> expected = List.of("TEST_DEVICE1", "TEST_DEVICE2");

		when(normalizer.normalizeDeviceNames(deviceNames)).thenReturn(expected);
		lenient().doThrow(new InvalidParameterException("Validation error")).when(deviceNameValidator).validateDeviceName("TEST_DEVICE2");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRemoveDevices(deviceNames, "test origin"));
		assertEquals("Validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	// SERVICE DEFINITION

	// create

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCreateServiceDefinitionsNullDto() {

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateServiceDefinitions(null, "test origin"));
		assertEquals(MISSING_PAYLOAD, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCreateServiceDefinitionsEmptyList() {

		final ServiceDefinitionListRequestDTO dto = new ServiceDefinitionListRequestDTO(List.of());
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateServiceDefinitions(dto, "test origin"));
		assertEquals(EMPTY_SERVICE_DEF_LIST, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCreateServiceDefinitionsNullOrEmptyName() {

		final ServiceDefinitionListRequestDTO dto = new ServiceDefinitionListRequestDTO(List.of(EMPTY));
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateServiceDefinitions(dto, "test origin"));
		assertEquals(SERVICE_DEF_NAME_LIST_NULL_OR_EMPTY_ELEMENT, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCreateServiceDefinitionsDuplicatedName() {

		final ServiceDefinitionListRequestDTO dto = new ServiceDefinitionListRequestDTO(List.of("temperature info", "temperatureInfo"));
		when(serviceDefNameNormalizer.normalize(any())).thenReturn("temperatureInfo");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateServiceDefinitions(dto, "test origin"));
		assertEquals(DUPLICATED_SERVICE_DEF_NAME_PREFIX + "temperatureInfo", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateServiceDefinitionsOk() {

		final ServiceDefinitionListRequestDTO dto = new ServiceDefinitionListRequestDTO(List.of("temperatureInfo", "alertService"));
		final List<String> expected = List.of("temperatureInfo", "alertService");

		when(serviceDefNameNormalizer.normalize(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(normalizer.normalizeCreateServiceDefinitions(any())).thenReturn(List.of("temperatureInfo", "alertService"));

		final List<String> normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeCreateServiceDefinitions(dto, "test origin"));
		assertEquals(expected, normalized);
		verify(normalizer, times(1)).normalizeCreateServiceDefinitions(any());
		verify(serviceDefNameValidator, times(2)).validateServiceDefinitionName(any());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateServiceDefinitionsThrowsException() {

		final ServiceDefinitionListRequestDTO dto = new ServiceDefinitionListRequestDTO(List.of("temperatureInfo", "alertService"));

		when(serviceDefNameNormalizer.normalize(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(normalizer.normalizeCreateServiceDefinitions(any())).thenReturn(List.of("temperatureInfo", "alertService"));
		lenient().doThrow(new InvalidParameterException("Validation error")).when(serviceDefNameValidator).validateServiceDefinitionName("alertService");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateServiceDefinitions(dto, "test origin"));
		assertEquals("Validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	// query

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateQueryServiceDefinitions() {

		assertAll(

			// dto is null
			() -> {
				assertDoesNotThrow(() -> validator.validateQueryServiceDefinitions(null, "test origin"));
				verify(pageValidator, never()).validatePageParameter(any(), any(), any());
			},

			// dto is not null
			() -> {
				final PageDTO dto = new PageDTO(10, 20, "ASC", "id");
				assertDoesNotThrow(() -> validator.validateQueryServiceDefinitions(dto, "test origin"));
				verify(pageValidator, times(1)).validatePageParameter(any(), any(), eq("test origin"));
			}
		);
	}

	// remove

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateRemoveServiceDefinitionsMissingNameList() {

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRemoveServiceDefinitions(List.of(), "test origin"));
		assertEquals(MISSING_SERVICE_DEF_NAME_LIST, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateRemoveServiceDefinitionsMissingName() {

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRemoveServiceDefinitions(List.of(EMPTY), "test origin"));
		assertEquals(SERVICE_DEF_NAME_LIST_NULL_OR_EMPTY_ELEMENT, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRemoveServiceDefinitionsOk() {

		final List<String> names = List.of("temperatureInfo", "alertService");
		when(normalizer.normalizeRemoveServiceDefinitions(any())).thenReturn(names);

		final List<String> normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeRemoveServiceDefinitions(names, "test origin"));
		assertEquals(names, normalized);
		verify(normalizer, times(1)).normalizeRemoveServiceDefinitions(any());
		verify(serviceDefNameValidator, times(2)).validateServiceDefinitionName(any());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRemoveServiceDefinitionsThrowsException() {

		final List<String> names = List.of("temperatureInfo", "alertService");
		when(normalizer.normalizeRemoveServiceDefinitions(any())).thenReturn(names);
		lenient().doThrow(new InvalidParameterException("Validation error")).when(serviceDefNameValidator).validateServiceDefinitionName("alertService");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRemoveServiceDefinitions(names, "test origin"));
		assertEquals("Validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

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
    	utilitiesMock.when(() -> Utilities.isEmpty((String)null)).thenReturn(true);
    	utilitiesMock.when(() -> Utilities.isEmpty((List<String>)null)).thenReturn(true);
    	utilitiesMock.when(() -> Utilities.isEmpty(Map.of())).thenReturn(true);
    	utilitiesMock.when(() -> Utilities.isEmpty(List.of())).thenReturn(true);
    	utilitiesMock.when(() -> Utilities.isEnumValue("MAC", AddressType.class)).thenReturn(true);
    	utilitiesMock.when(() -> Utilities.containsNullOrEmpty((List<String>)List.of(EMPTY))).thenReturn(true);
    }
}
