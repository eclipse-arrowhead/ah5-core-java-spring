package eu.arrowhead.authorization.jpa.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import eu.arrowhead.authorization.jpa.entity.TokenHeader;
import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.dto.enums.AuthorizationTokenType;

@Repository
public interface TokenHeaderRepository extends RefreshableRepository<TokenHeader, Long> {
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public List<TokenHeader> findAllByTokenIn(final List<String> tokens);
	
	//-------------------------------------------------------------------------------------------------
	public List<TokenHeader> findAllByRequester(final String requester);
	
	//-------------------------------------------------------------------------------------------------
	public List<TokenHeader> findAllByTokenType(final AuthorizationTokenType tokenType);
	
	//-------------------------------------------------------------------------------------------------
	public List<TokenHeader> findAllByConsumerCloud(final String consumerCloud);
	
	//-------------------------------------------------------------------------------------------------
	public List<TokenHeader> findAllByConsumer(final String consumer);
	
	//-------------------------------------------------------------------------------------------------
	public List<TokenHeader> findAllByProvider(final String provider);
	
	//-------------------------------------------------------------------------------------------------
	public List<TokenHeader> findAllByServiceDefinition(final String serviceDefinition);
	
	//-------------------------------------------------------------------------------------------------
	public Optional<TokenHeader> findByProviderAndToken(final String provider, final String token);
	
	//-------------------------------------------------------------------------------------------------
	public Optional<TokenHeader> findByConsumerCloudAndConsumerAndProviderAndServiceDefinition(final String consumerCloud, final String counsumer, final String provider, final String serviceDefinition);
	
	//-------------------------------------------------------------------------------------------------
	public Page<TokenHeader> findAllByIdIn(final Collection<Long> ids, final Pageable pageble);
}