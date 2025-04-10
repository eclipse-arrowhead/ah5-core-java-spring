package eu.arrowhead.authorization.service;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authorization.jpa.entity.AuthPolicy;
import eu.arrowhead.authorization.jpa.entity.AuthProviderPolicyHeader;
import eu.arrowhead.authorization.jpa.service.AuthorizationPolicyDbService;
import eu.arrowhead.authorization.service.dto.DTOConverter;
import eu.arrowhead.authorization.service.dto.NormalizedGrantRequest;
import eu.arrowhead.authorization.service.validation.AuthorizationValidation;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.AuthorizationGrantRequestDTO;
import eu.arrowhead.dto.AuthorizationPolicyResponseDTO;

@Service
public class AuthorizationService {

	//=================================================================================================
	// members

	@Autowired
	private AuthorizationValidation validator;

	@Autowired
	private AuthorizationPolicyDbService dbService;

	@Autowired
	private DTOConverter dtoConverter;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public AuthorizationPolicyResponseDTO grantOperation(final String provider, final AuthorizationGrantRequestDTO dto, final String origin) {
		logger.debug("grantOperation started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final String normalizedProvider = validator.validateAndNormalizeSystemName(provider, origin);
		final NormalizedGrantRequest normalized = validator.validateAndNormalizeGrantRequest(normalizedProvider, dto, origin);

		try {
			final Pair<AuthProviderPolicyHeader, List<AuthPolicy>> result = dbService.createProviderLevelPolicy(normalized);

			return dtoConverter.convertProviderLevelPolicyToResponse(result);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public boolean revokeOperation(final String identifiedSystemName, final String instanceId, final String origin) {
		logger.debug("revokeOperation started...");
		Assert.isTrue(!Utilities.isEmpty(identifiedSystemName), "identifiedSystemName is empty");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		// TODO: continue

		return false;
	}
}