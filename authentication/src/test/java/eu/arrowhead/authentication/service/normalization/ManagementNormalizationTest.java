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
package eu.arrowhead.authentication.service.normalization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.authentication.method.AuthenticationMethods;
import eu.arrowhead.common.service.PageService;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.dto.IdentityListMgmtCreateRequestDTO;
import eu.arrowhead.dto.IdentityMgmtRequestDTO;
import eu.arrowhead.dto.enums.AuthenticationMethod;

@ExtendWith(MockitoExtension.class)
public class ManagementNormalizationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private ManagementNormalization normalizer;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	@Mock
	private PageService pageService;

	@Mock
	private AuthenticationMethods methods;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeCreateIdentityListDTONull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeCreateIdentityList(null));

		assertEquals("Payload is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeCreateIdentityListNullList() {
		final IdentityListMgmtCreateRequestDTO dto = new IdentityListMgmtCreateRequestDTO(null, null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeCreateIdentityList(dto));

		assertEquals("Identities list is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeCreateIdentityListEmptyList() {
		final IdentityListMgmtCreateRequestDTO dto = new IdentityListMgmtCreateRequestDTO(null, List.of());

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeCreateIdentityList(dto));

		assertEquals("Identities list is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeCreateIdentityListListContainsNull() {
		final List<IdentityMgmtRequestDTO> list = new ArrayList<>(1);
		list.add(null);

		final IdentityListMgmtCreateRequestDTO dto = new IdentityListMgmtCreateRequestDTO(null, list);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeCreateIdentityList(dto));

		assertEquals("Identities list contains null element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeCreateIdentityListAuthenticationMethodNull() {
		final IdentityMgmtRequestDTO request = new IdentityMgmtRequestDTO("TestProvider", Map.of("key", "value"), false);
		final IdentityListMgmtCreateRequestDTO dto = new IdentityListMgmtCreateRequestDTO(null, List.of(request));

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeCreateIdentityList(dto));

		assertEquals("Authentication method is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeCreateIdentityListAuthenticationMethodEmpty() {
		final IdentityMgmtRequestDTO request = new IdentityMgmtRequestDTO("TestProvider", Map.of("key", "value"), false);
		final IdentityListMgmtCreateRequestDTO dto = new IdentityListMgmtCreateRequestDTO("", List.of(request));

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeCreateIdentityList(dto));

		assertEquals("Authentication method is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeCreateIdentityListAuthenticationMethodInvalid() {
		final IdentityMgmtRequestDTO request = new IdentityMgmtRequestDTO("TestProvider", Map.of("key", "value"), false);
		final IdentityListMgmtCreateRequestDTO dto = new IdentityListMgmtCreateRequestDTO("invalid", List.of(request));

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeCreateIdentityList(dto));

		assertEquals("Authentication method is invalid", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeCreateIdentityListAuthenticationMethodUnsupported() {
		final IdentityMgmtRequestDTO request = new IdentityMgmtRequestDTO("TestProvider", Map.of("key", "value"), false);
		final IdentityListMgmtCreateRequestDTO dto = new IdentityListMgmtCreateRequestDTO("password", List.of(request));

		when(methods.method(AuthenticationMethod.PASSWORD)).thenReturn(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeCreateIdentityList(dto));

		assertEquals("Authentication method is unsupported", ex.getMessage());

		verify(methods).method(AuthenticationMethod.PASSWORD);
	}
}