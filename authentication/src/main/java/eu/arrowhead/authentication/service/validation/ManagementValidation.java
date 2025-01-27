package eu.arrowhead.authentication.service.validation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.authentication.AuthenticationConstants;
import eu.arrowhead.authentication.method.AuthenticationMethods;
import eu.arrowhead.authentication.method.IAuthenticationMethod;
import eu.arrowhead.authentication.service.dto.NormalizedIdentityListMgmtRequestDTO;
import eu.arrowhead.authentication.service.normalization.ManagementNormalization;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.NameNormalizer;
import eu.arrowhead.common.service.validation.name.NameValidator;
import eu.arrowhead.dto.IdentityListMgmtRequestDTO;
import eu.arrowhead.dto.IdentityMgmtRequestDTO;
import eu.arrowhead.dto.enums.AuthenticationMethod;

@Service
public class ManagementValidation {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private NameValidator nameValidator;

	@Autowired
	private NameNormalizer nameNormalizer;

	@Autowired
	private ManagementNormalization normalizer;

	@Autowired
	private AuthenticationMethods methods;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// VALIDATION

	//-------------------------------------------------------------------------------------------------
	public void validateRequester(final String requester, final String origin) {
		logger.debug("validateRequester started...");

		if (Utilities.isEmpty(requester)) {
			throw new InvalidParameterException("Requester name is missing or empty", origin);
		}

		if (requester.length() > AuthenticationConstants.SYSTEM_NAME_LENGTH) {
			throw new InvalidParameterException("Requester name is too long: " + requester, origin);
		}

		try {
			nameValidator.validateName(requester);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void validateIdentityList(final IdentityListMgmtRequestDTO dto, final String origin) {
		logger.debug("validateIdentityListPhase1 started...");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		if (Utilities.isEmpty(dto.authenticationMethod())) {
			throw new InvalidParameterException("Authentication method is missing", origin);
		}

		final String authMethodName = dto.authenticationMethod().trim().toUpperCase();

		if (!Utilities.isEnumValue(authMethodName, AuthenticationMethod.class)) {
			throw new InvalidParameterException("Authentication method is invalid: " + authMethodName, origin);
		}

		final AuthenticationMethod methodType = AuthenticationMethod.valueOf(authMethodName);
		final IAuthenticationMethod method = methods.method(methodType);
		if (method == null) {
			throw new InvalidParameterException("Authentication method is unsupported: " + authMethodName, origin);
		}

		final List<IdentityMgmtRequestDTO> list = dto.identities();
		if (Utilities.isEmpty(list)) {
			throw new InvalidParameterException("Identity list is missing or empty", origin);
		}

		if (Utilities.containsNull(list)) {
			throw new InvalidParameterException("Identity list contains null element", origin);
		}

		for (final IdentityMgmtRequestDTO identity : list) {
			validateIdentity(method, identity, origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	// VALIDATION AND NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public String validateAndNormalizeRequester(final String requester, final String origin) {
		logger.debug("validateAndNormalizeRequester started...");

		validateRequester(requester, origin);

		return nameNormalizer.normalize(requester);
	}

	//-------------------------------------------------------------------------------------------------
	public NormalizedIdentityListMgmtRequestDTO validateAndNormalizeIdentityList(final IdentityListMgmtRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeIdentityListPhase1 started...");

		validateIdentityList(dto, origin);

		try {
			final NormalizedIdentityListMgmtRequestDTO result = normalizer.normalizeIdentityList(dto);
			checkNameDuplications(result.identities().stream().map(ni -> ni.systemName()).toList(), origin);

			return result;
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void validateIdentity(final IAuthenticationMethod method, final IdentityMgmtRequestDTO identity, final String origin) {
		logger.debug("validateIdentity started...");

		if (Utilities.isEmpty(identity.systemName())) {
			throw new InvalidParameterException("System name is missing or empty", origin);
		}

		if (identity.systemName().length() > AuthenticationConstants.SYSTEM_NAME_LENGTH) {
			throw new InvalidParameterException("System name is too long: " + identity.systemName(), origin);
		}

		try {
			nameValidator.validateName(identity.systemName());
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		try {
			method.validator().validateCredentials(identity.credentials());
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void checkNameDuplications(final List<String> names, final String origin) {
		logger.debug("checkNameDuplications started...");

		final Set<String> uniqueNames = new HashSet<>();
		for (final String name : names) {
			if (uniqueNames.contains(name)) {
				throw new InvalidParameterException("Duplicated system name: " + name, origin);
			}

			uniqueNames.add(name);
		}
	}
}