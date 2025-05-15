package eu.arrowhead.serviceorchestration.jpa.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
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
	@Transactional(rollbackFor = ArrowheadException.class)
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
	
	//-------------------------------------------------------------------------------------------------
	public Page<OrchestrationStore> getPage(
			final PageRequest pagination,
			final List<UUID> ids,
			final List<String> consumerNames,
			final List<String> serviceDefinitions,
			final List<String> serviceInstanceIds,
			final Integer minPriority,
			final Integer maxPriority,
			final String createdBy) {
		
		logger.debug("getPage started...");
		Assert.notNull(pagination, "page is null");
		
		// without filters
		if (Utilities.allEmpty(ids, consumerNames, serviceDefinitions, serviceInstanceIds)
				&& Utilities.isEmpty(createdBy)
				&& minPriority == null 
				&& maxPriority == null) {
			return storeRepo.findAll(pagination);
		}
		
		try {
		
			// with filters
			BaseFilter baseFilter = BaseFilter.NONE;
			List<OrchestrationStore> toFilter = new ArrayList<>();
			if (!Utilities.isEmpty(ids)) {
				baseFilter = BaseFilter.ID;
				toFilter = storeRepo.findAllById(ids);
			} else if (!Utilities.isEmpty(consumerNames)) {
				baseFilter = BaseFilter.CONSUMER;
				toFilter = storeRepo.findAllByConsumerIn(consumerNames);
			} else if (!Utilities.isEmpty(serviceDefinitions)) {
				baseFilter = BaseFilter.DEFINITION;
				toFilter = storeRepo.findAllByServiceDefinitionIn(serviceDefinitions);
			} else if (!Utilities.isEmpty(serviceInstanceIds)) {
				baseFilter = BaseFilter.INSTANCE;
				toFilter = storeRepo.findAllByServiceInstanceIdIn(serviceInstanceIds);
			} else if (!Utilities.isEmpty(createdBy)) {
				baseFilter = BaseFilter.CREATOR;
				toFilter = storeRepo.findAllByCreatedBy(createdBy);
			} else {
				toFilter = storeRepo.findAll();
			}
			final List<UUID> matchingIds = new ArrayList<UUID>();
			for (final OrchestrationStore entity : toFilter) {
				
				// match against id not needed, because if it was not empty, it was the baseFilter
				
				// match against consumer
				if (baseFilter != BaseFilter.CONSUMER && consumerNames != null && !consumerNames.contains(entity.getConsumer())) {
					continue;
				}
				
				// match against service definition
				if (baseFilter != BaseFilter.DEFINITION && serviceDefinitions != null && !serviceDefinitions.contains(entity.getServiceDefinition())) {
					continue;
				}
				
				// match against service instance id
				if (baseFilter != BaseFilter.INSTANCE && serviceInstanceIds != null && !serviceInstanceIds.contains(entity.getServiceInstanceId())) {
					continue;
				}
				
				// match against created by
				if (baseFilter != BaseFilter.CREATOR && createdBy != null && !createdBy.equals(entity.getCreatedBy())) {
					continue;
				}
				
				// match against minimum priority
				if (minPriority != null && entity.getPriority() < minPriority) {
					continue;
				}
				
				// match against maximum priority
				if (maxPriority != null && entity.getPriority() > maxPriority) {
					continue;
				}
				matchingIds.add(entity.getId());
			}
			
			return storeRepo.findAllByIdIn(matchingIds, pagination);
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
		
		// unique fields: consumer, service instance id, priority
		for (final OrchestrationStore candidate : candidates) {
			
			// check if there is and existing record
			final List<OrchestrationStore> existing = storeRepo.findAllByConsumerAndServiceInstanceIdAndPriority(candidate.getConsumer(), candidate.getServiceInstanceId(), candidate.getPriority());
			if (!Utilities.isEmpty(existing)) {
				throw new InvalidParameterException("There is already an existing entity with consumer name: " +  candidate.getConsumer() + ", service instance id: " 
						+ candidate.getServiceInstanceId() + ", priority: " + candidate.getPriority());
			}
		}
	}
	
	//=================================================================================================
	// nested classes
	
	private enum BaseFilter {
		NONE, ID, CONSUMER, DEFINITION, INSTANCE, CREATOR;
	}
}
