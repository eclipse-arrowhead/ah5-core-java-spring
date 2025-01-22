package eu.arrowhead.authentication.service;

import eu.arrowhead.authentication.jpa.entity.System;
import eu.arrowhead.dto.IdentityLoginResponseDTO;
import eu.arrowhead.dto.IdentityRequestDTO;

public record IdentityLoginData(
		IdentityRequestDTO normalizedRequest,
		System system,
		IdentityLoginResponseDTO response) {

}
