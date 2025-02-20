package eu.arrowhead.serviceorchestration.jpa.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationLock;

@Repository
public interface OrchestrationLockRepository extends RefreshableRepository<OrchestrationLock, Long> {

	//-------------------------------------------------------------------------------------------------
	Page<OrchestrationLock> findAllByIdIn(final Collection<Long> ids, final Pageable pageble);

	//-------------------------------------------------------------------------------------------------
	List<OrchestrationLock> findAllByServiceInstanceIdIn(final List<String> serviceInstanceIds);

	//-------------------------------------------------------------------------------------------------
	List<OrchestrationLock> findAllByOrchestrationJobIdIn(final List<String> orchestrationJobIds);

	//-------------------------------------------------------------------------------------------------
	List<OrchestrationLock> findAllByOwnerIn(final List<String> owners);

	//-------------------------------------------------------------------------------------------------
	Optional<OrchestrationLock> findByOrchestrationJobIdAndServiceInstanceId(final String orchestrationJobId, final String serviceInstanceId);
}
