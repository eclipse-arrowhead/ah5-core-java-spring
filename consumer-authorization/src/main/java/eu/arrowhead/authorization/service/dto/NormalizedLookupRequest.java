package eu.arrowhead.authorization.service.dto;

import java.util.List;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.enums.AuthorizationTargetType;

public class NormalizedLookupRequest {

	//=================================================================================================
	// members

	private String provider;
	private List<String> instanceIds;
	private List<String> cloudIdentifiers;
	private List<String> targetNames;
	private AuthorizationTargetType targetType;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public NormalizedLookupRequest() {
	}

	//-------------------------------------------------------------------------------------------------
	public boolean hasFilter() {
		return !Utilities.isEmpty(provider)
				|| !Utilities.isEmpty(instanceIds)
				|| !Utilities.isEmpty(cloudIdentifiers)
				|| !Utilities.isEmpty(targetNames)
				|| targetType != null;
	}

	//=================================================================================================
	// boilerplate code

	//-------------------------------------------------------------------------------------------------
	public String provider() {
		return provider;
	}

	//-------------------------------------------------------------------------------------------------
	public void setProvider(final String provider) {
		this.provider = provider;
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
		return "NormalizedLookupRequest [provider=" + provider + ", instanceIds=" + instanceIds + ", cloudIdentifiers=" + cloudIdentifiers + ", targetNames=" + targetNames
				+ ", targetType=" + targetType + "]";
	}
}