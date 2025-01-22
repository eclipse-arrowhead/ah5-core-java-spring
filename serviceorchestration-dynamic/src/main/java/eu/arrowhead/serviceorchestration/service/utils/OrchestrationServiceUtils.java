package eu.arrowhead.serviceorchestration.service.utils;

import java.util.List;

import org.springframework.stereotype.Service;

import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationCandidate;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationFrom;

@Service
public class OrchestrationServiceUtils {

	//=================================================================================================
	// members

	private static final Object LOCK = new Object();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public List<OrchestrationCandidate> localCloudServiceDiscovery(final OrchestrationFrom form) {
		// use provider filter if only preferred providers flag
		return List.of();
	}

	//-------------------------------------------------------------------------------------------------
	public List<OrchestrationCandidate> interCloudServiceDiscovery(final OrchestrationFrom form) {
		throw new ArrowheadException("Inter-cloud orchestration is not implemented");
	}

	//-------------------------------------------------------------------------------------------------
	public List<OrchestrationCandidate> filterOutReservedOnes(final List<OrchestrationCandidate> candidates) {
		synchronized (LOCK) {
			// filter out those what currently exclusive for someone other
			return List.of();
		}
	}

	//-------------------------------------------------------------------------------------------------
	public List<OrchestrationCandidate> filterOutReservedOnesAndTemporaryLock(final List<OrchestrationCandidate> candidates) {
		synchronized (LOCK) {
			// TODO in synchronized
			// filter out those what already locked or currently exclusive for someone other
			// set locked where it was succesfull
			return List.of();
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void markExclusivityIfFeasible(final List<OrchestrationCandidate> candidates) {
		// TODO set the canBeExclusive flag if possible to fulfilled
	}

	//-------------------------------------------------------------------------------------------------
	public boolean reserveIfFree(final OrchestrationCandidate candidate, final int duration) {
		synchronized (LOCK) {
			// TODO
			return true;
		}
	}

	//-------------------------------------------------------------------------------------------------
	public List<OrchestrationCandidate> filterOutNotReservableOnes(final List<OrchestrationCandidate> candidates) {
		// TODO
		return List.of();
	}

	//-------------------------------------------------------------------------------------------------
	public List<OrchestrationCandidate> filterOutUnauthorizedOnes(final List<OrchestrationCandidate> candidates) {
		// TODO
		return List.of();
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationCandidate matchmaking(final List<OrchestrationCandidate> candidates) {
		// TODO
		return null;
	}

	//-------------------------------------------------------------------------------------------------
	public void obtainAuthorizationTokens(final List<OrchestrationCandidate> candidates) {
		// TODO where ServiceInterfacePolicy is TOKEN_AUTH
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------

}
