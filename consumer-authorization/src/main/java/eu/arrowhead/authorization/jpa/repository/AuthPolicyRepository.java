package eu.arrowhead.authorization.jpa.repository;

import org.springframework.stereotype.Repository;

import eu.arrowhead.authorization.jpa.entity.AuthPolicy;
import eu.arrowhead.common.jpa.RefreshableRepository;

@Repository
public interface AuthPolicyRepository extends RefreshableRepository<AuthPolicy, Long> {
}