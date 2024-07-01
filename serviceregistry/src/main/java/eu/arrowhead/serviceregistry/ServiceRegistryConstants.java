package eu.arrowhead.serviceregistry;

public class ServiceRegistryConstants {
	
	//=================================================================================================
	// members
	
	public static final String DATABASE_ENTITY_PACKAGE = "eu.arrowhead.servicergistry.jpa.entity";
	public static final String DATABASE_REPOSITORY_PACKAGE = "eu.arrowhead.servicergistry.jpa.repository";
	public static final String HTTP_API_BASE_PATH = "/serviceregistry";
	public static final String HTTP_API_MONITOR_PATH = HTTP_API_BASE_PATH + "/monitor";
	
	//=================================================================================================
	// assistant methods
	
	//-------------------------------------------------------------------------------------------------
	private ServiceRegistryConstants() {
		throw new UnsupportedOperationException();
	}

}
