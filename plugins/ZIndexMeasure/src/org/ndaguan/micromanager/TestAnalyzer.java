package org.ndaguan.micromanager;

import mmcorej.TaggedImage;

import org.micromanager.MMStudioMainFrame;
import org.micromanager.api.TaggedImageAnalyzer;

public class TestAnalyzer extends TaggedImageAnalyzer{

	private static TestAnalyzer instance_;
	private ZIndexMeasure main;
	private MMStudioMainFrame gui_;
	public TestAnalyzer(MMStudioMainFrame gui,ZIndexMeasure main_) {
		main =  main_;		
		gui_ =   gui;
	}
	public static TestAnalyzer getInstance(MMStudioMainFrame gui,
			ZIndexMeasure zIndexMeasure) {
		if(instance_ == null)
			instance_ = new TestAnalyzer(gui,zIndexMeasure);
		return instance_;
	}

	@Override
	protected void analyze(TaggedImage taggedImage) {
		//testing start
		if(main.isTestingRunning){
			if(main.calibrationState_ < main.getCalibrateLenth()-1){
				main.SaveTestingData(taggedImage.pix,main.calibrationState_);
				main.calibrationState_++;
				main.setTestingPosition(main.calibrationState_);
				gui_.snapSingleImage();
				return;
			}
			else{
				main.isTestingRunning = false;
				main.calibrationState_ = 0;
				main.isCalibrated = true;
				main.testtingEnd();
			}
		}
		//testing end
	}

}
