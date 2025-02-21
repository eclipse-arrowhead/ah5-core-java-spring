package eu.arrowhead.serviceorchestration.jpa.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.serviceorchestration.jpa.entity.Subscription;

@Repository
public interface SubscriptionRepository extends RefreshableRepository<Subscription, UUID> {

	//-------------------------------------------------------------------------------------------------
	Optional<Subscription> findByOwnerSystemAndTargetSystemAndServiceDefinition(final String ownerSystem, final String targetSystem, final String serviceDefinition);

	//-------------------------------------------------------------------------------------------------
	List<Subscription> findByOwnerSystemIn(final List<String> ownerSystems);

	//-------------------------------------------------------------------------------------------------
	List<Subscription> findByTargetSystemIn(final List<String> targetSystems);

	//-------------------------------------------------------------------------------------------------
	List<Subscription> findByServiceDefinitionIn(final List<String> serviceDefinitions);

	//-------------------------------------------------------------------------------------------------
	Page<Subscription> findByIdIn(final List<UUID> ids, PageRequest pagination);

}
