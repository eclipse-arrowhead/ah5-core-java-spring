package eu.arrowhead.authorization.jpa.repository;

import org.springframework.stereotype.Repository;

import eu.arrowhead.authorization.jpa.entity.AuthMgmtPolicyHeader;
import eu.arrowhead.common.jpa.RefreshableRepository;

@Repository
public interface AuthMgmtPolicyHeaderRepository extends RefreshableRepository<AuthMgmtPolicyHeader, Long> {
}