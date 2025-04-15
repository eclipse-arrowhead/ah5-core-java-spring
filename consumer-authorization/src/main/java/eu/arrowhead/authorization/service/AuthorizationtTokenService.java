package eu.arrowhead.authorization.service;

import java.security.PrivateKey;

import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authorization.service.model.JWTPayload;
import eu.arrowhead.authorization.service.utils.TokenGenerator;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.util.ServiceInstanceIdParts;
import eu.arrowhead.common.service.util.ServiceInstanceIdUtils;
import eu.arrowhead.dto.AuthorizationTokenGenerationRequestDTO;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;

@Service
public class AuthorizationtTokenService {

	//=================================================================================================
	// members

	@Autowired
	private TokenGenerator tokenGenerator;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public String generate(final String requesterSystem, final AuthorizationTokenGenerationRequestDTO dto, final String origin) {
		final AuthorizationTokenGenerationRequestDTO normalized = dto; // TODO validate and normalize		

		final ServiceInterfacePolicy tokenType = ServiceInterfacePolicy.valueOf(normalized.tokenType());
		String token = null;

		if (tokenType == ServiceInterfacePolicy.USAGE_LIMITED_TOKEN_AUTH) {
			final Integer limit = null; // TODO from config
			token = tokenGenerator.generateSimpleToken(32); // TODO length to come from config
			// TODO encode and save into DB
			return token;
		}

		final Integer duration = null; // TODO config

		if (tokenType == ServiceInterfacePolicy.TIME_LIMITED_TOKEN_AUTH) {
			token = tokenGenerator.generateSimpleToken(32); // TODO length to come from config
			// TODO encode and save into DB
			return token;
		}

		final PrivateKey privateKey = null; // TODO
		final String encriptionKey = null; // TODO from DB and decode it
		final JWTPayload jwtPayload = getJWTPayload(normalized.serviceInstanceId(), normalized.serviceOperation(), requesterSystem);

		try {
			if (tokenType == ServiceInterfacePolicy.RSASHA256_AES128GCM_JSON_WEB_TOKEN_AUTH) {
				token = tokenGenerator.generateJsonWebToken(AlgorithmIdentifiers.RSA_USING_SHA512, privateKey, ContentEncryptionAlgorithmIdentifiers.AES_128_GCM, encriptionKey, duration, jwtPayload);
				// TODO encode and save into DB
			}
			if (tokenType == ServiceInterfacePolicy.RSASHA256_AES256GCM_JSON_WEB_TOKEN_AUTH) {
				token = tokenGenerator.generateJsonWebToken(AlgorithmIdentifiers.RSA_USING_SHA512, privateKey, ContentEncryptionAlgorithmIdentifiers.AES_256_GCM, encriptionKey, duration, jwtPayload);
				// TODO encode and save into DB
			}
		} catch (final JoseException ex) {
			// TODO: handle exception
		}
		
		Assert.isTrue(!Utilities.isEmpty(token), "Unhandled token type: " + tokenType);

		return token;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private JWTPayload getJWTPayload(final String serviceInstanceId, final String serviceOperation, final String requesterSystem) {
		final ServiceInstanceIdParts serviceInstanceIdParts = ServiceInstanceIdUtils.breakDownInstanceId(serviceInstanceId);
		return new JWTPayload(serviceInstanceIdParts.systemName(), requesterSystem, "LOCAL", AuthorizationTargetType.SERVICE_DEF, serviceInstanceIdParts.serviceDefinition(), serviceOperation);
	}
}
