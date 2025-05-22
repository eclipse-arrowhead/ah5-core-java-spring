package eu.arrowhead.serviceorchestration.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.OrchestrationRequestDTO;
import eu.arrowhead.dto.OrchestrationResponseDTO;
import eu.arrowhead.dto.enums.OrchestrationFlag;
import eu.arrowhead.serviceorchestration.SimpleServiceOrchestrationConstants;

@Service
public class OrchestrationService {
	
	//=================================================================================================
	// members
	
	private final Logger logger = LogManager.getLogger(this.getClass());
	
	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationResponseDTO pull(final String requesterSystem, final OrchestrationRequestDTO dto, final String origin) {
		logger.debug("pull started...");
		
		//List<String> warnings = 
		
		// validate and normalize
		
		
		// create jobs
		
		// orchestrate
		
		return null;
	}
	
	//=================================================================================================
	// assistant methods
	
	//-----------------------------------------------------------------------------------------------
	// Returns a list with maximum 2 strings: ignored fields if any, and ignored flags if any
	private List<String> collectIgnoreWarnings(final OrchestrationRequestDTO dto) {
		
		List<String> warnings = new ArrayList<>();
		
		// 1. ignored fields
		
		String ignoredFields = SimpleServiceOrchestrationConstants.IGNORED_FIELDS;
		
		if (!Utilities.isEmpty(dto.serviceRequirement().operations())) {
			ignoredFields.concat(SimpleServiceOrchestrationConstants.FIELD_OPERATIONS);
		}
		
		if (!Utilities.isEmpty(dto.serviceRequirement().alivesAt())) {
			ignoredFields.concat(SimpleServiceOrchestrationConstants.FIELD_ALIVES_AT);
		}
		
		if (!Utilities.isEmpty(dto.serviceRequirement().metadataRequirements())) {
			ignoredFields.concat(SimpleServiceOrchestrationConstants.FIELD_METADATA_REQ);
		}
		
		if (!Utilities.isEmpty(dto.serviceRequirement().interfaceTemplateNames())) {
			ignoredFields.concat(SimpleServiceOrchestrationConstants.FIELD_INTF_TEMPLATE_NAMES);
		}
		
		if (!Utilities.isEmpty(dto.serviceRequirement().interfaceAddressTypes())) {
			ignoredFields.concat(SimpleServiceOrchestrationConstants.FIELD_INTF_ADDRESS_TYPES);
		}
		
		if (!Utilities.isEmpty(dto.serviceRequirement().interfacePropertyRequirements())) {
			ignoredFields.concat(SimpleServiceOrchestrationConstants.FIELD_INTF_PROP_REQ);
		}
		
		if (!Utilities.isEmpty(dto.serviceRequirement().securityPolicies())) {
			ignoredFields.concat(SimpleServiceOrchestrationConstants.FIELD_SECURITY_POLICIES);
		}
		
		if (!Utilities.isEmpty(dto.serviceRequirement().preferredProviders())) {
			ignoredFields.concat(SimpleServiceOrchestrationConstants.FIELD_PREFFERED_PROVIDERS);
		}
		
		if (!Utilities.isEmpty(dto.qosRequirements())) {
			ignoredFields.concat(SimpleServiceOrchestrationConstants.FIELD_QOS_REQ);
		}
		
		if (dto.exclusivityDuration() != null) {
			ignoredFields.concat(SimpleServiceOrchestrationConstants.FIELD_EXCLUSIVITY_DURATION);
		}
		
		// check is at least one ignored field was found
		if (!ignoredFields.equals(SimpleServiceOrchestrationConstants.IGNORED_FIELDS)) {
			// add ignored fields to the warnings while removing the last comma
			warnings.add(ignoredFields.replaceFirst(",$", "")); 
		}
		
		// 2. ignored flags
		
		if (!Utilities.isEmpty(dto.orchestrationFlags())) {
			final String ignoredFlags = dto.orchestrationFlags().entrySet()
					.stream()
					.filter(e -> !e.getKey().equals(OrchestrationFlag.MATCHMAKING.toString()))
					.map(Map.Entry::getKey)
					.collect(Collectors.joining(", "));
			
			warnings.add(SimpleServiceOrchestrationConstants.IGNORED_FLAGS.concat(ignoredFlags));
		}
		
		return warnings;
		
	}

}
