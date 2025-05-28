package eu.arrowhead.authorization.jpa.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import eu.arrowhead.authorization.jpa.entity.TokenHeader;
import eu.arrowhead.authorization.jpa.entity.UsageLimitedToken;
import eu.arrowhead.common.jpa.RefreshableRepository;

@Repository
public interface UsageLimitedTokenRepository extends RefreshableRepository<UsageLimitedToken, Long> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Optional<UsageLimitedToken> findByHeader(final TokenHeader header);
}