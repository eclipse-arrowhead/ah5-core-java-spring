package eu.arrowhead.serviceregistry.jpa.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInterfaceTemplate;

@Repository
public interface ServiceInterfaceTemplateRepository extends RefreshableRepository<ServiceInterfaceTemplate, Long> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Page<ServiceInterfaceTemplate> findAllByIdIn(final Collection<Long> ids, final Pageable pageble);

	//-------------------------------------------------------------------------------------------------
	public List<ServiceInterfaceTemplate> findAllByNameIn(final Collection<String> names);

	//-------------------------------------------------------------------------------------------------
	public List<ServiceInterfaceTemplate> findAllByProtocolIn(final Collection<String> protocols);

	//-------------------------------------------------------------------------------------------------
	public Optional<ServiceInterfaceTemplate> findByName(final String name);

	//-------------------------------------------------------------------------------------------------
	public boolean existsByName(final String name);
}