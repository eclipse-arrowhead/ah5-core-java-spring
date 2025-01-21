package eu.arrowhead.serviceorchestration;

public final class DynamicServiceOrchestrationConstants {

	//=================================================================================================
	// members

	public static final String SYSTEM_NAME = "serviceorchestration-dynamic";

	public static final String DATABASE_ENTITY_PACKAGE = "eu.arrowhead.serviceorchestration.jpa.entity";
	public static final String DATABASE_REPOSITORY_PACKAGE = "eu.arrowhead.serviceorchestration.jpa.repository";

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private DynamicServiceOrchestrationConstants() {
		throw new UnsupportedOperationException();
	}
}
