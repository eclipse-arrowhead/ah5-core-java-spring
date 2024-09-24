package eu.arrowhead.serviceregistry.api.http.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.http.HttpUtilities;
import eu.arrowhead.dto.SystemRegisterRequestDTO;
import eu.arrowhead.dto.SystemRequestDTO;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class SystemNamePreprocessor {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public SystemRequestDTO process(final SystemRegisterRequestDTO dto, final HttpServletRequest request, final String origin) throws InvalidParameterException {
		logger.debug("process (SystemRegisterRequestDTO to SystemRequestDTO) started");

		if (dto == null) {
			return null;
		}
		final String name = HttpUtilities.acquireName(request, origin);

		return new SystemRequestDTO(name, dto.metadata(), dto.version(), dto.addresses(), dto.deviceName());
	}

	//-------------------------------------------------------------------------------------------------
	public String process(final HttpServletRequest request, final String origin) throws InvalidParameterException {
		logger.debug("process started");

		final String name = HttpUtilities.acquireName(request, origin);

		return name;
	}

}
