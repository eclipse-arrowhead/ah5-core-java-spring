package eu.arrowhead.authorization.jpa.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import eu.arrowhead.authorization.jpa.entity.SelfContainedToken;
import eu.arrowhead.authorization.jpa.entity.TokenHeader;
import eu.arrowhead.common.jpa.RefreshableRepository;

@Repository
public interface SelfContainedTokenRepository extends RefreshableRepository<SelfContainedToken, Long> {
	
	//=================================================================================================
	// methods
	
	public Optional<SelfContainedToken> findByHeader(final TokenHeader header);
}