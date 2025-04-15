package eu.arrowhead.authorization.jpa.repository;

import org.springframework.stereotype.Repository;

import eu.arrowhead.authorization.jpa.entity.CryptographerAuxiliary;
import eu.arrowhead.common.jpa.RefreshableRepository;

@Repository
public interface CryptographerAuxiliaryRepository extends RefreshableRepository<CryptographerAuxiliary, Long> {

}
