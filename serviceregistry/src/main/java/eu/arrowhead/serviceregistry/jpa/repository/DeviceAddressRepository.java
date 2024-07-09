package eu.arrowhead.serviceregistry.jpa.repository;

import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.serviceregistry.jpa.entity.DeviceAddress;

@Repository
public interface DeviceAddressRepository extends RefreshableRepository<DeviceAddress, Long> {
}