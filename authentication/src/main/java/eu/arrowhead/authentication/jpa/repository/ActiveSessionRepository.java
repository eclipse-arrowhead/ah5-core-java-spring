package eu.arrowhead.authentication.jpa.repository;

import org.springframework.stereotype.Repository;

import eu.arrowhead.authentication.jpa.entity.ActiveSession;
import eu.arrowhead.common.jpa.RefreshableRepository;

@Repository
public interface ActiveSessionRepository extends RefreshableRepository<ActiveSession, Long> {
}