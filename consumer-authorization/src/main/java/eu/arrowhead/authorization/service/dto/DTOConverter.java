package eu.arrowhead.authorization.service.dto;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authorization.jpa.entity.AuthPolicy;
import eu.arrowhead.authorization.jpa.entity.AuthProviderPolicyHeader;
import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.AuthorizationPolicyDTO;
import eu.arrowhead.dto.AuthorizationPolicyListResponseDTO;
import eu.arrowhead.dto.AuthorizationPolicyResponseDTO;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.enums.AuthorizationLevel;

@Service
public class DTOConverter {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public AuthorizationPolicyListResponseDTO convertProviderLevelPolicyPageToResponse(final Page<Pair<AuthProviderPolicyHeader, List<AuthPolicy>>> page) {
		logger.debug("convertProviderLevelPolicyToResponse started...");
		Assert.notNull(page, "page is null");

		final List<AuthorizationPolicyResponseDTO> convertedList = page
				.stream()
				.map(e -> convertProviderLevelPolicyToResponse(e))
				.toList();

		return new AuthorizationPolicyListResponseDTO(convertedList, page.getTotalElements());
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationPolicyResponseDTO convertProviderLevelPolicyToResponse(final Pair<AuthProviderPolicyHeader, List<AuthPolicy>> entities) {
		logger.debug("convertProviderLevelPolicyToResponse started...");
		Assert.notNull(entities, "entities is null");
		Assert.notNull(entities.getFirst(), "header is null");
		Assert.isTrue(!Utilities.isEmpty(entities.getSecond()), "list is missing");

		final AuthProviderPolicyHeader header = entities.getFirst();
		final List<AuthPolicy> policies = entities.getSecond();

		AuthorizationPolicyDTO defaultPolicy = null;
		final Map<String, AuthorizationPolicyDTO> scopedPolicies = policies.size() > 1 ? new HashMap<>(policies.size() - 1) : null;
		for (final AuthPolicy policy : policies) {
			if (Defaults.DEFAULT_AUTHORIZATION_SCOPE.equals(policy.getScope())) {
				// this is the default policy
				defaultPolicy = convertAuthPolicyToDTO(policy);
			} else {
				scopedPolicies.put(policy.getScope(), convertAuthPolicyToDTO(policy));
			}
		}

		return new AuthorizationPolicyResponseDTO(
				header.getInstanceId(),
				AuthorizationLevel.PROVIDER,
				header.getCloud(),
				header.getProvider(),
				header.getTargetType(),
				header.getTarget(),
				header.getDescription(),
				defaultPolicy,
				scopedPolicies,
				header.getProvider(),
				Utilities.convertZonedDateTimeToUTCString(header.getCreatedAt()));
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private AuthorizationPolicyDTO convertAuthPolicyToDTO(final AuthPolicy policy) {
		logger.debug("convertAuthPolicyToDTO started...");

		List<String> list = null;
		MetadataRequirementDTO metadataRequirement = null;

		switch (policy.getPolicyType()) {
		case ALL:
			// intentionally does nothing
			break;
		case BLACKLIST:
		case WHITELIST:
			list = Stream
					.of(policy.getPolicy().split(AuthPolicy.LIST_POLICY_DELIMITER))
					.map(String::trim)
					.toList();
			break;
		case SYS_METADATA:
			metadataRequirement = Utilities.fromJson(policy.getPolicy(), MetadataRequirementDTO.class);
			break;
		default:
			throw new InvalidParameterException("Unknown policy type: " + policy.getPolicyType().name());
		}

		return new AuthorizationPolicyDTO(
				policy.getScope(),
				policy.getPolicyType(),
				list,
				metadataRequirement);
	}
}