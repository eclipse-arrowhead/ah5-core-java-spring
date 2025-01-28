package eu.arrowhead.serviceorchestration.service.utils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.OrchestrationResponseDTO;
import eu.arrowhead.dto.enums.OrchestrationFlag;
import eu.arrowhead.dto.enums.QoSEvaulationType;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationConstants;
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
		final Set<String> warnings = new HashSet<>();

		// Service Discovery
		final boolean translationAllowed = form.hasFlag(OrchestrationFlag.ALLOW_TRANSLATION) && sysInfo.isTranslationEnabled();
		List<OrchestrationCandidate> candidates = serviceDiscovery(form, translationAllowed, form.hasFlag(OrchestrationFlag.ONLY_PREFERRED));
		filterOutReservedOnes(candidates);

		if (Utilities.isEmpty(candidates)) {
			return doInterCloudOrReturn(form);
		}

		// Dealing with exclusivity
		if (form.exclusivityIsPreferred()) {
			markExclusivityIfFeasible(candidates);
		}

		if (form.hasFlag(OrchestrationFlag.ONLY_EXCLUSIVE)) {
			candidates = filterOutWhereExclusivityIsNotPossible(candidates);
			if (form.addFlag(OrchestrationFlag.MATCHMAKING)) {
				warnings.add(DynamicServiceOrchestrationConstants.ORCH_WARN_AUTO_MATCHMAKING);
			}
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
		if (translationAllowed) {
			markIfTranslationIsNeeded(candidates);
		}

		// QoS cross-check
		if (form.hasQoSRequirements()) {
			if (!sysInfo.isQoSEnabled()) {
				warnings.add(DynamicServiceOrchestrationConstants.ORCH_WARN_QOS_NOT_ENABLED);
				releaseTemporaryLockIfItWasLocked(candidates);
				return convertToOrchestrationResponse(List.of(), warnings);
			}
			candidates = doQoSCompliance(candidates);
		}

		if (Utilities.isEmpty(candidates)) {
			return doInterCloudOrReturn(form);
		}

		// Deal with translations
		if (translationAllowed) {
			if (checkIfHasNativeOnes(candidates)) {
				candidates = filterOutNonNativeOnes(candidates);
			} else {
				candidates = filterOutNotTranslatableOnes(candidates); // translation discovery
				if (form.addFlag(OrchestrationFlag.MATCHMAKING)) {
					warnings.add(DynamicServiceOrchestrationConstants.ORCH_WARN_AUTO_MATCHMAKING);
				}
			}
		}

		if (Utilities.isEmpty(candidates)) {
			return doInterCloudOrReturn(form);
		}

		// Dealing with preferences
		if (form.exclusivityIsPreferred() && !form.hasFlag(OrchestrationFlag.ONLY_EXCLUSIVE)) {
			if (containsReservable(candidates)) {
				candidates = filterOutWhereExclusivityIsNotPossible(candidates);
				if (form.addFlag(OrchestrationFlag.MATCHMAKING)) {
					warnings.add(DynamicServiceOrchestrationConstants.ORCH_WARN_AUTO_MATCHMAKING);
				}
			} else {
				warnings.add(DynamicServiceOrchestrationConstants.ORCH_WARN_NOT_EXCLUSIVE);
			}
		}

		if (form.hasPreferredProviders() && !form.hasFlag(OrchestrationFlag.ONLY_PREFERRED)) {
			if (containsPreferredProviders(candidates)) {
				candidates = filterOutNonPreferredProviders(candidates);
			}
		}

		// Matchmaking if required
		if (Utilities.isEmpty(candidates)) {
			return doInterCloudOrReturn(form);
		}

		if (form.hasFlag(OrchestrationFlag.MATCHMAKING)) {
			final OrchestrationCandidate match = matchmaking(candidates, getQoSEvaulationType(form));
			if (form.exclusivityIsPreferred()) {
				reserve(match, form.getExclusivityDuration());
				String matchInstanceId = match.getServiceInstance().instanceId();
				releaseTemporaryLockIfItWasLocked(candidates.stream().filter(c -> !c.getServiceInstance().instanceId().equals(matchInstanceId)).toList());
				if (form.getExclusivityDuration() < match.getExclusivityDuration()) {
					warnings.add(DynamicServiceOrchestrationConstants.ORCH_WARN_PART_TIME_EXCLUSIVITY);
				}
			}

			candidates.clear();
			candidates.add(match);
		}

		// Obtain Authorization tokens when required
		if (sysInfo.isAuthorizationEnabled()) {
			obtainAuthorizationTokensIfRequired(candidates);
		}

		// Create translation bridge if necessary
		if (form.hasFlag(OrchestrationFlag.MATCHMAKING) && candidates.get(0).isTranslationNeeded()) {
			buildTranslationBridge(candidates.get(0));
		}

		return convertToOrchestrationResponse(candidates, warnings);
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
			// set locked where it can be exclusive even if the duration is lower
			// do not lock where the reservation is denied
			return List.of();
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void markExclusivityIfFeasible(final List<OrchestrationCandidate> candidates) {
		// TODO set the canBeExclusive flag if possible to fulfilled
	}

	//-------------------------------------------------------------------------------------------------
	private List<OrchestrationCandidate> filterOutWhereExclusivityIsNotPossible(final List<OrchestrationCandidate> candidates) {
		// TODO
		return null;
	}

	//-------------------------------------------------------------------------------------------------
	private boolean containsReservable(final List<OrchestrationCandidate> candidates) {
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
		// let QoS evaluator know that translation is necessary or not
		return List.of();
	}

	//-------------------------------------------------------------------------------------------------
	private boolean checkIfHasNativeOnes(final List<OrchestrationCandidate> candidates) {
		// TODO
		return true;
	}

	//-------------------------------------------------------------------------------------------------
	private List<OrchestrationCandidate> filterOutNonNativeOnes(final List<OrchestrationCandidate> candidates) {
		// TODO
		return List.of();
	}

	//-------------------------------------------------------------------------------------------------
	private List<OrchestrationCandidate> filterOutNotTranslatableOnes(final List<OrchestrationCandidate> candidates) {
		// TODO
		// here comes the translation discovery
		return List.of();
	}

	//-------------------------------------------------------------------------------------------------
	private boolean containsPreferredProviders(final List<OrchestrationCandidate> candidates) {
		// TODO
		return false;
	}

	//-------------------------------------------------------------------------------------------------
	private List<OrchestrationCandidate> filterOutNonPreferredProviders(final List<OrchestrationCandidate> candidates) {
		// TODO
		return List.of();
	}

	//-------------------------------------------------------------------------------------------------
	private OrchestrationCandidate matchmaking(final List<OrchestrationCandidate> candidates, final QoSEvaulationType qosType) {
		if (qosType == QoSEvaulationType.RANKING) {
			return candidates.getFirst();
		}

		// TODO
		// canBeExclusive and full-time exclusivity has priority
		return null;
	}

	//-------------------------------------------------------------------------------------------------
	private void reserve(final OrchestrationCandidate candidate, final int duration) {
		synchronized (LOCK) {
			// TODO if reserve is not allowed just return
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void releaseTemporaryLockIfItWasLocked(final List<OrchestrationCandidate> candidates) {
		synchronized (LOCK) {
			// TODO
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
	private OrchestrationResponseDTO convertToOrchestrationResponse(final List<OrchestrationCandidate> candidates, final Set<String> warnings) {
		// TODO
		return null;
	}

}
