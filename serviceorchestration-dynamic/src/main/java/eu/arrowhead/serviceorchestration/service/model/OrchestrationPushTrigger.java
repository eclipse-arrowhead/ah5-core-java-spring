package eu.arrowhead.serviceorchestration.service.model;

import java.util.List;

import eu.arrowhead.dto.OrchestrationPushTriggerDTO;

public class OrchestrationPushTrigger {

	//=================================================================================================
	// members

	private String requesterSystem;
	private List<String> targetSystems;
	private List<String> subscriptionIds;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationPushTrigger(final String requesterSystem, final OrchestrationPushTriggerDTO dto) {
		this.requesterSystem = requesterSystem;
		if (dto != null) {
			this.targetSystems = dto.targetSystems();
			this.subscriptionIds = dto.subscriptionIds();
		}
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public String getRequesterSystem() {
		return requesterSystem;
	}

	//-------------------------------------------------------------------------------------------------
	public void setRequesterSystem(final String requesterSystem) {
		this.requesterSystem = requesterSystem;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> getTargetSystems() {
		return targetSystems;
	}

	//-------------------------------------------------------------------------------------------------
	public void setTargetSystems(final List<String> targetSystems) {
		this.targetSystems = targetSystems;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> getSubscriptionIds() {
		return subscriptionIds;
	}

	//-------------------------------------------------------------------------------------------------
	public void setSubscriptionIds(final List<String> subscriptionIds) {
		this.subscriptionIds = subscriptionIds;
	}
}