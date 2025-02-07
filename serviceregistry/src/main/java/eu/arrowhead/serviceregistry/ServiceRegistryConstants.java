package eu.arrowhead.serviceregistry;

import java.util.List;

import eu.arrowhead.common.jpa.ArrowheadEntity;

public final class ServiceRegistryConstants {

	//=================================================================================================
	// members

	public static final String SYSTEM_NAME = "serviceregistry";

	public static final String DATABASE_ENTITY_PACKAGE = "eu.arrowhead.serviceregistry.jpa.entity";
	public static final String DATABASE_REPOSITORY_PACKAGE = "eu.arrowhead.serviceregistry.jpa.repository";

	public static final String REQUEST_ATTR_RESTRICTED_SERVICE_LOOKUP = "restricted.service.lookup";
	public static final int REQUEST_FILTER_ORDER_SERVICE_LOOKUP = 35;

	public static final String HTTP_API_BASE_PATH = "/serviceregistry";
	public static final String HTTP_API_MONITOR_PATH = HTTP_API_BASE_PATH + "/monitor";
	public static final String HTTP_API_DEVICE_DISCOVERY_PATH = HTTP_API_BASE_PATH + "/device-discovery";
	public static final String HTTP_API_SERVICE_DISCOVERY_PATH = HTTP_API_BASE_PATH + "/service-discovery";
	public static final String HTTP_API_SYSTEM_DISCOVERY_PATH = HTTP_API_BASE_PATH + "/system-discovery";
	public static final String HTTP_API_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/mgmt";
	public static final String HTTP_API_GENERAL_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/general/mgmt";

	public static final String MQTT_API_BASE_TOPIC = "arrowhead/serviceregistry";
	public static final String MQTT_API_MONITOR_TOPIC = MQTT_API_BASE_TOPIC + "/monitor";
	public static final String MQTT_API_DEVICE_DISCOVERY_TOPIC = MQTT_API_BASE_TOPIC + "/device-discovery";
	public static final String MQTT_API_SERVICE_DISCOVERY_TOPIC = MQTT_API_BASE_TOPIC + "/service-discovery";
	public static final String MQTT_API_SYSTEM_DISCOVERY_TOPIC = MQTT_API_BASE_TOPIC + "/system-discovery";
	public static final String MQTT_API_MANAGEMENT_TOPIC = MQTT_API_BASE_TOPIC + "/management";
	public static final String MQTT_API_GENERAL_MANAGEMENT_TOPIC = MQTT_API_BASE_TOPIC + "/general/management";

	public static final String VERSION_DEVICE_DISCOVERY = "1.0.0";
	public static final String VERSION_SYSTEM_DISCOVERY = "1.0.0";
	public static final String VERSION_SERVICE_DISCOVERY = "1.0.0";
	public static final String VERSION_GENERAL_MANAGEMENT = "1.0.0";
	public static final String VERSION_SERVICE_REGISTRY_MANAGEMENT = "1.0.0";

	// Operation related

