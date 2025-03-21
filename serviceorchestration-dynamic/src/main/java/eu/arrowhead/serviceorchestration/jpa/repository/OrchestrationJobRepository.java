package eu.arrowhead.serviceorchestration.jpa.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationJob;

@Repository
public interface OrchestrationJobRepository extends RefreshableRepository<OrchestrationJob, UUID> {

	//-------------------------------------------------------------------------------------------------
	public Page<OrchestrationJob> findAllByIdIn(final Collection<UUID> ids, final Pageable pageable);

	//-------------------------------------------------------------------------------------------------
	public List<OrchestrationJob> findAllByStatusIn(final List<String> statuses);

	//-------------------------------------------------------------------------------------------------
	public List<OrchestrationJob> findAllByRequesterSystemIn(final List<String> requesterSystems);

	//-------------------------------------------------------------------------------------------------
	public List<OrchestrationJob> findAllByTargetSystemIn(final List<String> targetSystems);

	//-------------------------------------------------------------------------------------------------
	public List<OrchestrationJob> findAllByServiceDefinitionIn(final List<String> serviceDefinitions);
}
