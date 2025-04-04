package eu.arrowhead.authorization.jpa.repository;

import org.springframework.stereotype.Repository;

import eu.arrowhead.authorization.jpa.entity.TokenHeader;
import eu.arrowhead.common.jpa.RefreshableRepository;

@Repository
public interface TokenHeaderRepository extends RefreshableRepository<TokenHeader, Long> {
}