package eu.arrowhead.authentication.service.validation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authentication.AuthenticationConstants;
import eu.arrowhead.authentication.method.AuthenticationMethods;
import eu.arrowhead.authentication.method.IAuthenticationMethod;
import eu.arrowhead.authentication.service.dto.NormalizedIdentityListMgmtRequestDTO;
import eu.arrowhead.authentication.service.dto.NormalizedIdentityMgmtRequestDTO;
import eu.arrowhead.authentication.service.normalization.ManagementNormalization;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.NameNormalizer;
import eu.arrowhead.common.service.validation.name.NameValidator;
import eu.arrowhead.dto.IdentityListMgmtCreateRequestDTO;
import eu.arrowhead.dto.IdentityListMgmtUpdateRequestDTO;
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
	public void validateCreateIdentityList(final IdentityListMgmtCreateRequestDTO dto, final String origin) {
		logger.debug("validateCreateIdentityList started...");

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
	public void validateUpdateIdentityListPhase1(final IdentityListMgmtUpdateRequestDTO dto, final String origin) {
		logger.debug("validateUpdateIdentityListPhase1 started...");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		final List<IdentityMgmtRequestDTO> list = dto.identities();
		if (Utilities.isEmpty(list)) {
			throw new InvalidParameterException("Identity list is missing or empty", origin);
		}

		if (Utilities.containsNull(list)) {
			throw new InvalidParameterException("Identity list contains null element", origin);
		}

		for (final IdentityMgmtRequestDTO identity : list) {
			validateIdentityWithoutCredentials(identity, origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void validateUpdateIdentityListPhase2(final IAuthenticationMethod authenticationMethod, final List<NormalizedIdentityMgmtRequestDTO> identities, final String origin) {
		logger.debug("validateUpdateIdentityListPhase2 started...");
		Assert.notNull(authenticationMethod, "Authentication method is null");
		Assert.isTrue(!Utilities.isEmpty(identities), "Identities list is missing or empty");
		Assert.isTrue(!Utilities.containsNull(identities), "Identities list contains null element");

		for (final NormalizedIdentityMgmtRequestDTO identity : identities) {
			try {
				authenticationMethod.validator().validateCredentials(identity.credentials());
			} catch (final InvalidParameterException ex) {
				throw new InvalidParameterException(ex.getMessage(), origin);
			} catch (final InternalServerError ex) {
				throw new InternalServerError(ex.getMessage(), origin);
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void validateRemoveIdentities(final List<String> originalNames, final String origin) {
		logger.debug("validateRemoveSystems started");

		if (Utilities.isEmpty(originalNames)) {
			throw new InvalidParameterException("Identifiable system name list is missing or empty", origin);
		}

		if (Utilities.containsNullOrEmpty(originalNames)) {
			throw new InvalidParameterException("Identifiable system name list contains null or empty element", origin);
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
	public NormalizedIdentityListMgmtRequestDTO validateAndNormalizeCreateIdentityList(final IdentityListMgmtCreateRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeCreateIdentityList started...");

		validateCreateIdentityList(dto, origin);

		try {
			final NormalizedIdentityListMgmtRequestDTO result = normalizer.normalizeCreateIdentityList(dto);
			checkNameDuplications(result.identities().stream().map(ni -> ni.systemName()).toList(), origin);

			return result;
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public List<NormalizedIdentityMgmtRequestDTO> validateAndNormalizeUpdateIdentityListPhase1(final IdentityListMgmtUpdateRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeUpdateIdentityListPhase1 started...");

		validateUpdateIdentityListPhase1(dto, origin);

		final List<NormalizedIdentityMgmtRequestDTO> result = normalizer.normalizeUpdateIdentityListWithoutCredentials(dto);
		checkNameDuplications(result.stream().map(ni -> ni.systemName()).toList(), origin);

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	public List<NormalizedIdentityMgmtRequestDTO> validateAndNormalizeUpdateIdentityListPhase2(
			final IAuthenticationMethod authenticationMethod,
			final List<NormalizedIdentityMgmtRequestDTO> identities,
			final String origin) {
		logger.debug("validateAndNormalizeUpdateIdentityListPhase2 started...");

		validateUpdateIdentityListPhase2(authenticationMethod, identities, origin);

		try {
			return normalizer.normalizeCredentials(authenticationMethod, identities);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> validateAndNormalizeRemoveIdentities(final List<String> names, final String origin) {
		logger.debug("validateAndNormalizeRemoveIdentities started...");

		validateRemoveIdentities(names, origin);

		return normalizer.normalizeRemoveIdentifiableSystemNames(names);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void validateIdentity(final IAuthenticationMethod method, final IdentityMgmtRequestDTO identity, final String origin) {
		logger.debug("validateIdentity started...");

		validateIdentityWithoutCredentials(identity, origin);

		try {
			method.validator().validateCredentials(identity.credentials());
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateIdentityWithoutCredentials(final IdentityMgmtRequestDTO identity, final String origin) {
		logger.debug("validateIdentityWithoutCredentials started...");

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