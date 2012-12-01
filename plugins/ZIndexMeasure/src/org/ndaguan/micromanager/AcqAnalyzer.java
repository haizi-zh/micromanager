package org.ndaguan.micromanager;

import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;

import javax.swing.SwingUtilities;

import mmcorej.TaggedImage;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.solvers.LaguerreSolver;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.json.JSONException;
import org.micromanager.MMStudioMainFrame;
import org.micromanager.acquisition.TaggedImageQueue;
import org.micromanager.api.ScriptInterface;
import org.micromanager.api.TaggedImageAnalyzer;
import org.micromanager.utils.MMScriptException;
import org.zephyre.micromanager.OverlayRender;
import org.zephyre.micromanager.OverlayRender.RenderItem;

public class AcqAnalyzer extends TaggedImageAnalyzer {
	private ZIndexMeasure main;
	private MyGUI myGUI_;
	private ScriptInterface mainWnd_;
	private String baseDir_;
	private static AcqAnalyzer instance_;
	private double persistance_;
	private double beadRadius_;
	private double kT_;
	private double contourLength_;
	private int windowSize_;
	private double[] pixelToPhys_;

	public void setPixelToPhys(double[] transform) {
		pixelToPhys_ = transform;
	}

	public void setBaseDir(String path) {
		baseDir_ = path;
	}

	private Writer dataFileWriter_;

	protected AcqAnalyzer(ScriptInterface gui, ZIndexMeasure main_, MyGUI mygui_) {
		myGUI_ = mygui_;
		main = main_;
		mainWnd_ = gui;
		render_ = OverlayRender.getInstance(gui);
		stats_ = new DescriptiveStatistics[3];
		windowSize_ = 1000;
		for (int i = 0; i < stats_.length; i++) {
			stats_[i] = new DescriptiveStatistics(windowSize_);
		}
		statCross_ = new DescriptiveStatistics(windowSize_);
		persistance_ = 50;
		beadRadius_ = 1400;
		kT_ = 4.2;
		contourLength_ = 16700;
	}

	public static AcqAnalyzer getInstance(ScriptInterface gui,
			ZIndexMeasure main_, MyGUI mygui_) {
		if (instance_ == null)
			instance_ = new AcqAnalyzer(gui, main_, mygui_);
		return instance_;
	}

	public boolean resetData_;
	private OverlayRender render_;
	private DescriptiveStatistics[] stats_;
	private DescriptiveStatistics statCross_;

	@Override
	protected void analyze(final TaggedImage taggedImage) {
		// Retrieving a POISON image indicates that current acquisition is
		// completed or has been canceled.
		if (taggedImage == TaggedImageQueue.POISON) {
			if (dataFileWriter_ != null) {
				try {
					dataFileWriter_.close();
					dataFileWriter_ = null;
					resetData_ = true;
				} catch (IOException e) {
					mainWnd_.logError(e);
				}
			}
		}

		if (taggedImage == null || taggedImage == TaggedImageQueue.POISON
				|| !main.isCalibrated)
			return;

		// myGUI_.start();
		double pos[] = null;
		try {
			pos = GetPosition(myGUI_.currFrame, taggedImage);
		} catch (IOException | JSONException e) {
			mainWnd_.logError(e);
			return;
		}
		myGUI_.currFrame++;
		// myGUI_.end(String.format("%d,#JAVA Cost Time",myGUI_.currFrame));

		// Render the overlay
		String acqName;
		long index;
		try {
			if (!taggedImage.tags.has("FrameIndex"))
				index = 0;
			else
				index = taggedImage.tags.getLong("FrameIndex");
			acqName = taggedImage.tags.getString("AcqName");
		} catch (JSONException e) {
			e.printStackTrace();
			return;
		}

		ArrayList<RenderItem> list = new ArrayList<OverlayRender.RenderItem>();
		list.add(RenderItem.createInstance(new Point2D.Float((float) pos[0],
				(float) pos[1]), String.format("(%f, %f, %f)", pos[0], pos[1],
				pos[2])));
		boolean update = acqName.equals(MMStudioMainFrame.SIMPLE_ACQ) ? true
				: false;
		try {
			render_.render(acqName, list, index, update);
		} catch (MMScriptException e) {
		}
	}

