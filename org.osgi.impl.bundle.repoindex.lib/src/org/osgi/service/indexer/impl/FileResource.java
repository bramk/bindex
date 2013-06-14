package org.osgi.service.indexer.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.jar.Manifest;

import org.osgi.service.indexer.Resource;

class FileResource implements Resource {

	private final File file;
	private final String location;

	private final Dictionary<String, Object> properties = new Hashtable<String, Object>();

	public FileResource(File file) {

		this.file = file;
		location = file.getPath();
		properties.put(NAME, file.getName());
		properties.put(LOCATION, location);
		properties.put(SIZE, file.length());
		properties.put(LAST_MODIFIED, file.lastModified());
		properties.put(MIMETYPE, MimeType.Unknown.toString());
	}

	public String getMimeType() {
		return (String) properties.get(Resource.MIMETYPE);
	}

	public String getLocation() {
		return location;
	}

	public Dictionary<String, Object> getProperties() {
		return properties;
	}

	public long getSize() {
		return file.length();
	}

	public InputStream getStream() throws IOException {
		return new FileInputStream(file);
	}

	public Manifest getManifest() throws IOException {
		return null;
	}

	public List<String> listChildren(String prefix) throws IOException {
		return null;
	}

	public Resource getChild(String path) throws IOException {
		return null;
	}

	public void close() {
	}
}
