package eu.arrowhead.serviceregistry.jpa.entity;

import eu.arrowhead.common.jpa.ArrowheadEntity;
import eu.arrowhead.dto.enums.AddressType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class DeviceAddress extends ArrowheadEntity {

	//=================================================================================================
	// members

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "deviceId", referencedColumnName = "id", nullable = false)
	private Device device;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private AddressType addressType;

	@Column(nullable = false, length = VARCHAR_LARGE)
	private String address;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public DeviceAddress() {
	}

	//-------------------------------------------------------------------------------------------------
	public DeviceAddress(final Device device, final AddressType addressType, final String address) {
		this.device = device;
		this.addressType = addressType;
		this.address = address;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "DeviceAddress [id = " + id + ", device = " + device + ", addressType = " + addressType + ", address = " + address + "]";
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
	public AddressType getAddressType() {
		return addressType;
	}

	//-------------------------------------------------------------------------------------------------
	public void setAddressType(final AddressType addressType) {
		this.addressType = addressType;
	}

	//-------------------------------------------------------------------------------------------------
	public String getAddress() {
		return address;
	}

	//-------------------------------------------------------------------------------------------------
	public void setAddress(final String address) {
		this.address = address;
	}
}