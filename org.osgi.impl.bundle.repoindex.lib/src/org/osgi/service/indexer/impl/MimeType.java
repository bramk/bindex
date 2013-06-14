package org.osgi.service.indexer.impl;

public enum MimeType {
	Bundle("application/vnd.osgi.bundle"),
	Fragment("application/vnd.osgi.bundle"),
	Jar("application/java-archive"),
	Json("application/json"),
	Xml("application/xml"),
	MetaType("application/xml:osgi-autoconf"),
	Unknown("application/octet-stream");
	
	private String mimeType;
	
	MimeType(String mimeType) {
		this.mimeType = mimeType;
	}
	
	@Override
	public String toString() {
		return mimeType;
	}
}
