package eu.arrowhead.authorization.service.utils;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;

@Service
public class SecretCryptographer {

	//=================================================================================================
	// members

	public static final String AES_ALOGRITHM = "AES/CBC/PKCS5Padding";
	private static final String AES_KEY_ALGORITHM = "AES";
	private static final int AES_KEY_SIZE = 16; // 128 bits
	
	public static final String HMAC_ALGORITHM = "HmacSHA256";

	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public Pair<String, String> encryptAESCBCPKCS5P(final String plainSecret, final String key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		return encryptAESCBCPKCS5P(plainSecret, key,  generateIV());
	}

	//-------------------------------------------------------------------------------------------------
	public Pair<String, String> encryptAESCBCPKCS5P(final String plainSecret, final String key, final byte[] iv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		Assert.isTrue(!Utilities.isEmpty(plainSecret), "plainSecret is empty");
		Assert.isTrue(!Utilities.isEmpty(key), "key is empty");

		final String encryptedBase64 = encryptAESCBCPKCS5P(plainSecret, key, getIvParameterSpec(iv));
		final String ivBase64 = Base64.getEncoder().encodeToString(iv);
		return Pair.of(encryptedBase64, ivBase64);
	}
	
	//-------------------------------------------------------------------------------------------------
	public Pair<String, String> encryptAESCBCPKCS5P(final String plainSecret, final String key, final String ivBase64) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		Assert.isTrue(!Utilities.isEmpty(plainSecret), "plainSecret is empty");
		Assert.isTrue(!Utilities.isEmpty(key), "key is empty");

		final String encryptedBase64 = encryptAESCBCPKCS5P(plainSecret, key, getIvParameterSpec(ivBase64));
		return Pair.of(encryptedBase64, ivBase64);
	}

	//-------------------------------------------------------------------------------------------------
	public String decryptAESCBCPKCS5P(final String encryptedSecretBase64, final String ivBase64, final String key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		Assert.isTrue(!Utilities.isEmpty(encryptedSecretBase64), "encryptedSecretBase64 is empty");
		Assert.isTrue(!Utilities.isEmpty(ivBase64), "ivBase64 is empty");
		Assert.isTrue(!Utilities.isEmpty(key), "key is empty");
		
		final SecretKeySpec keySpec = getKeyFromStringAESCBCPKCS5P(key);
		byte[] iv = Base64.getDecoder().decode(ivBase64);
		final IvParameterSpec ivSpec = new IvParameterSpec(iv);
		final Cipher cipher = Cipher.getInstance(AES_ALOGRITHM);
		
	    cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
	    byte[] encryptedBytes = Base64.getDecoder().decode(encryptedSecretBase64);
	    byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
	    return new String(decryptedBytes);
	}

	//-------------------------------------------------------------------------------------------------
	public String encryptHMACSHA256(final String plainSecret, final String key) throws NoSuchAlgorithmException, InvalidKeyException {
		Assert.isTrue(!Utilities.isEmpty(plainSecret), "plainSecret is empty");
		Assert.isTrue(!Utilities.isEmpty(key), "key is empty");
		
		Mac sha256_HMAC = Mac.getInstance(HMAC_ALGORITHM);
		SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), HMAC_ALGORITHM);
        sha256_HMAC.init(keySpec);
        
        byte[] hash = sha256_HMAC.doFinal(plainSecret.getBytes());
        return Base64.getEncoder().encodeToString(hash);
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
	private IvParameterSpec getIvParameterSpec(byte[] iv) {
		return new IvParameterSpec(iv);
	}
	
	//-------------------------------------------------------------------------------------------------
	private IvParameterSpec getIvParameterSpec(String ivBase64) {
		return new IvParameterSpec(Base64.getDecoder().decode(ivBase64));
	}

	//-------------------------------------------------------------------------------------------------
	private SecretKeySpec getKeyFromStringAESCBCPKCS5P(String key) {
		byte[] keyBytes = key.getBytes();
		Assert.isTrue(keyBytes.length != AES_KEY_SIZE, "key size is not " + AES_KEY_SIZE + "byte long");
		return new SecretKeySpec(keyBytes, AES_KEY_ALGORITHM);
	}
	
	//-------------------------------------------------------------------------------------------------
	private String encryptAESCBCPKCS5P(final String plainSecret, final String key, final IvParameterSpec ivSpec) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		Assert.isTrue(!Utilities.isEmpty(plainSecret), "plainSecret is empty");
		Assert.isTrue(!Utilities.isEmpty(key), "key is empty");
		Assert.notNull(ivSpec, "ivSpec is null");

		final SecretKeySpec keySpec = getKeyFromStringAESCBCPKCS5P(key);
		final Cipher cipher = Cipher.getInstance(AES_ALOGRITHM);

		cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
		byte[] encrypted = cipher.doFinal(plainSecret.getBytes());
		return Base64.getEncoder().encodeToString(encrypted);
	}
}
