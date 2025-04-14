package eu.arrowhead.authorization.jpa.entity;

import java.util.List;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import jakarta.persistence.Entity;

@Entity
public class AuthProviderPolicyHeader extends AuthPolicyHeader {

	//=================================================================================================
	// members

	public static final List<String> SORTABLE_FIELDS_BY = List.of("id", "instanceId", "createdAt", "targetType", "cloud", "provider", "target", "createdBy");
	public static final String DEFAULT_SORT_FIELD = "instanceId";

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public AuthProviderPolicyHeader() {
	}

	//-------------------------------------------------------------------------------------------------
	public AuthProviderPolicyHeader(
			final String instanceId,
			final AuthorizationTargetType targetType,
			final String cloud,
			final String provider,
			final String target,
			final String description) {
		this.instanceId = instanceId;
		this.targetType = targetType;
		this.cloud = cloud;
		this.provider = provider;
		this.target = target;
		this.description = Utilities.isEmpty(description) ? null : description;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "AuthProviderPolicyHeader [instanceId=" + instanceId + ", targetType=" + targetType + ", cloud=" + cloud + ", provider=" + provider + ", target=" + target
				+ ", description=" + description + ", id=" + id + ", createdAt=" + createdAt + "]";
	}
}