package eu.arrowhead.serviceregistry;

import eu.arrowhead.common.Defaults;
import eu.arrowhead.serviceregistry.service.ServiceDiscoveryInterfacePolicy;
import eu.arrowhead.serviceregistry.service.ServiceDiscoveryPolicy;

public final class ServiceRegistryDefaults extends Defaults {

	//=================================================================================================
	// members

	public static final String DISCOVERY_VERBOSE_DEFAULT = "false";
	public static final String SERVICE_DISCOVERY_POLICY_DEFAULT = ServiceDiscoveryPolicy.RESTRICTED_VALUE;
	public static final String SERVICE_DISCOVERY_DIRECT_ACCESS_DEFAULT = "\"\"";
	public static final String SERVICE_DISCOVERY_INTERFACE_POLICY_DEFAULT = ServiceDiscoveryInterfacePolicy.RESTRICTED_VALUE;

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private ServiceRegistryDefaults() {
		throw new UnsupportedOperationException();
	}

}
