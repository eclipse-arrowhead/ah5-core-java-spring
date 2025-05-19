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
import eu.arrowhead.dto.AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO;
import eu.arrowhead.dto.AuthorizationMgmtEncryptionKeyRegistrationRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationMgmtListRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationMgmtRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationRequestDTO;
import eu.arrowhead.dto.DTODefaults;

@Service
public class AuthorizationTokenNormalizer {

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

	//-------------------------------------------------------------------------------------------------
	public AuthorizationTokenGenerationMgmtListRequestDTO normalizeAuthorizationTokenGenerationMgmtListRequestDTO(final AuthorizationTokenGenerationMgmtListRequestDTO dto) {
		logger.debug("normalizeAuthorizationTokenGenerationMgmtListRequestDTO started...");
		Assert.notNull(dto, "AuthorizationTokenGenerationMgmtListRequestDTO is null.");
		Assert.notNull(dto.list(), "AuthorizationTokenGenerationMgmtListRequestDTO.list is null.");

		return new AuthorizationTokenGenerationMgmtListRequestDTO(
				dto.list().stream()
						.map((item) -> new AuthorizationTokenGenerationMgmtRequestDTO(
								item.tokenType().trim().toUpperCase(),
								Utilities.isEmpty(item.consumerCloud()) ? DTODefaults.DEFAULT_CLOUD : nameNormalizer.normalize(item.consumerCloud()),
								nameNormalizer.normalize(item.consumer()),
								nameNormalizer.normalize(item.provider()),
								nameNormalizer.normalize(item.serviceDefinition()),
								Utilities.isEmpty(item.serviceOperation()) ? Defaults.DEFAULT_AUTHORIZATION_SCOPE : nameNormalizer.normalize(item.serviceOperation()),
								Utilities.isEmpty(item.expireAt()) ? null : item.expireAt().trim(),
								item.usageLimit()))
						.toList());
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO normalizeAuthorizationMgmtEncryptionKeyRegistrationListRequestDTO(final AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO dto) {
		logger.debug("normalizeAuthorizationMgmtEncryptionKeyRegistrationListRequestDTO started...");
		Assert.notNull(dto, "AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO is null.");
		Assert.notNull(dto.list(), "AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO.list is null.");

		return new AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO(
				dto.list().stream()
						.map((item) -> new AuthorizationMgmtEncryptionKeyRegistrationRequestDTO(nameNormalizer.normalize(item.systemName()), item.key(), item.algorithm().trim().toUpperCase()))
						.toList());
	}
}
