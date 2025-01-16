package eu.arrowhead.authentication.service;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authentication.jpa.entity.System;
import eu.arrowhead.authentication.jpa.service.IdentityDbService;
import eu.arrowhead.authentication.validation.IdentityValidation;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.AuthException;
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

		// TODO: continue
		// call method dependent service
		// if it is return true, create a session
		// if session creation throws an exception, call method dependent service to "rollback" login

		return null;
	}
}