package eu.arrowhead.authentication.method.password;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import eu.arrowhead.authentication.method.IAuthenticationMethodInputNormalizer;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;

@Service
public class PasswordAuthenticationMethodInputNormalizer implements IAuthenticationMethodInputNormalizer {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public Map<String, String> normalizeCredentials(final Map<String, String> credentials) throws InternalServerError {
		logger.debug("PasswordAuthenticationMethodInputNormalizer.normalizeCredentials started...");

		if (credentials == null
				|| !credentials.containsKey(PasswordAuthenticationMethod.KEY_PASSWORD)
				|| Utilities.isEmpty(credentials.get(PasswordAuthenticationMethod.KEY_PASSWORD))) {
			return credentials;
		}

		return Map.of(PasswordAuthenticationMethod.KEY_PASSWORD, credentials.get(PasswordAuthenticationMethod.KEY_PASSWORD).trim());
	}
}