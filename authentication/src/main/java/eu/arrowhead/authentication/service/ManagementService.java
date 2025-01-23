package eu.arrowhead.authentication.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authentication.validation.ManagementValidation;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.IdentityListMgmtRequestDTO;
import eu.arrowhead.dto.IdentityListMgmtResponseDTO;

@Service
public class ManagementService {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private ManagementValidation validator;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public IdentityListMgmtResponseDTO createIdentitiesOperation(final String requesterName, final IdentityListMgmtRequestDTO dto, final String origin) {
		logger.debug("createIdentitiesOperation started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final String normalizedRequester = validator.validateAndNormalizeRequester(requesterName, origin);

		return null;
	}

}
