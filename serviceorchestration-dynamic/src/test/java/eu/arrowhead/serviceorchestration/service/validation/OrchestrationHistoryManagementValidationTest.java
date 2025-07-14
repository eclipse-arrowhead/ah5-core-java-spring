package eu.arrowhead.serviceorchestration.service.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.PageValidator;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.dto.OrchestrationHistoryQueryRequestDTO;
import eu.arrowhead.serviceorchestration.service.normalization.OrchestrationHistoryManagementNormalization;

@ExtendWith(MockitoExtension.class)
public class OrchestrationHistoryManagementValidationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private OrchestrationHistoryManagementValidation validator;

	@Mock
	private PageValidator pageValidator;

	@Mock
	private SystemNameValidator systemNameValidator;

	@Mock
	private ServiceDefinitionNameValidator serviceDefNameValidator;

	@Mock
	private OrchestrationHistoryManagementNormalization normalization;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceNullDTO() {
		when(normalization.normalizeOrchestrationHistoryQueryRequestDTO(any()))
				.thenReturn(new OrchestrationHistoryQueryRequestDTO(null, List.of(), List.of(), null, List.of(), List.of(), List.of(), List.of()));

		assertDoesNotThrow(() -> validator.validateAndNormalizeQueryService(null, "test.origin"));

		verify(normalization).normalizeOrchestrationHistoryQueryRequestDTO(isNull());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceContainsEmptyId() {
		final OrchestrationHistoryQueryRequestDTO dto = new OrchestrationHistoryQueryRequestDTO(null, List.of("id1", "", "id3"), List.of(), null, List.of(), List.of(), List.of(), List.of());
		doNothing().when(pageValidator).validatePageParameter(any(), anyList(), anyString());

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeQueryService(dto, "test.origin"));

		verify(pageValidator).validatePageParameter(any(), anyList(), anyString());

		assertEquals("ID list contains empty element", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}
}
