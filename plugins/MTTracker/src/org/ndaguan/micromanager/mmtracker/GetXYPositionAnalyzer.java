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
	private double elapsed = 0;
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
			synchronized(MMT.Acqlock){
				if(kernel_.roiList_.size()<=0){
					Function.getInstance().reDraw(acqName, frameNum_, update);
					return;
				}
				if(!kernel_.getXYPosition(taggedImage.pix))return;
			}//lock
			String nameComp;
			if (acqName.equals(MMStudioMainFrame.SIMPLE_ACQ))
				nameComp = "Live";
			else
				nameComp = acqName;
			if(MMT.VariablesNUPD.saveFile.value() == 1 && MMT.VariablesNUPD.responceXY.value() == 1)
				try {
					kernel_.saveRoiData(nameComp,frameNum_,elapsed);
				} catch (IOException e) {
					MMT.logError("Save data error");
				}
			Function.getInstance().reDraw(acqName, frameNum_, update);
			if( MMT.VariablesNUPD.responceXY.value() == 1){
				Function.getInstance().updateChart(frameNum_);
				if(MMTFrame.getInstance().isMagnetAuto() && (frameNum_ % (int)(MMT.VariablesNUPD.frameToCalcForce.value()) == 0)){
					Function.getInstance().PullMagnet();
				}
			}

		} catch (JSONException e) {
		} catch (MMScriptException e) {
		}
		System.out.print(String.format("\r\n%d:\tcostTime:\t%f\t\n", frameNum_,(System.nanoTime()-timeStart)/10e6));

	}
}