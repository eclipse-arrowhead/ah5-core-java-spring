package eu.arrowhead.authorization.jpa.repository;

import org.springframework.stereotype.Repository;

import eu.arrowhead.authorization.jpa.entity.Logs;
import eu.arrowhead.common.jpa.LogEntityRepository;

@Repository
public interface LogsRepository extends LogEntityRepository<Logs> {
}