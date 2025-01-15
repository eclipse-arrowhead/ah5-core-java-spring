package eu.arrowhead.authentication.jpa.repository;

import org.springframework.stereotype.Repository;

import eu.arrowhead.authentication.jpa.entity.System;
import eu.arrowhead.common.jpa.RefreshableRepository;

@Repository
public interface SystemRepository extends RefreshableRepository<System, Long> {
}