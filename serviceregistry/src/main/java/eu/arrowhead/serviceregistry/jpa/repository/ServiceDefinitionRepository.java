package eu.arrowhead.serviceregistry.jpa.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceDefinition;

@Repository
public interface ServiceDefinitionRepository extends RefreshableRepository<ServiceDefinition, Long> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public List<ServiceDefinition> findAllByNameIn(final Collection<String> names);
}