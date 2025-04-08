package eu.arrowhead.authorization.service.validation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authorization.service.dto.NormalizedGrantRequest;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.cloud.CloudIdentifierNormalizer;
import eu.arrowhead.common.service.validation.cloud.CloudIdentifierValidator;
import eu.arrowhead.common.service.validation.name.NameNormalizer;
import eu.arrowhead.common.service.validation.name.NameValidator;
import eu.arrowhead.dto.AuthorizationGrantRequestDTO;
import eu.arrowhead.dto.enums.AuthorizationLevel;

@Service
public class AuthorizationValidation {

	//=================================================================================================
	// members

	@Autowired
	private NameValidator nameValidator;

	@Autowired
	private NameNormalizer nameNormalizer;

	@Autowired
	private CloudIdentifierValidator cloudIdentitiferValidator;

	@Autowired
	private CloudIdentifierNormalizer cloudIdentifierNormalizer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	// VALIDATION

	//-------------------------------------------------------------------------------------------------
	public void validateSystemName(final String systemName, final String origin) {
		logger.debug("validateSystemName started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		if (Utilities.isEmpty(systemName)) {
			throw new InvalidParameterException("System name is empty", origin);
		}

		if (systemName.length() > Constants.SYSTEM_NAME_MAX_LENGTH) {
			throw new InvalidParameterException("System name is too long: " + systemName, origin);
		}

		nameValidator.validateName(systemName);
	}

	//-------------------------------------------------------------------------------------------------
	public void validateGrantRequest(final AuthorizationGrantRequestDTO dto, final String origin) {
		logger.debug("validateGrantRequest started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		// TODO: continue validation
	}

	// VALIDATION AND NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public String validateAndNormalizeSystemName(final String systemName, final String origin) {
		logger.debug("validateAndNormalizeSystemName started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateSystemName(systemName, origin);
		return nameNormalizer.normalize(systemName);
	}

	//-------------------------------------------------------------------------------------------------
	public NormalizedGrantRequest validateAndNormalizeGrantRequest(final String normalizedProvider, final AuthorizationGrantRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeGrantRequest started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateGrantRequest(dto, origin);

		final NormalizedGrantRequest result = new NormalizedGrantRequest(AuthorizationLevel.PROVIDER);
		result.setProvider(normalizedProvider);
		// TODO: continue normalization

		return result;
	}
}
