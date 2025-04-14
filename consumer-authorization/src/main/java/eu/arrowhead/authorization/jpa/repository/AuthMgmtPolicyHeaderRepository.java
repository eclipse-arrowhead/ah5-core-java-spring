package eu.arrowhead.authorization.jpa.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import eu.arrowhead.authorization.jpa.entity.AuthMgmtPolicyHeader;
import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.dto.enums.AuthorizationTargetType;

@Repository
public interface AuthMgmtPolicyHeaderRepository extends RefreshableRepository<AuthMgmtPolicyHeader, Long> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Optional<AuthMgmtPolicyHeader> findByCloudAndProviderAndTargetTypeAndTarget(final String cloud, final String provider, final AuthorizationTargetType targetType, final String target);
}