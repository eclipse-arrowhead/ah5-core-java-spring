package eu.arrowhead.serviceregistry.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.KeyValuesDTO;
import eu.arrowhead.serviceregistry.ServiceRegistryConstants;
import eu.arrowhead.serviceregistry.service.dto.DTOConverter;
import eu.arrowhead.serviceregistry.service.validation.ConfigValidation;

@Service
public class ConfigService {

	//=================================================================================================
	// members

	@Autowired
	private ConfigValidation validator;

	@Autowired
	private DTOConverter dtoConverter;

	@Autowired
	private Environment environment;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public KeyValuesDTO getConfig(final List<String> keys, final String origin) {
		logger.debug("getConfig started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		try {
			final List<String> normalized = validator.validateAndNormalizeConfigKeyList(keys);

			final Map<String, String> result = new HashMap<>();
			for (final String key : normalized) {
				if (!ServiceRegistryConstants.FORBIDDEN_KEYS.contains(key) && environment.getProperty(key) != null) {
					result.put(key, environment.getProperty(key));
				}
			}

			return dtoConverter.convertConfigMapToDTO(result);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}
	}
}