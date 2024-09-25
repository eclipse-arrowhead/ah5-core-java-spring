package eu.arrowhead.serviceregistry.jpa.service;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInterfaceTemplate;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInterfaceTemplateProperty;
import eu.arrowhead.serviceregistry.jpa.repository.ServiceInterfaceTemplatePropertyRepository;
import eu.arrowhead.serviceregistry.jpa.repository.ServiceInterfaceTemplateRepository;

@Service
public class ServiceInterfaceTemplateDbService {

	//=================================================================================================
	// members

	@Autowired
	private ServiceInterfaceTemplateRepository templateRepo;

	@Autowired
	private ServiceInterfaceTemplatePropertyRepository templatePropsRepo;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Optional<ServiceInterfaceTemplate> getByName(final String name) {
		logger.debug("getByName started");
		Assert.isTrue(!Utilities.isEmpty(name), "name is empty");

		return templateRepo.findByName(name);
	}

	//-------------------------------------------------------------------------------------------------
	public List<ServiceInterfaceTemplateProperty> getPropertiesByName(final String name) {
		logger.debug("getByName started");
		Assert.isTrue(!Utilities.isEmpty(name), "name is empty");

		final Optional<ServiceInterfaceTemplate> templateOpt = templateRepo.findByName(name);

		if (templateOpt.isPresent()) {
			return templatePropsRepo.findByServiceInterfaceTemplate(templateOpt.get());
		}

		return List.of();
	}
}
