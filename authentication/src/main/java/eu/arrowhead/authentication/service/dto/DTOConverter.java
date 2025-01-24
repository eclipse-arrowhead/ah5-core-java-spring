package eu.arrowhead.authentication.service.dto;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authentication.jpa.entity.System;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.IdentityListMgmtResponseDTO;
import eu.arrowhead.dto.IdentityMgmtResponseDTO;

@Service
public class DTOConverter {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public IdentityListMgmtResponseDTO convertIdentifiableSystemListToDTO(final List<System> systems) {
		logger.debug("convertIdentifiableSystemListToDTO started...");
		Assert.notNull(systems, "systems is null");

		return new IdentityListMgmtResponseDTO(systems.stream().map(s -> convertIdentifiableSystemToDTO(s)).toList(), systems.size());
	}

	//-------------------------------------------------------------------------------------------------
	public IdentityMgmtResponseDTO convertIdentifiableSystemToDTO(final System system) {
		logger.debug("convertIdentifiableSystemToDTO started...");
		Assert.notNull(system, "system is null");

		return new IdentityMgmtResponseDTO(
				system.getName(),
				system.getAuthenticationMethod().name(),
				system.isSysop(),
				system.getCreatedBy(),
				Utilities.convertZonedDateTimeToUTCString(system.getCreatedAt()),
				system.getUpdatedBy(),
				Utilities.convertZonedDateTimeToUTCString(system.getUpdatedAt()));
	}
}
