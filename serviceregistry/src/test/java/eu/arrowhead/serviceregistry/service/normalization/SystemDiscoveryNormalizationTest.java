package eu.arrowhead.serviceregistry.service.normalization;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
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
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.version.VersionNormalizer;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.SystemRequestDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.service.dto.NormalizedSystemRequestDTO;

@ExtendWith(MockitoExtension.class)
public class SystemDiscoveryNormalizationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private SystemDiscoveryNormalization normalizer;

	@Mock
	private AddressValidator addressValidator;

	@Mock
	private AddressNormalizer addressNormalizer;

	@Mock
	private VersionNormalizer versionNormalizer;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	@Mock
	private DeviceNameNormalizer deviceNameNormalizer;

	private static MockedStatic<Utilities> utilitiesMock;

	private static final String EMPTY = "\n ";

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeSystemRequestDTO() {

    	when(systemNameNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    	when(versionNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    	when(addressNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    	when(deviceNameNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    	when(addressValidator.detectType("192.168.4.4")).thenReturn(AddressType.IPV4);

		assertAll(
			// nothing is empty
			() -> {

				final SystemRequestDTO dto = new SystemRequestDTO("TemperatureConsumer", Map.of("indoor", false), "1.0.0", List.of("192.168.4.4"), "TEST_DEVICE");
				final NormalizedSystemRequestDTO expected = new NormalizedSystemRequestDTO("TemperatureConsumer", Map.of("indoor", false), "1.0.0", List.of(new AddressDTO("IPV4", "192.168.4.4")), "TEST_DEVICE");

				final NormalizedSystemRequestDTO normalized = assertDoesNotThrow(() -> normalizer.normalizeSystemRequestDTO(dto));
				assertEquals(expected, normalized);
			},

			// addresses are empty
			() -> {
				
				final SystemRequestDTO dto = new SystemRequestDTO("TemperatureConsumer", Map.of("indoor", false), "1.0.0", List.of(), "TEST_DEVICE");
				final NormalizedSystemRequestDTO expected = new NormalizedSystemRequestDTO("TemperatureConsumer", Map.of("indoor", false), "1.0.0", new ArrayList<>(), "TEST_DEVICE");

				final NormalizedSystemRequestDTO normalized = assertDoesNotThrow(() -> normalizer.normalizeSystemRequestDTO(dto));
				assertEquals(expected, normalized);
			},
			
			// device name is empty
			() -> {
				
			},
			
			// dto is null
			() -> {
				
			}
		);
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
    }
    
    //-------------------------------------------------------------------------------------------------
    @AfterAll
    private static void closeUtilitiesMock() {
    	utilitiesMock.close();
    }
}
