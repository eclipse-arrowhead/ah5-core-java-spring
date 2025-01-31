package eu.arrowhead.serviceregistry.jpa.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.serviceregistry.jpa.entity.Device;
import eu.arrowhead.serviceregistry.jpa.entity.DeviceSystemConnector;
import eu.arrowhead.serviceregistry.jpa.entity.System;

@Repository
public interface DeviceSystemConnectorRepository extends RefreshableRepository<DeviceSystemConnector, Long> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Optional<DeviceSystemConnector> findBySystem(final System system);

	//-------------------------------------------------------------------------------------------------
	public List<DeviceSystemConnector> findBySystemIn(final Collection<System> systems);

	//-------------------------------------------------------------------------------------------------
	public List<DeviceSystemConnector> findByDevice(final Device device);

	//-------------------------------------------------------------------------------------------------
	public List<DeviceSystemConnector> findByDeviceIn(final Collection<Device> devices);
}