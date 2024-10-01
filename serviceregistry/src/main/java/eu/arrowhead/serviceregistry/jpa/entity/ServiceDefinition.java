package eu.arrowhead.serviceregistry.jpa.entity;

import eu.arrowhead.common.jpa.ArrowheadEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class ServiceDefinition extends ArrowheadEntity {

	//=================================================================================================
	// members

	@Column(nullable = false, unique = true, length = VARCHAR_SMALL)
	private String name;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public ServiceDefinition() {
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceDefinition(final String name) {
		this.name = name;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "ServiceDefinition [id = " + id + ", name = " + name + "]";
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public String getName() {
		return name;
	}

	//-------------------------------------------------------------------------------------------------
	public void setName(final String name) {
		this.name = name;
	}
}