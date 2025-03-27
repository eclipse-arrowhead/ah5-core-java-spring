package eu.arrowhead.authentication.jpa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import eu.arrowhead.authentication.jpa.entity.System;
import eu.arrowhead.common.jpa.RefreshableRepository;

@Repository
public interface SystemRepository extends RefreshableRepository<System, Long> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Optional<System> findByName(final String systemName);

	//-------------------------------------------------------------------------------------------------
	public List<System> findAllByNameIn(final List<String> names);

	//-------------------------------------------------------------------------------------------------
	public List<System> findAllByNameContains(final String namePart);

	//-------------------------------------------------------------------------------------------------
	public Page<System> findAllByIdIn(final Pageable pageable, final List<Long> ids);
}