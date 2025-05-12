package eu.arrowhead.serviceorchestration.service.normalization;

import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.validation.name.NameNormalizer;
import eu.arrowhead.dto.OrchestrationSimpleStoreQueryRequestDTO;
import eu.arrowhead.dto.OrchestrationSimpleStoreRequestDTO;

@Service
public class SimpleStoreManagementServiceNormalization {
	
	//=================================================================================================
	// members
	
	private final Logger logger = LogManager.getLogger(this.getClass());
	
	@Autowired
	private NameNormalizer nameNormalizer;

	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public OrchestrationSimpleStoreRequestDTO normalizeCreate(final OrchestrationSimpleStoreRequestDTO dto) {
		logger.debug("normalizeCreate started...");
		Assert.notNull(dto, "DTO is null");
		
		return new OrchestrationSimpleStoreRequestDTO(
				
				//TODO: replace with the new implementation
				nameNormalizer.normalize(dto.consumer()),
				
				//TODO: replace with the new implementation
				nameNormalizer.normalize(dto.serviceInstanceId()), 
				
				dto.priority());
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationSimpleStoreQueryRequestDTO normalizeQuery(final OrchestrationSimpleStoreQueryRequestDTO dto) {
		logger.debug("normalizeQuery started...");

		return new OrchestrationSimpleStoreQueryRequestDTO(
				// no need to normalize, because it will happen in the getPageRequest method
				dto.pagination(),
				
				Utilities.isEmpty(dto.ids()) ? null 
						: dto.ids().stream().map(i -> i.trim()).collect(Collectors.toList()),
						
				//TODO: replace with the new implementation
				Utilities.isEmpty(dto.consumerNames()) ? null 
						: dto.consumerNames().stream().map(c -> nameNormalizer.normalize(c)).collect(Collectors.toList()),
				
				//TODO: replace with the new implementation
				Utilities.isEmpty(dto.serviceDefinitionNames()) ? null 
						: dto.serviceDefinitionNames().stream().map(s -> nameNormalizer.normalize(s)).collect(Collectors.toList()),
				
				//TODO: replace with the new implementation
				Utilities.isEmpty(dto.serviceInstanceIds()) ? null 
						: dto.serviceInstanceIds().stream().map(s -> nameNormalizer.normalize(s)).collect(Collectors.toList()),
						
				dto.minPriority(),
				dto.maxPriority(),
				
				//TODO: replace with the new implementation
				Utilities.isEmpty(dto.createdBy()) ? null :
					nameNormalizer.normalize(dto.createdBy()));
	}
}
