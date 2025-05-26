package eu.arrowhead.authorization.jpa.entity;

import eu.arrowhead.common.jpa.UnmodifiableArrowheadEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class CryptographerAuxiliary extends UnmodifiableArrowheadEntity {

	//=================================================================================================
	// members
	
	@Column(nullable = false)
	private String value;
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public CryptographerAuxiliary() {

	}

	//-------------------------------------------------------------------------------------------------
	public CryptographerAuxiliary(final String auxiliary) {
		this.value = auxiliary;
	}
	
	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "CryptographerAuxiliary [id=" + id + ", value=" + value + ", createdAt=" + createdAt + "]";
	}

	//=================================================================================================
	// boilerplate methods
	
	//-------------------------------------------------------------------------------------------------
	public String getValue() {
		return value;
	}

	//-------------------------------------------------------------------------------------------------
	public void setValue(final String auxiliary) {
		this.value = auxiliary;
	}
}
