package eu.arrowhead.authorization.jpa.repository;

import org.springframework.stereotype.Repository;

import eu.arrowhead.authorization.jpa.entity.TimeLimitedToken;
import eu.arrowhead.common.jpa.RefreshableRepository;

@Repository
public interface TimeLimitedTokenRepository extends RefreshableRepository<TimeLimitedToken, Long> {
}