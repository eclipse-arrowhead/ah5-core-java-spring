package eu.arrowhead.authorization.jpa.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import eu.arrowhead.authorization.jpa.entity.TimeLimitedToken;
import eu.arrowhead.authorization.jpa.entity.TokenHeader;
import eu.arrowhead.common.jpa.RefreshableRepository;

@Repository
public interface TimeLimitedTokenRepository extends RefreshableRepository<TimeLimitedToken, Long> {
	
	//=================================================================================================
	// methods
	
	public Optional<TimeLimitedToken> findByHeader(final TokenHeader header);
}