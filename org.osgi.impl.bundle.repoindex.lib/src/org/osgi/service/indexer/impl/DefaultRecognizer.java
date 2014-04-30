package org.osgi.service.indexer.impl;

import java.io.File;
import java.io.IOException;

import org.osgi.framework.Constants;
import org.osgi.service.indexer.Resource;
import org.osgi.service.indexer.ResourceRecognizer;
import org.osgi.service.log.LogService;

/**
 * Default Recognizer that is intended to be <b>first</b> in line of any number
 * of recognizers to be consulted. It recognizes OSGi bundles and jars,
 * returning a {@link JarResource}. For any other file type and it tries to
 * determine the type by extension, and returns a {@link FileResource}. The
 * default mimetype is {@code MimeType#Unknown}.
 */
public class DefaultRecognizer implements ResourceRecognizer {

	@SuppressWarnings("unused")
	private final LogService log;

	public DefaultRecognizer(LogService log) {
		this.log = log;
	}

	public Resource recognizeResource(File file, Resource resource) {
		if (file.getName().endsWith(".jar")) {
			try {
				resource = new JarResource(file);
				String bsn = resource.getManifest().getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
				if (bsn != null && !bsn.equals("")) {
					resource.getProperties().put(Resource.MIMETYPE, MimeType.Bundle.toString());
				} else {
					resource.getProperties().put(Resource.MIMETYPE, MimeType.Jar.toString());
				}
			} catch (IOException e) {
				// not a jar
			}
		}
		if (resource == null) {
			resource = new FileResource(file);
			resource.getProperties().put(Resource.MIMETYPE, detectMimeType(file));
		}
		return resource;
	}

	private String detectMimeType(File file) {
		String fileName = file.getName();
		String mimeType = MimeType.Unknown.toString();

		// TODO extend to a comprehensive set
		if (fileName.endsWith(".xml")) {
			mimeType = MimeType.Xml.toString();
		} else if (fileName.endsWith(".json")) {
			mimeType = MimeType.Json.toString();
		}
		return mimeType;
	}
}
