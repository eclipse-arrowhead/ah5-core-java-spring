package eu.arrowhead.authentication.jpa.repository;

import org.springframework.stereotype.Repository;

import eu.arrowhead.authentication.jpa.entity.PasswordAuthentication;
import eu.arrowhead.common.jpa.RefreshableRepository;

@Repository
public interface PasswordAuthenticationRepository extends RefreshableRepository<PasswordAuthentication, Long> {
}