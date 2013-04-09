package org.zephyre.micromanager.ZIndexMeasure;

import ij.ImageListener;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.TextRoi;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.micromanager.api.ScriptInterface;
import org.micromanager.utils.MMScriptException;

/**
 * 在相应的图像采集显示窗口中，设置overlay，显示相关信息。
 * 
 * @author Zephyre
 * 
 */
public class OverlayRender {
	/**
	 * 每个Item，代表追踪到的一个目标，包括：定为圆心的十字叉，以及一个text层， 显示相关的信息，比如坐标：(x, y)，亮度，等等。
	 * 
	 * @author Zephyre
	 * 
	 */
	public static class RenderItem {
		Point2D.Float loc_;
		Rectangle roi_;
		String msg_;

		private RenderItem(Point2D.Float loc, String msg) {
			loc_ = loc;
			msg_ = msg;
		}

		private RenderItem(Point2D.Float loc, Rectangle2D.Float roi) {

		}

		/**
		 * 创建实例。
		 * 
		 * @param loc
		 *            十字叉的坐标，原点为整个图像的左上角。
		 * @param msg
		 *            需要显式的信息。
		 * @return
		 */
		public static RenderItem createInstance(Point2D.Float loc, String msg) {
			return new RenderItem(loc, msg);
		}
	}

	protected ScriptInterface gui_;
	private Color itemColor_;
	private Color labelColor_;
	private Font labelFont_;
	private volatile int sizeCrossHair_;
	private static OverlayRender instance_;

	/**
	 * 
	 * @param gui
	 *            MMStudioMainFrame的实例。
	 */
	private OverlayRender(ScriptInterface gui) {
		gui_ = gui;
		itemColor_ = Color.RED;
		labelColor_ = Color.RED;
		labelFont_ = new Font("SansSerif", Font.PLAIN, 14);
		sizeCrossHair_ = 8;
		overlayMap_ = new HashMap<ImagePlus, HashMap<Long, Overlay>>();
		instance_ = this;

		// 更新overlay的回调函数
		ImagePlus.addImageListener(new ImageListener() {
			@Override
			public void imageClosed(ImagePlus image) {
				overlayMap_.remove(image);
			}

			@Override
			public void imageOpened(ImagePlus arg0) {
			}

			@Override
			public void imageUpdated(ImagePlus image) {
				// 如果正在采集，强行update会打乱显示。
				if (gui_.isAcquisitionRunning())
					return;

				HashMap<Long, Overlay> map = overlayMap_.get(image);
				if (map == null)
					return;
				int slice = image.getCurrentSlice();
				// zero-based
				Overlay overlay = map.get(Long.valueOf(slice - 1));
				image.setOverlay(overlay);
			}

		});
	}

	public static OverlayRender getInstance(ScriptInterface gui) {
		if (instance_ == null)
			instance_ = new OverlayRender(gui);
		return instance_;
	}

	/**
	 * 圆心十字叉丝的大小。
	 * 
	 * @param size
	 */
	public void setCrossHairSize(int size) {
		sizeCrossHair_ = size;
	}

	public ScriptInterface getMainFrame() {
		return gui_;
	}

	/**
	 * 叠加一个渲染图层。
	 * 
	 * @param acqName
	 *            MMAcquisition的名字，用于指定渲染的目标窗口。
	 * @param itemList
	 *            渲染目标的集合。
	 * @param frameNumber
	 * @param update
	 *            是否立刻重绘图像。
	 * @throws MMScriptException
	 *             acqName指定的MMAcquisition不存在。
	 */
	public void render(String acqName, Collection<RenderItem> itemList,
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
	public void render(final ImagePlus image, Collection<RenderItem> itemList,
			long frameNumber, boolean update) {
		if (image == null || itemList == null || itemList.size() == 0)
			return;

		// 新建Overlay
		Overlay overlay = new Overlay();

		// 清空原有的Roi，新建。
		Iterator<RenderItem> it = itemList.iterator();
		while (it.hasNext()) {
			RenderItem item = it.next();

			if (item.loc_ != null) {
				float x = item.loc_.x;
				float y = item.loc_.y;
				ShapeRoi sr = new ShapeRoi(new float[] {
						PathIterator.SEG_MOVETO, x - sizeCrossHair_, y,
						PathIterator.SEG_LINETO, x + sizeCrossHair_, y,
						PathIterator.SEG_MOVETO, x, y - sizeCrossHair_,
						PathIterator.SEG_LINETO, x, y + sizeCrossHair_,
						PathIterator.SEG_CLOSE });
				sr.setStrokeColor(itemColor_);
				sr.setName("");
				overlay.add(sr);

				// dummyRoi is for displaying the message
				Roi dummyRoi = new Roi(x + 2 * sizeCrossHair_, y - 2
						* sizeCrossHair_, 1, 1);
				dummyRoi.setStrokeColor(new Color(0, 0, 0, 0));
				dummyRoi.setName(item.msg_);
				overlay.add(dummyRoi);
			}

			if (item.roi_ != null) {
				ShapeRoi srRoi = new ShapeRoi(item.roi_);
				srRoi.setStrokeColor(itemColor_);
				srRoi.setName("");
				overlay.add(srRoi);
			}
		}
		overlay.setLabelColor(labelColor_);
		overlay.setLabelFont(labelFont_);
		overlay.drawNames(true);
		image.setOverlay(overlay);

		HashMap<Long, Overlay> overlayList = overlayMap_.get(image);
		if (overlayList == null) {
			overlayList = new HashMap<Long, Overlay>();
			overlayMap_.put(image, overlayList);
		}
		overlayList.put(Long.valueOf(frameNumber), overlay);

		// }
		// 重绘
		if (update)
			image.updateAndDraw();
	}

	private HashMap<ImagePlus, HashMap<Long, Overlay>> overlayMap_;

	public Color getItemColor() {
		return itemColor_;
	}

	public void setItemColor(Color clr) {
		itemColor_ = clr;
	}

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
