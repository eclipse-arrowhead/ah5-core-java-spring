package eu.arrowhead.authorization;

public final class AuthorizationDefaults {

	//=================================================================================================
	// members

	public static final String DEFAULT_SCOPE = "*";
	public static final String DEFAULT_SECRET_CRYPTOGRAPHER_KEY="abcd1234efgh5678";

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private AuthorizationDefaults() {
		throw new UnsupportedOperationException();
	}
}