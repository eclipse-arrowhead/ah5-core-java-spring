package eu.arrowhead.authentication.method.password;

import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authentication.jpa.entity.PasswordAuthentication;
import eu.arrowhead.authentication.jpa.entity.System;
import eu.arrowhead.authentication.jpa.service.IdentityDbService;
import eu.arrowhead.authentication.method.IAuthenticationMethodService;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.exception.InternalServerError;

@Service
public class PasswordAuthenticationMethodService implements IAuthenticationMethodService {

	//=================================================================================================
	// members

	@Autowired
	private PasswordEncoder encoder;

	@Autowired
	private IdentityDbService identityDbService;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public boolean verifyCredentials(final System system, final Map<String, String> credentials) throws InternalServerError, ExternalServerError {
		logger.debug("PasswordAuthenticationMethodService.verifyCredentials started...");
		Assert.notNull(system, "system is null");
		Assert.notNull(credentials, "credentials is null");
		Assert.isTrue(!Utilities.isEmpty(credentials.get(PasswordAuthenticationMethod.KEY_PASSWORD)), "password field is missing or empty");

		final String rawPassword = credentials.get(PasswordAuthenticationMethod.KEY_PASSWORD);
		final Optional<PasswordAuthentication> passwordOpt = identityDbService.getPasswordAuthenticationBySystem(system);
		if (passwordOpt.isEmpty()) {
			// database inconsistency
			logger.error("System {} has no assigned password", system.getName());
			return false;
		}

		return encoder.matches(rawPassword, passwordOpt.get().getPassword());
	}

}
