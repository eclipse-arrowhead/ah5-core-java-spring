/*******************************************************************************
 *
 * Copyright (c) 2025 AITIA
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 *
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  	AITIA - implementation
 *  	Arrowhead Consortia - conceptualization
 *
 *******************************************************************************/
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
import eu.arrowhead.serviceorchestration.SimpleStoreServiceOrchestrationConstants;

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

		String ignoredFields = SimpleStoreServiceOrchestrationConstants.IGNORED_FIELDS;
		
		if (!Utilities.isEmpty(dto.serviceRequirement().operations())) {
			ignoredFields.concat(SimpleStoreServiceOrchestrationConstants.FIELD_OPERATIONS);
		}
		
		if (!Utilities.isEmpty(dto.serviceRequirement().alivesAt())) {
			ignoredFields.concat(SimpleStoreServiceOrchestrationConstants.FIELD_ALIVES_AT);
		}
		
		if (!Utilities.isEmpty(dto.serviceRequirement().metadataRequirements())) {
			ignoredFields.concat(SimpleStoreServiceOrchestrationConstants.FIELD_METADATA_REQ);
		}
		
		if (!Utilities.isEmpty(dto.serviceRequirement().interfaceTemplateNames())) {
			ignoredFields.concat(SimpleStoreServiceOrchestrationConstants.FIELD_INTF_TEMPLATE_NAMES);
		}
		
		if (!Utilities.isEmpty(dto.serviceRequirement().interfaceAddressTypes())) {
			ignoredFields.concat(SimpleStoreServiceOrchestrationConstants.FIELD_INTF_ADDRESS_TYPES);
		}
		
		if (!Utilities.isEmpty(dto.serviceRequirement().interfacePropertyRequirements())) {
			ignoredFields.concat(SimpleStoreServiceOrchestrationConstants.FIELD_INTF_PROP_REQ);
		}
		
		if (!Utilities.isEmpty(dto.serviceRequirement().securityPolicies())) {
			ignoredFields.concat(SimpleStoreServiceOrchestrationConstants.FIELD_SECURITY_POLICIES);
		}
		
		if (!Utilities.isEmpty(dto.serviceRequirement().preferredProviders())) {
			ignoredFields.concat(SimpleStoreServiceOrchestrationConstants.FIELD_PREFFERED_PROVIDERS);
		}
		
		if (!Utilities.isEmpty(dto.qosRequirements())) {
			ignoredFields.concat(SimpleStoreServiceOrchestrationConstants.FIELD_QOS_REQ);
		}
		
		if (dto.exclusivityDuration() != null) {
			ignoredFields.concat(SimpleStoreServiceOrchestrationConstants.FIELD_EXCLUSIVITY_DURATION);
		}
		
		// check is at least one ignored field was found
		if (!ignoredFields.equals(SimpleStoreServiceOrchestrationConstants.IGNORED_FIELDS)) {
			// add ignored fields to the warnings while removing the last comma
			warnings.add(ignoredFields.replaceFirst(",$", "")); //TODO: change this!!
		}
		
		// 2. ignored flags
		
		if (!Utilities.isEmpty(dto.orchestrationFlags())) {
			final String ignoredFlags = dto.orchestrationFlags().entrySet()
					.stream()
					.filter(e -> !e.getKey().equals(OrchestrationFlag.MATCHMAKING.toString()))
					.map(Map.Entry::getKey)
					.collect(Collectors.joining(", "));
			
			warnings.add(SimpleStoreServiceOrchestrationConstants.IGNORED_FLAGS.concat(ignoredFlags));
		}
		
		return warnings;
		
	}

}
