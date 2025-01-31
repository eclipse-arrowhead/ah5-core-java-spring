package eu.arrowhead.serviceregistry.api.http.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import eu.arrowhead.serviceregistry.ServiceRegistryConstants;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class ServiceLookupPreprocessor {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public boolean isRestricted(final HttpServletRequest request) {
		logger.debug("isRestricted started");

		if (request == null) {
			return true;
		}

		final Object boolObject = request.getAttribute(ServiceRegistryConstants.REQUEST_ATTR_RESTRICTED_SERVICE_LOOKUP);
		return boolObject != null && Boolean.valueOf(boolObject.toString());
	}
}