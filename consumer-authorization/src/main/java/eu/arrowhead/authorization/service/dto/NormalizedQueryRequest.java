package eu.arrowhead.authorization.service.dto;

import eu.arrowhead.dto.enums.AuthorizationLevel;

public class NormalizedQueryRequest extends NormalizedLookupRequest {

	//=================================================================================================
	// members

	private AuthorizationLevel level;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public NormalizedQueryRequest() {
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public boolean hasFilter() {
		return true; // since level is a mandatory filter
	}

	//-------------------------------------------------------------------------------------------------
	public boolean hasAnyOptionalFilter() {
		return super.hasFilter();
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "NormalizedQueryRequest [level=" + level + ", providers=" + providers + ", instanceIds=" + instanceIds + ", cloudIdentifiers=" + cloudIdentifiers + ", targetNames=" + targetNames
				+ ", targetType=" + targetType + "]";
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
}