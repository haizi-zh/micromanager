package org.ndaguan.micromanager.mmtracker;
import mmcorej.TaggedImage;

import org.micromanager.MMStudioMainFrame;
import org.micromanager.acquisition.TaggedImageQueue;
import org.micromanager.api.TaggedImageAnalyzer;

public class TestAnalyzer extends TaggedImageAnalyzer {

	private static TestAnalyzer instance_;

	private int calibrationState_;
	public int bitDepth_;
	public double imgwidth_;
	public double imgheight_;
	public  String acqName_;
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
		if(Function.isTestingRunning_){
			if(calibrationState_ < kernel_.zPosProfiles.length){
				try {
					double[] pos = Function.getInstance().getStagePostion();
					boolean ret = kernel_.getPosition(taggedImage.pix);
					if(!ret){
						MMT.logError("Testting Error");
						return;
					}
					Function.getInstance().reDraw(MMStudioMainFrame.SIMPLE_ACQ,calibrationState_, true);
					Function.getInstance().updateTestingChart(pos[2]);
					MMT.logMessage((String.format("Testing:\tZ\t%d/%d\tXP\t%f\tYP\t%f\tZP\t%f\n",calibrationState_,kernel_.zPosProfiles.length,pos[0],pos[1],pos[2])));
					calibrationState_++;
					if(calibrationState_ < kernel_.zPosProfiles.length)
					Function.getInstance().setZCalPosition(calibrationState_);
					Function.getInstance().snapImage();

				} catch (Exception e) {
					MMTFrame.getInstance().setCalibrateIcon(true);
					Function.isTestingRunning_ = false;
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
					System.out.print(String.format("error\t%s\nTesting false!", e.toString()));
				}

			}
			else{
				MMTFrame.getInstance().setCalibrateIcon(true);
				Function.getInstance().installCalibrateAnalyzer(false);
				Function.getInstance().installTestingAnalyzer(false);
				Function.getInstance().installAcqAnalyzer(true);
				calibrationState_ = 0;
				kernel_.isCalibrated_ = true;
				MMTFrame.getInstance().LiveViewCaptureEnable(true);
				MMTFrame.getInstance().MultCaptureEnable(true);
				Function.isTestingRunning_  = false;
				try {
					Function.getInstance().StageGoHome();
				} catch (Exception e1) {
					MMT.logError("Stage cannot go back home");
				}
				Function.getInstance().snapImage();
				Function.getInstance().liveView();
				Function.getInstance().cleanStaticData();
			}
		}
		//calibration end
	}
}