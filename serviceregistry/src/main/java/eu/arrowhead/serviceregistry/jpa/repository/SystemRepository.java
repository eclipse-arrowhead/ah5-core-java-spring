package eu.arrowhead.serviceregistry.jpa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.serviceregistry.jpa.entity.System;

@Repository
public interface SystemRepository extends RefreshableRepository<System, Long> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public List<System> findAllByNameIn(final List<String> names);

	//-------------------------------------------------------------------------------------------------
	public Optional<System> findByName(final String name);

	//-------------------------------------------------------------------------------------------------
	public Page<System> findAllByNameIn(final List<String> names, Pageable pageble);

	//-------------------------------------------------------------------------------------------------
	public List<System> findAllByVersionIn(final List<String> versions);
}