package org.ndaguan.study;

import ij.ImageListener;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.ShapeRoi;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.geom.PathIterator;
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
public class NongOverlayRender {
	
	/**
	 * 每个Item，代表追踪到的一个目标，包括：定为圆心的十字叉，以及一个text层， 显示相关的信息，比如坐标：(x, y)，亮度，等等。
	 * 
	 * @author Zephyre
	 * 
	 */
	public static class RenderItem {
		private Point loc_;
		private String msg_;
		private int radius_;
		public Color itemColor_;
		public int sizeCrossHair_;
		
		private RenderItem(int radius,Point loc, String msg) {
			loc_ = loc;
			msg_ = msg;
			radius_ = radius;			
			itemColor_ = Color.RED;			
			sizeCrossHair_ = 8;
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
		public static RenderItem createInstance(int radius,Point loc, String msg) {
			
			return new RenderItem(radius,loc, msg);
		}
		/**
		 * 圆心十字叉丝的大小。
		 * 
		 * @param size
		 */
		public void setCrossHairSize(int size) {
			sizeCrossHair_ = size;
		}
		public Color getItemColor() {
			return itemColor_;
		}

		public void setItemColor(Color clr) {
			itemColor_ = clr;
		}

	

	
	}

	protected ScriptInterface gui_;
	
	private static NongOverlayRender instance_;
	public Font labelFont_;
	public Color labelColor_;
	/**
	 * 
	 * @param gui
	 *            MMStudioMainFrame的实例。
	 */
	private NongOverlayRender(ScriptInterface gui) {
		gui_ = gui;
		labelColor_ = Color.RED;
		labelFont_ = new Font("SansSerif", Font.PLAIN, 14);
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

	public static NongOverlayRender getInstance(ScriptInterface gui) {
		if (instance_ == null)
			instance_ = new NongOverlayRender(gui);
		return instance_;
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
			int x = item.loc_.x;
			int y = item.loc_.y;
			int radius = item.radius_;
			//叉丝
			ShapeRoi sr = new ShapeRoi(new float[] { PathIterator.SEG_MOVETO,
					x - item.sizeCrossHair_, y, PathIterator.SEG_LINETO,
					x + item.sizeCrossHair_, y, PathIterator.SEG_MOVETO, x,
					y - item.sizeCrossHair_, PathIterator.SEG_LINETO, x,
					y + item.sizeCrossHair_, PathIterator.SEG_CLOSE });
			sr.setStrokeColor(item.itemColor_);
			sr.setName("");
			overlay.add(sr);
			
			//框
			Roi roi_ = new Roi(x-radius,y-radius,radius*2,radius*2);
			roi_.setStrokeColor(item.itemColor_);
			roi_.setName("");
			overlay.add(roi_);
			
			Roi dummyRoi = new Roi(x + 2 * item.sizeCrossHair_, y - 2
					* item.sizeCrossHair_, 1, 1);
			dummyRoi.setName(item.msg_);
			overlay.add(dummyRoi);
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
	
	public Font getLabelFont() {
		return labelFont_;
	}

	public void setLabelFont(Font font) {
		labelFont_ = font;
	}
	public Color getLabelColor() {
		return labelColor_;
	}

	public void setLabelColor(Color clr) {
		labelColor_ = clr;
	}
	
}
