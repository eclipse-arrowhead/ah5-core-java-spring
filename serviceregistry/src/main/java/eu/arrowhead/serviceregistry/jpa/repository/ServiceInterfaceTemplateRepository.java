package eu.arrowhead.serviceregistry.jpa.repository;

import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInterfaceTemplate;

@Repository
public interface ServiceInterfaceTemplateRepository extends RefreshableRepository<ServiceInterfaceTemplate, Long> {
}