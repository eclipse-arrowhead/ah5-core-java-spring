package eu.arrowhead.authentication;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.http.filter.authentication.AuthenticationPolicy;
import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.model.InterfaceModel;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.model.SystemModel;
import eu.arrowhead.common.security.SecurityUtilities;

@Component
public class AuthenticationSystemInfo extends SystemInfo {

	//=================================================================================================
	// members

	private SystemModel systemModel;

	@Value(AuthenticationConstants.$AUTHENTICATION_SECRET_KEY)
	private String authenticationSecretKey;

	private String specIdentityToken;

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
		final ServiceModel identity = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_IDENTITY)
				.version(AuthenticationConstants.VERSION_IDENTITY)
				.serviceInterface(getHttpServiceInterfaceForIdentity())
				.build();

		final ServiceModel identityManagement = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_IDENTITY_MANAGEMENT)
				.version(AuthenticationConstants.VERSION_IDENTITY_MANAGEMENT)
				.serviceInterface(getHttpServiceInterfaceForIdentityManagement())
				.build();

		final ServiceModel generalManagement = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_GENERAL_MANAGEMENT)
				.version(AuthenticationConstants.VERSION_GENERAL_MANAGEMENT)
				.serviceInterface(getHttpServiceInterfaceForGeneralManagement())
				.build();

		return List.of(identity, identityManagement, generalManagement);
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public AuthenticationPolicy getAuthenticationPolicy() {
		return AuthenticationPolicy.INTERNAL;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String getIdentityToken() {
		return specIdentityToken;
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

		try {
			specIdentityToken = SecurityUtilities.hashWithSecretKey(getSystemName(), authenticationSecretKey);
		} catch (final InvalidKeyException | NoSuchAlgorithmException ex) {
			throw new InternalServerError(ex.getMessage(), "System initialization", ex);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForIdentity() {
		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;

		final HttpOperationModel login = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(AuthenticationConstants.HTTP_API_OP_LOGIN_PATH)
				.build();
		final HttpOperationModel logout = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(AuthenticationConstants.HTTP_API_OP_LOGOUT_PATH)
				.build();
		final HttpOperationModel change = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(AuthenticationConstants.HTTP_API_OP_CHANGE_PATH)
				.build();
		final HttpOperationModel verify = new HttpOperationModel.Builder()
				.method(HttpMethod.GET.name())
				.path(AuthenticationConstants.HTTP_API_OP_VERIFY_BASE_PATH)
				.build();

		return new HttpInterfaceModel.Builder(templateName, getDomainAddress(), getServerPort())
				.basePath(AuthenticationConstants.HTTP_API_IDENTITY_PATH)
				.operation(Constants.SERVICE_OP_IDENTITY_LOGIN, login)
				.operation(Constants.SERVICE_OP_IDENTITY_LOGOUT, logout)
				.operation(Constants.SERVICE_OP_IDENTITY_CHANGE, change)
				.operation(Constants.SERVICE_OP_IDENTITY_VERIFY, verify)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForIdentityManagement() {
		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;

		final HttpOperationModel create = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(AuthenticationConstants.HTTP_API_OP_IDENTITIES_PATH)
				.build();
		final HttpOperationModel update = new HttpOperationModel.Builder()
				.method(HttpMethod.PUT.name())
				.path(AuthenticationConstants.HTTP_API_OP_IDENTITIES_PATH)
				.build();
		final HttpOperationModel remove = new HttpOperationModel.Builder()
				.method(HttpMethod.DELETE.name())
				.path(AuthenticationConstants.HTTP_API_OP_IDENTITIES_PATH)
				.build();
		final HttpOperationModel query = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(AuthenticationConstants.HTTP_API_OP_IDENTITIES_QUERY_PATH)
				.build();
		final HttpOperationModel close = new HttpOperationModel.Builder()
				.method(HttpMethod.DELETE.name())
				.path(AuthenticationConstants.HTTP_API_OP_SESSION_PATH)
				.build();
		final HttpOperationModel querySessions = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(AuthenticationConstants.HTTP_API_OP_SESSION_PATH)
				.build();

		return new HttpInterfaceModel.Builder(templateName, getDomainAddress(), getServerPort())
				.basePath(AuthenticationConstants.HTTP_API_MANAGEMENT_PATH)
				.operation(Constants.SERVICE_OP_IDENTITY_MGMT_CREATE, create)
				.operation(Constants.SERVICE_OP_IDENTITY_MGMT_UPDATE, update)
				.operation(Constants.SERVICE_OP_IDENTITY_MGMT_REMOVE, remove)
				.operation(Constants.SERVICE_OP_IDENTITY_MGMT_QUERY, query)
				.operation(Constants.SERVICE_OP_IDENTITY_MGMT_SESSION_CLOSE, close)
				.operation(Constants.SERVICE_OP_IDENTITY_MGMT_SESSION_QUERY, querySessions)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForGeneralManagement() {
		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;

		final HttpOperationModel log = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(Constants.HTTP_API_OP_LOGS_PATH)
				.build();
		final HttpOperationModel config = new HttpOperationModel.Builder()
				.method(HttpMethod.GET.name())
				.path(Constants.HTTP_API_OP_GET_CONFIG_PATH)
				.build();

		return new HttpInterfaceModel.Builder(templateName, getDomainAddress(), getServerPort())
				.basePath(AuthenticationConstants.HTTP_API_GENERAL_MANAGEMENT_PATH)
				.operation(Constants.SERVICE_OP_GET_LOG, log)
				.operation(Constants.SERVICE_OP_GET_CONFIG, config)
				.build();
	}
}