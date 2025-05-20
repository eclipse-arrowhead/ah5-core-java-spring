package eu.arrowhead.authorization;

public final class AuthorizationConstants {

	//=================================================================================================
	// members

	public static final String SYSTEM_NAME = "consumer-authorization";

	public static final String DATABASE_ENTITY_PACKAGE = "eu.arrowhead.authorization.jpa.entity";
	public static final String DATABASE_REPOSITORY_PACKAGE = "eu.arrowhead.authorization.jpa.repository";

	public static final String VERSION_GENERAL_MANAGEMENT = "1.0.0";
	public static final String VERSION_AUTHORIZATION = "1.0.0";
	public static final String VERSION_AUTHORIZATION_MANAGEMENT = "1.0.0";

	public static final String HTTP_API_BASE_PATH = "/consumerauthorization";
	public static final String HTTP_API_AUTHORIZATION_PATH = HTTP_API_BASE_PATH + "/authorization";
	public static final String HTTP_API_MONITOR_PATH = HTTP_API_BASE_PATH + "/monitor";
	public static final String HTTP_API_MANAGEMENT_PATH = HTTP_API_AUTHORIZATION_PATH + "/mgmt";
	public static final String HTTP_API_GENERAL_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/general/mgmt";
	public static final String HTTP_API_AUTHORIZATION_TOKEN_PATH = HTTP_API_BASE_PATH + "/authorization-token";

	public static final String MQTT_API_BASE_TOPIC_PREFIX = "arrowhead/consumer-authorization";
	public static final String MQTT_API_AUTHORIZATION_BASE_TOPIC = MQTT_API_BASE_TOPIC_PREFIX + "/authorization/";
	public static final String MQTT_API_MANAGEMENT_BASE_TOPIC = MQTT_API_AUTHORIZATION_BASE_TOPIC + "management/";
	public static final String MQTT_API_MONITOR_BASE_TOPIC = MQTT_API_BASE_TOPIC_PREFIX + "/monitor/";
	public static final String MQTT_API_GENERAL_MANAGEMENT_BASE_TOPIC = MQTT_API_BASE_TOPIC_PREFIX + "/general/management/";
	
	public static final String HTTP_PATH_PARAM_TOKEN = "{token}";
	public static final String HTTP_PARAM_INSTANCE_ID = "{instanceId}";

	public static final String HTTP_API_OP_GRANT_PATH = "/grant";
	public static final String HTTP_API_OP_REVOKE_PATH = "/revoke";
	public static final String HTTP_API_OP_SERVICE_REVOKE_PATH = HTTP_API_OP_REVOKE_PATH + "/" + HTTP_PARAM_INSTANCE_ID;
	public static final String HTTP_API_OP_TOKEN_REVOKE_PATH = HTTP_API_OP_REVOKE_PATH + "/token";
	public static final String HTTP_API_OP_LOOKUP_PATH = "/lookup";
	public static final String HTTP_API_OP_VERIFY_PATH = "/verify";
	public static final String HTTP_API_OP_QUERY_PATH = "/query";
	public static final String HTTP_API_OP_TOKEN_QUERY_PATH = HTTP_API_OP_QUERY_PATH + "/token";
	public static final String HTTP_API_OP_CHECK_PATH = "/check";
	public static final String HTTP_API_OP_GENERATE_PATH = "/generate";
	public static final String HTTP_API_OP_VERIFY_TOKEN_PATH = HTTP_API_OP_VERIFY_PATH + "/" + HTTP_PATH_PARAM_TOKEN;
	public static final String HTTP_API_OP_PUBLIC_KEY_PATH = "/public-key";
	public static final String HTTP_API_OP_ENCRYPTION_KEY_PATH = "/encryption-key";

	public static final String INSTANCE_ID_PREFIX_MGMT = "MGMT";
	public static final String INSTANCE_ID_PREFIX_PR = "PR";
	public static final String TOKEN_TYPE_AUTH_SUFFIX = "TOKEN_AUTH";
	
	// Configuration related

	public static final String TOKEN_TIME_LIMIT = "token.time.limit";
	public static final String $TOKEN_TIME_LIMIT_WD = "${" + TOKEN_TIME_LIMIT + ":" + AuthorizationDefaults.DEFAULT_TOKEN_TIME_LIMIT + "}";
	public static final String SIMPLE_TOKEN_BYTE_SIZE = "simple.token.byte.size";
	public static final String $SIMPLE_TOKEN_BYTE_SIZE_WD = "${" + SIMPLE_TOKEN_BYTE_SIZE + ":" + AuthorizationDefaults.DEFAULT_SIMPLE_TOKEN_BYTE_SIZE + "}";
	public static final String SIMPLE_TOKEN_USAGE_LIMIT = "simple.token.usage.limit";
	public static final String $SIMPLE_TOKEN_USAGE_LIMIT_WD = "${" + SIMPLE_TOKEN_USAGE_LIMIT + ":" + AuthorizationDefaults.DEFAULT_SIMPLE_TOKEN_USAGE_LIMIT + "}";
	public static final String SECRET_CRYPTOGRAPHER_KEY = "secret.cryptographer.key";
	public static final String $SECRET_CRYPTOGRAPHER_KEY_WD = "${" + SECRET_CRYPTOGRAPHER_KEY + ":" + AuthorizationDefaults.DEFAULT_SECRET_CRYPTOGRAPHER_KEY + "}";
	public static final String UNBOUNDED_TOKEN_GENERATION_WHITELIST = "unbounded.token.generation.whitelist";
	public static final String $UNBOUNDED_TOKEN_GENERATION_WHITELIST_WD = "${" + UNBOUNDED_TOKEN_GENERATION_WHITELIST + ":" + AuthorizationDefaults.DEFAULT_UNBOUNDED_TOKEN_GENERATION_WHITELIST + "}";

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private AuthorizationConstants() {
		throw new UnsupportedOperationException();
	}
}