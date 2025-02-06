package eu.arrowhead.serviceorchestration.service.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import eu.arrowhead.serviceorchestration.service.enums.OrchestrationJobStatus;
import eu.arrowhead.serviceorchestration.service.enums.OrchestrationType;

public class OrchestrationJobFilter {

	//=================================================================================================
	// members

	private List<UUID> ids = new ArrayList<>();
	private List<OrchestrationJobStatus> statuses = new ArrayList<>();
	private final OrchestrationType type;
	private List<String> requesterSystems = new ArrayList<>();
	private List<String> targetSystems = new ArrayList<>();
	private List<String> serviceDefinitions = new ArrayList<>();
	private List<String> subscriptionIds = new ArrayList<>();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationJobFilter(final List<UUID> ids, final List<OrchestrationJobStatus> statuses, final OrchestrationType type, final List<String> requesterSystems, final List<String> targetSystems, final List<String> serviceDefinitions,
			final List<String> subscriptionIds) {
		this.ids = ids;
		this.statuses = statuses;
		this.type = type;
		this.requesterSystems = requesterSystems;
		this.targetSystems = targetSystems;
		this.serviceDefinitions = serviceDefinitions;
		this.subscriptionIds = subscriptionIds;
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public List<UUID> getIds() {
		return ids;
	}

	//-------------------------------------------------------------------------------------------------
	public List<OrchestrationJobStatus> getStatuses() {
		return statuses;
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationType getType() {
		return type;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> getRequesterSystems() {
		return requesterSystems;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> getTargetSystems() {
		return targetSystems;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> getServiceDefinitions() {
		return serviceDefinitions;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> getSubscriptionIds() {
		return subscriptionIds;
	}

}
