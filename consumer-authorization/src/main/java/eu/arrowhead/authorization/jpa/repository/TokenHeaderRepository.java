package eu.arrowhead.authorization.jpa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import eu.arrowhead.authorization.jpa.entity.TokenHeader;
import eu.arrowhead.common.jpa.RefreshableRepository;

@Repository
public interface TokenHeaderRepository extends RefreshableRepository<TokenHeader, Long> {
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public List<TokenHeader> findAllByTokenIn(final List<String> tokens);
	
	//-------------------------------------------------------------------------------------------------
	public Optional<TokenHeader> findByProviderAndToken(final String provider, final String token);
	
	//-------------------------------------------------------------------------------------------------
	public Optional<TokenHeader> findByConsumerCloudAndConsumerAndProviderAndServiceDefinition(final String consumerCloud, final String counsumer, final String provider, final String serviceDefinition);
}