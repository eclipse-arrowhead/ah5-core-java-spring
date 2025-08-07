/*******************************************************************************
 *
 * Copyright (c) 2025 AITIA
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 *
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  	AITIA - implementation
 *  	Arrowhead Consortia - conceptualization
 *
 *******************************************************************************/
package eu.arrowhead.serviceregistry.service.normalization;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.validation.address.AddressNormalizer;
import eu.arrowhead.common.service.validation.address.AddressValidator;
import eu.arrowhead.common.service.validation.name.DeviceNameNormalizer;
import eu.arrowhead.common.service.validation.name.InterfaceTemplateNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.serviceinstance.ServiceInstanceIdentifierNormalizer;
import eu.arrowhead.common.service.validation.version.VersionNormalizer;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.dto.SystemListRequestDTO;
import eu.arrowhead.dto.SystemQueryRequestDTO;
import eu.arrowhead.dto.SystemRequestDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.service.dto.NormalizedSystemRequestDTO;
import eu.arrowhead.serviceregistry.service.validation.interf.InterfaceNormalizer;

@ExtendWith(MockitoExtension.class)
public class ManagementNormalizationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private ManagementNormalization normalizer;

	@Mock
	private AddressValidator addressValidator;

	@Mock
	private AddressNormalizer addressNormalizer;

	@Mock
	private VersionNormalizer versionNormalizer;

	@Mock
	private InterfaceNormalizer interfaceNormalizer;

	@Mock
	private DeviceNameNormalizer deviceNameNormalizer;

	@Mock
	private ServiceDefinitionNameNormalizer serviceDefNameNormalizer;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	@Mock
	private ServiceInstanceIdentifierNormalizer serviceInstanceIdentifierNormalizer;

	@Mock
	private InterfaceTemplateNameNormalizer interfaceTemplateNameNormalizer;

	private static MockedStatic<Utilities> utilitiesMock;

	private static final String EMPTY = "\n ";

	//=================================================================================================
	// members

	// SYSTEMS

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeSystemRequestDTOsNotEmptyList() {

		when(systemNameNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
		when(versionNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
		when(addressNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
		when(deviceNameNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
		when(addressValidator.detectType("192.168.4.4")).thenReturn(AddressType.IPV4);

		// empty addresses
		final SystemRequestDTO dto1 = new SystemRequestDTO("TemperatureConsumer", Map.of("indoor", false), "1.0.0", List.of(), "TEST_DEVICE");
		final NormalizedSystemRequestDTO expected1 = new NormalizedSystemRequestDTO("TemperatureConsumer", Map.of("indoor", false), "1.0.0", new ArrayList<>(), "TEST_DEVICE");

		// empty device name
		final SystemRequestDTO dto2 = new SystemRequestDTO("TemperatureConsumer", Map.of("indoor", false), "1.0.0", List.of("192.168.4.4"), EMPTY);
		final NormalizedSystemRequestDTO expected2 = new NormalizedSystemRequestDTO("TemperatureConsumer", Map.of("indoor", false), "1.0.0", List.of(new AddressDTO("IPV4", "192.168.4.4")), null);

		final List<NormalizedSystemRequestDTO> normalized = assertDoesNotThrow(() -> normalizer.normalizeSystemRequestDTOs(new SystemListRequestDTO(List.of(dto1, dto2))));
		assertEquals(List.of(expected1, expected2), normalized);
		verify(systemNameNormalizer, times(2)).normalize(anyString());
		verify(versionNormalizer, times(2)).normalize(anyString());
		verify(addressNormalizer, times(1)).normalize(anyString());
		verify(deviceNameNormalizer, times(1)).normalize(anyString());
		utilitiesMock.verify(() -> Utilities.isEmpty(List.of()), times(1));
		utilitiesMock.verify(() -> Utilities.isEmpty(EMPTY), times(1));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeSystemRequestDTOsEmptyList() {

		final List<NormalizedSystemRequestDTO> normalized = assertDoesNotThrow(() -> normalizer.normalizeSystemRequestDTOs(new SystemListRequestDTO(List.of())));
		assertEquals(new ArrayList<>(), normalized);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testNormalizeSystemQueryRequestDTO() {

		when(systemNameNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
		when(versionNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
		when(addressNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
		when(deviceNameNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

		assertAll(

				// nothing is empty
				() -> {
					final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
					requirement.put("priority", Map.of("op", "LESS_THAN", "value", 10));

					final SystemQueryRequestDTO dto = new SystemQueryRequestDTO(
							new PageDTO(10, 20, "ASC", "id"), List.of("TemperatureConsumer"), List.of("192.168.4.4"), "IPv4 \n", List.of(requirement), List.of("5.0.0"), List.of("TEST_DEVICE"));
					final SystemQueryRequestDTO expected = new SystemQueryRequestDTO(
							new PageDTO(10, 20, "ASC", "id"), List.of("TemperatureConsumer"), List.of("192.168.4.4"), "IPV4", List.of(requirement), List.of("5.0.0"), List.of("TEST_DEVICE"));

					final SystemQueryRequestDTO normalized = assertDoesNotThrow(() -> normalizer.normalizeSystemQueryRequestDTO(dto));
					assertEquals(expected, normalized);
					verify(systemNameNormalizer, times(1)).normalize("TemperatureConsumer");
					verify(versionNormalizer, times(1)).normalize("5.0.0");
					verify(addressNormalizer, times(1)).normalize("192.168.4.4");
					verify(deviceNameNormalizer, times(1)).normalize("TEST_DEVICE");
				},

				// everything is empty
				() -> {
					resetUtilitiesMock();
					final SystemQueryRequestDTO dto = new SystemQueryRequestDTO(null, List.of(), List.of(), EMPTY, List.of(), List.of(), List.of());
					final SystemQueryRequestDTO expected = new SystemQueryRequestDTO(null, null, null, null, List.of(), null, null);

					final SystemQueryRequestDTO normalized = assertDoesNotThrow(() -> normalizer.normalizeSystemQueryRequestDTO(dto));
					assertEquals(expected, normalized);
					utilitiesMock.verify(() -> Utilities.isEmpty(eq(List.of())), times(4));
					utilitiesMock.verify(() -> Utilities.isEmpty(eq(EMPTY)), times(1));

				},

				// dto is empty
				() -> {
					final SystemQueryRequestDTO expected = new SystemQueryRequestDTO(null, null, null, null, null, null, null);
					final SystemQueryRequestDTO normalized = assertDoesNotThrow(() -> normalizer.normalizeSystemQueryRequestDTO(null));
					assertEquals(expected, normalized);
				});
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void tesNormalizeRemoveSystemNames() {

		when(systemNameNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

		final List<String> toNormalize = new ArrayList<>(3);
		toNormalize.add("TemperatureProvider");
		toNormalize.add(null);
		toNormalize.add("TemperatureConsumer");

		final List<String> normalized = assertDoesNotThrow(() -> normalizer.normalizeRemoveSystemNames(toNormalize));
		assertEquals(List.of("TemperatureProvider", "TemperatureConsumer"), normalized);
	}

	// DEVICES

	// SERVICE DEFINITIONS

	// INTERFACE TEMPLATES

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
		utilitiesMock.when(() -> Utilities.isEmpty((String) null)).thenReturn(true);
		utilitiesMock.when(() -> Utilities.isEmpty(List.of())).thenReturn(true);
	}

	//-------------------------------------------------------------------------------------------------
	@AfterAll
	private static void closeUtilitiesMock() {
		utilitiesMock.close();
	}
}
