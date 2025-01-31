package eu.arrowhead.serviceregistry.jpa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.serviceregistry.jpa.entity.Device;

@Repository
public interface DeviceRepository extends RefreshableRepository<Device, Long> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Optional<Device> findByName(final String name);

	//-------------------------------------------------------------------------------------------------
	public List<Device> findAllByNameIn(final List<String> names);

	//-------------------------------------------------------------------------------------------------
	public Page<Device> findAllByNameIn(final List<String> names, final Pageable pageble);
}