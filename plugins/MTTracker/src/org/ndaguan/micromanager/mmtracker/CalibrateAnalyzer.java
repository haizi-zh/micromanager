package org.ndaguan.micromanager.mmtracker;
import mmcorej.TaggedImage;

import org.micromanager.MMStudioMainFrame;
import org.micromanager.acquisition.TaggedImageQueue;
import org.micromanager.api.TaggedImageAnalyzer;

public class CalibrateAnalyzer extends TaggedImageAnalyzer {

	private static CalibrateAnalyzer instance_;

	private int calibrationState_;
	public int bitDepth_;
	public double imgwidth_;
	public double imgheight_;
	public  String acqName_;
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

		if (taggedImage == null || taggedImage == TaggedImageQueue.POISON)
		{
			Function.getInstance().dataReset();
			calibrationState_ = 0;
			return;
		}
		//calibration start
		if(Function.isCalibrationRunning){
			if(calibrationState_ < kernel_.zPosProfiles.length){
				try {
					double[] pos = Function.getInstance().getStagePostion();
					boolean ret = kernel_.calibration(taggedImage.pix, calibrationState_,pos[2]);
					if(!ret){
						MMT.logError("Calbration err");
						return;
					}
					Function.getInstance().reDraw(MMStudioMainFrame.SIMPLE_ACQ,calibrationState_, true);
					MMT.logMessage((String.format("Z\t%d/%d\tXP\t%f\tYP\t%f\tZP\t%f\n",calibrationState_,kernel_.zPosProfiles.length,pos[0],pos[1],pos[2])));
					calibrationState_++;
					if(calibrationState_ < kernel_.zPosProfiles.length)
					Function.getInstance().setXYZCalPosition(calibrationState_);
					Function.getInstance().snapImage();

				} catch (Exception e) {
					MMTFrame.getInstance().setCalibrateIcon(true);
					Function.isCalibrationRunning = false;
					Function.getInstance().installCalibrateAnalyzer(false);
					Function.getInstance().installAcqAnalyzer(true);
					calibrationState_ = 0;
					kernel_.isCalibrated_ = true;
					try {
						Function.getInstance().StageGoHome();
					} catch (Exception e1) {
						MMT.logError("Stage cannot go back home");
					}
					Function.getInstance().snapImage();
					Function.getInstance().liveView();
					System.out.print(String.format("error\t%s\ncalibrate false!", e.toString()));
				}

			}
			else{
				MMTFrame.getInstance().setCalibrateIcon(true);
				Function.isCalibrationRunning = false;
				Function.getInstance().installCalibrateAnalyzer(false);
				Function.getInstance().installTestingAnalyzer(true);
				calibrationState_ = 0;
				kernel_.isCalibrated_ = true;
				MMTFrame.getInstance().LiveViewCaptureEnable(true);
				MMTFrame.getInstance().MultCaptureEnable(true);
				Function.isTestingRunning_  = true;
				try {
					Function.getInstance().StageGoHome();
				} catch (Exception e1) {
					MMT.logError("Stage cannot go back home");
				}
				Function.getInstance().snapImage();
				Function.getInstance().cleanStaticData();
				Function.getInstance().cleanTestingData();
			}
		}
		//calibration end
	}
}