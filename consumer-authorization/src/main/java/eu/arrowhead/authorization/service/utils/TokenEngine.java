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
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.AuthorizationTokenMgmtListResponseDTO;
import eu.arrowhead.dto.AuthorizationTokenResponseDTO;
import eu.arrowhead.dto.AuthorizationTokenVerifyResponseDTO;
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
	public Pair<AuthorizationTokenResponseDTO, Boolean> produce(final String requesterSystem, final String consumerSystem, final ServiceInterfacePolicy tokenType, final String providerSystem, final String serviceDefinition, final String operation,
			final String origin) {
		logger.debug("produce started...");

		return produce(requesterSystem, consumerSystem, null, tokenType, providerSystem, serviceDefinition, operation, null, null, origin);
	}

	//-------------------------------------------------------------------------------------------------
	public Pair<AuthorizationTokenResponseDTO, Boolean> produce(final String requesterSystem, final String consumerSystem, final String consumerCloud, final ServiceInterfacePolicy tokenType, final String providerSystem,
			final String serviceDefinition, final String operation, final Integer usageLimit, final ZonedDateTime expiry, final String origin) {
		logger.debug("produce started...");

		try {
			String token = null;
			String encryptedToSaveHMAC = null;
			final String cloud = Utilities.isEmpty(consumerCloud) ? Defaults.DEFAULT_CLOUD : consumerCloud;

			// SIMMPLE USAGE LIMITED TOKEN

			if (tokenType == ServiceInterfacePolicy.USAGE_LIMITED_TOKEN_AUTH) {
				token = tokenGenerator.generateSimpleToken(sysInfo.getSimpleTokenByteSize());
				encryptedToSaveHMAC = secretCryptographer.encryptHMACSHA256(token, sysInfo.getSecretCryptographerKey());
				final Pair<UsageLimitedToken, Boolean> usageLimitTokenResult = usageLimitedTokenDbService.save(
						AuthorizationTokenType.fromServiceInterfacePolicy(tokenType),
						encryptedToSaveHMAC,
						requesterSystem,
						cloud,
						consumerSystem,
						providerSystem,
						serviceDefinition,
						operation,
						usageLimit == null ? sysInfo.getSimpleTokenUsageLimit() : usageLimit);
				return Pair.of(new AuthorizationTokenResponseDTO(
						AuthorizationTokenType.fromServiceInterfacePolicy(tokenType),
						null,
						token,
						requesterSystem,
						cloud,
						consumerSystem,
						providerSystem,
						serviceDefinition,
						operation,
						Utilities.convertZonedDateTimeToUTCString(usageLimitTokenResult.getFirst().getHeader().getCreatedAt()),
						usageLimitTokenResult.getFirst().getUsageLimit(),
						usageLimitTokenResult.getFirst().getUsageLeft(),
						null),
						usageLimitTokenResult.getSecond());
			}

			// SIMPLE TIME LIMITED TOKEN

			if (tokenType == ServiceInterfacePolicy.TIME_LIMITED_TOKEN_AUTH) {
				token = tokenGenerator.generateSimpleToken(sysInfo.getSimpleTokenByteSize());
				encryptedToSaveHMAC = secretCryptographer.encryptHMACSHA256(token, sysInfo.getSecretCryptographerKey());
				final ZonedDateTime expiresAt = expiry != null ? expiry : Utilities.utcNow().plusSeconds(sysInfo.getTokenTimeLimit());
				final Pair<TimeLimitedToken, Boolean> timeLimitTokenResult = timeLimitedTokenDbService.save(
						AuthorizationTokenType.fromServiceInterfacePolicy(tokenType),
						encryptedToSaveHMAC,
						requesterSystem,
						cloud,
						consumerSystem,
						providerSystem,
						serviceDefinition,
						operation,
						expiresAt);
				return Pair.of(new AuthorizationTokenResponseDTO(
						AuthorizationTokenType.fromServiceInterfacePolicy(tokenType),
						null,
						token,
						requesterSystem,
						cloud,
						consumerSystem,
						providerSystem,
						serviceDefinition,
						operation,
						Utilities.convertZonedDateTimeToUTCString(timeLimitTokenResult.getFirst().getHeader().getCreatedAt()),
						null,
						null,
						Utilities.convertZonedDateTimeToUTCString(timeLimitTokenResult.getFirst().getExpiresAt())),
						timeLimitTokenResult.getSecond());
			}

			// SELF CONTAINED TOKENS

			Pair<String, String> encryptedToSaveAES = null;

			// -- BASE64 SELF CONTAINED TOKEN
			final ZonedDateTime expiresAt = expiry != null ? expiry : Utilities.utcNow().plusSeconds(sysInfo.getTokenTimeLimit());

			final SelfContainedTokenPayload tokenPayload = new SelfContainedTokenPayload(providerSystem, consumerSystem, cloud, AuthorizationTargetType.SERVICE_DEF, serviceDefinition,
					operation);

			if (tokenType == ServiceInterfacePolicy.BASE64_SELF_CONTAINED_AUTH_TOKEN) {
				token = tokenGenerator.generateBas64SelfSignedToken(expiresAt, tokenPayload);

			} else {
				//  -- JSON WEB TOKEN

				if (!arrowheadContext.containsKey(Constants.SERVER_PRIVATE_KEY)) {
					throw new InvalidParameterException("JWT is supported only when SSL is enabled.", origin);
				}

				final PrivateKey privateKey = (PrivateKey) arrowheadContext.get(Constants.SERVER_PRIVATE_KEY);

				if (tokenType == ServiceInterfacePolicy.RSA_SHA512_JSON_WEB_TOKEN_AUTH) {
					token = tokenGenerator.generateJsonWebToken(AlgorithmIdentifiers.RSA_USING_SHA512, privateKey, expiresAt, tokenPayload);
				}
				if (tokenType == ServiceInterfacePolicy.RSA_SHA256_JSON_WEB_TOKEN_AUTH) {
					token = tokenGenerator.generateJsonWebToken(AlgorithmIdentifiers.RSA_USING_SHA256, privateKey, expiresAt, tokenPayload);
				}
			}

			Assert.isTrue(!Utilities.isEmpty(token), "Unhandled token type: " + tokenType);

			encryptedToSaveAES = secretCryptographer.encryptAESCBCPKCS5P(token, sysInfo.getSecretCryptographerKey());
			final Pair<SelfContainedToken, Boolean> selfContainedTokenResult = selfContainedTokenDbService.save(
					AuthorizationTokenType.fromServiceInterfacePolicy(tokenType),
					encryptedToSaveAES.getFirst(),
					encryptedToSaveAES.getSecond(),
					requesterSystem,
					cloud,
					consumerSystem,
					providerSystem,
					serviceDefinition,
					operation,
					tokenType.name(),
					expiresAt);
			return Pair.of(new AuthorizationTokenResponseDTO(
					AuthorizationTokenType.fromServiceInterfacePolicy(tokenType),
					tokenType.name(),
					token,
					requesterSystem,
					cloud,
					consumerSystem,
					providerSystem,
					serviceDefinition,
					operation,
					Utilities.convertZonedDateTimeToUTCString(selfContainedTokenResult.getFirst().getHeader().getCreatedAt()),
					null,
					null,
					Utilities.convertZonedDateTimeToUTCString(selfContainedTokenResult.getFirst().getExpiresAt())),
					selfContainedTokenResult.getSecond());

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
	public AuthorizationTokenVerifyResponseDTO verify(final String requesterSystem, final String token, final String origin) {
		logger.debug("verify started...");

		String tokenAsSaved = null;
		try {
			// Only the not self contained token can be verified this way and those are encrypted with HMAC (to not to have auxiliary, otherwise we could not find it)
			tokenAsSaved = secretCryptographer.encryptHMACSHA256(token, sysInfo.getSecretCryptographerKey());
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Token verification failed.", origin);
		}

		try {
			final Optional<TokenHeader> optional = tokenHeaderDbService.find(requesterSystem, tokenAsSaved);
			if (optional.isEmpty()) {
				return new AuthorizationTokenVerifyResponseDTO(false, null, null, null, null);
			}
			final TokenHeader tokenHeader = optional.get();

			if (tokenHeader.getTokenType() == AuthorizationTokenType.SELF_CONTAINED_TOKEN) {
				// Cannot happen, but who knows...
				throw new InvalidParameterException("Self contained tokens can't be verified this way.", origin);
			}

			// USAGE LIMITED TOKEN
			if (tokenHeader.getTokenType() == AuthorizationTokenType.USAGE_LIMITED_TOKEN) {
				final UsageLimitedToken usageLimitedToken = usageLimitedTokenDbService.getByHeader(tokenHeader).get();
				final boolean verified = usageLimitedToken.getUsageLeft() > 0;
				if (verified) {
					usageLimitedTokenDbService.decrease(usageLimitedToken.getId());
				}
				return new AuthorizationTokenVerifyResponseDTO(verified, tokenHeader.getConsumerCloud(), tokenHeader.getConsumer(), tokenHeader.getServiceDefinition(), tokenHeader.getServiceOperation());
			}

			// TIME LIMITED TOKEN
			if (tokenHeader.getTokenType() == AuthorizationTokenType.TIME_LIMITED_TOKEN) {
				final TimeLimitedToken timeLimitedToken = timeLimitedTokenDbService.getByHeader(tokenHeader).get();
				final boolean verified = timeLimitedToken.getExpiresAt().isAfter(Utilities.utcNow());
				return new AuthorizationTokenVerifyResponseDTO(verified, tokenHeader.getConsumerCloud(), tokenHeader.getConsumer(), tokenHeader.getServiceDefinition(), tokenHeader.getServiceOperation());
			}

			throw new InternalServerError("Unhandled token type: " + tokenHeader.getTokenType());

		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}

	}

	//-------------------------------------------------------------------------------------------------
	public void revoke(final List<String> tokens, final String origin) {
		logger.debug("revoke started...");

		try {
			// Only the not self contained token can be revoked and those are encrypted with HMAC (to not to have auxiliary, otherwise we could not find it)
			final List<String> tokensAsSaved = tokens.stream().map((token) -> {
				try {
					return secretCryptographer.encryptHMACSHA256(token, sysInfo.getSecretCryptographerKey());
				} catch (final Exception ex) {
					logger.error(ex.getMessage());
					logger.debug(ex);
					throw new InternalServerError("Token verification failed.");
				}
			}).toList();

			final List<TokenHeader> tokenHeaders = tokenHeaderDbService.findByTokenList(tokensAsSaved);
			tokenHeaderDbService.deleteById(tokenHeaders.stream().map((header) -> header.getId()).toList()); // Delete on cascade removes also the belonged time or usage limited token details 

		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationTokenMgmtListResponseDTO query(final Pageable pagination, final String requester, final AuthorizationTokenType tokenType, final String consumerCloud, final String consumer, final String provider,
			final String serviceDefinition, final String origin) {
		logger.debug("query started...");

		try {
			final List<AuthorizationTokenResponseDTO> results = new ArrayList<>();
			final Page<TokenHeader> tokenHeaderPage = tokenHeaderDbService.query(pagination, requester, tokenType, consumerCloud, consumer, provider, serviceDefinition);
			for (final TokenHeader tokenHeader : tokenHeaderPage) {
				// USAGE LIMITED TOKEN
				if (tokenHeader.getTokenType() == AuthorizationTokenType.USAGE_LIMITED_TOKEN) {
					final Optional<UsageLimitedToken> detailsOpt = usageLimitedTokenDbService.getByHeader(tokenHeader);
					results.add(new AuthorizationTokenResponseDTO(
							tokenType,
							null,
							tokenHeader.getToken(),
							tokenHeader.getRequester(),
							tokenHeader.getConsumerCloud(),
							tokenHeader.getConsumer(),
							tokenHeader.getProvider(),
							tokenHeader.getServiceDefinition(),
							tokenHeader.getServiceOperation(),
							Utilities.convertZonedDateTimeToUTCString(tokenHeader.getCreatedAt()),
							detailsOpt.isEmpty() ? null : detailsOpt.get().getUsageLimit(),
							detailsOpt.isEmpty() ? null : detailsOpt.get().getUsageLeft(),
							null));
				}

				// TIME LIMITED TOKEN
				if (tokenHeader.getTokenType() == AuthorizationTokenType.TIME_LIMITED_TOKEN) {
					final Optional<TimeLimitedToken> detailsOpt = timeLimitedTokenDbService.getByHeader(tokenHeader);
					results.add(new AuthorizationTokenResponseDTO(
							tokenType,
							null,
							tokenHeader.getToken(),
							tokenHeader.getRequester(),
							tokenHeader.getConsumerCloud(),
							tokenHeader.getConsumer(),
							tokenHeader.getProvider(),
							tokenHeader.getServiceDefinition(),
							tokenHeader.getServiceOperation(),
							Utilities.convertZonedDateTimeToUTCString(tokenHeader.getCreatedAt()),
							null,
							null,
							detailsOpt.isEmpty() ? null : Utilities.convertZonedDateTimeToUTCString(detailsOpt.get().getExpiresAt())));
				}
				
				// SELF CONTAINED TOKEN
				if (tokenHeader.getTokenType() == AuthorizationTokenType.SELF_CONTAINED_TOKEN) {
					final Optional<SelfContainedToken> detailsOpt = selfContainedTokenDbService.getByHeader(tokenHeader);
					results.add(new AuthorizationTokenResponseDTO(
							tokenType,
							detailsOpt.isEmpty() ? null : detailsOpt.get().getType(),
							tokenHeader.getToken(),
							tokenHeader.getRequester(),
							tokenHeader.getConsumerCloud(),
							tokenHeader.getConsumer(),
							tokenHeader.getProvider(),
							tokenHeader.getServiceDefinition(),
							tokenHeader.getServiceOperation(),
							Utilities.convertZonedDateTimeToUTCString(tokenHeader.getCreatedAt()),
							null,
							null,
							detailsOpt.isEmpty() ? null : Utilities.convertZonedDateTimeToUTCString(detailsOpt.get().getExpiresAt())));
				}
			}

			return new AuthorizationTokenMgmtListResponseDTO(results, tokenHeaderPage.getTotalElements());

		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}
}
