package eu.arrowhead.authorization.jpa.service;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authorization.jpa.entity.TokenHeader;
import eu.arrowhead.authorization.jpa.repository.TokenHeaderRepository;
import eu.arrowhead.common.Utilities;

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
		
		return headerRepo.findByProviderAndToken(provider, token);
	}
}
