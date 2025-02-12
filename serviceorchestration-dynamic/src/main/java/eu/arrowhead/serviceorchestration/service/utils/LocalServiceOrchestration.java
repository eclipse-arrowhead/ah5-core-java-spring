package eu.arrowhead.serviceorchestration.service.utils;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.common.service.validation.MetadataRequirementsMatcher;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.OrchestrationResponseDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceResponseDTO;
import eu.arrowhead.dto.ServiceInstanceListResponseDTO;
import eu.arrowhead.dto.ServiceInstanceLookupRequestDTO;
import eu.arrowhead.dto.enums.OrchestrationFlag;
import eu.arrowhead.dto.enums.QoSEvaulationType;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationConstants;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationSystemInfo;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationLock;
import eu.arrowhead.serviceorchestration.jpa.service.OrchestrationJobDbService;
import eu.arrowhead.serviceorchestration.jpa.service.OrchestrationLockDbService;
import eu.arrowhead.serviceorchestration.service.enums.OrchestrationJobStatus;
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

	@Autowired
	private OrchestrationJobDbService orchJobDbService;

	@Autowired
	private OrchestrationLockDbService orchLockDbService;

	@Autowired
	private ArrowheadHttpService ahHttpService;

	private static final Object LOCK = new Object();

	private static final int TEMPORARY_LOCK_DURATION = 60; // sec

	private static final int RESERVATION_BUFER = 10; // sec

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationResponseDTO doLocalServiceOrchestration(final UUID jobId, final OrchestrationForm form) {
		logger.debug("doLocalServiceOrchestration started...");

		orchJobDbService.setStatus(jobId, OrchestrationJobStatus.IN_PROGRESS, null);

		try {
			final Set<String> warnings = new HashSet<>();

			// Service Discovery
			final boolean translationAllowed = form.hasFlag(OrchestrationFlag.ALLOW_TRANSLATION) && sysInfo.isTranslationEnabled();
			List<OrchestrationCandidate> candidates = serviceDiscovery(form, translationAllowed, form.hasFlag(OrchestrationFlag.ONLY_PREFERRED));
			candidates = filterOutLockedOnes(jobId, candidates);

			if (Utilities.isEmpty(candidates)) {
				return doInterCloudOrReturn(jobId, form);
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

			// Dealing with address types
			candidates = filterOutUnsuitableAddressTypes(form, candidates);

			// Authorization cross-check
			if (sysInfo.isAuthorizationEnabled()) {
				candidates = filterOutUnauthorizedOnes(candidates);
			}

			if (Utilities.isEmpty(candidates)) {
				return doInterCloudOrReturn(jobId, form);
			}

			// Temporary lock if required and possible
			if (form.exclusivityIsPreferred()) {
				candidates = filterOutLockedOnesAndTemporaryLockIfCanBeExclusive(jobId, form.getRequesterSystemName(), candidates);
			} else {
				candidates = filterOutLockedOnes(jobId, candidates);
			}

			// Check if translation is necessary
			if (translationAllowed) {
				markIfTranslationIsNeeded(form, candidates);
			}

			// QoS cross-check
			if (form.hasQoSRequirements()) {
				if (!sysInfo.isQoSEnabled()) {
					warnings.add(DynamicServiceOrchestrationConstants.ORCH_WARN_QOS_NOT_ENABLED);
					releaseTemporaryLockIfItWasLocked(jobId, candidates);
					orchJobDbService.setStatus(jobId, OrchestrationJobStatus.DONE, candidates.size() + " local result.");
					return convertToOrchestrationResponse(List.of(), warnings);
				}
				candidates = doQoSCompliance(candidates);
			}

			if (Utilities.isEmpty(candidates)) {
				return doInterCloudOrReturn(jobId, form);
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
				return doInterCloudOrReturn(jobId, form);
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
				if (containsPreferredProviders(form, candidates)) {
					candidates = filterOutNonPreferredProviders(form, candidates);
				}
			}

			// Matchmaking if required
			if (Utilities.isEmpty(candidates)) {
				return doInterCloudOrReturn(jobId, form);
			}

			if (form.hasFlag(OrchestrationFlag.MATCHMAKING)) {
				final OrchestrationCandidate match = matchmaking(candidates, getQoSEvaulationType(form));
				if (form.exclusivityIsPreferred()) {
					reserve(jobId, form.getRequesterSystemName(), match, form.getExclusivityDuration());
					final String matchInstanceId = match.getServiceInstance().instanceId();
					releaseTemporaryLockIfItWasLocked(jobId, candidates.stream().filter(c -> !c.getServiceInstance().instanceId().equals(matchInstanceId)).toList());
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

			final OrchestrationResponseDTO result = convertToOrchestrationResponse(candidates, warnings);
			orchJobDbService.setStatus(jobId, OrchestrationJobStatus.DONE, candidates.size() + " local result.");
			return result;

		} catch (final Exception ex) {
			orchJobDbService.setStatus(jobId, OrchestrationJobStatus.ERROR, ex.getMessage());
			throw ex;
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private List<OrchestrationCandidate> serviceDiscovery(final OrchestrationForm form, final boolean withoutInterace, final boolean onlyPreferred) {
		logger.debug("serviceDiscovery started...");

		final ServiceInstanceLookupRequestDTO lookupDTO = new ServiceInstanceLookupRequestDTO.Builder()
				.serviceDefinitionName(form.getServiceDefinition())
				.versions(form.getVersions())
				.alivesAt(form.getAlivesAt())
				.metadataRequirementsList(form.getMetadataRequirements())
				.policies(form.getSecurityPolicies())
				.interfaceTemplateNames(withoutInterace ? null : form.getInterfaceTemplateNames())
				.interfacePropertyRequirementsList(withoutInterace ? null : form.getInterfacePropertyRequirements())
				.providerNames(onlyPreferred ? form.getPrefferedProviders() : null)
				.build();

		final ServiceInstanceListResponseDTO response = ahHttpService.consumeService(Constants.SERVICE_DEF_SERVICE_DISCOVERY, Constants.SERVICE_OP_LOOKUP, ServiceInstanceListResponseDTO.class, lookupDTO);

		if (Utilities.isEmpty(response.entries())) {
			return List.of();
		}

		return response.entries().stream().map(instance -> new OrchestrationCandidate(instance, true)).toList();
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:.EmptyBlockCheck")
	private List<OrchestrationCandidate> filterOutLockedOnes(final UUID jobId, final List<OrchestrationCandidate> candidates) {
		logger.debug("filterOutLockedOnes started...");

		synchronized (LOCK) {
			final String jobIdStr = jobId.toString();
			final List<OrchestrationLock> lockRecords = orchLockDbService.getByServiceInstanceId(candidates.stream().map(c -> c.getServiceInstance().instanceId()).toList());

			final ZonedDateTime now = Utilities.utcNow();
			final Set<Long> expiredLocks = new HashSet<>();
			final Set<String> lockedServiceInstanceIds = new HashSet<>();

			for (final OrchestrationLock lockRecord : lockRecords) {
				if (lockRecord.getOrchestrationJobId().equals(jobIdStr)) {
					// lock belongs to this session
				} else if (lockRecord.getExpiresAt().isBefore(now)) {
					expiredLocks.add(lockRecord.getId());
				} else {
					lockedServiceInstanceIds.add(lockRecord.getServiceInstanceId());
				}
			}

			orchLockDbService.delete(expiredLocks);
			return candidates.stream().filter(c -> !lockedServiceInstanceIds.contains(c.getServiceInstance().instanceId())).toList();
		}
	}

	//-------------------------------------------------------------------------------------------------
	public List<OrchestrationCandidate> filterOutLockedOnesAndTemporaryLockIfCanBeExclusive(final UUID jobId, final String consumerSystem, final List<OrchestrationCandidate> candidates) {
		logger.debug("filterOutLockedOnesAndTemporaryLockIfCanBeExclusive started...");

		synchronized (LOCK) {
			final List<OrchestrationCandidate> freeCandidates = filterOutLockedOnes(jobId, candidates);
			final String jonIdStr = jobId.toString();
			final ZonedDateTime tempLockExpiresAt = Utilities.utcNow().plusSeconds(TEMPORARY_LOCK_DURATION);

			final List<OrchestrationLock> toLock = new ArrayList<>(freeCandidates.size());
			for (final OrchestrationCandidate candidate : freeCandidates) {
				if (candidate.canBeExclusive()) {
					candidate.setLocked(true);
					toLock.add(new OrchestrationLock(jonIdStr, candidate.getServiceInstance().instanceId(), consumerSystem, tempLockExpiresAt, true));
				}
			}
			orchLockDbService.create(toLock);
			return freeCandidates;
		}
	}

	//-------------------------------------------------------------------------------------------------
	private List<OrchestrationCandidate> filterOutUnsuitableAddressTypes(final OrchestrationForm form, final List<OrchestrationCandidate> candidates) {
		logger.debug("filterOutUnsuitableAddressTypes started...");

		final List<String> suitableAddressTypes = form.getInterfaceAddressTypes();
		if (Utilities.isEmpty(suitableAddressTypes)) {
			return candidates;
		}

		final List<OrchestrationCandidate> suitableCandidates = new ArrayList<>();
		for (final OrchestrationCandidate candidate : candidates) {

			// TODO how to do this? Address value appears only in the interface properties if any and with whatever keys. It can be even a simple string or string list...
		}

		return suitableCandidates;
	}

	//-------------------------------------------------------------------------------------------------
	private void markExclusivityIfFeasible(final List<OrchestrationCandidate> candidates) {
		logger.debug("markExclusivityIfFeasible");

		for (final OrchestrationCandidate candidate : candidates) {
			final Object allowedExclusivityDurationObj = candidate.getServiceInstance().metadata().get(Constants.METADATA_KEY_ALLOW_EXCLUSIVITY);
			if (allowedExclusivityDurationObj != null) {
				try {
					final int allowedExclusivityDuration = Integer.parseInt((String) allowedExclusivityDurationObj);
					candidate.setCanBeExclusive(allowedExclusivityDuration > 0);
					candidate.setExclusivityDuration(allowedExclusivityDuration);

				} catch (final Exception ex) {
					// Should not happen
					logger.error(Constants.METADATA_KEY_ALLOW_EXCLUSIVITY + " metadata is not an integer. Service instance id: " + candidate.getServiceInstance().instanceId());
				}
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	private List<OrchestrationCandidate> filterOutWhereExclusivityIsNotPossible(final List<OrchestrationCandidate> candidates) {
		logger.debug("filterOutWhereExclusivityIsNotPossible started...");
		return candidates.stream().filter(c -> c.canBeExclusive()).toList();
	}

	//-------------------------------------------------------------------------------------------------
	private boolean containsReservable(final List<OrchestrationCandidate> candidates) {
		logger.debug("containsReservable started...");

		for (final OrchestrationCandidate candidate : candidates) {
			if (candidate.canBeExclusive()) {
				return true;
			}
		}
		return false;
	}

	//-------------------------------------------------------------------------------------------------
	private List<OrchestrationCandidate> filterOutUnauthorizedOnes(final List<OrchestrationCandidate> candidates) {
		logger.debug("filterOutUnauthorizedOnes started...");

		// TODO call auth service once it is implemented
		logger.warn("Authorization crosscheck is not implemented yet");
		return candidates;
	}

	//-------------------------------------------------------------------------------------------------
	private void markIfTranslationIsNeeded(final OrchestrationForm form, final List<OrchestrationCandidate> candidates) {
		logger.debug("markIfTranslationIsNeeded started...");

		if (Utilities.isEmpty(form.getInterfaceTemplateNames()) && Utilities.isEmpty(form.getInterfacePropertyRequirements())) {
			return;
		}

		for (final OrchestrationCandidate candidate : candidates) {
			for (final ServiceInstanceInterfaceResponseDTO offeredInterface : candidate.getServiceInstance().interfaces()) {

				// Checking interface template names
				if (!Utilities.isEmpty(form.getInterfaceTemplateNames())
						&& !form.getInterfaceTemplateNames().contains(offeredInterface.templateName())) {
					candidate.setTranslationNeeded(true);
					continue;
				}

				// Checking interface properties
				if (!Utilities.isEmpty(form.getInterfacePropertyRequirements())) {
					boolean match = false;
					for (final MetadataRequirementDTO interfacePropertyRequirement : form.getInterfacePropertyRequirements()) {
						if (MetadataRequirementsMatcher.isMetadataMatch(offeredInterface.properties(), interfacePropertyRequirement)) {
							match = true;
							break;
						}
					}
					candidate.setTranslationNeeded(!match);
				}
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	private QoSEvaulationType getQoSEvaulationType(final OrchestrationForm form) {
		// TODO
		return null;
	}

	//-------------------------------------------------------------------------------------------------
	private List<OrchestrationCandidate> doQoSCompliance(final List<OrchestrationCandidate> candidates) {
		// TODO implement when QoS Evaluator is ready
		// TODO let QoS evaluator know that translation is necessary or not

		logger.warn("QoS crosschek is not implemented yet");
		return candidates;
	}

	//-------------------------------------------------------------------------------------------------
	private boolean checkIfHasNativeOnes(final List<OrchestrationCandidate> candidates) {
		logger.debug("checkIfHasNativeOnes started...");

		for (final OrchestrationCandidate candidate : candidates) {
			if (!candidate.isTranslationNeeded()) {
				return true;
			}
		}
		return false;
	}

	//-------------------------------------------------------------------------------------------------
	private List<OrchestrationCandidate> filterOutNonNativeOnes(final List<OrchestrationCandidate> candidates) {
		logger.debug("filterOutNonNativeOnes started...");

		return candidates.stream().filter(c -> !c.isTranslationNeeded()).toList();
	}

	//-------------------------------------------------------------------------------------------------
	private List<OrchestrationCandidate> filterOutNotTranslatableOnes(final List<OrchestrationCandidate> candidates) {
		logger.debug("filterOutNotTranslatableOnes started...");

		// TODO here comes the translation discovery
		logger.warn("Translation support is not implemented yet");
		return List.of();
	}

	//-------------------------------------------------------------------------------------------------
	private boolean containsPreferredProviders(final OrchestrationForm form, final List<OrchestrationCandidate> candidates) {
		logger.debug("containsPreferredProviders started...");

		for (final OrchestrationCandidate candidate : candidates) {
			if (form.getPrefferedProviders().contains(candidate.getServiceInstance().provider().name())) {
				return true;
			}
		}
		return false;
	}

	//-------------------------------------------------------------------------------------------------
	private List<OrchestrationCandidate> filterOutNonPreferredProviders(final OrchestrationForm form, final List<OrchestrationCandidate> candidates) {
		logger.debug("filterOutNonPreferredProviders started...");
		
		return candidates.stream().filter(c -> form.getPrefferedProviders().contains(c.getServiceInstance().provider().name())).toList();
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
	private void reserve(final UUID jobId, final String consumerSystem, final OrchestrationCandidate candidate, final int duration) {
		logger.debug("reserve started...");

		if (!candidate.canBeExclusive()) {
			return;
		}

		synchronized (LOCK) {
			Assert.isTrue(candidate.isLocked(), "Unlocked candidate is intended to being reserved.");

			final String jobIdStr = jobId.toString();
			final ZonedDateTime expiresAt = Utilities.utcNow().plusSeconds(RESERVATION_BUFER + Math.min(duration, candidate.getExclusivityDuration()));

			final Optional<OrchestrationLock> optional = orchLockDbService.changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(jobIdStr, candidate.getServiceInstance().instanceId(), expiresAt);

			// the temporary lock may expire
			if (optional.isEmpty()) {
				orchLockDbService.create(List.of(new OrchestrationLock(jobIdStr, candidate.getServiceInstance().instanceId(), consumerSystem, expiresAt, false)));
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void releaseTemporaryLockIfItWasLocked(final UUID jobId, final List<OrchestrationCandidate> candidates) {
		logger.debug("releaseTemporaryLockIfItWasLocked start...");

		synchronized (LOCK) {
			final String jobIdStr = jobId.toString();

			final List<OrchestrationLock> lockRecords = orchLockDbService.getByServiceInstanceId(candidates.stream()
					.filter(c -> c.isLocked())
					.map(c -> c.getServiceInstance().instanceId())
					.toList());

			orchLockDbService.delete(lockRecords.stream()
					.filter(lr -> lr.getOrchestrationJobId().equals(jobIdStr))
					.map(lr -> lr.getId())
					.toList());
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
	private OrchestrationResponseDTO doInterCloudOrReturn(final UUID jobId, final OrchestrationForm form) {
		if (sysInfo.isInterCloudEnabled() && form.hasFlag(OrchestrationFlag.ALLOW_INTERCLOUD)) {
			return interCloudOrch.doInterCloudServiceOrchestration(jobId, form);
		} else {
			orchJobDbService.setStatus(jobId, OrchestrationJobStatus.DONE, "No results were found.");
			return new OrchestrationResponseDTO();
		}
	}

	//-------------------------------------------------------------------------------------------------
	private OrchestrationResponseDTO convertToOrchestrationResponse(final List<OrchestrationCandidate> candidates, final Set<String> warnings) {
		// TODO
		return null;
	}

}
