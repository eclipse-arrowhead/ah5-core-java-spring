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
package eu.arrowhead.authorization.service.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.util.Pair;

import eu.arrowhead.authorization.jpa.entity.AuthPolicy;
import eu.arrowhead.authorization.jpa.entity.AuthPolicyHeader;
import eu.arrowhead.authorization.jpa.entity.AuthProviderPolicyHeader;
import eu.arrowhead.dto.enums.AuthorizationLevel;

public class DTOConverterTest {

	//=================================================================================================
	// members

	private DTOConverter converter = new DTOConverter();

	//=================================================================================================
	// methods
	
	// TODO: continue with testing convertAuthPolicyToDTO

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertPolicyToResponseLevelNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertPolicyToResponse(null, null));

		assertEquals("level is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertPolicyToResponseEntitiesNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertPolicyToResponse(AuthorizationLevel.PROVIDER, null));

		assertEquals("entities is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertPolicyToResponseListEmpty() {
		final Pair<? extends AuthPolicyHeader, List<AuthPolicy>> pair = Pair.of(new AuthProviderPolicyHeader(), List.of());

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertPolicyToResponse(AuthorizationLevel.PROVIDER, pair));

		assertEquals("list is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertPolicyToResponseListContainsNullElement() {
		final List<AuthPolicy> list = new ArrayList<>(1);
		list.add(null);
		final Pair<? extends AuthPolicyHeader, List<AuthPolicy>> pair = Pair.of(new AuthProviderPolicyHeader(), list);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertPolicyToResponse(AuthorizationLevel.PROVIDER, pair));

		assertEquals("list contains null element", ex.getMessage());
	}

	// TODO: continue after convertAuthPolicyToDTO is tested

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertProviderLevelPolicyPageToResponseNullPage() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertProviderLevelPolicyPageToResponse(null));

		assertEquals("page is null", ex.getMessage());
	}

	// TODO: continue after convertPolicyToResponse is tested
}