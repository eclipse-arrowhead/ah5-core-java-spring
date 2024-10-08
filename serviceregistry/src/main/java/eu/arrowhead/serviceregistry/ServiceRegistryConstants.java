package eu.arrowhead.serviceregistry;

import java.util.List;

import eu.arrowhead.common.jpa.ArrowheadEntity;

public final class ServiceRegistryConstants {

	//=================================================================================================
	// members

	public static final String SYSTEM_NAME = "serviceregistry";

	public static final String DATABASE_ENTITY_PACKAGE = "eu.arrowhead.serviceregistry.jpa.entity";
	public static final String DATABASE_REPOSITORY_PACKAGE = "eu.arrowhead.serviceregistry.jpa.repository";
	public static final String HTTP_API_BASE_PATH = "/serviceregistry";
	public static final String HTTP_API_MONITOR_PATH = HTTP_API_BASE_PATH + "/monitor";
	public static final String HTTP_API_DEVICE_DISCOVERY_PATH = HTTP_API_BASE_PATH + "/device-discovery";
	public static final String HTTP_API_SERVICE_DISCOVERY_PATH = HTTP_API_BASE_PATH + "/service-discovery";
	public static final String HTTP_API_SYSTEM_DISCOVERY_PATH = HTTP_API_BASE_PATH + "/system-discovery";
	public static final String HTTP_API_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/mgmt";
	public static final String HTTP_ATTR_RESTRICTED_SERVICE_LOOKUP = "restricted.service.lookup";

	public static final String METADATA_KEY_UNRESTRICTED_DISCOVERY = "unrestricted-discovery";

	// Operation related

	public static final String HTTP_API_OP_DEVICE_PATH = "/device";
	public static final String HTTP_API_OP_DEVICE_QUERY_PATH = HTTP_API_OP_DEVICE_PATH + "/query";
	public static final String HTTP_API_OP_REGISTER_PATH = "/register";
	public static final String HTTP_API_OP_LOOKUP_PATH = "/lookup";
	public static final String HTTP_API_OP_REVOKE_PATH = "/revoke";
	public static final String HTTP_API_OP_DEVICE_REVOKE_PATH = HTTP_API_OP_REVOKE_PATH + "/{name}";
	public static final String HTTP_API_OP_SERVICE_DEFINITION_PATH = "/service-definition";
	public static final String HTTP_API_OP_SERVICE_DEFINITION_QUERY_PATH = HTTP_API_OP_SERVICE_DEFINITION_PATH + "/query";
	public static final String HTTP_API_OP_SYSTEM_PATH = "/systems";
	public static final String HTTP_API_OP_SYSTEM_QUERY_PATH = HTTP_API_OP_SYSTEM_PATH + "/query";
	public static final String HTTP_API_OP_SERVICE_REVOKE_PATH = HTTP_API_OP_REVOKE_PATH + "/{instanceId}";
	public static final String HTTP_API_OP_GET_CONFIG_PATH = "/get-config";
	public static final String HTTP_API_OP_SERVICE_INSTANCE_PATH = "/service-instance";

	// Configuration related

	public static final String ALLOW_SELF_ADDRESSING = "allow.self.addressing";
	public static final String $ALLOW_SELF_ADDRESSING_WD = "${" + ALLOW_SELF_ADDRESSING + ":true}";
	public static final String ALLOW_NON_ROUTABLE_ADDRESSING = "allow.non.routable.addressing";
	public static final String $ALLOW_NON_ROUTABLE_ADDRESSING_WD = "${" + ALLOW_NON_ROUTABLE_ADDRESSING + ":true}";
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
			// cert related
			"authenticator.secret.key",
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
	public static final int ADDRESS_TYPE_LENGTH = 30;
	public static final int ADDRESS_ADDRESS_LENGTH = ArrowheadEntity.VARCHAR_LARGE;

	// Others

	public static final String INTERFACE_PROPERTY_VALIDATOR_DELIMITER = "\\|";

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private ServiceRegistryConstants() {
		throw new UnsupportedOperationException();
	}

}
