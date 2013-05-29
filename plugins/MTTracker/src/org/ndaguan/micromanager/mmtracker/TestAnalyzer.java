package org.ndaguan.micromanager.mmtracker;
import mmcorej.TaggedImage;

import org.micromanager.MMStudioMainFrame;
import org.micromanager.acquisition.TaggedImageQueue;
import org.micromanager.api.TaggedImageAnalyzer;

public class TestAnalyzer extends TaggedImageAnalyzer {

	private static TestAnalyzer instance_;
	private Kernel kernel_;

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

		if (!MMT.isTestingRunning_ || taggedImage == null || taggedImage == TaggedImageQueue.POISON)return;
		//Testing start
		try {
			double[] pos = Function.getInstance().getStagePosition();
			boolean ret = kernel_.getXYZPosition(taggedImage.pix);
			Function.getInstance().reDraw(MMStudioMainFrame.SIMPLE_ACQ,MMT.testingIndex_, true,true);
			if(!ret){
				MMT.logError("Testting Error");
				MMT.isTestingRunning_ = false;
				MMT.isAnalyzerBusy_ = false;
				return;
			}
			Function.getInstance().updateTestingChart(pos[2]);
			MMT.logMessage(String.format("Testing:\t\t%d/%d",MMT.testingIndex_,kernel_.zTestingPosProfiles.length));
			MMT.debugMessage((String.format("Testing:\tZ\t%d/%d\tXP\t%f\tYP\t%f\tZP\t%f\n",MMT.testingIndex_,kernel_.zTestingPosProfiles.length,pos[0],pos[1],pos[2])));
		} catch (Exception e) {
			MMT.isTestingRunning_  = false;
			MMT.lastError_ = e.toString();
		}
		MMT.isAnalyzerBusy_ = false;
		//Testing end
	}
}