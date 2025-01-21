package eu.arrowhead.authentication.validation;

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

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		if (Utilities.isEmpty(dto.systemName())) {
			throw new InvalidParameterException("System name is empty", origin);
		}

		nameValidator.validateName(dto.systemName());
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
			final Map<String, String> normalizeCredentials = method.normalizer().normalizeCredentials(dto.credentials());

			return new IdentityRequestDTO(dto.systemName(), normalizeCredentials);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}
}