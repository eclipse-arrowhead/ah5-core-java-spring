package eu.arrowhead.serviceorchestration.jpa.entity;

import java.util.List;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.jpa.ArrowheadEntity;
import eu.arrowhead.common.jpa.UUIDArrowheadEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = { "consumer", "serviceDefinition", "priority" }) })
public class OrchestrationStore extends UUIDArrowheadEntity {

	//=================================================================================================
	// members
	
	public static final List<String> SORTABLE_FIELDS_BY = List.of("id", "consumer", "serviceDefinition", "priority", "serviceInstanceId", "createdAt");
	public static final String DEFAULT_SORT_FIELD = "id";
	
	@Column(nullable = false, length = ArrowheadEntity.VARCHAR_SMALL)
	private String consumer;

	@Column(nullable = false, length = ArrowheadEntity.VARCHAR_SMALL)
	private String serviceDefinition;
	
	@Column(nullable = false, length = ArrowheadEntity.VARCHAR_SMALL)
	private String serviceInstanceId;
	
	@Column(nullable = false)
	private int priority;
	
	@Column(nullable = false, length = ArrowheadEntity.VARCHAR_SMALL)
	private String createdBy;
	
	@Column(nullable = false, length = ArrowheadEntity.VARCHAR_SMALL)
	private String updatedBy;
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	@Override
	@PrePersist
	public void onCreate() {
		this.createdAt = Utilities.utcNow();
		this.updatedAt = this.createdAt;
		this.updatedBy = this.createdBy;
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationStore(final String consumer, final String serviceDefinition, final String serviceInstanceId, final int priority, final String createdBy) {
		this.consumer = consumer;
		this.serviceDefinition = serviceDefinition;
		this.serviceInstanceId = serviceInstanceId;
		this.priority = priority;
		this.createdBy = createdBy;
		
	}
	
	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "ServiceInstance [id = " + id + ", serviceDefinition = " + serviceDefinition + ", serviceInstanceId = " + serviceInstanceId + ", priority = " + priority
				+ ", createdBy = " + createdBy + ", updatedBy = " + updatedBy + " createdAt " + createdAt + " updatedAt " + updatedAt + "]";
	}
	
	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public String getConsumer() {
		return consumer;
	}

	//-------------------------------------------------------------------------------------------------
	public void setConsumer(String consumer) {
		this.consumer = consumer;
	}

	//-------------------------------------------------------------------------------------------------
	public String getServiceDefinition() {
		return serviceDefinition;
	}

	//-------------------------------------------------------------------------------------------------
	public void setServiceDefinition(String serviceDefinition) {
		this.serviceDefinition = serviceDefinition;
	}

	//-------------------------------------------------------------------------------------------------
	public String getServiceInstanceId() {
		return serviceInstanceId;
	}

	//-------------------------------------------------------------------------------------------------
	public void setServiceInstanceId(String serviceInstanceId) {
		this.serviceInstanceId = serviceInstanceId;
	}

	//-------------------------------------------------------------------------------------------------
	public int getPriority() {
		return priority;
	}

	//-------------------------------------------------------------------------------------------------
	public void setPriority(int priority) {
		this.priority = priority;
	}

	//-------------------------------------------------------------------------------------------------
	public String getCreatedBy() {
		return createdBy;
	}

	//-------------------------------------------------------------------------------------------------
	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	//-------------------------------------------------------------------------------------------------
	public String getUpdatedBy() {
		return updatedBy;
	}

	//-------------------------------------------------------------------------------------------------
	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}
}
