package eu.arrowhead.serviceorchestration.jpa.service;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationStore;
import eu.arrowhead.serviceorchestration.jpa.repository.OrchestrationStoreRepository;

@Service
public class SimpleStoreDbService {
	
	//=================================================================================================
	// members
	
	private final Logger logger = LogManager.getLogger(this.getClass());
	
	@Autowired
	private OrchestrationStoreRepository storeRepo;

	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public List<OrchestrationStore> createBulk(final List<OrchestrationStore> candidates) {
		logger.debug("createBulk started...");
		Assert.isTrue(!Utilities.isEmpty(candidates), "candidate list is empty");
		Assert.isTrue(!Utilities.containsNull(candidates), "candidate list contains null element");
		
		try {
			return storeRepo.saveAllAndFlush(candidates);

		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}
}
