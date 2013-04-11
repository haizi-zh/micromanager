package org.ndaguan.micromanager.mmtracker;
import mmcorej.TaggedImage;

import org.micromanager.MMStudioMainFrame;
import org.micromanager.acquisition.TaggedImageQueue;
import org.micromanager.api.TaggedImageAnalyzer;

public class CalibrateAnalyzer extends TaggedImageAnalyzer {

	private static CalibrateAnalyzer instance_;

	private int calibrationState_;
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
		calibrationState_ = 0;
	}

	@Override
	protected void analyze(TaggedImage taggedImage) {

		if (!Function.isCalibrationRunning || taggedImage == null || taggedImage == TaggedImageQueue.POISON)
		{
			Function.getInstance().dataReset();
			calibrationState_ = 0;
			return;
		}
		//calibration start
		try {
			double[] pos = Function.getInstance().getStagePostion();
			boolean ret = kernel_.calibration(taggedImage.pix, calibrationState_,pos[0],pos[1],pos[2]);
			if(!ret){
				MMT.logError("Calbration err");
				return;
			}
			Function.getInstance().reDraw(MMStudioMainFrame.SIMPLE_ACQ,calibrationState_, true);
			MMT.logMessage((String.format("Z\t%d/%d\tXP\t%f\tYP\t%f\tZP\t%f\n",calibrationState_,kernel_.zPosProfiles.length,pos[0],pos[1],pos[2])));

		} catch (Exception e) {
			Function.isCalibrationRunning = false;
			calibrationState_ = 0;
		}

	}
	//calibration end
}