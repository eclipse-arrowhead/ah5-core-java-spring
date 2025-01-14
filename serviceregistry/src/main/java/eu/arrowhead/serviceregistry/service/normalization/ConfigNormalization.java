package eu.arrowhead.serviceregistry.service.normalization;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class ConfigNormalization {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public List<String> normalizeConfigKeyList(final List<String> keys) {
		logger.debug("normalizeConfigKeyList started");
		Assert.notNull(keys, "key list is null");


		return keys
			.stream()
			.distinct()
			.map(k -> k.trim())
			.collect(Collectors.toList());
	}
}