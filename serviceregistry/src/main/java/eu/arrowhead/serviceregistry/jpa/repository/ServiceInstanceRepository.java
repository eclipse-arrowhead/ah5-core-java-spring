package eu.arrowhead.serviceregistry.jpa.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInstance;

@SuppressWarnings("checkstyle:MethodNameCheck")
@Repository
public interface ServiceInstanceRepository extends RefreshableRepository<ServiceInstance, Long> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Optional<ServiceInstance> findByServiceInstanceId(final String serviceInstanceId);

	//-------------------------------------------------------------------------------------------------
	public List<ServiceInstance> findAllByServiceInstanceIdIn(final Collection<String> serviceInstanceIds);

	//-------------------------------------------------------------------------------------------------
	public Page<ServiceInstance> findAllByIdIn(final Collection<Long> ids, final Pageable pageble);

	//-------------------------------------------------------------------------------------------------
	public List<ServiceInstance> findAllBySystem_NameIn(final Collection<String> systemNames);

	//-------------------------------------------------------------------------------------------------
	public List<ServiceInstance> findAllByServiceDefinition_NameIn(final Collection<String> serviceDefinitionNames);
}