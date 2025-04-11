package eu.arrowhead.authorization.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.authorization.AuthorizationConstants;
import eu.arrowhead.authorization.service.AuthorizationtTokenService;
import eu.arrowhead.common.Constants;
import eu.arrowhead.dto.AuthorizationTokenGenerationRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationResponseDTO;
import eu.arrowhead.dto.AuthorizationTokenVerifyResponseDTO;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping(AuthorizationConstants.HTTP_API_AUTHORIZATION_TOKEN_PATH)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class AuthorizationTokenAPI {
	
	//=================================================================================================
	// members
	
	@Autowired
	private AuthorizationtTokenService authTokenService;

	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public AuthorizationTokenGenerationResponseDTO generate(final AuthorizationTokenGenerationRequestDTO dto) {
		// TODO
		return null;
	}
	
	//-------------------------------------------------------------------------------------------------
	public AuthorizationTokenVerifyResponseDTO verify(final String token) {
		// TODO
		return null;
	}
}
