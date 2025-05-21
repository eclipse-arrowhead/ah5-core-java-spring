package eu.arrowhead.authorization.service.utils;

import java.security.PrivateKey;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authorization.AuthorizationSystemInfo;
import eu.arrowhead.authorization.jpa.entity.SelfContainedToken;
import eu.arrowhead.authorization.jpa.entity.TimeLimitedToken;
import eu.arrowhead.authorization.jpa.entity.TokenHeader;
import eu.arrowhead.authorization.jpa.entity.UsageLimitedToken;
import eu.arrowhead.authorization.jpa.service.SelfContainedTokenDbService;
import eu.arrowhead.authorization.jpa.service.TimeLimitedTokenDbService;
import eu.arrowhead.authorization.jpa.service.TokenHeaderDbService;
import eu.arrowhead.authorization.jpa.service.UsageLimitedTokenDbService;
import eu.arrowhead.authorization.service.dto.SelfContainedTokenPayload;
import eu.arrowhead.authorization.service.model.TokenModel;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import eu.arrowhead.dto.enums.AuthorizationTokenType;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;
import jakarta.annotation.Resource;

@Service
public class TokenEngine {

	//=================================================================================================
	// members

	@Autowired
	private AuthorizationSystemInfo sysInfo;

	@Autowired
	private TokenGenerator tokenGenerator;

	@Autowired
	private SecretCryptographer secretCryptographer;

	@Autowired
	private TokenHeaderDbService tokenHeaderDbService;

	@Autowired
	private UsageLimitedTokenDbService usageLimitedTokenDbService;

	@Autowired
	private TimeLimitedTokenDbService timeLimitedTokenDbService;

	@Autowired
	private SelfContainedTokenDbService selfContainedTokenDbService;

	@Resource(name = Constants.ARROWHEAD_CONTEXT)
	private Map<String, Object> arrowheadContext;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Pair<TokenModel, Boolean> produce(final String requesterSystem, final String consumerSystem, final ServiceInterfacePolicy tokenType, final String providerSystem, final AuthorizationTargetType targetType, final String serviceDefinition, final String operation,
			final String origin) {
		logger.debug("produce started...");

		return produce(requesterSystem, consumerSystem, null, tokenType, providerSystem, targetType, serviceDefinition, operation, null, null, origin);
	}

