package eu.arrowhead.authentication.service.dto;

import java.util.Map;

import eu.arrowhead.dto.enums.AuthenticationMethod;

public record NormalizedIdentityMgmtRequestDTO(
		String systemName,
		AuthenticationMethod authenticationMethod,
		Map<String, String> credentials,
		boolean sysop) {
}