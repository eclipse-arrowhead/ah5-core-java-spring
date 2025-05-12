package eu.arrowhead.authorization.jpa.entity;

import java.util.List;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class AuthMgmtPolicyHeader extends AuthPolicyHeader {

	//=================================================================================================
	// members

	public static final List<String> SORTABLE_FIELDS_BY = List.of("id", "instanceId", "createdAt", "targetType", "cloud", "provider", "target", "createdBy");

	@Column(nullable = false, length = VARCHAR_SMALL)
	private String createdBy;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public AuthMgmtPolicyHeader() {
	}

	//-------------------------------------------------------------------------------------------------
	public AuthMgmtPolicyHeader(
			final String instanceId,
			final AuthorizationTargetType targetType,
			final String cloud,
			final String provider,
			final String target,
			final String description,
			final String createdBy) {
		this.instanceId = instanceId;
		this.targetType = targetType;
		this.cloud = cloud;
		this.provider = provider;
		this.target = target;
		this.description = Utilities.isEmpty(description) ? null : description;
		this.createdBy = createdBy;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "AuthMgmtPolicyHeader [instanceId=" + instanceId + ", targetType=" + targetType + ", cloud=" + cloud + ", provider=" + provider + ", target=" + target + ", description=" + description
				+ ", createdBy=" + createdBy + ", id=" + id + ", createdAt=" + createdAt + "]";
	}

	//=================================================================================================
	// boilerplate code

	//-------------------------------------------------------------------------------------------------
	public String getCreatedBy() {
		return createdBy;
	}

	//-------------------------------------------------------------------------------------------------
	public void setCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
	}
}