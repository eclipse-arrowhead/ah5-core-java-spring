package eu.arrowhead.serviceregistry.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

	//Configuration

	@Value(ServiceRegistryConstants.$ALLOW_SELF_ADDRESSING_WD)
	private boolean allowSelfAddressing;

	@Value(ServiceRegistryConstants.$ALLOW_NON_ROUTABLE_ADDRESSING_WD)
	private boolean allowNonRoutableAddressing;

	@Value(ServiceRegistryConstants.$SERVICE_DISCOVERY_VERBOSE_WD)
	private boolean serviceDiscoveryVerbose;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public KeyValuesDTO getConfig(final List<String> keys, final String origin) {
		logger.debug("getConfig started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");
		
		//TODO: miert lesz minden true??
		final Map<String, String> configKeyValues = new HashMap<>();
		configKeyValues.put(ServiceRegistryConstants.ALLOW_SELF_ADDRESSING, String.valueOf(allowSelfAddressing));
		configKeyValues.put(ServiceRegistryConstants.ALLOW_NON_ROUTABLE_ADDRESSING, String.valueOf(allowNonRoutableAddressing));
		configKeyValues.put(ServiceRegistryConstants.SERVICE_DISCOVERY_VERBOSE, String.valueOf(serviceDiscoveryVerbose));

		try {
			final List<String> normalized = validator.validateAndNormalizeConfigKeyList(keys);

			// Empty input list means all is required
			if (normalized.size() == 0) {
				return dtoConverter.convertConfigMapToDTO(configKeyValues);
			}

			checkKeySetValid(keys, configKeyValues.keySet());

			final Map<String, String> result = keys.stream().collect(Collectors.toMap(key -> key, key -> configKeyValues.get(key)));
			return dtoConverter.convertConfigMapToDTO(result);

		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void checkKeySetValid(final List<String> keys, final Set<String> existing) {
		final String notExisting = keys.stream().filter(k -> !existing.contains(k)).collect(Collectors.joining(","));

		if (!notExisting.isEmpty()) {
			throw new InvalidParameterException("Invalid key(s): " + notExisting);
		}
	}
}
