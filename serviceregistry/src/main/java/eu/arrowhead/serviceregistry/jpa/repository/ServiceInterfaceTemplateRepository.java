package eu.arrowhead.serviceregistry.jpa.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInterfaceTemplate;

@Repository
public interface ServiceInterfaceTemplateRepository extends RefreshableRepository<ServiceInterfaceTemplate, Long> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public List<ServiceInterfaceTemplate> findAllByNameIn(final Collection<String> names);

	//-------------------------------------------------------------------------------------------------
	public boolean existsByName(final String name);
}