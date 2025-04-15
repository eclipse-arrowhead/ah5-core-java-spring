package eu.arrowhead.authorization.jpa.repository;

import org.springframework.stereotype.Repository;

import eu.arrowhead.authorization.jpa.entity.CryptographerIV;
import eu.arrowhead.common.jpa.RefreshableRepository;

@Repository
public interface CryptographerIVRepository extends RefreshableRepository<CryptographerIV, Long> {

}
