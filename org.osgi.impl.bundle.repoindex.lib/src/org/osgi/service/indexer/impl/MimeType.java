package org.osgi.service.indexer.impl;

public enum MimeType {
	Bundle("application/vnd.osgi.bundle"), Fragment("application/vnd.osgi.bundle"), Jar("application/java-archive"), Unknown("application/octet-stream"), Json("application/json"), Xml(
			"application/xml"), MetaType("application/xml:osgi-autoconf");

	private String mimeType;

	MimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	@Override
	public String toString() {
		return mimeType;
	}
}
