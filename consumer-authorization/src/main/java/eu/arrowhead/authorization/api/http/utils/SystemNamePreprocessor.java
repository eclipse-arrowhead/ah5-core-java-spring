package eu.arrowhead.authorization.api.http.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.http.HttpUtilities;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class SystemNamePreprocessor {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public String process(final HttpServletRequest request, final String origin) throws InvalidParameterException {
		logger.debug("process started");

		return HttpUtilities.acquireName(request, origin);
	}
}