package eu.arrowhead.authorization.jpa.service;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import eu.arrowhead.authorization.jpa.entity.TokenHeader;
import eu.arrowhead.authorization.jpa.entity.UsageLimitedToken;
import eu.arrowhead.authorization.jpa.repository.CryptographerAuxiliaryRepository;
import eu.arrowhead.authorization.jpa.repository.TokenHeaderRepository;
import eu.arrowhead.authorization.jpa.repository.UsageLimitedTokenRepository;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.dto.enums.AuthorizationTokenType;

@Service
public class UsageLimitedTokenDbService {

	//=================================================================================================
	// members

	@Autowired
	private UsageLimitedTokenRepository tokenRepo;

	@Autowired
	private TokenHeaderRepository tokenHeaderRepo;

	@Autowired
	private CryptographerAuxiliaryRepository auxiliaryRepo;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:parameternumber")
	@Transactional(rollbackFor = ArrowheadException.class)
	public Pair<UsageLimitedToken, Boolean> save(
			final AuthorizationTokenType tokenType,
			final String token,
			final String requester,
			final String consumerCloud,
			final String consumer,
			final String provider,
			final String serviceDefinition,
			final String serviceOperation,
			final int usageLimit) {
		logger.debug("save started...");
		Assert.notNull(tokenType, "tokenType is null");
		Assert.isTrue(!Utilities.isEmpty(token), "token is empty");
		Assert.isTrue(!Utilities.isEmpty(requester), "requester is empty");
		Assert.isTrue(!Utilities.isEmpty(consumerCloud), "consumerCloud is empty");
		Assert.isTrue(!Utilities.isEmpty(consumer), "consumer is empty");
		Assert.isTrue(!Utilities.isEmpty(provider), "provider is empty");
		Assert.isTrue(!Utilities.isEmpty(serviceDefinition), "serviceDefinition is empty");

		try {
			boolean override = false;
			final Optional<TokenHeader> tokenHeaderOpt = tokenHeaderRepo.findByConsumerCloudAndConsumerAndProviderAndServiceDefinition(consumerCloud, consumer, provider, serviceDefinition);
			if (tokenHeaderOpt.isPresent()) {
				final Optional<UsageLimitedToken> tokenOpt = tokenRepo.findByHeader(tokenHeaderOpt.get());
				if (tokenOpt.isPresent()) {
					tokenRepo.delete(tokenOpt.get());
					override = true;
				}
				tokenHeaderRepo.delete(tokenHeaderOpt.get());
			}

			final TokenHeader tokenHeaderRecord = tokenHeaderRepo.saveAndFlush(new TokenHeader(tokenType, token, null, requester, consumerCloud, consumer, provider, serviceDefinition, serviceOperation));
			final UsageLimitedToken tokenRecord = tokenRepo.saveAndFlush(new UsageLimitedToken(tokenHeaderRecord, usageLimit));
			return Pair.of(tokenRecord, !override);

		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public Optional<UsageLimitedToken> getByHeader(final TokenHeader header) {
		logger.debug("getByHeader started...");
		Assert.notNull(header, "header is null");

		return tokenRepo.findByHeader(header);
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public Optional<UsageLimitedToken> decrease(final long id) {
		logger.debug("decrease started...");

		final Optional<UsageLimitedToken> optional = tokenRepo.findById(id);
		if (optional.isEmpty()) {
			return optional;
		}

		final UsageLimitedToken usageLimitedToken = optional.get();
		usageLimitedToken.setUsageLeft(usageLimitedToken.getUsageLeft() - 1);
		final UsageLimitedToken result = tokenRepo.saveAndFlush(usageLimitedToken);
		return Optional.of(result);
	}
}
