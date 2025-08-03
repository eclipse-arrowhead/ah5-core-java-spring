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

import org.junit.jupiter.api.Assertions;
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
import eu.arrowhead.dto.SystemListRequestDTO;
import eu.arrowhead.dto.SystemQueryRequestDTO;
import eu.arrowhead.dto.SystemRequestDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.jpa.entity.Device;
import eu.arrowhead.serviceregistry.service.dto.NormalizedDeviceRequestDTO;
import eu.arrowhead.serviceregistry.service.dto.NormalizedSystemRequestDTO;
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
	private static final String NULL_OR_EMPTY_ADDRESS = "Address list contains null or empty element";
	private static final String INVALID_ADDRESS_TYPE_PREFIX = "Invalid address type: ";
	private static final String NULL_METADATA_REQUIREMENT = "Metadata requirement list contains null element";
	private static final String MISSING_DEVICE_NAME_LIST = "Device name list is missing or empty";
	private static final String EMPTY_SERVICE_DEF_LIST = "Service definition name list is empty";
	private static final String SERVICE_DEF_NAME_LIST_NULL_OR_EMPTY_ELEMENT = "Service definition name list contains null or empty element";
	private static final String DUPLICATED_SERVICE_DEF_NAME_PREFIX = "Duplicated service defitition name: ";
	private static final String MISSING_SERVICE_DEF_NAME_LIST = "Service definition name list is missing or empty";
	private static final String SYSTEM_LIST_NULL_ELEMENT = "System list contains null element";
	private static final String EMPTY_SYSTEM_NAME = "System name is empty";
	private static final String DUPLICATED_SYSTEM_NAME_PREFIX = "Duplicated system name: ";
	private static final String MISSING_ADDRESS_VALUE = "Address value is missing";
	private static final String NO_ADDRESS_PROVIDED = "At least one system address is needed for every system";
	private static final String NULL_OR_EMPTY_SYSTEM_NAME = "System name list contains null or empty element";
	private static final String NULL_OR_EMPTY_VERSION = "Version list contains null or empty element";
	private static final String MISSING_SYSTEM_NAME_LIST = "System name list is missing or empty";

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

	// create

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCreateSystemsMissingPayload() {

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateSystems(null, "test origin"));
		assertEquals(MISSING_PAYLOAD, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCreateSystemsEmptyPayload() {

		final SystemListRequestDTO dto = new SystemListRequestDTO(List.of());
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateSystems(dto, "test origin"));
		assertEquals(EMPTY_PAYLOAD, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCreateSystemsNullElement() {

		final SystemRequestDTO element = new SystemRequestDTO("TemperatureProvider", Map.of("indoor", false), "1.0.0", List.of("greenhouse.com"), "TEST_DEVICE");
		final List<SystemRequestDTO> listWithNull = new ArrayList<SystemRequestDTO>(2);
		listWithNull.add(element);
		listWithNull.add(null);
		final SystemListRequestDTO dto = new SystemListRequestDTO(listWithNull);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateSystems(dto, "test origin"));
		assertEquals(SYSTEM_LIST_NULL_ELEMENT, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCreateSystemsEmptyName() {

		final SystemListRequestDTO dto = new SystemListRequestDTO(List.of(new SystemRequestDTO(EMPTY, Map.of("indoor", false), "1.0.0", List.of("greenhouse.com"), "TEST_DEVICE")));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateSystems(dto, "test origin"));
		assertEquals(EMPTY_SYSTEM_NAME, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCreateSystemsDuplicatedName() {

		final SystemRequestDTO element1 = new SystemRequestDTO("TemperatureProvider", Map.of("indoor", false), "1.0.0", List.of("greenhouse.com"), "TEST_DEVICE");
		final SystemRequestDTO element2 = new SystemRequestDTO("Temperature Provider", Map.of("indoor", true), "1.1.0", List.of("greenhouse.com"), "TEST_DEVICE");
		final List<SystemRequestDTO> systemList = new ArrayList<SystemRequestDTO>(2);
		systemList.add(element1);
		systemList.add(element2);
		final SystemListRequestDTO dto = new SystemListRequestDTO(systemList);

		when(systemNameNormalizer.normalize(any())).thenReturn("TemperatureProvider");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateSystems(dto, "test origin"));
		assertEquals(DUPLICATED_SYSTEM_NAME_PREFIX + "TemperatureProvider", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCreateSystemsMissingAddress() {

		final SystemRequestDTO element1 = new SystemRequestDTO("TemperatureProvider1", Map.of("indoor", false), "1.0.0", List.of("greenhouse.com"), "TEST_DEVICE");
		final SystemRequestDTO element2 = new SystemRequestDTO("TemperatureProvider2", Map.of("indoor", true), "1.1.0", List.of("greenhouse.com", EMPTY), "TEST_DEVICE");
		final List<SystemRequestDTO> systemList = new ArrayList<SystemRequestDTO>(2);
		systemList.add(element1);
		systemList.add(element2);
		final SystemListRequestDTO dto = new SystemListRequestDTO(systemList);

		when(systemNameNormalizer.normalize(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateSystems(dto, "test origin"));
		assertEquals(MISSING_ADDRESS_VALUE, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCreateSystemsNoAddressProvided() {
		final SystemRequestDTO system = new SystemRequestDTO("TemperatureProvider1", Map.of("indoor", false), "1.0.0", List.of(), null);
		final SystemListRequestDTO dto = new SystemListRequestDTO(List.of(system));

		when(systemNameNormalizer.normalize(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateSystems(dto, "test origin"));
		assertEquals(NO_ADDRESS_PROVIDED, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCreateSystemsInvalidMetadata() {

		final SystemRequestDTO system = new SystemRequestDTO("TemperatureProvider1", Map.of("in.door", false), "1.0.0", List.of("192.168.10.20"), "TEST_DEVICE");
		final SystemListRequestDTO dto = new SystemListRequestDTO(List.of(system));

		final MockedStatic<MetadataValidation> metadataValidationMock = mockStatic(MetadataValidation.class);
		metadataValidationMock.when(() -> MetadataValidation.validateMetadataKey(Map.of("in.door", false))).thenThrow(new InvalidParameterException("Validation error"));
		when(systemNameNormalizer.normalize(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateSystems(dto, "test origin"));
		assertEquals("Validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		metadataValidationMock.verify(() -> MetadataValidation.validateMetadataKey(Map.of("in.door", false)));
		metadataValidationMock.close();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateSystemsOk() {

		// not empty metadata, not empty addresses
		final SystemRequestDTO element1 = new SystemRequestDTO("TemperatureProvider1", Map.of("indoor", false), "1.0.0", List.of("192.168.20.21"), EMPTY);
		// empty metadata, empty addresses
		final SystemRequestDTO element2 = new SystemRequestDTO("TemperatureProvider2", Map.of(), "1.1.0", List.of(), "TEST_DEVICE1");

		final NormalizedSystemRequestDTO expected1 = new NormalizedSystemRequestDTO("TemperatureProvider1", Map.of("indoor", false), "1.0.0", List.of(new AddressDTO("IPV4", "192.168.20.21")), null);
		final NormalizedSystemRequestDTO expected2 = new NormalizedSystemRequestDTO("TemperatureProvider2", Map.of(), "1.1.0", List.of(), "TEST_DEVICE1");

		final MockedStatic<MetadataValidation> metadataValidationMock = mockStatic(MetadataValidation.class);
		when(systemNameNormalizer.normalize(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(normalizer.normalizeSystemRequestDTOs(any())).thenReturn(List.of(expected1, expected2));

		final List<NormalizedSystemRequestDTO> normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeCreateSystems(new SystemListRequestDTO(List.of(element1, element2)), "test origin"));
		assertEquals(List.of(expected1, expected2), normalized);
		verify(systemNameValidator, times(2)).validateSystemName(anyString());
		verify(versionValidator, times(2)).validateNormalizedVersion(anyString());
		verify(addressTypeValidator, times(1)).validateNormalizedAddress(any(), anyString());
		verify(deviceNameValidator, times(1)).validateDeviceName(anyString());
		metadataValidationMock.verify(() -> MetadataValidation.validateMetadataKey(any()), times(1));
		metadataValidationMock.close();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateSystemsThrowsException() {

		final SystemRequestDTO system = new SystemRequestDTO("TemperätureProvider", Map.of("indoor", false), "1.0.0", List.of("192.168.10.20"), "TEST_DEVICE");
		final NormalizedSystemRequestDTO normalizedSystem = new NormalizedSystemRequestDTO("TemperätureProvider", Map.of("indoor", false), "1.0.0", List.of(new AddressDTO("IPV4", "192.168.10.20")), "TEST_DEVICE");

		final SystemListRequestDTO dto = new SystemListRequestDTO(List.of(system));

		when(systemNameNormalizer.normalize(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(normalizer.normalizeSystemRequestDTOs(any())).thenReturn(List.of(normalizedSystem));
		doThrow(new InvalidParameterException("Validation error")).when(systemNameValidator).validateSystemName("TemperätureProvider");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateSystems(dto, "test origin"));
		assertEquals("Validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	// update

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateUpdateSystemsMissingPayload() {

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateSystems(null, "test origin"));
		assertEquals(MISSING_PAYLOAD, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateUpdateSystemsEmptyPayload() {

		final SystemListRequestDTO dto = new SystemListRequestDTO(List.of());
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateSystems(dto, "test origin"));
		assertEquals(EMPTY_PAYLOAD, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateUpdateSystemsNullElement() {

		final SystemRequestDTO element = new SystemRequestDTO("TemperatureProvider", Map.of("indoor", false), "1.0.0", List.of("greenhouse.com"), "TEST_DEVICE");
		final List<SystemRequestDTO> listWithNull = new ArrayList<SystemRequestDTO>(2);
		listWithNull.add(element);
		listWithNull.add(null);
		final SystemListRequestDTO dto = new SystemListRequestDTO(listWithNull);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateSystems(dto, "test origin"));
		assertEquals(SYSTEM_LIST_NULL_ELEMENT, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateUpdateSystemsEmptyName() {

		final SystemListRequestDTO dto = new SystemListRequestDTO(List.of(new SystemRequestDTO(EMPTY, Map.of("indoor", false), "1.0.0", List.of("greenhouse.com"), "TEST_DEVICE")));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateSystems(dto, "test origin"));
		assertEquals(EMPTY_SYSTEM_NAME, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateUpdateSystemsDuplicatedName() {

		final SystemRequestDTO element1 = new SystemRequestDTO("TemperatureProvider", Map.of("indoor", false), "1.0.0", List.of("greenhouse.com"), "TEST_DEVICE");
		final SystemRequestDTO element2 = new SystemRequestDTO("Temperature Provider", Map.of("indoor", true), "1.1.0", List.of("greenhouse.com"), "TEST_DEVICE");
		final List<SystemRequestDTO> systemList = new ArrayList<SystemRequestDTO>(2);
		systemList.add(element1);
		systemList.add(element2);
		final SystemListRequestDTO dto = new SystemListRequestDTO(systemList);

		when(systemNameNormalizer.normalize(any())).thenReturn("TemperatureProvider");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateSystems(dto, "test origin"));
		assertEquals(DUPLICATED_SYSTEM_NAME_PREFIX + "TemperatureProvider", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateUpdateSystemsMissingAddress() {

		final SystemRequestDTO element1 = new SystemRequestDTO("TemperatureProvider1", Map.of("indoor", false), "1.0.0", List.of("greenhouse.com"), "TEST_DEVICE");
		final SystemRequestDTO element2 = new SystemRequestDTO("TemperatureProvider2", Map.of("indoor", true), "1.1.0", List.of("greenhouse.com", EMPTY), "TEST_DEVICE");
		final List<SystemRequestDTO> systemList = new ArrayList<SystemRequestDTO>(2);
		systemList.add(element1);
		systemList.add(element2);
		final SystemListRequestDTO dto = new SystemListRequestDTO(systemList);

		when(systemNameNormalizer.normalize(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateSystems(dto, "test origin"));
		assertEquals(MISSING_ADDRESS_VALUE, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateUpdateSystemsNoAddressProvided() {
		final SystemRequestDTO system = new SystemRequestDTO("TemperatureProvider1", Map.of("indoor", false), "1.0.0", List.of(), null);
		final SystemListRequestDTO dto = new SystemListRequestDTO(List.of(system));

		when(systemNameNormalizer.normalize(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateSystems(dto, "test origin"));
		assertEquals(NO_ADDRESS_PROVIDED, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateUpdateSystemsInvalidMetadata() {

		final SystemRequestDTO system = new SystemRequestDTO("TemperatureProvider1", Map.of("in.door", false), "1.0.0", List.of("192.168.10.20"), "TEST_DEVICE");
		final SystemListRequestDTO dto = new SystemListRequestDTO(List.of(system));

		final MockedStatic<MetadataValidation> metadataValidationMock = mockStatic(MetadataValidation.class);
		metadataValidationMock.when(() -> MetadataValidation.validateMetadataKey(Map.of("in.door", false))).thenThrow(new InvalidParameterException("Validation error"));
		when(systemNameNormalizer.normalize(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateSystems(dto, "test origin"));
		assertEquals("Validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		metadataValidationMock.verify(() -> MetadataValidation.validateMetadataKey(Map.of("in.door", false)));
		metadataValidationMock.close();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeUpdateSystemsOk() {

		// not empty metadata, not empty addresses
		final SystemRequestDTO element1 = new SystemRequestDTO("TemperatureProvider1", Map.of("indoor", false), "1.0.0", List.of("192.168.20.21"), EMPTY);
		// empty metadata, empty addresses
		final SystemRequestDTO element2 = new SystemRequestDTO("TemperatureProvider2", Map.of(), "1.1.0", List.of(), "TEST_DEVICE1");

		final NormalizedSystemRequestDTO expected1 = new NormalizedSystemRequestDTO("TemperatureProvider1", Map.of("indoor", false), "1.0.0", List.of(new AddressDTO("IPV4", "192.168.20.21")), null);
		final NormalizedSystemRequestDTO expected2 = new NormalizedSystemRequestDTO("TemperatureProvider2", Map.of(), "1.1.0", List.of(), "TEST_DEVICE1");

		final MockedStatic<MetadataValidation> metadataValidationMock = mockStatic(MetadataValidation.class);
		when(systemNameNormalizer.normalize(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(normalizer.normalizeSystemRequestDTOs(any())).thenReturn(List.of(expected1, expected2));

		final List<NormalizedSystemRequestDTO> normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeUpdateSystems(new SystemListRequestDTO(List.of(element1, element2)), "test origin"));
		assertEquals(List.of(expected1, expected2), normalized);
		verify(systemNameValidator, times(2)).validateSystemName(anyString());
		verify(versionValidator, times(2)).validateNormalizedVersion(anyString());
		verify(addressTypeValidator, times(1)).validateNormalizedAddress(any(), anyString());
		verify(deviceNameValidator, times(1)).validateDeviceName(anyString());
		metadataValidationMock.verify(() -> MetadataValidation.validateMetadataKey(any()), times(1));
		metadataValidationMock.close();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeUpdateSystemsThrowsException() {

		final SystemRequestDTO system = new SystemRequestDTO("TemperätureProvider", Map.of("indoor", false), "1.0.0", List.of("192.168.10.20"), "TEST_DEVICE");
		final NormalizedSystemRequestDTO normalizedSystem = new NormalizedSystemRequestDTO("TemperätureProvider", Map.of("indoor", false), "1.0.0", List.of(new AddressDTO("IPV4", "192.168.10.20")), "TEST_DEVICE");

		final SystemListRequestDTO dto = new SystemListRequestDTO(List.of(system));

		when(systemNameNormalizer.normalize(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(normalizer.normalizeSystemRequestDTOs(any())).thenReturn(List.of(normalizedSystem));
		doThrow(new InvalidParameterException("Validation error")).when(systemNameValidator).validateSystemName("TemperätureProvider");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateSystems(dto, "test origin"));
		assertEquals("Validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	// query

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateQuerySystemsNullOrEmptySystemName() {

		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final SystemQueryRequestDTO dto = new SystemQueryRequestDTO(
				new PageDTO(10, 20, "ASC", "id"),
				List.of(EMPTY),
				List.of("192.168.4.1"),
				"IPV4",
				List.of(requirement),
				List.of("5.0.0"),
				List.of("TEST_DEVICE")
			);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQuerySystems(dto, "test origin"));
		assertEquals(NULL_OR_EMPTY_SYSTEM_NAME, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateQuerySystemsNullOrEmptyAddress() {

		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final SystemQueryRequestDTO dto = new SystemQueryRequestDTO(
				new PageDTO(10, 20, "ASC", "id"),
				List.of("TemperatureProvider1"),
				List.of(EMPTY),
				"IPV4",
				List.of(requirement),
				List.of("5.0.0"),
				List.of("TEST_DEVICE")
		);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQuerySystems(dto, "test origin"));
		assertEquals(NULL_OR_EMPTY_ADDRESS, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateQuerySystemsInvalidAddressType() {

		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final SystemQueryRequestDTO dto = new SystemQueryRequestDTO(
				new PageDTO(10, 20, "ASC", "id"),
				List.of("TemperatureProvider1"),
				List.of("192.168.4.1"),
				"IPV5",
				List.of(requirement),
				List.of("5.0.0"),
				List.of("TEST_DEVICE")
		);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQuerySystems(dto, "test origin"));
		assertEquals(INVALID_ADDRESS_TYPE_PREFIX + "IPV5", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.isEnumValue("IPV5", AddressType.class));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateQuerySystemsNullMetadataRequirement() {

		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final List<MetadataRequirementDTO> requirements = new ArrayList<MetadataRequirementDTO>(2);
		requirements.add(requirement);
		requirements.add(null);
		utilitiesMock.when(() -> Utilities.containsNull(requirements)).thenReturn(true);

		final SystemQueryRequestDTO dto = new SystemQueryRequestDTO(
				new PageDTO(10, 20, "ASC", "id"),
				List.of("TemperatureProvider1"),
				List.of("192.168.4.1"),
				"IPV4",
				requirements,
				List.of("5.0.0"),
				List.of("TEST_DEVICE")
		);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQuerySystems(dto, "test origin"));
		assertEquals(NULL_METADATA_REQUIREMENT, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateQuerySystemsNullOrEmptyVersion() {

		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final SystemQueryRequestDTO dto = new SystemQueryRequestDTO(
				new PageDTO(10, 20, "ASC", "id"),
				List.of("TemperatureProvider1"),
				List.of("192.168.4.1"),
				"IPV4",
				List.of(requirement),
				List.of(EMPTY),
				List.of("TEST_DEVICE")
		);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQuerySystems(dto, "test origin"));
		assertEquals(NULL_OR_EMPTY_VERSION, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateQuerySystemsNullOrEmptyDeviceName() {

		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final SystemQueryRequestDTO dto = new SystemQueryRequestDTO(
				new PageDTO(10, 20, "ASC", "id"),
				List.of("TemperatureProvider1"),
				List.of("192.168.4.1"),
				"IPV4",
				List.of(requirement),
				List.of("5.0.0"),
				List.of(EMPTY)
		);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQuerySystems(dto, "test origin"));
		assertEquals(NULL_OR_EMPTY_DEVICE_NAME, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQuerySystemsOk() {

		Assertions.assertAll(

			// nothing is null
			() -> {
				final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
				requirement.put("priority", Map.of("op", "LESS_THAN", "value", 10));

				final SystemQueryRequestDTO dto = new SystemQueryRequestDTO(
						new PageDTO(10, 20, "ASC", "id"),
						List.of("TemperatureProvider1"),
						List.of("192.168.4.1"),
						"IPV4",
						List.of(requirement),
						List.of("5.0.0"),
						List.of("TEST_DEVICE")
				);

				when(normalizer.normalizeSystemQueryRequestDTO(dto)).then(invocation -> invocation.getArgument(0));

				final SystemQueryRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeQuerySystems(dto, "test origin"));
				verify(pageValidator, times(1)).validatePageParameter(any(), any(), eq("test origin"));
				verify(normalizer, times(1)).normalizeSystemQueryRequestDTO(dto);
				verify(systemNameValidator, times(1)).validateSystemName("TemperatureProvider1");
				verify(addressTypeValidator, times(1)).validateNormalizedAddress(AddressType.IPV4, "192.168.4.1");
				verify(versionValidator, times(1)).validateNormalizedVersion("5.0.0");
				verify(deviceNameValidator, times(1)).validateDeviceName("TEST_DEVICE");
				assertEquals(dto, normalized);
			},

			// dto is null
			() -> {
				final SystemQueryRequestDTO expected = new SystemQueryRequestDTO(null, null, null, null, null, null, null);
				when(normalizer.normalizeSystemQueryRequestDTO(null)).thenReturn(expected);

				final SystemQueryRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeQuerySystems(null, "test origin"));
				assertEquals(expected, normalized);
			},

			// everything is null
			() -> {
				final SystemQueryRequestDTO dto = new SystemQueryRequestDTO(null, null, null, null, null, null, null);
				when(normalizer.normalizeSystemQueryRequestDTO(dto)).thenReturn(dto);

				final SystemQueryRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeQuerySystems(dto, "test origin"));
				assertEquals(dto, normalized);
			},

			// address type is present, but addresses are empty
			() -> {
				final SystemQueryRequestDTO dto = new SystemQueryRequestDTO(null, null, null, "IPV4", null, null, null);
				when(normalizer.normalizeSystemQueryRequestDTO(dto)).thenReturn(dto);

				final SystemQueryRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeQuerySystems(dto, "test origin"));
				assertEquals(dto, normalized);
			}
		);

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQuerySystemsThrowsException() {

		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final SystemQueryRequestDTO dto = new SystemQueryRequestDTO(
				new PageDTO(10, 20, "ASC", "id"),
				List.of("TemperätureProvider1"),
				List.of("192.168.4.1"),
				"IPV4",
				List.of(requirement),
				List.of("5.0.0"),
				List.of("TEST_DEVICE")
		);

		when(normalizer.normalizeSystemQueryRequestDTO(dto)).then(invocation -> invocation.getArgument(0));
		doThrow(new InvalidParameterException("Validation error")).when(systemNameValidator).validateSystemName("TemperätureProvider1");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQuerySystems(dto, "test origin"));
		assertEquals("Validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	// remove

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateRemoveSystemsEmptyNameList() {

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRemoveSystems(List.of(), "test origin"));
		assertEquals(MISSING_SYSTEM_NAME_LIST, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateRemoveSystemsNullOrEmptySystemName() {

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRemoveSystems(List.of(EMPTY), "test origin"));
		assertEquals(NULL_OR_EMPTY_SYSTEM_NAME, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRemoveSystemsOk() {

		when(normalizer.normalizeRemoveSystemNames(any())).thenAnswer(invocation -> invocation.getArgument(0));
		final List<String> normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeRemoveSystems(List.of("TemperatureProvider1", "TemperatureProvider2"), "test origin"));

		assertEquals(List.of("TemperatureProvider1", "TemperatureProvider2"), normalized);
		verify(normalizer, times(1)).normalizeRemoveSystemNames(any());
		verify(systemNameValidator, times(2)).validateSystemName(any());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRemoveSystemsThrowsException() {

		when(normalizer.normalizeRemoveSystemNames(any())).thenAnswer(invocation -> invocation.getArgument(0));
		lenient().doThrow(new InvalidParameterException("Validation error")).when(systemNameValidator).validateSystemName("TemperätureProvider2");
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRemoveSystems(List.of("TemperatureProvider1", "TemperätureProvider2"), "test origin"));
		assertEquals("Validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

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
    	utilitiesMock.when(() -> Utilities.isEnumValue("IPV4", AddressType.class)).thenReturn(true);
    	utilitiesMock.when(() -> Utilities.containsNullOrEmpty((List<String>)List.of(EMPTY))).thenReturn(true);
    }
}
