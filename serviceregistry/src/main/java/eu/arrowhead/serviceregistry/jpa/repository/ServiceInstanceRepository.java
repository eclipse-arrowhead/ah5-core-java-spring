package eu.arrowhead.serviceregistry.jpa.repository;

import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInstance;

@Repository
public interface ServiceInstanceRepository extends RefreshableRepository<ServiceInstance, Long> {
}