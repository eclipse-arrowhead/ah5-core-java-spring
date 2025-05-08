package eu.arrowhead.authorization.jpa.service;

import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import eu.arrowhead.authorization.jpa.entity.CryptographerAuxiliary;
import eu.arrowhead.authorization.jpa.entity.EncryptionKey;
import eu.arrowhead.authorization.jpa.repository.CryptographerAuxiliaryRepository;
import eu.arrowhead.authorization.jpa.repository.EncryptionKeyRepository;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InternalServerError;

@Service
public class EncryptionKeyDbService {

	//=================================================================================================
	// members	

	@Autowired
	private EncryptionKeyRepository keyRepo;

	@Autowired
	private CryptographerAuxiliaryRepository auxiliaryRepo;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Optional<EncryptionKey> get(final String systemName) {
		Assert.isTrue(!Utilities.isEmpty(systemName), "systemName is empty");

		try {
			return keyRepo.findBySystemName(systemName);

		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public Pair<EncryptionKey, Boolean> save(final String systemName, final String key, final String algorithm, final String internalAuxiliary, final String externalAuxiliary) {
		Assert.isTrue(!Utilities.isEmpty(systemName), "systemName is empty");
		Assert.isTrue(!Utilities.isEmpty(key), "key is empty");
		Assert.isTrue(!Utilities.isEmpty(algorithm), "algorithm is empty");
		Assert.isTrue(!Utilities.isEmpty(internalAuxiliary), "internalAuxiliary is empty");

		try {
			boolean override = false;
			final CryptographerAuxiliary internalAuxiliaryRecord = auxiliaryRepo.saveAndFlush(new CryptographerAuxiliary(internalAuxiliary));
			final CryptographerAuxiliary externalAuxiliaryRecord = Utilities.isEmpty(externalAuxiliary) ? null : auxiliaryRepo.saveAndFlush(new CryptographerAuxiliary(externalAuxiliary));

			EncryptionKey toSave = null;
			final Optional<EncryptionKey> optional = keyRepo.findBySystemName(systemName);
			if (optional.isEmpty()) {
				toSave = new EncryptionKey(systemName, key, algorithm, internalAuxiliaryRecord, externalAuxiliaryRecord);
			} else {
				toSave = optional.get();
				toSave.setKeyValue(key);
				toSave.setAlgorithm(algorithm);
				toSave.setInternalAuxiliary(internalAuxiliaryRecord);
				toSave.setExternalAuxiliary(externalAuxiliaryRecord);
				override = true;

			}
			return Pair.of(keyRepo.saveAndFlush(toSave), !override);

		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public boolean delete(final String systemName) {
		Assert.isTrue(!Utilities.isEmpty(systemName), "systemName is empty");

		try {
			final Optional<EncryptionKey> optional = keyRepo.findBySystemName(systemName);
			if (optional.isEmpty()) {
				return false;
			}

			keyRepo.deleteById(optional.get().getId());
			return true;

		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}
}
