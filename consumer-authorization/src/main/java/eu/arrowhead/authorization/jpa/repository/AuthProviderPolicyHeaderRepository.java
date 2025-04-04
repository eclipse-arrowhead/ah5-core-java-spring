package eu.arrowhead.authorization.jpa.repository;

import org.springframework.stereotype.Repository;

import eu.arrowhead.authorization.jpa.entity.AuthProviderPolicyHeader;
import eu.arrowhead.common.jpa.RefreshableRepository;

@Repository
public interface AuthProviderPolicyHeaderRepository extends RefreshableRepository<AuthProviderPolicyHeader, Long> {
}