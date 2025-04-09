package eu.arrowhead.authorization.service.dto;

import java.util.List;

import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.enums.AuthorizationPolicyType;

public record NormalizedAuthorizationRequestPolicy(
		AuthorizationPolicyType policyType,
		List<String> policyList,
		MetadataRequirementDTO policyMetadataRequirement) {
}