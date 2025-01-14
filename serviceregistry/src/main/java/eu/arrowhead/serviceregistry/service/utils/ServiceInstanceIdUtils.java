package eu.arrowhead.serviceregistry.service.utils;

import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;

public final class ServiceInstanceIdUtils {

	//=================================================================================================
	// members

	private static final String delimiter = "::";
	private static final int parts = 3;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public static String calculateInstanceId(final String systemName, final String serviceDefinitionName, final String version) {
		Assert.isTrue(!Utilities.isEmpty(systemName), "systemName is empty");
		Assert.isTrue(!Utilities.isEmpty(serviceDefinitionName), "serviceDefinitionName is empty");
		Assert.isTrue(!Utilities.isEmpty(version), "version is empty");

		return systemName + delimiter + serviceDefinitionName + delimiter + version;
	}

	//-------------------------------------------------------------------------------------------------
	public static String retrieveSystemNameFromInstaceId(final String instanceId) {
		Assert.isTrue(!Utilities.isEmpty(instanceId), "instanceId is empty");

		final String[] split = instanceId.split(delimiter);
		Assert.isTrue(split.length == parts, "Invalid instanceId");
		return split[0];
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private ServiceInstanceIdUtils() {
		throw new UnsupportedOperationException();
	}
}