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

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.dto.enums.OrchestrationType;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationJob;
import eu.arrowhead.serviceorchestration.jpa.repository.OrchestrationJobRepository;
import eu.arrowhead.serviceorchestration.service.enums.BaseFilter;
import eu.arrowhead.serviceorchestration.service.enums.OrchestrationJobStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.*;

@Service
public class OrchestrationJobDbService {

    //=================================================================================================
    // members

    @Autowired
    private OrchestrationJobRepository jobRepo;

    private final Logger logger = LogManager.getLogger(this.getClass());

    //=================================================================================================
    // methods

    //-------------------------------------------------------------------------------------------------
    @Transactional(rollbackFor = ArrowheadException.class)
    public List<OrchestrationJob> create(final List<OrchestrationJob> jobs) {
        logger.debug("create started...");
        Assert.isTrue(!Utilities.isEmpty(jobs), "job list is empty");
        Assert.isTrue(!Utilities.containsNull(jobs), "job list contains null element");

        try {
            return jobRepo.saveAllAndFlush(jobs);
        } catch (final Exception ex) {
            logger.error(ex.getMessage());
            logger.debug(ex);
            throw new InternalServerError("Database operation error");
        }
    }

    //-------------------------------------------------------------------------------------------------
    public Optional<OrchestrationJob> getById(final UUID id) {
        logger.debug("getById started...");
        Assert.notNull(id, "id is null");

        try {
            return jobRepo.findById(id);
        } catch (final Exception ex) {
            logger.error(ex.getMessage());
            logger.debug(ex);
            throw new InternalServerError("Database operation error");
        }
    }

    //-------------------------------------------------------------------------------------------------
    public List<OrchestrationJob> getAllByStatusIn(final List<OrchestrationJobStatus> statuses) {
        logger.debug("getAllByStatusIn started...");
        Assert.isTrue(!Utilities.isEmpty(statuses), "status list is empty");
        Assert.isTrue(!Utilities.containsNull(statuses), "status list contains null element");

        try {
            return jobRepo.findAllByStatusIn(statuses);
        } catch (final Exception ex) {
            logger.error(ex.getMessage());
            logger.debug(ex);
            throw new InternalServerError("Database operation error");
        }
    }

    //-------------------------------------------------------------------------------------------------
    @Transactional(rollbackFor = ArrowheadException.class)
    public OrchestrationJob setStatus(final UUID jobId, final OrchestrationJobStatus status, final String message) {
        logger.debug("setStatus started...");
        Assert.notNull(jobId, "jobId is null");
        Assert.notNull(status, "status is null");
        Assert.isTrue(status != OrchestrationJobStatus.PENDING, "status can't be changed to PENDING");

        try {
            final Optional<OrchestrationJob> optional = jobRepo.findById(jobId);
            Assert.isTrue(optional.isPresent(), "job does not exists: " + jobId);
            final OrchestrationJob job = optional.get();

            job.setStatus(status);
            if (!Utilities.isEmpty(message)) {
                job.setMessage(message);
            }

            switch (status) {
                case IN_PROGRESS:
                    job.setStartedAt(Utilities.utcNow());
                    job.setFinishedAt(null);
                    break;

                case DONE:
                case ERROR:
                    job.setFinishedAt(Utilities.utcNow());
                    break;

                default:
                    throw new IllegalArgumentException("Unhandled orchestration job status: " + status);
            }

            return jobRepo.saveAndFlush(job);

        } catch (final IllegalArgumentException ex) {
            throw ex;

        } catch (final Exception ex) {
            logger.error(ex.getMessage());
            logger.debug(ex);
            throw new InternalServerError("Database operation error");
        }
    }

