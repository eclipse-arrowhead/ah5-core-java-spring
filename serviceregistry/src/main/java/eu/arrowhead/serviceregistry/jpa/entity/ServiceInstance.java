package eu.arrowhead.serviceregistry.jpa.entity;

import java.time.ZonedDateTime;

import eu.arrowhead.common.jpa.ArrowheadEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"systemId", "serviceDefinitionId", "version"}))
public class ServiceInstance extends ArrowheadEntity {

	//=================================================================================================
	// members

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column(nullable = false, length = VARCHAR_MEDIUM)
	private String serviceInstanceId;

	@OneToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "systemId", referencedColumnName = "id", nullable = false)
	private System system;

	@OneToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "serviceDefinitionId", referencedColumnName = "id", nullable = false)
	private ServiceDefinition serviceDefinition;

	@Column(nullable = false, length = VARCHAR_TINY)
	private String version = "1.0.0";

	@Column(nullable = true)
	private ZonedDateTime expireAt;

	@Column(nullable = true)
	private String metadata;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public ServiceInstance() {
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceInstance(final String serviceInstanceId, final System system, final ServiceDefinition serviceDefinition, final String version, final ZonedDateTime expireAt, final String metadata) {
		this.serviceInstanceId = serviceInstanceId;
		this.system = system;
		this.serviceDefinition = serviceDefinition;
		this.version = version;
		this.expireAt = expireAt;
		this.metadata = metadata;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "ServiceInstance [id = " + id + ", serviceInstanceId = " + serviceInstanceId + ", system = " + system + ", serviceDefinition = " + serviceDefinition + ", version = " + version
				+ ", expireAt = " + expireAt + ", metadata = " + metadata + "]";
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
	public String getServiceInstanceId() {
		return serviceInstanceId;
	}

	//-------------------------------------------------------------------------------------------------
	public void setServiceInstanceId(final String serviceInstanceId) {
		this.serviceInstanceId = serviceInstanceId;
	}

	//-------------------------------------------------------------------------------------------------
	public System getSystem() {
		return system;
	}

	//-------------------------------------------------------------------------------------------------
	public void setSystem(final System system) {
		this.system = system;
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceDefinition getServiceDefinition() {
		return serviceDefinition;
	}

	//-------------------------------------------------------------------------------------------------
	public void setServiceDefinition(final ServiceDefinition serviceDefinition) {
		this.serviceDefinition = serviceDefinition;
	}

	//-------------------------------------------------------------------------------------------------
	public String getVersion() {
		return version;
	}

	//-------------------------------------------------------------------------------------------------
	public void setVersion(final String version) {
		this.version = version;
	}

	//-------------------------------------------------------------------------------------------------
	public ZonedDateTime getExpireAt() {
		return expireAt;
	}

	//-------------------------------------------------------------------------------------------------
	public void setExpireAt(final ZonedDateTime expireAt) {
		this.expireAt = expireAt;
	}

	//-------------------------------------------------------------------------------------------------
	public String getMetadata() {
		return metadata;
	}

	//-------------------------------------------------------------------------------------------------
	public void setMetadata(final String metadata) {
		this.metadata = metadata;
	}

}
