package eu.arrowhead.authorization.jpa.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import eu.arrowhead.authorization.jpa.entity.AuthPolicy;
import eu.arrowhead.common.jpa.RefreshableRepository;

@Repository
public interface AuthPolicyRepository extends RefreshableRepository<AuthPolicy, Long> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public void deleteByHeaderId(final long headerId);

	//-------------------------------------------------------------------------------------------------
	public List<AuthPolicy> findByHeaderId(final long headerId);
}