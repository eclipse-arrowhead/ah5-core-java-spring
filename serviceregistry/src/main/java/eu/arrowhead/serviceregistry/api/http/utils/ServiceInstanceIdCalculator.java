package eu.arrowhead.serviceregistry.api.http.utils;

import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;

public final class ServiceInstanceIdCalculator {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public static String calculate(final String systemName, final String serviceDefinitionName, final String version) {
		Assert.isTrue(!Utilities.isEmpty(systemName), "systemName is empty");
		Assert.isTrue(!Utilities.isEmpty(serviceDefinitionName), "serviceDefinitionName is empty");
		Assert.isTrue(!Utilities.isEmpty(version), "version is empty");

		return systemName + "-" + serviceDefinitionName + "-" + version;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private ServiceInstanceIdCalculator() {
		throw new UnsupportedOperationException();
	}

}
