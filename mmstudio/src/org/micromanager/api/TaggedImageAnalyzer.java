/**
 * Example implementation of DataProcessor
 */

package org.micromanager.api;

import mmcorej.TaggedImage;

/**
 * This class is used to analyze (but not modify images). As images arrive in
 * the input queue, they are passed on to the DataProcessor output queue, but
 * the reference is held for analysis. This analysis should be carried out by
 * overriding the analyze function.
 */
public abstract class TaggedImageAnalyzer extends DataProcessor<TaggedImage> {

	private String analyzerName_;

	public TaggedImageAnalyzer(String baseName) {
		analyzerName_ = baseName;
		setName(analyzerName_);
	}

	public TaggedImageAnalyzer() {
		analyzerName_ = this.getClass().getSimpleName();
		setName(analyzerName_);
	}

	/*
	 * This method is overriding DataProcessor.process() to poll() one image at
	 * a time, pass it on to the output queue, and then call analyze on the
	 * image.
	 */
	@Override
	protected void process() throws InterruptedException {
		TaggedImage taggedImage;
		taggedImage = poll();
		analyze(taggedImage);
		produce(taggedImage);
	}

	/*
	 * Override this method to analyze images as they arrive.
	 */
	protected abstract void analyze(TaggedImage taggedImage);

}
