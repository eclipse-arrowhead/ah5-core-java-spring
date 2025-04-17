package eu.arrowhead.authorization.jpa.entity;

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

@Entity
public class SelfContainedToken {

	//=================================================================================================
	// members

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@OneToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "headerId", referencedColumnName = "id", nullable = false)
	private TokenHeader header;
	
	@Column(nullable = false, length = ArrowheadEntity.VARCHAR_SMALL)
	private String type;

	@Column(nullable = false)
	protected ZonedDateTime expiresAt;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public SelfContainedToken() {

	}

	//-------------------------------------------------------------------------------------------------
	public SelfContainedToken(
			final TokenHeader header,
			final String type,
			final ZonedDateTime expiresAt) {
		this.header = header;
		this.type = type;
		this.expiresAt = expiresAt;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "JsonWebToken [id=" + id + ", header=" + header + ", type=" + type + ", expiresAt=" + expiresAt + "]";
	}

	//=================================================================================================
	// boilerplate code

	//-------------------------------------------------------------------------------------------------
	public long getId() {
		return id;
	}

	//-------------------------------------------------------------------------------------------------
	public void setId(final long id) {
		this.id = id;
	}

	//-------------------------------------------------------------------------------------------------
	public TokenHeader getHeader() {
		return header;
	}

	//-------------------------------------------------------------------------------------------------
	public void setHeader(final TokenHeader header) {
		this.header = header;
	}

	//-------------------------------------------------------------------------------------------------
	public String getType() {
		return type;
	}

	//-------------------------------------------------------------------------------------------------
	public void setType(String type) {
		this.type = type;
	}

	//-------------------------------------------------------------------------------------------------
	public ZonedDateTime getExpiresAt() {
		return expiresAt;
	}

	//-------------------------------------------------------------------------------------------------
	public void setExpiresAt(final ZonedDateTime expiresAt) {
		this.expiresAt = expiresAt;
	}
}