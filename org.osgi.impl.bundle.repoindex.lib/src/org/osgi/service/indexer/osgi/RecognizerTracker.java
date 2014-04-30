package org.osgi.service.indexer.osgi;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.indexer.ResourceRecognizer;
import org.osgi.service.indexer.impl.RepoIndex;
import org.osgi.util.tracker.ServiceTracker;

class RecognizerTracker extends ServiceTracker {

	private final RepoIndex indexer;

	public RecognizerTracker(BundleContext context, RepoIndex indexer) {
		super(context, ResourceRecognizer.class.getName(), null);
		this.indexer = indexer;
	}

	@Override
	public Object addingService(ServiceReference reference) {
		ResourceRecognizer recognizer = (ResourceRecognizer) context.getService(reference);
		if (recognizer == null)
			return null;

		indexer.addRecognizer(recognizer);
		return recognizer;
	}

	@Override
	public void removedService(ServiceReference reference, Object service) {
		ResourceRecognizer recognizer = (ResourceRecognizer) service;
		indexer.removeRecognizer(recognizer);
	}
}
