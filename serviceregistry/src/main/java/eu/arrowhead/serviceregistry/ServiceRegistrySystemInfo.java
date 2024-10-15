package eu.arrowhead.serviceregistry;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.http.filter.authentication.AuthenticationPolicy;
import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.mqtt.model.MqttInterfaceModel;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.model.InterfaceModel;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.model.SystemModel;
import eu.arrowhead.serviceregistry.service.ServiceDiscoveryInterfacePolicy;
import eu.arrowhead.serviceregistry.service.ServiceDiscoveryPolicy;

@Component
public class ServiceRegistrySystemInfo extends SystemInfo {

	//=================================================================================================
	// members

	@Value(ServiceRegistryConstants.$DISCOVERY_VERBOSE_WD)
	private boolean discoveryVerbose;

	@Value(ServiceRegistryConstants.SERVICE_DISCOVERY_DIRECT_ACCESS)
	private List<String> serviceDiscoveryDirectAccess;

	@Value(ServiceRegistryConstants.$SERVICE_DISCOVERY_POLICY_WD)
	private ServiceDiscoveryPolicy serviceDiscoveryPolicy;

	@Value(ServiceRegistryConstants.$SERVICE_DISCOVERY_INTERFACE_POLICY_WD)
	private ServiceDiscoveryInterfacePolicy serviceDiscoveryInterfacePolicy;

	private SystemModel systemModel;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public String getSystemName() {
		return ServiceRegistryConstants.SYSTEM_NAME;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public List<ServiceModel> getServices() {
		final ServiceModel deviceDiscovery = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_DEVICE_DISCOVERY)
				.version(ServiceRegistryConstants.VERSION_DEVICE_DISCOVERY)
				.metadata(ServiceRegistryConstants.METADATA_KEY_UNRESTRICTED_DISCOVERY, true)
				.serviceInterface(getHttpServiceInterfaceForDeviceDiscovery())
				.serviceInterface(getMqttServiceInterfaceForDeviceDiscovery())
				.build();

		final ServiceModel systemDiscovery = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_SYSTEM_DISCOVERY)
				.version(ServiceRegistryConstants.VERSION_SYSTEM_DISCOVERY)
				.metadata(ServiceRegistryConstants.METADATA_KEY_UNRESTRICTED_DISCOVERY, true)
				.serviceInterface(getHttpServiceInterfaceForSystemDiscovery())
				.serviceInterface(getMqttServiceInterfaceForSystemDiscovery())
				.build();

