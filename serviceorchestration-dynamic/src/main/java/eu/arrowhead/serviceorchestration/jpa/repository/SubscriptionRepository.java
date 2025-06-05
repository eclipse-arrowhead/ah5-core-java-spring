package eu.arrowhead.serviceorchestration.jpa.repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.serviceorchestration.jpa.entity.Subscription;

@Repository
public interface SubscriptionRepository extends RefreshableRepository<Subscription, UUID> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Optional<Subscription> findByOwnerSystemAndTargetSystemAndServiceDefinition(final String ownerSystem, final String targetSystem, final String serviceDefinition);

	//-------------------------------------------------------------------------------------------------
	public List<Subscription> findByOwnerSystemIn(final List<String> ownerSystems);

	//-------------------------------------------------------------------------------------------------
	public List<Subscription> findByTargetSystemIn(final List<String> targetSystems);

	//-------------------------------------------------------------------------------------------------
	public List<Subscription> findByServiceDefinitionIn(final List<String> serviceDefinitions);

	//-------------------------------------------------------------------------------------------------
	public Page<Subscription> findByIdIn(final List<UUID> ids, final Pageable pagination);

	//-------------------------------------------------------------------------------------------------
	public List<Subscription> findAllByExpiresAtBefore(final ZonedDateTime time);
}