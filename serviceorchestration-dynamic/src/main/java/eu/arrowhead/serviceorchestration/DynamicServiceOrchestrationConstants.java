package eu.arrowhead.serviceorchestration;

import java.util.List;

public final class DynamicServiceOrchestrationConstants {

	//=================================================================================================
	// members

	public static final String SYSTEM_NAME = "serviceorchestration-dynamic";

	public static final String DATABASE_ENTITY_PACKAGE = "eu.arrowhead.serviceorchestration.jpa.entity";
	public static final String DATABASE_REPOSITORY_PACKAGE = "eu.arrowhead.serviceorchestration.jpa.repository";

	public static final String HTTP_API_BASE_PATH = "/serviceorchestration";
	public static final String HTTP_API_MONITOR_PATH = HTTP_API_BASE_PATH + "/monitor";
	public static final String HTTP_API_ORCHESTRATION_PATH = HTTP_API_BASE_PATH + "/orchestration";
	public static final String HTTP_API_GENERAL_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/general/mgmt";

	public static final String VERSION_ORCHESTRATION = "1.0.0";

	public static final String METADATA_KEY_ORCHESTRATION_STRATEGY = "orchestration-strategy";
	public static final String METADATA_VALUE_ORCHESTRATION_STRATEGY = "dynamic";

	// Operation related

	public static final String HTTP_API_OP_PULL_PATH = "/pull";
	public static final String HTTP_API_OP_PUSH_SUBSCRIBE_PATH = "/push-subscribe";
	public static final String HTTP_API_OP_PUSH_UNSUBSCRIBE_PATH = "/push-unsubscribe";
	public static final String HTTP_API_OP_GET_CONFIG_PATH = "/get-config";

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

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private DynamicServiceOrchestrationConstants() {
		throw new UnsupportedOperationException();
	}
}
