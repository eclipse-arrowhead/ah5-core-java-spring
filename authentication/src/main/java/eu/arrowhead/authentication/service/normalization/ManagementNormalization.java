package eu.arrowhead.authentication.service.normalization;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authentication.method.AuthenticationMethods;
import eu.arrowhead.authentication.method.IAuthenticationMethod;
import eu.arrowhead.authentication.service.dto.NormalizedIdentityMgmtRequestDTO;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.validation.name.NameNormalizer;
import eu.arrowhead.dto.IdentityMgmtRequestDTO;
import eu.arrowhead.dto.enums.AuthenticationMethod;

@Service
public class ManagementNormalization {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private NameNormalizer nameNormalizer;

	@Autowired
	private AuthenticationMethods methods;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public List<NormalizedIdentityMgmtRequestDTO> normalizeIdentityList(final List<IdentityMgmtRequestDTO> identities) {
		logger.debug("normalizeIdentityList started...");
		Assert.isTrue(!Utilities.isEmpty(identities), "Identities list is missing or empty");
		Assert.isTrue(!Utilities.containsNull(identities), "Identities list contains null element");

		final List<NormalizedIdentityMgmtRequestDTO> result = new ArrayList<>(identities.size());
		for (final IdentityMgmtRequestDTO identity : identities) {
			final String normalizedSystem = nameNormalizer.normalize(identity.systemName());

			final AuthenticationMethod normalizedAuthenticationMethod = authenticationMethodFromString(identity.authenticationMethod());
			final IAuthenticationMethod method = methods.method(normalizedAuthenticationMethod);
			Assert.notNull(method, "Authentication method is unsupported");

			final Map<String, String> normalizedCredentials = method.normalizer().normalizeCredentials(identity.credentials());

			final boolean normalizedSysop = identity.sysop() == null ? false : identity.sysop().booleanValue();

			result.add(new NormalizedIdentityMgmtRequestDTO(
					normalizedSystem,
					normalizedAuthenticationMethod,
					normalizedCredentials,
					normalizedSysop));
		}

		return result;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private AuthenticationMethod authenticationMethodFromString(final String str) {
		logger.debug("authenticationMethodFromString started...");
		Assert.isTrue(!Utilities.isEmpty(str), "Authentication method is missing or empty");

		final String authMethodName = str.trim().toUpperCase();
		Assert.isTrue(Utilities.isEnumValue(authMethodName, AuthenticationMethod.class), "Authentication method is invalid");

		return AuthenticationMethod.valueOf(authMethodName);
	}
}