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
package eu.arrowhead.authorization.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.authorization.AuthorizationSystemInfo;
import eu.arrowhead.authorization.jpa.entity.TokenHeader;
import eu.arrowhead.authorization.jpa.service.EncryptionKeyDbService;
import eu.arrowhead.authorization.service.dto.DTOConverter;
import eu.arrowhead.authorization.service.dto.NormalizedVerifyRequest;
import eu.arrowhead.authorization.service.engine.AuthorizationPolicyEngine;
import eu.arrowhead.authorization.service.engine.TokenEngine;
import eu.arrowhead.authorization.service.model.TokenModel;
import eu.arrowhead.authorization.service.utils.SecretCryptographer;
import eu.arrowhead.authorization.service.validation.AuthorizationTokenValidation;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.dto.AuthorizationTokenGenerationRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationResponseDTO;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import eu.arrowhead.dto.enums.AuthorizationTokenType;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;

@ExtendWith(MockitoExtension.class)
public class AuthorizationTokenServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private AuthorizationTokenService service;

	@Mock
	private AuthorizationSystemInfo sysInfo;

	@Mock
	private AuthorizationPolicyEngine policyEngine;

	@Mock
	private TokenEngine tokenEngine;

	@Mock
	private SecretCryptographer secretCryptographer;

	@Mock
	private EncryptionKeyDbService encryptionKeyDbService;

	@Mock
	private AuthorizationTokenValidation validator;

	@Mock
	private DTOConverter dtoConverter;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateNullOrigin() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.generate(null, null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateEmptyOrigin() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.generate(null, null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateForbiddenException() {
		final AuthorizationTokenGenerationRequestDTO dto = new AuthorizationTokenGenerationRequestDTO(
				"USAGE_LIMITED_TOKEN_AUTH",
				"TestProvider",
				"SERVICE_DEF",
				"serviceDef",
				"operation");

		when(validator.validateAndNormalizeSystemName("TestConsumer", "origin")).thenReturn("TestConsumer");
		when(validator.validateAndNormalizeGenerateRequest(dto, "origin")).thenReturn(dto);
		when(policyEngine.isAccessGranted(any(NormalizedVerifyRequest.class))).thenReturn(false);

		final ArrowheadException ex = assertThrows(
				ForbiddenException.class,
				() -> service.generate("TestConsumer", dto, "origin"));

		assertEquals("Requester has no permisson to the service instance and/or operation", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeSystemName("TestConsumer", "origin");
		verify(validator).validateAndNormalizeGenerateRequest(dto, "origin");
		verify(policyEngine).isAccessGranted(any(NormalizedVerifyRequest.class));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateNoEncryption() {
		final AuthorizationTokenGenerationRequestDTO dto = new AuthorizationTokenGenerationRequestDTO(
				"USAGE_LIMITED_TOKEN_AUTH",
				"TestProvider",
				"SERVICE_DEF",
				"serviceDef",
				"operation");

		final TokenHeader usageLimitedToken = new TokenHeader(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"hashedToken",
				"TestConsumer",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		final TokenModel token = new TokenModel(usageLimitedToken);

		final AuthorizationTokenGenerationResponseDTO converted = new AuthorizationTokenGenerationResponseDTO(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				AuthorizationTargetType.SERVICE_DEF,
				"token",
				10,
				null);

		when(validator.validateAndNormalizeSystemName("TestConsumer", "origin")).thenReturn("TestConsumer");
		when(validator.validateAndNormalizeGenerateRequest(dto, "origin")).thenReturn(dto);
		when(policyEngine.isAccessGranted(any(NormalizedVerifyRequest.class))).thenReturn(true);
		when(tokenEngine.produce(
				"TestConsumer",
				"TestConsumer",
				ServiceInterfacePolicy.USAGE_LIMITED_TOKEN_AUTH,
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation",
				"origin")).thenReturn(token);
		when(dtoConverter.convertTokenModelToResponse(token)).thenReturn(converted);

		assertDoesNotThrow(() -> service.generate("TestConsumer", dto, "origin"));

		verify(validator).validateAndNormalizeSystemName("TestConsumer", "origin");
		verify(validator).validateAndNormalizeGenerateRequest(dto, "origin");
		verify(policyEngine).isAccessGranted(any(NormalizedVerifyRequest.class));
		verify(tokenEngine).produce(
				"TestConsumer",
				"TestConsumer",
				ServiceInterfacePolicy.USAGE_LIMITED_TOKEN_AUTH,
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation",
				"origin");
		verify(tokenEngine, never()).encryptTokenIfNeeded(token, "origin");
		verify(dtoConverter).convertTokenModelToResponse(token);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateEncryption() {
		final AuthorizationTokenGenerationRequestDTO dto = new AuthorizationTokenGenerationRequestDTO(
				"BASE64_SELF_CONTAINED_TOKEN_AUTH",
				"TestProvider",
				"SERVICE_DEF",
				"serviceDef",
				null);

		final TokenHeader tokenHeader = new TokenHeader(
				AuthorizationTokenType.SELF_CONTAINED_TOKEN,
				"hashedToken",
				"TestConsumer",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				null);
		final TokenModel token = new TokenModel(tokenHeader);

		final AuthorizationTokenGenerationResponseDTO converted = new AuthorizationTokenGenerationResponseDTO(
				AuthorizationTokenType.SELF_CONTAINED_TOKEN,
				AuthorizationTargetType.SERVICE_DEF,
				"token",
				null,
				null);

		when(validator.validateAndNormalizeSystemName("TestConsumer", "origin")).thenReturn("TestConsumer");
		when(validator.validateAndNormalizeGenerateRequest(dto, "origin")).thenReturn(dto);
		when(policyEngine.isAccessGranted(any(NormalizedVerifyRequest.class))).thenReturn(true);
		when(tokenEngine.produce(
				"TestConsumer",
				"TestConsumer",
				ServiceInterfacePolicy.BASE64_SELF_CONTAINED_TOKEN_AUTH,
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				null,
				"origin")).thenReturn(token);
		doNothing().when(tokenEngine).encryptTokenIfNeeded(token, "origin");
		when(dtoConverter.convertTokenModelToResponse(token)).thenReturn(converted);

		assertDoesNotThrow(() -> service.generate("TestConsumer", dto, "origin"));

		verify(validator).validateAndNormalizeSystemName("TestConsumer", "origin");
		verify(validator).validateAndNormalizeGenerateRequest(dto, "origin");
		verify(policyEngine).isAccessGranted(any(NormalizedVerifyRequest.class));
		verify(tokenEngine).produce(
				"TestConsumer",
				"TestConsumer",
				ServiceInterfacePolicy.BASE64_SELF_CONTAINED_TOKEN_AUTH,
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				null,
				"origin");
		verify(tokenEngine).encryptTokenIfNeeded(token, "origin");
		verify(dtoConverter).convertTokenModelToResponse(token);
	}
}