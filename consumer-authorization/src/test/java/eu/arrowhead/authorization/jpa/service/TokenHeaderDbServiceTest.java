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
package eu.arrowhead.authorization.jpa.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.authorization.jpa.entity.TokenHeader;
import eu.arrowhead.authorization.jpa.repository.TokenHeaderRepository;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import eu.arrowhead.dto.enums.AuthorizationTokenType;

@ExtendWith(MockitoExtension.class)
public class TokenHeaderDbServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private TokenHeaderDbService dbService;

	@Mock
	private TokenHeaderRepository headerRepo;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFind1ProviderNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.find(null, null));

		assertEquals("provider is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFind1ProviderEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.find("", null));

		assertEquals("provider is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFind1TokenHashNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.find("TestProvider", null));

		assertEquals("tokenHash is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFind1TokenHashEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.find("TestProvider", ""));

		assertEquals("tokenHash is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFind1InternalServerError() {
		when(headerRepo.findByProviderAndTokenHash("TestProvider", "hash")).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.find("TestProvider", "hash"));

		assertEquals("Database operation error", ex.getMessage());

		verify(headerRepo).findByProviderAndTokenHash("TestProvider", "hash");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFind1ok() {
		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"hash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		header.setId(1);

		when(headerRepo.findByProviderAndTokenHash("TestProvider", "hash")).thenReturn(Optional.of(header));

		final Optional<TokenHeader> result = dbService.find("TestProvider", "hash");

		assertFalse(result.isEmpty());
		assertEquals(header, result.get());

		verify(headerRepo).findByProviderAndTokenHash("TestProvider", "hash");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFind2TokenHashNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.find(null));

		assertEquals("tokenHash is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFind2TokenHashEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.find(""));

		assertEquals("tokenHash is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFind2InternalServerError() {
		when(headerRepo.findByTokenHash("hash")).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.find("hash"));

		assertEquals("Database operation error", ex.getMessage());

		verify(headerRepo).findByTokenHash("hash");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFind2ok() {
		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"hash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		header.setId(1);

		when(headerRepo.findByTokenHash("hash")).thenReturn(Optional.of(header));

		final Optional<TokenHeader> result = dbService.find("hash");

		assertFalse(result.isEmpty());
		assertEquals(header, result.get());

		verify(headerRepo).findByTokenHash("hash");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindByTokenHashListNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.findByTokenHashList(null));

		assertEquals("token hash list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindByTokenHashListEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.findByTokenHashList(List.of()));

		assertEquals("token hash list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindByTokenHashListContainsNullElement() {
		final List<String> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.findByTokenHashList(list));

		assertEquals("token hash list contains null or empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindByTokenHashListContainsEmptyElement() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.findByTokenHashList(List.of("")));

		assertEquals("token hash list contains null or empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindByTokenHashListInternalServerError() {
		when(headerRepo.findAllByTokenHashIn(List.of("hash"))).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.findByTokenHashList(List.of("hash")));

		assertEquals("Database operation error", ex.getMessage());

		verify(headerRepo).findAllByTokenHashIn(List.of("hash"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindByTokenHashListOk() {
		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"hash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		header.setId(1);

		when(headerRepo.findAllByTokenHashIn(List.of("hash"))).thenReturn(List.of(header));

		final List<TokenHeader> result = dbService.findByTokenHashList(List.of("hash"));

		assertFalse(result.isEmpty());
		assertEquals(header, result.get(0));

		verify(headerRepo).findAllByTokenHashIn(List.of("hash"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteByIdCollectionNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.deleteById(null));

		assertEquals("ID collection is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteByIdCollectionEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.deleteById(List.of()));

		assertEquals("ID collection is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteByIdCollectionContainsNullElement() {
		final List<Long> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.deleteById(list));

		assertEquals("ID collection contains null element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteByIdInternalServerError() {
		doThrow(RuntimeException.class).when(headerRepo).deleteAllByIdInBatch(List.of(1L));

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.deleteById(List.of(1L)));

		assertEquals("Database operation error", ex.getMessage());

		verify(headerRepo).deleteAllByIdInBatch(List.of(1L));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteByIdOk() {
		doNothing().when(headerRepo).deleteAllByIdInBatch(List.of(1L));
		doNothing().when(headerRepo).flush();

		assertDoesNotThrow(() -> dbService.deleteById(List.of(1L)));

		verify(headerRepo).deleteAllByIdInBatch(List.of(1L));
		verify(headerRepo).flush();
	}
	
	// TODO: continue with query
}