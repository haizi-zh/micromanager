package org.ndaguan.micromanager.mmtracker;
import mmcorej.TaggedImage;

import org.micromanager.MMStudioMainFrame;
import org.micromanager.acquisition.TaggedImageQueue;
import org.micromanager.api.TaggedImageAnalyzer;

public class TestAnalyzer extends TaggedImageAnalyzer {

	private static TestAnalyzer instance_;
	private Kernel kernel_;
	private long testingIndex_ = 0;

	public static TestAnalyzer getInstance() {	
		return instance_;
	}
	public static TestAnalyzer getInstance(Kernel kernel) {
		if(instance_ == null)
			instance_ = new TestAnalyzer(kernel);
		return instance_;
	}

	public TestAnalyzer(Kernel kernel) {
		kernel_ = kernel;
	}

	@Override
	protected void analyze(TaggedImage taggedImage) {

		if (!Function.isTestingRunning_ || taggedImage == null || taggedImage == TaggedImageQueue.POISON)return;
		//Testing start
		try {
			double[] pos = Function.getInstance().getStagePostion();
			boolean ret = kernel_.getPosition(taggedImage.pix);
			if(!ret){
				MMT.logError("Testting Error");
				return;
			}
			Function.getInstance().reDraw(MMStudioMainFrame.SIMPLE_ACQ,testingIndex_, true);
			Function.getInstance().updateTestingChart(pos[2]);
			MMT.logMessage((String.format("Testing:\tZ\t%d/%d\tXP\t%f\tYP\t%f\tZP\t%f\n",testingIndex_,kernel_.zPosProfiles.length,pos[0],pos[1],pos[2])));
		} catch (Exception e) {
			Function.isTestingRunning_ = false;
			testingIndex_ = 0;
		}
		testingIndex_ ++;
		MMT.isAnalyzerBusy_ = false;
		//Testing end
	}
}