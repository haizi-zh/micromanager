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

public class AcqAnalyzer extends TaggedImageAnalyzer {

	private static AcqAnalyzer instance_;
	private long frameNum_;

	private Listener listener_;
	private double elapsed = 0;
	public int bitDepth_;
	public double imgwidth_;
	public double imgheight_;
	public  String acqName_;
	private Kernel kernel_;
	private Preferences preferences_;
	private long timeStart;	


	public static AcqAnalyzer getInstance() {	
		return instance_;
	}
	public static AcqAnalyzer getInstance(Kernel kernel, Listener listener,
			List<RoiItem> roiList_, OverlayRender render, Preferences preferences) {
		if(instance_ == null)
			instance_ = new AcqAnalyzer(kernel,listener,roiList_,render,preferences);
		return instance_;
	}

	public AcqAnalyzer(Kernel kernel, Listener listener,List<RoiItem> roiList,OverlayRender render, Preferences preferences) {
		listener_ = listener;
		kernel_ = kernel;
		preferences_ = preferences;
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
			String acqName = (String) taggedImage.tags.get("AcqName");
			boolean update = acqName.equals(MMStudioMainFrame.SIMPLE_ACQ) ? true
					: false;

			if(!listener_.isRunning()){
				Function.getInstance().dataReset();
				listener_.start(acqName);
				acqName_ = acqName;
				frameNum_ = 0;
				if(acqName.equals(MMStudioMainFrame.SIMPLE_ACQ)){
					kernel_.imageHeight =Integer.parseInt(taggedImage.tags.get("Height").toString());
					kernel_.imageWidth = Integer.parseInt(taggedImage.tags.get("Width").toString());
				}
				else{
					kernel_.imageHeight=Integer.parseInt((String)taggedImage.tags.get("Height"));
					kernel_.imageWidth =Integer.parseInt((String)taggedImage.tags.get("Width"));
				}
			}
			if(!update){
				frameNum_ = MDUtils.getFrameIndex(taggedImage.tags);
			}
			else{
				frameNum_ ++;
			}
			String nameComp;
			if (acqName.equals(MMStudioMainFrame.SIMPLE_ACQ))
				nameComp = "Live";
			else
				nameComp = acqName;

			if(!kernel_.getPosition(taggedImage.pix))return;
			try {
				kernel_.saveRoiData(nameComp,frameNum_,elapsed);
			} catch (IOException e) {
				MMT.logError("Save data error");
			}
			Function.getInstance().updateChart(frameNum_);
			Function.getInstance().reDraw(acqName, frameNum_, update);
			if(MMTFrame.getInstance().isMagnetAuto() && (frameNum_ % (int)(preferences_.frameToCalcForce_) == 0)){
				Function.getInstance().PullMagnet();
			}

		} catch (JSONException e) {
		} catch (MMScriptException e) {
		}
		System.out.print(String.format("\r\n%d:\tcostTime:\t%f", frameNum_,(System.nanoTime()-timeStart)/10e6));
	}
}