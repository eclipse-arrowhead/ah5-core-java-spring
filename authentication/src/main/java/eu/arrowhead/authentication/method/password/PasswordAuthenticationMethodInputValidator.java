package eu.arrowhead.authentication.method.password;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import eu.arrowhead.authentication.method.IAuthenticationMethodInputValidator;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;

@Service
public class PasswordAuthenticationMethodInputValidator implements IAuthenticationMethodInputValidator {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public void validateCredentials(final Map<String, String> credentials) throws InvalidParameterException, InternalServerError {
		logger.debug("PasswordAuthenticationMethodInputValidator.validateCredentials started...");

		if (Utilities.isEmpty(credentials)
				|| !credentials.containsKey(PasswordAuthenticationMethod.KEY_PASSWORD)) {
			throw new InvalidParameterException("Missing credentials");
		}

		if (Utilities.isEmpty(credentials.get(PasswordAuthenticationMethod.KEY_PASSWORD))) {
			throw new InvalidParameterException("Missing or empty password.");
		}
	}
}