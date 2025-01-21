package eu.arrowhead.authentication.service;

import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authentication.jpa.entity.ActiveSession;
import eu.arrowhead.authentication.jpa.entity.System;
import eu.arrowhead.authentication.jpa.service.IdentityDbService;
import eu.arrowhead.authentication.method.AuthenticationMethods;
import eu.arrowhead.authentication.method.IAuthenticationMethod;
import eu.arrowhead.authentication.validation.IdentityValidation;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.dto.IdentityLoginResponseDTO;
import eu.arrowhead.dto.IdentityRequestDTO;

@Service
public class IdentityService {

	//=================================================================================================
	// members

	@Autowired
	private IdentityValidation validator;

	@Autowired
	private IdentityDbService dbService;

	@Autowired
	private AuthenticationMethods methods;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public IdentityLoginData loginService(final IdentityRequestDTO dto, final boolean noSession, final String origin) {
		logger.debug("login service started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		// Phase 1: authentication method independent steps
		IdentityRequestDTO normalized = validator.validateAndNormalizeLoginServicePhase1(dto, origin);
		final Optional<System> systemOpt = dbService.getSystemByName(normalized.systemName());

		if (systemOpt.isEmpty()) {
			// system is not known
			throw new AuthException("Invalid name and/or credentials", origin);
		}

		final System system = systemOpt.get();

		// Phase 2: authentication method dependent steps
		normalized = validator.validateAndNormalizeLoginServicePhase2(normalized, system.getAuthenticationMethod(), origin);

		final IAuthenticationMethod method = methods.method(system.getAuthenticationMethod());
		Assert.notNull(method, "Authentication method implementation not found: " + system.getAuthenticationMethod().name());

		boolean verified = false;
		try {
			verified = method.service().verifyCredentials(system, normalized.credentials());
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		} catch (final ExternalServerError ex) {
			throw new ExternalServerError(ex.getMessage(), origin);
		}

		// Phase 3: authentication method independent steps
		if (!verified) {
			throw new AuthException("Invalid name and/or credentials", origin);
		}

		if (noSession) {
			// no need to create session, we just want to check credentials
			return new IdentityLoginData(normalized, null);
		}

		final String token = UUID.randomUUID().toString();
		try {
			final ActiveSession session = dbService.createOrUpdateSession(system, token);

			return new IdentityLoginData(normalized, new IdentityLoginResponseDTO(token, Utilities.convertZonedDateTimeToUTCString(session.getExpirationTime())));
		} catch (final InternalServerError ex) {
			method.service().rollbackCredentialsVerification(system, normalized.credentials(), ex.getMessage());
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void logoutService(final IdentityRequestDTO dto, final String origin) {
		logger.debug("logout service started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		// check if request is coming from a verified system
		final IdentityLoginData data = loginService(dto, true, origin);

		try {
			final System system = dbService.removeSession(data.normalizedRequest().systemName());
			if (system != null) {
				final IAuthenticationMethod method = methods.method(system.getAuthenticationMethod());
				Assert.notNull(method, "Authentication method implementation not found: " + system.getAuthenticationMethod().name());
				method.service().logout(system, data.normalizedRequest().credentials());
			}
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//=================================================================================================
	// assistant methods
}