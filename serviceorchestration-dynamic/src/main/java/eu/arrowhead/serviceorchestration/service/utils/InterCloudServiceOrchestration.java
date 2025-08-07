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
package eu.arrowhead.serviceorchestration.service.utils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import eu.arrowhead.dto.OrchestrationResponseDTO;
import eu.arrowhead.dto.enums.OrchestrationFlag;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationConstants;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationForm;

@Service
public class InterCloudServiceOrchestration {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationResponseDTO doInterCloudServiceOrchestration(final UUID jobId, final OrchestrationForm form) {
		logger.debug("doInterCloudServiceOrchestration started...");

		final Set<String> warnings = new HashSet<>();
		warnings.add(DynamicServiceOrchestrationConstants.ORCH_WARN_INTER_CLOUD);

		if (form.addFlag(OrchestrationFlag.MATCHMAKING, true)) {
			warnings.add(DynamicServiceOrchestrationConstants.ORCH_WARN_AUTO_MATCHMAKING);
		}

		// TODO implement when Gatekeeper & Gateway are ready.

		logger.warn("Inter-cloud orchestration is not supported yet");
		return new OrchestrationResponseDTO(List.of(), warnings.stream().toList());
	}
}