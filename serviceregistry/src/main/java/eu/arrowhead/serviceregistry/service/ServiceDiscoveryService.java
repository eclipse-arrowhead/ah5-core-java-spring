package eu.arrowhead.serviceregistry.service;

import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.ServiceInstanceRequestDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.serviceregistry.ServiceRegistrySystemInfo;
import eu.arrowhead.serviceregistry.api.http.utils.ServiceInstanceIdUtils;
import eu.arrowhead.serviceregistry.jpa.entity.Device;
import eu.arrowhead.serviceregistry.jpa.entity.DeviceAddress;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInstance;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInstanceInterface;
import eu.arrowhead.serviceregistry.jpa.entity.System;
import eu.arrowhead.serviceregistry.jpa.entity.SystemAddress;
import eu.arrowhead.serviceregistry.jpa.service.ServiceInstanceDbService;
import eu.arrowhead.serviceregistry.jpa.service.SystemDbService;
import eu.arrowhead.serviceregistry.service.dto.DTOConverter;
import eu.arrowhead.serviceregistry.service.validation.ServiceDiscoveryValidation;

@Service
public class ServiceDiscoveryService {

	//=================================================================================================
	// members

	@Autowired
	private ServiceRegistrySystemInfo sysInfo;

	@Autowired
	private ServiceDiscoveryValidation validator;

	@Autowired
	private ServiceInstanceDbService instanceDbService;

	@Autowired
	private SystemDbService systemDbService;

	@Autowired
	private DTOConverter dtoConverter;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public ServiceInstanceResponseDTO registerService(final ServiceInstanceRequestDTO dto, final String origin) {
		logger.debug("registerService started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final ServiceInstanceRequestDTO normalized = validator.validateAndNormalizeRegisterService(dto, origin);
		final String instanceId = ServiceInstanceIdUtils.calculateInstanceId(normalized.systemName(), normalized.serviceDefinitionName(), normalized.version());

		try {
			instanceDbService.deleteByInstanceId(instanceId);
			final Entry<ServiceInstance, List<ServiceInstanceInterface>> instanceEntry = instanceDbService.createBulk(List.of(normalized),
					sysInfo.getServiceDisciveryInterfacePolicy() == ServiceDiscoveryInterfacePolicy.RESTRICTED).getFirst();
			final Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>> systemTriplet = systemDbService.getByName(instanceEntry.getKey().getSystem().getName()).get();

			return dtoConverter.convertServiceInstanceEntityToDTO(instanceEntry, systemTriplet);

		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);

		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public boolean revokeService(final String identifiedSystemName, final String instanceId, final String origin) {
		logger.debug("revokeService started");
		Assert.isTrue(!Utilities.isEmpty(identifiedSystemName), "identifiedSystemName is empty");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validator.validateAndNormalizeRevokeService(instanceId, origin);

		try {
			if (!ServiceInstanceIdUtils.retriveSystemNameFromInstaceId(instanceId).equals(identifiedSystemName)) {
				throw new ForbiddenException("Revoking other systems' service is forbidden", origin);
			}

			return instanceDbService.deleteByInstanceId(instanceId);

		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}
}
