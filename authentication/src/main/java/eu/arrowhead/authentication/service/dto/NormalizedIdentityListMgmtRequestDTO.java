package eu.arrowhead.authentication.service.dto;

import java.util.List;

import eu.arrowhead.dto.enums.AuthenticationMethod;

public record NormalizedIdentityListMgmtRequestDTO(
		AuthenticationMethod authenticationMethod,
		List<NormalizedIdentityMgmtRequestDTO> identities) {
}