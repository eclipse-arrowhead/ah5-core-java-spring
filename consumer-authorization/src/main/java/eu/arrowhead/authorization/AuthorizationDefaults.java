package eu.arrowhead.authorization;

import eu.arrowhead.common.Defaults;

public final class AuthorizationDefaults extends Defaults {

	//=================================================================================================
	// members

	public static final String DEFAULT_SCOPE = "*";
	public static final int DEFAULT_TOKEN_TIME_LIMIT = 30;
	public static final int DEFAULT_SIMPLE_TOKEN_BYTE_SIZE = 32;
	public static final int DEFAULT_SIMPLE_TOKEN_USAGE_LIMIT = 10;
	public static final String DEFAULT_SECRET_CRYPTOGRAPHER_KEY = "abcd1234efgh5678";

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private AuthorizationDefaults() {
		throw new UnsupportedOperationException();
	}
}