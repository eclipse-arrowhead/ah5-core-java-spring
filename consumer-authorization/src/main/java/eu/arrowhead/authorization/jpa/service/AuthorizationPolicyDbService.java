package eu.arrowhead.authorization.jpa.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import eu.arrowhead.authorization.jpa.entity.AuthPolicy;
import eu.arrowhead.authorization.jpa.entity.AuthProviderPolicyHeader;
import eu.arrowhead.authorization.jpa.repository.AuthPolicyRepository;
import eu.arrowhead.authorization.jpa.repository.AuthProviderPolicyHeaderRepository;
import eu.arrowhead.authorization.service.dto.NormalizedAuthorizationPolicyRequest;
import eu.arrowhead.authorization.service.dto.NormalizedGrantRequest;
import eu.arrowhead.authorization.service.utils.InstanceIdUtils;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.enums.AuthorizationLevel;

@Service
public class AuthorizationPolicyDbService {

	//=================================================================================================
	// members

	@Autowired
	private AuthProviderPolicyHeaderRepository providerHeaderRepository;

	@Autowired
	private AuthPolicyRepository authPolicyRepository;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public Pair<AuthProviderPolicyHeader, List<AuthPolicy>> createProviderLevelPolicy(final NormalizedGrantRequest request) {
		logger.debug("createProviderLevelPolicy started...");
		Assert.notNull(request, "request is null");

		try {
			// check existing record
			final Optional<AuthProviderPolicyHeader> existing = providerHeaderRepository.findByCloudAndProviderAndTargetTypeAndTarget(
					request.cloud(),
					request.provider(),
					request.targetType(),
					request.target());

			if (existing.isPresent()) {
				throw new InvalidParameterException("A policy is already existing for this cloud /provider / target type / target combination: "
						+ request.cloud() + " / "
						+ request.provider() + " / "
						+ request.targetType().name() + " / "
						+ request.target());
			}

			// create header

			AuthProviderPolicyHeader header = new AuthProviderPolicyHeader(
					InstanceIdUtils.calculateInstanceId(request.level(), request.cloud(), request.provider(), request.targetType(), request.target()),
					request.targetType(),
					request.cloud(),
					request.provider(),
					request.target(),
					request.description());

			header = providerHeaderRepository.saveAndFlush(header);
			final List<AuthPolicy> policies = createAuthPoliciesForProvider(header, request.policies());

			return Pair.of(header, policies);
		} catch (final InvalidParameterException ex) {
			throw ex;
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private List<AuthPolicy> createAuthPoliciesForProvider(final AuthProviderPolicyHeader header, final Map<String, NormalizedAuthorizationPolicyRequest> policies) {
		logger.debug("createAuthPolicies started...");

		final List<AuthPolicy> result = new ArrayList<>(policies.size());
		for (final Entry<String, NormalizedAuthorizationPolicyRequest> policyRequest : policies.entrySet()) {
			final String scope = policyRequest.getKey();
			final NormalizedAuthorizationPolicyRequest policy = policyRequest.getValue();
			result.add(new AuthPolicy(
					AuthorizationLevel.PROVIDER,
					header.getId(),
					scope,
					policy.policyType(),
					stringifyPolicy(policy)));
		}

		return authPolicyRepository.saveAllAndFlush(result);
	}

	//-------------------------------------------------------------------------------------------------
	private String stringifyPolicy(final NormalizedAuthorizationPolicyRequest policy) {
		logger.debug("stringifyPolicy started...");
		String result = null;

		switch (policy.policyType()) {
		case ALL:
			// intentionally does nothing
			break;
		case BLACKLIST:
		case WHITELIST:
			result = String.join(AuthPolicy.LIST_POLICY_DELIMITER, policy.policyList());
			break;
		case SYS_METADATA:
			result = Utilities.toJson(policy.policyMetadataRequirement());
			break;
		default:
			throw new InternalServerError("Unknown policy type: " + policy.policyType().name());
		}

		return result;
	}
}