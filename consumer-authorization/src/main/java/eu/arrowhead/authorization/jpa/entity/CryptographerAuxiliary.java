package eu.arrowhead.authorization.jpa.entity;

import eu.arrowhead.common.jpa.UnmodifiableArrowheadEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class CryptographerAuxiliary extends UnmodifiableArrowheadEntity {

	//=================================================================================================
	// members
	
	@Column(nullable = false, length = VARCHAR_MEDIUM)
	private String auxiliary;
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public CryptographerAuxiliary() {

	}

	//-------------------------------------------------------------------------------------------------
	public CryptographerAuxiliary(final String auxiliary) {
		this.auxiliary = auxiliary;
	}
	
	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "CryptographerAuxiliary [id=" + id + ", auxiliary=" + auxiliary + ", createdAt=" + createdAt + "]";
	}

	//=================================================================================================
	// boilerplate methods
	
	//-------------------------------------------------------------------------------------------------
	public String getAuxiliary() {
		return auxiliary;
	}

	//-------------------------------------------------------------------------------------------------
	public void setAuxiliary(final String auxiliary) {
		this.auxiliary = auxiliary;
	}
}
