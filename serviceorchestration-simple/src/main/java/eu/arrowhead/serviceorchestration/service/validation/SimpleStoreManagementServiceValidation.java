package eu.arrowhead.serviceorchestration.service.validation;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Triple;
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
			checkDuplicates(normalized);
			
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
		
		if (!Utilities.isEmpty(dto.serviceDefinition())) {
			nameValidator.validateName(dto.serviceDefinition());
		}
		
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
	
	//-------------------------------------------------------------------------------------------------
	private void checkDuplicates(final List<OrchestrationSimpleStoreRequestDTO> candidates) {
		// consumer, serviceDefinition and priority must be unique
		final List<Triple<String, String, Integer>> existing = new ArrayList<Triple<String, String, Integer>>();
		for (final OrchestrationSimpleStoreRequestDTO candidate : candidates) {
			final Triple<String, String, Integer> current = Triple.of(candidate.consumer(), candidate.serviceDefinition(), candidate.priority());
			if (existing.contains(current)) {
				throw new InvalidParameterException("The following fields must be unique: consumer, serviceDefinition, priority");
			} 
			else {
				existing.add(current);
			}
		}
	}
	
}
