package eu.arrowhead.authorization.jpa.entity;

import java.time.ZonedDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@Entity
public class TimeLimitedToken {

	//=================================================================================================
	// members

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@OneToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "headerId", referencedColumnName = "id", nullable = false)
	private TokenHeader header;

	@Column(nullable = false)
	protected ZonedDateTime expiresAt;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public TimeLimitedToken() {

	}

	//-------------------------------------------------------------------------------------------------
	public TimeLimitedToken(
			final TokenHeader header,
			final ZonedDateTime expiresAt) {
		this.header = header;
		this.expiresAt = expiresAt;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "TimeLimitedToken [id=" + id + ", header=" + header + ", expiresAt=" + expiresAt + "]";
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
	public ZonedDateTime getExpiresAt() {
		return expiresAt;
	}

	//-------------------------------------------------------------------------------------------------
	public void setExpiresAt(final ZonedDateTime expiresAt) {
		this.expiresAt = expiresAt;
	}
}