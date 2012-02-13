Introduction
============

BIndex2 program is a small Java program that generates repository indexes compliant with the Repository Service Specification version 1.0, as defined in the OSGi Service Platform Service Compendium, Release 5. It can recurse over a directory structure generates a repository.xml file. The URLs can be rewritten using a template.

BIndex2 is a command line application that can easily be integrated in scripts. It is also an OSGi bundle that publishes a service under the `ResourceIndexer` interface, and a standalone library that can be used in conventional Java runtimes. While mainly intended for indexing OSGi bundles, it can generate metadata for any arbitrary file type by extending it with pluggable `ResourceAnalyzer` objects.


Command Line Usage
==================

The basic command line usage is as follows:

	java -jar bindex2.jar -n Bundles bundles/*.jar

This generates an index file in the local directory named `repository.xml` with metadata for all JAR files found under the `bundles` directory. The repository name will be set to `Bundles` and the URLs for the resources will be relative to the current directory, i.e.:

	<attribute name='osgi.content' value='bundles/foo.jar'/>

The full set of options is as follows:

	java -jar bindex2.jar
		[-h]
		[-r repository.(xml|zip)]
		[-n Untitled]
		[-d rootdir]
		[-l file:license.html]
		[-v]
		[-stylesheet http://www.osgi.org/www/obr2html.xsl]
		[-t "%s" symbolic name "%v" version "%f" filename "%p" dirpath ]
		<file> [<file>*]

<table>
	<tr>
		<th>Option</th>
		<th>Description</th>
	</tr>

	<tr>
		<td>`-h`</td>
		<td>Provide short description of command line options.</td>
	</tr>

	<tr>
		<td>`-r repository.xml`</td>
		<td>Set the name of the repository file as it is stored in the on the file system. The default is repository.xml. The extension may also be .zip. In that case a zip file is created with a single entry called repository.xml.</td>
	</tr>

	<tr>
		<td>`-n Untitled`</td>
		<td>Sets the name of the repository as declared in the `name` attribute on the top-level `repository` element of the XML index document.</td>
	</tr>

	<tr>
		<td>`d rootdir`</td>
		<td>Sets the root directory from where files are indexed. Default is the current directory.</td>
	</tr>

	<tr>
		<td>`-l licence.html`</td>
		<td>Provides a default for the license file. If the bundle itself does not provide a license file, this license is used. There is no default provided.</td>
	</tr>

	<tr>
		<td>`-v`</td>
		<td>Enables verbose reporting to the console.</td>
	</tr>

	<tr>
		<td>`-stylesheet <stylesheet>`</td>
		<td>Sets a custom stylesheet for the xml. The default style sheet is `http://www.osgi.org/www/obr2html.xsl`.</td>
	</tr>

	<tr>
		<td>`-t <template>`</td>
		<td>
		<p>Use a template to make the URL of the resource. The template supports the following keys. The examples assume d1/d2/f1.jar with com.acme.bundle as symbolic name and 1.0 as version:</p>
		
		<pre>
		%s com.acme.bundle Bundle Symbolic Name
		%v 1.0             Bundle Version
		%p d1/d2           Relative directory path of current working directory
		%f f1.jar          File name of resource
		</pre>
		</td>
	</tr>
</table>

If custom resource analyzers are required (see below), these can be simply placed on the Java runtime classpath. In this case the `java -jar` launch method cannot be used, so it is necessary to launch with the application class name as follows:

	java -cp bindex2.jar;foo-analyzer.jar \
	     org.osgi.impl.bundle.bindex.cli.Index \
	     -n MyRepo bundles/*.jar

Library Usage
=============

BIndex2 can be used as a JAR library in a conventional Java application or web/Java EE container by adding `bindex2.jar` to your application classpath. The API is as follows:

	BIndex2 indexer = new BIndex2();
	// optional: add one or more custom resource analyzers
	indexer.add(new MyExtenderResourceAnalyzer(), null);

	// optional: set config params
	Map<String, String> config = new HashMap<String, String>();
	config.put(ResourceIndexer.REPOSITORY_NAME, "My Repository");

	Set<File> inputs = findInputs();
	Writer output = new FileWriter("repository.xml");
	indexer.index(inputs, output, config);

Note that it is not generally encouraged to instantiate the `BIndex2` class directly as shown, since this creates an implementation dependency. It is better to use the `ResourceIndexer` API interface along with a Dependency Injection framework such as Guice or Spring to supply the instance. It is even better to use `ResourceIndexer` as an OSGi Service as shown in the next section.

Resource analyzers are added with an optional Filter, which is matched against incoming resources as they are processed. If the filter is `null` then the analyzer is invoked for all incoming resources. Filters are generated using the `org.osgi.framework.FrameworkUtil.createFilter()` method, for example:
	
	import static org.osgi.framework.FrameworkUtil.createFilter;
	// ...
	Filter warFilter = createFilter("(name=*.war)");
	indexer.add(new WarAnalyzer(), warFilter);

For more information on the filter string syntax and the properties available to match, see "Resource Analyzers" below.

OSGi Bundle Usage
=================

BIndex2 is also an OSGi bundle that publishes a service under the interface `org.osgi.service.bindex.ResourceIndexer` when in ACTIVE state. For example, to use the `ResourceIndexer` service from a Declarative Services component:

	@Reference
	public void setIndexer(ResourceIndexer indexer) {
		this.indexer = indexer;
	}
	
	public void doSomething() {
		// ...
		indexer.index(input, output, config);
	}

When used as an OSGi bundle, BIndex2 uses the "Whiteboard Pattern" to find custom resource analyzers. The filter, if required, can be given as a property of the service. For example to register an analyzer for WAR files, again using Declarative Services annotations:

	@Component(property = "filter=(name=*.war)")
	public class WarAnalyzer implements ResourceAnalyzer {
		// ...
	}

Resource Analyzers
==================

BIndex2 is expected to be used primarily for analyzing and indexing OSGi bundles. However it is designed to be extensible to analyze any other kind of resource, since the OSGi Repository specification supports arbitrary resource types. It can also be extended to extract additional metadata from existing known types such as OSGi bundles.

For example, we may wish to extend BIndex2 to understand configuration files, script files, or native libraries. Alternatively we may wish to process custom extender headers from the MANIFEST.MF of OSGi bundles.

The `ResourceAnalyzer` interfaces defines a single method `analyzeResource` which takes a `Resource` object and the lists of already discovered Requirements and Capabilities. An analyzer is permitted to add zero to many of each but it must not remove or alter any existing entries. The `Builder` class is provided as a convenience for constructing instances of Capability and Requirement.

The `Resource` interface is an abstraction over the types of resource that may be supplied to the analyzer. Analyzer implementations are not expected to implement `Resource`. The abbreviated interface is as follows:

	public interface Resource {
		String getLocation();
		Dictionary<String, Object> getProperties();
		long getSize();
		InputStream getStream() throws IOException;
		Manifest getManifest() throws IOException;
		List<String> listChildren(String prefix) throws IOException;
		Resource getChild(String path) throws IOException;
		void close();
	}

We expect that in the vast majority of cases, the resource in question will be a bundle, i.e. a JAR file. Therefore the `getManifest`, `listChildren` and `getChild` methods are provided as optimisations so that each analyzer does not need to re-parse the JAR file. If the resource does not contain a `META-INF/MANIFEST.MF` then `getManifest` will return `null`. If the resource is not a "compound" resource such as a JAR or ZIP then `listChildren` and `getChild` will return `null` for all input values.

Access to the underlying content of the file is always available through the `getStream` method but this should be considered an expensive operation that should only be called when the data required is not available from the properties, manifest or children.

Example Analyzer
----------------

The following example shows an analyzer for a custom extender header, `Help-Docs`. If the analyzer finds this header in the bundle manifest, it generates both a capability and a requirement:

	public class HelpExtenderAnalyzer {
		public void analyzeResource(Resource resource,
									List<? super Capability> caps,
									List<? super Requirement> reqs) {

			// Ignore this resource if no META-INF/MANIFEST.MF
			// or Help-Docs header
			Manifest manifest = resource.getManifest();
			if (manifest == null)
				return;
			String help = manifest.getMainAttributes().getValue("Help-Docs");
			if (help == null)
				return;
			
			// Generate a new requirement
			Requirement req = new Builder()
				.setNamespace("help.system")
				.addDirective("filter", "(&(version>=1.0)(!(version>=2.0)))")
				.buildRequirement();
			reqs.add(req);
			
			// Generate a new capability
			String bsn = manifest.getMainAttributes()
								 .getValue(Constants.BUNDLE_SYMBOLICNAME);
			Capability cap = new Builder()
				.setNamespace("help.doc")
				.addAttribute("version", new Version("1.0"))
				.addAttribute("bsn", bsn);
			caps.add(cap);
		}
	}

Resource Properties and Filters
-------------------------------

The `getProperties` method on `Resource` returns a dictionary of properties for the resource. These properties can be used to define filters so that an analyzer can be invoked only for a subset of interesting resources. The property names are defined as static constants on the `Resource` interface:

* `name`: the simple name of the resource, i.e. usually the filename excluding its location path;
* `location`: the full location of the resource (also available from the `getLocation` method);
* `size`: the size (as a `Long` value) of the resource in bytes (also available from the `getSize` method);
* `lastmodified`: the last-modified timestamp of the resource, as a `Long` value representing milliseconds since the epoch (00:00:00 GMT, January 1, 1970).

The filter expression syntax is described in section 3.2.7 of the OSGi Core Specification.

For example, to select files named either `foo.jar` or ending with the `.ear` extension, created on or after midnight on 1 January 2010:

	(&(|(name=foo.jar)(name=*.ear))(lastmodified>=1262217600753))

Packaging Resource Analyzers
----------------------------

Resource analyzers should be packaged for delivery as OSGi bundles that register services under the `ResourceAnalyzer` interface. The BIndex2 command-line application uses a lightweight OSGi-like runtime called [PojoSR](https://code.google.com/p/pojosr/) to configure services, and looks for resource analyzers using the whiteboard pattern.

Note that if an analyzer implementation relies on Declarative Service to manage and register it as a service, then it will be necessary to place a Declarative Service implementation (also known as a Service Component Runtime or SCR) on the BIndex2 classpath. For example the Apache Felix SCR can be used as follows:

	java -cp bindex2.jar;org.apache.felix.scr-1.6.0.jar;MyAnalyzer.jar \
			 org.osgi.impl.bundle.bindex.cli.Index \
			 bundles/*.jar

Building from Source
====================

BIndex can be built from source as follows:

	$ cd bindex2.build
	$ ant release

The `bindex2.jar` file will be released into `release/bindex2/bindex2-VERSION.jar`.

To run all unit and integration tests, invoke `ant test`. The test report will be generated into `generated/test-reports/junit-noframes.html`.