	//-------------------------------------------------------------------------------------------------
	public Pair<TokenModel, Boolean> produce(final String requesterSystem, final String consumerSystem, final String consumerCloud, final ServiceInterfacePolicy tokenType, final String providerSystem,
			 final AuthorizationTargetType targetType, final String serviceDefinition, final String operation, final Integer usageLimit, final ZonedDateTime expiry, final String origin) {
		logger.debug("produce started...");

		try {
			String rawToken = null;
			String hashedToken = null;
			final String cloud = Utilities.isEmpty(consumerCloud) ? Defaults.DEFAULT_CLOUD : consumerCloud;

			// SIMMPLE USAGE LIMITED TOKEN

			if (tokenType == ServiceInterfacePolicy.USAGE_LIMITED_TOKEN_AUTH) {
				rawToken = tokenGenerator.generateSimpleToken(sysInfo.getSimpleTokenByteSize());
				hashedToken = secretCryptographer.encryptHMACSHA256(rawToken, sysInfo.getSecretCryptographerKey());
				final Pair<UsageLimitedToken, Boolean> usageLimitTokenResult = usageLimitedTokenDbService.save(
						AuthorizationTokenType.fromServiceInterfacePolicy(tokenType),
						hashedToken,
						requesterSystem,
						cloud,
						consumerSystem,
						providerSystem,
						targetType,
						serviceDefinition,
						operation,
						usageLimit == null ? sysInfo.getSimpleTokenUsageLimit() : usageLimit);
				return Pair.of(new TokenModel(usageLimitTokenResult.getFirst(), rawToken), usageLimitTokenResult.getSecond());
			}

			// SIMPLE TIME LIMITED TOKEN

			if (tokenType == ServiceInterfacePolicy.TIME_LIMITED_TOKEN_AUTH) {
				rawToken = tokenGenerator.generateSimpleToken(sysInfo.getSimpleTokenByteSize());
				hashedToken = secretCryptographer.encryptHMACSHA256(rawToken, sysInfo.getSecretCryptographerKey());
				final ZonedDateTime expiresAt = expiry != null ? expiry : Utilities.utcNow().plusSeconds(sysInfo.getTokenTimeLimit());
				final Pair<TimeLimitedToken, Boolean> timeLimitTokenResult = timeLimitedTokenDbService.save(
						AuthorizationTokenType.fromServiceInterfacePolicy(tokenType),
						hashedToken,
						requesterSystem,
						cloud,
						consumerSystem,
						providerSystem,
						targetType,
						serviceDefinition,
						operation,
						expiresAt);
				return Pair.of(new TokenModel(timeLimitTokenResult.getFirst(), rawToken), timeLimitTokenResult.getSecond());
			}

			// SELF CONTAINED TOKENS

			// -- BASE64 SELF CONTAINED TOKEN
			final ZonedDateTime expiresAt = expiry != null ? expiry : Utilities.utcNow().plusSeconds(sysInfo.getTokenTimeLimit());

			final SelfContainedTokenPayload tokenPayload = new SelfContainedTokenPayload(providerSystem, consumerSystem, cloud, AuthorizationTargetType.SERVICE_DEF, serviceDefinition,
					operation);

			if (tokenType == ServiceInterfacePolicy.BASE64_SELF_CONTAINED_AUTH_TOKEN) {
				rawToken = tokenGenerator.generateBas64SelfSignedToken(expiresAt, tokenPayload);

			} else {
				//  -- JSON WEB TOKEN

				if (!arrowheadContext.containsKey(Constants.SERVER_PRIVATE_KEY)) {
					throw new InvalidParameterException("JWT is supported only when SSL is enabled.", origin);
				}

				final PrivateKey privateKey = (PrivateKey) arrowheadContext.get(Constants.SERVER_PRIVATE_KEY);

				if (tokenType == ServiceInterfacePolicy.RSA_SHA512_JSON_WEB_TOKEN_AUTH) {
					rawToken = tokenGenerator.generateJsonWebToken(AlgorithmIdentifiers.RSA_USING_SHA512, privateKey, expiresAt, tokenPayload);
				}
				if (tokenType == ServiceInterfacePolicy.RSA_SHA256_JSON_WEB_TOKEN_AUTH) {
					rawToken = tokenGenerator.generateJsonWebToken(AlgorithmIdentifiers.RSA_USING_SHA256, privateKey, expiresAt, tokenPayload);
				}
			}

			Assert.isTrue(!Utilities.isEmpty(rawToken), "Unhandled token type: " + tokenType);

			hashedToken = secretCryptographer.encryptHMACSHA256(rawToken, sysInfo.getSecretCryptographerKey());
			final Pair<SelfContainedToken, Boolean> selfContainedTokenResult = selfContainedTokenDbService.save(
					AuthorizationTokenType.fromServiceInterfacePolicy(tokenType),
					hashedToken,
					requesterSystem,
					cloud,
					consumerSystem,
					providerSystem,
					targetType,
					serviceDefinition,
					operation,
					tokenType.name(),
					expiresAt);
			return Pair.of(new TokenModel(selfContainedTokenResult.getFirst(), rawToken), selfContainedTokenResult.getSecond());

		} catch (final InvalidParameterException ex) {
			throw ex;

		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);

		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Token generation failed!", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public Pair<Boolean, TokenModel> verify(final String requesterSystem, final String rawToken, final String origin) {
		logger.debug("verify started...");

		String hashedToken = null;
		try {
			hashedToken = secretCryptographer.encryptHMACSHA256(rawToken, sysInfo.getSecretCryptographerKey());
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Token verification failed.", origin);
		}

		try {
			final Optional<TokenHeader> optional = tokenHeaderDbService.find(requesterSystem, hashedToken);
			if (optional.isEmpty()) {
				return Pair.of(false, null);
			}
			final TokenHeader tokenHeader = optional.get();

			if (tokenHeader.getTokenType() == AuthorizationTokenType.SELF_CONTAINED_TOKEN) {
				throw new InvalidParameterException("Self contained tokens can't be verified this way.", origin);
			}

			// USAGE LIMITED TOKEN
			if (tokenHeader.getTokenType() == AuthorizationTokenType.USAGE_LIMITED_TOKEN) {
				final UsageLimitedToken usageLimitedToken = usageLimitedTokenDbService.getByHeader(tokenHeader).get();
				final boolean verified = usageLimitedToken.getUsageLeft() > 0;
				if (verified) {
					usageLimitedTokenDbService.decrease(usageLimitedToken.getId());
				}
				return Pair.of(verified, new TokenModel(tokenHeader));
			}

			// TIME LIMITED TOKEN
			if (tokenHeader.getTokenType() == AuthorizationTokenType.TIME_LIMITED_TOKEN) {
				final TimeLimitedToken timeLimitedToken = timeLimitedTokenDbService.getByHeader(tokenHeader).get();
				final boolean verified = timeLimitedToken.getExpiresAt().isAfter(Utilities.utcNow());
				return Pair.of(verified, new TokenModel(tokenHeader));
			}

			throw new InternalServerError("Unhandled token type: " + tokenHeader.getTokenType());

		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}

	}

