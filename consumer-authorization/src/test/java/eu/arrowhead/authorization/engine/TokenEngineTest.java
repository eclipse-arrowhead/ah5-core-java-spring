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
package eu.arrowhead.authorization.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Pair;

import eu.arrowhead.authorization.AuthorizationSystemInfo;
import eu.arrowhead.authorization.jpa.entity.TimeLimitedToken;
import eu.arrowhead.authorization.jpa.entity.TokenHeader;
import eu.arrowhead.authorization.jpa.entity.UsageLimitedToken;
import eu.arrowhead.authorization.jpa.service.EncryptionKeyDbService;
import eu.arrowhead.authorization.jpa.service.SelfContainedTokenDbService;
import eu.arrowhead.authorization.jpa.service.TimeLimitedTokenDbService;
import eu.arrowhead.authorization.jpa.service.TokenHeaderDbService;
import eu.arrowhead.authorization.jpa.service.UsageLimitedTokenDbService;
import eu.arrowhead.authorization.service.dto.SelfContainedTokenPayload;
import eu.arrowhead.authorization.service.engine.TokenEngine;
import eu.arrowhead.authorization.service.engine.TokenGenerator;
import eu.arrowhead.authorization.service.model.TokenModel;
import eu.arrowhead.authorization.service.utils.SecretCryptographer;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import eu.arrowhead.dto.enums.AuthorizationTokenType;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;

@SuppressWarnings("checkstyle:MagicNumber")
@ExtendWith(MockitoExtension.class)
public class TokenEngineTest {

	//=================================================================================================
	// members

	@InjectMocks
	private TokenEngine engine;

	@Mock
	private AuthorizationSystemInfo sysInfo;

	@Mock
	private TokenGenerator tokenGenerator;

	@Mock
	private SecretCryptographer secretCryptographer;

	@Mock
	private EncryptionKeyDbService encryptionKeyDbService;

	@Mock
	private TokenHeaderDbService tokenHeaderDbService;

	@Mock
	private UsageLimitedTokenDbService usageLimitedTokenDbService;

	@Mock
	private TimeLimitedTokenDbService timeLimitedTokenDbService;

