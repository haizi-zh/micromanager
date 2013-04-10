import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ij.IJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.Overlay;
import ij.gui.TextRoi;

public class MBTrackerListener implements ImageListener {

	@Override
	public void imageClosed(ImagePlus arg0) {
		resMap_.remove(arg0);
	}

	@Override
	public void imageOpened(ImagePlus arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void imageUpdated(ImagePlus image) {
		List<Point2D> track = resMap_.get(image);
		if (track == null || track.size() != image.getNSlices())
			return;
		Point2D pt = track.get(image.getCurrentSlice() - 1);
		Overlay ol = new Overlay();
		ol.add(new Line(0, pt.getY(), image.getWidth(), pt.getY()));
		ol.add(new Line(pt.getX(), 0, pt.getX(), image.getHeight()));
		TextRoi textRoi = new TextRoi(pt.getX() + 5, pt.getY() + 5, String.format(
				"(%.2f,  %.2f)", pt.getX(), pt.getY()));
		Font font = textRoi.getCurrentFont();
		textRoi.setCurrentFont(new Font(font.getName(), font.getStyle(), 6));
		ol.add(textRoi);
		ol.setStrokeColor(Color.RED);		
		image.setOverlay(ol);

//		IJ.log(String.format("Slice: %d", image.getCurrentSlice()));
	}

	/**
	 * ImagePlus和分析结果的映射表。
	 */
	private Map<ImagePlus, List<Point2D>> resMap_ = new HashMap<ImagePlus, List<Point2D>>();

	public void addTrackingResult(ImagePlus img, List<Point2D> trackingList) {
		resMap_.put(img, trackingList);
	}

}
