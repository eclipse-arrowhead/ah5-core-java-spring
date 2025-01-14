package eu.arrowhead.authentication;

public final class AuthenticationConstants {

	//=================================================================================================
	// members

	public static final String SYSTEM_NAME = "authentication";

	public static final String DATABASE_ENTITY_PACKAGE = "eu.arrowhead.authentication.jpa.entity";
	public static final String DATABASE_REPOSITORY_PACKAGE = "eu.arrowhead.authentication.jpa.repository";

	public static final String HTTP_API_BASE_PATH = "/authentication";
	public static final String HTTP_API_MONITOR_PATH = HTTP_API_BASE_PATH + "/monitor";

	// configuration related

	public static final String AUTHENTICATION_SECRET_KEY = "authentication.secret.key";
	public static final String $AUTHENTICATION_SECRET_KEY = "${" + AUTHENTICATION_SECRET_KEY + "}";

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private AuthenticationConstants() {
		throw new UnsupportedOperationException();
	}
}
