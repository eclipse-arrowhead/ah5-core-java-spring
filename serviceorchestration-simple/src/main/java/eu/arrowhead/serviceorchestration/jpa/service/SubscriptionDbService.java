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
import eu.arrowhead.serviceorchestration.jpa.entity.Subscription;
import eu.arrowhead.serviceorchestration.jpa.repository.SubscriptionRepository;
import eu.arrowhead.serviceorchestration.service.model.SimpleOrchestrationSubscriptionRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SubscriptionDbService {

    //=================================================================================================
    // members

    @Autowired
    private SubscriptionRepository subscriptionRepo;

    private final Logger logger = LogManager.getLogger(this.getClass());

    //=================================================================================================
    // methods

    //-------------------------------------------------------------------------------------------------
    @Transactional(rollbackFor = ArrowheadException.class)
    public List<Subscription> create(final List<SimpleOrchestrationSubscriptionRequest> candidates, final String requesterSystemName) {
        logger.debug("create started..");
        Assert.isTrue(!Utilities.isEmpty(candidates), "subscription candidate list is empty");
        Assert.isTrue(!Utilities.containsNull(candidates), "subscription candidate list contains null element");

        try {
            final List<UUID> toRemove = new ArrayList<>();
            final List<Subscription> toSave = new ArrayList<>(candidates.size());
            final ZonedDateTime now = Utilities.utcNow();
            for (final SimpleOrchestrationSubscriptionRequest candidate : candidates) {
                final Optional<Subscription> optional = subscriptionRepo.findByOwnerSystemAndTargetSystemAndServiceDefinition(
                        requesterSystemName,
                        candidate.getTargetSystemName(),
                        candidate.getOrchestrationRequest().getServiceDefinition());

                if (optional.isPresent()) {
                    toRemove.add(optional.get().getId());
                }

                final ZonedDateTime expiresAt = candidate.getDuration() == null ? null : now.plusSeconds(candidate.getDuration());
                toSave.add(new Subscription(
                        requesterSystemName,
                        candidate.getTargetSystemName(),
                        candidate.getOrchestrationRequest().getServiceDefinition(),
                        expiresAt,
                        candidate.getNotifyInterface().protocol(),
                        Utilities.toJson(candidate.getNotifyInterface().properties()),
                        Utilities.toJson(candidate.getOrchestrationRequest())));
            }

            if (!Utilities.isEmpty(toRemove)) {
                subscriptionRepo.deleteAllById(toRemove);
            }
            subscriptionRepo.flush();

            return subscriptionRepo.saveAllAndFlush(toSave);
        } catch (final Exception ex) {
            logger.error(ex.getMessage());
            logger.debug(ex);
            throw new InternalServerError("Database operation error");
        }
    }

    //-------------------------------------------------------------------------------------------------
    public Optional<Subscription> get(final String ownerSystem, final String targetSystem, final String serviceDefinition) {
        logger.debug("get started...");
        Assert.isTrue(!Utilities.isEmpty(ownerSystem), "ownerSystem is empty");
        Assert.isTrue(!Utilities.isEmpty(targetSystem), "targetSystem is empty");
        Assert.isTrue(!Utilities.isEmpty(serviceDefinition), "serviceDefinition is empty");

        try {
            return subscriptionRepo.findByOwnerSystemAndTargetSystemAndServiceDefinition(ownerSystem, targetSystem, serviceDefinition);
        } catch (final Exception ex) {
            logger.error(ex.getMessage());
            logger.debug(ex);
            throw new InternalServerError("Database operation error");
        }
    }

}
