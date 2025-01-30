package eu.arrowhead.authentication.jpa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import eu.arrowhead.authentication.jpa.entity.PasswordAuthentication;
import eu.arrowhead.authentication.jpa.entity.System;
import eu.arrowhead.common.jpa.RefreshableRepository;

@Repository
public interface PasswordAuthenticationRepository extends RefreshableRepository<PasswordAuthentication, Long> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Optional<PasswordAuthentication> findBySystem(final System system);

	//-------------------------------------------------------------------------------------------------
	public List<PasswordAuthentication> findAllBySystemIn(final List<System> systems);
}