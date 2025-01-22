package eu.arrowhead.serviceorchestration.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.OrchestrationResponseDTO;
import eu.arrowhead.dto.enums.OrchestrationFlag;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationSystemInfo;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationCandidate;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationFrom;
import eu.arrowhead.serviceorchestration.service.utils.OrchestrationServiceUtils;

@Service
public class OrchestrationService {

	//=================================================================================================
	// members

	@Autowired
	private DynamicServiceOrchestrationSystemInfo sysInfo;

	@Autowired
	private OrchestrationServiceUtils orchServiceUtils;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationResponseDTO pull(final OrchestrationFrom form) {
		//TODO
		// from syntax validation
		// form syntax normalization
		// from context validation (ONLY_Intercloud, but it is not enabled | no reservation in intercloud | ONLY_EXCLUSIVE and ONLY_INTERCLOUD cannot be together | ONLY_INTERCLOUD can't be with ALLOW_TRANSLATION)

		final OrchestrationResponseDTO emptyResult = new OrchestrationResponseDTO();

		List<OrchestrationCandidate> candidates = new ArrayList<>();
		boolean resultsAreInterCloud = false;
		boolean temporaryLock = false;

		// Service discovery
		if (form.hasFlag(OrchestrationFlag.ONLY_INTERCLOUD)) {
			candidates.addAll(orchServiceUtils.interCloudServiceDiscovery(form));
			resultsAreInterCloud = true;
			form.addFlag(OrchestrationFlag.MATCHMAKING);
		} else {
			candidates.addAll(orchServiceUtils.localCloudServiceDiscovery(form, form.hasFlag(OrchestrationFlag.ALLOW_TRANSLATION) && sysInfo.isTranslationEnabled()));
			if (Utilities.isEmpty(candidates)
					&& sysInfo.isInterCloudEnabled()
					&& form.hasFlag(OrchestrationFlag.ALLOW_INTERCLOUD)
					&& form.getExclusivityDuration() == null) { // reservation is not supported in inter-cloud
				candidates.addAll(orchServiceUtils.interCloudServiceDiscovery(form));
				resultsAreInterCloud = true;
				form.addFlag(OrchestrationFlag.MATCHMAKING);
			}
		}

		if (Utilities.isEmpty(candidates)) {
			return emptyResult;
		}

		// Dealing with reservations
		if (!resultsAreInterCloud) {
			if (form.getExclusivityDuration() == null) {
				candidates = orchServiceUtils.filterOutReservedOnes(candidates);
				if (Utilities.isEmpty(candidates) && !form.hasFlag(OrchestrationFlag.ALLOW_INTERCLOUD)) {
					return emptyResult;
				}
			} else {
				candidates = orchServiceUtils.filterOutReservedOnesAndTemporaryLock(candidates);
				if (Utilities.isEmpty(candidates)) {
					return emptyResult;
				}
				temporaryLock = true;
				orchServiceUtils.markExclusivityIfFeasible(candidates);
			}

		}

		// Drop out those what cannot be reserved if required
		if (!resultsAreInterCloud) {
			if (form.hasFlag(OrchestrationFlag.ONLY_EXCLUSIVE)) {
				candidates = orchServiceUtils.filterOutNotReservableOnes(candidates);
				if (Utilities.isEmpty(candidates)) {
					return emptyResult;
				}
				form.addFlag(OrchestrationFlag.MATCHMAKING);
			}
		}

		// Authorization cross-check
		if (!resultsAreInterCloud) {
			if (sysInfo.isAuthorizationEnabled()) {
				candidates = orchServiceUtils.filterOutUnauthorizedOnes(candidates);
				if (Utilities.isEmpty(candidates) && (form.hasFlag(OrchestrationFlag.ONLY_EXCLUSIVE) || !form.hasFlag(OrchestrationFlag.ALLOW_INTERCLOUD))) {
					return emptyResult;
				}
			}
		}

		// Check reservations again if candidates were not locked
		if (!resultsAreInterCloud) {
			if (!temporaryLock) {
				candidates = orchServiceUtils.filterOutReservedOnes(candidates);
				if (Utilities.isEmpty(candidates) && (form.hasFlag(OrchestrationFlag.ONLY_EXCLUSIVE) || !form.hasFlag(OrchestrationFlag.ALLOW_INTERCLOUD))) {
					return emptyResult;
				}
			}
		}

		// Deal with translations
		if (!resultsAreInterCloud) {
			orchServiceUtils.markIfTranslationIsNeeded(candidates);
			candidates = orchServiceUtils.filterOutNotTranslatableOnes(candidates);
			if (Utilities.isEmpty(candidates) && (form.hasFlag(OrchestrationFlag.ONLY_EXCLUSIVE) || !form.hasFlag(OrchestrationFlag.ALLOW_INTERCLOUD))) {
				return emptyResult;
			}
			boolean withTranslationOnly = true;
			for (OrchestrationCandidate candidate : candidates) {
				if (!candidate.isTranslationNeeded()) {
					withTranslationOnly = false;
					break;
				}
			}
			if (withTranslationOnly) {
				form.addFlag(OrchestrationFlag.MATCHMAKING);
			}
		}

		// Check reservations again if candidates were not locked
		if (!resultsAreInterCloud) {
			if (!temporaryLock) {
				candidates = orchServiceUtils.filterOutReservedOnes(candidates);
				if (Utilities.isEmpty(candidates) && (form.hasFlag(OrchestrationFlag.ONLY_EXCLUSIVE) || !form.hasFlag(OrchestrationFlag.ALLOW_INTERCLOUD))) {
					return emptyResult;
				}
			}
		}

		// If results are not originally inter-cloud, but no local candidate left, then we try inter-cloud if allowed and exclusivity is not a must
		if (!resultsAreInterCloud && Utilities.isEmpty(candidates) && !form.hasFlag(OrchestrationFlag.ONLY_EXCLUSIVE) && form.hasFlag(OrchestrationFlag.ALLOW_INTERCLOUD)) {
			candidates.addAll(orchServiceUtils.interCloudServiceDiscovery(form));
			if (Utilities.isEmpty(candidates)) {
				return emptyResult;
			}
			resultsAreInterCloud = true;
			form.addFlag(OrchestrationFlag.MATCHMAKING);
		}

		// Matchmaking
		if (form.hasFlag(OrchestrationFlag.MATCHMAKING)) {
			OrchestrationCandidate match = null;
			while (match == null || !Utilities.isEmpty(candidates)) {
				match = orchServiceUtils.matchmaking(candidates);
				if (form.getExclusivityDuration() != null && !orchServiceUtils.reserveIfFree(match, form.getExclusivityDuration())) {
					final String matchInstanceId = match.getServiceInstance().instanceId();
					candidates = candidates.stream().filter(c -> !c.getServiceInstance().instanceId().equals(matchInstanceId)).toList();
					match = null;
				}
			}

			candidates.clear();
			if (match != null) {
				candidates.add(match);
			}

			if (Utilities.isEmpty(candidates)) {
				return emptyResult;
			}
		}

		// Create translation bridge if necessary
		if (form.hasFlag(OrchestrationFlag.MATCHMAKING) && candidates.get(0).isTranslationNeeded()) {
			orchServiceUtils.buildTranslationBridge(candidates.get(0));

		// Create inter-cloud bridge if necessary
		} else if (form.hasFlag(OrchestrationFlag.MATCHMAKING) && !candidates.get(0).isLocal()) {
			orchServiceUtils.buildInterCloudBridge(candidates.get(0));
		}

		// Obtain Authorization tokens when required
		if (!resultsAreInterCloud && sysInfo.isAuthorizationEnabled()) {
			orchServiceUtils.obtainAuthorizationTokensIfRequired(candidates);
		}

		return orchServiceUtils.convertToOrchestrationResponse(candidates);
	}
}
