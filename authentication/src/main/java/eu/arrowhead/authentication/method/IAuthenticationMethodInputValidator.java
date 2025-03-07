package eu.arrowhead.authentication.method;

import java.util.Map;

import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;

public interface IAuthenticationMethodInputValidator {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public default void validateCredentials(final Map<String, String> credentials) throws InvalidParameterException, InternalServerError {
		// intentionally do nothing
	}
}
