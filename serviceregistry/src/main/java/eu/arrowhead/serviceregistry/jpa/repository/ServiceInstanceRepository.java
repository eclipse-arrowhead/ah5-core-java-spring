package eu.arrowhead.serviceregistry.jpa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInstance;

@Repository
public interface ServiceInstanceRepository extends RefreshableRepository<ServiceInstance, Long> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Optional<ServiceInstance> findByServiceInstanceId(final String serviceInstanceId);

	//-------------------------------------------------------------------------------------------------
	public List<ServiceInstance> findAllByServiceInstanceIdIn(final List<String> serviceInstanceIds);
}