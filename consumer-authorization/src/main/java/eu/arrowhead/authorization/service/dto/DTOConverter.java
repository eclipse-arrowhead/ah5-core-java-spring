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

import eu.arrowhead.authorization.jpa.entity.AuthMgmtPolicyHeader;
import eu.arrowhead.authorization.jpa.entity.AuthPolicy;
import eu.arrowhead.authorization.jpa.entity.AuthPolicyHeader;
import eu.arrowhead.authorization.jpa.entity.AuthProviderPolicyHeader;
import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.AuthorizationPolicyDTO;
import eu.arrowhead.dto.AuthorizationPolicyListResponseDTO;
import eu.arrowhead.dto.AuthorizationPolicyResponseDTO;
import eu.arrowhead.dto.AuthorizationVerifyListResponseDTO;
import eu.arrowhead.dto.AuthorizationVerifyResponseDTO;
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
		logger.debug("convertProviderLevelPolicyPageToResponse started...");
		Assert.notNull(page, "page is null");

		final List<AuthorizationPolicyResponseDTO> convertedList = page
				.stream()
				.map(e -> convertPolicyToResponse(AuthorizationLevel.PROVIDER, e))
				.toList();

		return new AuthorizationPolicyListResponseDTO(convertedList, page.getTotalElements());
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationPolicyListResponseDTO convertMgmtLevelPolicyListToResponse(final List<Pair<AuthMgmtPolicyHeader, List<AuthPolicy>>> policies) {
		logger.debug("convertMgmtLevelPolicyListToResponse started...");
		Assert.isTrue(!Utilities.isEmpty(policies), "policies is missing");
		Assert.isTrue(!Utilities.containsNull(policies), "policies list contains null element");

		final List<AuthorizationPolicyResponseDTO> convertedList = policies
				.stream()
				.map(e -> convertPolicyToResponse(AuthorizationLevel.MGMT, e))
				.toList();

		return new AuthorizationPolicyListResponseDTO(convertedList, policies.size());
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationPolicyListResponseDTO convertMgmtLevelPolicyPageToResponse(final Page<Pair<AuthMgmtPolicyHeader, List<AuthPolicy>>> page) {
		logger.debug("convertMgmtLevelPolicyPageToResponse started...");
		Assert.notNull(page, "page is null");

		final List<AuthorizationPolicyResponseDTO> convertedList = page
				.stream()
				.map(e -> convertPolicyToResponse(AuthorizationLevel.MGMT, e))
				.toList();

		return new AuthorizationPolicyListResponseDTO(convertedList, page.getTotalElements());
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationPolicyResponseDTO convertPolicyToResponse(final AuthorizationLevel level, final Pair<? extends AuthPolicyHeader, List<AuthPolicy>> entities) {
		logger.debug("convertPolicyToResponse started...");
		Assert.notNull(level, "level is null");
		Assert.notNull(entities, "entities is null");
		Assert.notNull(entities.getFirst(), "header is null");
		Assert.isTrue(!Utilities.isEmpty(entities.getSecond()), "list is missing");

		final AuthPolicyHeader header = entities.getFirst();
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

		final String createdBy = level == AuthorizationLevel.PROVIDER ? header.getProvider() : ((AuthMgmtPolicyHeader) header).getCreatedBy();

		return new AuthorizationPolicyResponseDTO(
				header.getInstanceId(),
				level,
				header.getCloud(),
				header.getProvider(),
				header.getTargetType(),
				header.getTarget(),
				header.getDescription(),
				defaultPolicy,
				scopedPolicies,
				createdBy,
				Utilities.convertZonedDateTimeToUTCString(header.getCreatedAt()));
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationPolicyDTO convertAuthPolicyToDTO(final AuthPolicy policy) {
		logger.debug("convertAuthPolicyToDTO started...");
		Assert.notNull(policy, "policy is null");

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

	//-------------------------------------------------------------------------------------------------
	public AuthorizationVerifyListResponseDTO convertCheckResultListToResponse(final List<Pair<NormalizedVerifyRequest, Boolean>> result) {
		logger.debug("convertCheckResultListToResponse started...");
		Assert.notNull(result, "result is null");

		final List<AuthorizationVerifyResponseDTO> convertedList = result
				.stream()
				.map(e -> convertCheckResultToResponse(e.getFirst(), e.getSecond()))
				.toList();

		return new AuthorizationVerifyListResponseDTO(convertedList, result.size());
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private AuthorizationVerifyResponseDTO convertCheckResultToResponse(final NormalizedVerifyRequest input, final boolean output) {
		logger.debug("convertCheckResultToResponse started...");

		return new AuthorizationVerifyResponseDTO(
				input.provider(),
				input.consumer(),
				input.cloud(),
				input.targetType(),
				input.target(),
				input.scope(),
				output);
	}
}