package eu.arrowhead.serviceregistry.jpa.repository;

import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;

@Repository
public interface SystemRepository extends RefreshableRepository<System, Long> {
}