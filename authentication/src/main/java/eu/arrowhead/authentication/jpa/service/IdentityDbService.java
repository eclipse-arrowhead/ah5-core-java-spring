package eu.arrowhead.authentication.jpa.service;

import java.security.InvalidParameterException;
import java.time.ZonedDateTime;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import eu.arrowhead.authentication.AuthenticationConstants;
import eu.arrowhead.authentication.jpa.entity.ActiveSession;
import eu.arrowhead.authentication.jpa.entity.PasswordAuthentication;
import eu.arrowhead.authentication.jpa.entity.System;
import eu.arrowhead.authentication.jpa.repository.ActiveSessionRepository;
import eu.arrowhead.authentication.jpa.repository.PasswordAuthenticationRepository;
import eu.arrowhead.authentication.jpa.repository.SystemRepository;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InternalServerError;

@Service
public class IdentityDbService {

	//=================================================================================================
	// members

	@Value(AuthenticationConstants.$IDENTITY_TOKEN_DURATION)
	private int identityTokenDuration;

	@Autowired
	private SystemRepository systemRepository;

	@Autowired
	private PasswordAuthenticationRepository paRepository;

	@Autowired
	private ActiveSessionRepository asRepository;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Optional<System> getSystemByName(final String systemName) {
		logger.debug("getSystemByName started...");
		Assert.isTrue(!Utilities.isEmpty(systemName), "systemName is empty");

		try {
			return systemRepository.findBySystemName(systemName);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public Optional<PasswordAuthentication> getPasswordAuthenticationBySystem(final System system) {
		logger.debug("getPasswordAuthenticationBySystem started...");
		Assert.notNull(system, "system is null");

		try {
			return paRepository.findBySystem(system);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public void changePassword(final System system, final String newPassword) {
		logger.debug("changePassword started...");
		Assert.notNull(system, "system is null");
		Assert.isTrue(!Utilities.isEmpty(newPassword), "newPassword is null or empty");

		try {
			final Optional<PasswordAuthentication> authOpt = paRepository.findBySystem(system);
			if (authOpt.isEmpty()) {
				throw new InvalidParameterException("Entry for system " + system.getName() + " not found");
			}

			final PasswordAuthentication auth = authOpt.get();
			auth.setPassword(newPassword);
			paRepository.saveAndFlush(auth);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public Optional<ActiveSession> getSessionByToken(final String token) {
		logger.debug("getSessionByToken started...");
		Assert.isTrue(!Utilities.isEmpty(token), "Token is missing or empty");

		try {
			return asRepository.findByToken(token);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public ActiveSession createOrUpdateSession(final System system, final String token) {
		logger.debug("getPasswordAuthenticationBySystem started...");
		Assert.notNull(system, "system is null");
		Assert.isTrue(!Utilities.isEmpty(token), "Token is missing or empty");

		try {
			final Optional<ActiveSession> sessionOpt = asRepository.findBySystem(system);
			final ZonedDateTime now = Utilities.utcNow();
			final ZonedDateTime expirationTime = identityTokenDuration > 0 ? now.plusSeconds(identityTokenDuration) : now.plusYears(AuthenticationConstants.INFINITE_TOKEN_DURATION);

			final ActiveSession session = sessionOpt.isPresent() ? sessionOpt.get() : new ActiveSession(system, token, now, expirationTime);
			if (sessionOpt.isPresent()) {
				if (now.isAfter(session.getExpirationTime())) {
					// session is already expired, just the record is not removed => re-using it
					session.setLoginTime(now);
				}

				// update
				session.setToken(token);
				session.setExpirationTime(expirationTime);
			}

			return asRepository.saveAndFlush(session);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public void removeSession(final String systemName) {
		logger.debug("removeSession started...");
		Assert.isTrue(!Utilities.isEmpty(systemName), "System name is missing or empty");

		try {
			asRepository.deleteBySystem_Name(systemName);
			asRepository.flush();
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}
}