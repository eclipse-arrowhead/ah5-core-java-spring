package eu.arrowhead.serviceorchestration.jpa.entity;

import java.time.ZonedDateTime;

import eu.arrowhead.common.jpa.ArrowheadEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class OrchestrationLock {

	//=================================================================================================
	// members

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column(nullable = false, length = ArrowheadEntity.VARCHAR_SMALL)
	private String orchestrationJobId;

	@Column(nullable = false, length = ArrowheadEntity.VARCHAR_MEDIUM)
	private String serviceInstanceId;

	@Column(nullable = false, length = ArrowheadEntity.VARCHAR_SMALL)
	private String consumerSystem;

	@Column(nullable = true)
	private ZonedDateTime expiresAt;

	private boolean temporary;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationLock(final String orchestrationJobId, final String serviceInstanceId, final String consumerSystem, final ZonedDateTime expiresAt, final boolean temporary) {
		this.orchestrationJobId = orchestrationJobId;
		this.serviceInstanceId = serviceInstanceId;
		this.consumerSystem = consumerSystem;
		this.expiresAt = expiresAt;
		this.temporary = temporary;
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public long getId() {
		return id;
	}

	//-------------------------------------------------------------------------------------------------
	public void setId(final long id) {
		this.id = id;
	}

	//-------------------------------------------------------------------------------------------------
	public String getOrchestrationJobId() {
		return orchestrationJobId;
	}

	//-------------------------------------------------------------------------------------------------
	public void setOrchestrationJobId(final String orchestrationJobId) {
		this.orchestrationJobId = orchestrationJobId;
	}

	//-------------------------------------------------------------------------------------------------
	public String getServiceInstanceId() {
		return serviceInstanceId;
	}

	//-------------------------------------------------------------------------------------------------
	public void setServiceInstanceId(final String serviceInstanceId) {
		this.serviceInstanceId = serviceInstanceId;
	}

	//-------------------------------------------------------------------------------------------------
	public String getConsumerSystem() {
		return consumerSystem;
	}

	//-------------------------------------------------------------------------------------------------
	public void setConsumerSystem(final String consumerSystem) {
		this.consumerSystem = consumerSystem;
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
	public boolean isTemporary() {
		return temporary;
	}

	//-------------------------------------------------------------------------------------------------
	public void setTemporary(final boolean temporary) {
		this.temporary = temporary;
	}
}
