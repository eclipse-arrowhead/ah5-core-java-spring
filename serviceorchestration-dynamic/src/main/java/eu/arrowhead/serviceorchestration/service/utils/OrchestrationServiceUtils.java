package eu.arrowhead.serviceorchestration.service.utils;

import java.util.List;

import org.springframework.stereotype.Service;

import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.dto.OrchestrationResponseDTO;
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
	public List<OrchestrationCandidate> localCloudServiceDiscovery(final OrchestrationFrom form, final boolean withoutInterace) {
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
	public void markIfTranslationIsNeeded(final List<OrchestrationCandidate> candidates) {
		// TODO
	}

	//-------------------------------------------------------------------------------------------------
	public List<OrchestrationCandidate> filterOutNotTranslatableOnes(final List<OrchestrationCandidate> candidates) {
		// TODO
		// here comes the translation discovery
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
		// those that are not to be translated have priority
		return null;
	}

	//-------------------------------------------------------------------------------------------------
	public void buildTranslationBridge(final OrchestrationCandidate candidate) {
		// TODO set new connection details in the candidate object
	}

	//-------------------------------------------------------------------------------------------------
	public void buildInterCloudBridge(final OrchestrationCandidate candidate) {
		// TODO set new connection details in the candidate object
	}

	//-------------------------------------------------------------------------------------------------
	public void obtainAuthorizationTokensIfRequired(final List<OrchestrationCandidate> candidates) {
		// TODO where ServiceInterfacePolicy is TOKEN_AUTH
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationResponseDTO convertToOrchestrationResponse(final List<OrchestrationCandidate> candidates) {
		// TODO
		return null;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------

}
