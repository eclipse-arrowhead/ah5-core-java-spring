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
package eu.arrowhead.authentication.service.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.authentication.method.AuthenticationMethods;
import eu.arrowhead.authentication.method.IAuthenticationMethod;
import eu.arrowhead.authentication.service.normalization.ManagementNormalization;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.PageValidator;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.dto.IdentityListMgmtCreateRequestDTO;
import eu.arrowhead.dto.IdentityMgmtRequestDTO;
import eu.arrowhead.dto.enums.AuthenticationMethod;

@ExtendWith(MockitoExtension.class)
public class ManagementValidationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private ManagementValidation validator;

	@Mock
	private PageValidator pageValidator;

	@Mock
	private SystemNameValidator systemNameValidator;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	@Mock
	private ManagementNormalization normalizer;

	@Mock
	private AuthenticationMethods methods;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRequesterOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeRequester(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRequesterOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeRequester(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRequesterNullRequester() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeRequester(null, "origin"));

		assertEquals("Requester name is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRequesterEmptyRequester() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeRequester("", "origin"));

		assertEquals("Requester name is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRequesterInvalidParameterException() {
		when(systemNameNormalizer.normalize("1Requester")).thenReturn("1Requester");
		doThrow(new InvalidParameterException("test")).when(systemNameValidator).validateSystemName("1Requester");

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeRequester("1Requester", "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(systemNameNormalizer).normalize("1Requester");
		verify(systemNameValidator).validateSystemName("1Requester");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRequesterOk() {
		when(systemNameNormalizer.normalize("Requester")).thenReturn("Requester");
		doNothing().when(systemNameValidator).validateSystemName("Requester");

		final String result = validator.validateAndNormalizeRequester("Requester", "origin");

		assertEquals("Requester", result);

		verify(systemNameNormalizer).normalize("Requester");
		verify(systemNameValidator).validateSystemName("Requester");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateIdentityListOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeCreateIdentityList(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateIdentityListOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> validator.validateAndNormalizeCreateIdentityList(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateIdentityListDTONull() {
		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeCreateIdentityList(null, "origin"));

		assertEquals("Request payload is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateIdentityListAuthenticationMethodNull() {
		final IdentityListMgmtCreateRequestDTO dto = new IdentityListMgmtCreateRequestDTO(null, null);

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeCreateIdentityList(dto, "origin"));

		assertEquals("Authentication method is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateIdentityListAuthenticationMethodEmpty() {
		final IdentityListMgmtCreateRequestDTO dto = new IdentityListMgmtCreateRequestDTO(" ", null);

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeCreateIdentityList(dto, "origin"));

		assertEquals("Authentication method is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateIdentityListAuthenticationMethodInvalid() {
		final IdentityListMgmtCreateRequestDTO dto = new IdentityListMgmtCreateRequestDTO("Invalid", null);

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeCreateIdentityList(dto, "origin"));

		assertEquals("Authentication method is invalid: Invalid", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateIdentityListAuthenticationMethodUnsupported() {
		final IdentityListMgmtCreateRequestDTO dto = new IdentityListMgmtCreateRequestDTO("password", null);

		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(null);

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeCreateIdentityList(dto, "origin"));

		assertEquals("Authentication method is unsupported: password", ex.getMessage());

		verify(methods).method(AuthenticationMethod.PASSWORD);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateIdentityListIdentityListNull() {
		final IdentityListMgmtCreateRequestDTO dto = new IdentityListMgmtCreateRequestDTO("password", null);

		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);

		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeCreateIdentityList(dto, "origin"));

		assertEquals("Identity list is missing or empty", ex.getMessage());

		verify(methods).method(AuthenticationMethod.PASSWORD);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateIdentityListIdentityListEmpty() {
		final IdentityListMgmtCreateRequestDTO dto = new IdentityListMgmtCreateRequestDTO("password", List.of());

		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);

		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeCreateIdentityList(dto, "origin"));

		assertEquals("Identity list is missing or empty", ex.getMessage());

		verify(methods).method(AuthenticationMethod.PASSWORD);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateIdentityListIdentityListConstainsNullElement() {
		final List<IdentityMgmtRequestDTO> list = new ArrayList<>(1);
		list.add(null);

		final IdentityListMgmtCreateRequestDTO dto = new IdentityListMgmtCreateRequestDTO("password", list);

		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);

		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeCreateIdentityList(dto, "origin"));

		assertEquals("Identity list contains null element", ex.getMessage());

		verify(methods).method(AuthenticationMethod.PASSWORD);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateIdentityListSystemNameNull() {
		final IdentityMgmtRequestDTO identity = new IdentityMgmtRequestDTO(null, null, null);

		final IdentityListMgmtCreateRequestDTO dto = new IdentityListMgmtCreateRequestDTO("password", List.of(identity));

		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);

		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeCreateIdentityList(dto, "origin"));

		assertEquals("System name is missing or empty", ex.getMessage());

		verify(methods).method(AuthenticationMethod.PASSWORD);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateIdentityListSystemNameEmpty() {
		final IdentityMgmtRequestDTO identity = new IdentityMgmtRequestDTO(" ", null, null);

		final IdentityListMgmtCreateRequestDTO dto = new IdentityListMgmtCreateRequestDTO("password", List.of(identity));

		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);

		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(methodMock);

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeCreateIdentityList(dto, "origin"));

		assertEquals("System name is missing or empty", ex.getMessage());

		verify(methods).method(AuthenticationMethod.PASSWORD);
	}

	// TODO: continue
}