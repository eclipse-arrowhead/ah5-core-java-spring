package eu.arrowhead.authorization.jpa.entity;

import eu.arrowhead.common.jpa.UnmodifiableArrowheadEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class CryptographerIV extends UnmodifiableArrowheadEntity {

	//=================================================================================================
	// members
	
	@Column(nullable = false, length = VARCHAR_SMALL)
	private String iv;
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public CryptographerIV() {

	}

	//-------------------------------------------------------------------------------------------------
	public CryptographerIV(final String iv) {
		this.iv = iv;
	}
	
	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "CryptographerIV [id=" + id + ", iv=" + iv + ", createdAt=" + createdAt + "]";
	}

	//=================================================================================================
	// boilerplate methods
	
	//-------------------------------------------------------------------------------------------------
	public String getIv() {
		return iv;
	}

	//-------------------------------------------------------------------------------------------------
	public void setIv(final String iv) {
		this.iv = iv;
	}
}
