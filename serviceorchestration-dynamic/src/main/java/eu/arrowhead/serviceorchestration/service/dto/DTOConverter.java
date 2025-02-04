package eu.arrowhead.serviceorchestration.service.dto;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.OrchestrationNotifyInterfaceDTO;
import eu.arrowhead.dto.OrchestrationRequestDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionListResponseDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionResponseDTO;
import eu.arrowhead.serviceorchestration.jpa.entity.Subscription;

@Service
public class DTOConverter {

	//=================================================================================================
	// members

	@Autowired
	private ObjectMapper mapper;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationSubscriptionListResponseDTO convertSubscriptionListToDTO(final List<Subscription> subscriptions) {
		logger.debug("convertSubscriptionListToDTO started...");
		Assert.notNull(subscriptions, "subscriptions list is null");

		final List<OrchestrationSubscriptionResponseDTO> entries = subscriptions.stream().map(subscription -> convertSubscriptionToDTO(subscription)).toList();
		return new OrchestrationSubscriptionListResponseDTO(entries, (long) entries.size());
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationSubscriptionResponseDTO convertSubscriptionToDTO(final Subscription subscription) {
		logger.debug("convertSubscriptionListToDTO started...");
		Assert.notNull(subscription, "subscription list is null");

		return new OrchestrationSubscriptionResponseDTO(
				subscription.getId().toString(),
				subscription.getOwnerSystem(),
				subscription.getTargetSystem(),
				createOrchestrationRequestDTO(subscription.getOrchestrationRequest()),
				createOrchestrationNotifyInterfaceDTO(subscription.getNotifyProtocol(), subscription.getNotifyProperties()),
				Utilities.convertZonedDateTimeToUTCString(subscription.getExpiresAt()),
				Utilities.convertZonedDateTimeToUTCString(subscription.getCreatedAt()));
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationRequestDTO createOrchestrationRequestDTO(final String orchestrationRequestStr) {
		logger.debug("createOrchestrationRequestDTO started...");

		try {
			return mapper.readValue(orchestrationRequestStr, OrchestrationRequestDTO.class);
		} catch (final JsonProcessingException ex) {
			logger.debug(ex);
			throw new IllegalArgumentException("DTOconverter.createOrchestrationRequestDTO failed. Error: " + ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationNotifyInterfaceDTO createOrchestrationNotifyInterfaceDTO(final String protocol, final String propertiesStr) {
		logger.debug("createOrchestrationNotifyInterfaceDTO started...");

		final TypeReference<Map<String, String>> typeReference = new TypeReference<Map<String, String>>() {
		};
		try {
			return new OrchestrationNotifyInterfaceDTO(protocol, mapper.readValue(propertiesStr, typeReference));
		} catch (final JsonProcessingException ex) {
			logger.debug(ex);
			throw new IllegalArgumentException("DTOconverter.createOrchestrationNotifyInterfaceDTO failed. Error: " + ex.getMessage());
		}
	}
}
