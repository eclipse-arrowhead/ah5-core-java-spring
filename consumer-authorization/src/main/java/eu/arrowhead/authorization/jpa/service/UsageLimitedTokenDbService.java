package eu.arrowhead.authorization.jpa.service;

import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import eu.arrowhead.authorization.jpa.entity.CryptographerIV;
import eu.arrowhead.authorization.jpa.entity.TokenHeader;
import eu.arrowhead.authorization.jpa.entity.UsageLimitedToken;
import eu.arrowhead.authorization.jpa.repository.CryptographerIVRepository;
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
	private CryptographerIVRepository ivRepo;
	
	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:parameternumber")
	@Transactional(rollbackFor = ArrowheadException.class)
	public Pair<UsageLimitedToken, Boolean> save(
			final AuthorizationTokenType tokenType,
			final String token,
			final String ivBase64,
			final String requester,
			final String consumerCloud,
			final String consumer,
			final String provider,
			final String serviceDefinition,
			final String serviceOperation,
			int usageLimit) {
		Assert.notNull(tokenType, "tokenType is null");
		Assert.isTrue(!Utilities.isEmpty(ivBase64), "ivBase64 is empty");
		Assert.isTrue(!Utilities.isEmpty(token), "token is empty");
		Assert.isTrue(!Utilities.isEmpty(requester), "requester is empty");
		Assert.isTrue(!Utilities.isEmpty(consumerCloud), "consumerCloud is empty");
		Assert.isTrue(!Utilities.isEmpty(consumer), "consumer is empty");
		Assert.isTrue(!Utilities.isEmpty(provider), "provider is empty");
		Assert.isTrue(!Utilities.isEmpty(serviceDefinition), "serviceDefinition is empty");
		Assert.isTrue(!Utilities.isEmpty(serviceOperation), "serviceOperation is empty");

		try {
			boolean override = false;
			final Optional<TokenHeader> tokenHeaderOpt = tokenHeaderRepo.findByConsumerCloudAndConsumerAndProviderAndServiceDefinition(consumerCloud, consumer, provider, serviceDefinition);
			if (tokenHeaderOpt.isPresent()) {
				final Optional<UsageLimitedToken> tokenOpt = tokenRepo.findByHeader(tokenHeaderOpt.get());
				if (tokenOpt.isPresent()) {
					tokenRepo.delete(tokenOpt.get());
					override = true;
				}
			}
			
			final CryptographerIV ivRecord = ivRepo.saveAndFlush(new CryptographerIV(ivBase64));
			final TokenHeader tokenHeaderRecord = tokenHeaderRepo.saveAndFlush(new TokenHeader(tokenType, token, ivRecord, requester, consumerCloud, consumer, provider, serviceDefinition, serviceOperation));
			final UsageLimitedToken tokenRecord = tokenRepo.saveAndFlush(new UsageLimitedToken(tokenHeaderRecord, usageLimit));
			return Pair.of(tokenRecord, !override);
			
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}
}
