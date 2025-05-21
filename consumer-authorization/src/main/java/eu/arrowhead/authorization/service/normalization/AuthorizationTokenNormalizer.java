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
import eu.arrowhead.dto.AuthorizationTokenQueryRequestDTO;
import eu.arrowhead.dto.DTODefaults;
import eu.arrowhead.dto.enums.AuthorizationTargetType;

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
				nameNormalizer.normalize(dto.provider()),
				Utilities.isEmpty(dto.targetType()) ? AuthorizationTargetType.SERVICE_DEF.name() : dto.targetType().toUpperCase().trim(),
				nameNormalizer.normalize(dto.target()),
				Utilities.isEmpty(dto.scope()) ? Defaults.DEFAULT_AUTHORIZATION_SCOPE : nameNormalizer.normalize(dto.scope()));
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
								Utilities.isEmpty(item.targetType()) ? AuthorizationTargetType.SERVICE_DEF.name() : item.targetType().toUpperCase().trim(),
								Utilities.isEmpty(item.consumerCloud()) ? DTODefaults.DEFAULT_CLOUD : nameNormalizer.normalize(item.consumerCloud()),
								nameNormalizer.normalize(item.consumer()),
								nameNormalizer.normalize(item.provider()),
								nameNormalizer.normalize(item.target()),
								Utilities.isEmpty(item.scope()) ? Defaults.DEFAULT_AUTHORIZATION_SCOPE : nameNormalizer.normalize(item.scope()),
								Utilities.isEmpty(item.expireAt()) ? null : item.expireAt().trim(),
								item.usageLimit()))
						.toList());
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationTokenQueryRequestDTO normalizeAuthorizationTokenQueryRequestDTO(final AuthorizationTokenQueryRequestDTO dto) {
		logger.debug("normalizeAuthorizationTokenQueryRequestDTO started...");
		Assert.notNull(dto, "AuthorizationTokenQueryRequestDTO is null.");

		return new AuthorizationTokenQueryRequestDTO(
				dto.pagination(),
				Utilities.isEmpty(dto.requester()) ? null : nameNormalizer.normalize(dto.requester()),
				Utilities.isEmpty(dto.tokenType()) ? null : dto.tokenType().trim().toUpperCase(),
				Utilities.isEmpty(dto.consumerCloud()) ? null : nameNormalizer.normalize(dto.consumerCloud()),
				Utilities.isEmpty(dto.consumer()) ? null : nameNormalizer.normalize(dto.consumer()),
				Utilities.isEmpty(dto.provider()) ? null : nameNormalizer.normalize(dto.provider()),
				Utilities.isEmpty(dto.targetType()) ? null : dto.targetType().trim().toUpperCase(),
				Utilities.isEmpty(dto.target()) ? null : nameNormalizer.normalize(dto.target()));
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
