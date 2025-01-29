package eu.arrowhead.serviceorchestration.jpa.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.serviceorchestration.jpa.entity.Subscription;

@Repository
public interface SubscriptionRepository extends RefreshableRepository<Subscription, UUID> {

	//-------------------------------------------------------------------------------------------------
	Optional<Subscription> findByOwnerSystemAndTargetSystemAndServiceDefinition(final String ownerSystem, final String targetSystem, final String serviceDefinition);
}
