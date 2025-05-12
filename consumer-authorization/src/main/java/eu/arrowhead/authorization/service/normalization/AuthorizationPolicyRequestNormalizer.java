package eu.arrowhead.authorization.service.normalization;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.authorization.service.dto.NormalizedAuthorizationPolicyRequest;
import eu.arrowhead.common.service.validation.name.NameNormalizer;
import eu.arrowhead.dto.AuthorizationPolicyRequestDTO;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.enums.AuthorizationPolicyType;

@Service
public class AuthorizationPolicyRequestNormalizer {

	//=================================================================================================
	// members

	@Autowired
	private NameNormalizer nameNormalizer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public NormalizedAuthorizationPolicyRequest normalize(final AuthorizationPolicyRequestDTO dto) {
		logger.debug("AuthorizationPolicyRequestNormalizer.normalize started...");

		final AuthorizationPolicyType policyType = AuthorizationPolicyType.valueOf(dto.policyType().trim().toUpperCase());
		List<String> list = null;
		MetadataRequirementDTO metadataRequirement = null;

		switch (policyType) {
		case ALL:
			break;
		case BLACKLIST:
		case WHITELIST:
			list = normalizeSystemList(dto.policyList());
			break;
		case SYS_METADATA:
			metadataRequirement = dto.policyMetadataRequirement();
			break;
		default:
			throw new InvalidParameterException("Unknown policy type: " + policyType.name());
		}

		return new NormalizedAuthorizationPolicyRequest(
				policyType,
				list,
				metadataRequirement);

	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private List<String> normalizeSystemList(final List<String> systemList) {
		logger.debug("normalizeSystemList started...");
		final Set<String> set = systemList
				.stream()
				.map(sys -> nameNormalizer.normalize(sys))
				.collect(Collectors.toSet()); // to remove any duplicates

		return new ArrayList<>(set);
	}
}
