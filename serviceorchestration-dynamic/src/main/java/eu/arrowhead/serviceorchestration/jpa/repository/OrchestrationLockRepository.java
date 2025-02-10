package eu.arrowhead.serviceorchestration.jpa.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationLock;

@Repository
public interface OrchestrationLockRepository extends RefreshableRepository<OrchestrationLock, Long> {

	List<OrchestrationLock> findAllByServiceInstanceIdIn(final List<String> serviceInstanceIds);
}
