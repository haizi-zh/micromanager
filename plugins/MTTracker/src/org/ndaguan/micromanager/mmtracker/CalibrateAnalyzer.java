package org.ndaguan.micromanager.mmtracker;
import mmcorej.TaggedImage;

import org.micromanager.MMStudioMainFrame;
import org.micromanager.acquisition.TaggedImageQueue;
import org.micromanager.api.TaggedImageAnalyzer;

public class CalibrateAnalyzer extends TaggedImageAnalyzer {

	private static CalibrateAnalyzer instance_;
	private Kernel kernel_;
	public static CalibrateAnalyzer getInstance() {	
		return instance_;
	}
	public static CalibrateAnalyzer getInstance(Kernel kernel) {
		if(instance_ == null)
			instance_ = new CalibrateAnalyzer(kernel);
		return instance_;
	}

	public CalibrateAnalyzer(Kernel kernel) {
		kernel_ = kernel;
	}

	@Override
	protected void analyze(TaggedImage taggedImage) {

		if (!MMT.isCalibrationRunning_ || taggedImage == null || taggedImage == TaggedImageQueue.POISON)
		{
			return;
		}
		//calibration start
		double[] pos = null;
		try {
			pos = Function.getInstance().getStagePosition();
		} catch (Exception e) {
			MMT.isCalibrationRunning_ = false;
			MMT.isAnalyzerBusy_ = false;
			MMT.lastError_ = "Get stage position error" +e.toString();
			return;
		}
		
		boolean ret = kernel_.calibration(taggedImage.pix, MMT.calibrateIndex_,pos[0],pos[1],pos[2]);
		Function.getInstance().reDraw(MMStudioMainFrame.SIMPLE_ACQ,MMT.calibrateIndex_, true);
		if(!ret) 
		{
			MMT.isCalibrationRunning_ = false;
			MMT.isAnalyzerBusy_ = false;
			return;
		}
		
		MMT.logMessage(String.format("calibrating:\t\t%d/%d",MMT.calibrateIndex_,kernel_.zPosProfiles.length));
		MMT.debugMessage((String.format("Z\t%d/%d\tXP\t%f\tYP\t%f\tZP\t%f\n",MMT.calibrateIndex_,kernel_.zPosProfiles.length,pos[0],pos[1],pos[2])));

		MMT.isAnalyzerBusy_ = false;
	}
	
	
}