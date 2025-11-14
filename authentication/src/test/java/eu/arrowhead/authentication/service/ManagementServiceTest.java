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
package eu.arrowhead.authentication.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.authentication.jpa.entity.System;
import eu.arrowhead.authentication.jpa.service.IdentityDbService;
import eu.arrowhead.authentication.method.AuthenticationMethods;
import eu.arrowhead.authentication.method.IAuthenticationMethod;
import eu.arrowhead.authentication.service.dto.DTOConverter;
import eu.arrowhead.authentication.service.dto.NormalizedIdentityListMgmtRequestDTO;
import eu.arrowhead.authentication.service.dto.NormalizedIdentityMgmtRequestDTO;
import eu.arrowhead.authentication.service.validation.ManagementValidation;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.IdentityListMgmtCreateRequestDTO;
import eu.arrowhead.dto.IdentityListMgmtResponseDTO;
import eu.arrowhead.dto.IdentityListMgmtUpdateRequestDTO;
import eu.arrowhead.dto.IdentityMgmtRequestDTO;
import eu.arrowhead.dto.IdentityMgmtResponseDTO;
import eu.arrowhead.dto.enums.AuthenticationMethod;

@ExtendWith(MockitoExtension.class)
public class ManagementServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private ManagementService service;

	@Mock
	private ManagementValidation validator;

	@Mock
	private IdentityDbService dbService;

	@Mock
	private DTOConverter converter;

	@Mock
	private AuthenticationMethods methods;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentitiesOperationOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.createIdentitiesOperation(null, null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentitiesOperationOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.createIdentitiesOperation(null, null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentitiesOperationInvalidParameterException() {
		final IdentityMgmtRequestDTO dto = new IdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final IdentityListMgmtCreateRequestDTO list = new IdentityListMgmtCreateRequestDTO("PASSWORD", List.of(dto));
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final NormalizedIdentityMgmtRequestDTO normalized = new NormalizedIdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final NormalizedIdentityListMgmtRequestDTO normalizedList = new NormalizedIdentityListMgmtRequestDTO(methodMock, List.of(normalized));

		when(validator.validateAndNormalizeRequester("AdminSystem", "origin")).thenReturn("AdminSystem");
		when(validator.validateAndNormalizeCreateIdentityList(list, "origin")).thenReturn(normalizedList);
		when(dbService.createIdentifiableSystemsInBulk("AdminSystem", normalizedList)).thenThrow(new InvalidParameterException("test"));

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> service.createIdentitiesOperation("AdminSystem", list, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeRequester("AdminSystem", "origin");
		verify(validator).validateAndNormalizeCreateIdentityList(list, "origin");
		verify(dbService).createIdentifiableSystemsInBulk("AdminSystem", normalizedList);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentitiesOperationInternalServerError() {
		final IdentityMgmtRequestDTO dto = new IdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final IdentityListMgmtCreateRequestDTO list = new IdentityListMgmtCreateRequestDTO("PASSWORD", List.of(dto));
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final NormalizedIdentityMgmtRequestDTO normalized = new NormalizedIdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final NormalizedIdentityListMgmtRequestDTO normalizedList = new NormalizedIdentityListMgmtRequestDTO(methodMock, List.of(normalized));

		when(validator.validateAndNormalizeRequester("AdminSystem", "origin")).thenReturn("AdminSystem");
		when(validator.validateAndNormalizeCreateIdentityList(list, "origin")).thenReturn(normalizedList);
		when(dbService.createIdentifiableSystemsInBulk("AdminSystem", normalizedList)).thenThrow(new InternalServerError("test"));

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> service.createIdentitiesOperation("AdminSystem", list, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeRequester("AdminSystem", "origin");
		verify(validator).validateAndNormalizeCreateIdentityList(list, "origin");
		verify(dbService).createIdentifiableSystemsInBulk("AdminSystem", normalizedList);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentitiesOperationExternalServerError() {
		final IdentityMgmtRequestDTO dto = new IdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final IdentityListMgmtCreateRequestDTO list = new IdentityListMgmtCreateRequestDTO("PASSWORD", List.of(dto));
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final NormalizedIdentityMgmtRequestDTO normalized = new NormalizedIdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final NormalizedIdentityListMgmtRequestDTO normalizedList = new NormalizedIdentityListMgmtRequestDTO(methodMock, List.of(normalized));

		when(validator.validateAndNormalizeRequester("AdminSystem", "origin")).thenReturn("AdminSystem");
		when(validator.validateAndNormalizeCreateIdentityList(list, "origin")).thenReturn(normalizedList);
		when(dbService.createIdentifiableSystemsInBulk("AdminSystem", normalizedList)).thenThrow(new ExternalServerError("test"));

		final ArrowheadException ex = assertThrows(
				ExternalServerError.class,
				() -> service.createIdentitiesOperation("AdminSystem", list, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeRequester("AdminSystem", "origin");
		verify(validator).validateAndNormalizeCreateIdentityList(list, "origin");
		verify(dbService).createIdentifiableSystemsInBulk("AdminSystem", normalizedList);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentitiesOperationOk() {
		final IdentityMgmtRequestDTO dto = new IdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final IdentityListMgmtCreateRequestDTO list = new IdentityListMgmtCreateRequestDTO("PASSWORD", List.of(dto));
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final NormalizedIdentityMgmtRequestDTO normalized = new NormalizedIdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final NormalizedIdentityListMgmtRequestDTO normalizedList = new NormalizedIdentityListMgmtRequestDTO(methodMock, List.of(normalized));
		final System system = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		system.setId(1);
		final ZonedDateTime time = ZonedDateTime.of(2025, 11, 14, 15, 7, 12, 0, ZoneId.of(Constants.UTC));
		system.setCreatedAt(time);
		system.setUpdatedAt(time);
		final IdentityMgmtResponseDTO responseDto = new IdentityMgmtResponseDTO("TestSystem", "PASSWORD", false, "AdminSystem", "2025-11-14T15:07:12Z", "AdminSystem", "2025-11-14T15:07:12Z");
		final IdentityListMgmtResponseDTO response = new IdentityListMgmtResponseDTO(List.of(responseDto), 1);

		when(validator.validateAndNormalizeRequester("AdminSystem", "origin")).thenReturn("AdminSystem");
		when(validator.validateAndNormalizeCreateIdentityList(list, "origin")).thenReturn(normalizedList);
		when(dbService.createIdentifiableSystemsInBulk("AdminSystem", normalizedList)).thenReturn(List.of(system));
		when(converter.convertIdentifiableSystemListToDTO(List.of(system))).thenReturn(response);

		assertDoesNotThrow(() -> service.createIdentitiesOperation("AdminSystem", list, "origin"));

		verify(validator).validateAndNormalizeRequester("AdminSystem", "origin");
		verify(validator).validateAndNormalizeCreateIdentityList(list, "origin");
		verify(dbService).createIdentifiableSystemsInBulk("AdminSystem", normalizedList);
		verify(converter).convertIdentifiableSystemListToDTO(List.of(system));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateIdentitiesOperationOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.updateIdentitiesOperation(null, null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateIdentitiesOperationOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.updateIdentitiesOperation(null, null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateIdentitiesOperationInternalServerError1() {
		final IdentityMgmtRequestDTO dto = new IdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final IdentityListMgmtUpdateRequestDTO list = new IdentityListMgmtUpdateRequestDTO(List.of(dto));
		final NormalizedIdentityMgmtRequestDTO normalized = new NormalizedIdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);

		when(validator.validateAndNormalizeRequester("AdminSystem", "origin")).thenReturn("AdminSystem");
		when(validator.validateAndNormalizeUpdateIdentityListPhase1(list, "origin")).thenReturn(List.of(normalized));
		when(dbService.getSystemsByNames(List.of("TestSystem"), true)).thenThrow(new InternalServerError("test"));

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> service.updateIdentitiesOperation("AdminSystem", list, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeRequester("AdminSystem", "origin");
		verify(validator).validateAndNormalizeUpdateIdentityListPhase1(list, "origin");
		verify(dbService).getSystemsByNames(List.of("TestSystem"), true);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateIdentitiesOperationDifferentAuthenticationMethods() {
		final IdentityMgmtRequestDTO dto = new IdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final IdentityListMgmtUpdateRequestDTO list = new IdentityListMgmtUpdateRequestDTO(List.of(dto));
		final NormalizedIdentityMgmtRequestDTO normalized = new NormalizedIdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final System system = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final System system2 = new System("TestSystem2", null, false, "AdminSystem");

		when(validator.validateAndNormalizeRequester("AdminSystem", "origin")).thenReturn("AdminSystem");
		when(validator.validateAndNormalizeUpdateIdentityListPhase1(list, "origin")).thenReturn(List.of(normalized));
		when(dbService.getSystemsByNames(List.of("TestSystem"), true)).thenReturn(List.of(system, system2));

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> service.updateIdentitiesOperation("AdminSystem", list, "origin"));

		assertEquals("Bulk updating systems with different authentication method is not supported", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeRequester("AdminSystem", "origin");
		verify(validator).validateAndNormalizeUpdateIdentityListPhase1(list, "origin");
		verify(dbService).getSystemsByNames(List.of("TestSystem"), true);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateIdentitiesOperationAuthenticationMethodUnsupported() {
		final IdentityMgmtRequestDTO dto = new IdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final IdentityListMgmtUpdateRequestDTO list = new IdentityListMgmtUpdateRequestDTO(List.of(dto));
		final NormalizedIdentityMgmtRequestDTO normalized = new NormalizedIdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final System system = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");

		when(validator.validateAndNormalizeRequester("AdminSystem", "origin")).thenReturn("AdminSystem");
		when(validator.validateAndNormalizeUpdateIdentityListPhase1(list, "origin")).thenReturn(List.of(normalized));
		when(dbService.getSystemsByNames(List.of("TestSystem"), true)).thenReturn(List.of(system));
		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(null);

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> service.updateIdentitiesOperation("AdminSystem", list, "origin"));

		assertEquals("Authentication method is unsupported: PASSWORD", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeRequester("AdminSystem", "origin");
		verify(validator).validateAndNormalizeUpdateIdentityListPhase1(list, "origin");
		verify(dbService).getSystemsByNames(List.of("TestSystem"), true);
		verify(methods).method(AuthenticationMethod.PASSWORD);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateIdentitiesOperationInvalidParameterException() {
		final IdentityMgmtRequestDTO dto = new IdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final IdentityListMgmtUpdateRequestDTO list = new IdentityListMgmtUpdateRequestDTO(List.of(dto));
		final NormalizedIdentityMgmtRequestDTO normalized = new NormalizedIdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final System system = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);

		when(validator.validateAndNormalizeRequester("AdminSystem", "origin")).thenReturn("AdminSystem");
		when(validator.validateAndNormalizeUpdateIdentityListPhase1(list, "origin")).thenReturn(List.of(normalized));
		when(dbService.getSystemsByNames(List.of("TestSystem"), true)).thenReturn(List.of(system));
		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);
		when(validator.validateAndNormalizeUpdateIdentityListPhase2(methodMock, List.of(normalized), "origin")).thenReturn(List.of(normalized));
		when(dbService.updateIdentifiableSystemsInBulk(methodMock, "AdminSystem", List.of(normalized))).thenThrow(new InvalidParameterException("test"));

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> service.updateIdentitiesOperation("AdminSystem", list, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeRequester("AdminSystem", "origin");
		verify(validator).validateAndNormalizeUpdateIdentityListPhase1(list, "origin");
		verify(dbService).getSystemsByNames(List.of("TestSystem"), true);
		verify(methods).method(AuthenticationMethod.PASSWORD);
		verify(validator).validateAndNormalizeUpdateIdentityListPhase2(methodMock, List.of(normalized), "origin");
		verify(dbService).updateIdentifiableSystemsInBulk(methodMock, "AdminSystem", List.of(normalized));
	}
	
	// TODO: continue
}