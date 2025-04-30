package eu.arrowhead.serviceorchestration.jpa.service;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
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
		
		checkUniqueFields(candidates);
		
		try {
			return storeRepo.saveAllAndFlush(candidates);

		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}
	
	//=================================================================================================
	// assistan methods
	
	//-------------------------------------------------------------------------------------------------
	private void checkUniqueFields(final List<OrchestrationStore> candidates) {
		Assert.isTrue(!Utilities.isEmpty(candidates), "candidate list is empty");
		Assert.isTrue(!Utilities.containsNull(candidates), "candidate list contains null element");
		
		// unique fields: consumer, service definition, priority
		for (OrchestrationStore candidate : candidates) {
			
			// get existing records by consumer and priority (service definition is ignored here since it can be null)
			List<OrchestrationStore> existing = storeRepo.findAllByConsumerAndPriority(candidate.getConsumer(), candidate.getPriority());
			
			// check if there is an existing record where the consumer name is the same or both are null
			List<String> existingConsumers = existing.stream().map(e -> e.getConsumer()).collect(Collectors.toList());
			if (existingConsumers.contains(candidate.getConsumer())) {
				throw new InvalidParameterException("Store record with the following fields already exists: consumer: " + 
						candidate.getConsumer() + ", serviceDefinition: " + candidate.getServiceDefinition() + ", priority: " + candidate.getPriority());
			}
		}
	}
}
