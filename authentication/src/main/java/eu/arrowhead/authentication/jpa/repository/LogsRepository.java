package eu.arrowhead.authentication.jpa.repository;

import org.springframework.stereotype.Repository;

import eu.arrowhead.authentication.jpa.entity.Logs;
import eu.arrowhead.common.jpa.LogEntityRepository;

@Repository
public interface LogsRepository extends LogEntityRepository<Logs> {
}