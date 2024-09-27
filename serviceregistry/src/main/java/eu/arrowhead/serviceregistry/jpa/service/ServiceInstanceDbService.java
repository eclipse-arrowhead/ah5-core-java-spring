package eu.arrowhead.serviceregistry.jpa.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.ServiceInstanceInterfaceRequestDTO;
import eu.arrowhead.dto.ServiceInstanceRequestDTO;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;
import eu.arrowhead.serviceregistry.ServiceRegistrySystemInfo;
import eu.arrowhead.serviceregistry.api.http.utils.ServiceInstanceIdUtils;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceDefinition;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInstance;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInstanceInterface;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInterfaceTemplate;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInterfaceTemplateProperty;
import eu.arrowhead.serviceregistry.jpa.entity.System;
import eu.arrowhead.serviceregistry.jpa.repository.ServiceDefinitionRepository;
import eu.arrowhead.serviceregistry.jpa.repository.ServiceInstanceInterfaceRepository;
import eu.arrowhead.serviceregistry.jpa.repository.ServiceInstanceRepository;
import eu.arrowhead.serviceregistry.jpa.repository.ServiceInterfaceTemplatePropertyRepository;
import eu.arrowhead.serviceregistry.jpa.repository.ServiceInterfaceTemplateRepository;
import eu.arrowhead.serviceregistry.jpa.repository.SystemRepository;
import eu.arrowhead.serviceregistry.service.ServiceDiscoveryInterfacePolicy;

@Service
public class ServiceInstanceDbService {

	//=================================================================================================
	// members

	@Autowired
	private ServiceRegistrySystemInfo sysInfo;

	@Autowired
	private ServiceInstanceRepository serviceInstanceRepo;

	@Autowired
	private ServiceInstanceInterfaceRepository serviceInstanceInterfaceRepo;

	@Autowired
	private SystemRepository systemRepo;

	@Autowired
	private ServiceDefinitionRepository serviceDefinitionRepo;

	@Autowired
	private ServiceInterfaceTemplateRepository serviceInterfaceTemplateRepo;

	@Autowired
	private ServiceInterfaceTemplatePropertyRepository serviceInterfaceTemplatePropsRepo;

