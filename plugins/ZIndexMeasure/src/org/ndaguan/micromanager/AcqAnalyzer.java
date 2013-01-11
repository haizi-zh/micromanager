package org.ndaguan.micromanager;

import ij.IJ;

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
import org.apache.commons.math3.exception.NoBracketingException;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.json.JSONException;
import org.micromanager.MMStudioMainFrame;
import org.micromanager.acquisition.TaggedImageQueue;
import org.micromanager.api.ScriptInterface;
import org.micromanager.api.TaggedImageAnalyzer;
import org.micromanager.utils.MMScriptException;
import org.zephyre.micromanager.OverlayRender;
import org.zephyre.micromanager.OverlayRender.RenderItem;

public class AcqAnalyzer extends TaggedImageAnalyzer {
	private static final long minAnalyzeWindow = 100;
	private static final int DRAWWINDOW = 5000;
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
	private XYSeries zDataSeries_;
	private JFreeChart zDataChart;
	private XYSeries corrSeries_;
	private XYSeries xDataSeries_;
	private XYSeries yDataSeries_;
	private JFreeChart xDataChart;
	private JFreeChart yDataChart;
	private XYSeries fxDataSeries_;
	private XYSeries fyDataSeries_;

	protected AcqAnalyzer(ScriptInterface gui, ZIndexMeasure main_, MyGUI mygui_) {
		myGUI_ = mygui_;
		main = main_;
		mainWnd_ = gui;
		
		zDataSeries_ = myGUI_.myForm_.getDataSeries_().get("Chart-Z");
		xDataSeries_ = myGUI_.myForm_.getDataSeries_().get("Chart-X");
		yDataSeries_ = myGUI_.myForm_.getDataSeries_().get("Chart-Y");
		fxDataSeries_ = myGUI_.myForm_.getDataSeries_().get("Chart-FX");
		fyDataSeries_ = myGUI_.myForm_.getDataSeries_().get("Chart-FY");
		zDataChart = myGUI_.myForm_.getChartSeries_().get("Chart-Z");
		xDataChart = myGUI_.myForm_.getChartSeries_().get("Chart-X");
		yDataChart = myGUI_.myForm_.getChartSeries_().get("Chart-Y");
		corrSeries_ = mygui_.myForm_.getDataSeries_().get("Chart-Corr");
		
		render_ = OverlayRender.getInstance(gui);
		stats_ = new DescriptiveStatistics[3];
		windowSize_ = 1000;
		for (int i = 0; i < stats_.length; i++) {
			stats_[i] = new DescriptiveStatistics(windowSize_);
		}
		statCross_ = new DescriptiveStatistics(windowSize_);
		persistance_ = 50;
		beadRadius_ = mygui_.getRadius_()*1000;//1400;//
		kT_ = 4.2;
		contourLength_ = mygui_.getDNALen_()*1000;//16700;//
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
	private long startTs_;
	private int DRAW;
	long index = 0;
	private long frame=0;
	long timest =0;
	@Override
	protected void analyze(final TaggedImage taggedImage) {
		// Retrieving a POISON image indicates that current acquisition is
		// completed or has been canceled.
		timest = now();
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


		// Render the overlay
		String acqName;

		try {
			if (!taggedImage.tags.has("FrameIndex")){	
				index =0;
				frame++;
			}
			else{
				index = taggedImage.tags.getLong("FrameIndex");
				frame = index;
			}
			acqName = taggedImage.tags.getString("AcqName");
		} catch (JSONException e) {
			e.printStackTrace();
			return;
		}

		double pos[] = null;
		try {
			pos = GetPosition(frame, taggedImage);
		} catch (IOException | JSONException e) {
			mainWnd_.logError(e);
			return;
		}

		ArrayList<RenderItem> list = new ArrayList<OverlayRender.RenderItem>();
		list.add(RenderItem.createInstance(new Point2D.Float((float) pos[0],
				(float) pos[1]), String.format("(%.2f, %.2f, %.2f)(%.2f,%.2f)", pos[3],
						pos[4], pos[2],  pos[7], pos[8])));
		boolean update = acqName.equals(MMStudioMainFrame.SIMPLE_ACQ) ? true
				: false;
		try {
			render_.render(acqName, list, index, update);
		} catch (MMScriptException e) {
			mainWnd_.logError(e);
		}

		mainWnd_.logMessage(String.format("\t\t\t\t\t\t\t\t\tJava Cost#\t%.2f",(double)(( now() - timest)/1000000)));

	}

	private long now() {
		// TODO Auto-generated method stub
		return System.nanoTime();
	}

	public double[] GetPosition(final long index_, TaggedImage taggedImage)
			throws IOException, JSONException {
		//pos 0~5 x y z dx dy dz  6~11  <x2> <y2> <z2> <x> <y> <z>
		Object[] dpos = main.mCalc.GetZPosition(taggedImage.pix,
				myGUI_.calcRoi_, 0);
		final double pos[] = (double[]) dpos[0];
		mainWnd_.logMessage(String.format("\t\t\t\t\t\t\t\t\tC Cost##\t%.2f",((double[]) dpos[1])[1]));
		final double[] corrProfile = (double[]) dpos[2];

		if(myGUI_.myForm_.getCurrTab() == 3){
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					corrSeries_.clear();
					for(int i = 0;i<corrProfile.length;i++){
						corrSeries_.add(main.getCalPosZ_()[i], corrProfile[i]);
					}
				}
			});
		}
		final boolean clearChart = resetData_;
		// Reset the stat container
		if (resetData_) {
			for (DescriptiveStatistics stat : stats_)
				stat.clear();
			statCross_.clear();
			resetData_ = false;
			startTs_ = System.nanoTime();

		}
		final double xPhys = pixelToPhys_[0] + pixelToPhys_[1] * pos[0];
		final double yPhys = pixelToPhys_[2] + pixelToPhys_[3] * pos[1];

		stats_[0].addValue(xPhys * 1000);
		stats_[1].addValue(yPhys * 1000);
		stats_[2].addValue(pos[2]);
		statCross_.addValue(xPhys * yPhys * 1e6);
		// Calculate forces
		final double[] forces = calcForces();
		double[] skrewness = calcSkrewness();

		updateGUI(forces,pos,clearChart,(int)index_,xPhys,yPhys);

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
			.write("Frame, Timestamp, XPos/pixel, YPos/pixel, XPos/um, YPos/um, ZPos/um,<StdXPos>/nm,<StdYPos>/nm,<StdZPos>/nm,meanX/pixel,meanY/pixel,meanZ/pixel,ForceX/pN,ForceY/pN,skrewnessx,skrewnessy\r\n");
		}

		double elapsed;
		if (taggedImage.tags.has("ElapsedTime-ms"))
			elapsed = taggedImage.tags.getDouble("ElapsedTime-ms");
		else
			elapsed = (System.nanoTime() - startTs_) / 1e6;
		dataFileWriter_.write(String.format(
				"%d,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f\r\n", index_,
				elapsed, pos[0], pos[1], xPhys, yPhys, pos[2], pos[6], pos[7],
				pos[8], pos[9], pos[10], pos[11], forces[0], forces[1],skrewness[0],skrewness[1]));

		if (index_ % myGUI_.movingWindowLen_ == 0 && myGUI_.myForm_.isMagnetAuto()) {
			main.PullMagnet();
		}

		return new double[] { pos[0], pos[1], pos[2], xPhys, yPhys, forces[0], forces[1],skrewness[0],skrewness[1] };
	}

	private void updateGUI(final double[] forces, final double[] pos,final boolean clearChart,final int index_,final double xPhys,final double yPhys) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				myGUI_.reSetROI((int) pos[0], (int) pos[1]);//reset ROI
				if (clearChart) {
					zDataSeries_.clear(); 
					xDataSeries_.clear(); 
					yDataSeries_.clear(); 
					fxDataSeries_.clear(); 
					fyDataSeries_.clear(); 
					main.mp285StepCounter = 0;
				} 
				//ZPOS

				double center =  Math.floor(pos[11]*100)/100;//.2f
				double scale = 5*pos[8];
				if(scale<0.05)
					scale = 0.05;	

				zDataChart.getXYPlot().getRangeAxis().setRange(center - scale,center +  scale);				
				zDataSeries_.add(index_, pos[2]);	

				//XPOS
				double xphycenter = pixelToPhys_[0] + pixelToPhys_[1]*pos[9];
				center =  Math.floor(xphycenter*100)/100;
				scale = 2*pos[6];
				if(scale<0.1)
					scale = 0.1;	

				xDataChart.getXYPlot().getRangeAxis().setRange(center - scale,center +  scale);				
				xDataSeries_.add(index_,xPhys);	

				//YPOS

				double yphycenter = pixelToPhys_[2] + pixelToPhys_[3]*pos[10];
				center =  Math.floor(yphycenter*100)/100;
				scale = 2*pos[7];
				if(scale<0.1)
					scale = 0.1;		

				yDataChart.getXYPlot().getRangeAxis().setRange(center - scale,center +  scale);				
				yDataSeries_.add(index_, yPhys);

				//FX,FY

				fxDataSeries_.add(index_,forces[0]);
				fyDataSeries_.add(index_,forces[1]);

			}
		});

	}

	private double[] calcSkrewness() {
		double[] skrewness = new double[2];

		double[] stds = new double[2];
		for (int i = 0; i < stds.length; i++) {
			if (stats_[i].getN() < minAnalyzeWindow)
				continue;
			stds[i] = stats_[i].getStandardDeviation();
		}

		double n = statCross_.getN();
		skrewness[0] = stds[0] / stds[1];
		skrewness[1] = (statCross_.getMean() - stats_[0].getMean()
				* stats_[1].getMean())
				* n / (n - 1) / (stds[0] * stds[1]);
		return skrewness;
	}

	private double[] calcForces() {
		LaguerreSolver solver = new LaguerreSolver();
		double[] forces = new double[2];
		for (int i = 0; i < forces.length; i++) {
			if (stats_[i].getN() < minAnalyzeWindow)
				continue;
			double variance = stats_[i].getVariance();
			mainWnd_.logMessage(String.format("Variance: %f", variance));
			double a = 4 * persistance_ * contourLength_ / variance;
			double b = 4 * persistance_ * beadRadius_ / variance;
			PolynomialFunction func = new PolynomialFunction(new double[] { b,
					a - 2 * b - 6, b - 2 * a + 9, a - 4 });
			try {
				forces[i] = (solver.solve(100, func, 0, 1, 0.8) * a + b) * kT_
						/ (4 * persistance_);
			} catch (NoBracketingException e) {
				mainWnd_.logError(String.format("a=%f, b=%f, message: %s", a,
						b, e.toString()));
			} finally {
				mainWnd_.logMessage(String.format(
						"%s: a=%f, b=%f, variance=%f, force=%f", i == 0 ? "x"
								: "y", a, b, variance, forces[i]));
			}
		}
		return forces;
	}
}