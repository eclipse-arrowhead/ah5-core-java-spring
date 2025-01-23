package eu.arrowhead.serviceorchestration.service.utils;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.OrchestrationResponseDTO;
import eu.arrowhead.dto.enums.OrchestrationFlag;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationSystemInfo;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationCandidate;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationForm;

@Service
public class LocalServiceOrchestration {

	//=================================================================================================
	// members

	@Autowired
	private DynamicServiceOrchestrationSystemInfo sysInfo;

	@Autowired
	private InterCloudServiceOrchestration interCloudOrch;

	private static final Object LOCK = new Object();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationResponseDTO doLocalServiceOrchestration(final OrchestrationForm form) {

		// Service Discovery
		final boolean discoveyrWithoutInterace = form.hasFlag(OrchestrationFlag.ALLOW_TRANSLATION) && sysInfo.isTranslationEnabled();
		List<OrchestrationCandidate> candidates = serviceDiscovery(form, discoveyrWithoutInterace, form.hasFlag(OrchestrationFlag.ONLY_PREFERRED));
		filterOutReservedOnes(candidates);

		if (Utilities.isEmpty(candidates)) {
			return doInterCloudOrReturn(form);
		}

		// Dealing with exclusivity
		if (form.needExclusivity()) {
			markExclusivityIfFeasible(candidates);
		}

		if (form.hasFlag(OrchestrationFlag.ONLY_EXCLUSIVE)) {
			candidates = filterOutNotReservableOnes(candidates);
			form.addFlag(OrchestrationFlag.MATCHMAKING);
		}

		// Authorization cross-check
		if (sysInfo.isAuthorizationEnabled()) {
			candidates = filterOutUnauthorizedOnes(candidates);
		}

		if (Utilities.isEmpty(candidates)) {
			return doInterCloudOrReturn(form);
		}

		boolean temporaryLock = false;
		if (form.hasFlag(OrchestrationFlag.ONLY_EXCLUSIVE)) {
			candidates = filterOutReservedOnesAndTemporaryLock(candidates);
			temporaryLock = true;
		} else {
			candidates = filterOutReservedOnes(candidates);
		}

		// Check if translation is necessary
		if (discoveyrWithoutInterace) {
			markIfTranslationIsNeeded(candidates);
		}

		// QoS cross-check
		if (sysInfo.isQoSEnabled()) {
			candidates = doQoSCompliance(candidates);
		}

		if (Utilities.isEmpty(candidates)) {
			return doInterCloudOrReturn(form);
		}

		// Deal with translations
		if (discoveyrWithoutInterace) {
			candidates = filterOutNotTranslatableOnes(candidates); // translation discovery
			if (checkIfOnlyWithTranslation(candidates)) {
				form.addFlag(OrchestrationFlag.MATCHMAKING);
			}
		}

		if (Utilities.isEmpty(candidates)) {
			return doInterCloudOrReturn(form);
		}

		// Check again reservations before matchmaking and authorization tokens
		if (!temporaryLock) {
			candidates = filterOutReservedOnes(candidates);
			if (Utilities.isEmpty(candidates)) {
				return doInterCloudOrReturn(form);
			}
		}

		// Matchmaking if required
		if (form.hasFlag(OrchestrationFlag.MATCHMAKING)) {

			// TODO matchmaking (handle only preferred too and ranking QoS)
		}

		// TODO Auth tokens

		// Check again reservations before bridge buildings
		if (!temporaryLock) {
			candidates = filterOutReservedOnes(candidates);
			if (Utilities.isEmpty(candidates)) {
				return doInterCloudOrReturn(form);
			}
		}

		// TODO bridge buildings if was matchmaking

		return convertToOrchestrationResponse(candidates);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private List<OrchestrationCandidate> serviceDiscovery(final OrchestrationForm form, final boolean withoutInterace, final boolean onlyPreferred) {
		// use provider filter if only preferred
		// do not use interface filters if withoutInterace is true
		return List.of();
	}

	//-------------------------------------------------------------------------------------------------
	private List<OrchestrationCandidate> filterOutReservedOnes(final List<OrchestrationCandidate> candidates) {
		synchronized (LOCK) {
			// filter out those what currently reserved for someone else
			return List.of();
		}
	}

	//-------------------------------------------------------------------------------------------------
	public List<OrchestrationCandidate> filterOutReservedOnesAndTemporaryLock(final List<OrchestrationCandidate> candidates) {
		synchronized (LOCK) {
			// filter out those what already locked or currently exclusive for someone other
			// set locked where it was succesfull
			return List.of();
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void markExclusivityIfFeasible(final List<OrchestrationCandidate> candidates) {
		// TODO set the canBeExclusive flag if possible to fulfilled
	}

	//-------------------------------------------------------------------------------------------------
	private List<OrchestrationCandidate> filterOutNotReservableOnes(final List<OrchestrationCandidate> candidates) {
		// TODO
		return null;
	}

	//-------------------------------------------------------------------------------------------------
	private List<OrchestrationCandidate> filterOutUnauthorizedOnes(final List<OrchestrationCandidate> candidates) {
		// TODO
		return List.of();
	}

	//-------------------------------------------------------------------------------------------------
	private void markIfTranslationIsNeeded(final List<OrchestrationCandidate> candidates) {
		// TODO
	}

	//-------------------------------------------------------------------------------------------------
	private List<OrchestrationCandidate> doQoSCompliance(final List<OrchestrationCandidate> candidates) {
		// TODO
		// let QoS elevator know that translation is necessary or not
		return List.of();
	}

	//-------------------------------------------------------------------------------------------------
	private List<OrchestrationCandidate> filterOutNotTranslatableOnes(final List<OrchestrationCandidate> candidates) {
		// TODO
		// here comes the translation discovery
		return List.of();
	}

	//-------------------------------------------------------------------------------------------------
	private boolean checkIfOnlyWithTranslation(final List<OrchestrationCandidate> candidates) {
		for (OrchestrationCandidate candidate : candidates) {
			if (!candidate.isTranslationNeeded()) {
				return false;
			}
		}
		return true;
	}

	//-------------------------------------------------------------------------------------------------
	private OrchestrationResponseDTO doInterCloudOrReturn(final OrchestrationForm form) {
		if (sysInfo.isInterCloudEnabled() && form.hasFlag(OrchestrationFlag.ALLOW_INTERCLOUD)) {
			return interCloudOrch.doInterCloudServiceOrchestration(form);
		} else {
			return new OrchestrationResponseDTO();
		}
	}

	//-------------------------------------------------------------------------------------------------
	private OrchestrationResponseDTO convertToOrchestrationResponse(final List<OrchestrationCandidate> candidates) {
		// TODO
		return null;
	}

}
