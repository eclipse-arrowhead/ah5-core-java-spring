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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

import org.jose4j.lang.JoseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.util.Pair;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.authorization.AuthorizationSystemInfo;
import eu.arrowhead.authorization.jpa.entity.SelfContainedToken;
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
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.security.SecurityUtilities;
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
	public void testProduce11UnhandledTokenType() throws InvalidKeyException, NoSuchAlgorithmException {
		when(sysInfo.getTokenTimeLimit()).thenReturn(300);

		final Throwable ex = assertThrows(InternalServerError.class,
				() -> engine.produce(
						"ConsumerName",
						"ConsumerName",
						"LOCAL",
						ServiceInterfacePolicy.CERT_AUTH,
						"ProviderName",
						AuthorizationTargetType.SERVICE_DEF,
						"testService",
						"op",
						null,
						null,
						"testOrigin"));

		verify(sysInfo).getTokenTimeLimit();

		assertEquals("Token generation failed", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testProduce11Base64SelfContainedTokenOk() throws InvalidKeyException, NoSuchAlgorithmException {
		final ZonedDateTime expiresAt = ZonedDateTime.of(2025, 12, 10, 12, 0, 0, 0, ZoneId.of("UTC"));
		final Pair<SelfContainedToken, Boolean> dbToken = Pair.of(
				new SelfContainedToken(
						new TokenHeader(
								AuthorizationTokenType.SELF_CONTAINED_TOKEN,
								"hashedToken",
								"ConsumerName",
								"LOCAL",
								"ConsumerName",
								"ProviderName",
								AuthorizationTargetType.SERVICE_DEF,
								"testService",
								"op"),
						"BASE64_SELF_CONTAINED_TOKEN_AUTH",
						expiresAt),
				true);

		when(tokenGenerator.generateBas64SelfContainedToken(eq(expiresAt), any(SelfContainedTokenPayload.class))).thenReturn("rawToken");
		when(sysInfo.getSecretCryptographerKey()).thenReturn("aVerySecretKey");
		when(secretCryptographer.encrypt_HMAC_SHA256("rawToken", "aVerySecretKey")).thenReturn("hashedToken");
		when(selfContainedTokenDbService.save(
				AuthorizationTokenType.SELF_CONTAINED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op",
				"BASE64_SELF_CONTAINED_TOKEN_AUTH",
				expiresAt)).thenReturn(dbToken);

		final TokenModel result = engine.produce(
				"ConsumerName",
				"ConsumerName",
				"LOCAL",
				ServiceInterfacePolicy.BASE64_SELF_CONTAINED_TOKEN_AUTH,
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op",
				null,
				expiresAt,
				"testOrigin");

		verify(sysInfo, never()).getTokenTimeLimit();
		verify(tokenGenerator).generateBas64SelfContainedToken(eq(expiresAt), any(SelfContainedTokenPayload.class));
		verify(sysInfo).getSecretCryptographerKey();
		verify(secretCryptographer).encrypt_HMAC_SHA256("rawToken", "aVerySecretKey");
		verify(selfContainedTokenDbService).save(
				AuthorizationTokenType.SELF_CONTAINED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op",
				"BASE64_SELF_CONTAINED_TOKEN_AUTH",
				expiresAt);

		assertFalse(result.isEncrypted());
		assertEquals(AuthorizationTokenType.SELF_CONTAINED_TOKEN, result.getTokenType());
		assertEquals("rawToken", result.getRawToken());
		assertEquals("hashedToken", result.getHashedToken());
		assertEquals("ConsumerName", result.getRequester());
		assertEquals("LOCAL", result.getConsumerCloud());
		assertEquals("ConsumerName", result.getConsumer());
		assertEquals("ProviderName", result.getProvider());
		assertEquals(AuthorizationTargetType.SERVICE_DEF, result.getTargetType());
		assertEquals("testService", result.getTarget());
		assertEquals("op", result.getScope());
		assertEquals("BASE64_SELF_CONTAINED_TOKEN_AUTH", result.getVariant());
		assertEquals(expiresAt, result.getExpiresAt());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testProduce11JWTNoSSL() throws InvalidKeyException, NoSuchAlgorithmException {
		ReflectionTestUtils.setField(engine, "arrowheadContext", Map.of());

		when(sysInfo.getTokenTimeLimit()).thenReturn(300);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> engine.produce(
						"ConsumerName",
						"ConsumerName",
						"LOCAL",
						ServiceInterfacePolicy.RSA_SHA256_JSON_WEB_TOKEN_AUTH,
						"ProviderName",
						AuthorizationTargetType.SERVICE_DEF,
						"testService",
						"op",
						null,
						null,
						"testOrigin"));

		verify(sysInfo).getTokenTimeLimit();

		assertEquals("JWT is supported only when SSL is enabled", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testProduce11JWTRSA512Ok() throws InvalidKeyException, NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException, JoseException {
		final Resource keyStoreResource = new ClassPathResource("certificate/ConsumerAuthorization.p12");
		final KeyStore keyStore = KeyStore.getInstance("pkcs12");
		keyStore.load(keyStoreResource.getInputStream(), "123456".toCharArray());
		final PrivateKey privateKey = SecurityUtilities.getPrivateKeyFromKeyStore(keyStore, "ConsumerAuthorization.TestCloud.Company.arrowhead.eu", "123456");
		final Map<String, Object> context = Map.of("server.private.key", privateKey);
		ReflectionTestUtils.setField(engine, "arrowheadContext", context);

		final ZonedDateTime expiresAt = ZonedDateTime.of(2025, 12, 10, 12, 0, 0, 0, ZoneId.of("UTC"));
		final Pair<SelfContainedToken, Boolean> dbToken = Pair.of(
				new SelfContainedToken(
						new TokenHeader(
								AuthorizationTokenType.SELF_CONTAINED_TOKEN,
								"hashedToken",
								"ConsumerName",
								"LOCAL",
								"ConsumerName",
								"ProviderName",
								AuthorizationTargetType.SERVICE_DEF,
								"testService",
								"op"),
						"RSA_SHA512_JSON_WEB_TOKEN_AUTH",
						expiresAt),
				true);

		when(tokenGenerator.generateJsonWebToken(eq("RS512"), eq(privateKey), eq(expiresAt), any(SelfContainedTokenPayload.class))).thenReturn("rawToken");
		when(sysInfo.getSecretCryptographerKey()).thenReturn("aVerySecretKey");
		when(secretCryptographer.encrypt_HMAC_SHA256("rawToken", "aVerySecretKey")).thenReturn("hashedToken");
		when(selfContainedTokenDbService.save(
				AuthorizationTokenType.SELF_CONTAINED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op",
				"RSA_SHA512_JSON_WEB_TOKEN_AUTH",
				expiresAt)).thenReturn(dbToken);

		final TokenModel result = engine.produce(
				"ConsumerName",
				"ConsumerName",
				"LOCAL",
				ServiceInterfacePolicy.RSA_SHA512_JSON_WEB_TOKEN_AUTH,
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op",
				null,
				expiresAt,
				"testOrigin");

		verify(sysInfo, never()).getTokenTimeLimit();
		verify(tokenGenerator).generateJsonWebToken(eq("RS512"), eq(privateKey), eq(expiresAt), any(SelfContainedTokenPayload.class));
		verify(sysInfo).getSecretCryptographerKey();
		verify(secretCryptographer).encrypt_HMAC_SHA256("rawToken", "aVerySecretKey");
		verify(selfContainedTokenDbService).save(
				AuthorizationTokenType.SELF_CONTAINED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op",
				"RSA_SHA512_JSON_WEB_TOKEN_AUTH",
				expiresAt);

		assertFalse(result.isEncrypted());
		assertEquals(AuthorizationTokenType.SELF_CONTAINED_TOKEN, result.getTokenType());
		assertEquals("rawToken", result.getRawToken());
		assertEquals("hashedToken", result.getHashedToken());
		assertEquals("ConsumerName", result.getRequester());
		assertEquals("LOCAL", result.getConsumerCloud());
		assertEquals("ConsumerName", result.getConsumer());
		assertEquals("ProviderName", result.getProvider());
		assertEquals(AuthorizationTargetType.SERVICE_DEF, result.getTargetType());
		assertEquals("testService", result.getTarget());
		assertEquals("op", result.getScope());
		assertEquals("RSA_SHA512_JSON_WEB_TOKEN_AUTH", result.getVariant());
		assertEquals(expiresAt, result.getExpiresAt());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testProduce11JWTRSA256Ok() throws InvalidKeyException, NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException, JoseException {
		final Resource keyStoreResource = new ClassPathResource("certificate/ConsumerAuthorization.p12");
		final KeyStore keyStore = KeyStore.getInstance("pkcs12");
		keyStore.load(keyStoreResource.getInputStream(), "123456".toCharArray());
		final PrivateKey privateKey = SecurityUtilities.getPrivateKeyFromKeyStore(keyStore, "ConsumerAuthorization.TestCloud.Company.arrowhead.eu", "123456");
		final Map<String, Object> context = Map.of("server.private.key", privateKey);
		ReflectionTestUtils.setField(engine, "arrowheadContext", context);

		final ZonedDateTime expiresAt = ZonedDateTime.of(2025, 12, 10, 12, 0, 0, 0, ZoneId.of("UTC"));
		final Pair<SelfContainedToken, Boolean> dbToken = Pair.of(
				new SelfContainedToken(
						new TokenHeader(
								AuthorizationTokenType.SELF_CONTAINED_TOKEN,
								"hashedToken",
								"ConsumerName",
								"LOCAL",
								"ConsumerName",
								"ProviderName",
								AuthorizationTargetType.SERVICE_DEF,
								"testService",
								"op"),
						"RSA_SHA256_JSON_WEB_TOKEN_AUTH",
						expiresAt),
				true);

		when(tokenGenerator.generateJsonWebToken(eq("RS256"), eq(privateKey), eq(expiresAt), any(SelfContainedTokenPayload.class))).thenReturn("rawToken");
		when(sysInfo.getSecretCryptographerKey()).thenReturn("aVerySecretKey");
		when(secretCryptographer.encrypt_HMAC_SHA256("rawToken", "aVerySecretKey")).thenReturn("hashedToken");
		when(selfContainedTokenDbService.save(
				AuthorizationTokenType.SELF_CONTAINED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op",
				"RSA_SHA256_JSON_WEB_TOKEN_AUTH",
				expiresAt)).thenReturn(dbToken);

		final TokenModel result = engine.produce(
				"ConsumerName",
				"ConsumerName",
				"LOCAL",
				ServiceInterfacePolicy.RSA_SHA256_JSON_WEB_TOKEN_AUTH,
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op",
				null,
				expiresAt,
				"testOrigin");

		verify(sysInfo, never()).getTokenTimeLimit();
		verify(tokenGenerator).generateJsonWebToken(eq("RS256"), eq(privateKey), eq(expiresAt), any(SelfContainedTokenPayload.class));
		verify(sysInfo).getSecretCryptographerKey();
		verify(secretCryptographer).encrypt_HMAC_SHA256("rawToken", "aVerySecretKey");
		verify(selfContainedTokenDbService).save(
				AuthorizationTokenType.SELF_CONTAINED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op",
				"RSA_SHA256_JSON_WEB_TOKEN_AUTH",
				expiresAt);

		assertFalse(result.isEncrypted());
		assertEquals(AuthorizationTokenType.SELF_CONTAINED_TOKEN, result.getTokenType());
		assertEquals("rawToken", result.getRawToken());
		assertEquals("hashedToken", result.getHashedToken());
		assertEquals("ConsumerName", result.getRequester());
		assertEquals("LOCAL", result.getConsumerCloud());
		assertEquals("ConsumerName", result.getConsumer());
		assertEquals("ProviderName", result.getProvider());
		assertEquals(AuthorizationTargetType.SERVICE_DEF, result.getTargetType());
		assertEquals("testService", result.getTarget());
		assertEquals("op", result.getScope());
		assertEquals("RSA_SHA256_JSON_WEB_TOKEN_AUTH", result.getVariant());
		assertEquals(expiresAt, result.getExpiresAt());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testVerifyHashCalculationProblem() throws InvalidKeyException, NoSuchAlgorithmException {
		when(sysInfo.getSecretCryptographerKey()).thenReturn("aKey");
		when(secretCryptographer.encrypt_HMAC_SHA256("rawToken", "aKey")).thenThrow(InvalidKeyException.class);

		final Throwable ex = assertThrows(InternalServerError.class,
				() -> engine.verify("ConsumerName", "rawToken", "testOrigin"));

		verify(sysInfo).getSecretCryptographerKey();
		verify(secretCryptographer).encrypt_HMAC_SHA256("rawToken", "aKey");

		assertEquals("Token verification failed", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testVerifyDbProblem() throws InvalidKeyException, NoSuchAlgorithmException {
		when(sysInfo.getSecretCryptographerKey()).thenReturn("aKey");
		when(secretCryptographer.encrypt_HMAC_SHA256("rawToken", "aKey")).thenReturn("hashedToken");
		when(tokenHeaderDbService.find("ConsumerName", "hashedToken")).thenThrow(new InternalServerError("test"));

		final ArrowheadException ex = assertThrows(InternalServerError.class,
				() -> engine.verify("ConsumerName", "rawToken", "testOrigin"));

		verify(sysInfo).getSecretCryptographerKey();
		verify(secretCryptographer).encrypt_HMAC_SHA256("rawToken", "aKey");
		verify(tokenHeaderDbService).find("ConsumerName", "hashedToken");

		assertEquals("test", ex.getMessage());
		assertEquals("testOrigin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testVerifyHashedTokenNotFound() throws InvalidKeyException, NoSuchAlgorithmException {
		when(sysInfo.getSecretCryptographerKey()).thenReturn("aKey");
		when(secretCryptographer.encrypt_HMAC_SHA256("rawToken", "aKey")).thenReturn("hashedToken");
		when(tokenHeaderDbService.find("ConsumerName", "hashedToken")).thenReturn(Optional.empty());

		final Pair<Boolean, Optional<TokenModel>> result = engine.verify("ConsumerName", "rawToken", "testOrigin");

		verify(sysInfo).getSecretCryptographerKey();
		verify(secretCryptographer).encrypt_HMAC_SHA256("rawToken", "aKey");
		verify(tokenHeaderDbService).find("ConsumerName", "hashedToken");

		assertFalse(result.getFirst());
		assertTrue(result.getSecond().isEmpty());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testVerifySelfContainedToken() throws InvalidKeyException, NoSuchAlgorithmException {
		final TokenHeader tokenHeader = new TokenHeader(
				AuthorizationTokenType.SELF_CONTAINED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op");

		when(sysInfo.getSecretCryptographerKey()).thenReturn("aKey");
		when(secretCryptographer.encrypt_HMAC_SHA256("rawToken", "aKey")).thenReturn("hashedToken");
		when(tokenHeaderDbService.find("ConsumerName", "hashedToken")).thenReturn(Optional.of(tokenHeader));

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> engine.verify("ConsumerName", "rawToken", "testOrigin"));

		verify(sysInfo).getSecretCryptographerKey();
		verify(secretCryptographer).encrypt_HMAC_SHA256("rawToken", "aKey");
		verify(tokenHeaderDbService).find("ConsumerName", "hashedToken");

		assertEquals("Self contained tokens can't be verified this way", ex.getMessage());
	}
}