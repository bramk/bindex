package org.osgi.service.indexer.impl;

import java.io.File;
import java.io.IOException;

import org.osgi.framework.Constants;
import org.osgi.service.indexer.Resource;
import org.osgi.service.indexer.ResourceRecognizer;
import org.osgi.service.log.LogService;

/**
 * Recognizers that handles jars and bundles using a {@link JarResource} as well
 * as common IANA types using a {@link FileResource}.
 * 
 */
public class DefaultRecognizer implements ResourceRecognizer {

	@SuppressWarnings("unused")
	private final LogService log;

	public DefaultRecognizer(LogService log) {
		this.log = log;
	}

	public Resource recognizeResource(File file, Resource resource) {
		if (resource != null) {
			return resource;
		}
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

	//TODO extend to a comprehensive set
	private String detectMimeType(File file) {
		String fileName = file.getName();
		String mimeType = MimeType.Unknown.toString();
		if (fileName.endsWith(".xml")) {
			mimeType = MimeType.Xml.toString();
		} else if (fileName.endsWith(".json")) {
			mimeType = MimeType.Json.toString();
		}
		return mimeType;
	}
}
