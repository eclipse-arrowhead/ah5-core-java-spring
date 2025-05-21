package eu.arrowhead.authorization.service.validation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authorization.AuthorizationConstants;
import eu.arrowhead.authorization.service.normalization.AuthorizationTokenNormalizer;
import eu.arrowhead.authorization.service.utils.SecretCryptographer;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.NameValidator;
import eu.arrowhead.dto.AuthorizationEncryptionKeyRegistrationRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationRequestDTO;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;

@Service
public class AuthorizationTokenValidation {
	
	//-------------------------------------------------------------------------------------------------
	// members
	
	@Autowired
	private NameValidator nameValidator;
	
	@Autowired
	private AuthorizationTokenNormalizer normalizer;
	
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

		try {
			nameValidator.validateName(systemName);
		} catch (final InvalidParameterException ex) {
			if (Utilities.isEmpty(ex.getOrigin())) {
				throw new InvalidParameterException(ex.getMessage(), origin);
			}

			throw ex;
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	public void validateToken(final String token, final String origin) {
		logger.debug("validateToken started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		if (Utilities.isEmpty(token)) {
			throw new InvalidParameterException("Token is empty", origin);
		}		
	}
	
	//-------------------------------------------------------------------------------------------------
	public void validateGenerateRequest(final AuthorizationTokenGenerationRequestDTO dto, final String origin) {
		logger.debug("validateGenerateRequest started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");
		
		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}
		
		if (Utilities.isEmpty(dto.tokenType())) {
			throw new InvalidParameterException("Token type is missing", origin);
		}
		
		if (Utilities.isEmpty(dto.serviceInstanceId())) {
			throw new InvalidParameterException("Service instance ID is missing", origin);
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	public void validateRegisterEncryptionKeyRequest(final AuthorizationEncryptionKeyRegistrationRequestDTO dto, final String origin) {
		logger.debug("validateRegisterEncryptionKey started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");
		
		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}
		
		if (Utilities.isEmpty(dto.algorithm())) {
			throw new InvalidParameterException("Algorithm is empty.", origin);
		}
		
		if (Utilities.isEmpty(dto.key())) {
			throw new InvalidParameterException("Key is empty.", origin);
		}
	}
	
	// VALIDATION AND NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public String validateAndNormalizeSystemName(final String systemName, final String origin) {
		logger.debug("validateAndNormalizeSystemName started...");

		validateSystemName(systemName, origin);
		return normalizer.normalizeSystemName(systemName);
	}
	
	//-------------------------------------------------------------------------------------------------
	public String validateAndNormalizeToken(final String token, final String origin) {
		logger.debug("validateAndNormalizeToken started...");
		
		validateToken(token, origin);
		
		return normalizer.normalizeToken(token);
	}
	
	//-------------------------------------------------------------------------------------------------
	public AuthorizationTokenGenerationRequestDTO validateAndNormalizeGenerateRequest(final AuthorizationTokenGenerationRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeGenerateRequest started...");
		
		validateGenerateRequest(dto, origin);
		
		final AuthorizationTokenGenerationRequestDTO normalized = normalizer.normalizeAuthorizationTokenGenerationRequestDTO(dto);
		
		try {
			if (!Utilities.isEnumValue(normalized.tokenType(), ServiceInterfacePolicy.class)
					|| !normalized.tokenType().endsWith(AuthorizationConstants.TOKEN_TYPE_AUTH_SUFFIX)) {
				throw new InvalidParameterException("Token type is invalid", origin);
			}
			
			// TODO validate service instance id with the future validator
			
			if (!normalized.serviceOperation().equals(Defaults.DEFAULT_AUTHORIZATION_SCOPE)) {
				nameValidator.validateName(normalized.serviceOperation());
			}
			
		} catch (final InvalidParameterException ex) {
			if (Utilities.isEmpty(ex.getOrigin())) {
				throw new InvalidParameterException(ex.getMessage(), origin);
			}

			throw ex;
		}
		
		return normalized;
	}
	
	//-------------------------------------------------------------------------------------------------
	public AuthorizationEncryptionKeyRegistrationRequestDTO normalizeAndValidateRegisterEncryptionKeyRequest(final AuthorizationEncryptionKeyRegistrationRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeGenerateRequest started...");
		
		validateRegisterEncryptionKeyRequest(dto, origin);
		final AuthorizationEncryptionKeyRegistrationRequestDTO normalized = normalizer.normalizeAuthorizationEncryptionKeyRegistrationRequestDTO(dto);
		
		if (!(normalized.algorithm().equalsIgnoreCase(SecretCryptographer.HMAC_ALGORITHM) || normalized.algorithm().equalsIgnoreCase(SecretCryptographer.AES_CBC_ALOGRITHM))) {
			throw new InvalidParameterException("Unsupported algorithm", origin);
		}		
		
		if (normalized.algorithm().equalsIgnoreCase(SecretCryptographer.AES_CBC_ALOGRITHM)
				&& normalized.key().getBytes().length != SecretCryptographer.AES_KEY_SIZE) {
			throw new InvalidParameterException("Key size is not " + SecretCryptographer.AES_KEY_SIZE + " byte long");
		}
		
		return normalized;
	}
}
