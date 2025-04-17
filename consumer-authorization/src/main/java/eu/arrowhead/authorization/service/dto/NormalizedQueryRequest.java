package eu.arrowhead.authorization.service.dto;

import java.util.List;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.enums.AuthorizationLevel;
import eu.arrowhead.dto.enums.AuthorizationTargetType;

public class NormalizedQueryRequest {

	//=================================================================================================
	// members

	private AuthorizationLevel level;
	private List<String> instanceIds;
	private List<String> cloudIdentifiers;
	private List<String> targetNames;
	private AuthorizationTargetType targetType;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public NormalizedQueryRequest() {
	}

	//-------------------------------------------------------------------------------------------------
	public boolean hasAnyOptionalFilter() {
		return !Utilities.isEmpty(instanceIds)
				|| !Utilities.isEmpty(cloudIdentifiers)
				|| !Utilities.isEmpty(targetNames)
				|| targetType != null;
	}

	//=================================================================================================
	// boilerplate code

	//-------------------------------------------------------------------------------------------------
	public AuthorizationLevel level() {
		return level;
	}

	//-------------------------------------------------------------------------------------------------
	public void setLevel(final AuthorizationLevel level) {
		this.level = level;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> instanceIds() {
		return instanceIds;
	}

	//-------------------------------------------------------------------------------------------------
	public void setInstanceIds(final List<String> instanceIds) {
		this.instanceIds = instanceIds;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> cloudIdentifiers() {
		return cloudIdentifiers;
	}

	//-------------------------------------------------------------------------------------------------
	public void setCloudIdentifiers(final List<String> cloudIdentifiers) {
		this.cloudIdentifiers = cloudIdentifiers;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> targetNames() {
		return targetNames;
	}

	//-------------------------------------------------------------------------------------------------
	public void setTargetNames(final List<String> targetNames) {
		this.targetNames = targetNames;
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationTargetType targetType() {
		return targetType;
	}

	//-------------------------------------------------------------------------------------------------
	public void setTargetType(final AuthorizationTargetType targetType) {
		this.targetType = targetType;
	}

	@Override
	public String toString() {
		return "NormalizedQueryRequest [level=" + level + ", instanceIds=" + instanceIds + ", cloudIdentifiers=" + cloudIdentifiers + ", targetNames=" + targetNames
				+ ", targetType=" + targetType + "]";
	}
}