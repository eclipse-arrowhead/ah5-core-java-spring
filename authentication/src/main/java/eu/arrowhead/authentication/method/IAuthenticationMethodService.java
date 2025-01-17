package eu.arrowhead.authentication.method;

import java.util.Map;

import eu.arrowhead.authentication.jpa.entity.System;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.exception.InternalServerError;

public interface IAuthenticationMethodService {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public boolean verifyCredentials(final System system, final Map<String, String> credentials) throws InternalServerError, ExternalServerError;

	//-------------------------------------------------------------------------------------------------
	public default void rollbackCredentialsVerification(final System system, final Map<String, String> credentials, final String cause) {
		// intentionally do nothing
	}
}