package eu.arrowhead.serviceorchestration.jpa.entity;

import java.time.ZonedDateTime;
import java.util.UUID;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.jpa.ArrowheadEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = { "ownerSystem", "targetSystem", "serviceDefinition" }) })
public class Subscription {

	//=================================================================================================
	// members

	@Id
	private UUID id;

	@Column(nullable = false, length = ArrowheadEntity.VARCHAR_SMALL)
	private String ownerSystem;

	@Column(nullable = false, length = ArrowheadEntity.VARCHAR_SMALL)
	private String targetSystem;

	@Column(nullable = false, length = ArrowheadEntity.VARCHAR_SMALL)
	private String serviceDefinition;

	@Column(nullable = true)
	private ZonedDateTime expiresAt;

	@Column(nullable = false, length = ArrowheadEntity.VARCHAR_TINY)
	private String notifyProtocol;

	@Column(nullable = false)
	private String notifyProperties;

	@Column(nullable = false)
	private String orchestrationRequest;

	@Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
	private ZonedDateTime createdAt;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Subscription(final UUID id, final String ownerSystem, final String targetSystem, final ZonedDateTime expiresAt, final String notifyProtocol, final String notifyProperties, final String orchestrationRequest) {
		this.id = id;
		this.ownerSystem = ownerSystem;
		this.targetSystem = targetSystem;
		this.expiresAt = expiresAt;
		this.notifyProtocol = notifyProtocol;
		this.notifyProperties = notifyProperties;
		this.orchestrationRequest = orchestrationRequest;
	}

	//-------------------------------------------------------------------------------------------------
	@PrePersist
	public void onCreate() {
		this.createdAt = Utilities.utcNow();
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "System [id = " + id + ", ownerSystem = " + ownerSystem + ", targetSystem = " + targetSystem + ", serviceDefinition = " + serviceDefinition + ", expiresAt = " + expiresAt + ", notifyProtocol = " + notifyProtocol
				+ ", notifyProperties = " + notifyProperties + ", orchestrationRequest = " + orchestrationRequest + ", createdAt = " + createdAt + "]";
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public UUID getId() {
		return id;
	}

	//-------------------------------------------------------------------------------------------------
	public void setId(final UUID id) {
		this.id = id;
	}

	//-------------------------------------------------------------------------------------------------
	public String getOwnerSystem() {
		return ownerSystem;
	}

	//-------------------------------------------------------------------------------------------------
	public void setOwnerSystem(final String ownerSystem) {
		this.ownerSystem = ownerSystem;
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

	//-------------------------------------------------------------------------------------------------
	public ZonedDateTime getExpiresAt() {
		return expiresAt;
	}

	//-------------------------------------------------------------------------------------------------
	public void setExpiresAt(final ZonedDateTime expiresAt) {
		this.expiresAt = expiresAt;
	}

	//-------------------------------------------------------------------------------------------------
	public String getNotifyProtocol() {
		return notifyProtocol;
	}

	//-------------------------------------------------------------------------------------------------
	public void setNotifyProtocol(final String notifyProtocol) {
		this.notifyProtocol = notifyProtocol;
	}

	//-------------------------------------------------------------------------------------------------
	public String getNotifyProperties() {
		return notifyProperties;
	}

	//-------------------------------------------------------------------------------------------------
	public void setNotifyProperties(final String notifyProperties) {
		this.notifyProperties = notifyProperties;
	}

	//-------------------------------------------------------------------------------------------------
	public String getOrchestrationRequest() {
		return orchestrationRequest;
	}

	//-------------------------------------------------------------------------------------------------
	public void setOrchestrationRequest(final String orchestrationRequest) {
		this.orchestrationRequest = orchestrationRequest;
	}

	//-------------------------------------------------------------------------------------------------
	public ZonedDateTime getCreatedAt() {
		return createdAt;
	}

	//-------------------------------------------------------------------------------------------------
	public void setCreatedAt(final ZonedDateTime createdAt) {
		this.createdAt = createdAt;
	}

}
