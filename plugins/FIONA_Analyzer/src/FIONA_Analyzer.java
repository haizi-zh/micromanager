import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import ij.*;
import ij.measure.ResultsTable;
import ij.process.*;
import ij.plugin.*;
import ij.gui.*;

import com.mathworks.toolbox.javabuilder.MWClassID;
import com.mathworks.toolbox.javabuilder.MWNumericArray;
import com.zephyre.FIONA.*;

public class FIONA_Analyzer implements PlugIn, ImageListener {
	private ImagePlus image;
	private ImageProcessor ip;

	// For overlays
	private double[] xPos, yPos, rInfo;
	private boolean[] hasPosInfo;

	@Override
	public void run(String arg0) {
		image = WindowManager.getCurrentImage();
		if (image == null) {
			IJ.showMessage("No image opened.");
			return;
		}
		ip = image.getProcessor();
		if (ip == null)
			return;
		Rectangle roi = ip.getRoi();
		Rectangle oriRoi = roi;
		if (roi == null) {
			roi = new Rectangle(0, 0, ip.getWidth(), ip.getHeight());
			image.setRoi(roi);
		}

		int stackSize = image.getStackSize();

		xPos = new double[stackSize];
		yPos = new double[stackSize];
		rInfo = new double[stackSize];
		hasPosInfo = new boolean[stackSize];
		for (int i = 0; i < hasPosInfo.length; ++i)
			hasPosInfo[i] = false;

		// Acquire parameters
		double radius = 2;
		double xStart = roi.x + roi.width / 2.0;
		double yStart = roi.y + roi.height / 2.0;
		int[][] tmpPix = ip.getIntArray();
		double background = (tmpPix[0][0]
				+ tmpPix[(int) (roi.x + roi.width - 1)][0]
				+ tmpPix[0][(int) (roi.y + roi.height - 1)] + tmpPix[(int) (roi.x
				+ roi.width - 1)][(int) (roi.y + roi.height - 1)]) / 4.0;
		double intensity = tmpPix[(int) xStart][(int) yStart] - background;

		boolean errorEst = false;
		boolean emGainSwitch = false;
		double bkgNoise = 5;
		double overallGain = 1;
		double startFrame = image.getCurrentSlice();
		double endFrame = stackSize;
		double sumRadius = (int) (roi.width / 3);
		boolean robust = false;

		GenericDialog gd = new GenericDialog("Parameters");
		gd.addMessage("(c) Copyright SM4, Institute of Physics, CAS");
		gd.addMessage("Author: Haizi Zheng");
		gd.addMessage("------------------------");
		gd.addNumericField("From", startFrame, 0);
		gd.addNumericField("To", endFrame, 0);
		gd.addNumericField("Background", background, 2);
		gd.addNumericField("Intensity", intensity, 2);
		gd.addNumericField("Radius", radius, 2);
		gd.addNumericField("X", xStart, 2);
		gd.addNumericField("Y", yStart, 2);
		gd.addNumericField("Particle Size", sumRadius, 0);
		gd.addMessage("------------------------");
		gd.addCheckbox("Use weighted errors", errorEst);
		gd.addNumericField("Background Noise", bkgNoise, 2);
		gd.addCheckbox("EMGain ON", emGainSwitch);
		gd.addNumericField("Overall gain (counts/e-)", overallGain, 2);
		gd.addCheckbox("Robust", robust);

		gd.showDialog();
		if (gd.wasCanceled())
			return;
		else {
			double temp1 = gd.getNextNumber();
			double temp2 = gd.getNextNumber();
			if (temp1 >= 1 && temp2 <= stackSize && temp1 <= temp2) {
				startFrame = temp1;
				endFrame = temp2;
			}
			background = gd.getNextNumber();
			intensity = gd.getNextNumber();
			radius = gd.getNextNumber();
			xStart = gd.getNextNumber();
			yStart = gd.getNextNumber();
			sumRadius = (int) (gd.getNextNumber());
			errorEst = gd.getNextBoolean();
			bkgNoise = gd.getNextNumber();
			emGainSwitch = gd.getNextBoolean();
			overallGain = gd.getNextNumber();
			robust = gd.getNextBoolean();
		}

		ImagePlus.removeImageListener(this);

		FIONA f = null;
		MWNumericArray startPointMW = null;
		MWNumericArray roiDataMW = null;
		MWNumericArray imageMW = null;
		MWNumericArray weightsMW = null;
		Object[] ret = null;
		try {
			ResultsTable rt = new ResultsTable();
			rt.reset();
			rt.setPrecision(5);

			f = new FIONA();
			for (int i = (int) startFrame; i <= (int) endFrame; i++) {
				image.setSlice(i);
				IJ.showProgress((i - startFrame + 1)
						/ (endFrame - startFrame + 1));
				IJ.showStatus(String.format("%d/%d",
						(int) (i - startFrame + 1), (int) (endFrame
								- startFrame + 1)));
				// Prepare data
				int[][] pix = ip.getIntArray();
				double[][] weights = new double[roi.height][roi.width];
				double[][] data = new double[roi.height][roi.width];

				// For centroid localization
				int Ntot = 0;
				double xCentroid = 0;
				double yCentroid = 0;

				for (int j = roi.y; j < roi.y + roi.height; j++) {
					for (int k = roi.x; k < roi.x + roi.width; k++) {
						int val = pix[k][j];
						data[j - roi.y][k - roi.x] = val;

						Ntot += (val - background);
						xCentroid += (val - background) * k;
						yCentroid += (val - background) * j;

						if (!errorEst) {
							weights[j - roi.y][k - roi.x] = 1.0;
						} else {
							double factor;
							if (emGainSwitch) {
								factor = 2;
							} else {
								factor = 1;
							}
							weights[j - roi.y][k - roi.x] = Math.sqrt(Math.pow(
									bkgNoise, 2)
									+ factor
									* overallGain
									* (val - background));
							// Math.pow(factor * preAmp / preAmpDenomenator *
							// overallGain * (val - background));
						}
					}
				}

				// Centroid
				xCentroid /= Ntot;
				yCentroid /= Ntot;

				double[][] startPoint = new double[][] { { background,
						intensity, radius, xCentroid, yCentroid } };
				double[][] roiData = new double[][] { { roi.x, roi.y } };
				startPointMW = new MWNumericArray(startPoint, MWClassID.DOUBLE);
				roiDataMW = new MWNumericArray(roiData, MWClassID.DOUBLE);
				imageMW = new MWNumericArray(data, MWClassID.DOUBLE);
				weightsMW = new MWNumericArray(weights, MWClassID.DOUBLE);

				ret = f.FIONA(1, imageMW, weightsMW, false, startPointMW,
						roiDataMW);
				double[] result = ((MWNumericArray) (ret[0])).getDoubleData();

				int xCenter = (int) result[3];
				int yCenter = (int) result[4];
				int tempR = (int) sumRadius;
				int intSum = 0;
				try {
					for (int j = yCenter - tempR; j <= yCenter + tempR; j++) {
						for (int k = xCenter - tempR; k <= xCenter + tempR; k++) {
							intSum = pix[k][j] + intSum;
						}
					}
				} catch (Exception e) {
					intSum = 0;
				}

				rt.incrementCounter();
				rt.addValue("Frame", i);
				rt.addValue("Area", Math.pow((2 * sumRadius + 1), 2));
				rt.addValue("Background", result[0]);
				rt.addValue("Intensity", result[1]);
				rt.addValue("Radius", result[2]);
				rt.addValue("Intensity FIOINA",
						result[1] * 3.14 * Math.pow(result[2], 2));
				rt.addValue("Intensity Sum", intSum);
				rt.addValue("X", result[3]);
				rt.addValue("Y", result[4]);
				rt.addValue("gof-sse", result[5]);
				rt.addValue("gof-rsquare", result[6]);
				rt.addValue("gof-dfe", result[7]);
				rt.addValue("gof-adjrsquare", result[8]);
				rt.addValue("gof-rmse", result[9]);

				if (result[8] < 0.7)
					return;

				// If the gof is above the threshold of 0.7, update the new
				// start points and ROI.
				xPos[i - 1] = result[3];
				yPos[i - 1] = result[4];
				rInfo[i - 1] = result[2];
				hasPosInfo[i - 1] = true;

				background = result[0];
				intensity = result[1];
				radius = result[2];
				xStart = result[3];
				yStart = result[4];
				// Set the roi
				int w = oriRoi.width;
				int h = oriRoi.height;
				roi.x = (int) (xStart - w / 2.0);
				roi.width = w;
				roi.y = (int) (yStart - h / 2.0);
				roi.height = h;
				if (roi.x < 0)
					roi.x = 0;
				if (roi.y < 0)
					roi.y = 0;
				if (roi.x + w > ip.getWidth()) {
					roi.x = ip.getWidth() - w;
					if (roi.x < 0)
						roi.x = 0;
				}
				if (roi.y + h > ip.getHeight()) {
					roi.y = ip.getHeight() - h;
					if (roi.y < 0)
						roi.y = 0;
				}

				if ((i - startFrame) % 100 == 0) {
					image.setRoi(roi);
					rt.show("FIONA Tracking Results");
				}
			}
			ImagePlus.addImageListener(this);
			image.setSlice((int) startFrame);
			image.killRoi();
			rt.show("FIONA Tracking Results");
		} catch (Exception e) {
			IJ.log(e.toString());
			IJ.showMessage("ERROR");
		} finally {
			MWNumericArray.disposeArray(startPointMW);
			MWNumericArray.disposeArray(roiDataMW);
			MWNumericArray.disposeArray(imageMW);
			MWNumericArray.disposeArray(weightsMW);
			MWNumericArray.disposeArray(ret);
			if (f != null) {
				f.dispose();
				f = null;
			}
		}
	}