	@Mock
	private SelfContainedTokenDbService selfContainedTokenDbService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testProduce8UsageOk() throws InvalidKeyException, NoSuchAlgorithmException {
		final Pair<UsageLimitedToken, Boolean> dbResult = Pair.of(
				new UsageLimitedToken(
						new TokenHeader(
								AuthorizationTokenType.USAGE_LIMITED_TOKEN,
								"hashedToken",
								"ConsumerName",
								"LOCAL",
								"ConsumerName",
								"ProviderName",
								AuthorizationTargetType.SERVICE_DEF,
								"testService",
								"op"),
						7),
				true);

		when(sysInfo.getSimpleTokenByteSize()).thenReturn(16);
		when(tokenGenerator.generateSimpleToken(16)).thenReturn("0123456789abcdef");
		when(sysInfo.getSecretCryptographerKey()).thenReturn("aVerySecretKey");
		when(secretCryptographer.encrypt_HMAC_SHA256("0123456789abcdef", "aVerySecretKey")).thenReturn("hashedToken");
		when(tokenHeaderDbService.find("hashedToken")).thenReturn(Optional.empty());
		when(sysInfo.getSimpleTokenUsageLimit()).thenReturn(7);
		when(usageLimitedTokenDbService.save(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op",
				7)).thenReturn(dbResult);

		final TokenModel result = engine.produce(
				"ConsumerName",
				"ConsumerName",
				ServiceInterfacePolicy.USAGE_LIMITED_TOKEN_AUTH,
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op",
				"testOrigin");

		verify(sysInfo).getSimpleTokenByteSize();
		verify(tokenGenerator).generateSimpleToken(16);
		verify(sysInfo).getSecretCryptographerKey();
		verify(secretCryptographer).encrypt_HMAC_SHA256("0123456789abcdef", "aVerySecretKey");
		verify(tokenHeaderDbService).find("hashedToken");
		verify(sysInfo).getSimpleTokenUsageLimit();
		verify(usageLimitedTokenDbService).save(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op",
				7);

		assertFalse(result.isEncrypted());
		assertEquals(AuthorizationTokenType.USAGE_LIMITED_TOKEN, result.getTokenType());
		assertEquals("0123456789abcdef", result.getRawToken());
		assertEquals("hashedToken", result.getHashedToken());
		assertEquals("ConsumerName", result.getRequester());
		assertEquals("LOCAL", result.getConsumerCloud());
		assertEquals("ConsumerName", result.getConsumer());
		assertEquals("ProviderName", result.getProvider());
		assertEquals(AuthorizationTargetType.SERVICE_DEF, result.getTargetType());
		assertEquals("testService", result.getTarget());
		assertEquals("op", result.getScope());
		assertEquals("USAGE_LIMITED_TOKEN_AUTH", result.getVariant());
		assertEquals(7, result.getUsageLimit());
		assertEquals(7, result.getUsageLeft());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testProduce8TimeOk() throws InvalidKeyException, NoSuchAlgorithmException {
		final ZonedDateTime expiresAt = ZonedDateTime.of(2025, 8, 6, 12, 0, 10, 0, ZoneId.of("UTC"));
		final Pair<TimeLimitedToken, Boolean> dbResult = Pair.of(
				new TimeLimitedToken(
						new TokenHeader(
								AuthorizationTokenType.TIME_LIMITED_TOKEN,
								"hashedToken",
								"ConsumerName",
								"LOCAL",
								"ConsumerName",
								"ProviderName",
								AuthorizationTargetType.SERVICE_DEF,
								"testService",
								"op"),
						expiresAt),
				true);

		try (MockedStatic<Utilities> utilMock = Mockito.mockStatic(Utilities.class)) {
			utilMock.when(() -> Utilities.isEmpty((String) null)).thenReturn(true);
			when(sysInfo.getSimpleTokenByteSize()).thenReturn(16);
			when(tokenGenerator.generateSimpleToken(16)).thenReturn("aaaaaaaaaaaaaaaa", "0123456789abcdef");
			when(sysInfo.getSecretCryptographerKey()).thenReturn("aVerySecretKey");
			when(secretCryptographer.encrypt_HMAC_SHA256("aaaaaaaaaaaaaaaa", "aVerySecretKey")).thenReturn("existingToken");
			when(secretCryptographer.encrypt_HMAC_SHA256("0123456789abcdef", "aVerySecretKey")).thenReturn("hashedToken");
			when(tokenHeaderDbService.find("existingToken")).thenReturn(Optional.of(new TokenHeader()));
			when(tokenHeaderDbService.find("hashedToken")).thenReturn(Optional.empty());
			utilMock.when(() -> Utilities.utcNow()).thenReturn(ZonedDateTime.of(2025, 8, 6, 12, 0, 0, 0, ZoneId.of("UTC")));
			when(sysInfo.getTokenTimeLimit()).thenReturn(10);
			when(timeLimitedTokenDbService.save(
					AuthorizationTokenType.TIME_LIMITED_TOKEN,
					"hashedToken",
					"ConsumerName",
					"LOCAL",
					"ConsumerName",
					"ProviderName",
					AuthorizationTargetType.SERVICE_DEF,
					"testService",
					"op",
					expiresAt)).thenReturn(dbResult);

			final TokenModel result = engine.produce(
					"ConsumerName",
					"ConsumerName",
					ServiceInterfacePolicy.TIME_LIMITED_TOKEN_AUTH,
					"ProviderName",
					AuthorizationTargetType.SERVICE_DEF,
					"testService",
					"op",
					"testOrigin");

			utilMock.verify(() -> Utilities.isEmpty((String) null));
			verify(sysInfo, times(2)).getSimpleTokenByteSize();
			verify(tokenGenerator, times(2)).generateSimpleToken(16);
			verify(sysInfo, times(2)).getSecretCryptographerKey();
			verify(secretCryptographer).encrypt_HMAC_SHA256("aaaaaaaaaaaaaaaa", "aVerySecretKey");
			verify(secretCryptographer).encrypt_HMAC_SHA256("0123456789abcdef", "aVerySecretKey");
			verify(tokenHeaderDbService).find("existingToken");
			verify(tokenHeaderDbService).find("hashedToken");
			utilMock.verify(() -> Utilities.utcNow());
			verify(sysInfo).getTokenTimeLimit();
			verify(timeLimitedTokenDbService).save(
					AuthorizationTokenType.TIME_LIMITED_TOKEN,
					"hashedToken",
					"ConsumerName",
					"LOCAL",
					"ConsumerName",
					"ProviderName",
					AuthorizationTargetType.SERVICE_DEF,
					"testService",
					"op",
					expiresAt);

			assertFalse(result.isEncrypted());
			assertEquals(AuthorizationTokenType.TIME_LIMITED_TOKEN, result.getTokenType());
			assertEquals("0123456789abcdef", result.getRawToken());
			assertEquals("hashedToken", result.getHashedToken());
			assertEquals("ConsumerName", result.getRequester());
			assertEquals("LOCAL", result.getConsumerCloud());
			assertEquals("ConsumerName", result.getConsumer());
			assertEquals("ProviderName", result.getProvider());
			assertEquals(AuthorizationTargetType.SERVICE_DEF, result.getTargetType());
			assertEquals("testService", result.getTarget());
			assertEquals("op", result.getScope());
			assertEquals("TIME_LIMITED_TOKEN_AUTH", result.getVariant());
			assertEquals(expiresAt, result.getExpiresAt());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testProduce11UsageGeneralException() throws InvalidKeyException, NoSuchAlgorithmException {
		when(sysInfo.getSimpleTokenByteSize()).thenReturn(16);
		when(tokenGenerator.generateSimpleToken(16)).thenReturn("0123456789abcdef");
		when(sysInfo.getSecretCryptographerKey()).thenReturn("aVerySecretKey");
		when(secretCryptographer.encrypt_HMAC_SHA256("0123456789abcdef", "aVerySecretKey")).thenThrow(InvalidKeyException.class);

		final Throwable ex = assertThrows(InternalServerError.class,
				() -> engine.produce(
						"ConsumerName",
						"ConsumerName",
						"LOCAL",
						ServiceInterfacePolicy.USAGE_LIMITED_TOKEN_AUTH,
						"ProviderName",
						AuthorizationTargetType.SERVICE_DEF,
						"testService",
						"op",
						7,
						null,
						"testOrigin"));

		verify(sysInfo).getSimpleTokenByteSize();
		verify(tokenGenerator).generateSimpleToken(16);
		verify(sysInfo).getSecretCryptographerKey();
		verify(secretCryptographer).encrypt_HMAC_SHA256("0123456789abcdef", "aVerySecretKey");

		assertEquals("Token generation failed", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testProduce11UsageInternalServerError() throws InvalidKeyException, NoSuchAlgorithmException {
		when(sysInfo.getSimpleTokenByteSize()).thenReturn(16);
		when(tokenGenerator.generateSimpleToken(16)).thenReturn("0123456789abcdef");
		when(sysInfo.getSecretCryptographerKey()).thenReturn("aVerySecretKey");
		when(secretCryptographer.encrypt_HMAC_SHA256("0123456789abcdef", "aVerySecretKey")).thenReturn("hashedToken");
		when(tokenHeaderDbService.find("hashedToken")).thenReturn(Optional.empty());
		when(usageLimitedTokenDbService.save(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op",
				7)).thenThrow(new InternalServerError("test"));

		final ArrowheadException ex = assertThrows(InternalServerError.class,
				() -> engine.produce(
						"ConsumerName",
						"ConsumerName",
						"LOCAL",
						ServiceInterfacePolicy.USAGE_LIMITED_TOKEN_AUTH,
						"ProviderName",
						AuthorizationTargetType.SERVICE_DEF,
						"testService",
						"op",
						7,
						null,
						"testOrigin"));

		verify(sysInfo).getSimpleTokenByteSize();
		verify(tokenGenerator).generateSimpleToken(16);
		verify(sysInfo).getSecretCryptographerKey();
		verify(secretCryptographer).encrypt_HMAC_SHA256("0123456789abcdef", "aVerySecretKey");
		verify(tokenHeaderDbService).find("hashedToken");

		assertEquals("test", ex.getMessage());
		assertEquals("testOrigin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testProduce11UsageOk() throws InvalidKeyException, NoSuchAlgorithmException {
		final Pair<UsageLimitedToken, Boolean> dbResult = Pair.of(
				new UsageLimitedToken(
						new TokenHeader(
								AuthorizationTokenType.USAGE_LIMITED_TOKEN,
								"hashedToken",
								"ConsumerName",
								"LOCAL",
								"ConsumerName",
								"ProviderName",
								AuthorizationTargetType.SERVICE_DEF,
								"testService",
								"op"),
						7),
				true);

		when(sysInfo.getSimpleTokenByteSize()).thenReturn(16);
		when(tokenGenerator.generateSimpleToken(16)).thenReturn("0123456789abcdef");
		when(sysInfo.getSecretCryptographerKey()).thenReturn("aVerySecretKey");
		when(secretCryptographer.encrypt_HMAC_SHA256("0123456789abcdef", "aVerySecretKey")).thenReturn("hashedToken");
		when(tokenHeaderDbService.find("hashedToken")).thenReturn(Optional.empty());
		when(usageLimitedTokenDbService.save(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op",
				7)).thenReturn(dbResult);

		final TokenModel result = engine.produce(
				"ConsumerName",
				"ConsumerName",
				"LOCAL",
				ServiceInterfacePolicy.USAGE_LIMITED_TOKEN_AUTH,
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op",
				7,
				null,
				"testOrigin");

		verify(sysInfo).getSimpleTokenByteSize();
		verify(tokenGenerator).generateSimpleToken(16);
		verify(sysInfo).getSecretCryptographerKey();
		verify(secretCryptographer).encrypt_HMAC_SHA256("0123456789abcdef", "aVerySecretKey");
		verify(tokenHeaderDbService).find("hashedToken");
		verify(sysInfo, never()).getSimpleTokenUsageLimit();
		verify(usageLimitedTokenDbService).save(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op",
				7);

		assertFalse(result.isEncrypted());
		assertEquals(AuthorizationTokenType.USAGE_LIMITED_TOKEN, result.getTokenType());
		assertEquals("0123456789abcdef", result.getRawToken());
		assertEquals("hashedToken", result.getHashedToken());
		assertEquals("ConsumerName", result.getRequester());
		assertEquals("LOCAL", result.getConsumerCloud());
		assertEquals("ConsumerName", result.getConsumer());
		assertEquals("ProviderName", result.getProvider());
		assertEquals(AuthorizationTargetType.SERVICE_DEF, result.getTargetType());
		assertEquals("testService", result.getTarget());
		assertEquals("op", result.getScope());
		assertEquals("USAGE_LIMITED_TOKEN_AUTH", result.getVariant());
		assertEquals(7, result.getUsageLimit());
		assertEquals(7, result.getUsageLeft());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testProduce11TimeOk() throws InvalidKeyException, NoSuchAlgorithmException {
		final ZonedDateTime expiresAt = ZonedDateTime.of(2025, 12, 10, 12, 0, 0, 0, ZoneId.of("UTC"));
		final Pair<TimeLimitedToken, Boolean> dbResult = Pair.of(
				new TimeLimitedToken(
						new TokenHeader(
								AuthorizationTokenType.TIME_LIMITED_TOKEN,
								"hashedToken",
								"ConsumerName",
								"LOCAL",
								"ConsumerName",
								"ProviderName",
								AuthorizationTargetType.SERVICE_DEF,
								"testService",
								"op"),
						expiresAt),
				true);

		when(sysInfo.getSimpleTokenByteSize()).thenReturn(16);
		when(tokenGenerator.generateSimpleToken(16)).thenReturn("0123456789abcdef");
		when(sysInfo.getSecretCryptographerKey()).thenReturn("aVerySecretKey");
		when(secretCryptographer.encrypt_HMAC_SHA256("0123456789abcdef", "aVerySecretKey")).thenReturn("hashedToken");
		when(tokenHeaderDbService.find("hashedToken")).thenReturn(Optional.empty());
		when(timeLimitedTokenDbService.save(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op",
				expiresAt)).thenReturn(dbResult);

		final TokenModel result = engine.produce(
				"ConsumerName",
				"ConsumerName",
				"LOCAL",
				ServiceInterfacePolicy.TIME_LIMITED_TOKEN_AUTH,
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op",
				null,
				expiresAt,
				"testOrigin");

		verify(sysInfo).getSimpleTokenByteSize();
		verify(tokenGenerator).generateSimpleToken(16);
		verify(sysInfo).getSecretCryptographerKey();
		verify(secretCryptographer).encrypt_HMAC_SHA256("0123456789abcdef", "aVerySecretKey");
		verify(tokenHeaderDbService).find("hashedToken");
		verify(sysInfo, never()).getTokenTimeLimit();
		verify(timeLimitedTokenDbService).save(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op",
				expiresAt);

		assertFalse(result.isEncrypted());
		assertEquals(AuthorizationTokenType.TIME_LIMITED_TOKEN, result.getTokenType());
		assertEquals("0123456789abcdef", result.getRawToken());
		assertEquals("hashedToken", result.getHashedToken());
		assertEquals("ConsumerName", result.getRequester());
		assertEquals("LOCAL", result.getConsumerCloud());
		assertEquals("ConsumerName", result.getConsumer());
		assertEquals("ProviderName", result.getProvider());
		assertEquals(AuthorizationTargetType.SERVICE_DEF, result.getTargetType());
		assertEquals("testService", result.getTarget());
		assertEquals("op", result.getScope());
		assertEquals("TIME_LIMITED_TOKEN_AUTH", result.getVariant());
		assertEquals(expiresAt, result.getExpiresAt());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testProduce11Base64SelfContainedTokenRawTokenNull() throws InvalidKeyException, NoSuchAlgorithmException {
		when(sysInfo.getTokenTimeLimit()).thenReturn(300);
		when(tokenGenerator.generateBas64SelfContainedToken(any(ZonedDateTime.class), any(SelfContainedTokenPayload.class))).thenReturn(null);

		final Throwable ex = assertThrows(InternalServerError.class,
				() -> engine.produce(
						"ConsumerName",
						"ConsumerName",
						"LOCAL",
						ServiceInterfacePolicy.BASE64_SELF_CONTAINED_TOKEN_AUTH,
						"ProviderName",
						AuthorizationTargetType.SERVICE_DEF,
						"testService",
						"op",
						null,
						null,
						"testOrigin"));

		verify(sysInfo).getTokenTimeLimit();
		verify(tokenGenerator).generateBas64SelfContainedToken(any(ZonedDateTime.class), any(SelfContainedTokenPayload.class));

		assertEquals("Token generation failed", ex.getMessage());

	}
}