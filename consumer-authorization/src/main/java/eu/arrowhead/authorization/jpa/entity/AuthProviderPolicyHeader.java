package eu.arrowhead.authorization.jpa.entity;

import java.util.List;

import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.jpa.UnmodifiableArrowheadEntity;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
public class AuthProviderPolicyHeader extends UnmodifiableArrowheadEntity {

	//=================================================================================================
	// members

	public static final List<String> SORTABLE_FIELDS_BY = List.of("id", "instanceId", "createdAt", "targetType", "cloud", "provider", "target", "createdBy");
	public static final String DEFAULT_SORT_FIELD = "instanceId";

	@Column(nullable = false, unique = true, length = VARCHAR_LARGE)
	private String instanceId;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private AuthorizationTargetType targetType;

	@Column(nullable = false, length = VARCHAR_MEDIUM)
	private String cloud = Defaults.DEFAULT_CLOUD;

	@Column(nullable = false, length = VARCHAR_SMALL)
	private String provider;

	@Column(nullable = false, length = VARCHAR_SMALL)
	private String target;

	@Column(nullable = true, length = VARCHAR_LARGE)
	private String description;

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

	//=================================================================================================
	// boilerplate code

	//-------------------------------------------------------------------------------------------------
	public String getInstanceId() {
		return instanceId;
	}

	//-------------------------------------------------------------------------------------------------
	public void setInstanceId(final String instanceId) {
		this.instanceId = instanceId;
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationTargetType getTargetType() {
		return targetType;
	}

	//-------------------------------------------------------------------------------------------------
	public void setTargetType(final AuthorizationTargetType targetType) {
		this.targetType = targetType;
	}

	//-------------------------------------------------------------------------------------------------
	public String getCloud() {
		return cloud;
	}

	//-------------------------------------------------------------------------------------------------
	public void setCloud(final String cloud) {
		this.cloud = cloud;
	}

	//-------------------------------------------------------------------------------------------------
	public String getProvider() {
		return provider;
	}

	//-------------------------------------------------------------------------------------------------
	public void setProvider(final String provider) {
		this.provider = provider;
	}

	//-------------------------------------------------------------------------------------------------
	public String getTarget() {
		return target;
	}

	//-------------------------------------------------------------------------------------------------
	public void setTarget(final String target) {
		this.target = target;
	}

	//-------------------------------------------------------------------------------------------------
	public String getDescription() {
		return description;
	}

	//-------------------------------------------------------------------------------------------------
	public void setDescription(final String description) {
		this.description = description;
	}
}