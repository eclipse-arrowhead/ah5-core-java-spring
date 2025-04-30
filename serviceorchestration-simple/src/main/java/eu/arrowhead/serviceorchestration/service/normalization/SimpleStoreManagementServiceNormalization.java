package eu.arrowhead.serviceorchestration.service.normalization;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.validation.name.NameNormalizer;
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
				nameNormalizer.normalize(dto.consumer()), 
				Utilities.isEmpty(dto.serviceDefinition()) ? null : nameNormalizer.normalize(dto.serviceDefinition()), 
				nameNormalizer.normalize(dto.serviceInstanceId()), 
				dto.priority());
	}
}
