package eu.arrowhead.serviceregistry;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.http.filter.authentication.AuthenticationPolicy;
import eu.arrowhead.common.Utilities;
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
		// TODO Auto-generated method stub
		return List.of();
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
}
