package org.ndaguan.micromanager;
import java.util.ArrayList;

import mmcorej.TaggedImage;

import org.json.JSONException;
import org.micromanager.MMStudioMainFrame;
import org.micromanager.acquisition.TaggedImageQueue;
import org.micromanager.api.TaggedImageAnalyzer;
import org.micromanager.utils.MDUtils;
import org.micromanager.utils.MMScriptException;
import org.ndaguan.micromanager.OverlayRender.RenderItem;

public class AcqAnalyzer extends TaggedImageAnalyzer {

	private static AcqAnalyzer instance_;
	private long frameNum_;

	private OverlayRender render_;
	private Kernel kernel_;
	private Listener listener_;
	private ArrayList<RenderItem> roiList_;
	private Function function_;	
	boolean debug =  false;

	public static AcqAnalyzer getInstance() {	
		return instance_;
	}
	public static AcqAnalyzer getInstance(Kernel kernel, Listener listener,
			ArrayList<RenderItem> roiList, Function function, OverlayRender render) {
		if(instance_ == null)
			instance_ = new AcqAnalyzer(kernel,listener,roiList,function,render);
		return instance_;
	}

	public AcqAnalyzer(Kernel kernel, Listener listener,ArrayList<RenderItem> roiList, Function function,OverlayRender render) {
		kernel_ = kernel;
		listener_ = listener;
		roiList_ = roiList;
		function_ = function;
		render_ = render;

		frameNum_ = 0;
		//demo


		roiList_.add(RenderItem.createInstance(new double[]{300,300,0,0,0},false));
	}

	@Override
	protected void analyze(TaggedImage taggedImage) {
		long stat = System.nanoTime();
		if (taggedImage == null || taggedImage == TaggedImageQueue.POISON)
		{
			function_.dataReset();
			frameNum_ = 0;
			return;
		}

		kernel_.getPosition(taggedImage.pix);

		try {
			String acqName = (String) taggedImage.tags.get("AcqName");
			if(!listener_.isRunning()){
				listener_.start(acqName);
			}
			boolean update = acqName.equals(MMStudioMainFrame.SIMPLE_ACQ) ? true
					: false;
			if(!update){
				frameNum_ = MDUtils.getFrameIndex(taggedImage.tags);
			}
			else{
				frameNum_ ++;
			}
			render_.render(acqName, roiList_, frameNum_, update);

		} catch (JSONException e) {
		} catch (MMScriptException e) {
		}
		if(debug)System.out.print(String.format("\t\t\t\tTime consume:\t%.2f\r\n",(System.nanoTime() - stat)/1e6));
	}
}