package eu.arrowhead.serviceregistry.jpa.entity;

import eu.arrowhead.common.jpa.ArrowheadEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = { "deviceId", "systemId" }), @UniqueConstraint(columnNames = { "systemId" }) })
public class DeviceSystemConnector extends ArrowheadEntity {

	//=================================================================================================
	// members

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "deviceId", referencedColumnName = "id", nullable = false)
	private Device device;

	@OneToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "systemId", referencedColumnName = "id", nullable = false)
	private System system;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public DeviceSystemConnector() {
	}

	//-------------------------------------------------------------------------------------------------
	public DeviceSystemConnector(final Device device, final System system) {
		this.device = device;
		this.system = system;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "DeviceSystemConnector [id = " + id + ", device = " + device + ", system = " + system + "]";
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public Device getDevice() {
		return device;
	}

	//-------------------------------------------------------------------------------------------------
	public void setDevice(final Device device) {
		this.device = device;
	}

	//-------------------------------------------------------------------------------------------------
	public System getSystem() {
		return system;
	}

	//-------------------------------------------------------------------------------------------------
	public void setSystem(final System system) {
		this.system = system;
	}
}