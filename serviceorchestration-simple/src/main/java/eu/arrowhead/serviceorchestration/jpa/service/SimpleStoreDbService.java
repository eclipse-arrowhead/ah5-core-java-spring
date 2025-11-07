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
package eu.arrowhead.serviceorchestration.jpa.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.OrchestrationSimpleStoreRequestDTO;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationStore;
import eu.arrowhead.serviceorchestration.jpa.repository.OrchestrationStoreRepository;

@Service
public class SimpleStoreDbService {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private OrchestrationStoreRepository storeRepo;

	private static final Object LOCK = new Object();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public List<OrchestrationStore> createBulk(final List<OrchestrationSimpleStoreRequestDTO> candidates, final String requesterName) {
		logger.debug("createBulk started...");
		Assert.isTrue(!Utilities.isEmpty(candidates), "candidate list is empty");
		Assert.isTrue(!Utilities.containsNull(candidates), "candidate list contains null element");

		final List<OrchestrationStore> toSave = candidates
			.stream().map(n -> new OrchestrationStore(
					n.consumer(),
					n.serviceInstanceId().split(Constants.COMPOSITE_ID_DELIMITER_REGEXP)[1], // service definition
					n.serviceInstanceId(),
					n.priority(),
					requesterName)).collect(Collectors.toList());

		checkUniqueFields(candidates);

		try {
			return storeRepo.saveAllAndFlush(toSave);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:parameternumber")
	public Page<OrchestrationStore> getPageByFilters(
			final PageRequest pagination,
			final List<UUID> ids,
			final List<String> consumerNames,
			final List<String> serviceDefinitions,
			final List<String> serviceInstanceIds,
			final Integer minPriority,
			final Integer maxPriority,
			final String createdBy) {

		logger.debug("getPage started...");
		Assert.notNull(pagination, "page is null");

		// without filters
		if (Utilities.isEmpty(ids)
				&& Utilities.isEmpty(serviceInstanceIds)
				&& Utilities.isEmpty(serviceDefinitions)
				&& Utilities.isEmpty(consumerNames)
				&& Utilities.isEmpty(createdBy)
				&& minPriority == null
				&& maxPriority == null) {
			return storeRepo.findAll(pagination);
		}

		try {

			// with filters
			BaseFilter baseFilter = BaseFilter.NONE;
			List<OrchestrationStore> toFilter = new ArrayList<>();

			if (!Utilities.isEmpty(ids)) {
				baseFilter = BaseFilter.ID;
				toFilter = storeRepo.findAllById(ids);
			} else if (!Utilities.isEmpty(consumerNames)) {
				baseFilter = BaseFilter.CONSUMER;
				toFilter = storeRepo.findAllByConsumerIn(consumerNames);
			} else if (!Utilities.isEmpty(serviceDefinitions)) {
				baseFilter = BaseFilter.DEFINITION;
				toFilter = storeRepo.findAllByServiceDefinitionIn(serviceDefinitions);
			} else if (!Utilities.isEmpty(serviceInstanceIds)) {
				baseFilter = BaseFilter.INSTANCE;
				toFilter = storeRepo.findAllByServiceInstanceIdIn(serviceInstanceIds);
			} else if (!Utilities.isEmpty(createdBy)) {
				baseFilter = BaseFilter.CREATOR;
				toFilter = storeRepo.findAllByCreatedBy(createdBy);
			} else {
				toFilter = storeRepo.findAll();
			}
			final List<UUID> matchingIds = new ArrayList<UUID>();
			for (final OrchestrationStore entity : toFilter) {

				// match against id not needed, because if it was not empty, it was the baseFilter

				// match against consumer
				if (baseFilter != BaseFilter.CONSUMER && consumerNames != null && !consumerNames.contains(entity.getConsumer())) {
					continue;
				}

				// match against service definition
				if (baseFilter != BaseFilter.DEFINITION && serviceDefinitions != null && !serviceDefinitions.contains(entity.getServiceDefinition())) {
					continue;
				}

				// match against service instance id
				if (baseFilter != BaseFilter.INSTANCE && serviceInstanceIds != null && !serviceInstanceIds.contains(entity.getServiceInstanceId())) {
					continue;
				}

				// match against created by
				if (baseFilter != BaseFilter.CREATOR && createdBy != null && !createdBy.equals(entity.getCreatedBy())) {
					continue;
				}

				// match against minimum priority
				if (minPriority != null && entity.getPriority() < minPriority) {
					continue;
				}

				// match against maximum priority
				if (maxPriority != null && entity.getPriority() > maxPriority) {
					continue;
				}
				matchingIds.add(entity.getId());
			}

			return storeRepo.findAllByIdIn(matchingIds, pagination);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public List<OrchestrationStore> setPriorities(final Map<UUID, Integer> priorities, final String requester) {
		Assert.notNull(priorities, "priorities is null");
		logger.info("setPriorities started...");

		try {
			synchronized (LOCK) {
				List<OrchestrationStore> toModify = storeRepo.findAllById(priorities.keySet());
				List<OrchestrationStore> modified = new ArrayList<OrchestrationStore>();

				for (OrchestrationStore entry : toModify) {

					final Integer newPriority = priorities.get(entry.getId());

					// check if the same <consumer, service instance id, priority> already exists in the DB
					final Optional<OrchestrationStore> existing = storeRepo.findByConsumerAndServiceInstanceIdAndPriority(entry.getConsumer(), entry.getServiceInstanceId(), newPriority);
					if (existing.isPresent()) {
						if (!existing.get().getId().equals(entry.getId())) {
							throw new InvalidParameterException("There is already an existing entity with consumer name: " +  existing.get().getConsumer() + ", service instance id: "
									+ existing.get().getServiceInstanceId() + ", priority: " + existing.get().getPriority());
							}
						} else {
							// no need to modify this entry, because this priority is already set
							continue;
					}

					entry.setPriority(priorities.get(entry.getId()));
					entry.setUpdatedBy(requester);
					modified.add(entry);
					}

				return storeRepo.saveAllAndFlush(modified);
			}
		} catch (InvalidParameterException ex) {
			throw ex;
		} catch (Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public void deleteBulk(final List<UUID> uuids) {
		Assert.isTrue(!Utilities.isEmpty(uuids), "UUID list is empty");

		try {
			storeRepo.deleteAllById(uuids);
			storeRepo.flush();
		} catch (Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	// Throws exception, if there is an existing entity in the DB with the same unique fields.
	private void checkUniqueFields(final List<OrchestrationSimpleStoreRequestDTO> candidates) {
		Assert.isTrue(!Utilities.isEmpty(candidates), "candidate list is empty");
		Assert.isTrue(!Utilities.containsNull(candidates), "candidate list contains null element");

		for (final OrchestrationSimpleStoreRequestDTO candidate : candidates) {

			// check if there is and existing record
			final Optional<OrchestrationStore> entity = storeRepo.findByConsumerAndServiceInstanceIdAndPriority(candidate.consumer(), candidate.serviceInstanceId(), candidate.priority());
			if (entity.isPresent()) {
				final OrchestrationStore existing = entity.get();
				throw new InvalidParameterException("There is already an existing entity with consumer name: " +  existing.getConsumer() + ", service instance id: "
						+ existing.getServiceInstanceId() + ", priority: " + existing.getPriority());
			}
		}
	}

	//=================================================================================================
	// nested classes

	private enum BaseFilter {
		NONE, ID, CONSUMER, DEFINITION, INSTANCE, CREATOR;
	}
}