	//-------------------------------------------------------------------------------------------------
	public void revoke(final List<String> hashedTokens, final String origin) {
		logger.debug("revoke started...");

		if (Utilities.isEmpty(hashedTokens)) {
			return;
		}

		try {
			final List<TokenHeader> tokenHeaders = tokenHeaderDbService.findByTokenList(hashedTokens);
			tokenHeaderDbService.deleteById(tokenHeaders.stream().map((header) -> header.getId()).toList()); // Delete on cascade removes also the belonged token details 

		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public Page<TokenModel> query(final Pageable pagination, final String requester, final AuthorizationTokenType tokenType, final String consumerCloud, final String consumer, final String provider,
			final AuthorizationTargetType targetType, final String target, final String origin) {
		logger.debug("query started...");

		try {
			final List<TokenModel> results = new ArrayList<>();
			final Page<TokenHeader> tokenHeaderPage = tokenHeaderDbService.query(pagination, requester, tokenType, consumerCloud, consumer, provider, target, targetType);
			for (final TokenHeader tokenHeader : tokenHeaderPage) {
				// USAGE LIMITED TOKEN
				if (tokenHeader.getTokenType() == AuthorizationTokenType.USAGE_LIMITED_TOKEN) {
					final Optional<UsageLimitedToken> detailsOpt = usageLimitedTokenDbService.getByHeader(tokenHeader);
					results.add(detailsOpt.isEmpty() ? new TokenModel(tokenHeader) : new TokenModel(detailsOpt.get()));
				}

				// TIME LIMITED TOKEN
				if (tokenHeader.getTokenType() == AuthorizationTokenType.TIME_LIMITED_TOKEN) {
					final Optional<TimeLimitedToken> detailsOpt = timeLimitedTokenDbService.getByHeader(tokenHeader);
					results.add(detailsOpt.isEmpty() ? new TokenModel(tokenHeader) : new TokenModel(detailsOpt.get()));
				}

				// SELF CONTAINED TOKEN
				if (tokenHeader.getTokenType() == AuthorizationTokenType.SELF_CONTAINED_TOKEN) {
					final Optional<SelfContainedToken> detailsOpt = selfContainedTokenDbService.getByHeader(tokenHeader);
					results.add(detailsOpt.isEmpty() ? new TokenModel(tokenHeader) : new TokenModel(detailsOpt.get()));
				}
			}

			return new PageImpl<TokenModel>(results, pagination, tokenHeaderPage.getTotalElements());

		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}
}
