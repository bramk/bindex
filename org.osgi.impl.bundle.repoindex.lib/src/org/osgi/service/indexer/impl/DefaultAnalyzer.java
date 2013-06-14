package org.osgi.service.indexer.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Version;
import org.osgi.service.indexer.Builder;
import org.osgi.service.indexer.Capability;
import org.osgi.service.indexer.Namespaces;
import org.osgi.service.indexer.Requirement;
import org.osgi.service.indexer.Resource;
import org.osgi.service.indexer.ResourceAnalyzer;
import org.osgi.service.indexer.impl.util.Hex;
import org.osgi.service.log.LogService;

/**
 * Analyzer implementation that generates capabilities for
 * <code>Namespaces.NS_IDENTITY</code> and <code>Namespaces.NS_CONTENT</code> if
 * <b>not</b> set by a previous Analyzer. <br/>
 * The identity is extracted from the resource name based on heuristic
 * &lt;identity&gt;[-&lt;version&gt;][.&lt;suffix&gt;]. If no version is found
 * it is omitted. Type always set to file.
 * 
 */
class DefaultAnalyzer implements ResourceAnalyzer {

	/*
	 * FIXME: This implementation duplicates some code from BundleAnalyzer in
	 * order to keep the initial patch small. Should be addressed by moving it
	 * to Util at a later stage.
	 */

	private static final Pattern SUFFIX_PATTERN = Pattern.compile(".*(\\..+)$");

	private static final String SHA_256 = "SHA-256";

	private final ThreadLocal<GeneratorState> state = new ThreadLocal<GeneratorState>();
	@SuppressWarnings("unused")
	private final LogService log;

	public DefaultAnalyzer(LogService log) {
		this.log = log;
	}

	public void analyzeResource(Resource resource, List<Capability> capabilities, List<Requirement> requirements) throws Exception {
		if (!containsNameSpace(capabilities, Namespaces.NS_IDENTITY)) {
			doPlainFileIdentity(resource, capabilities);
		}
		if (!containsNameSpace(capabilities, Namespaces.NS_CONTENT)) {
			doPlainFileContent(resource, capabilities);
		}
	}

	void setStateLocal(GeneratorState state) {
		this.state.set(state);
	}

	private GeneratorState getStateLocal() {
		return state.get();
	}

	private boolean containsNameSpace(List<Capability> capabilities, String ns) {
		for (Capability capability : capabilities) {
			if (capability.getNamespace().equals(ns)) {
				return true;
			}
		}
		return false;
	}

	private void doPlainFileIdentity(Resource resource, List<? super Capability> caps) {
		String name = (String) resource.getProperties().get(Resource.NAME);
		String suffix = null;
		Matcher suffixMatcher = SUFFIX_PATTERN.matcher(name);
		if (suffixMatcher.find()) {
			suffix = suffixMatcher.group(1);
		}
		if (suffix != null) {
			name = name.substring(0, name.length() - suffix.length());
		}

		Version version = null;
		int dashIndex = name.lastIndexOf('-');
		if (dashIndex > 0) {
			try {
				String versionStr = name.substring(dashIndex + 1);
				version = new Version(versionStr);
				name = name.substring(0, dashIndex);
			} catch (Exception e) {
				// not a version
			}
		}

		Builder builder = new Builder().setNamespace(Namespaces.NS_IDENTITY).addAttribute(Namespaces.NS_IDENTITY, name).addAttribute(Namespaces.ATTR_IDENTITY_TYPE, "file");
		if (version != null)
			builder.addAttribute(Namespaces.ATTR_VERSION, version);
		caps.add(builder.buildCapability());
	}

	private void doPlainFileContent(Resource resource, List<? super Capability> capabilities) throws Exception {

		String mimeType = (String) resource.getProperties().get(Resource.MIMETYPE);
		if (mimeType == null) {
			mimeType = MimeType.Unknown.toString();
		}

		Builder builder = new Builder().setNamespace(Namespaces.NS_CONTENT);

		String sha = calculateSHA(resource);
		builder.addAttribute(Namespaces.NS_CONTENT, sha);

		String location = calculateLocation(resource);
		builder.addAttribute(Namespaces.ATTR_CONTENT_URL, location);

		long size = resource.getSize();
		if (size > 0L)
			builder.addAttribute(Namespaces.ATTR_CONTENT_SIZE, size);

		builder.addAttribute(Namespaces.ATTR_CONTENT_MIME, (String) resource.getProperties().get(Resource.MIMETYPE));

		capabilities.add(builder.buildCapability());
	}

	private String calculateSHA(Resource resource) throws IOException, NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance(SHA_256);
		byte[] buf = new byte[1024];

		InputStream stream = null;
		try {
			stream = resource.getStream();
			while (true) {
				int bytesRead = stream.read(buf, 0, 1024);
				if (bytesRead < 0)
					break;

				digest.update(buf, 0, bytesRead);
			}
		} finally {
			if (stream != null)
				stream.close();
		}

		return Hex.toHexString(digest.digest());
	}

	private String calculateLocation(Resource resource) throws IOException {
		String location = resource.getLocation();

		File path = new File(location);
		String fileName = path.getName();
		String dir = path.getAbsoluteFile().getParentFile().toURI().toURL().toString();

		String result = location;

		GeneratorState state = getStateLocal();
		if (state != null) {
			String rootUrl = state.getRootUrl().toString();
			if (!rootUrl.endsWith("/"))
				rootUrl += "/";

			if (rootUrl != null) {
				if (dir.startsWith(rootUrl))
					dir = dir.substring(rootUrl.length());
				else
					throw new IllegalArgumentException("Cannot index files above the root URL.");
			}

			String urlTemplate = state.getUrlTemplate();
			if (urlTemplate != null) {
				result = urlTemplate.replaceAll("%s", Util.getSymbolicName(resource).getName());
				result = result.replaceAll("%f", fileName);
				result = result.replaceAll("%p", dir);
				result = result.replaceAll("%v", "" + Util.getVersion(resource));
			} else {
				result = dir + fileName;
			}
		}

		return result;
	}
}
