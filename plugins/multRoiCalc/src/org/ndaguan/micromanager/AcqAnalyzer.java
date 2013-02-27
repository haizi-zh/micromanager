package org.ndaguan.micromanager;
import java.util.ArrayList;

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

	private OverlayRender render_;
	private Kernel kernel_;
	private Listener listener_;
	private ArrayList<RoiItem> roiList_;
	private Function function_;
	private long startTs_;
	private double elapsed = 0;	


	public static AcqAnalyzer getInstance() {	
		return instance_;
	}
	public static AcqAnalyzer getInstance(Kernel kernel, Listener listener,
			ArrayList<RoiItem> roiList, Function function, OverlayRender render) {
		if(instance_ == null)
			instance_ = new AcqAnalyzer(kernel,listener,roiList,function,render);
		return instance_;
	}

	public AcqAnalyzer(Kernel kernel, Listener listener,ArrayList<RoiItem> roiList, Function function,OverlayRender render) {
		kernel_ = kernel;
		listener_ = listener;
		roiList_ = roiList;
		function_ = function;
		render_ = render;

		frameNum_ = 0;
	}

	@Override
	protected void analyze(TaggedImage taggedImage) {
		
		if (taggedImage == null || taggedImage == TaggedImageQueue.POISON)
		{
			function_.dataReset();
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
			if(!listener_.isRunning()){
				listener_.start(acqName);
				Preferences.getInstance().acqName_ = acqName;
				Preferences.getInstance().imgheight_ = (long)taggedImage.tags.get("Height");
				Preferences.getInstance().imgwidth_ = (long)taggedImage.tags.get("Width");
				Kernel.getInstance().releaseBuffer();
				Kernel.getInstance().dataInitialize();
			}
			boolean update = acqName.equals(MMStudioMainFrame.SIMPLE_ACQ) ? true
					: false;
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
			function_.getPosition(taggedImage.pix,frameNum_,nameComp,elapsed);

			function_.reDraw(acqName, frameNum_, update);
			if(ZIndexMeasureFrame.getInstance().isMagnetAuto() && (frameNum_ % (int)(Preferences.getInstance().frameToCalcForce_) == 0)){
				function_.PullMagnet();
			}

		} catch (JSONException e) {
		} catch (MMScriptException e) {
		}
	}
}