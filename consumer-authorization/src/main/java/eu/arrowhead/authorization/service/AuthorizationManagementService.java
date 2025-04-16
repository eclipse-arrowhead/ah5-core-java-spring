package eu.arrowhead.authorization.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authorization.service.validation.AuthorizationManagementValidation;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.AuthorizationMgmtGrantListRequestDTO;
import eu.arrowhead.dto.AuthorizationPolicyListResponseDTO;

@Service
public class AuthorizationManagementService {

	//=================================================================================================
	// members

	@Autowired
	private AuthorizationManagementValidation validator;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public AuthorizationPolicyListResponseDTO grantPoliciesOperation(final String requester, final AuthorizationMgmtGrantListRequestDTO dto, final String origin) {
		logger.debug("grantPoliciesOperation started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		// TODO: continue

		return null;
	}

}
