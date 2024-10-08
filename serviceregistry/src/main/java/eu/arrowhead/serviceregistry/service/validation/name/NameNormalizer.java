package eu.arrowhead.serviceregistry.service.validation.name;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;

@Component
public class NameNormalizer {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public String normalize(final String name) {
		logger.debug("normalize name started");
		Assert.isTrue(!Utilities.isEmpty(name), "name is empty");

		return name.trim().toLowerCase();
	}
}
