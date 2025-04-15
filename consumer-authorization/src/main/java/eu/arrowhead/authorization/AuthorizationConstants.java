package eu.arrowhead.authorization;

public final class AuthorizationConstants {

	//=================================================================================================
	// members

	public static final String SYSTEM_NAME = "consumer-authorization";

	public static final String DATABASE_ENTITY_PACKAGE = "eu.arrowhead.authorization.jpa.entity";
	public static final String DATABASE_REPOSITORY_PACKAGE = "eu.arrowhead.authorization.jpa.repository";

	public static final String HTTP_API_BASE_PATH = "/consumerauthorization";
	public static final String HTTP_API_MONITOR_PATH = HTTP_API_BASE_PATH + "/monitor";
	public static final String HTTP_API_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/mgmt";
	public static final String HTTP_API_GENERAL_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/general/mgmt";
	public static final String HTTP_API_AUTHORIZATION_TOKEN_PATH = HTTP_API_BASE_PATH + "/authorization-token";

	public static final String MQTT_API_BASE_TOPIC_PREFIX = "arrowhead/consumer-authorization";
	public static final String MQTT_API_MONITOR_BASE_TOPIC = MQTT_API_BASE_TOPIC_PREFIX + "/monitor/";
	public static final String MQTT_API_GENERAL_MANAGEMENT_BASE_TOPIC = MQTT_API_BASE_TOPIC_PREFIX + "/general/management/";

	public static final String INSTANCE_ID_PREFIX_MGMT = "MGMT";
	public static final String INSTANCE_ID_PREFIX_PR = "PR";
	
	// Configuration related

	public static final String SIMPLE_TOKEN_BYTE_SIZE = "simple.token.byte.size";
	public static final String $SIMPLE_TOKEN_BYTE_SIZE_WD = "${" + SIMPLE_TOKEN_BYTE_SIZE + ":" + AuthorizationDefaults.DEFAULT_SIMPLE_TOKEN_BYTE_SIZE + "}";
	public static final String SIMPLE_TOKEN_USAGE_LIMIT = "simple.token.usage.limit";
	public static final String $SIMPLE_TOKEN_USAGE_LIMIT_WD = "${" + SIMPLE_TOKEN_USAGE_LIMIT + ":" + AuthorizationDefaults.DEFAULT_SIMPLE_TOKEN_USAGE_LIMIT + "}";
	public static final String SECRET_CRYPTOGRAPHER_KEY = "secret.cryptographer.key";
	public static final String $SECRET_CRYPTOGRAPHER_KEY_WD = "${" + SECRET_CRYPTOGRAPHER_KEY + ":" + AuthorizationDefaults.DEFAULT_SECRET_CRYPTOGRAPHER_KEY + "}";

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private AuthorizationConstants() {
		throw new UnsupportedOperationException();
	}
}