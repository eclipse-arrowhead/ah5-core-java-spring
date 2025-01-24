package eu.arrowhead.authentication.service.validation;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.authentication.method.AuthenticationMethods;
import eu.arrowhead.authentication.method.IAuthenticationMethod;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.NameNormalizer;
import eu.arrowhead.common.service.validation.name.NameValidator;
import eu.arrowhead.dto.ISystemName;
import eu.arrowhead.dto.IdentityChangeRequestDTO;
import eu.arrowhead.dto.IdentityRequestDTO;
import eu.arrowhead.dto.enums.AuthenticationMethod;

@Service
public class IdentityValidation {

	//=================================================================================================
	// members

	@Autowired
	private NameValidator nameValidator;

	@Autowired
	private NameNormalizer nameNormalizer;

	@Autowired
	private AuthenticationMethods methods;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	// VALIDATION

	//-------------------------------------------------------------------------------------------------
	public void validateLoginServicePhase1(final IdentityRequestDTO dto, final String origin) {
		logger.debug("validateLoginServicePhase1 started...");

		validateSystemName(dto, origin);
	}

	// VALIDATION AND NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public IdentityRequestDTO validateAndNormalizeLoginServicePhase1(final IdentityRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeLoginServicePhase1 started...");

		validateLoginServicePhase1(dto, origin);
		final String normalized = nameNormalizer.normalize(dto.systemName());

		return new IdentityRequestDTO(normalized, dto.credentials());
	}

	//-------------------------------------------------------------------------------------------------
	public IdentityRequestDTO validateAndNormalizeLoginServicePhase2(final IdentityRequestDTO dto, final AuthenticationMethod authenticationMethod, final String origin) {
		logger.debug("validateAndNormalizeLoginServicePhase2 started...");

		final IAuthenticationMethod method = methods.method(authenticationMethod);
		if (method == null) {
			throw new InternalServerError("Unsupported authentication method: " + authenticationMethod.name(), origin);
		}

		try {
			method.validator().validateCredentials(dto.credentials());
			final Map<String, String> normalizedCredentials = method.normalizer().normalizeCredentials(dto.credentials());

			return new IdentityRequestDTO(dto.systemName(), normalizedCredentials);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public IdentityChangeRequestDTO validateAndNormalizeChangeServicePhase2(final IdentityChangeRequestDTO dto, final AuthenticationMethod authenticationMethod, final String origin) {
		logger.debug("validateAndNormalizeChangeServicePhase2 started...");

		// at this point only new credential are not validated or normalized
		final IAuthenticationMethod method = methods.method(authenticationMethod);
		if (method == null) {
			throw new InternalServerError("Unsupported authentication method: " + authenticationMethod.name(), origin);
		}

		try {
			method.validator().validateCredentials(dto.newCredentials());
			final Map<String, String> normalizedNewCredentials = method.normalizer().normalizeCredentials(dto.newCredentials());

			return new IdentityChangeRequestDTO(dto.systemName(), dto.credentials(), normalizedNewCredentials);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public String validateAndNormalizeIdentityToken(final String token, final String origin) {
		logger.debug("validateAndNormalizeChangeServicePhase2 started...");

		if (Utilities.isEmpty(token)) {
			throw new InvalidParameterException("Token is missing or empty", origin);
		}

		return token.trim();
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void validateSystemName(final ISystemName dto, final String origin) {
		logger.debug("validateSystemName started...");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		if (Utilities.isEmpty(dto.systemName())) {
			throw new InvalidParameterException("System name is empty", origin);
		}

		try {
			nameValidator.validateName(dto.systemName());
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}
	}
}