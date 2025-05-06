package eu.arrowhead.authorization.jpa.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

	//-------------------------------------------------------------------------------------------------
	public List<AuthMgmtPolicyHeader> findByInstanceIdIn(final Collection<String> instanceIds);

	//-------------------------------------------------------------------------------------------------
	public List<AuthMgmtPolicyHeader> findByProviderIn(final List<String> providers);

	//-------------------------------------------------------------------------------------------------
	public List<AuthMgmtPolicyHeader> findByCloudIn(final List<String> clouds);

	//-------------------------------------------------------------------------------------------------
	public List<AuthMgmtPolicyHeader> findByTargetIn(final List<String> targets);

	//-------------------------------------------------------------------------------------------------
	public Page<AuthMgmtPolicyHeader> findAllByIdIn(final Collection<Long> ids, final Pageable pageble);
}