package eu.arrowhead.authentication.service.dto;

import java.util.List;

import eu.arrowhead.authentication.method.IAuthenticationMethod;

public record NormalizedIdentityListMgmtRequestDTO(
		IAuthenticationMethod authenticationMethod,
		List<NormalizedIdentityMgmtRequestDTO> identities) {
}