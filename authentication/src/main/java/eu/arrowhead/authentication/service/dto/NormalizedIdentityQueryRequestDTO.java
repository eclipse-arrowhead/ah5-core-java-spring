package eu.arrowhead.authentication.service.dto;

import java.time.ZonedDateTime;

import org.springframework.data.domain.PageRequest;

public record NormalizedIdentityQueryRequestDTO(
		PageRequest pageRequest,
		String namePart,
		Boolean isSysop,
		String createdBy,
		ZonedDateTime creationFrom,
		ZonedDateTime creationTo,
		Boolean hasSession) {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public boolean hasFilters() {
		return namePart != null
				|| isSysop != null
				|| createdBy != null
				|| creationFrom != null
				|| creationTo != null
				|| hasSession != null;
	}
}