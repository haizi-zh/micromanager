package org.zephyre.ccdgain;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.geom.Ellipse2D;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import mmcorej.CMMCore;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.Variance;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.micromanager.api.AcquisitionEngine;
import org.micromanager.api.MMPlugin;
import org.micromanager.api.ScriptInterface;
import org.micromanager.utils.MMScriptException;

public class CCDGainCalc implements MMPlugin {
	private ScriptInterface gui_;
	private CMMCore core_;
	private CCDGainCalcFrame frame_;
	private Thread calibrThrd_;

	// public static void main(String[] arg) {
	// CCDGainCalc obj = new CCDGainCalc();
	// obj.setApp(null);
	// }

	/*
	 * Start the CCD gain calibrating process
	 */
	synchronized void startCalibr(final CalibrSetting settings) {
		calibrThrd_ = new Thread(new Runnable() {
			@Override
			public void run() {
				int l = settings.nLevels;
				int s = settings.nSamples;
				final CCDGainCalcFrame frame = settings.frame_;
				AcquisitionEngine acq = gui_.getAcquisitionEngine();
				boolean saveFiles = acq.getSaveFiles();
				acq.setSaveFiles(false);
				gui_.closeAllAcquisitions();

				double[] lvlVals = new double[l * s];
				double[] noiseVals = new double[l * s];

				int tot = 0;

				for (int i = 0; i < l; i++) {
					try {
						gui_.runBurstAcquisition(2 * s);
						while (gui_.isAcquisitionRunning()) {
							try {
								TimeUnit.MILLISECONDS.sleep(100);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						List<List<Number>> data = analyzeStack();
						// gui_.closeAcquisitionWindow("Acq");
						// gui_.closeAcquisition("Acq");

						for (int j = 0; j < s; j++) {
							lvlVals[i * s + j] = data.get(0).get(j)
									.doubleValue();
							noiseVals[i * s + j] = data.get(1).get(j)
									.doubleValue();
						}
						tot += s;

					} catch (MMScriptException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					final AtomicReference<Integer> retCode = new AtomicReference<Integer>();
					final int index = i;
					Runnable dialog = new Runnable() {
						@Override
						public void run() {
							Object[] options = { "Continue", "Stop" };
							// TODO Auto-generated method stub
							retCode.set(JOptionPane.showOptionDialog(frame_,
									String.format(
											"#%d: Adjust the intensity level.",
											index), "Calibrating...",
									JOptionPane.OK_OPTION,
									JOptionPane.QUESTION_MESSAGE, null,
									options, options[0]));
						}
					};
					try {
						SwingUtilities.invokeAndWait(dialog);
					} catch (InvocationTargetException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					int n = retCode.get();
					if (n == -1 || n == 1)
						break;
					else if (n == 0)
						continue;
				}
				acq.setSaveFiles(saveFiles);

				// Linear fit
				plotData(Arrays.copyOfRange(lvlVals, 0, tot),
						Arrays.copyOfRange(noiseVals, 0, tot));

				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						frame.isCalibr = false;
						frame.calibrBtn.setText("Start");
						frame.setVisible(false);
					}
				});
			}
		});
		calibrThrd_.start();
	}

	private void plotData(final double[] lvlVals, final double[] noiseVals) {
		final double[][] data = new double[lvlVals.length][2];
		for (int i = 0; i < lvlVals.length; i++) {
			data[i][0] = lvlVals[i];
			data[i][1] = noiseVals[i];
		}
		SimpleRegression reg = new SimpleRegression();
		reg.addData(data);
		final double intercept = reg.getIntercept();
		final double slope = reg.getSlope();

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// Plot
				XYSeries s = new XYSeries(1);
				for (int i = 0; i < lvlVals.length; i++) {
					s.add(lvlVals[i], noiseVals[i]);
				}
				XYSeries p = new XYSeries("Plot");
				double[] sortedLvl = Arrays.copyOf(lvlVals, lvlVals.length);
				double x = sortedLvl[0];
				double y = intercept + slope * x;
				p.add(x, y);
				x = sortedLvl[sortedLvl.length - 1];
				y = intercept + slope * x;
				p.add(x, y);
				XYSeriesCollection dataset = new XYSeriesCollection(s);
				JFreeChart chart = ChartFactory.createScatterPlot(String
						.format("Cabliration curve, slope=%f, intercept=%f",
								slope, intercept), "Count", "Noise variance",
						dataset, PlotOrientation.VERTICAL, true, true, false);
				XYPlot plot = (XYPlot) chart.getPlot();
				XYLineAndShapeRenderer scatterRenderer = (XYLineAndShapeRenderer) plot
						.getRenderer();
				scatterRenderer.setSeriesShape(0, new Ellipse2D.Double(-0.5,
						-0.5, 1, 1));
				scatterRenderer.setSeriesFillPaint(0, Color.BLACK);
				scatterRenderer.setSeriesShapesFilled(0, true);

				XYSeriesCollection pDataset = new XYSeriesCollection(p);
				plot.setDataset(1, pDataset);
				StandardXYItemRenderer renderer = new StandardXYItemRenderer();
				plot.setRenderer(1, renderer);

				ChartFrame graphFrame = new ChartFrame("CCD Gain Calibration",
						chart);
				graphFrame.getChartPanel().setMouseWheelEnabled(true);
				graphFrame.pack();
				graphFrame.setVisible(true);
			}
		});
	}

	/**
	 * 返回：平均值和方差
	 * 
	 * @return
	 */
	private List<List<Number>> analyzeStack() {
		// TODO Auto-generated method stub
		ImagePlus img = IJ.getImage();
		int samples = img.getImageStackSize() / 2;
		int width = img.getWidth();
		int height = img.getHeight();
		Mean m = new Mean();
		Variance var = new Variance();
		ArrayRealVector vec1 = new ArrayRealVector(width * height);
		ArrayRealVector vec2 = new ArrayRealVector(width * height);
		List<Number> lvlList = new ArrayList<Number>();
		List<Number> noiseList = new ArrayList<Number>();
		for (int i = 0; i < samples; i++) {
			img.setSlice(i * 2 + 1);
			ImageProcessor ip1 = img.getProcessor().crop();
			img.setSlice(i * 2 + 2);
			ImageProcessor ip2 = img.getProcessor().crop();
			for (int j = 0; j < width; j++) {
				for (int k = 0; k < height; k++) {
					vec1.setEntry(j * height + k, ip1.getPixel(j, k));
					vec2.setEntry(j * height + k, ip2.getPixel(j, k));
				}
			}
			double ratio = -m.evaluate(vec1.getDataRef())
					/ m.evaluate(vec2.getDataRef());
			lvlList.add(m.evaluate(vec1.getDataRef()));
			vec1.combineToSelf(1, ratio, vec2);			
			noiseList.add(var.evaluate(vec1.getDataRef()));
		}
		List<List<Number>> result = new ArrayList<List<Number>>();
		result.add(lvlList);
		result.add(noiseList);
		return result;
	}

	boolean isCalibrating() {
		if (calibrThrd_ == null)
			return false;
		return calibrThrd_.isAlive();
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setApp(ScriptInterface app) {
		// TODO Auto-generated method stub
		gui_ = app;
		if (gui_ != null)
			core_ = gui_.getMMCore();
	}

	/*
	 * Get micro-manager main window
	 */
	public ScriptInterface getMMGui() {
		return gui_;
	}

	/*
	 * Get micro-manager's CMMCore object
	 */
	public CMMCore getMMCore() {
		return core_;
	}

	@Override
	public void show() {
		frame_ = CCDGainCalcFrame.getInstance(CCDGainCalc.this);
		frame_.setVisible(true);
	}

	@Override
	public void configurationChanged() {
		// TODO Auto-generated method stub

	}

	@Override
	public String getDescription() {
		return "Calibration the CCD Gain via noise analysis.";
	}

	@Override
	public String getInfo() {
		return "CCD Gain Calc Plugin";
	}

	@Override
	public String getVersion() {
		// TODO Auto-generated method stub
		return "CCD Gain Calc v1.0";
	}

	@Override
	public String getCopyright() {
		// TODO Auto-generated method stub
		return "GPL/Zephyre, 2012";
	}

	void stopCalibr() {
		// TODO Auto-generated method stub

	}
}
