package eu.arrowhead.authorization.service.utils;

import java.security.PrivateKey;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Base64.Encoder;

import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authorization.AuthorizationSystemInfo;
import eu.arrowhead.authorization.service.model.SelfContainedTokenPayload;
import eu.arrowhead.common.Utilities;

@Service
public class TokenGenerator {
	
	//=================================================================================================
	// members
	
	@Autowired
	private AuthorizationSystemInfo sysInfo;
	
	
	private final SecureRandom secureRandom = new SecureRandom();
	
	private final Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();
	
	private static final String JWT_PAYLOAD_KEY_PROVIDER_SYSTEM_NAME = "psn";
	private static final String JWT_PAYLOAD_KEY_CONSUMER_SYSTEM_NAME = "csn";
	private static final String JWT_PAYLOAD_KEY_CONSUMER_CLOUD_NAME = "ccn";
	private static final String JWT_PAYLOAD_KEY_TARGET_TYPE = "tat";
	private static final String JWT_PAYLOAD_KEY_TARGET_NAME = "tan";
	private static final String JWT_PAYLOAD_KEY_SCOPE = "sco";
	
	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public String generateSimpleToken(final int byteLength) {
		Assert.isTrue(byteLength >=  16, "Minimum byte length for token is 16");
		
		final byte[] randomBytes = new byte[byteLength];
        secureRandom.nextBytes(randomBytes);
        return base64Encoder.encodeToString(randomBytes);
	}
	
	//-------------------------------------------------------------------------------------------------
	public String generateJsonWebToken(final String signAlgorithm, final PrivateKey privateKey, final Integer durationSec, final SelfContainedTokenPayload payload) throws JoseException {
		Assert.isTrue(!Utilities.isEmpty(signAlgorithm), "signAlgorithm is empty");
		Assert.notNull(privateKey, "privateKey is null");
		Assert.notNull(payload, "JWT payload is null");
		Assert.isTrue(!Utilities.isEmpty(payload.provider()), "provider is empty");
		Assert.isTrue(!Utilities.isEmpty(payload.provider()), "consumer is empty");
		Assert.isTrue(!Utilities.isEmpty(payload.cloud()), "cloud is empty");
		Assert.notNull(payload.targetType(), "JWT payload is null");
		Assert.isTrue(!Utilities.isEmpty(payload.target()), "target is empty");
		Assert.isTrue(!Utilities.isEmpty(payload.scope()), "scope is empty");
		
		final JwtClaims claims = createJWTClaims(durationSec, payload);		
		final String jwt = signJWT(signAlgorithm, privateKey, claims);        
        return jwt;
        
	}
	
	//=================================================================================================
	// assistant methods
	
	//-------------------------------------------------------------------------------------------------
	private JwtClaims createJWTClaims(final Integer durationSec, final SelfContainedTokenPayload payload) {
		final JwtClaims claims = new JwtClaims();
		claims.setGeneratedJwtId();
		claims.setIssuer(sysInfo.getSystemName());
		claims.setIssuedAtToNow();
		claims.setNotBeforeMinutesInThePast(1);
		if (durationSec != null) {
			claims.setExpirationTimeMinutesInTheFuture(durationSec.floatValue() / 60);
		}
		claims.setStringClaim(JWT_PAYLOAD_KEY_PROVIDER_SYSTEM_NAME, payload.provider());
		claims.setStringClaim(JWT_PAYLOAD_KEY_CONSUMER_SYSTEM_NAME, payload.consumer());
		claims.setStringClaim(JWT_PAYLOAD_KEY_CONSUMER_CLOUD_NAME, payload.cloud());
		claims.setStringClaim(JWT_PAYLOAD_KEY_TARGET_TYPE, payload.targetType().name());
		claims.setStringClaim(JWT_PAYLOAD_KEY_TARGET_NAME, payload.target());
		claims.setStringClaim(JWT_PAYLOAD_KEY_SCOPE, payload.scope());
		return claims;
	}
	
	//-------------------------------------------------------------------------------------------------
	private String signJWT(final String signAlgorithm, final PrivateKey privateKey, final JwtClaims claims) throws JoseException {
		final JsonWebSignature jws = new JsonWebSignature();
		jws.setHeader("typ", "JWT");
        jws.setPayload(claims.toJson());
        jws.setKey(privateKey);
        
        if (signAlgorithm.equalsIgnoreCase(AlgorithmIdentifiers.RSA_USING_SHA512)
        		|| signAlgorithm.equalsIgnoreCase(AlgorithmIdentifiers.RSA_USING_SHA256)) {
        	jws.setAlgorithmHeaderValue(signAlgorithm);		
			
		} else {
			throw new JoseException("Unsupported sign algorithm: " + signAlgorithm);
		}        
        
        return jws.getCompactSerialization();
	}
}
