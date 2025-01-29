package eu.arrowhead.authentication.jpa.entity;

import java.util.List;

import eu.arrowhead.common.jpa.ArrowheadEntity;
import eu.arrowhead.dto.enums.AuthenticationMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "system_")
public class System extends ArrowheadEntity {

	//=================================================================================================
	// members

	public static final List<String> SORTABLE_FIELDS_BY = List.of("id", "name", "createdAt");
	public static final String DEFAULT_SORT_FIELD = "createdAt";

	@Column(nullable = false, unique = true, length = VARCHAR_SMALL)
	private String name;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private AuthenticationMethod authenticationMethod;

	@Column(nullable = false, columnDefinition = "INT(1)")
	private boolean sysop = false;

	@Column(length = VARCHAR_LARGE)
	private String extra;

	@Column(nullable = false, length = VARCHAR_SMALL)
	private String createdBy;

	@Column(nullable = false, length = VARCHAR_SMALL)
	private String updatedBy;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public System() {
	}

	//-------------------------------------------------------------------------------------------------
	public System(final String name, final AuthenticationMethod authenticationMethod, final boolean sysop, final String createdBy) {
		this.name = name;
		this.authenticationMethod = authenticationMethod;
		this.sysop = sysop;
		this.createdBy = createdBy;
		this.updatedBy = createdBy; // intentional
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "System [name=" + name + ", authenticationMethod=" + authenticationMethod + ", sysop=" + sysop + ",extra=" + extra + ", createdBy=" + createdBy + ", updatedBy=" + updatedBy + "]";
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

	//-------------------------------------------------------------------------------------------------
	public AuthenticationMethod getAuthenticationMethod() {
		return authenticationMethod;
	}

	//-------------------------------------------------------------------------------------------------
	public void setAuthenticationMethod(final AuthenticationMethod authenticationMethod) {
		this.authenticationMethod = authenticationMethod;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isSysop() {
		return sysop;
	}

	//-------------------------------------------------------------------------------------------------
	public void setSysop(final boolean sysop) {
		this.sysop = sysop;
	}

	//-------------------------------------------------------------------------------------------------
	public String getCreatedBy() {
		return createdBy;
	}

	//-------------------------------------------------------------------------------------------------
	public void setCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
	}

	//-------------------------------------------------------------------------------------------------
	public String getUpdatedBy() {
		return updatedBy;
	}

	//-------------------------------------------------------------------------------------------------
	public void setUpdatedBy(final String updatedBy) {
		this.updatedBy = updatedBy;
	}

	//-------------------------------------------------------------------------------------------------
	public String getExtra() {
		return extra;
	}

	//-------------------------------------------------------------------------------------------------
	public void setExtra(final String extra) {
		this.extra = extra;
	}
}