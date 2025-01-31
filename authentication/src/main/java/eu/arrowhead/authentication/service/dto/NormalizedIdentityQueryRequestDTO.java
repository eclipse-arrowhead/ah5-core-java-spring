package eu.arrowhead.authentication.service.dto;

import java.time.ZonedDateTime;

import eu.arrowhead.dto.PageDTO;

public record NormalizedIdentityQueryRequestDTO(
		PageDTO pagination,
		String namePart,
		Boolean isSysop,
		String createdBy,
		ZonedDateTime creationFrom,
		ZonedDateTime creationTo,
		Boolean hasSession) {
}