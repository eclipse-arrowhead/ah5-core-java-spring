package eu.arrowhead.authorization.jpa.entity;

import eu.arrowhead.common.jpa.UnmodifiableArrowheadEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@Entity
public class EncryptionKey extends UnmodifiableArrowheadEntity {

	//=================================================================================================
	// members

	@Column(nullable = false, unique = true, length = VARCHAR_SMALL)
	private String systemName;

	@Column(nullable = false)
	private String key;
	
	@OneToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "ivId", referencedColumnName = "id", nullable = false)
	private CryptographerIV iv;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public EncryptionKey() {

	}

	//-------------------------------------------------------------------------------------------------
	public EncryptionKey(final String systemName, final String key, final CryptographerIV iv) {
		this.systemName = systemName;
		this.key = key;
		this.iv = iv;
	}
	
	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "EncryptionKey [id=" + id + ", systemName=" + systemName + ", key=" + key + ", iv=" + iv + ", createdAt=" + createdAt + "]";
	}
	
	//=================================================================================================
	// boilerplate methods

	//-------------------------------------------------------------------------------------------------
	public String getSystemName() {
		return systemName;
	}

	//-------------------------------------------------------------------------------------------------
	public void setSystemName(final String systemName) {
		this.systemName = systemName;
	}

	//-------------------------------------------------------------------------------------------------
	public String getKey() {
		return key;
	}

	//-------------------------------------------------------------------------------------------------
	public void setKey(final String key) {
		this.key = key;
	}

	//-------------------------------------------------------------------------------------------------
	public CryptographerIV getIv() {
		return iv;
	}

	//-------------------------------------------------------------------------------------------------
	public void setIv(final CryptographerIV iv) {
		this.iv = iv;
	}
}
