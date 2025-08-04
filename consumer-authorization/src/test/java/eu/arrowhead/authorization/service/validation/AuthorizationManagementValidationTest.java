package eu.arrowhead.authorization.service.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.authorization.service.normalization.AuthorizationPolicyInstanceIdentifierNormalizer;
import eu.arrowhead.authorization.service.normalization.AuthorizationPolicyRequestNormalizer;
import eu.arrowhead.authorization.service.normalization.AuthorizationScopeNormalizer;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.PageValidator;
import eu.arrowhead.common.service.validation.cloud.CloudIdentifierNormalizer;
import eu.arrowhead.common.service.validation.cloud.CloudIdentifierValidator;
import eu.arrowhead.common.service.validation.name.EventTypeNameNormalizer;
import eu.arrowhead.common.service.validation.name.EventTypeNameValidator;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.dto.AuthorizationMgmtGrantListRequestDTO;
import eu.arrowhead.dto.AuthorizationMgmtGrantRequestDTO;
import eu.arrowhead.dto.AuthorizationPolicyRequestDTO;
import eu.arrowhead.dto.enums.AuthorizationPolicyType;

@ExtendWith(MockitoExtension.class)
public class AuthorizationManagementValidationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private AuthorizationManagementValidation validator;

	@Mock
	private SystemNameValidator systemNameValidator;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	@Mock
	private CloudIdentifierValidator cloudIdentifierValidator;

	@Mock
	private CloudIdentifierNormalizer cloudIdentifierNormalizer;

	@Mock
	private ServiceDefinitionNameValidator serviceDefNameValidator;

	@Mock
	private ServiceDefinitionNameNormalizer serviceDefNameNormalizer;

	@Mock
	private EventTypeNameValidator eventTypeNameValidator;

	@Mock
	private EventTypeNameNormalizer eventTypeNameNormalizer;

	@Mock
	private AuthorizationScopeValidator scopeValidator;

	@Mock
	private AuthorizationScopeNormalizer scopeNormalizer;

	@Mock
	private AuthorizationPolicyRequestValidator policyRequestValidator;

	@Mock
	private AuthorizationPolicyRequestNormalizer policyRequestNormalizer;

	@Mock
	private AuthorizationPolicyInstanceIdentifierValidator instanceIdValidator;

	@Mock
	private AuthorizationPolicyInstanceIdentifierNormalizer instanceIdNormalizer;

	@Mock
	private PageValidator pageValidator;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeSystemNameOriginNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateAndNormalizeSystemName("ProviderName", null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeSystemNameEmpty() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeSystemName("", "testOrigin"));

		assertEquals("System name is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeSystemNameExceptionInValidator() {
		when(systemNameNormalizer.normalize("ProviderName")).thenReturn("ProviderName");
		doThrow(new InvalidParameterException("test")).when(systemNameValidator).validateSystemName("ProviderName");

		final ArrowheadException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeSystemName("ProviderName", "testOrigin"));

		verify(systemNameNormalizer).normalize("ProviderName");
		verify(systemNameValidator).validateSystemName("ProviderName");

		assertEquals("test", ex.getMessage());
		assertEquals("testOrigin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeSystemNameOk() {
		when(systemNameNormalizer.normalize("ProviderName")).thenReturn("ProviderName");
		doNothing().when(systemNameValidator).validateSystemName("ProviderName");

		final String result = validator.validateAndNormalizeSystemName("ProviderName", "testOrigin");

		final InOrder orderVerifier = Mockito.inOrder(systemNameNormalizer, systemNameValidator);
		orderVerifier.verify(systemNameNormalizer).normalize("ProviderName");
		orderVerifier.verify(systemNameValidator).validateSystemName("ProviderName");

		assertEquals("ProviderName", result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGrantListRequestOriginEmpty() {
		final AuthorizationMgmtGrantListRequestDTO request = new AuthorizationMgmtGrantListRequestDTO(null);

		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> validator.validateAndNormalizeGrantListRequest(request, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGrantListRequestNullRequest() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGrantListRequest(null, "testOrigin"));

		assertEquals("Request payload is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGrantListRequestEmptyList() {
		final AuthorizationMgmtGrantListRequestDTO request = new AuthorizationMgmtGrantListRequestDTO(List.of());

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGrantListRequest(request, "testOrigin"));

		assertEquals("Request payload is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGrantListRequestNullElement() {
		final List<AuthorizationMgmtGrantRequestDTO> list = new ArrayList<>(1);
		list.add(null);
		final AuthorizationMgmtGrantListRequestDTO request = new AuthorizationMgmtGrantListRequestDTO(list);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGrantListRequest(request, "testOrigin"));

		assertEquals("Request payload list contains null element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGrantListRequestMissingTargetType() {
		final List<AuthorizationMgmtGrantRequestDTO> list = List.of(
				new AuthorizationMgmtGrantRequestDTO(
						"LOCAL",
						"ProviderName",
						"",
						"testService",
						"description",
						new AuthorizationPolicyRequestDTO.Builder(AuthorizationPolicyType.ALL)
								.build(),
						Map.of()));
		final AuthorizationMgmtGrantListRequestDTO request = new AuthorizationMgmtGrantListRequestDTO(list);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGrantListRequest(request, "testOrigin"));

		assertEquals("Target type is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGrantListRequestInvalidTargetType() {
		final List<AuthorizationMgmtGrantRequestDTO> list = List.of(
				new AuthorizationMgmtGrantRequestDTO(
						"LOCAL",
						"ProviderName",
						"invalid",
						"testService",
						"description",
						new AuthorizationPolicyRequestDTO.Builder(AuthorizationPolicyType.ALL)
								.build(),
						Map.of()));
		final AuthorizationMgmtGrantListRequestDTO request = new AuthorizationMgmtGrantListRequestDTO(list);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGrantListRequest(request, "testOrigin"));

		assertEquals("Target type is invalid: invalid", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeGrantListRequestMissingTarget() {
		final List<AuthorizationMgmtGrantRequestDTO> list = List.of(
				new AuthorizationMgmtGrantRequestDTO(
						"LOCAL",
						"ProviderName",
						"SERVICE_DEF",
						null,
						"description",
						new AuthorizationPolicyRequestDTO.Builder(AuthorizationPolicyType.ALL)
								.build(),
						Map.of()));
		final AuthorizationMgmtGrantListRequestDTO request = new AuthorizationMgmtGrantListRequestDTO(list);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeGrantListRequest(request, "testOrigin"));

		assertEquals("Target is missing", ex.getMessage());
	}
}