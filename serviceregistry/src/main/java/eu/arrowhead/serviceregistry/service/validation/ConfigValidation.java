package eu.arrowhead.serviceregistry.service.validation;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.serviceregistry.service.normalization.ConfigNormalization;

@Service
public class ConfigValidation {

	//=================================================================================================
	// members

	@Autowired
	private ConfigNormalization normalizer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public void validateConfigKeyList(final List<String> keys) {
		logger.debug("validateConfigKeyList started");

		if (Utilities.isEmpty(keys)) {
			throw new InvalidParameterException("The list of the requested configuration keys is null or empty");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> validateAndNormalizeConfigKeyList(final List<String> keys) {
		logger.debug("validateAndNormalizeConfigKeyList started");

		validateConfigKeyList(keys);
		return normalizer.normalizeConfigKeyList(keys);
	}
}