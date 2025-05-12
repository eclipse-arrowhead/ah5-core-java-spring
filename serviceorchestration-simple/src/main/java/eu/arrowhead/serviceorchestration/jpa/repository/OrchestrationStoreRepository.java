package eu.arrowhead.serviceorchestration.jpa.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationStore;

@Repository
public interface OrchestrationStoreRepository extends RefreshableRepository<OrchestrationStore, UUID> {
	
	//-------------------------------------------------------------------------------------------------
	public List<OrchestrationStore> findAllByConsumerAndServiceInstanceIdAndPriority(
			final String consumer, final String serviceInstanceId, final int priority);
	
}
