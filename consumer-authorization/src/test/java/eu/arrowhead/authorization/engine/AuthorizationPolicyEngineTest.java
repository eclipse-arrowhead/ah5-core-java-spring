package eu.arrowhead.authorization.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.authorization.jpa.service.AuthorizationPolicyDbService;
import eu.arrowhead.authorization.service.dto.DTOConverter;
import eu.arrowhead.authorization.service.engine.AuthorizationPolicyEngine;
import eu.arrowhead.common.http.ArrowheadHttpService;

@ExtendWith(MockitoExtension.class)
public class AuthorizationPolicyEngineTest {

	//=================================================================================================
	// members

	@InjectMocks
	private AuthorizationPolicyEngine engine;

	@Mock
	private AuthorizationPolicyDbService dbService;

	@Mock
	private ArrowheadHttpService httpService;

	@Mock
	private DTOConverter dtoConverter;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsAccessGrantedNullInput() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> engine.isAccessGranted(null));

		assertEquals("request is null", ex.getMessage());
	}

}