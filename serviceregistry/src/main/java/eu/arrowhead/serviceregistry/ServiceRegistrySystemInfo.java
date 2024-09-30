package eu.arrowhead.serviceregistry;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.serviceregistry.service.ServiceDiscoveryInterfacePolicy;

@Component
public class ServiceRegistrySystemInfo extends SystemInfo {

	//=================================================================================================
	// members

	@Value(ServiceRegistryConstants.$SERVICE_DISCOVERY_INTERFACE_POLICY_WD)
	private ServiceDiscoveryInterfacePolicy serviceDisciveryInterfacePolicy;

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
		return null;
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceDiscoveryInterfacePolicy getServiceDiscoveryInterfacePolicy() {
		return this.serviceDisciveryInterfacePolicy;
	}
}
