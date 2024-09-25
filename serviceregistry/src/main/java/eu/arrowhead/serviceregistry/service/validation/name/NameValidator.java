package eu.arrowhead.serviceregistry.service.validation.name;

import eu.arrowhead.common.exception.InvalidParameterException;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class NameValidator {
	
	//=================================================================================================
	// members

	private static final Logger logger = LogManager.getLogger(NameValidator.class);
	
	private static final String NAME_REGEX_STRING = "([A-Za-z]{1})|(^[A-Za-z][0-9A-Za-z\\-]*[0-9A-Za-z]$)";
	private static final Pattern NAME_REGEX_PATTERN = Pattern.compile(NAME_REGEX_STRING);

	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public static void validateName(final String name) {
		logger.debug("Validate name started: {}", name);
		
		if (!NAME_REGEX_PATTERN.matcher(name).matches()) {
			throw new InvalidParameterException("The specified name does not match the naming convention: " + name);
		}
	}
	
}