	private void drawMarker(double xc, double yc, double d, double r, Color clr) {
		GeneralPath path = new GeneralPath();
		xc = xc + 0.5;
		yc = yc + 0.5;

		// draw the cross
		path.moveTo(xc - d / 2, yc);
		path.lineTo(xc + d / 2, yc);
		path.moveTo(xc, yc - d / 2);
		path.lineTo(xc, yc + d / 2);

		// draw the rectangle
		path.moveTo(xc - 3 * r, yc - 2 * r);
		path.lineTo(xc - 3 * r, yc - 3 * r);
		path.lineTo(xc - 2 * r, yc - 3 * r);
		path.moveTo(xc + 2 * r, yc - 3 * r);
		path.lineTo(xc + 3 * r, yc - 3 * r);
		path.lineTo(xc + 3 * r, yc - 2 * r);
		path.moveTo(xc + 3 * r, yc + 2 * r);
		path.lineTo(xc + 3 * r, yc + 3 * r);
		path.lineTo(xc + 2 * r, yc + 3 * r);
		path.moveTo(xc - 2 * r, yc + 3 * r);
		path.lineTo(xc - 3 * r, yc + 3 * r);
		path.lineTo(xc - 3 * r, yc + 2 * r);

		image.setOverlay(path, clr, null);
	}

	// called when an image is opened
	public void imageOpened(ImagePlus imp) {
	}

	// Called when an image is closed
	public void imageClosed(ImagePlus imp) {
		ImagePlus.removeImageListener(this);
	}

	// Called when an image's pixel data is updated
	public void imageUpdated(ImagePlus imp) {
		int stackIndex = image.getCurrentSlice();
		if (!hasPosInfo[stackIndex - 1]) {
			image.setOverlay(null);
			return;
		}

		try {
			drawMarker(xPos[stackIndex - 1], yPos[stackIndex - 1],
					ip.getWidth() / 16.0, rInfo[stackIndex - 1], Color.red);
		} catch (Exception e) {
			IJ.log(e.toString());
		}
	}
}
