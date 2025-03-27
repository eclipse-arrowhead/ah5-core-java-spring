package eu.arrowhead.authentication.method;

import java.util.Map;

import eu.arrowhead.common.exception.InternalServerError;

public interface IAuthenticationMethodInputNormalizer {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public default Map<String, String> normalizeCredentials(final Map<String, String> credentials) throws InternalServerError {
		return credentials;
	}
}
