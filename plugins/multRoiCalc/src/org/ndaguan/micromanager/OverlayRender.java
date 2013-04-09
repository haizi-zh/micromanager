package org.ndaguan.micromanager;

import ij.ImageListener;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.ShapeRoi;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.PathIterator;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.micromanager.MMStudioMainFrame;
import org.micromanager.api.ScriptInterface;
import org.micromanager.utils.MMScriptException;

/**
 * 在相应的图像采集显示窗口中，设置overlay，显示相关信息。
 * 
 * @author Zephyre
 * 
 */
public class OverlayRender {
	
	protected ScriptInterface gui_;
	private Color labelColor_;
	private Font labelFont_;
	private int sizeCrossHair_;
	private Preferences preferences_;
	private static OverlayRender instance_;

	private OverlayRender(MMStudioMainFrame gui, Preferences preferences) {
		gui_ = gui;
		preferences_ =preferences;
		labelColor_ = Color.GREEN;
		labelFont_ = new Font("SansSerif", Font.PLAIN, 14);
		sizeCrossHair_ = 8;
		overlayMap_ = new HashMap<ImagePlus, HashMap<Long, Overlay>>();
		instance_ = this;

		// 更新overlay的回调函数
		ImagePlus.addImageListener(new ImageListener() {
			@Override
			public void imageClosed(ImagePlus arg0) {
				overlayMap_.remove(arg0);
			}

			@Override
			public void imageOpened(ImagePlus arg0) {
			}

			@Override
			public void imageUpdated(ImagePlus arg0) {
				// 如果正在采集，强行update会打乱显示。
				if (gui_.isAcquisitionRunning())
					return;

				HashMap<Long, Overlay> map = overlayMap_.get(arg0);
				if (map == null)
					return;
				int slice = arg0.getCurrentSlice();
				// zero-based
				Overlay overlay = map.get(Long.valueOf(slice - 1));
				arg0.setOverlay(overlay);
			}

		});
	}
	public static OverlayRender getInstance() {
		return instance_;
	}
	public static OverlayRender getInstance(MMStudioMainFrame gui, Preferences preferences) {
		if (instance_ == null)
			instance_ = new OverlayRender(gui,preferences);
		return instance_;
	}

	public void setCrossHairSize(int size) {
		sizeCrossHair_ = size;
	}

	public ScriptInterface getMainFrame() {
		return gui_;
	}

	public  void render(String acqName, Collection<RoiItem> itemList,
			long frameNumber, boolean update) throws MMScriptException {
		render(gui_.getAcquisition(acqName).getAcquisitionWindow()
				.getHyperImage(), itemList, frameNumber, update);
	}

	/**
	 * 
	 * @param image
	 *            指定渲染的目标窗s口。
	 * @param itemList
	 *            渲染目标的集合。
	 * @param frameNumber
	 * @param update
	 *            是否立刻重绘图像。
	 */
	public void render(final ImagePlus image, Collection<RoiItem> itemList,
			long frameNumber, boolean update) {
		if (image == null || itemList == null)
			return;
		// 新建Overlay
		Overlay overlay = new Overlay();

		// 清空原有的ROI，新建。
		Iterator<RoiItem> it = itemList.iterator();
		int beanRadius = (int) preferences_.beanRadiusPiexl_;
		while (it.hasNext()) {
			RoiItem item = it.next();
			if(item.isdelete_)continue;
			int x = (int)item.x_;
			int y = (int)item.y_;
			ShapeRoi sr = new ShapeRoi(new float[] { PathIterator.SEG_MOVETO,
					x - sizeCrossHair_, y, PathIterator.SEG_LINETO,
					x + sizeCrossHair_, y, PathIterator.SEG_MOVETO, x,
					y - sizeCrossHair_, PathIterator.SEG_LINETO, x,
					y + sizeCrossHair_, PathIterator.SEG_CLOSE });

			sr.setStrokeColor(item.getItemColor());
			sr.setName("");
			overlay.add(sr);
			Roi dummyRoi = new Roi(x + 2 * sizeCrossHair_, y - 2
					* sizeCrossHair_, 0, 0);
			dummyRoi.setStrokeColor(new Color(0,0,0,0));
			dummyRoi.setName(item.getMsg());
			overlay.add(dummyRoi);

			
			Roi roi = new Roi(x - beanRadius , y -beanRadius, beanRadius*2, beanRadius*2);
			roi.setStrokeColor(new Color(0, 0, 0, 0));
			roi.setName("");
			roi.setStrokeColor(item.getItemColor());
			overlay.add(roi);

		}
		overlay.setLabelColor(labelColor_);
		overlay.setLabelFont(labelFont_);
		overlay.drawNames(true);
		image.setOverlay(overlay);

		// }
		// 重绘
		if (update)
			image.updateAndDraw();
		else{
			HashMap<Long, Overlay> overlayList = overlayMap_.get(image);
			if (overlayList == null) {
				overlayList = new HashMap<Long, Overlay>();
				overlayMap_.put(image, overlayList);
			}
			overlayList.put(Long.valueOf(frameNumber), overlay);
		}
	}

	private HashMap<ImagePlus, HashMap<Long, Overlay>> overlayMap_;



	public Color getLabelColor() {
		return labelColor_;
	}

	public void setLabelColor(Color clr) {
		labelColor_ = clr;
	}

	public Font getLabelFont() {
		return labelFont_;
	}

	public void setLabelFont(Font font) {
		labelFont_ = font;
	}

	
}
