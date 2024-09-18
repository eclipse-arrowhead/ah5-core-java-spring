package eu.arrowhead.serviceregistry.jpa.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.jpa.entity.SystemAddress;
import eu.arrowhead.serviceregistry.jpa.entity.System;

@Repository
public interface SystemAddressRepository extends RefreshableRepository<SystemAddress, Long> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public List<SystemAddress> findAllBySystem(final System system);

	//-------------------------------------------------------------------------------------------------
	public List<SystemAddress> findAllBySystemIn(final List<System> system);

	//-------------------------------------------------------------------------------------------------
	public List<SystemAddress> findAllBySystemAndAddressType(final System system, final AddressType addressType);

	//-------------------------------------------------------------------------------------------------
	public List<SystemAddress> findAllBySystemAndAddressIn(final System system, final List<String> addresses);

	//-------------------------------------------------------------------------------------------------
	public List<SystemAddress> deleteAllBySystemIn(final List<System> systems);
}