	public static final String HTTP_API_OP_DEVICE_PATH = "/devices";
	public static final String HTTP_API_OP_DEVICE_QUERY_PATH = HTTP_API_OP_DEVICE_PATH + "/query";
	public static final String HTTP_API_OP_REGISTER_PATH = "/register";
	public static final String HTTP_API_OP_LOOKUP_PATH = "/lookup";
	public static final String HTTP_API_OP_REVOKE_PATH = "/revoke";
	public static final String HTTP_PARAM_NAME = "{name}";
	public static final String HTTP_API_OP_DEVICE_REVOKE_PATH = HTTP_API_OP_REVOKE_PATH + "/" + HTTP_PARAM_NAME;
	public static final String HTTP_API_OP_SERVICE_DEFINITION_PATH = "/service-definitions";
	public static final String HTTP_API_OP_SERVICE_DEFINITION_QUERY_PATH = HTTP_API_OP_SERVICE_DEFINITION_PATH + "/query";
	public static final String HTTP_API_OP_SYSTEM_PATH = "/systems";
	public static final String HTTP_API_OP_SYSTEM_QUERY_PATH = HTTP_API_OP_SYSTEM_PATH + "/query";
	public static final String HTTP_PARAM_INSTANCE_ID = "{instanceId}";
	public static final String HTTP_API_OP_SERVICE_REVOKE_PATH = HTTP_API_OP_REVOKE_PATH + "/" + HTTP_PARAM_INSTANCE_ID;
	public static final String HTTP_API_OP_SERVICE_INSTANCE_PATH = "/service-instances";
	public static final String HTTP_API_OP_SERVICE_INSTANCE_QUERY_PATH = HTTP_API_OP_SERVICE_INSTANCE_PATH + "/query";
	public static final String HTTP_API_OP_INTERFACE_TEMPLATE_PATH = "/interface-templates";
	public static final String HTTP_API_OP_INTERFACE_TEMPLATE_QUERY_PATH = HTTP_API_OP_INTERFACE_TEMPLATE_PATH + "/query";

	// Configuration related

	public static final String DISCOVERY_VERBOSE = "discovery.verbose";
	public static final String $DISCOVERY_VERBOSE_WD = "${" + DISCOVERY_VERBOSE + ":false}";
	public static final String SERVICE_DISCOVERY_POLICY = "service.discovery.policy";
	public static final String $SERVICE_DISCOVERY_POLICY_WD = "${" + SERVICE_DISCOVERY_POLICY + ":restricted}";
	public static final String SERVICE_DISCOVERY_DIRECT_ACCESS = "service.discovery.direct.access";
	public static final String SERVICE_DISCOVERY_INTERFACE_POLICY = "service.discovery.interface.policy";
	public static final String $SERVICE_DISCOVERY_INTERFACE_POLICY_WD = "${" + SERVICE_DISCOVERY_INTERFACE_POLICY + ":restricted}";

	// Forbidden keys (for config service)

	public static final List<String> FORBIDDEN_KEYS = List.of(
			// database related
			"spring.datasource.url",
			"spring.datasource.username",
			"spring.datasource.password",
			"spring.datasource.driver-class-name",
			"spring.jpa.hibernate.ddl-auto",
			"spring.jpa.show-sql",

			// MQTT related
			"mqtt.client.password",

			// authentication related
			"authenticator.secret.keys",
			"authenticator.credentials",
			"server.ssl.key-store-type",
			"server.ssl.key-store",
			"server.ssl.key-store-password",
			"server.ssl.key-alias",
			"server.ssl.key-password",
			"server.ssl.client-auth",
			"server.ssl.trust-store-type",
			"server.ssl.trust-store",
			"server.ssl.trust-store-password",
			"disable.hostname.verifier");

	// Property size related

	public static final int DEVICE_NAME_LENGTH = ArrowheadEntity.VARCHAR_SMALL;
	public static final int SYSTEM_NAME_LENGTH = ArrowheadEntity.VARCHAR_SMALL;
	public static final int SYSTEM_VERSION_LENGTH = ArrowheadEntity.VARCHAR_TINY;
	public static final int SERVICE_DEFINITION_NAME_LENGTH = ArrowheadEntity.VARCHAR_SMALL;
	public static final int ADDRESS_LENGTH = ArrowheadEntity.VARCHAR_LARGE;
	public static final int INTERFACE_TEMPLATE_NAME_LENGTH = ArrowheadEntity.VARCHAR_MEDIUM;
	public static final int INTERFACE_TEMPLATE_PROTOCOL_LENGTH = ArrowheadEntity.VARCHAR_SMALL;
	public static final int INTERFACE_PROPERTY_NAME_LENGTH = ArrowheadEntity.VARCHAR_SMALL;

	// Others

	public static final String INTERFACE_PROPERTY_VALIDATOR_DELIMITER = "|";

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private ServiceRegistryConstants() {
		throw new UnsupportedOperationException();
	}
}