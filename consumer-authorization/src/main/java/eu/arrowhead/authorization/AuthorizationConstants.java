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
	public static final String HTTP_API_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/mgmt";
	public static final String HTTP_API_GENERAL_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/general/mgmt";

	public static final String MQTT_API_BASE_TOPIC_PREFIX = "arrowhead/consumer-authorization";
	public static final String MQTT_API_AUTHORIZATION_BASE_TOPIC = MQTT_API_BASE_TOPIC_PREFIX + "/authorization/";
	public static final String MQTT_API_MANAGEMENT_BASE_TOPIC = MQTT_API_AUTHORIZATION_BASE_TOPIC + "management/";
	public static final String MQTT_API_MONITOR_BASE_TOPIC = MQTT_API_BASE_TOPIC_PREFIX + "/monitor/";
	public static final String MQTT_API_GENERAL_MANAGEMENT_BASE_TOPIC = MQTT_API_BASE_TOPIC_PREFIX + "/general/management/";

	public static final String HTTP_API_OP_GRANT_PATH = "/grant";
	public static final String HTTP_API_OP_REVOKE_PATH = "/revoke";
	public static final String HTTP_PARAM_INSTANCE_ID = "{instanceId}";
	public static final String HTTP_API_OP_SERVICE_REVOKE_PATH = HTTP_API_OP_REVOKE_PATH + "/" + HTTP_PARAM_INSTANCE_ID;
	public static final String HTTP_API_OP_LOOKUP_PATH = "/lookup";
	public static final String HTTP_API_OP_VERIFY_PATH = "/verify";
	public static final String HTTP_API_OP_QUERY_PATH = "/query";
	public static final String HTTP_API_OP_CHECK_PATH = "/check";

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private AuthorizationConstants() {
		throw new UnsupportedOperationException();
	}
}