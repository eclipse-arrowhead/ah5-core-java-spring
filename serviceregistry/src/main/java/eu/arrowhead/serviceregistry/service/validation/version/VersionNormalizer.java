package eu.arrowhead.serviceregistry.service.validation.version;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Utilities;

@Component
public class VersionNormalizer {

	//=================================================================================================
	// members

	public static final String DEFAULT_MAJOR = "1";
	public static final String DEFAULT_MINOR = "0";
	public static final String DEFAULT_PATCH = "0";
	public static final String DOT = ".";
	public static final String DOT_REGEX = "\\.";
	public static final String DEFAULT_VERSION = DEFAULT_MAJOR + DOT + DEFAULT_MINOR + DOT + DEFAULT_PATCH;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public String normalize(final String version) {
		logger.debug("normalize version started...");

		if (Utilities.isEmpty(version)) {
			return DEFAULT_VERSION;
		}

		final String candidate = version.trim();

		final String[] chunks = candidate.split(DOT_REGEX);
		final int numberOfChunks = chunks.length;
		if (numberOfChunks == 1) {
			return chunks[0] + DOT + DEFAULT_MINOR + DOT + DEFAULT_PATCH;
		}

		if (numberOfChunks == 2) {
			return chunks[0] + DOT + chunks[1] + DOT + DEFAULT_PATCH;
		}

		return candidate;
	}
}