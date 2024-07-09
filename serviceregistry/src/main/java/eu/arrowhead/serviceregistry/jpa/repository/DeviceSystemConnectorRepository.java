package eu.arrowhead.serviceregistry.jpa.repository;

import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.serviceregistry.jpa.entity.DeviceSystemConnector;

@Repository
public interface DeviceSystemConnectorRepository extends RefreshableRepository<DeviceSystemConnector, Long> {
}