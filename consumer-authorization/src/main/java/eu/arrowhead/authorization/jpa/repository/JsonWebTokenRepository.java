package eu.arrowhead.authorization.jpa.repository;

import org.springframework.stereotype.Repository;

import eu.arrowhead.authorization.jpa.entity.JsonWebToken;
import eu.arrowhead.common.jpa.RefreshableRepository;

@Repository
public interface JsonWebTokenRepository extends RefreshableRepository<JsonWebToken, Long> {
}