    //-------------------------------------------------------------------------------------------------
    public Page<OrchestrationJob> query(
            final List<UUID> ids,
            final List<OrchestrationJobStatus> statuses,
            final OrchestrationType type,
            final List<String> requesterSystems,
            final List<String> targetSystems,
            final List<String> serviceDefinitions,
            final List<UUID> subscriptionIds,
            final PageRequest pagination) {
        logger.debug("query started...");
        Assert.notNull(pagination, "pagination is null");

        BaseFilter baseFilter = BaseFilter.NONE;

        if (!Utilities.isEmpty(ids)) {
            baseFilter = BaseFilter.ID;
        }
        if (!Utilities.isEmpty(statuses)) {
            baseFilter = BaseFilter.STATUS;
        }
        if (!Utilities.isEmpty(requesterSystems)) {
            baseFilter = BaseFilter.OWNER;
        }
        if (!Utilities.isEmpty(targetSystems)) {
            baseFilter = BaseFilter.TARGET;
        }
        if (!Utilities.isEmpty(serviceDefinitions)) {
            baseFilter = BaseFilter.SERVICE;
        }

        try {
            List<OrchestrationJob> toFilter;

            if (baseFilter == BaseFilter.ID) {
                toFilter = jobRepo.findAllById(ids);
            } else if (baseFilter == BaseFilter.STATUS) {
                toFilter = jobRepo.findAllByStatusIn(statuses);
            } else if (baseFilter == BaseFilter.OWNER) {
                toFilter = jobRepo.findAllByRequesterSystemIn(requesterSystems);
            } else if (baseFilter == BaseFilter.TARGET) {
                toFilter = jobRepo.findAllByTargetSystemIn(targetSystems);
            } else if (baseFilter == BaseFilter.SERVICE) {
                toFilter = jobRepo.findAllByServiceDefinitionIn(serviceDefinitions);
            } else {
                toFilter = jobRepo.findAll();
            }

            final List<UUID> matchingIds = new ArrayList<>();
            for (final OrchestrationJob job : toFilter) {
                boolean matching = true;

                // No need to match against the id, because that is the basefilter if not null

                // Match against to job statuses
                if (baseFilter != BaseFilter.STATUS && !Utilities.isEmpty(statuses) && !statuses.contains(job.getStatus())) {
                    matching = false;

                // Match against to job type
                } else if (type != null && type != job.getType()) {
                    matching = false;

                 // Match against to requester systems
                } else if (baseFilter != BaseFilter.OWNER && !Utilities.isEmpty(requesterSystems) && !requesterSystems.contains(job.getRequesterSystem())) {
                    matching = false;

                // Match against to target systems
                } else if (baseFilter != BaseFilter.TARGET && !Utilities.isEmpty(targetSystems) && !targetSystems.contains(job.getTargetSystem())) {
                    matching = false;

                // Match against to service definitions
                } else if (baseFilter != BaseFilter.SERVICE && !Utilities.isEmpty(serviceDefinitions) && !serviceDefinitions.contains(job.getServiceDefinition())) {
                    matching = false;

                // Match against to subscription ids
                } else if (!Utilities.isEmpty(subscriptionIds) && !Utilities.isEmpty(job.getSubscriptionId()) && !subscriptionIds.contains(job.getSubscriptionId())) {
                    matching = false;
                }

                if (matching) {
                    matchingIds.add(job.getId());
                }
            }

            return jobRepo.findAllByIdIn(matchingIds, pagination);
        } catch (final Exception ex) {
            logger.error(ex.getMessage());
            logger.debug(ex);
            throw new InternalServerError("Database operation error");
        }
    }

    //-------------------------------------------------------------------------------------------------
    @Transactional(rollbackFor = ArrowheadException.class)
    public void deleteInBatch(final Collection<UUID> ids) {
        logger.debug("deleteInBatch started...");
        Assert.isTrue(!Utilities.isEmpty(ids), "job id list is empty");
        Assert.isTrue(!Utilities.containsNull(ids), "job id list contains null element");

        try {
            jobRepo.deleteAllByIdInBatch(ids);
            jobRepo.flush();
        } catch (final Exception ex) {
            logger.error(ex.getMessage());
            logger.debug(ex);
            throw new InternalServerError("Database operation error");
        }
    }

}
