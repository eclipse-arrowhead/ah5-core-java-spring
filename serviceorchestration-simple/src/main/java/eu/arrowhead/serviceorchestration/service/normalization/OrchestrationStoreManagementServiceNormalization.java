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
package eu.arrowhead.serviceorchestration.service.normalization;

import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.serviceinstance.ServiceInstanceIdentifierNormalizer;
import eu.arrowhead.dto.OrchestrationSimpleStoreQueryRequestDTO;
import eu.arrowhead.dto.OrchestrationSimpleStoreRequestDTO;

@Service
public class OrchestrationStoreManagementServiceNormalization {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private ServiceInstanceIdentifierNormalizer serviceInstanceIdNormalizer;

	@Autowired
	private SystemNameNormalizer systemNameNormalizer;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationSimpleStoreRequestDTO normalizeCreate(final OrchestrationSimpleStoreRequestDTO dto) {
		logger.debug("normalizeCreate started...");
		Assert.notNull(dto, "DTO is null");

		return new OrchestrationSimpleStoreRequestDTO(
				systemNameNormalizer.normalize(dto.consumer()),
				serviceInstanceIdNormalizer.normalize(dto.serviceInstanceId()),
				dto.priority());
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationSimpleStoreQueryRequestDTO normalizeQuery(final OrchestrationSimpleStoreQueryRequestDTO dto) {
		logger.debug("normalizeQuery started...");

		throw new NotImplementedException();

		/*return new OrchestrationSimpleStoreQueryRequestDTO(
				// no need to normalize, because it will happen in the getPageRequest method
				dto.pagination(),

				dto.ids(),

				//TODO: replace with the new implementation
				Utilities.isEmpty(dto.consumerNames()) ? null
						: dto.consumerNames().stream().map(c -> nameNormalizer.normalize(c)).collect(Collectors.toList()),

				//TODO: replace with the new implementation
				Utilities.isEmpty(dto.serviceDefinitions()) ? null
						: dto.serviceDefinitions().stream().map(s -> nameNormalizer.normalize(s)).collect(Collectors.toList()),
						
				//TODO: replace with the new implementation
				Utilities.isEmpty(dto.serviceInstanceIds()) ? null
						: dto.serviceInstanceIds().stream().map(s -> nameNormalizer.normalize(s)).collect(Collectors.toList()),

				dto.minPriority(),
				dto.maxPriority(),

				//TODO: replace with the new implementation
				Utilities.isEmpty(dto.createdBy()) ? null :
					nameNormalizer.normalize(dto.createdBy()));*/
	}
}
