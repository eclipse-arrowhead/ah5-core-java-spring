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
package eu.arrowhead.authentication.method.password;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import eu.arrowhead.authentication.jpa.repository.PasswordAuthenticationRepository;
import eu.arrowhead.authentication.service.dto.IdentityData;

@ExtendWith(MockitoExtension.class)
public class PasswordAuthenticationMethodDbServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private PasswordAuthenticationMethodDbService dbService;

	@Mock
	private PasswordEncoder encoder;

	@Mock
	private PasswordAuthenticationRepository paRepository;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentifiableSystemsInBulkNullList() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.createIdentifiableSystemsInBulk(null));

		assertEquals("Identity list is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateIdentifiableSystemsInBulkListContainsNull() {
		final List<IdentityData> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.createIdentifiableSystemsInBulk(list));

		assertEquals("Identity list contains null value", ex.getMessage());
	}
	
	// TODO: cont
}