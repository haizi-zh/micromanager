package org.ndaguan.micromanager;

import mmcorej.TaggedImage;

import org.micromanager.MMStudioMainFrame;
import org.micromanager.api.TaggedImageAnalyzer;

public class CalAnalyzer extends TaggedImageAnalyzer{

	private static CalAnalyzer instance_;
	private int calibrationState_ = 0;
	private ZIndexMeasure main;
	private MMStudioMainFrame gui_;
	public CalAnalyzer(MMStudioMainFrame gui,ZIndexMeasure main_) {
		main =  main_;		
		gui_ =   gui;
	}
	public static CalAnalyzer getInstance(MMStudioMainFrame gui,
			ZIndexMeasure zIndexMeasure) {
		if(instance_ == null)
			instance_ = new CalAnalyzer(gui,zIndexMeasure);
		return instance_;
	}

	@Override
	protected void analyze(TaggedImage taggedImage) {
		//calibration start
		if(main.isCalibrationRunning){
			if(calibrationState_ < main.getCalibrateLenth()-1){
				main.calibration(taggedImage.pix,calibrationState_);
				calibrationState_++;
				main.setXYZCalPosition(calibrationState_);
				gui_.snapSingleImage();
				return;
			}
			else{
				main.isCalibrationRunning = false;
				calibrationState_ = 0;
				gui_.snapSingleImage();
				main.isCalibrated =true;
			}
		}
		//calibration end

	}

}
