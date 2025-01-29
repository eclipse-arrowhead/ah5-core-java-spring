package eu.arrowhead.serviceorchestration.service.model;

import java.util.Map;
import java.util.UUID;

import eu.arrowhead.dto.OrchestrationSubscriptionRequestDTO;

public class OrchestrationSubscription {

	//=================================================================================================
	// members

	private final UUID id;
	private OrchestrationForm orchestrationForm;
	private Long duration;
	private String notifyProtocol;
	private Map<String, String> notifyProperties;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationSubscription(final String requesterSystemName, final OrchestrationSubscriptionRequestDTO dto) {
		this.id = UUID.randomUUID();

		if (dto != null) {
			this.orchestrationForm = new OrchestrationForm(requesterSystemName, dto.targetSystemName(), dto.orchestrationRequest());
			this.duration = dto.duration();

			if (dto.notifyInterface() != null) {
				this.notifyProtocol = dto.notifyInterface().protocol();
				this.notifyProperties = dto.notifyInterface().properties();
			}
		}
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public UUID getId() {
		return id;
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationForm getOrchestrationForm() {
		return orchestrationForm;
	}

	//-------------------------------------------------------------------------------------------------
	public Long getDuration() {
		return duration;
	}

	//-------------------------------------------------------------------------------------------------
	public String getNotifyProtocol() {
		return notifyProtocol;
	}

	//-------------------------------------------------------------------------------------------------
	public Map<String, String> getNotifyProperties() {
		return notifyProperties;
	}

	//-------------------------------------------------------------------------------------------------
	public void setNotifyProtocol(final String notifyProtocol) {
		this.notifyProtocol = notifyProtocol;
	}

	//-------------------------------------------------------------------------------------------------
	public void setNotifyProperties(final Map<String, String> notifyProperties) {
		this.notifyProperties = notifyProperties;
	}

}
