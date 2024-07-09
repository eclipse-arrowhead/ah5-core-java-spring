package eu.arrowhead.serviceregistry.jpa.repository;

import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInstanceInterface;

@Repository
public interface ServiceInstanceInterfaceRepository extends RefreshableRepository<ServiceInstanceInterface, Long> {
}