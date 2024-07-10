package eu.arrowhead.serviceregistry.jpa.entity;

import eu.arrowhead.common.jpa.ArrowheadEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class ServiceDefinition extends ArrowheadEntity {

	//=================================================================================================
	// members

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

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
	public long getId() {
		return id;
	}

	//-------------------------------------------------------------------------------------------------
	public String getName() {
		return name;
	}

	//-------------------------------------------------------------------------------------------------
	public void setId(final long id) {
		this.id = id;
	}

	//-------------------------------------------------------------------------------------------------
	public void setName(final String name) {
		this.name = name;
	}
}