package eu.arrowhead.authentication;

public final class AuthenticationConstants {

	//=================================================================================================
	// members

	public static final String SYSTEM_NAME = "authentication";

	public static final String DATABASE_ENTITY_PACKAGE = "eu.arrowhead.authentication.jpa.entity";
	public static final String DATABASE_REPOSITORY_PACKAGE = "eu.arrowhead.authentication.jpa.repository";
	public static final int ENCODER_STRENGTH = 10;

	public static final String HTTP_API_BASE_PATH = "/authentication";
	public static final String HTTP_API_MONITOR_PATH = HTTP_API_BASE_PATH + "/monitor";
	public static final String HTTP_API_IDENTITY_PATH = HTTP_API_BASE_PATH + "/identity";

	// operation related

	public static final String HTTP_API_OP_LOGIN_PATH = "/login";

	// configuration related

	public static final String AUTHENTICATION_SECRET_KEY = "authentication.secret.key";
	public static final String $AUTHENTICATION_SECRET_KEY = "${" + AUTHENTICATION_SECRET_KEY + "}";
	public static final String IDENTITY_TOKEN_DURATION = "identity.token.duration"; // in seconds
	public static final String $IDENTITY_TOKEN_DURATION = "${" + IDENTITY_TOKEN_DURATION + ":600}";
	public static final int INFINITE_TOKEN_DURATION = 100; // in years

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private AuthenticationConstants() {
		throw new UnsupportedOperationException();
	}
}
