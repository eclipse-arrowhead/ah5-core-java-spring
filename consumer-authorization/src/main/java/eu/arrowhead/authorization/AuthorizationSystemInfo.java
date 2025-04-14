package eu.arrowhead.authorization;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.model.InterfaceModel;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.model.SystemModel;
import eu.arrowhead.common.mqtt.model.MqttInterfaceModel;

@Component(Constants.BEAN_NAME_SYSTEM_INFO)
public class AuthorizationSystemInfo extends SystemInfo {

	//=================================================================================================
	// members

	private SystemModel systemModel;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public String getSystemName() {
		return AuthorizationConstants.SYSTEM_NAME;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public SystemModel getSystemModel() {
		if (systemModel == null) {
			SystemModel.Builder builder = new SystemModel.Builder()
					.address(getAddress())
					.version(Constants.AH_FRAMEWORK_VERSION);

			if (this.isSslEnabled()) {
				builder = builder.metadata(Constants.METADATA_KEY_X509_PUBLIC_KEY, getPublicKey());
			}

			systemModel = builder.build();
		}

		return systemModel;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public List<ServiceModel> getServices() {
		final ServiceModel authorization = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_IDENTITY)
				.version(AuthorizationConstants.VERSION_AUTHORIZATION)
				.metadata(Constants.METADATA_KEY_UNRESTRICTED_DISCOVERY, true)
				.serviceInterface(getHttpServiceInterfaceForAuthorization())
				.serviceInterface(getMqttServiceInterfaceForAuthorization())
				.build();

		final ServiceModel generalManagement = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_GENERAL_MANAGEMENT)
				.version(AuthorizationConstants.VERSION_GENERAL_MANAGEMENT)
				.serviceInterface(getHttpServiceInterfaceForGeneralManagement())
				.serviceInterface(getMqttServiceInterfaceForGeneralManagement())
				.build();

		// TODO extend this
		return List.of(authorization, generalManagement);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected PublicConfigurationKeysAndDefaults getPublicConfigurationKeysAndDefaults() {
		// TODO extend with authorization specific configuration
		return new PublicConfigurationKeysAndDefaults(
				Set.of(Constants.SERVER_ADDRESS,
						Constants.SERVER_PORT,
						Constants.MQTT_API_ENABLED,
						Constants.DOMAIN_NAME,
						Constants.AUTHENTICATION_POLICY,
						Constants.ENABLE_MANAGEMENT_FILTER,
						Constants.MANAGEMENT_POLICY,
						Constants.ENABLE_BLACKLIST_FILTER,
						Constants.FORCE_BLACKLIST_FILTER,
						Constants.MAX_PAGE_SIZE),
				AuthorizationDefaults.class);
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForAuthorization() {
		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;

		final HttpOperationModel grant = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(AuthorizationConstants.HTTP_API_OP_GRANT_PATH)
				.build();
		final HttpOperationModel revoke = new HttpOperationModel.Builder()
				.method(HttpMethod.DELETE.name())
				.path(AuthorizationConstants.HTTP_API_OP_REVOKE_PATH)
				.build();
		final HttpOperationModel lookup = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(AuthorizationConstants.HTTP_API_OP_LOOKUP_PATH)
				.build();
		final HttpOperationModel verify = new HttpOperationModel.Builder()
				.method(HttpMethod.GET.name())
				.path(AuthorizationConstants.HTTP_API_OP_VERIFY_PATH)
				.build();

		return new HttpInterfaceModel.Builder(templateName, getDomainAddress(), getServerPort())
				.basePath(AuthorizationConstants.HTTP_API_AUTHORIZATION_PATH)
				.operation(Constants.SERVICE_OP_GRANT, grant)
				.operation(Constants.SERVICE_OP_REVOKE, revoke)
				.operation(Constants.SERVICE_OP_LOOKUP, lookup)
				.operation(Constants.SERVICE_OP_VERIFY, verify)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getMqttServiceInterfaceForAuthorization() {
		if (!isMqttApiEnabled()) {
			return null;
		}

		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_MQTTS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_MQTT_INTERFACE_TEMPLATE_NAME;
		return new MqttInterfaceModel.Builder(templateName, getMqttBrokerAddress(), getMqttBrokerPort())
				.baseTopic(AuthorizationConstants.MQTT_API_AUTHORIZATION_BASE_TOPIC)
				.operations(Set.of(Constants.SERVICE_OP_GRANT,
						Constants.SERVICE_OP_REVOKE,
						Constants.SERVICE_OP_LOOKUP,
						Constants.SERVICE_OP_VERIFY))
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
				.basePath(AuthorizationConstants.HTTP_API_GENERAL_MANAGEMENT_PATH)
				.operation(Constants.SERVICE_OP_GET_LOG, log)
				.operation(Constants.SERVICE_OP_GET_CONFIG, config)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getMqttServiceInterfaceForGeneralManagement() {
		if (!isMqttApiEnabled()) {
			return null;
		}

		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_MQTTS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_MQTT_INTERFACE_TEMPLATE_NAME;
		return new MqttInterfaceModel.Builder(templateName, getMqttBrokerAddress(), getMqttBrokerPort())
				.baseTopic(AuthorizationConstants.MQTT_API_GENERAL_MANAGEMENT_BASE_TOPIC)
				.operations(Set.of(Constants.SERVICE_OP_GET_LOG, Constants.SERVICE_OP_GET_CONFIG))
				.build();
	}
}
