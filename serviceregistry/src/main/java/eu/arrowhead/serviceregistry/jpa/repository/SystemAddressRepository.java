package eu.arrowhead.serviceregistry.jpa.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.serviceregistry.jpa.entity.SystemAddress;
import eu.arrowhead.serviceregistry.jpa.entity.System;

@Repository
public interface SystemAddressRepository extends RefreshableRepository<SystemAddress, Long> {
	
	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public List<SystemAddress> findAllBySystem(final System system);
}