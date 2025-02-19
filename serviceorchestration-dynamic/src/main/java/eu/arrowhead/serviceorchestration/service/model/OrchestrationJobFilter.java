package eu.arrowhead.serviceorchestration.service.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.OrchestrationHistoryQueryRequestDTO;
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

	//-------------------------------------------------------------------------------------------------
	public OrchestrationJobFilter(final OrchestrationHistoryQueryRequestDTO dto) {
		this.ids = Utilities.isEmpty(dto.ids()) ? new ArrayList<>() : dto.ids().stream().map(id -> UUID.fromString(id)).toList();
		this.statuses = Utilities.isEmpty(dto.statuses()) ? new ArrayList<>() : dto.statuses().stream().map(s -> OrchestrationJobStatus.valueOf(s)).toList();
		this.type = Utilities.isEmpty(dto.type()) ? null : OrchestrationType.valueOf(dto.type());
		this.requesterSystems = dto.requesterSystrems();
		this.targetSystems = dto.targetSystems();
		this.serviceDefinitions = dto.serviceDefinitions();
		this.subscriptionIds = dto.subscriptionIds();
	}

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public boolean hasFilter() {
		return !Utilities.isEmpty(ids) || !Utilities.isEmpty(statuses) || type != null || !Utilities.isEmpty(requesterSystems)
				|| !Utilities.isEmpty(targetSystems) || !Utilities.isEmpty(serviceDefinitions) || !Utilities.isEmpty(subscriptionIds);
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
