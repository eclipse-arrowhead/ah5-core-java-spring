package eu.arrowhead.authorization;

import eu.arrowhead.common.Defaults;

public final class AuthorizationDefaults extends Defaults {

	//=================================================================================================
	// members

	public static final String DEFAULT_SCOPE = "*";

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private AuthorizationDefaults() {
		throw new UnsupportedOperationException();
	}
}