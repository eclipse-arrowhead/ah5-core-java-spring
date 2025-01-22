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
		// from context validation & correction (ONLY_Intercloud, but it is not enabled | no reservation in intercloud | if ONLY_EXCLUSIVE, then set MATCHMAKING also)

		final OrchestrationResponseDTO emptyResult = new OrchestrationResponseDTO();

		List<OrchestrationCandidate> candidates = new ArrayList<>();
		boolean temporaryLock = false;

		// Service discovery
		if (form.hasOrchestrationFlag(OrchestrationFlag.ONLY_INTERCLOUD)) {
			candidates.addAll(orchServiceUtils.interCloudServiceDiscovery(form));
		} else {
			candidates.addAll(orchServiceUtils.localCloudServiceDiscovery(form, false));
			if (Utilities.isEmpty(candidates)
					&& sysInfo.isInterCloudEnabled()
					&& form.hasOrchestrationFlag(OrchestrationFlag.ALLOW_INTERCLOUD)
					&& form.getExclusivityDuration() == null) { // reservation is not supported in inter-cloud
				candidates.addAll(orchServiceUtils.interCloudServiceDiscovery(form));
			}
		}

		if (Utilities.isEmpty(candidates) && form.hasOrchestrationFlag(OrchestrationFlag.ALLOW_TRANSLATION)) {
			candidates.addAll(orchServiceUtils.localCloudServiceDiscovery(form, true));
			if (Utilities.isEmpty(candidates)) {
				return emptyResult;
			}
			candidates.forEach(c -> c.setTranslationNeeded(true));
		}

		// Dealing with reservations
		if (form.getExclusivityDuration() == null) {
			candidates = orchServiceUtils.filterOutReservedOnes(candidates);
		} else {
			candidates = orchServiceUtils.filterOutReservedOnesAndTemporaryLock(candidates);
			temporaryLock = true;
			orchServiceUtils.markExclusivityIfFeasible(candidates);
		}

		if (Utilities.isEmpty(candidates)) {
			return emptyResult;
		}

		// Drop out those what cannot be reserved if required
		if (form.hasOrchestrationFlag(OrchestrationFlag.ONLY_EXCLUSIVE)) {
			candidates = orchServiceUtils.filterOutNotReservableOnes(candidates);
			if (Utilities.isEmpty(candidates)) {
				return emptyResult;
			}
		}

		// Authorization cross-check
		if (sysInfo.isAuthorizationEnabled()) {
			candidates = orchServiceUtils.filterOutUnauthorizedOnes(candidates);
			if (Utilities.isEmpty(candidates)) {
				return emptyResult;
			}
		}

		// Check reservations again if candidates were not locked
		if (!temporaryLock) {
			candidates = orchServiceUtils.filterOutReservedOnes(candidates);
			if (Utilities.isEmpty(candidates)) {
				return emptyResult;
			}
		}

		// Matchmaking
		if (form.hasOrchestrationFlag(OrchestrationFlag.MATCHMAKING)) {
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

		// Obtain Authorization tokens when required
		if (sysInfo.isAuthorizationEnabled()) {
			orchServiceUtils.obtainAuthorizationTokensIfRequired(candidates);
		}

		// Check reservations again if candidates were not locked
		if (!temporaryLock) {
			candidates = orchServiceUtils.filterOutReservedOnes(candidates);
			if (Utilities.isEmpty(candidates)) {
				return emptyResult;
			}
		}

		return null; // result
	}
}
