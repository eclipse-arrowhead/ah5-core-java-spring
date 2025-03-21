package eu.arrowhead.authentication;

import eu.arrowhead.common.jpa.ArrowheadEntity;

public final class AuthenticationConstants {

	//=================================================================================================
	// members

	public static final String SYSTEM_NAME = "authentication";

	public static final String DATABASE_ENTITY_PACKAGE = "eu.arrowhead.authentication.jpa.entity";
	public static final String DATABASE_REPOSITORY_PACKAGE = "eu.arrowhead.authentication.jpa.repository";
	public static final int ENCODER_STRENGTH = 10;

	public static final String VERSION_GENERAL_MANAGEMENT = "1.0.0";
	public static final String VERSION_IDENTITY = "1.0.0";
	public static final String VERSION_IDENTITY_MANAGEMENT = "1.0.0";

	public static final String HTTP_API_BASE_PATH = "/authentication";
	public static final String HTTP_API_MONITOR_PATH = HTTP_API_BASE_PATH + "/monitor";
	public static final String HTTP_API_IDENTITY_PATH = HTTP_API_BASE_PATH + "/identity";
	public static final String HTTP_API_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/mgmt";
	public static final String HTTP_API_GENERAL_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/general/mgmt";

	// operation related

	public static final String HTTP_API_OP_LOGIN_PATH = "/login";
	public static final String HTTP_API_OP_LOGOUT_PATH = "/logout";
	public static final String HTTP_API_OP_CHANGE_PATH = "/change";
	public static final String HTTP_API_OP_VERIFY_BASE_PATH = "/verify";
	public static final String HTTP_PARAM_TOKEN = "{token}";
	public static final String HTTP_API_OP_VERIFY_PATH = "/" + HTTP_PARAM_TOKEN;
	public static final String HTTP_API_OP_IDENTITIES_PATH = "/identities";
	public static final String HTTP_API_OP_IDENTITIES_QUERY_PATH = HTTP_API_OP_IDENTITIES_PATH + "/query";
	public static final String HTTP_API_OP_SESSION_PATH = "/sessions";

	// configuration related

	public static final String AUTHENTICATION_SECRET_KEY = "authentication.secret.key";
	public static final String $AUTHENTICATION_SECRET_KEY = "${" + AUTHENTICATION_SECRET_KEY + "}";
	public static final String IDENTITY_TOKEN_DURATION = "identity.token.duration"; // in seconds
	public static final String $IDENTITY_TOKEN_DURATION = "${" + IDENTITY_TOKEN_DURATION + ":" + AuthenticationDefaults.IDENTITY_TOKEN_DURATION_DEFAULT + "}";
	public static final int INFINITE_TOKEN_DURATION = 100; // in years
	public static final String CLEANER_JOB_INTERVAL = "cleaner.job.interval";
	public static final String $CLEANER_JOB_INTERVAL_WD = "${" + CLEANER_JOB_INTERVAL + ":" + AuthenticationDefaults.CLEANER_JOB_INTERVAL_DEFAULT + "}";

	// Quartz related
	public static final String CLEANER_TRIGGER = "authenticationCleanerTrigger";
	public static final String CLEANER_JOB = "authenticationCleanerJob";

	// Forbidden keys (for config service)

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private AuthenticationConstants() {
		throw new UnsupportedOperationException();
	}
}