package eu.arrowhead.serviceorchestration.service.utils;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.OrchestrationResponseDTO;
import eu.arrowhead.dto.enums.OrchestrationFlag;
import eu.arrowhead.dto.enums.QoSEvaulationType;
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
		final boolean discoveyrWithoutInterface = form.hasFlag(OrchestrationFlag.ALLOW_TRANSLATION) && sysInfo.isTranslationEnabled();
		List<OrchestrationCandidate> candidates = serviceDiscovery(form, discoveyrWithoutInterface, form.hasFlag(OrchestrationFlag.ONLY_PREFERRED));
		filterOutReservedOnes(candidates);

		if (Utilities.isEmpty(candidates)) {
			return doInterCloudOrReturn(form);
		}

		// Dealing with exclusivity
		if (form.exclusivityIsPreferred()) {
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

		// Temporary lock if required and possible
		if (form.exclusivityIsPreferred()) {
			candidates = filterOutReservedOnesAndTemporaryLockIfCanBeExclusive(candidates);
		} else {
			candidates = filterOutReservedOnes(candidates);
		}

		// Check if translation is necessary
		if (discoveyrWithoutInterface) {
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
		if (discoveyrWithoutInterface) {
			candidates = filterOutNotTranslatableOnes(candidates); // translation discovery
			if (checkIfOnlyWithTranslation(candidates)) {
				form.addFlag(OrchestrationFlag.MATCHMAKING);
			}
		}

		if (Utilities.isEmpty(candidates)) {
			return doInterCloudOrReturn(form);
		}

		// Check again reservations before possible matchmaking, preferences and authorization tokens
		if (!form.hasFlag(OrchestrationFlag.ONLY_EXCLUSIVE)) { // otherwise all of them were already locked
			candidates = filterOutReservedOnes(candidates);
			if (Utilities.isEmpty(candidates)) {
				return doInterCloudOrReturn(form);
			}
		}

		// Dealing with preferences
		if (form.exclusivityIsPreferred() && !form.hasFlag(OrchestrationFlag.ONLY_EXCLUSIVE)) {
			if (conatinsReservable(candidates)) {
				candidates = filterOutNotReservableOnes(candidates);
				form.addFlag(OrchestrationFlag.MATCHMAKING);
			}
		}

		if (form.hasPreferredProviders() && !form.hasFlag(OrchestrationFlag.ONLY_PREFERRED)) {
			if (conatinsPreferredProviders(candidates)) {
				candidates = filterOutNonPreferredProviders(candidates);
			}
		}

		// Matchmaking if required
		if (form.hasFlag(OrchestrationFlag.MATCHMAKING)) {
			final List<OrchestrationCandidate> notMatchingList = new ArrayList<>();
			OrchestrationCandidate match = null;
			while (match == null && notMatchingList.size() <= candidates.size()) {
				match = matchmaking(candidates, getQoSEvaulationType(form), notMatchingList);
				if (form.exclusivityIsPreferred() && !reserveIfFree(match, form.getExclusivityDuration())) {
					notMatchingList.add(match);
					match = null;
				}
			}
		}

		// Obtain Authorization tokens when required
		if (sysInfo.isAuthorizationEnabled()) {
			obtainAuthorizationTokensIfRequired(candidates);
		}

		// Check again reservations before finish
		if (!form.hasFlag(OrchestrationFlag.ONLY_EXCLUSIVE)) { // otherwise all of them were already locked
			candidates = filterOutReservedOnes(candidates);
			if (Utilities.isEmpty(candidates)) {
				return doInterCloudOrReturn(form);
			}
		}

		// Create translation bridge if necessary
		if (form.hasFlag(OrchestrationFlag.MATCHMAKING) && candidates.get(0).isTranslationNeeded()) {
			buildTranslationBridge(candidates.get(0));
		}

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
			// If isLocked is true in candidate object, that means is locked by this session
			return List.of();
		}
	}

	//-------------------------------------------------------------------------------------------------
	public List<OrchestrationCandidate> filterOutReservedOnesAndTemporaryLockIfCanBeExclusive(final List<OrchestrationCandidate> candidates) {
		synchronized (LOCK) {
			// filter out those what already locked or currently exclusive for someone other
			// set locked where it can be exclusive
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
	private boolean conatinsReservable(final List<OrchestrationCandidate> candidates) {
		// TODO
		return false;
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
	private QoSEvaulationType getQoSEvaulationType(final OrchestrationForm form) {
		// TODO
		return null;
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
	private boolean conatinsPreferredProviders(final List<OrchestrationCandidate> candidates) {
		// TODO
		return false;
	}

	//-------------------------------------------------------------------------------------------------
	private List<OrchestrationCandidate> filterOutNonPreferredProviders(final List<OrchestrationCandidate> candidates) {
		// TODO
		return List.of();
	}

	//-------------------------------------------------------------------------------------------------
	private OrchestrationCandidate matchmaking(final List<OrchestrationCandidate> candidates, final QoSEvaulationType qosType, final List<OrchestrationCandidate> excludes) {
		if (qosType == QoSEvaulationType.RANKING) {
			for (final OrchestrationCandidate candidate : candidates) {
				for (OrchestrationCandidate exclude : excludes) {
					if (candidate.getServiceInstance().instanceId().equals(exclude.getServiceInstance().instanceId())) {
						return candidate;
					}
				}
			}
			return null;
		}

		// TODO
		return null;
	}

	//-------------------------------------------------------------------------------------------------
	private boolean reserveIfFree(final OrchestrationCandidate candidate, final int duration) {
		synchronized (LOCK) {
			// TODO
			return true;
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void obtainAuthorizationTokensIfRequired(final List<OrchestrationCandidate> candidates) {
		// TODO where ServiceInterfacePolicy is TOKEN_AUTH
	}

	//-------------------------------------------------------------------------------------------------
	private void buildTranslationBridge(final OrchestrationCandidate candidate) {
		// TODO set new connection details in the candidate object
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
