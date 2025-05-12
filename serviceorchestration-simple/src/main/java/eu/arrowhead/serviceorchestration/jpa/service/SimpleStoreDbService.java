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
			final List<String> serviceDefinitionNames,
			final List<String> serviceInstanceIds,
			final Integer minPriority,
			final Integer maxPriority,
			final String createdBy) {
		
		logger.debug("getPage started...");
		Assert.notNull(pagination, "page is null");
		
		// withot filters
		if (ids == null 
			&& consumerNames == null 
			&& serviceDefinitionNames == null 
			&& serviceInstanceIds == null 
			&& minPriority == null
			&& maxPriority == null
			&& createdBy == null) {
			
			return storeRepo.findAll(pagination);
		}
		
		// with filters
		
		//TODO: create BASE_FILTER
		//lock?
		List<OrchestrationStore> matchings = ids == null ? storeRepo.findAll() : storeRepo.findAllById(ids);
		for (OrchestrationStore match : matchings) {
			
			if (consumerNames != null && !consumerNames.contains(match.getConsumer())) {
				matchings.remove(match);
				continue;
			}
			
			if (serviceDefinitionNames != null && !serviceDefinitionNames.contains(match.getServiceDefinition())) {
				matchings.remove(match);
				continue;
			}
			
			if (serviceInstanceIds != null && !serviceInstanceIds.contains(match.getServiceInstanceId())) {
				matchings.remove(match);
				continue;
			}
			
			if (createdBy != null && !createdBy.equals(match.getCreatedBy())) {
				matchings.remove(match);
			}
			
			if (minPriority != null && match.getPriority() < minPriority) {
				matchings.remove(match);
				continue;
			}
			
			if (maxPriority != null && match.getPriority() > maxPriority) {
				matchings.remove(match);
				continue;
			}
		}
		
		//TODO
		return null;
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
}
