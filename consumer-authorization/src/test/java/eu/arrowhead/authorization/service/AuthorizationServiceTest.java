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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Pair;

import eu.arrowhead.authorization.jpa.entity.AuthPolicy;
import eu.arrowhead.authorization.jpa.entity.AuthProviderPolicyHeader;
import eu.arrowhead.authorization.jpa.service.AuthorizationPolicyDbService;
import eu.arrowhead.authorization.service.dto.DTOConverter;
import eu.arrowhead.authorization.service.dto.NormalizedGrantRequest;
import eu.arrowhead.authorization.service.engine.AuthorizationPolicyEngine;
import eu.arrowhead.authorization.service.validation.AuthorizationValidation;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.AuthorizationGrantRequestDTO;
import eu.arrowhead.dto.AuthorizationPolicyDTO;
import eu.arrowhead.dto.AuthorizationPolicyRequestDTO;
import eu.arrowhead.dto.AuthorizationPolicyResponseDTO;
import eu.arrowhead.dto.enums.AuthorizationLevel;
import eu.arrowhead.dto.enums.AuthorizationPolicyType;
import eu.arrowhead.dto.enums.AuthorizationTargetType;

@ExtendWith(MockitoExtension.class)
public class AuthorizationServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private AuthorizationService service;

	@Mock
	private AuthorizationValidation validator;

	@Mock
	private AuthorizationPolicyDbService dbService;

	@Mock
	private AuthorizationPolicyEngine policyEngine;

	@Mock
	private DTOConverter dtoConverter;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGrantOperationNullOrigin() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.grantOperation(null, null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGrantOperationEmptyOrigin() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.grantOperation(null, null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGrantOperationInvalidParameterException() {
		final AuthorizationGrantRequestDTO dto = new AuthorizationGrantRequestDTO(
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF.name(),
				"serviceDef",
				"description",
				new AuthorizationPolicyRequestDTO(AuthorizationPolicyType.ALL.name(), null, null),
				null);

		final NormalizedGrantRequest normalized = new NormalizedGrantRequest(AuthorizationLevel.PROVIDER);

		when(validator.validateAndNormalizeSystemName("TestProvider", "origin")).thenReturn("TestProvider");
		when(validator.validateAndNormalizeGrantRequest("TestProvider", dto, "origin")).thenReturn(normalized);
		when(dbService.createProviderLevelPolicy(normalized)).thenThrow(new InvalidParameterException("test"));

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> service.grantOperation("TestProvider", dto, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeSystemName("TestProvider", "origin");
		verify(validator).validateAndNormalizeGrantRequest("TestProvider", dto, "origin");
		verify(dbService).createProviderLevelPolicy(normalized);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGrantOperationInternalServerError() {
		final AuthorizationGrantRequestDTO dto = new AuthorizationGrantRequestDTO(
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF.name(),
				"serviceDef",
				"description",
				new AuthorizationPolicyRequestDTO(AuthorizationPolicyType.ALL.name(), null, null),
				null);

		final NormalizedGrantRequest normalized = new NormalizedGrantRequest(AuthorizationLevel.PROVIDER);

		when(validator.validateAndNormalizeSystemName("TestProvider", "origin")).thenReturn("TestProvider");
		when(validator.validateAndNormalizeGrantRequest("TestProvider", dto, "origin")).thenReturn(normalized);
		when(dbService.createProviderLevelPolicy(normalized)).thenThrow(new InternalServerError("test"));

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> service.grantOperation("TestProvider", dto, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeSystemName("TestProvider", "origin");
		verify(validator).validateAndNormalizeGrantRequest("TestProvider", dto, "origin");
		verify(dbService).createProviderLevelPolicy(normalized);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGrantOperationOk() {
		final AuthorizationGrantRequestDTO dto = new AuthorizationGrantRequestDTO(
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF.name(),
				"serviceDef",
				"description",
				new AuthorizationPolicyRequestDTO(AuthorizationPolicyType.ALL.name(), null, null),
				null);

		final NormalizedGrantRequest normalized = new NormalizedGrantRequest(AuthorizationLevel.PROVIDER);

		final Pair<Pair<AuthProviderPolicyHeader, List<AuthPolicy>>, Boolean> dbResult = Pair.of(
				Pair.of(new AuthProviderPolicyHeader(), List.of(new AuthPolicy())),
				true);

		final AuthorizationPolicyResponseDTO result = new AuthorizationPolicyResponseDTO(
				"PR|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationLevel.PROVIDER,
				"LOCAL",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"description",
				new AuthorizationPolicyDTO(AuthorizationPolicyType.ALL, null, null),
				null,
				"2025-10-16T16:00:01Z",
				"2025-10-16T16:00:01Z");

		when(validator.validateAndNormalizeSystemName("TestProvider", "origin")).thenReturn("TestProvider");
		when(validator.validateAndNormalizeGrantRequest("TestProvider", dto, "origin")).thenReturn(normalized);
		when(dbService.createProviderLevelPolicy(normalized)).thenReturn(dbResult);
		when(dtoConverter.convertPolicyToResponse(AuthorizationLevel.PROVIDER, dbResult.getFirst())).thenReturn(result);

		final Pair<AuthorizationPolicyResponseDTO, Boolean> response = service.grantOperation("TestProvider", dto, "origin");

		assertEquals(result, response.getFirst());
		assertTrue(response.getSecond());

		verify(validator).validateAndNormalizeSystemName("TestProvider", "origin");
		verify(validator).validateAndNormalizeGrantRequest("TestProvider", dto, "origin");
		verify(dbService).createProviderLevelPolicy(normalized);
		verify(dtoConverter).convertPolicyToResponse(AuthorizationLevel.PROVIDER, dbResult.getFirst());
	}
}