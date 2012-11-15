package org.bioscope.readoutnoisecalc;

import java.awt.Color;
import java.awt.geom.Ellipse2D;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

import javax.swing.JOptionPane;

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
import org.micromanager.MMStudioMainFrame;
import org.micromanager.api.MMPlugin;
import org.micromanager.api.ScriptInterface;

public class ReadoutNoiseCalc implements MMPlugin {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	private MMStudioMainFrame gui_;
	private CMMCore core_;

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setApp(ScriptInterface app) {
		// TODO Auto-generated method stub
		gui_ = (MMStudioMainFrame) app;
		core_ = gui_.getMMCore();

		try {
			startCalc();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void startCalc() throws Throwable {
		String cam = core_.getCameraDevice();
		core_.setProperty(cam, "EMSwitch", "On");
		int nSamples = 20;
		double[] emGain = new double[nSamples];
		double F2 = 2;
		double[] x = new double[nSamples];
		for (int i = 0; i < nSamples; i++) {
			emGain[i] = i + 4;
			x[i] = F2 * Math.pow(emGain[i], 2);
		}

		for (int i = 0; i < 10; i++) {
			gui_.snapAndAddToImage5D();
		}
		for (int i = 0; i < nSamples; i++) {
			core_.setProperty(cam, "Gain", emGain[i]);
			for (int j = 0; j < 2; j++)
				gui_.snapAndAddToImage5D();
		}

		// Analyze
		ImagePlus img = IJ.getImage();
		int width = img.getWidth();
		int height = img.getHeight();
		Mean m = new Mean();
		Variance var = new Variance();
		ArrayRealVector vec1 = new ArrayRealVector(width * height);
		ArrayRealVector vec2 = new ArrayRealVector(width * height);
		double[] noise = new double[nSamples];
		for (int i = 0; i < nSamples; i++) {
			img.setSlice(i * 2 + 1 + 10);
			ImageProcessor ip1 = img.getProcessor().crop();
			img.setSlice(i * 2 + 2 + 10);
			ImageProcessor ip2 = img.getProcessor().crop();
			for (int j = 0; j < width; j++) {
				for (int k = 0; k < height; k++) {
					vec1.setEntry(j * height + k, ip1.getPixel(j, k));
					vec2.setEntry(j * height + k, ip2.getPixel(j, k));
				}
			}
			double ratio = -m.evaluate(vec1.getDataRef())
					/ m.evaluate(vec2.getDataRef());
			vec1.combineToSelf(1, ratio, vec2);
			noise[i] = var.evaluate(vec1.getDataRef());
		}

		// Linear fit
		final double[][] data = new double[nSamples][2];
		for (int i = 0; i < nSamples; i++) {
			data[i][0] = x[i];
			data[i][1] = noise[i];
		}
		SimpleRegression reg = new SimpleRegression();
		reg.addData(data);
		final double intercept = reg.getIntercept();
		final double slope = reg.getSlope();

		// Plot
		XYSeries s = new XYSeries(1);
		for (int i = 0; i < nSamples; i++) {
			s.add(x[i], noise[i]);
		}
		XYSeriesCollection dataset = new XYSeriesCollection(s);
		JFreeChart chart = ChartFactory.createScatterPlot(String.format(
				"Cabliration curve, slope=%f, intercept=%f", slope, intercept),
				"EM Factor (F^2*M^2)", "Noise variance", dataset,
				PlotOrientation.VERTICAL, true, true, false);
		XYPlot plot = (XYPlot) chart.getPlot();
		XYLineAndShapeRenderer scatterRenderer = (XYLineAndShapeRenderer) plot
				.getRenderer();
		scatterRenderer.setSeriesShape(0,
				new Ellipse2D.Double(-0.5, -0.5, 1, 1));
		scatterRenderer.setSeriesFillPaint(0, Color.BLACK);
		scatterRenderer.setSeriesShapesFilled(0, true);

		XYSeries p = new XYSeries("Plot");
		double x0 = x[0];
		double y = intercept + slope * x0;
		p.add(x0, y);
		x0 = x[nSamples - 1];
		y = intercept + slope * x0;
		p.add(x0, y);
		XYSeriesCollection pDataset = new XYSeriesCollection(p);
		plot.setDataset(1, pDataset);
		StandardXYItemRenderer renderer = new StandardXYItemRenderer();
		plot.setRenderer(1, renderer);

		ChartFrame graphFrame = new ChartFrame("CCD Readout Noise Calibration",
				chart);
		graphFrame.getChartPanel().setMouseWheelEnabled(true);
		graphFrame.pack();
		graphFrame.setVisible(true);
	}

	@Override
	public void show() {
		// TODO Auto-generated method stub

	}

	@Override
	public void configurationChanged() {
		// TODO Auto-generated method stub

	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getVersion() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCopyright() {
		// TODO Auto-generated method stub
		return null;
	}

}
