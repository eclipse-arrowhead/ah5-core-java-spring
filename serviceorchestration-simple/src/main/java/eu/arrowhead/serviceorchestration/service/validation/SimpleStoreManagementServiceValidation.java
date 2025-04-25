package eu.arrowhead.serviceorchestration.service.validation;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.NameValidator;
import eu.arrowhead.dto.OrchestrationSimpleStoreListRequestDTO;
import eu.arrowhead.dto.OrchestrationSimpleStoreRequestDTO;
import eu.arrowhead.serviceorchestration.service.normalization.SimpleStoreManagementServiceNormalization;

@Service
public class SimpleStoreManagementServiceValidation {

	//=================================================================================================
	// members
	
	private final Logger logger = LogManager.getLogger(this.getClass());
	
	@Autowired
	private SimpleStoreManagementServiceNormalization normalizer;
	
	@Autowired
	private NameValidator nameValidator;

	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public List<OrchestrationSimpleStoreRequestDTO> validateAndNormalizeCreateBulk(final OrchestrationSimpleStoreListRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeCreateBulk started...");
		
		if (dto == null) {
			throw new InvalidParameterException("Request payload is null", origin);
		}

		if (Utilities.containsNull(dto.candidates())) {
			throw new InvalidParameterException("Request payload contains null element", origin);
		}
		
		final List<OrchestrationSimpleStoreRequestDTO> normalized = new ArrayList<OrchestrationSimpleStoreRequestDTO>(dto.candidates().size());
		
		try {
			dto.candidates().forEach(c -> normalized.add(validateAndNormalizeCreate(c)));
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}
		
		return normalized;
	}
	
	//=================================================================================================
	// assistant methods
	
	//-------------------------------------------------------------------------------------------------
	private OrchestrationSimpleStoreRequestDTO validateAndNormalizeCreate(final OrchestrationSimpleStoreRequestDTO dto) {
		Assert.notNull(dto, "DTO is null!");
		
		validateCreate(dto);
		
		final OrchestrationSimpleStoreRequestDTO normalizedDto = normalizer.normalizeCreate(dto);
		
		nameValidator.validateName(dto.consumer());
		nameValidator.validateName(dto.serviceDefinition());
		nameValidator.validateServiceInstanceId(dto.serviceInstanceId());
		
		return normalizedDto; 
	}
	
	//-------------------------------------------------------------------------------------------------
	private void validateCreate(final OrchestrationSimpleStoreRequestDTO dto) {
		Assert.notNull(dto, "DTO is null!");
		
		// consumer
		if (Utilities.isEmpty(dto.consumer())) {
			throw new InvalidParameterException("consumer is null or empty");
		}
		
		// service definition
		if (Utilities.isEmpty(dto.serviceDefinition())) {
			throw new InvalidParameterException("serviceDefinition is null or empty");
		}
		
		// service instance id
		if (Utilities.isEmpty(dto.serviceInstanceId())) {
			throw new InvalidParameterException("serviceInstanceId is null or empty");
		}
		
		// priority
		if (dto.priority() == null) {
			throw new InvalidParameterException("priority is null");
		}	
		if (dto.priority() < 0) {
			throw new InvalidParameterException("priority should be a non-negative integer");
		}
	}
	
}
