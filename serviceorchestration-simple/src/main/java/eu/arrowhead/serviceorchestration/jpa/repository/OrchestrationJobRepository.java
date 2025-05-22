package eu.arrowhead.serviceorchestration.jpa.repository;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationJob;

@Repository
public interface OrchestrationJobRepository extends RefreshableRepository<OrchestrationJob, UUID> {
	
}