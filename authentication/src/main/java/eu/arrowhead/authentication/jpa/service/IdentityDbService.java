package eu.arrowhead.authentication.jpa.service;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authentication.jpa.entity.System;
import eu.arrowhead.authentication.jpa.repository.SystemRepository;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;

@Service
public class IdentityDbService {

	//=================================================================================================
	// members

	@Autowired
	private SystemRepository systemRepository;

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
}