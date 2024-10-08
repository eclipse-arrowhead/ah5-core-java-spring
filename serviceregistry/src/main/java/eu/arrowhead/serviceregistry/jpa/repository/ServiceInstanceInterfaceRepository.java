package eu.arrowhead.serviceregistry.jpa.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInstance;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInstanceInterface;

@Repository
public interface ServiceInstanceInterfaceRepository extends RefreshableRepository<ServiceInstanceInterface, Long> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public List<ServiceInstanceInterface> findAllByServiceInstance(final ServiceInstance serviceInstance);
}