package eu.arrowhead.authorization.service.utils;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authorization.AuthorizationSystemInfo;
import eu.arrowhead.common.Utilities;

@Service
public class SecretCryptographer {

	//=================================================================================================
	// members

	private static final String ALGORITHM = "AES";
	private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
	private static final int KEY_SIZE = 16; // 128 bits

	@Autowired
	private AuthorizationSystemInfo sysInfo;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Pair<String, String> encrypt(final String plainSecret) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		Assert.isTrue(!Utilities.isEmpty(plainSecret), "plainSecret is empty");

		final SecretKeySpec keySpec = getKeyFromString(sysInfo.getSecretCryptographerKey());
		byte[] iv = generateIV();
		final IvParameterSpec ivSpec = new IvParameterSpec(iv);
		final Cipher cipher = Cipher.getInstance(TRANSFORMATION);

		cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
		byte[] encrypted = cipher.doFinal(plainSecret.getBytes());
		return Pair.of(Base64.getEncoder().encodeToString(encrypted), Base64.getEncoder().encodeToString(iv));
	}

	//-------------------------------------------------------------------------------------------------
	public String decrypt(final String encryptedSecretBase64, final String ivBase64) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		Assert.isTrue(!Utilities.isEmpty(encryptedSecretBase64), "encryptedSecretBase64 is empty");
		Assert.isTrue(!Utilities.isEmpty(ivBase64), "ivBase64 is empty");
		
		final SecretKeySpec keySpec = getKeyFromString(sysInfo.getSecretCryptographerKey());
		byte[] iv = Base64.getDecoder().decode(ivBase64);
		final IvParameterSpec ivSpec = new IvParameterSpec(iv);
		final Cipher cipher = Cipher.getInstance(TRANSFORMATION);
		
	    cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
	    byte[] encryptedBytes = Base64.getDecoder().decode(encryptedSecretBase64);
	    byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
	    return new String(decryptedBytes);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private byte[] generateIV() {
		byte[] iv = new byte[16];
		new SecureRandom().nextBytes(iv);
		return iv;
	}

	//-------------------------------------------------------------------------------------------------
	private SecretKeySpec getKeyFromString(String key) {
		byte[] keyBytes = key.getBytes();
		Assert.isTrue(keyBytes.length != KEY_SIZE, "key size is not " + KEY_SIZE + "byte long");
		return new SecretKeySpec(keyBytes, ALGORITHM);
	}

}
