package eu.arrowhead.authorization.jpa.repository;

import org.springframework.stereotype.Repository;

import eu.arrowhead.authorization.jpa.entity.UsageLimitedToken;
import eu.arrowhead.common.jpa.RefreshableRepository;

@Repository
public interface UsageLimitedTokenRepository extends RefreshableRepository<UsageLimitedToken, Long> {
}