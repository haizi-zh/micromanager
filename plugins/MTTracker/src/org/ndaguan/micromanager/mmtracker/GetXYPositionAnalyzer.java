package org.ndaguan.micromanager.mmtracker;
import java.io.IOException;
import java.util.List;

import mmcorej.TaggedImage;

import org.json.JSONException;
import org.micromanager.MMStudioMainFrame;
import org.micromanager.acquisition.TaggedImageQueue;
import org.micromanager.api.TaggedImageAnalyzer;
import org.micromanager.utils.MDUtils;
import org.micromanager.utils.MMScriptException;

public class GetXYPositionAnalyzer extends TaggedImageAnalyzer {

	private static GetXYPositionAnalyzer instance_;
	private long frameNum_;

	private Listener listener_;
	public int bitDepth_;
	public double imgwidth_;
	public double imgheight_;
	public  String acqName_;
	private Kernel kernel_;
	private long timeStart;	


	public static GetXYPositionAnalyzer getInstance() {	
		return instance_;
	}
	public static GetXYPositionAnalyzer getInstance(Kernel kernel, Listener listener,
			List<RoiItem> roiList_, OverlayRender render) {
		if(instance_ == null)
			instance_ = new GetXYPositionAnalyzer(kernel,listener,roiList_,render);
		return instance_;
	}

	public GetXYPositionAnalyzer(Kernel kernel, Listener listener,List<RoiItem> roiList,OverlayRender render) {
		listener_ = listener;
		kernel_ = kernel;
		frameNum_ = 0;
	}

	@Override
	protected void analyze(TaggedImage taggedImage) {

		timeStart = System.nanoTime();

		if (taggedImage == null || taggedImage == TaggedImageQueue.POISON)
		{
			Function.getInstance().dataReset();
			frameNum_ = 0;
			return;
		}
		try {
			if(!listener_.isRunning()){
				Function.getInstance().dataReset();
				RoiItem.saveMetadata(taggedImage.tags);
				listener_.start();
				frameNum_ = 0;
				kernel_.imageWidth = RoiItem.imageWidth;
				kernel_.imageHeight = RoiItem.imageHeight;
			}

			RoiItem.updateTimestamp(taggedImage.tags);
		} catch (MMScriptException | JSONException e) {
			MMT.logError(e.toString());
			return;
		}

		frameNum_ ++;
		//get position
		synchronized(MMT.Acqlock){
			if(kernel_.roiList_.size()<=0){
				Function.getInstance().reDraw(frameNum_,true,true);
				return;
			}
			if(!kernel_.getXYPosition(taggedImage.pix))return;
			doFeedBack();
		}//lock

		writeFile();
		redrawImage();
		updateChart();
		pullMagnet();

		System.out.print(String.format("\r\n%d:\tcostTime:\t%f\t\n", frameNum_,(System.nanoTime()-timeStart)/10e6));
	}
	private void pullMagnet() {
		if(MMTFrame.getInstance().isMagnetAuto() && (frameNum_ % (int)(MMT.VariablesNUPD.frameToCalcForce.value()) == 0)){
			Function.getInstance().PullMagnet();
		}		
	}
	private void updateChart() {
		if( MMT.VariablesNUPD.responceXY.value() == 1){
			Function.getInstance().updateChart(frameNum_);


		}		
	}
	private void redrawImage() {
		Function.getInstance().reDraw(frameNum_, false,true);		
	}
	private void writeFile() {
		if(MMT.VariablesNUPD.saveFile.value() == 1 && MMT.VariablesNUPD.responceXY.value() == 1)
			try {
				kernel_.saveRoiData(frameNum_);
			} catch (IOException e) {
				MMT.logError("Save data error");
			}		
	}
	private void doFeedBack() {
		if(MMT.isFeedbackRunning_ && frameNum_ % MMT.VariablesNUPD.frameToFeedBack.value() == 0){
			Function.getInstance().doFeedback();
		}		
	}

}