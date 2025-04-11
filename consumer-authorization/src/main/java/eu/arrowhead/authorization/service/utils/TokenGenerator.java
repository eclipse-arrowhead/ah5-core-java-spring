package eu.arrowhead.authorization.service.utils;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Set;
import java.util.stream.Collectors;

import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.keys.HmacKey;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authorization.AuthorizationSystemInfo;
import eu.arrowhead.common.Utilities;

@Service
public class TokenGenerator {
	
	//=================================================================================================
	// members
	
	@Autowired
	private AuthorizationSystemInfo sysInfo;
	
	
	private final SecureRandom secureRandom = new SecureRandom();
	
	private final Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();
	
	private static final String JWT_PAYLOAD_KEY_SERVICE_INSTANCE_IDENTIFIER = "sii";
	private static final String JWT_PAYLOAD_KEY_SERVICE_OPERATION_LIST = "sol";
	
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
	public String generateJsonWebToken(final String algorithm, final String secret, final String serviceInstanceIdentifier, final Set<String> operations, final Integer durationSec) throws JoseException {
		Assert.isTrue(!Utilities.isEmpty(algorithm), "algorithm is empty");
		Assert.isTrue(!Utilities.isEmpty(secret), "secret is empty");
		Assert.isTrue(!Utilities.isEmpty(serviceInstanceIdentifier), "serviceInstanceIdentifier is empty");
		
		// Payload
		final JwtClaims claims = new JwtClaims();
		claims.setGeneratedJwtId();
		claims.setIssuer(sysInfo.getSystemName());
		claims.setIssuedAtToNow();
		claims.setNotBeforeMinutesInThePast(1);
		if (durationSec != null) {
			claims.setExpirationTimeMinutesInTheFuture(durationSec.floatValue() / 60);
		}
		claims.setStringClaim(JWT_PAYLOAD_KEY_SERVICE_INSTANCE_IDENTIFIER, serviceInstanceIdentifier);
		claims.setStringClaim(JWT_PAYLOAD_KEY_SERVICE_OPERATION_LIST, Utilities.isEmpty(operations) ? "" : operations.stream().collect(Collectors.joining(",")));
		
		// Signature & Header
		final JsonWebSignature jws = new JsonWebSignature();
		jws.setHeader("typ", "JWT");
        jws.setPayload(claims.toJson());
        if (algorithm.equalsIgnoreCase(AlgorithmIdentifiers.HMAC_SHA256)) {
        	jws.setKey(new HmacKey(secret.getBytes(StandardCharsets.UTF_8)));
        	jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.HMAC_SHA256);			
		} else {
			throw new JoseException("Unsupported algorithm: " + algorithm);
		}
        
        
        return jws.getCompactSerialization();
	}
}
