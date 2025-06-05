package eu.arrowhead.authorization.service.dto;

import eu.arrowhead.dto.enums.AuthorizationTargetType;

public record SelfContainedTokenPayload(
		String provider,
		String consumer,
		String cloud,
		AuthorizationTargetType targetType,
		String target,
		String scope) {
}