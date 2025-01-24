package eu.arrowhead.authentication;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.http.filter.authentication.AuthenticationPolicy;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.model.SystemModel;

@Component
public class AuthenticationSystemInfo extends SystemInfo {

	//=================================================================================================
	// members

	private SystemModel systemModel;

	@Value(AuthenticationConstants.$AUTHENTICATION_SECRET_KEY)
	private String authenticationSecretKey;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public String getSystemName() {
		return AuthenticationConstants.SYSTEM_NAME;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public SystemModel getSystemModel() {
		if (systemModel == null) {
			systemModel = new SystemModel.Builder()
					.address(getAddress())
					.version(Constants.AH_FRAMEWORK_VERSION)
					.build();
		}

		return systemModel;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public List<ServiceModel> getServices() {
		// TODO implement
		return List.of();
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public AuthenticationPolicy getAuthenticationPolicy() {
		return AuthenticationPolicy.INTERNAL;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String getIdentityToken() {
		return authenticationSecretKey;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void customInit() {
		if (Utilities.isEmpty(authenticationSecretKey)) {
			throw new InvalidParameterException("'authenticationSecretKey' is missing or empty");
		}

		if (this.getAuthenticationPolicy() != AuthenticationPolicy.INTERNAL) {
			throw new InvalidParameterException("'authenticationPolicy' is invalid");
		}
	}
}