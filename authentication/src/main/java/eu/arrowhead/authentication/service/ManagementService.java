package eu.arrowhead.authentication.service;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authentication.jpa.entity.System;
import eu.arrowhead.authentication.jpa.service.IdentityDbService;
import eu.arrowhead.authentication.service.dto.DTOConverter;
import eu.arrowhead.authentication.service.dto.NormalizedIdentityListMgmtRequestDTO;
import eu.arrowhead.authentication.service.validation.ManagementValidation;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.IdentityListMgmtRequestDTO;
import eu.arrowhead.dto.IdentityListMgmtResponseDTO;

@Service
public class ManagementService {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private ManagementValidation validator;

	@Autowired
	private IdentityDbService dbService;

	@Autowired
	private DTOConverter converter;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public IdentityListMgmtResponseDTO createIdentitiesOperation(final String requesterName, final IdentityListMgmtRequestDTO dto, final String origin) {
		logger.debug("createIdentitiesOperation started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final String normalizedRequester = validator.validateAndNormalizeRequester(requesterName, origin);
		final NormalizedIdentityListMgmtRequestDTO normalizedDto = validator.validateAndNormalizeIdentityList(dto, origin);

		try {
			final List<System> systems = dbService.createIdentifiableSystemsInBulk(normalizedRequester, normalizedDto);

			return converter.convertIdentifiableSystemListToDTO(systems);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		} catch (final ExternalServerError ex) {
			throw new ExternalServerError(ex.getMessage(), origin);
		}
	}
}