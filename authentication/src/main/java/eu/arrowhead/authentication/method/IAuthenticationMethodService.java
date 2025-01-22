package eu.arrowhead.authentication.method;

import java.util.Map;

import eu.arrowhead.authentication.jpa.entity.System;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;

public interface IAuthenticationMethodService {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public boolean verifyCredentials(final System system, final Map<String, String> credentials) throws InternalServerError, ExternalServerError;

	//-------------------------------------------------------------------------------------------------
	public default void rollbackCredentialsVerification(final System system, final Map<String, String> credentials, final String cause) {
		// intentionally do nothing
	}

	//-------------------------------------------------------------------------------------------------
	public void changeCredentials(final System system, final Map<String, String> oldCredentials, final Map<String, String> newCredentials)
			throws InvalidParameterException, InternalServerError, ExternalServerError;

	//-------------------------------------------------------------------------------------------------
	public default void logout(final System system, final Map<String, String> credentials) {
		// intentionally do nothing
	}
}