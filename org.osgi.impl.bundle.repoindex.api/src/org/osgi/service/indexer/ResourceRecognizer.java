package org.osgi.service.indexer;

import java.io.File;

public interface ResourceRecognizer {

	/**
	 * <p>
	 * This method is invoked for each file the indexer is requested to analyze.
	 * Implementations return a Resource that <b>MUST</b> set the
	 * {@link Resource#MIMETYPE} property.
	 * </p>
	 * 
	 * <p>
	 * Recognizers <b>may</b> examine the previously recognized resource and
	 * choose to modify, wrap, override or simply return it.
	 * </p>
	 * 
	 * @param file
	 *            The File to be recognized
	 * @param resource
	 *            The Resource that was recognized by the previous recognizer.
	 * @return The Resource to be propagated
	 */
	Resource recognizeResource(File file, Resource resource);
}
