package eu.arrowhead.authorization.service.normalization;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.validation.name.NameNormalizer;
import eu.arrowhead.dto.AuthorizationEncryptionKeyRegistrationRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationRequestDTO;

@Service
public class AuthorizationTokenNormalization {

	//=================================================================================================
	// members

	@Autowired
	private NameNormalizer nameNormalizer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public String normalizeSystemName(final String name) {
		logger.debug("normalizeSystemName started...");
		Assert.isTrue(!Utilities.isEmpty(name), "System name is empty.");

		return nameNormalizer.normalize(name);
	}

	//-------------------------------------------------------------------------------------------------
	public String normalizeToken(final String token) {
		logger.debug("normalizeToken started...");
		Assert.isTrue(!Utilities.isEmpty(token), "Token is empty.");

		return token.trim();
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationTokenGenerationRequestDTO normalizeAuthorizationTokenGenerationRequestDTO(final AuthorizationTokenGenerationRequestDTO dto) {
		logger.debug("normalizeAuthorizationTokenGenerationRequestDTO started...");
		Assert.notNull(dto, "AuthorizationTokenGenerationRequestDTO is null.");

		return new AuthorizationTokenGenerationRequestDTO(
				dto.tokenType().toUpperCase().trim(),
				nameNormalizer.normalize(dto.serviceInstanceId()),
				Utilities.isEmpty(dto.serviceOperation()) ? Defaults.DEFAULT_AUTHORIZATION_SCOPE : nameNormalizer.normalize(dto.serviceOperation()));
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationEncryptionKeyRegistrationRequestDTO normalizeAuthorizationEncryptionKeyRegistrationRequestDTO(final AuthorizationEncryptionKeyRegistrationRequestDTO dto) {
		logger.debug("normalizeAuthorizationEncryptionKeyRegistrationRequestDTO started...");
		Assert.notNull(dto, "AuthorizationEncryptionKeyRegistrationRequestDTO is null.");

		return new AuthorizationEncryptionKeyRegistrationRequestDTO(
				dto.key(),
				dto.algorithm().trim());
	}
}
