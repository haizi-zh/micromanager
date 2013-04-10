import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.process.ImageStatistics;

public class MBTracker_ implements PlugIn {

//	public void run2(String arg0) {
//		final URLClassLoader loader = (URLClassLoader) getClass()
//				.getClassLoader();
//		try {
//			Class<?> cls = (Executors.newFixedThreadPool(1)
//					.submit(new Callable<Class<?>>() {
//						@Override
//						public Class<?> call() throws Exception {
//							Thread.currentThread()
//									.setContextClassLoader(loader);
//							return Class.forName("org.zephyre.MBTracker");
//						}
//					})).get();
//			((Runnable) (cls.newInstance())).run();
//		} catch (InterruptedException | ExecutionException
//				| InstantiationException | IllegalAccessException
//				| SecurityException | IllegalArgumentException e) {
//			IJ.log(e.toString());
//		}
//	}

	private static MBTrackerListener listener_;
	static {
		listener_ = new MBTrackerListener();
		ImagePlus.addImageListener(listener_);
	}

	@Override
	public void run(String arg0) {
		ImagePlus image = WindowManager.getCurrentImage();
		if (image == null) {
			IJ.showMessage("Error", "No image opened.");
			return;
		}

		GenericDialog gd = new GenericDialog("Magnetic Beads Tracker");
		gd.addMessage("Powered by Haizi Zheng, 2013");
		gd.addMessage("");
		gd.addNumericField("Threshold: ", 0, 0);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		int threshold = (int) gd.getNextNumber();

		Random rand = new Random();
		List<Point2D> track = new ArrayList<Point2D>();

		Roi roi = image.getRoi();
		Rectangle roiRc;
		if (roi != null)
			roiRc = roi.getBounds();
		else
			roiRc = new Rectangle(image.getWidth(), image.getHeight());
		int rcW = roiRc.width;
		int rcH = roiRc.height;

		for (int i = 0; i < image.getNSlices(); i++) {
			image.setSlice(i + 1);
			Point2D trackPt = performTracking(image, roiRc, threshold);
			track.add(trackPt);

			int x = (int) (trackPt.getX() - 0.5 * rcW);
			int y = (int) (trackPt.getY() - 0.5 * rcH);
			if (x < 0)
				x = 0;
			else if (x + rcW >= image.getWidth())
				x = image.getWidth() - rcW;
			if (y < 0)
				y = 0;
			else if (y + rcH >= image.getHeight())
				y = image.getHeight() - rcH;
			roiRc = new Rectangle(x, y, rcW, rcH);
		}

		renderResultsTable(track);
		listener_.addTrackingResult(image, track);
		listener_.imageUpdated(image);
		image.updateAndDraw();
		
		// ImagePlus.addImageListener(listener_);
	}

	private void renderResultsTable(List<Point2D> track) {
		ResultsTable table = new ResultsTable();
		for (Point2D pt:track){
			table.incrementCounter();
			table.addValue("X", pt.getX());
			table.addValue("Y", pt.getY());
		}
		table.show("Results");
	}

	private static Point2D performTracking(ImagePlus image, Rectangle roiRc,
			int threshold) {
		short[] data = (short[]) image.getProcessor().getPixels();
		int width = image.getWidth();

		double sumx = 0, sumy = 0, sum = 0;
		for (int i = roiRc.y; i < roiRc.y + roiRc.height; i++) {
			for (int j = roiRc.x; j < roiRc.x + roiRc.height; j++) {
				short val = data[i * width + j];
				if (val < threshold)
					val = 0;
				sumx += val * j;
				sumy += val * i;
				sum += val;
			}
		}
		return new Point2D.Double(sumx / sum, sumy / sum);
	}
}
