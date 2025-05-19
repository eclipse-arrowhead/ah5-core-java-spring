package eu.arrowhead.authorization.jpa.service;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import eu.arrowhead.authorization.jpa.entity.TokenHeader;
import eu.arrowhead.authorization.jpa.repository.TokenHeaderRepository;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.dto.enums.AuthorizationTokenType;

@Service
public class TokenHeaderDbService {

	//=================================================================================================
	// members
	
	@Autowired
	private TokenHeaderRepository headerRepo;
	
	private final Logger logger = LogManager.getLogger(this.getClass());
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public Optional<TokenHeader> find(final String provider, final String token) {
		logger.debug("find started");
		Assert.isTrue(!Utilities.isEmpty(provider), "provider is empty");
		Assert.isTrue(!Utilities.isEmpty(token), "token is empty");
		
		try {
			return headerRepo.findByProviderAndToken(provider, token);
			
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	public List<TokenHeader> findByTokenList(final List<String> tokens) {
		logger.debug("findByTokenList started");
		Assert.isTrue(!Utilities.containsNullOrEmpty(tokens), "token list contains null or empty element");
		
		try {
			return headerRepo.findAllByTokenIn(tokens);
			
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public void deleteById(final List<Long> ids) {
		logger.debug("deleteById started");
		Assert.isTrue(!Utilities.containsNull(ids), "ID list contains null element");
		
		try {
			headerRepo.deleteAllByIdInBatch(ids);
			headerRepo.flush();
			
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	public Page<TokenHeader> query(final Pageable pagination, final String requester, final AuthorizationTokenType tokenType, final String consumerCloud, final String consumer, final String provider, final String serviceDefinition) {
		logger.debug("query started");
		Assert.notNull(pagination, "pagination is null");
		
		try {
			// TODO
			return null;
			
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}
	
	//=================================================================================================
	// nested structures

	//-------------------------------------------------------------------------------------------------
	private enum BaseFilter {
		NONE, REQUESTER, TOKEN_TYPE, CLOUD, CONSUMER, PROVIDER, SERVICE
	}
}
