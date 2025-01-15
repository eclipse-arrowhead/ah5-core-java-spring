package eu.arrowhead.authentication.jpa.entity;

import eu.arrowhead.common.jpa.ArrowheadEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class PasswordAuthentication {

	//=================================================================================================
	// members

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	protected long id;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "systemId", referencedColumnName = "id", nullable = false)
	private System system;

	@Column(nullable = false, length = ArrowheadEntity.VARCHAR_SMALL)
	private String password;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public PasswordAuthentication() {
	}

	//-------------------------------------------------------------------------------------------------
	public PasswordAuthentication(final System system, final String password) {
		this.system = system;
		this.password = password;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "PasswordAuthentication [id=" + id + ", system=" + system + "]";
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
	public System getSystem() {
		return system;
	}

	//-------------------------------------------------------------------------------------------------
	public void setSystem(final System system) {
		this.system = system;
	}

	//-------------------------------------------------------------------------------------------------
	public String getPassword() {
		return password;
	}

	//-------------------------------------------------------------------------------------------------
	public void setPassword(final String password) {
		this.password = password;
	}
}