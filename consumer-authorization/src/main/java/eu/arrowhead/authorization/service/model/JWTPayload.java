package eu.arrowhead.authorization.service.model;

import eu.arrowhead.dto.enums.AuthorizationTargetType;

public record JWTPayload(
				String provider,
				String consumer,
				String cloud,
				AuthorizationTargetType targetType,
				String target,
				String scope) {

}
