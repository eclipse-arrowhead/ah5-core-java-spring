package eu.arrowhead.serviceorchestration.jpa.service;

import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationLock;
import eu.arrowhead.serviceorchestration.jpa.repository.OrchestrationLockRepository;

@Service
public class OrchestrationLockDbService {

	//=================================================================================================
	// membres

	@Autowired
	private OrchestrationLockRepository lockRepo;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public List<OrchestrationLock> create(final List<OrchestrationLock> candidates) {
		logger.debug("create started...");
		Assert.isTrue(!Utilities.isEmpty(candidates), "Orchestration lock list is empty.");

		try {
			return lockRepo.saveAllAndFlush(candidates);

		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public List<OrchestrationLock> getByServiceInstanceId(final List<String> ids) {
		logger.debug("getByServiceInstanceId started...");
		Assert.isTrue(!Utilities.isEmpty(ids), "Serice instance id list is empty.");

		try {
			return lockRepo.findAllByServiceInstanceIdIn(ids);

		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public void delete(final Collection<Long> ids) {
		logger.debug("delete started...");
		Assert.isTrue(!Utilities.isEmpty(ids), "Orchestration lock is list is empty.");

		try {
			lockRepo.deleteAllById(ids);
			lockRepo.flush();

		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}
}