	public double[] GetPosition(final int index_, TaggedImage taggedImage)
			throws IOException, JSONException {
		Object[] dpos = main.mCalc.GetZPosition(taggedImage.pix,
				myGUI_.calcRoi_, index_);
		// double[] time = (double[]) dpos[1];
		// myGUI_.log(String.format("C Cost Time#%f", time[1]));
		final double pos[] = (double[]) dpos[0];

		final boolean clearChart = resetData_;

		// Reset the stat container
		if (resetData_) {
			for (DescriptiveStatistics stat : stats_)
				stat.clear();
			statCross_.clear();
			resetData_ = false;
		}
		double xPhys = pixelToPhys_[0] + pixelToPhys_[1] * pos[0];
		double yPhys = pixelToPhys_[2] + pixelToPhys_[3] * pos[1];
		stats_[0].addValue(xPhys);
		stats_[1].addValue(yPhys);
		stats_[2].addValue(pos[2]);
		statCross_.addValue(xPhys * yPhys);
		// Calculate forces
		final double[] forces = calcForces();
		double[] skrewness = calcSkrewness();

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (clearChart) {
					myGUI_.dataSeries_.clear();
				}

				myGUI_.dataSeries_.add(index_, pos[2]);

				if (pos[11] != 0) {
					double p = Math.pow(10, 2);
					double center = Math.round(pos[11] * p) / p;
					myGUI_.chart.getXYPlot().getRangeAxis()
							.setRange(center - 0.06, center + 0.06);
				}
				myGUI_.dataSeries_.add(index_, pos[2]);
				myGUI_.Msg0.setText(String
						.format("index = %d # xpos = %f # ypos = %f # zpos = %f # forceX/pN=%f # forceY/pN=%f",
								index_, pos[0], pos[1], pos[2], forces[0],
								forces[1]));
				myGUI_.Msg1.setText(String
						.format("# <stdx> = %f # <stdy> = %f # <stdz> = %f # meanx = %f # meany = %f # meanz = %f",
								pos[6], pos[7], pos[8], pos[9], pos[10],
								pos[11]));
				myGUI_.reSetROI((int) pos[0], (int) pos[1]);

			}
		});

		String acqName = (String) taggedImage.tags.get("AcqName");
		String nameComp = "";
		if (acqName.equals(MMStudioMainFrame.SIMPLE_ACQ))
			nameComp = "Live";
		else
			nameComp = acqName;

		if (dataFileWriter_ == null) {
			// Build the path
			Calendar cal = new GregorianCalendar();
			DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
			File dir = new File(new File(baseDir_, "ZIndexMeasure"),
					dateFormat.format(cal.getTime()));
			dir.mkdirs();

			dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
			File file = new File(dir, dateFormat.format(cal.getTime()) + "_"
					+ nameComp + ".txt");
			dataFileWriter_ = new BufferedWriter(new FileWriter(file));
			dataFileWriter_
					.write("Frame, Timestamp, XPos/pixel, YPos/pixel, ZPos/uM,<StdXPos>/nM,<StdYPos>/nM,<StdZPos>/nM,meanX/pixel,meanY/pixel,meanZ/pixel,ForceX/pN,ForceY/pN\r\n");
		}

		dataFileWriter_.write(String.format(
				"%d,%s,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f\r\n", index_,
				taggedImage.tags.get("ElapsedTime-ms"), xPhys, yPhys, pos[2],
				pos[6], pos[7], pos[8], pos[9], pos[10], pos[11], forces[0],
				forces[1]));

		if (index_ % myGUI_.FrameCalcForce_ == 0 && myGUI_.F_L_Flag_ == 1) {
			main.PullMagnet();
		}

		return pos;
	}

	private double[] calcSkrewness() {
		double[] skrewness = new double[2];

		double[] stds = new double[2];
		for (int i = 0; i < stds.length; i++)
			stds[i] = stats_[i].getStandardDeviation();

		double n = statCross_.getN();
		skrewness[0] = stds[0] / stds[1];
		skrewness[1] = (statCross_.getMean() - stats_[0].getMean()
				* stats_[1].getMean())
				* n / (n - 1) / (stds[0] * stds[1]);
		return null;
	}

	private double[] calcForces() {
		LaguerreSolver solver = new LaguerreSolver();
		double[] forces = new double[2];
		for (int i = 0; i < forces.length; i++) {
			double variance = stats_[i].getVariance();
			double a = 4 * persistance_ * contourLength_ / variance;
			double b = 4 * persistance_ * beadRadius_ / variance;
			PolynomialFunction func = new PolynomialFunction(new double[] { b,
					a - 2 * b - 6, b - 2 * a + 9, a - 4 });
			forces[i] = (solver.solve(100, func, 0, 1, 0.8) * a + b) * kT_
					/ (4 * persistance_);
		}
		return forces;
	}
}