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

	@Value(DynamicServiceOrchestrationConstants.$ENABLE_INTERCLOUD_WD)
	private boolean enableIntercloud;

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
		final ServiceModel orchestration = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_ORCHESTRATION)
				.version(DynamicServiceOrchestrationConstants.VERSION_ORCHESTRATION)
				.metadata(DynamicServiceOrchestrationConstants.METADATA_KEY_ORCHESTRATION_STRATEGY, DynamicServiceOrchestrationConstants.METADATA_VALUE_ORCHESTRATION_STRATEGY)
				.serviceInterface(getHttpServiceInterfaceForOrchestration())
				.build();

		// TODO add the rest
		return List.of(orchestration);
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isAuthorizationEnabled() {
		return enableAuthorization;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isInterCloudEnabled() {
		return enableIntercloud;
	}

	//=================================================================================================
	// assistant methods

	// HTTP Interfaces

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForOrchestration() {
		return getHttpServiceInterfaceForAnOrchestrationService(DynamicServiceOrchestrationConstants.HTTP_API_ORCHESTRATION_PATH);
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
				.operation(Constants.SERVICE_OP_ORCHESTRATION_PUSH_SUBSCRIBE, pushSubscribe)
				.operation(Constants.SERVICE_OP_ORCHESTRATION_PUSH_UNSUBSCRIBE, pushUnsubscribe)
				.build();
	}

}
