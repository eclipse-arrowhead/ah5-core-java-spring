package eu.arrowhead.authentication.jpa.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import eu.arrowhead.authentication.jpa.entity.ActiveSession;
import eu.arrowhead.authentication.jpa.entity.System;
import eu.arrowhead.common.jpa.RefreshableRepository;

@Repository
public interface ActiveSessionRepository extends RefreshableRepository<ActiveSession, Long> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Optional<ActiveSession> findBySystem(final System system);

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MethodNameCheck")
	public Optional<ActiveSession> deleteBySystem_Name(final String systemName);
}