package eu.arrowhead.serviceorchestration.service.dto;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.dto.OrchestrationSimpleStoreListResponseDTO;
import eu.arrowhead.dto.OrchestrationSimpleStoreResponseDTO;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationStore;

@Service
public class DTOConverter {
	
	//=================================================================================================
	// members
	
	private final Logger logger = LogManager.getLogger(this.getClass());
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public OrchestrationSimpleStoreListResponseDTO convertStoreEntityListToResponseListDTO(final List<OrchestrationStore> entities) {
		logger.debug("convertOrchestrationStoreListToResponseListDTO started...");
		
		if (entities == null) {
			return null;
		}
		
		return new OrchestrationSimpleStoreListResponseDTO(
				entities.stream().map(e -> convertOrchestrationStoreEntityToResponseDTO(e)).collect(Collectors.toList()), 
				entities.size());
	}
	
	//=================================================================================================
	// assistant methods
	
	//-------------------------------------------------------------------------------------------------
	private OrchestrationSimpleStoreResponseDTO convertOrchestrationStoreEntityToResponseDTO(final OrchestrationStore entity) {
		Assert.notNull(entity, "entity is null");
		
		return new OrchestrationSimpleStoreResponseDTO(
				entity.getId().toString(),
				entity.getConsumer(),
				entity.getServiceDefinition(),
				entity.getServiceInstanceId(),
				entity.getPriority(),
				entity.getCreatedBy(),
				entity.getUpdatedBy(),
				entity.getCreatedAt().toString(),
				entity.getUpdatedAt().toString());
	}

}
