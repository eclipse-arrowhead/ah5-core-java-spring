package eu.arrowhead.serviceorchestration.jpa.repository;

import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.LogEntityRepository;
import eu.arrowhead.serviceorchestration.jpa.entity.Logs;

@Repository
public interface LogsRepository extends LogEntityRepository<Logs> {
}