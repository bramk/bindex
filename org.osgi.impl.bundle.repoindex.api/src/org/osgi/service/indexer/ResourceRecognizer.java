package org.osgi.service.indexer;

import java.io.File;

import aQute.bnd.annotation.ConsumerType;

/**
 * <p>
 * A Resource Recognizer is responsible for identifying the type of a resource
 * file and returning a {@link Resource} instance capable of handling it.
 * </p>
 * 
 * <p>
 * Clients may implement this interface and register instances as services.
 * </p>
 * 
 */
@ConsumerType
public interface ResourceRecognizer {

	/**
	 * <p>
	 * This method is invoked for each file the indexer is requested to analyze.
	 * Implementations return a {@link Resource} that <b>MUST</b> set the
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
	 *            The Resource that was recognized by a previous recognizer.
	 * @return The Resource to be propagated, may be {@code null} if recognition
	 *         failed.
	 */
	Resource recognizeResource(File file, Resource resource);
}
