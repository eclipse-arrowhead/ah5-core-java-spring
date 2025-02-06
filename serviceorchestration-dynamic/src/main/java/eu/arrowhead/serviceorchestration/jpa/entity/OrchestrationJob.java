package eu.arrowhead.serviceorchestration.jpa.entity;

import java.time.ZonedDateTime;
import java.util.UUID;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.jpa.ArrowheadEntity;
import eu.arrowhead.serviceorchestration.service.enums.OrchestrationJobStatus;
import eu.arrowhead.serviceorchestration.service.enums.OrchestrationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;

@Entity
public class OrchestrationJob {

	//=================================================================================================
	// members

	@Id
	private UUID id;

	@Column(nullable = false, length = ArrowheadEntity.VARCHAR_TINY)
	private String status;

	@Column(nullable = false, length = ArrowheadEntity.VARCHAR_TINY)
	private String type;

	@Column(nullable = false, length = ArrowheadEntity.VARCHAR_SMALL)
	private String requesterSystem;

	@Column(nullable = false, length = ArrowheadEntity.VARCHAR_SMALL)
	private String targetSystem;

	@Column(nullable = false, length = ArrowheadEntity.VARCHAR_SMALL)
	private String serviceDefinition;

	@Column(nullable = false, length = ArrowheadEntity.VARCHAR_SMALL)
	private String subscriptionId;

	@Column(nullable = false)
	private String message;

	@Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
	private ZonedDateTime createdAt;

	@Column(nullable = false)
	private ZonedDateTime startedAt;

	@Column(nullable = false)
	private ZonedDateTime finishedAt;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@PrePersist
	public void onCreate() {
		this.createdAt = Utilities.utcNow();
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationJob(final OrchestrationType type, final String requesterSystem, final String targetSystem, final String serviceDefinition, final String subscriptionId) {
		this.id = UUID.randomUUID();
		this.type = type.name();
		this.status = OrchestrationJobStatus.PENDING.name();
		this.requesterSystem = requesterSystem;
		this.targetSystem = targetSystem;
		this.serviceDefinition = serviceDefinition;
		this.subscriptionId = subscriptionId;
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public UUID getId() {
		return id;
	}

	//-------------------------------------------------------------------------------------------------
	public void setFinishedAt(final ZonedDateTime finishedAt) {
		this.finishedAt = finishedAt;
	}

	//-------------------------------------------------------------------------------------------------
	public String getStatus() {
		return status;
	}

	//-------------------------------------------------------------------------------------------------
	public void setStatus(final String status) {
		this.status = status;
	}

	//-------------------------------------------------------------------------------------------------
	public String getType() {
		return type;
	}

	//-------------------------------------------------------------------------------------------------
	public void setType(final String type) {
		this.type = type;
	}

	//-------------------------------------------------------------------------------------------------
	public String getRequesterSystem() {
		return requesterSystem;
	}

	//-------------------------------------------------------------------------------------------------
	public void setRequesterSystem(final String requesterSystem) {
		this.requesterSystem = requesterSystem;
	}

	//-------------------------------------------------------------------------------------------------
	public String getTargetSystem() {
		return targetSystem;
	}

	//-------------------------------------------------------------------------------------------------
	public void setTargetSystem(final String targetSystem) {
		this.targetSystem = targetSystem;
	}

	//-------------------------------------------------------------------------------------------------
	public String getServiceDefinition() {
		return serviceDefinition;
	}

	//-------------------------------------------------------------------------------------------------
	public void setServiceDefinition(final String serviceDefinition) {
		this.serviceDefinition = serviceDefinition;
	}

	public String getSubscriptionId() {
		return subscriptionId;
	}

	//-------------------------------------------------------------------------------------------------
	public void setSubscriptionId(final String subscriptionId) {
		this.subscriptionId = subscriptionId;
	}

	//-------------------------------------------------------------------------------------------------
	public String getMessage() {
		return message;
	}

	//-------------------------------------------------------------------------------------------------
	public void setMessage(final String message) {
		this.message = message;
	}

	//-------------------------------------------------------------------------------------------------
	public ZonedDateTime getCreatedAt() {
		return createdAt;
	}

	//-------------------------------------------------------------------------------------------------
	public void setCreatedAt(final ZonedDateTime createdAt) {
		this.createdAt = createdAt;
	}

	//-------------------------------------------------------------------------------------------------
	public void setId(final UUID id) {
		this.id = id;
	}

	//-------------------------------------------------------------------------------------------------
	public ZonedDateTime getStartedAt() {
		return startedAt;
	}

	//-------------------------------------------------------------------------------------------------
	public void setStartedAt(final ZonedDateTime startedAt) {
		this.startedAt = startedAt;
	}

	//-------------------------------------------------------------------------------------------------
	public ZonedDateTime getFinishedAt() {
		return finishedAt;
	}
}
