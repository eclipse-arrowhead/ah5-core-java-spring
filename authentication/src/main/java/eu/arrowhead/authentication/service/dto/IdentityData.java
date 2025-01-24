package eu.arrowhead.authentication.service.dto;

import java.util.Map;

import eu.arrowhead.authentication.jpa.entity.System;

public record IdentityData(
		System system,
		Map<String, String> credentials) {
}