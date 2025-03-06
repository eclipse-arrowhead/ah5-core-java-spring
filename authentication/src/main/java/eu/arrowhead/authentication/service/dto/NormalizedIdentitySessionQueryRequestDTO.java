package eu.arrowhead.authentication.service.dto;

import java.time.ZonedDateTime;

import org.springframework.data.domain.PageRequest;

public record NormalizedIdentitySessionQueryRequestDTO(
		PageRequest pageRequest,
		String namePart,
		ZonedDateTime loginFrom,
		ZonedDateTime loginTo) {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public boolean hasFilters() {
		return namePart != null
				|| loginFrom != null
				|| loginTo != null;
	}
}