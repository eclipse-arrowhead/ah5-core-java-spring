package eu.arrowhead.serviceorchestration.service.normalization;

import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.validation.name.NameNormalizer;

@Service
public class OrchestrationServiceNormalization {

	//=================================================================================================
	// members

	@Autowired
	private NameNormalizer nameNormalizer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Pair<String, UUID> normalizePushUnsubscribe(final String requesterSystem, final String subscriptionId) {
		logger.debug("normalizePushUnsubscribe started...");
		Assert.isTrue(!Utilities.isEmpty(requesterSystem), "requesterSystem is empty");
		Assert.isTrue(!Utilities.isEmpty(subscriptionId), "subscriptionId is empty");

		return Pair.of(nameNormalizer.normalize(requesterSystem), UUID.fromString(subscriptionId.trim()));
	}
}
