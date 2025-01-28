package eu.arrowhead.authentication.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authentication.jpa.entity.System;
import eu.arrowhead.authentication.jpa.service.IdentityDbService;
import eu.arrowhead.authentication.method.AuthenticationMethods;
import eu.arrowhead.authentication.method.IAuthenticationMethod;
import eu.arrowhead.authentication.service.dto.DTOConverter;
import eu.arrowhead.authentication.service.dto.NormalizedIdentityListMgmtRequestDTO;
import eu.arrowhead.authentication.service.dto.NormalizedIdentityMgmtRequestDTO;
import eu.arrowhead.authentication.service.validation.ManagementValidation;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.IdentityListMgmtCreateRequestDTO;
import eu.arrowhead.dto.IdentityListMgmtResponseDTO;
import eu.arrowhead.dto.IdentityListMgmtUpdateRequestDTO;

@Service
public class ManagementService {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private ManagementValidation validator;

	@Autowired
	private IdentityDbService dbService;

	@Autowired
	private DTOConverter converter;
	
	@Autowired
	private AuthenticationMethods methods;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public IdentityListMgmtResponseDTO createIdentitiesOperation(final String requesterName, final IdentityListMgmtCreateRequestDTO dto, final String origin) {
		logger.debug("createIdentitiesOperation started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final String normalizedRequester = validator.validateAndNormalizeRequester(requesterName, origin);
		final NormalizedIdentityListMgmtRequestDTO normalizedDto = validator.validateAndNormalizeCreateIdentityList(dto, origin);

		try {
			final List<System> systems = dbService.createIdentifiableSystemsInBulk(normalizedRequester, normalizedDto);

			return converter.convertIdentifiableSystemListToDTO(systems);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		} catch (final ExternalServerError ex) {
			throw new ExternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public IdentityListMgmtResponseDTO updateIdentitiesOperation(final String requesterName, final IdentityListMgmtUpdateRequestDTO dto, final String origin) {
		logger.debug("updateIdentitiesOperation started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		// authentication method independent steps
		final String normalizedRequester = validator.validateAndNormalizeRequester(requesterName, origin);
		List<NormalizedIdentityMgmtRequestDTO> normalizedIdentities = validator.validateAndNormalizeUpdateIdentityListPhase1(dto, origin);
		
		// calculate the authentication method
		final IAuthenticationMethod authenticationMethod;
		try {
			// finding related systems
			final List<System> systems = dbService.getSystemsByNamesBeforeUpdate(normalizedIdentities.stream().map(id -> id.systemName()).toList());
			
			// all authentication methods have to be the same in related systems
			if (systems.stream().map(s -> s.getAuthenticationMethod()).collect(Collectors.toSet()).size() > 1) {
				throw new InvalidParameterException("Bulk updating systems with different authentication method is not supported");
			}
			
			authenticationMethod = getAuthenticationMethod(systems.getFirst());
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
		
		// authentication method dependent steps
		normalizedIdentities = validator.validateAndNormalizeUpdateIdentityListPhase2(authenticationMethod, normalizedIdentities, origin);

		try {
			final List<System> systems = dbService.updateIdentifiableSystemsInBulk(normalizedRequester, normalizedIdentities);

			return converter.convertIdentifiableSystemListToDTO(systems);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		} catch (final ExternalServerError ex) {
			throw new ExternalServerError(ex.getMessage(), origin);
		}
	}
	
	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private IAuthenticationMethod getAuthenticationMethod(final System aSystem) {
		logger.debug("getAuthenticationMethod started...");
		
		final IAuthenticationMethod method = methods.method(aSystem.getAuthenticationMethod());
		if (method == null) {
			throw new InvalidParameterException("Authentication method is unsupported: " + aSystem.getAuthenticationMethod());
		}
		
		return method;
	}
}