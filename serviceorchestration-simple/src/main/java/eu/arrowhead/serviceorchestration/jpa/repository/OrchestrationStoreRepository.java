package eu.arrowhead.serviceorchestration.jpa.repository;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationStore;

@Repository
public interface OrchestrationStoreRepository extends RefreshableRepository<OrchestrationStore, UUID> {

}
