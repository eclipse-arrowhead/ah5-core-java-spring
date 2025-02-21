package eu.arrowhead.serviceorchestration;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.http.filter.authentication.AuthenticationPolicy;
import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.model.InterfaceModel;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.model.SystemModel;

@Component
public class DynamicServiceOrchestrationSystemInfo extends SystemInfo {

	//=================================================================================================
	// members

	@Value(DynamicServiceOrchestrationConstants.$ENABLE_AUTHORIZATION_WD)
	private boolean enableAuthorization;

	@Value(DynamicServiceOrchestrationConstants.$ENABLE_TRANSLATION_WD)
	private boolean enableTranslation;

	@Value(DynamicServiceOrchestrationConstants.$ENABLE_QOS_WD)
	private boolean enableQoS;

	@Value(DynamicServiceOrchestrationConstants.$ENABLE_INTERCLOUD_WD)
	private boolean enableIntercloud;

	@Value(DynamicServiceOrchestrationConstants.$ORCHESTRATION_HISTORY_MAX_AGE_WD)
	private int orchestrationHistoryMaxAge;

	@Value(DynamicServiceOrchestrationConstants.$PUSH_ORCHESTRATION_MAX_THREAD_WD)
	private int pushOrchestrationMaxThread;

	private SystemModel systemModel;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public String getSystemName() {
		return DynamicServiceOrchestrationConstants.SYSTEM_NAME;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public SystemModel getSystemModel() {
		if (systemModel == null) {
			SystemModel.Builder builder = new SystemModel.Builder()
					.address(getAddress())
					.version(Constants.AH_FRAMEWORK_VERSION);

			if (AuthenticationPolicy.CERTIFICATE == this.getAuthenticationPolicy()) {
				builder = builder.metadata(Constants.METADATA_KEY_X509_PUBLIC_KEY, getPublicKey());
			}

			systemModel = builder.build();
		}

		return systemModel;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public List<ServiceModel> getServices() {
		final ServiceModel monitor = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_MONITOR)
				.version(DynamicServiceOrchestrationConstants.VERSION_MONITOR)
				.serviceInterface(getHttpServiceInterfaceForMonitorService())
				.build();

		final ServiceModel generalManagement = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_GENERAL_MANAGEMENT)
				.version(DynamicServiceOrchestrationConstants.VERSION_GENERAL_MANAGEMENT)
				.serviceInterface(getHttpServiceInterfaceForGeneralManagement())
				.build();

		final ServiceModel orchestration = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_ORCHESTRATION)
				.version(DynamicServiceOrchestrationConstants.VERSION_ORCHESTRATION)
				.metadata(DynamicServiceOrchestrationConstants.METADATA_KEY_ORCHESTRATION_STRATEGY, DynamicServiceOrchestrationConstants.METADATA_VALUE_ORCHESTRATION_STRATEGY)
				.serviceInterface(getHttpServiceInterfaceForOrchestration())
				.build();

