package org.zephyre.micromanager;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import mmcorej.TaggedImage;

import org.json.JSONException;
import org.json.JSONObject;
import org.micromanager.MMStudioMainFrame;
import org.micromanager.acquisition.MMAcquisition;
import org.micromanager.acquisition.TaggedImageQueue;
import org.micromanager.acquisition.VirtualAcquisitionDisplay;
import org.micromanager.api.ScriptInterface;
import org.micromanager.api.TaggedImageAnalyzer;
import org.micromanager.utils.MDUtils;
import org.micromanager.utils.MMScriptException;
import org.micromanager.utils.ReportingUtils;
import org.zephyre.micromanager.OverlayRender.RenderItem;

public class AcqAnalyzer extends TaggedImageAnalyzer {
	private long cnt_;
	private Point loc_;
	private Random rand_;
	private final int RADIUS = 5;
	private ScriptInterface gui_;
	private OverlayRender render_;

	public AcqAnalyzer(ScriptInterface gui) {
		rand_ = new Random();
		gui_ = gui;
		render_ = OverlayRender.getInstance(gui);
	}

	@Override
	protected void analyze(TaggedImage taggedImage) {
		if (taggedImage == null || taggedImage == TaggedImageQueue.POISON)
			return;

		if (loc_ == null) {
			JSONObject tags = taggedImage.tags;
			try {
				int width = MDUtils.getWidth(tags);
				int height = MDUtils.getHeight(tags);
				loc_ = new Point(width / 2, height / 2);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		} else {
			loc_.x += Math.round(2 * rand_.nextGaussian());
			loc_.y += Math.round(2 * rand_.nextGaussian());
		}

		try {
			String acqName = (String) taggedImage.tags.get("AcqName");
			int frameNum = MDUtils.getFrameIndex(taggedImage.tags);
			String message = String.format(
					"AcqAnalyzer: %d | Thread: %s / %d | AcqName: %s", cnt_,
					Thread.currentThread().getName(), Thread.currentThread()
							.getId(), acqName);
			IJ.log(message);
			cnt_++;

			ArrayList<RenderItem> list = new ArrayList<OverlayRender.RenderItem>();
			list.add(RenderItem.createInstance(loc_,
					String.format("(%d, %d)", loc_.x, loc_.y)));
			boolean update = acqName.equals(MMStudioMainFrame.SIMPLE_ACQ) ? true
					: false;
			render_.render(acqName, list, frameNum, update);
		} catch (JSONException e) {
		} catch (MMScriptException e) {
			// 指定的Acquisition不存在（一般来说，是因为还未来得及建立）
		}
	}
}