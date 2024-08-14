package eu.arrowhead.serviceregistry;

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

	// Operation related
	
	public static final String HTTP_API_OP_DEVICE_PATH = "/device";
	public static final String HTTP_API_OP_DEVICE_QUERY_PATH = HTTP_API_OP_DEVICE_PATH + "/query";
	public static final String HTTP_API_OP_REGISTER_PATH = "/register";
	public static final String HTTP_API_OP_LOOKUP_PATH = "/lookup";
	public static final String HTTP_API_OP_REVOKE_PATH = "/revoke/{name}";
	public static final String HTTP_API_OP_SERVICE_DEFINITION_PATH = "/service-definitions";
	
	//System related operations
	
	public static final String HTTP_API_OP_SYSTEM_PATH = "/systems";
	public static final String HTTP_API_OP_SYSTEM_QUERY_PATH = HTTP_API_OP_SYSTEM_PATH + "/query";
	
	// Configuration related

	public static final String ALLOW_SELF_ADDRESSING = "allow.self.addressing";
	public static final String $ALLOW_SELF_ADDRESSING_WD = "${" + ALLOW_SELF_ADDRESSING + ":true}";
	public static final String ALLOW_NON_ROUTABLE_ADDRESSING = "allow.non.routable.addressing";
	public static final String $ALLOW_NON_ROUTABLE_ADDRESSING_WD = "${" + ALLOW_NON_ROUTABLE_ADDRESSING + ":true}";

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private ServiceRegistryConstants() {
		throw new UnsupportedOperationException();
	}

}
