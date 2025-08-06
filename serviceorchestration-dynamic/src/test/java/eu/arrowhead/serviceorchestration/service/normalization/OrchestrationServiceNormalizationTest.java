package eu.arrowhead.serviceorchestration.service.normalization;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;

@ExtendWith(MockitoExtension.class)
public class OrchestrationServiceNormalizationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private OrchestrationServiceNormalization normalization;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizePushUnsubscribeTestOk() {
		final String subscriptionId = " " + UUID.randomUUID() + " ";
		final String requesterSystem = " RequesterSystem ";

		when(systemNameNormalizer.normalize(eq(requesterSystem))).thenReturn(requesterSystem.trim());

		final Pair<String, UUID> result = assertDoesNotThrow(() -> normalization.normalizePushUnsubscribe(requesterSystem, subscriptionId));

		verify(systemNameNormalizer).normalize(eq(requesterSystem));

		assertEquals(requesterSystem.trim(), result.getLeft());
		assertEquals(subscriptionId.trim(), result.getRight().toString());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizePushUnsubscribeTestEmptyId() {
		final String subscriptionId = "";
		final String requesterSystem = " RequesterSystem ";

		final Throwable ex = assertThrows(Throwable.class, () -> normalization.normalizePushUnsubscribe(requesterSystem, subscriptionId));

		verify(systemNameNormalizer, never()).normalize(anyString());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("subscriptionId is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizePushUnsubscribeTestEmptyRequester() {
		final String subscriptionId = UUID.randomUUID().toString();
		final String requesterSystem = "  ";

		final Throwable ex = assertThrows(Throwable.class, () -> normalization.normalizePushUnsubscribe(requesterSystem, subscriptionId));

		verify(systemNameNormalizer, never()).normalize(anyString());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("requesterSystem is empty", ex.getMessage());
	}
}
