package eu.arrowhead.authentication.service;

import eu.arrowhead.dto.IdentityLoginResponseDTO;
import eu.arrowhead.dto.IdentityRequestDTO;

public record IdentityLoginData(
		IdentityRequestDTO normalizedRequest,
		IdentityLoginResponseDTO response) {

}
