package eu.arrowhead.authorization.jpa.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import eu.arrowhead.authorization.jpa.entity.AuthProviderPolicyHeader;
import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.dto.enums.AuthorizationTargetType;

@Repository
public interface AuthProviderPolicyHeaderRepository extends RefreshableRepository<AuthProviderPolicyHeader, Long> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Optional<AuthProviderPolicyHeader> findByCloudAndProviderAndTargetTypeAndTarget(final String cloud, final String provider, final AuthorizationTargetType targetType, final String target);

	//-------------------------------------------------------------------------------------------------
	public Optional<AuthProviderPolicyHeader> findByInstanceId(final String instanceId);
}