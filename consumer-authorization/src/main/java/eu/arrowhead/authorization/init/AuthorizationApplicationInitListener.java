package eu.arrowhead.authorization.init;

import javax.naming.ConfigurationException;

import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import eu.arrowhead.authorization.AuthorizationSystemInfo;
import eu.arrowhead.authorization.service.utils.SecretCryptographer;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.init.ApplicationInitListener;

@Component
public class AuthorizationApplicationInitListener extends ApplicationInitListener {

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void customInit(final ContextRefreshedEvent event) throws InterruptedException, ConfigurationException {
		final AuthorizationSystemInfo authSysInfo = (AuthorizationSystemInfo) sysInfo;
		
		if (Utilities.isEmpty(authSysInfo.getSecretCryptographerKey())) {
			throw new ConfigurationException("secret.cryptographer.key property is empty.");
		}
		
		final byte[] cryptoKeyBytes = authSysInfo.getSecretCryptographerKey().getBytes();
		if (cryptoKeyBytes.length < SecretCryptographer.AES_KEY_MIN_SIZE) {
			throw new ConfigurationException("secret.cryptographer.key value size must be minimum " + SecretCryptographer.AES_KEY_MIN_SIZE + " bytes long");
		}
	}	
}
