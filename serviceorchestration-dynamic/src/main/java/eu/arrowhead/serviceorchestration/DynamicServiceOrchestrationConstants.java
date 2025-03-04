package eu.arrowhead.serviceorchestration;

import java.util.List;

public final class DynamicServiceOrchestrationConstants {

	//=================================================================================================
	// members

	public static final String SYSTEM_NAME = "serviceorchestration-dynamic";

	public static final String DATABASE_ENTITY_PACKAGE = "eu.arrowhead.serviceorchestration.jpa.entity";
	public static final String DATABASE_REPOSITORY_PACKAGE = "eu.arrowhead.serviceorchestration.jpa.repository";

	public static final String ENABLE_AUTHORIZATION = "enable.authorization";
	public static final String $ENABLE_AUTHORIZATION_WD = "${" + ENABLE_AUTHORIZATION + ":false}";
	public static final String ENABLE_TRANSLATION = "enable.translation";
	public static final String $ENABLE_TRANSLATION_WD = "${" + ENABLE_TRANSLATION + ":false}";
	public static final String ENABLE_QOS = "enable.qos";
	public static final String $ENABLE_QOS_WD = "${" + ENABLE_QOS + ":false}";
	public static final String ENABLE_INTERCLOUD = "enable.intercloud";
	public static final String $ENABLE_INTERCLOUD_WD = "${" + ENABLE_INTERCLOUD + ":false}";
	public static final String CLEANER_JOB_INTERVAL = "cleaner.job.interval";
	public static final String $CLEANER_JOB_INTERVAL_WD = "${" + CLEANER_JOB_INTERVAL + ":30000}";
	public static final String ORCHESTRATION_HISTORY_MAX_AGE = "orchestration.history.max.age";
	public static final String $ORCHESTRATION_HISTORY_MAX_AGE_WD = "${" + ORCHESTRATION_HISTORY_MAX_AGE + ":15}";
	public static final String PUSH_ORCHESTRATION_MAX_THREAD = "push.orchestration.max.thread";
	public static final String $PUSH_ORCHESTRATION_MAX_THREAD_WD = "${" + PUSH_ORCHESTRATION_MAX_THREAD + ":5}";

	public static final String HTTP_API_BASE_PATH = "/serviceorchestration";
	public static final String HTTP_API_MONITOR_PATH = HTTP_API_BASE_PATH + "/monitor";
	public static final String HTTP_API_GENERAL_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/general/mgmt";
	public static final String HTTP_API_ORCHESTRATION_PATH = HTTP_API_BASE_PATH + "/orchestration";
	public static final String HTTP_API_ORCHESTRATION_PUSH_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/orchestration/mgmt/push";
	public static final String HTTP_API_ORCHESTRATION_HISTORY_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/orchestration/mgmt/history";
	public static final String HTTP_API_ORCHESTRATION_LOCK_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/orchestration/mgmt/lock";

	public static final String MQTT_API_BASE_TOPIC_PREFIX = "arrowhead/serviceorchestration";
	public static final String MQTT_API_MONITOR_BASE_TOPIC = MQTT_API_BASE_TOPIC_PREFIX + "/monitor/";
	public static final String MQTT_API_GENERAL_MANAGEMENT_BASE_TOPIC = MQTT_API_BASE_TOPIC_PREFIX + "/general/management/";

	public static final String VERSION_MONITOR = "1.0.0";
	public static final String VERSION_GENERAL_MANAGEMENT = "1.0.0";
	public static final String VERSION_ORCHESTRATION = "1.0.0";
	public static final String VERSION_ORCHESTRATION_PUSH_MANAGEMENT = "1.0.0";
	public static final String VERSION_ORCHESTRATION_LOCK_MANAGEMENT = "1.0.0";
	public static final String VERSION_ORCHESTRATION_HISTORY_MANAGEMENT = "1.0.0";

	public static final String METADATA_KEY_ORCHESTRATION_STRATEGY = "orchestration-strategy";
	public static final String METADATA_VALUE_ORCHESTRATION_STRATEGY = "dynamic";

	public static final String JOB_QUEUE_PUSH_ORCHESTRATION = "jobQueuePushOrchestration";
	public static final String SERVICE_INSTANCE_MATCHMAKER = "serviceInstanceMatchmaker";

	// Quartz related
	public static final String CLEANER_TRIGGER = "dynamicOrchestrationCleanerTrigger";
	public static final String CLEANER_JOB = "dynamicOrchestrationCleanerJob";

	// Operation related

	public static final String HTTP_API_OP_PULL_PATH = "/pull";
	public static final String HTTP_API_OP_PUSH_SUBSCRIBE_PATH = "/subscribe";
	public static final String HTTP_API_OP_PUSH_UNSUBSCRIBE_PATH = "/unsubscribe/{id}";
	public static final String HTTP_API_OP_PUSH_UNSUBSCRIBE_BULK_PATH = "/unsubscribe";
	public static final String HTTP_API_OP_PUSH_TRIGGER_PATH = "/trigger";
	public static final String HTTP_API_OP_QUERY_PATH = "/query";
	public static final String HTTP_API_OP_CREATE_PATH = "/create";
	public static final String HTTP_API_OP_REMOVE_LOCK_PATH = "/remove/{instanceId}/{owner}";

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

	public static final String ORCH_WARN_AUTO_MATCHMAKING = "auto_matchmaking";
	public static final String ORCH_WARN_QOS_NOT_ENABLED = "qos_not_enabled";
	public static final String ORCH_WARN_NOT_EXCLUSIVE = "not_exclusive";
	public static final String ORCH_WARN_PART_TIME_EXCLUSIVITY = "part_time_exclusivity";
	public static final String ORCH_WARN_INTER_CLOUD = "inter_cloud";

	public static final String NOTIFY_KEY_ADDRESS = "address";
	public static final String NOTIFY_KEY_PORT = "port";
	public static final String NOTIFY_KEY_METHOD = "method";
	public static final String NOTIFY_KEY_PATH = "path";
	public static final String NOTIFY_KEY_TOPIC = "topic";

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private DynamicServiceOrchestrationConstants() {
		throw new UnsupportedOperationException();
	}
}
