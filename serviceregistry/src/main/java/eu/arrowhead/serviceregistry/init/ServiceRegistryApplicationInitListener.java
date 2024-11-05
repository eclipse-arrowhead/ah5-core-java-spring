package eu.arrowhead.serviceregistry.init;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.init.ApplicationInitListener;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.model.SystemModel;
import eu.arrowhead.dto.ServiceInstanceInterfaceRequestDTO;
import eu.arrowhead.dto.ServiceInstanceRequestDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.dto.SystemRequestDTO;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;
import eu.arrowhead.serviceregistry.service.ServiceDiscoveryService;
import eu.arrowhead.serviceregistry.service.SystemDiscoveryService;

@Component
public class ServiceRegistryApplicationInitListener extends ApplicationInitListener {

	//=================================================================================================
	// members

	private static final String INIT_ORIGIN = "INIT";

	@Autowired
	private SystemDiscoveryService sysdService;

	@Autowired
	private ServiceDiscoveryService sdService;

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void customInit(final ContextRefreshedEvent event) throws InterruptedException {
		logger.debug("customInit started...");

		if (standaloneMode) {
			return;
		}

		// revoke system (if any)
		sysdService.revokeSystem(sysInfo.getSystemName(), INIT_ORIGIN);

		// register system
		final SystemModel model = sysInfo.getSystemModel();
		final SystemRequestDTO dto = new SystemRequestDTO(sysInfo.getSystemName(), model.metadata(), model.version(), model.addresses(), model.deviceName());
		sysdService.registerSystem(dto, INIT_ORIGIN);

		// register services
		if (sysInfo.getServices() != null) {
			for (final ServiceModel serviceModel : sysInfo.getServices()) {
				registerService(serviceModel);
			}
		}

		logger.info("System {} published {} service(s).", sysInfo.getSystemName(), registeredServices.size());
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void customDestroy() {
		logger.debug("customDestroy started...");

		if (standaloneMode) {
			return;
		}

		try {
			for (final String serviceInstanceId : registeredServices) {
				sdService.revokeService(sysInfo.getSystemName(), serviceInstanceId, INIT_ORIGIN);
			}

			registeredServices.clear();
			logger.info("Core system {} revoked {} service(s).", sysInfo, registeredServices.size());
		} catch (final Throwable t) {
			logger.error(t.getMessage());
			logger.debug(t);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void registerService(final ServiceModel model) {
		logger.debug("registerService started...");

		final List<ServiceInstanceInterfaceRequestDTO> interfaces = model.interfaces()
				.stream()
				.map(i -> new ServiceInstanceInterfaceRequestDTO(i.templateName(), i.protocol(), ServiceInterfacePolicy.NONE.name(), i.properties()))
				.collect(Collectors.toList());
		final ServiceInstanceRequestDTO dto = new ServiceInstanceRequestDTO(sysInfo.getSystemName(), model.serviceDefinition(), model.version(), null, model.metadata(), interfaces);
		final ServiceInstanceResponseDTO result = sdService.registerService(dto, INIT_ORIGIN);
		registeredServices.add(result.instanceId());
	}
}