package org.ndaguan.micromanager.mmtracker;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.swing.SwingUtilities;

import mmcorej.TaggedImage;
import org.json.JSONException;
import org.micromanager.MMStudioMainFrame;
import org.micromanager.acquisition.TaggedImageQueue;
import org.micromanager.api.TaggedImageAnalyzer;
import org.micromanager.utils.MDUtils;
import org.micromanager.utils.MMScriptException;

public class GetXYZPositionAnalyzer extends TaggedImageAnalyzer {

	private static GetXYZPositionAnalyzer instance_;
	private long frameNum_;

	private Listener listener_;
	private double elapsed = 0;
	public int bitDepth_;
	public double imgwidth_;
	public double imgheight_;
	public  String acqName_;
	private Kernel kernel_;
	private long timeStart;
	private String acqName;
	private boolean update;	
	private HashMap<Long, Double> timeStamp;
	private List<RoiItem> copyOfRoiList;
	private String nameComp;

	public static GetXYZPositionAnalyzer getInstance() {	
		return instance_;
	}
	public static GetXYZPositionAnalyzer getInstance(Kernel kernel, Listener listener,
			List<RoiItem> roiList_, OverlayRender render) {
		if(instance_ == null)
			instance_ = new GetXYZPositionAnalyzer(kernel,listener,roiList_,render);
		return instance_;
	}

	public GetXYZPositionAnalyzer(Kernel kernel, Listener listener,List<RoiItem> roiList,OverlayRender render) {
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
			elapsed = 0;
			if(!update){
				reCalculateXYZ();
				timeStamp.clear();
			}
			return;
		}

		if (taggedImage.tags.has("ElapsedTime-ms"))
		{
			try {
				elapsed = taggedImage.tags.getDouble("ElapsedTime-ms");
			} catch (JSONException e) {
			}

		}
		else{
			elapsed = System.nanoTime()  / 1e6;
		}

		try {
			if(!listener_.isRunning()){
				timeStamp = new HashMap<Long, Double>();
				copyOfRoiList = Collections.synchronizedList(new ArrayList<RoiItem>());
				for (int i = 0; i < kernel_.roiList_.size(); i++) {
					copyOfRoiList.add(kernel_.roiList_.get(i));
				}
				
				acqName = (String) taggedImage.tags.get("AcqName");
				update = acqName.equals(MMStudioMainFrame.SIMPLE_ACQ) ? true
						: false;
				if (acqName.equals(MMStudioMainFrame.SIMPLE_ACQ))
					nameComp = "Live";
				else
					nameComp = acqName;
				Function.getInstance().dataReset();
				listener_.start(acqName);
				acqName_ = acqName;
				frameNum_ = 0;
				if (acqName.equals(MMStudioMainFrame.SIMPLE_ACQ)) {
					kernel_.imageHeight = Integer.parseInt(taggedImage.tags
							.get("Height").toString());
					kernel_.imageWidth = Integer.parseInt(taggedImage.tags.get(
							"Width").toString());
				} else {
					Object height = taggedImage.tags.get("Height");
					Object width = taggedImage.tags.get("Width");
					if (height instanceof Number)
						kernel_.imageHeight = ((Number) height).intValue();
					else
						kernel_.imageHeight = Integer.parseInt(height
								.toString());
					if (width instanceof Number)
						kernel_.imageWidth = ((Number) width).intValue();
					else
						kernel_.imageHeight = Integer
						.parseInt(width.toString());
				}
				
			}
			if(!update){
				frameNum_ = MDUtils.getFrameIndex(taggedImage.tags);
			}
			else{
				frameNum_ ++;
			}
			
			if(MMTFrame.getInstance().isMagnetAuto() && (frameNum_ % (int)(MMT.VariablesNUPD.frameToCalcForce.value()) == 0)){
				Function.getInstance().PullMagnet();
			}
			//storage timestamp
			timeStamp.put(frameNum_,elapsed);
			if(!update && (frameNum_%MMT.VariablesNUPD.frameToRefreshChart.value() != 0)){
				return;
			}
			
			synchronized(MMT.Acqlock){
				if(kernel_.roiList_.size()<=0){
					Function.getInstance().reDraw(acqName, frameNum_, update,true);
					return;
				}
				if(!kernel_.getXYZPosition(taggedImage.pix))return;
				if(MMT.isFeedbackRunning_ && frameNum_ % MMT.VariablesNUPD.frameToFeedBack.value() == 0){
					Function.getInstance().doFeedback();
				}
			}//lock
			
			if(MMT.VariablesNUPD.saveFile.value() == 1 && kernel_.isCalibrated_)
				try {
					kernel_.saveRoiData(nameComp,frameNum_,elapsed);
				} catch (IOException e) {
					MMT.logError("Save data error");
				}
			Function.getInstance().updateChart(frameNum_);
			Function.getInstance().reDraw(acqName, frameNum_, update,false);

		} catch (JSONException e) {
		} catch (MMScriptException e) {
		}
		System.out.print(String.format("\r\n%d:\tcostTime:\t%f\t\n", frameNum_,(System.nanoTime()-timeStart)/10e6));

	}
	private void reCalculateXYZ() {
		final ImagePlus currentImage = WindowManager.getCurrentImage();
		ImageStack images = currentImage.getImageStack();
		kernel_.roiList_ = copyOfRoiList;
		for (int i = 1; i < images.getSize(); i++) {
			if(!timeStamp.containsKey((long)i))continue;
			if(!kernel_.getXYZPosition(images.getPixels(i)))return;
			final long index = i;
			try {
				kernel_.saveRoiData("full_"+nameComp,i-1,timeStamp.get((long)i));
			} catch (IOException e) {
				MMT.logError("Save data error");
			}
			SwingUtilities.invokeLater(new Runnable(){
				@Override
				public void run() {
					currentImage.setSlice((int) (index+1));
					Function.getInstance().updateChart(index);
					Function.getInstance().reDraw(acqName, index, update,false);
				}
			});
			
		}
		for(RoiItem it:kernel_.roiList_)
			it.dataClean(false);
		
	}
}