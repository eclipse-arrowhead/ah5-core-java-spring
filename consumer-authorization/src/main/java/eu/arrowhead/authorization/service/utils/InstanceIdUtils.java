package eu.arrowhead.authorization.service.utils;

import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.enums.AuthorizationLevel;
import eu.arrowhead.dto.enums.AuthorizationTargetType;

public final class InstanceIdUtils {

	//=================================================================================================
	// members

	private static final String DELIMITER = "::";
	private static final int PARTS = 5;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public static String calculateInstanceId(final AuthorizationLevel level, final String cloud, final String provider, final AuthorizationTargetType targetType, final String target) {
		Assert.notNull(level, "level is empty");
		Assert.isTrue(!Utilities.isEmpty(cloud), "cloud is empty");
		Assert.isTrue(!Utilities.isEmpty(provider), "provider is empty");
		Assert.notNull(targetType, "targetType is empty");
		Assert.isTrue(!Utilities.isEmpty(target), "target is empty");

		return String.join(DELIMITER, level.getPrefix(), cloud, provider, targetType.name(), target);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private InstanceIdUtils() {
		throw new UnsupportedOperationException();
	}
}