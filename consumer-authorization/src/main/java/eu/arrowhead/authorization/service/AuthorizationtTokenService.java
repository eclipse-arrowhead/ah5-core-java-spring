package eu.arrowhead.authorization.service;

import java.security.PrivateKey;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authorization.AuthorizationSystemInfo;
import eu.arrowhead.authorization.jpa.service.UsageLimitedTokenDbService;
import eu.arrowhead.authorization.service.model.JWTPayload;
import eu.arrowhead.authorization.service.utils.SecretCryptographer;
import eu.arrowhead.authorization.service.utils.TokenGenerator;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.service.util.ServiceInstanceIdParts;
import eu.arrowhead.common.service.util.ServiceInstanceIdUtils;
import eu.arrowhead.dto.AuthorizationTokenGenerationRequestDTO;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import eu.arrowhead.dto.enums.AuthorizationTokenType;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;

@Service
public class AuthorizationtTokenService {

	//=================================================================================================
	// members

	@Autowired
	private AuthorizationSystemInfo sysInfo;
	
	@Autowired
	private TokenGenerator tokenGenerator;

	@Autowired
	private SecretCryptographer secretCryptographer;

	@Autowired
	private UsageLimitedTokenDbService usageLimitedTokenDbService;
	
	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Pair<String, Boolean> generate(final String requesterSystem, final AuthorizationTokenGenerationRequestDTO dto, final String origin) {
		final AuthorizationTokenGenerationRequestDTO normalized = dto; // TODO validate and normalize
		final ServiceInstanceIdParts serviceInstanceIdParts = ServiceInstanceIdUtils.breakDownInstanceId(normalized.serviceInstanceId());

		try {
			final ServiceInterfacePolicy tokenType = ServiceInterfacePolicy.valueOf(normalized.tokenType());
			Pair<String, Boolean> result = null;
			String token = null;
			Pair<String, String> encryptedToSave = null;

			if (tokenType == ServiceInterfacePolicy.USAGE_LIMITED_TOKEN_AUTH) {
				token = tokenGenerator.generateSimpleToken(sysInfo.getSimpleTokenByteSize());
				encryptedToSave = secretCryptographer.encrypt(token);
				usageLimitedTokenDbService.save(
						AuthorizationTokenType.fromServiceInterfacePolicy(tokenType),
						encryptedToSave.getLeft(),
						encryptedToSave.getRight(),
						requesterSystem,
						"LOCAL",
						requesterSystem,
						serviceInstanceIdParts.systemName(),
						serviceInstanceIdParts.serviceDefinition(),
						normalized.serviceOperation(),
						sysInfo.getSimpleTokenUsageLimit());
				return result;
			}

			final Integer duration = null; // TODO config

			if (tokenType == ServiceInterfacePolicy.TIME_LIMITED_TOKEN_AUTH) {
				token = tokenGenerator.generateSimpleToken(sysInfo.getSimpleTokenByteSize());
				// TODO encode and save into DB
				return result;
			}

			final PrivateKey privateKey = null; // TODO
			final String encriptionKey = null; // TODO from DB and decode it
			final JWTPayload jwtPayload = new JWTPayload(serviceInstanceIdParts.systemName(), requesterSystem, "LOCAL", AuthorizationTargetType.SERVICE_DEF, serviceInstanceIdParts.serviceDefinition(), normalized.serviceOperation());

			if (tokenType == ServiceInterfacePolicy.RSASHA256_AES128GCM_JSON_WEB_TOKEN_AUTH) {
				token = tokenGenerator.generateJsonWebToken(AlgorithmIdentifiers.RSA_USING_SHA512, privateKey, ContentEncryptionAlgorithmIdentifiers.AES_128_GCM, encriptionKey, duration, jwtPayload);
				// TODO encode and save into DB
			}
			if (tokenType == ServiceInterfacePolicy.RSASHA256_AES256GCM_JSON_WEB_TOKEN_AUTH) {
				token = tokenGenerator.generateJsonWebToken(AlgorithmIdentifiers.RSA_USING_SHA512, privateKey, ContentEncryptionAlgorithmIdentifiers.AES_256_GCM, encriptionKey, duration, jwtPayload);
				// TODO encode and save into DB
			}
			
			Assert.isTrue(!Utilities.isEmpty(token), "Unhandled token type: " + tokenType);
			
			return result;

		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Token generation failed!", origin);
		}

	}
}
