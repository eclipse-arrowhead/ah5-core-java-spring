package eu.arrowhead.authorization.jpa.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
import eu.arrowhead.authorization.service.dto.NormalizedLookupRequest;
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

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public boolean deleteProviderLevelPolicyByInstanceId(final String instanceId) {
		logger.debug("deleteProviderLevelPolicyByInstanceId started");
		Assert.isTrue(!Utilities.isEmpty(instanceId), "instanceId is empty");

		try {
			final Optional<AuthProviderPolicyHeader> headerOpt = providerHeaderRepository.findByInstanceId(instanceId);
			if (headerOpt.isPresent()) {
				final AuthProviderPolicyHeader header = headerOpt.get();

				providerHeaderRepository.delete(header);
				providerHeaderRepository.flush();
				authPolicyRepository.deleteByHeaderId(header.getId());
				authPolicyRepository.flush();

				return true;
			}

			return false;
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public Page<Pair<AuthProviderPolicyHeader, List<AuthPolicy>>> getProviderLevelPoliciesByFilters(final Pageable pagination, final NormalizedLookupRequest request) {
		logger.debug("getProviderLevelPoliciesByFilters started...");
		Assert.notNull(pagination, "pagination is null");

		try {
			final boolean hasFilter = request != null && request.hasFilter();

			return hasFilter ? findProviderLevelPoliciesByFilters(pagination, request) : findAllProviderLevelPolicies(pagination);
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

	//-------------------------------------------------------------------------------------------------
	private Page<Pair<AuthProviderPolicyHeader, List<AuthPolicy>>> findAllProviderLevelPolicies(final Pageable pagination) {
		logger.debug("findAllProviderLevelPolicies started...");

		final Page<AuthProviderPolicyHeader> headers = providerHeaderRepository.findAll(pagination);
		final List<Pair<AuthProviderPolicyHeader, List<AuthPolicy>>> entries = headers
				.stream()
				.map(h -> Pair.of(
						h,
						authPolicyRepository.findByHeaderId(h.getId())))
				.toList();

		return new PageImpl<>(
				entries,
				pagination,
				headers.getTotalElements());
	}

	//-------------------------------------------------------------------------------------------------
	private Page<Pair<AuthProviderPolicyHeader, List<AuthPolicy>>> findProviderLevelPoliciesByFilters(final Pageable pagination, final NormalizedLookupRequest request) {
		logger.debug("findProviderLevelPoliciesByFilters started...");
		Assert.notNull(request, "request is null");

		ProviderLevelBaseFilter baseFilter = ProviderLevelBaseFilter.NONE;
		List<AuthProviderPolicyHeader> toFilter = new ArrayList<>();

		if (!Utilities.isEmpty(request.instanceIds())) {
			toFilter = providerHeaderRepository.findByInstanceIdIn(request.instanceIds());
			baseFilter = ProviderLevelBaseFilter.INSTANCE_ID;
		} else if (!Utilities.isEmpty(request.targetNames())) {
			toFilter = providerHeaderRepository.findByTargetIn(request.targetNames());
			baseFilter = ProviderLevelBaseFilter.TARGET;
		} else if (!Utilities.isEmpty(request.cloudIdentifiers())) {
			toFilter = providerHeaderRepository.findByCloudIn(request.cloudIdentifiers());
			baseFilter = ProviderLevelBaseFilter.CLOUD;
		} else {
			toFilter = providerHeaderRepository.findAll();
		}

		final Set<Long> matchingIds = new HashSet<>();
		final Map<Long, List<AuthPolicy>> authPolicyMap = new HashMap<>();

		for (final AuthProviderPolicyHeader header : toFilter) {
			// Match against instance id requirements
			if (baseFilter != ProviderLevelBaseFilter.INSTANCE_ID && !Utilities.isEmpty(request.instanceIds()) && !request.instanceIds().contains(header.getInstanceId())) {
				continue;
			}

			// Match against target requirements
			if (baseFilter != ProviderLevelBaseFilter.TARGET && !Utilities.isEmpty(request.targetNames()) && !request.targetNames().contains(header.getTarget())) {
				continue;
			}

			// Match against cloud requirements
			if (baseFilter != ProviderLevelBaseFilter.CLOUD && !Utilities.isEmpty(request.cloudIdentifiers()) && !request.cloudIdentifiers().contains(header.getCloud())) {
				continue;
			}

			// Match against provider requirement
			if (!Utilities.isEmpty(request.provider()) && !request.provider().equals(header.getProvider())) {
				continue;
			}

			// Match against target type requirement
			if (request.targetType() != null && request.targetType() != header.getTargetType()) {
				continue;
			}

			final List<AuthPolicy> policies = authPolicyRepository.findByHeaderId(header.getId());
			if (!Utilities.isEmpty(policies)) {
				matchingIds.add(header.getId());
				authPolicyMap.put(header.getId(), policies);
			}
		}

		final Page<AuthProviderPolicyHeader> page = providerHeaderRepository.findAllByIdIn(matchingIds, pagination);
		final List<Pair<AuthProviderPolicyHeader, List<AuthPolicy>>> entries = page
				.stream()
				.map(h -> Pair.of(
						h,
						authPolicyMap.get(h.getId())))
				.toList();

		return new PageImpl<>(
				entries,
				pagination,
				page.getTotalElements());
	}

	//=================================================================================================
	// nested structures

	//-------------------------------------------------------------------------------------------------
	private enum ProviderLevelBaseFilter {
		NONE, INSTANCE_ID, CLOUD, TARGET
	}
}