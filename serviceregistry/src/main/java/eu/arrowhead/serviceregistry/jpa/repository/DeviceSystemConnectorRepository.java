package eu.arrowhead.serviceregistry.jpa.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.serviceregistry.jpa.entity.DeviceSystemConnector;
import eu.arrowhead.serviceregistry.jpa.entity.System;

@Repository
public interface DeviceSystemConnectorRepository extends RefreshableRepository<DeviceSystemConnector, Long> {
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public Optional<DeviceSystemConnector> findBySystem(final System system);
}