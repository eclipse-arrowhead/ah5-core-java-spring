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
import eu.arrowhead.dto.IdentityLoginRequestDTO;
import eu.arrowhead.dto.IdentityLoginResponseDTO;

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
	public IdentityLoginResponseDTO loginService(final IdentityLoginRequestDTO dto, final String origin) {
		logger.debug("login service started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		// Phase 1: authentication method independent steps
		IdentityLoginRequestDTO normalized = validator.validateAndNormalizeLoginServicePhase1(dto, origin);
		final Optional<System> systemOpt = dbService.getSystemByName(normalized.systemName());

		if (systemOpt.isEmpty()) {
			// system is not known
			throw new AuthException("Invalid name and/or credentials", origin);
		}

		final System system = systemOpt.get();

		// Phase 2: authentication method dependent steps
		normalized = validator.validateAndNormalizeLoginServicePhase2(dto, system.getAuthenticationMethod(), origin);

		final IAuthenticationMethod method = methods.method(system.getAuthenticationMethod());
		Assert.notNull(method, "Authentication method implementation not found: " + system.getAuthenticationMethod().name());

		boolean verified = false;
		try {
			verified = method.service().verifyCredentials(system, dto.credentials());
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		} catch (final ExternalServerError ex) {
			throw new ExternalServerError(ex.getMessage(), origin);
		}

		// Phase 3: authentication method independent steps
		if (!verified) {
			throw new AuthException("Invalid name and/or credentials", origin);
		}

		final String token = UUID.randomUUID().toString();
		try {
			final ActiveSession session = dbService.createOrUpdateSession(system, token);

			return new IdentityLoginResponseDTO(token, Utilities.convertZonedDateTimeToUTCString(session.getExpirationTime()));
		} catch (final InternalServerError ex) {
			method.service().rollbackCredentialsVerification(system, dto.credentials(), ex.getMessage());
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}
}