	private static final Object LOCK = new Object();

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public List<Entry<ServiceInstance, List<ServiceInstanceInterface>>> createBulk(final List<ServiceInstanceRequestDTO> candidates) {
		logger.debug("createBulk started");
		Assert.isTrue(!Utilities.isEmpty(candidates), "service instance candidate list is empty");

		final Map<String, System> systemCache = new HashMap<>();
		final Map<String, ServiceDefinition> definitionCache = new HashMap<>();
		final Map<String, ServiceInterfaceTemplate> interfaceTemplateCache = new HashMap<>();

		try {
			List<ServiceInstance> instanceEntities = new ArrayList<>(candidates.size());
			List<ServiceInstanceInterface> instanceInterfaceEntities = new ArrayList<>();

			synchronized (LOCK) {
				final Set<String> systemNames = new HashSet<>();
				final Set<String> serviceDefinitionNames = new HashSet<>();
				final Set<String> serviceInterfaceTemlateNames = new HashSet<>();
				for (final ServiceInstanceRequestDTO candidate : candidates) {
					systemNames.add(candidate.systemName());
					serviceDefinitionNames.add(candidate.serviceDefinitionName());
					serviceInterfaceTemlateNames.addAll(candidate.interfaces().stream().map(template -> template.templateName()).collect(Collectors.toSet()));
				}

				systemRepo.findAllByNameIn(systemNames).forEach(system -> systemCache.put(system.getName(), system));
				serviceDefinitionRepo.findAllByNameIn(serviceDefinitionNames).forEach(definition -> definitionCache.put(definition.getName(), definition));
				serviceInterfaceTemplateRepo.findAllByNameIn(serviceInterfaceTemlateNames).forEach(template -> interfaceTemplateCache.put(template.getName(), template));

				if (sysInfo.getServiceDisciveryInterfacePolicy() == ServiceDiscoveryInterfacePolicy.RESTRICTED) {
					serviceInterfaceTemlateNames.forEach(templateName -> {
						if (!interfaceTemplateCache.containsKey(templateName)) {
							throw new InvalidParameterException("Interface template not exists: " + templateName);
						}
					});
				}

				// Handle instance records
				for (final ServiceInstanceRequestDTO candidate : candidates) {
					if (!systemCache.containsKey(candidate.systemName())) {
						throw new InvalidParameterException("System not exists: " + candidate.systemName());
					}
					if (!definitionCache.containsKey(candidate.serviceDefinitionName())) {
						definitionCache.put(
								candidate.serviceDefinitionName(),
								serviceDefinitionRepo.saveAndFlush(new ServiceDefinition(candidate.serviceDefinitionName())));
					}
					final String instanceId = ServiceInstanceIdUtils.calculateInstanceId(candidate.systemName(), candidate.serviceDefinitionName(), candidate.version());

					instanceEntities.add(new ServiceInstance(
							instanceId,
							systemCache.get(candidate.systemName()),
							definitionCache.get(candidate.serviceDefinitionName()),
							candidate.version(),
							Utilities.parseUTCStringToZonedDateTime(candidate.expiresAt()),
							Utilities.toJson(candidate.metadata())));
				}

				instanceEntities = serviceInstanceRepo.saveAllAndFlush(instanceEntities);

				// Handle instance interface records
				Set<String> templatesCreated = new HashSet<>();
				for (final ServiceInstanceRequestDTO candidate : candidates) {
					final ServiceInstance serviceInstance = instanceEntities
							.stream()
							.filter(i -> i.getServiceInstanceId().equals(ServiceInstanceIdUtils.calculateInstanceId(candidate.systemName(), candidate.serviceDefinitionName(), candidate.version())))
							.findFirst()
							.get();

					for (final ServiceInstanceInterfaceRequestDTO interfaceCandidate : candidate.interfaces()) {
						Assert.isTrue(Utilities.isEnumValue(interfaceCandidate.policy(), ServiceInterfacePolicy.class), "Invalid service interface policy");

						if (!interfaceTemplateCache.containsKey(interfaceCandidate.templateName())) {
							if (Utilities.isEmpty(interfaceCandidate.protocol())) {
								throw new InvalidParameterException("No protocol has been defined");
							}

							interfaceTemplateCache.put(
									interfaceCandidate.templateName(),
									serviceInterfaceTemplateRepo.saveAndFlush(new ServiceInterfaceTemplate(interfaceCandidate.templateName(), interfaceCandidate.protocol())));
							templatesCreated.add(interfaceCandidate.templateName());

							if (sysInfo.getServiceDisciveryInterfacePolicy() == ServiceDiscoveryInterfacePolicy.EXTENDABLE) {
								final List<ServiceInterfaceTemplateProperty> templateProps = new ArrayList<>();
								interfaceCandidate.properties().keySet().forEach(propName -> {
									templateProps.add(new ServiceInterfaceTemplateProperty(
											interfaceTemplateCache.get(interfaceCandidate.templateName()),
											propName,
											false,
											null));
								});
							}
						}

						// Need to validate the properties if the template was created in this bulk operation (only in case of EXTENDABLE policy)
						if (templatesCreated.contains(interfaceCandidate.templateName()) && sysInfo.getServiceDisciveryInterfacePolicy() == ServiceDiscoveryInterfacePolicy.EXTENDABLE) {
							serviceInterfaceTemplatePropsRepo.findByServiceInterfaceTemplate(interfaceTemplateCache.get(interfaceCandidate.templateName())).forEach(templateProp -> {
								if (templateProp.isMandatory() && !interfaceCandidate.properties().containsKey(templateProp.getPropertyName())) {
									throw new InvalidParameterException("Mandatory interface property is missing: " + templateProp.getPropertyName());
								}
							});
						}

						if (!Utilities.isEmpty(interfaceCandidate.protocol()) && interfaceTemplateCache.get(interfaceCandidate.templateName()).getProtocol().equalsIgnoreCase(interfaceCandidate.protocol())) {
							throw new InvalidParameterException("Interface has different protocol than " + interfaceCandidate.templateName() + " template");
						}

						instanceInterfaceEntities.add(new ServiceInstanceInterface(
								serviceInstance,
								interfaceTemplateCache.get(interfaceCandidate.templateName()),
								Utilities.toJson(interfaceCandidate.properties()),
								ServiceInterfacePolicy.valueOf(interfaceCandidate.policy())));
					}
				}

				instanceInterfaceEntities = serviceInstanceInterfaceRepo.saveAllAndFlush(instanceInterfaceEntities);
			}

			final List<Entry<ServiceInstance, List<ServiceInstanceInterface>>> results = new ArrayList<>(candidates.size());
			for (final ServiceInstance instance : instanceEntities) {
				results.add(Map.entry(
						instance,
						instanceInterfaceEntities.stream().filter(interf -> interf.getServiceInstance().getServiceInstanceId().equals(instance.getServiceInstanceId())).toList()));
			}

			return results;

		} catch (final InvalidParameterException ex) {
			throw ex;

		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public Optional<ServiceInstance> getByInstanceId(final String serviceInstanceId) {
		logger.debug("getByInstanceId started");
		Assert.isTrue(!Utilities.isEmpty(serviceInstanceId), "serviceInstanceId is empty");

		try {
			return serviceInstanceRepo.findByServiceInstanceId(serviceInstanceId);

		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public boolean deleteByInstanceId(final String serviceInstanceId) {
		logger.debug("deleteByInstanceId started");
		Assert.isTrue(!Utilities.isEmpty(serviceInstanceId), "serviceInstanceId is empty");

		try {
			final Optional<ServiceInstance> optional = serviceInstanceRepo.findByServiceInstanceId(serviceInstanceId);
			if (optional.isPresent()) {
				serviceInstanceRepo.delete(optional.get());
				serviceInstanceRepo.flush();
				return true;
			}

			return false;

		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}
}
