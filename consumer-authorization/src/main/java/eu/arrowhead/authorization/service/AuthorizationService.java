package eu.arrowhead.authorization.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authorization.service.dto.NormalizedGrantRequest;
import eu.arrowhead.authorization.service.validation.AuthorizationValidation;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.AuthorizationGrantRequestDTO;
import eu.arrowhead.dto.AuthorizationPolicyResponseDTO;

@Service
public class AuthorizationService {

	//=================================================================================================
	// members

	@Autowired
	private AuthorizationValidation validator;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public AuthorizationPolicyResponseDTO grantOperation(final String provider, final AuthorizationGrantRequestDTO dto, final String origin) {
		logger.debug("grantOperation started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final String normalizedProvider = validator.validateAndNormalizeSystemName(provider, origin);
		final NormalizedGrantRequest normalized = validator.validateAndNormalizeGrantRequest(normalizedProvider, dto, origin);
		
		// TODO: continue

		return null;
	}

}