		final ServiceModel serviceDiscovery = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_SERVICE_DISCOVERY)
				.version(ServiceRegistryConstants.VERSION_SERVICE_DISCOVERY)
				.metadata(ServiceRegistryConstants.METADATA_KEY_UNRESTRICTED_DISCOVERY, true)
				.serviceInterface(getHttpServiceInterfaceForServiceDiscovery())
				.serviceInterface(getMqttServiceInterfaceForServiceDiscovery())
				.build();

		final ServiceModel serviceRegistryManagement = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_SERVICE_REGISTRY_MANAGEMENT)
				.version(ServiceRegistryConstants.VERSION_SERVICE_REGISTRY_MANAGEMENT)
				.serviceInterface(getHttpServiceInterfaceForServiceRegistryManagement())
				.build(); // TODO add MQTT mgmt interfaces

		// TODO: add monitor service when it is specified and implemented

		return List.of(deviceDiscovery, systemDiscovery, serviceDiscovery, serviceRegistryManagement);
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public SystemModel getSystemModel() {
		if (systemModel == null) {
			SystemModel.Builder builder = new SystemModel.Builder()
					.address(getAddressModel())
					.version(Constants.AH_FRAMEWORK_VERSION);

			if (AuthenticationPolicy.CERTIFICATE == this.getAuthenticationPolicy()) {
				builder = builder.metadata(Constants.METADATA_KEY_X509_PUBLIC_KEY, getPublicKey());
			}

			systemModel = builder.build();
		}

		return systemModel;
	}

	public boolean isDiscoveryVerbose() {
		return this.discoveryVerbose;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean hasClientDirectAccess(final String systemName) {
		Assert.isTrue(!Utilities.isEmpty(systemName), "systemName is empty");
		return serviceDiscoveryDirectAccess.contains(systemName);
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceDiscoveryPolicy getServiceDiscoveryPolicy() {
		return this.serviceDiscoveryPolicy;
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceDiscoveryInterfacePolicy getServiceDiscoveryInterfacePolicy() {
		return this.serviceDiscoveryInterfacePolicy;
	}

	//=================================================================================================
	// assistant methods

	// HTTP Interfaces

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForDeviceDiscovery() {
		return getHttpServiceInterfaceForADiscoveryService(ServiceRegistryConstants.HTTP_API_DEVICE_DISCOVERY_PATH);
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForSystemDiscovery() {
		return getHttpServiceInterfaceForADiscoveryService(ServiceRegistryConstants.HTTP_API_SYSTEM_DISCOVERY_PATH);
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForServiceDiscovery() {
		return getHttpServiceInterfaceForADiscoveryService(ServiceRegistryConstants.HTTP_API_SERVICE_DISCOVERY_PATH);
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForADiscoveryService(final String basePath) {
		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;

		final HttpOperationModel register = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_REGISTER_PATH)
				.build();
		final HttpOperationModel lookup = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_LOOKUP_PATH)
				.build();
		final HttpOperationModel revoke = new HttpOperationModel.Builder()
				.method(HttpMethod.DELETE.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_REVOKE_PATH)
				.build();

		return new HttpInterfaceModel.Builder(templateName, getDomainAddress(), getServerPort())
				.basePath(basePath)
				.operation(Constants.SERVICE_OP_REGISTER, register)
				.operation(Constants.SERVICE_OP_LOOKUP, lookup)
				.operation(Constants.SERVICE_OP_REVOKE, revoke)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForServiceRegistryManagement() {
		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;

		final HttpOperationModel log = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(Constants.HTTP_API_OP_LOGS_PATH)
				.build();
		final HttpOperationModel config = new HttpOperationModel.Builder()
				.method(HttpMethod.GET.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_GET_CONFIG_PATH)
				.build();

		final HttpOperationModel deviceQuery = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_DEVICE_QUERY_PATH)
				.build();
		final HttpOperationModel deviceCreate = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_DEVICE_PATH)
				.build();
		final HttpOperationModel deviceUpdate = new HttpOperationModel.Builder()
				.method(HttpMethod.PUT.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_DEVICE_PATH)
				.build();
		final HttpOperationModel deviceRemove = new HttpOperationModel.Builder()
				.method(HttpMethod.DELETE.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_DEVICE_PATH)
				.build();

		final HttpOperationModel systemQuery = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_SYSTEM_QUERY_PATH)
				.build();
		final HttpOperationModel systemCreate = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_SYSTEM_PATH)
				.build();
		final HttpOperationModel systemUpdate = new HttpOperationModel.Builder()
				.method(HttpMethod.PUT.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_SYSTEM_PATH)
				.build();
		final HttpOperationModel systemRemove = new HttpOperationModel.Builder()
				.method(HttpMethod.DELETE.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_SYSTEM_PATH)
				.build();

		final HttpOperationModel serviceDefQuery = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_SERVICE_DEFINITION_QUERY_PATH)
				.build();
		final HttpOperationModel serviceDefCreate = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_SERVICE_DEFINITION_PATH)
				.build();
		final HttpOperationModel serviceDefRemove = new HttpOperationModel.Builder()
				.method(HttpMethod.DELETE.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_SERVICE_DEFINITION_PATH)
				.build();

		final HttpOperationModel serviceQuery = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_SERVICE_INSTANCE_QUERY_PATH)
				.build();
		final HttpOperationModel serviceCreate = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_SERVICE_INSTANCE_PATH)
				.build();
		final HttpOperationModel serviceUpdate = new HttpOperationModel.Builder()
				.method(HttpMethod.PUT.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_SERVICE_INSTANCE_PATH)
				.build();
		final HttpOperationModel serviceRemove = new HttpOperationModel.Builder()
				.method(HttpMethod.DELETE.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_SERVICE_INSTANCE_PATH)
				.build();

		final HttpOperationModel intfQuery = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_INTERFACE_TEMPLATE_QUERY_PATH)
				.build();
		final HttpOperationModel intfCreate = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_INTERFACE_TEMPLATE_PATH)
				.build();
		final HttpOperationModel intfRemove = new HttpOperationModel.Builder()
				.method(HttpMethod.DELETE.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_INTERFACE_TEMPLATE_PATH)
				.build();

		return new HttpInterfaceModel.Builder(templateName, getDomainAddress(), getServerPort())
				.basePath(ServiceRegistryConstants.HTTP_API_MANAGEMENT_PATH)
				.operation(Constants.SERVICE_OP_GET_LOG, log)
				.operation(Constants.SERVICE_OP_GET_CONFIG, config)
				.operation(Constants.SERVICE_OP_DEVICE_QUERY, deviceQuery)
				.operation(Constants.SERVICE_OP_DEVICE_CREATE, deviceCreate)
				.operation(Constants.SERVICE_OP_DEVICE_UPDATE, deviceUpdate)
				.operation(Constants.SERVICE_OP_DEVICE_REMOVE, deviceRemove)
				.operation(Constants.SERVICE_OP_SYSTEM_QUERY, systemQuery)
				.operation(Constants.SERVICE_OP_SYSTEM_CREATE, systemCreate)
				.operation(Constants.SERVICE_OP_SYSTEM_UPDATE, systemUpdate)
				.operation(Constants.SERVICE_OP_SYSTEM_REMOVE, systemRemove)
				.operation(Constants.SERVICE_OP_SERVICE_DEF_QUERY, serviceDefQuery)
				.operation(Constants.SERVICE_OP_SERVICE_DEF_CREATE, serviceDefCreate)
				.operation(Constants.SERVICE_OP_SERVICE_DEF_REMOVE, serviceDefRemove)
				.operation(Constants.SERVICE_OP_SERVICE_QUERY, serviceQuery)
				.operation(Constants.SERVICE_OP_SERVICE_CREATE, serviceCreate)
				.operation(Constants.SERVICE_OP_SERVICE_UPDATE, serviceUpdate)
				.operation(Constants.SERVICE_OP_SERVICE_REMOVE, serviceRemove)
				.operation(Constants.SERVICE_OP_INTERFACE_TEMPLATE_QUERY, intfQuery)
				.operation(Constants.SERVICE_OP_INTERFACE_TEMPLATE_CREATE, intfCreate)
				.operation(Constants.SERVICE_OP_INTERFACE_TEMPLATE_REMOVE, intfRemove)
				.build();
	}

	// MQTT Interfaces

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getMqttServiceInterfaceForDeviceDiscovery() {
		if (!isMqttApiEnabled()) {
			return null;
		}
		return getMqttServiceInterfaceForADiscoveryService(ServiceRegistryConstants.MQTT_API_DEVICE_DISCOVERY_TOPIC);
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getMqttServiceInterfaceForSystemDiscovery() {
		if (!isMqttApiEnabled()) {
			return null;
		}
		return getMqttServiceInterfaceForADiscoveryService(ServiceRegistryConstants.MQTT_API_SYSTEM_DISCOVERY_TOPIC);
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getMqttServiceInterfaceForServiceDiscovery() {
		if (!isMqttApiEnabled()) {
			return null;
		}
		return getMqttServiceInterfaceForADiscoveryService(ServiceRegistryConstants.MQTT_API_SERVICE_DISCOVERY_TOPIC);
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getMqttServiceInterfaceForADiscoveryService(final String topic) {
		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_MQTTS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_MQTT_INTERFACE_TEMPLATE_NAME;

		return new MqttInterfaceModel.Builder(templateName, getMqttBrokerAddress(), getMqttBrokerPort())
				.topic(topic)
				.operation(Constants.SERVICE_OP_REGISTER, Constants.SERVICE_OP_REGISTER)
				.operation(Constants.SERVICE_OP_LOOKUP, Constants.SERVICE_OP_LOOKUP)
				.operation(Constants.SERVICE_OP_REVOKE, Constants.SERVICE_OP_REVOKE)
				.build();
	}
}