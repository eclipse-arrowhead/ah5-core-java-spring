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
package eu.arrowhead.authentication.jpa.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.authentication.jpa.entity.PasswordAuthentication;
import eu.arrowhead.authentication.jpa.entity.System;
import eu.arrowhead.authentication.jpa.repository.ActiveSessionRepository;
import eu.arrowhead.authentication.jpa.repository.PasswordAuthenticationRepository;
import eu.arrowhead.authentication.jpa.repository.SystemRepository;
import eu.arrowhead.authentication.method.IAuthenticationMethod;
import eu.arrowhead.authentication.service.dto.NormalizedIdentityListMgmtRequestDTO;
import eu.arrowhead.authentication.service.dto.NormalizedIdentityMgmtRequestDTO;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.enums.AuthenticationMethod;

@ExtendWith(MockitoExtension.class)
public class IdentityDbServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private IdentityDbService dbService;

	@Mock
	private SystemRepository systemRepository;

	@Mock
	private PasswordAuthenticationRepository paRepository;

	@Mock
	private ActiveSessionRepository asRepository;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetSystemByNameNullInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.getSystemByName(null));

		assertEquals("systemName is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetSystemByNameEmptyInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.getSystemByName(""));

		assertEquals("systemName is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetSystemByNameInternalServerError() {
		when(systemRepository.findByName("TestSystem")).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.getSystemByName("TestSystem"));

		assertEquals("Database operation error", ex.getMessage());

		verify(systemRepository).findByName("TestSystem");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetSystemByNameOk() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");

		when(systemRepository.findByName("TestSystem")).thenReturn(Optional.of(sys));

		final Optional<System> result = dbService.getSystemByName("TestSystem");

		assertEquals(Optional.of(sys), result);

		verify(systemRepository).findByName("TestSystem");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetSystemsByNamesListNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.getSystemsByNames(null, false));

		assertEquals("names is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetSystemsByNamesListEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.getSystemsByNames(List.of(), false));

		assertEquals("names is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetSystemsByNamesListContainsNull() {
		final List<String> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.getSystemsByNames(list, false));

		assertEquals("names contains null or empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetSystemsByNamesListContainsEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.getSystemsByNames(List.of(""), false));

		assertEquals("names contains null or empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetSystemsByNamesInternalServerError() {
		when(systemRepository.findAllByNameIn(List.of("TestSystem"))).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.getSystemsByNames(List.of("TestSystem"), false));

		assertEquals("Database operation error", ex.getMessage());

		verify(systemRepository).findAllByNameIn(List.of("TestSystem"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetSystemsByNamesStrictFalseOk() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setId(1);

		when(systemRepository.findAllByNameIn(List.of("TestSystem"))).thenReturn(List.of(sys));

		final List<System> result = dbService.getSystemsByNames(List.of("TestSystem"), false);

		assertEquals(List.of(sys), result);

		verify(systemRepository).findAllByNameIn(List.of("TestSystem"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetSystemsByNamesStrictTrueOk() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setId(1);

		when(systemRepository.findAllByNameIn(List.of("TestSystem"))).thenReturn(List.of(sys));

		final List<System> result = dbService.getSystemsByNames(List.of("TestSystem"), true);

		assertEquals(List.of(sys), result);

		verify(systemRepository).findAllByNameIn(List.of("TestSystem"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetSystemsByNamesStrictTrueProblem() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setId(1);

		when(systemRepository.findAllByNameIn(List.of("TestSystem", "TestSystem2"))).thenReturn(List.of(sys));

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> dbService.getSystemsByNames(List.of("TestSystem", "TestSystem2"), true));

		assertEquals("The following systems are not found: TestSystem2", ex.getMessage());

		verify(systemRepository).findAllByNameIn(List.of("TestSystem", "TestSystem2"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testChangePasswordSystemNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.changePassword(null, null));

		assertEquals("system is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testChangePasswordNewPasswordNull() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setId(1);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.changePassword(sys, null));

		assertEquals("newPassword is null or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testChangePasswordNewPasswordEmpty() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setId(1);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.changePassword(sys, ""));

		assertEquals("newPassword is null or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testChangePasswordInternalServerError() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setId(1);

		when(paRepository.findBySystem(sys)).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.changePassword(sys, "newEncoded"));

		assertEquals("Database operation error", ex.getMessage());

		verify(paRepository).findBySystem(sys);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testChangePasswordRecordNotFound() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setId(1);

		when(paRepository.findBySystem(sys)).thenReturn(Optional.empty());

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> dbService.changePassword(sys, "newEncoded"));

		assertEquals("Entry for system TestSystem is not found", ex.getMessage());

		verify(paRepository).findBySystem(sys);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testChangePasswordOk() {
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setId(1);
		final PasswordAuthentication pa = new PasswordAuthentication(sys, "oldEncoded");

		when(paRepository.findBySystem(sys)).thenReturn(Optional.of(pa));
		when(paRepository.saveAndFlush(pa)).thenReturn(pa);

		assertDoesNotThrow(() -> dbService.changePassword(sys, "newEncoded"));

		assertEquals("newEncoded", pa.getPassword());

		verify(paRepository).findBySystem(sys);
		verify(paRepository).saveAndFlush(pa);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentifiableSystemsInBulkRequesterNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.createIdentifiableSystemsInBulk(null, null));

		assertEquals("Requester is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentifiableSystemsInBulkRequesterEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.createIdentifiableSystemsInBulk("", null));

		assertEquals("Requester is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentifiableSystemsInBulkDtoNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.createIdentifiableSystemsInBulk("AdminSystem", null));

		assertEquals("Payload is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentifiableSystemsInBulkAuthenticationMethodNull() {
		final NormalizedIdentityListMgmtRequestDTO dto = new NormalizedIdentityListMgmtRequestDTO(null, null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.createIdentifiableSystemsInBulk("AdminSystem", dto));

		assertEquals("Authentication method is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentifiableSystemsInBulkIdentitiesListNull() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final NormalizedIdentityListMgmtRequestDTO dto = new NormalizedIdentityListMgmtRequestDTO(methodMock, null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.createIdentifiableSystemsInBulk("AdminSystem", dto));

		assertEquals("Identities is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentifiableSystemsInBulkIdentitiesListEmpty() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final NormalizedIdentityListMgmtRequestDTO dto = new NormalizedIdentityListMgmtRequestDTO(methodMock, List.of());

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.createIdentifiableSystemsInBulk("AdminSystem", dto));

		assertEquals("Identities is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentifiableSystemsInBulkIdentitiesListContainsNull() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final List<NormalizedIdentityMgmtRequestDTO> list = new ArrayList<>(1);
		list.add(null);
		final NormalizedIdentityListMgmtRequestDTO dto = new NormalizedIdentityListMgmtRequestDTO(methodMock, list);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.createIdentifiableSystemsInBulk("AdminSystem", dto));

		assertEquals("Identities contains null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentifiableSystemsInBulkInternalServerError1() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final NormalizedIdentityMgmtRequestDTO identityDto = new NormalizedIdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final NormalizedIdentityListMgmtRequestDTO dto = new NormalizedIdentityListMgmtRequestDTO(methodMock, List.of(identityDto));

		when(systemRepository.findAllByNameIn(List.of("TestSystem"))).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.createIdentifiableSystemsInBulk("AdminSystem", dto));

		assertEquals("Database operation error", ex.getMessage());

		verify(systemRepository).findAllByNameIn(List.of("TestSystem"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentifiableSystemsInBulkSystemNameAlreadyUsed() {
		final IAuthenticationMethod methodMock = Mockito.mock(IAuthenticationMethod.class);
		final NormalizedIdentityMgmtRequestDTO identityDto = new NormalizedIdentityMgmtRequestDTO("TestSystem", Map.of("password", "123456"), false);
		final NormalizedIdentityMgmtRequestDTO identityDto2 = new NormalizedIdentityMgmtRequestDTO(null, Map.of("password", "123456"), false);
		final NormalizedIdentityListMgmtRequestDTO dto = new NormalizedIdentityListMgmtRequestDTO(methodMock, List.of(identityDto, identityDto2));
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");

		when(systemRepository.findAllByNameIn(List.of("TestSystem"))).thenReturn(List.of(sys));

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> dbService.createIdentifiableSystemsInBulk("AdminSystem", dto));

		assertEquals("Identifiable systems with names already exist: TestSystem", ex.getMessage());

		verify(systemRepository).findAllByNameIn(List.of("TestSystem"));
	}
}