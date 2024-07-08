package eu.arrowhead.serviceregistry.jpa.entity;

import eu.arrowhead.common.jpa.ArrowheadEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class ServiceInterfaceTemplate extends ArrowheadEntity {

	//=================================================================================================
	// members

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column(nullable = false, unique = true, length = VARCHAR_MEDIUM)
	private String name;

	@Column(nullable = false, length = VARCHAR_SMALL)
	private String protocol;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public ServiceInterfaceTemplate() {
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceInterfaceTemplate(final String name, final String protocol) {
		this.name = name;
		this.protocol = protocol;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "ServiceInterfaceTemplate [id = " + id + ", name = " + name + ", protocol = " + protocol + "]";
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
	public String getName() {
		return name;
	}

	//-------------------------------------------------------------------------------------------------
	public void setName(final String name) {
		this.name = name;
	}

	//-------------------------------------------------------------------------------------------------
	public String getProtocol() {
		return protocol;
	}

	//-------------------------------------------------------------------------------------------------
	public void setProtocol(final String protocol) {
		this.protocol = protocol;
	}
}
