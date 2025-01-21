package eu.arrowhead.serviceorchestration;

public final class DynamicServiceOrchestrationConstants {

	//=================================================================================================
	// members

	public static final String SYSTEM_NAME = "serviceorchestration-dynamic";

	public static final String DATABASE_ENTITY_PACKAGE = "eu.arrowhead.serviceorchestration.jpa.entity";
	public static final String DATABASE_REPOSITORY_PACKAGE = "eu.arrowhead.serviceorchestration.jpa.repository";

	public static final String HTTP_API_BASE_PATH = "/serviceorchestration";
	public static final String HTTP_API_MONITOR_PATH = HTTP_API_BASE_PATH + "/monitor";
	public static final String HTTP_API_ORCHESTRATION_PATH = HTTP_API_BASE_PATH + "/orchestration";

	public static final String VERSION_ORCHESTRATION = "1.0.0";

	public static final String METADATA_KEY_ORCHESTRATION_STRATEGY = "orchestration-strategy";
	public static final String METADATA_VALUE_ORCHESTRATION_STRATEGY = "dynamic";

	// Operation related

	public static final String HTTP_API_OP_PULL_PATH = "/pull";
	public static final String HTTP_API_OP_PUSH_SUBSCRIBE_PATH = "/push-subscribe";
	public static final String HTTP_API_OP_PUSH_UNSUBSCRIBE_PATH = "/push-unsubscribe";

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private DynamicServiceOrchestrationConstants() {
		throw new UnsupportedOperationException();
	}
}
