package eu.arrowhead.authentication;

import eu.arrowhead.common.Defaults;

public final class AuthenticationDefaults extends Defaults {

	//=================================================================================================
	// members

	public static final String IDENTITY_TOKEN_DURATION_DEFAULT = "600"; // in seconds
	public static final String CLEANER_JOB_INTERVAL_DEFAULT = "30000"; // in ms

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private AuthenticationDefaults() {
		throw new UnsupportedOperationException();
	}
}
