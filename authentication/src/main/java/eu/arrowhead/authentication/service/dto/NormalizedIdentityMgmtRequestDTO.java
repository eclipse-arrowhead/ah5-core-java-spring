package eu.arrowhead.authentication.service.dto;

import java.util.Map;

public record NormalizedIdentityMgmtRequestDTO(
		String systemName,
		Map<String, String> credentials,
		boolean sysop) {
}