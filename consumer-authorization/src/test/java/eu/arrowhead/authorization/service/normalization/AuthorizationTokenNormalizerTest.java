package eu.arrowhead.authorization.service.normalization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.service.validation.cloud.CloudIdentifierNormalizer;
import eu.arrowhead.common.service.validation.name.EventTypeNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.dto.AuthorizationEncryptionKeyRegistrationRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationRequestDTO;

@ExtendWith(MockitoExtension.class)
public class AuthorizationTokenNormalizerTest {

	//=================================================================================================
	// members

	@InjectMocks
	private AuthorizationTokenNormalizer normalizer;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	@Mock
	private CloudIdentifierNormalizer cloudIdentifierNormalizer;

	@Mock
	private ServiceDefinitionNameNormalizer serviceDefNameNormalizer;

	@Mock
	private EventTypeNameNormalizer eventTypeNameNormalizer;

	@Mock
	private AuthorizationScopeNormalizer scopeNormalizer;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeSystemNameNullInput() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> normalizer.normalizeSystemName(null));

		assertEquals("System name is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeSystemNameOk() {
		when(systemNameNormalizer.normalize("TestProvider")).thenReturn("TestProvider");

		final String result = normalizer.normalizeSystemName("TestProvider");

		verify(systemNameNormalizer).normalize("TestProvider");

		assertEquals("TestProvider", result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeTokenEmptyInput() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> normalizer.normalizeToken(""));

		assertEquals("Token is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeTokenOk() {
		assertEquals("aToken", normalizer.normalizeToken("\taToken   "));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeAuthorizationTokenGenerationRequestDTONullInput() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> normalizer.normalizeAuthorizationTokenGenerationRequestDTO(null));

		assertEquals("AuthorizationTokenGenerationRequestDTO is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeAuthorizationTokenGenerationRequestDTOOk1() {
		final AuthorizationTokenGenerationRequestDTO dto = new AuthorizationTokenGenerationRequestDTO(
				"time_limited_token_auth",
				"ProviderName",
				null,
				"testService",
				"op");

		when(serviceDefNameNormalizer.normalize("testService")).thenReturn("testService");
		when(systemNameNormalizer.normalize("ProviderName")).thenReturn("ProviderName");
		when(scopeNormalizer.normalize("op")).thenReturn("op");

		final AuthorizationTokenGenerationRequestDTO result = normalizer.normalizeAuthorizationTokenGenerationRequestDTO(dto);

		verify(serviceDefNameNormalizer).normalize("testService");
		verify(eventTypeNameNormalizer, never()).normalize(anyString());
		verify(systemNameNormalizer).normalize("ProviderName");
		verify(scopeNormalizer).normalize("op");

		assertEquals("TIME_LIMITED_TOKEN_AUTH", result.tokenVariant());
		assertEquals("ProviderName", result.provider());
		assertEquals("SERVICE_DEF", result.targetType());
		assertEquals("testService", result.target());
		assertEquals("op", result.scope());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeAuthorizationTokenGenerationRequestDTOOk2() {
		final AuthorizationTokenGenerationRequestDTO dto = new AuthorizationTokenGenerationRequestDTO(
				"time_limited_token_auth",
				"ProviderName",
				"event_type",
				"testEvent",
				null);

		when(eventTypeNameNormalizer.normalize("testEvent")).thenReturn("testEvent");
		when(systemNameNormalizer.normalize("ProviderName")).thenReturn("ProviderName");

		final AuthorizationTokenGenerationRequestDTO result = normalizer.normalizeAuthorizationTokenGenerationRequestDTO(dto);

		verify(serviceDefNameNormalizer, never()).normalize(anyString());
		verify(eventTypeNameNormalizer).normalize("testEvent");
		verify(systemNameNormalizer).normalize("ProviderName");
		verify(scopeNormalizer, never()).normalize(anyString());

		assertEquals("TIME_LIMITED_TOKEN_AUTH", result.tokenVariant());
		assertEquals("ProviderName", result.provider());
		assertEquals("EVENT_TYPE", result.targetType());
		assertEquals("testEvent", result.target());
		assertNull(result.scope());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeAuthorizationEncryptionKeyRegistrationRequestDTONullInput() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> normalizer.normalizeAuthorizationEncryptionKeyRegistrationRequestDTO(null));

		assertEquals("AuthorizationEncryptionKeyRegistrationRequestDTO is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeAuthorizationEncryptionKeyRegistrationRequestDTOOk1() {
		final AuthorizationEncryptionKeyRegistrationRequestDTO dto = new AuthorizationEncryptionKeyRegistrationRequestDTO("key", null);

		final AuthorizationEncryptionKeyRegistrationRequestDTO result = normalizer.normalizeAuthorizationEncryptionKeyRegistrationRequestDTO(dto);

		assertEquals("key", result.key());
		assertEquals("AES/ECB/PKCS5Padding", result.algorithm());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeAuthorizationEncryptionKeyRegistrationRequestDTOOk2() {
		final AuthorizationEncryptionKeyRegistrationRequestDTO dto = new AuthorizationEncryptionKeyRegistrationRequestDTO("key", " AES/CBC/PKCS5Padding ");

		final AuthorizationEncryptionKeyRegistrationRequestDTO result = normalizer.normalizeAuthorizationEncryptionKeyRegistrationRequestDTO(dto);

		assertEquals("key", result.key());
		assertEquals("AES/CBC/PKCS5Padding", result.algorithm());
	}
}