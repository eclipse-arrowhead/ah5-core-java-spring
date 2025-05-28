package eu.arrowhead.authorization;

import eu.arrowhead.common.Defaults;

public final class AuthorizationDefaults extends Defaults {

	//=================================================================================================
	// members

	public static final int DEFAULT_TOKEN_MAX_AGE = 60;
	public static final int DEFAULT_TOKEN_TIME_LIMIT = 30;
	public static final int DEFAULT_SIMPLE_TOKEN_BYTE_SIZE = 32;
	public static final int DEFAULT_SIMPLE_TOKEN_USAGE_LIMIT = 10;
	public static final String DEFAULT_UNBOUNDED_TOKEN_GENERATION_WHITELIST = "\"\"";
	public static final String CLEANER_JOB_INTERVAL_DEFAULT = "30000";

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private AuthorizationDefaults() {
		throw new UnsupportedOperationException();
	}
}