package eu.arrowhead.serviceorchestration;

public final class SimpleServiceOrchestrationConstants {

	//=================================================================================================
	// members

	public static final String SYSTEM_NAME = "serviceorchestration-simple";
	
	// DB
	public static final String DATABASE_ENTITY_PACKAGE = "eu.arrowhead.serviceorchestration.jpa.entity";
	public static final String DATABASE_REPOSITORY_PACKAGE = "eu.arrowhead.serviceorchestration.jpa.repository";
	
	// HTTP API
	public static final String HTTP_API_BASE_PATH = "/serviceorchestration";
	public static final String HTTP_API_MONITOR_PATH = HTTP_API_BASE_PATH + "/monitor";
	public static final String HTTP_API_GENERAL_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/general/mgmt";
	public static final String HTTP_API_ORCHESTRATION_PATH = HTTP_API_BASE_PATH + "/orchestration";
	public static final String HTTP_API_ORCHESTRATION_MGMT_PREFIX = HTTP_API_ORCHESTRATION_PATH + "/mgmt";
	public static final String HTTP_API_SIMPLE_STORE_MANAGEMENT_PATH = HTTP_API_ORCHESTRATION_MGMT_PREFIX + "/simple-store";
	
	// operation
	public static final String HTTP_API_OP_CREATE_PATH = "/create";
	public static final String HTTP_API_OP_QUERY_PATH = "/query";
	

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private SimpleServiceOrchestrationConstants() {
		throw new UnsupportedOperationException();
	}
}