		final ServiceModel orchestrationPushManagement = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_ORCHESTRATION_PUSH_MANAGEMENT)
				.version(DynamicServiceOrchestrationConstants.VERSION_ORCHESTRATION_PUSH_MANAGEMENT)
				.serviceInterface(getHttpServiceInterfaceForOrchestrationPushManagement())
				.build();

		final ServiceModel orchestrationLockManagement = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_ORCHESTRATION_LOCK_MANAGEMENT)
				.version(DynamicServiceOrchestrationConstants.VERSION_ORCHESTRATION_PUSH_MANAGEMENT)
				.serviceInterface(getHttpServiceInterfaceForOrchestrationLockManagement())
				.build();

		final ServiceModel orchestrationHistoryManagement = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_ORCHESTRATION_HISTORY_MANAGEMENT)
				.version(DynamicServiceOrchestrationConstants.VERSION_ORCHESTRATION_HISTORY_MANAGEMENT)
				.serviceInterface(getHttpServiceInterfaceForOrchestrationHistoryManagement())
				.build();

		return List.of(monitor, generalManagement, orchestration, orchestrationPushManagement, orchestrationLockManagement, orchestrationHistoryManagement);
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isAuthorizationEnabled() {
		return enableAuthorization;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isTranslationEnabled() {
		return enableTranslation;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isQoSEnabled() {
		return enableQoS;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isInterCloudEnabled() {
		return enableIntercloud;
	}

	//-------------------------------------------------------------------------------------------------
	public int getOrchestrationHistoryMaxAge() {
		return orchestrationHistoryMaxAge;
	}

	//-------------------------------------------------------------------------------------------------
	public int getPushOrchestrationMaxThread() {
		return pushOrchestrationMaxThread;
	}

	//=================================================================================================
	// assistant methods

	// HTTP Interfaces

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForMonitorService() {
		return getHttpServiceInterfaceForAMonitorService(DynamicServiceOrchestrationConstants.HTTP_API_MONITOR_PATH);
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForGeneralManagement() {
		return getHttpServiceInterfaceForAGeneralManagementService(DynamicServiceOrchestrationConstants.HTTP_API_GENERAL_MANAGEMENT_PATH);
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForOrchestration() {
		return getHttpServiceInterfaceForAnOrchestrationService(DynamicServiceOrchestrationConstants.HTTP_API_ORCHESTRATION_PATH);
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForOrchestrationPushManagement() {
		return getHttpServiceInterfaceForAnOrchestrationPushManagementService(DynamicServiceOrchestrationConstants.HTTP_API_ORCHESTRATION_PUSH_MANAGEMENT_PATH);
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForOrchestrationLockManagement() {
		return getHttpServiceInterfaceForAnOrchestrationLockManagementService(DynamicServiceOrchestrationConstants.HTTP_API_ORCHESTRATION_LOCK_MANAGEMENT_PATH);
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForOrchestrationHistoryManagement() {
		return getHttpServiceInterfaceForAnOrchestrationHistoryManagementService(DynamicServiceOrchestrationConstants.HTTP_API_ORCHESTRATION_HISTORY_MANAGEMENT_PATH);
	}

	// HTTP Interface Operations

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForAMonitorService(final String basePath) {
		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;

		final HttpOperationModel echo = new HttpOperationModel.Builder()
				.method(HttpMethod.GET.name())
				.path(DynamicServiceOrchestrationConstants.HTTP_API_OP_ECHO)
				.build();

		return new HttpInterfaceModel.Builder(templateName, getDomainAddress(), getServerPort())
				.basePath(DynamicServiceOrchestrationConstants.HTTP_API_MONITOR_PATH)
				.operation(Constants.SERVICE_OP_ECHO, echo)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForAGeneralManagementService(final String basePath) {
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
				.basePath(DynamicServiceOrchestrationConstants.HTTP_API_GENERAL_MANAGEMENT_PATH)
				.operation(Constants.SERVICE_OP_GET_LOG, log)
				.operation(Constants.SERVICE_OP_GET_CONFIG, config)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForAnOrchestrationService(final String basePath) {
		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;

		final HttpOperationModel pull = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(DynamicServiceOrchestrationConstants.HTTP_API_OP_PULL_PATH)
				.build();
		final HttpOperationModel pushSubscribe = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(DynamicServiceOrchestrationConstants.HTTP_API_OP_PUSH_SUBSCRIBE_PATH)
				.build();
		final HttpOperationModel pushUnsubscribe = new HttpOperationModel.Builder()
				.method(HttpMethod.DELETE.name())
				.path(DynamicServiceOrchestrationConstants.HTTP_API_OP_PUSH_UNSUBSCRIBE_PATH)
				.build();

		return new HttpInterfaceModel.Builder(templateName, getDomainAddress(), getServerPort())
				.basePath(basePath)
				.operation(Constants.SERVICE_OP_ORCHESTRATION_PULL, pull)
				.operation(Constants.SERVICE_OP_ORCHESTRATION_SUBSCRIBE, pushSubscribe)
				.operation(Constants.SERVICE_OP_ORCHESTRATION_UNSUBSCRIBE, pushUnsubscribe)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForAnOrchestrationPushManagementService(final String basePath) {
		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;

		final HttpOperationModel pushSubscribe = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(DynamicServiceOrchestrationConstants.HTTP_API_OP_PUSH_SUBSCRIBE_PATH)
				.build();
		final HttpOperationModel pushUnsubscribe = new HttpOperationModel.Builder()
				.method(HttpMethod.DELETE.name())
				.path(DynamicServiceOrchestrationConstants.HTTP_API_OP_PUSH_UNSUBSCRIBE_PATH)
				.build();
		final HttpOperationModel pushTrigger = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(DynamicServiceOrchestrationConstants.HTTP_API_OP_PUSH_TRIGGER_PATH)
				.build();
		final HttpOperationModel pushQuery = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(DynamicServiceOrchestrationConstants.HTTP_API_OP_QUERY_PATH)
				.build();

		return new HttpInterfaceModel.Builder(templateName, getDomainAddress(), getServerPort())
				.basePath(basePath)
				.operation(Constants.SERVICE_OP_ORCHESTRATION_SUBSCRIBE, pushSubscribe)
				.operation(Constants.SERVICE_OP_ORCHESTRATION_UNSUBSCRIBE, pushUnsubscribe)
				.operation(Constants.SERVICE_OP_ORCHESTRATION_TRIGGER, pushTrigger)
				.operation(Constants.SERVICE_OP_ORCHESTRATION_QUERY, pushQuery)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForAnOrchestrationLockManagementService(final String basePath) {
		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;

		final HttpOperationModel lockQuery = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(DynamicServiceOrchestrationConstants.HTTP_API_OP_QUERY_PATH)
				.build();
		final HttpOperationModel lockRemove = new HttpOperationModel.Builder()
				.method(HttpMethod.DELETE.name())
				.path(DynamicServiceOrchestrationConstants.HTTP_API_OP_REMOVE_LOCK_PATH)
				.build();
		final HttpOperationModel lockCreate = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(DynamicServiceOrchestrationConstants.HTTP_API_OP_CREATE_PATH)
				.build();

		return new HttpInterfaceModel.Builder(templateName, getDomainAddress(), getServerPort())
				.basePath(basePath)
				.operation(Constants.SERVICE_OP_ORCHESTRATION_QUERY, lockQuery)
				.operation(Constants.SERVICE_OP_ORCHESTRATION_REMOVE, lockRemove)
				.operation(Constants.SERVICE_OP_ORCHESTRATION_CREATE, lockCreate)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForAnOrchestrationHistoryManagementService(final String basePath) {
		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;

		final HttpOperationModel historyQuery = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(DynamicServiceOrchestrationConstants.HTTP_API_OP_QUERY_PATH)
				.build();

		return new HttpInterfaceModel.Builder(templateName, getDomainAddress(), getServerPort())
				.basePath(basePath)
				.operation(Constants.SERVICE_OP_ORCHESTRATION_QUERY, historyQuery)
				.build();
	}
}
