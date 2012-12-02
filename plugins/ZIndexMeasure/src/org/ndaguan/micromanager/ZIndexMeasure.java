package org.ndaguan.micromanager;

import ij.IJ;

import java.awt.geom.Point2D;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import mmcorej.CMMCore;
import mmcorej.TaggedImage;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.jfree.data.xy.XYSeries;
import org.json.JSONException;
import org.micromanager.MMStudioMainFrame;
import org.micromanager.acquisition.TaggedImageQueue;
import org.micromanager.api.MMPlugin;
import org.micromanager.api.ScriptInterface;
import org.micromanager.api.TaggedImageAnalyzer;
import org.micromanager.utils.MMScriptException;
import org.zephyre.micromanager.OverlayRender;
import org.zephyre.micromanager.OverlayRender.RenderItem;

public class ZIndexMeasure implements MMPlugin {
	public CMMCore core_;
	public myCalculator mCalc = null;
	public MMStudioMainFrame gui_;
	private MyGUI mygui_;
	private static ZIndexMeasure instance_;
	private AcqAnalyzer processor_;

	public AcqAnalyzer getProcessor() {
		return processor_;
	}

	private String zStage_;
	private String xyStage_;

	// Z calibration positions
	double[] calPosZ_;
	// XY calibration positions
	double[][] calPosXY_;

	/**
	 * Update stage positions used for calibration.
	 * 
	 * @param xy
	 * @param z
	 */
	public void updateCalPos(double[][] xy, double[] z) {
		calPosZ_ = z;
		calPosXY_ = xy;
	}

	public double currxpos_ = 0;
	public double currypos_ = 0;
	public double currzpos_ = 0;

	// MyAnalyzer
	public boolean isSetScale = false;
	public boolean isInstalCallback = false;
	public boolean isAcquisitionRunning = false;
	public boolean isCalibrated = false;
	private TCPServer tcpServer_;
	private int port_ = 50501;
	private XYSeries zDataSeries_;

	public static ZIndexMeasure getInstance() {
		return instance_;
	}

	public void setApp(ScriptInterface app) {
		gui_ = (MMStudioMainFrame) app;
		core_ = gui_.getMMCore();
		instance_ = this;

		if (mygui_ == null) {
			mygui_ = new MyGUI(this);
			zDataSeries_ = mygui_.myForm_.getDataSeries_().get("Z-Chart");
			processor_ = AcqAnalyzer.getInstance(app, this, mygui_);
			mCalc = new myCalculator();
			tcpServer_ = new TCPServer(core_, port_);
			mygui_.myForm_.log("tcpServer ini ok");
			tcpServer_.start();
			mygui_.myForm_.log("mCalc ini ok");
			// gui_.getAcquisitionEngine().addImageProcessor(
			// TestAnalyzer.getInstance(gui_));
		}

		xyStage_ = core_.getXYStageDevice();
		zStage_ = core_.getFocusDevice();

		try {
			updatePositions();
		} catch (Exception e1) {
			mygui_.myForm_.log("GET POSTION ERR");
		}
	}

	public void updatePositions() throws Exception {
		currxpos_ = core_.getXPosition(xyStage_);
		currypos_ = core_.getYPosition(xyStage_);
		currzpos_ = core_.getPosition(zStage_);
	}

	public void PullMagnet() {
		(new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					double currMP285zpos = core_.getPosition("MP285 Z Stage");
					core_.setPosition("MP285 Z Stage", currMP285zpos
							- mygui_.Mstep_);
					mygui_.myForm_.log(String.format("Set MP285 ZStage to:%f",
							currMP285zpos - mygui_.Mstep_));
				} catch (Exception e) {
					mygui_.myForm_.log("Set MP285 ZStage ERR" + e.toString());
				}
			}
		})).start();
	}

	public static void main(String[] argv) {
		System.out.println(System.getProperty("user.home"));
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
		Calendar cal = new GregorianCalendar();
		System.out.println(dateFormat.format(cal.getTime()));
	}

	// (new Thread(new Runnable() { @Override public void run() { test1(); }
	// })).start();
	public void StartCalibration() {
		mygui_.myForm_.log("Calibration Start......Checking up in IJ log for more infomation");
		if (!isSetScale) {
			mygui_.myForm_.log("Setscale first!");
			return;
		}
		if (gui_.getAcquisitionEngine().isAcquisitionRunning()) {
			gui_.getAcquisitionEngine().stop(true);
		}
		if (gui_.isLiveModeOn()) {
			gui_.enableLiveMode(false);
		}
		double temp;
		double delta;
		try {
			updatePositions();
			OverlayRender render = OverlayRender.getInstance(gui_);

			// Polyfit to calibrate xy axes
			double[][] stagePos = new double[2][];
			double[][] loc = new double[2][];
			for (int i = 0; i < 2; i++) {
				stagePos[i] = new double[calPosZ_.length];
				loc[i] = new double[calPosZ_.length];
			}

			for (int z = 0; z < calPosZ_.length; z++) {
				// Calibration on X/Y/Z
				core_.setXYPosition(xyStage_, calPosXY_[0][z], calPosXY_[1][z]);
				setZPosition(calPosZ_[z]);

				temp = core_.getPosition(zStage_);
				double[] tempX = new double[1];
				double[] tempY = new double[2];
				core_.getXYPosition(xyStage_, tempX, tempY);

				delta = calPosZ_[z] - temp;
				if (delta > 0.002 || delta < -0.002) {
					IJ.log(String.format(
							"Warning:set z position at%f,return %f detal =%f",
							calPosZ_[z], temp, delta));
				}
				IJ.log(String.format("Calibrating:%d/%d\r\n", z,
						calPosZ_.length));
				Object[] ret_ = null;
				double[] outpos = new double[2];
				gui_.snapSingleImage();
				Object pix = core_.getTaggedImage().pix;
				gui_.logMessage(Arrays.toString(mygui_.calcRoi_));
				ret_ = mCalc.Calibration(pix, mygui_.calcRoi_, z);
				outpos[0] = ((double[]) ret_[0])[0];
				outpos[1] = ((double[]) ret_[0])[1];
				mygui_.resetROI((int) outpos[0], (int) outpos[1]);
				IJ.log(String.format("xpos:%f--ypos:%f\r\n", outpos[0],
						outpos[1]));

				ArrayList<RenderItem> list = new ArrayList<OverlayRender.RenderItem>();
				list.add(RenderItem.createInstance(new Point2D.Float(
						(float) outpos[0], (float) outpos[1]), String.format(
						"(%.2f, %.2f)", outpos[0], outpos[1])));
				render.render(MMStudioMainFrame.SIMPLE_ACQ, list, 0, true);

				// XY calibration
				stagePos[0][z] = tempX[0];
				stagePos[1][z] = tempY[0];
				loc[0][z] = outpos[0];
				loc[1][z] = outpos[1];
			}
			core_.setXYPosition(xyStage_, currxpos_, currypos_);
			setZPosition(currzpos_);// turn back to the first place,Always 5

			SimpleRegression regrX = new SimpleRegression();
			SimpleRegression regrY = new SimpleRegression();
			for (int i = 0; i < loc[0].length; i++) {
				regrX.addData(loc[0][i], stagePos[0][i]);
				regrY.addData(loc[1][i], stagePos[1][i]);
			}
			double[] vec = new double[4];
			vec[0] = regrX.getIntercept();
			vec[1] = regrX.getSlope();
			vec[2] = regrY.getIntercept();
			vec[3] = regrY.getSlope();
			processor_.setPixelToPhys(vec);

			gui_.logMessage(String.format("Xx: %s", Arrays.toString(loc[0])));
			gui_.logMessage(String.format("Xy: %s",
					Arrays.toString(stagePos[0])));
			gui_.logMessage(String.format("Yx: %s", Arrays.toString(loc[1])));
			gui_.logMessage(String.format("Yy: %s",
					Arrays.toString(stagePos[1])));
			gui_.logMessage(String.format("XY Calibration: %s",
					Arrays.toString(vec)));

			gui_.snapSingleImage();
			mygui_.myForm_.log("Calibration OK......");
			gui_.logMessage("Calibration OK......");

			testing();
			isCalibrated = true;
			processor_.resetData_ = true;
		} catch (Exception e) {
			mygui_.myForm_.log("Calibration False! god knows why......" + e.toString());
			e.printStackTrace();
		}
	}

	public void dispose() {
		mCalc.DeleteData();
		if (isInstalCallback) {
			gui_.getAcquisitionEngine().removeImageProcessor(processor_);
			isInstalCallback = false;
			mygui_.myForm_.log("Processor dettached.");
		}

	}

	public void show() {
// 		mygui_.setVisible(true);
		if ((!gui_.getAcquisitionEngine().isAcquisitionRunning())
				&& (!gui_.isLiveModeOn())) {
			// gui_.snapSingleImage();
			gui_.enableLiveMode(true);
//			mygui_.Live.setText("Stop Live");
		}
	}

	public void configurationChanged() {
	}

	public String getDescription() {
		return null;
	}

	public String getInfo() {
		return null;
	}

	public String getVersion() {
		return null;
	}

	public String getCopyright() {
		return null;
	}

	/*
	 * install call back
	 */

	public void InstallCallback() {

		if (!isCalibrated) {
			mygui_.myForm_.log("Start Calibration first!");
			return;
		}

		if (isInstalCallback) {
			mygui_.myForm_.log("Call back is installed! mission abort");
		} else {
			gui_.getAcquisitionEngine().addImageProcessor(processor_);
			isInstalCallback = true;
			mygui_.myForm_.log("Call back install,Start capture...");
		}

		// gui_.getAcquisitionEngine().enableFramesSetting(true);
		// gui_.getAcquisitionEngine().setSaveFiles(true);
		// gui_.getAcquisitionEngine().setRootName(mygui_.StoragePath_);
		// gui_.getAcquisitionEngine().setFrames(mygui_.Frame2Acq_,
		// mygui_.TimeIntervals_);
		// try {
		// gui_.getAcquisitionEngine().acquire();
		// } catch (MMException e) {
		// mygui_.myForm_.log("Image Acquistion False");
		// }
		// }
		// mygui_.dataSeries_.clear();
	}

	public void UninstallCallback() {

		if (isInstalCallback) {
			gui_.getAcquisitionEngine().removeImageProcessor(processor_);
			isInstalCallback = false;
			mygui_.myForm_.log("Call back uninstal,Stop capture");
		} else {
			mygui_.myForm_.log("Repeat!");
		}
		// gui_.getAcquisitionEngine().stop(true);
	}

	public void setXPosition(double xpos) throws Exception {

		core_.setXYPosition(xyStage_, xpos, core_.getYPosition(xyStage_));
		TimeUnit.MILLISECONDS.sleep(mygui_.sleeptime_);
	}

	public void setYPosition(double ypos) throws Exception {

		core_.setXYPosition(xyStage_, core_.getXPosition(xyStage_), ypos);
		TimeUnit.MILLISECONDS.sleep(mygui_.sleeptime_);
	}

	public void setZPosition(double zpos) throws Exception {

		core_.setPosition(zStage_, zpos);
		TimeUnit.MILLISECONDS.sleep(mygui_.sleeptime_);
	}

	public void setRXPosition(double xpos) throws Exception {

		core_.setRelativeXYPosition(xyStage_, xpos, 0);
		TimeUnit.MILLISECONDS.sleep(mygui_.sleeptime_);
	}

	public void setRYPosition(double ypos) throws Exception {

		core_.setRelativeXYPosition(xyStage_, 0, ypos);
		TimeUnit.MILLISECONDS.sleep(mygui_.sleeptime_);
	}

	public void setRZPosition(double zpos) throws Exception {

		core_.setRelativePosition(zStage_, zpos);
		TimeUnit.MILLISECONDS.sleep(mygui_.sleeptime_);
	}

	public void Debug() throws Exception {
		// (new Thread(new Runnable() {@Override public void run() {try {
		// //Maininstance_.debug();
		// } catch (Exception e) {e.printStackTrace();}}})).start();

	}

	private void testing() throws Exception {// to verify if this stuff
		// workable
		mygui_.myForm_.log("Test begin......checking out the IJ log for move infomation.");
		IJ.log(String.format("Testing:\r\n#index,#real,#get,#delta"));

		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				zDataSeries_.clear();
			}
		});
		int len = mygui_.calPos_.length;

		for (int i = 0; i < len; i++) {// get XYZPostion
			gui_.logMessage(String.format("Set z position: %f", calPosZ_[i]));
			setZPosition(calPosZ_[i]);
			final double zpos = core_.getPosition(zStage_);
			gui_.logMessage(String.format("Set z position done: %f", zpos));
			gui_.snapSingleImage();
			final double[] pos = getXYZPositon();
			final int index = i;
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					zDataSeries_.add(zpos, zpos - pos[2]);
					gui_.logMessage(String.format(
							"i=%d, zpos=%f, pos[2]=%f, delta=%f", index, zpos,
							pos[2], zpos - pos[2]));
				}
			});
		}
		setZPosition(currzpos_);// turn back to the first
								// place,Always 5

		mygui_.myForm_.log("Test over ");
	}

	// -----------------------------------------------------------------------------------------DEBUG
	public double[] getXYZPositon() throws Exception {
		gui_.logMessage("Localization...");
		Object[] ret = null;
		Object pix = null;
		pix = core_.getTaggedImage().pix;
		gui_.logMessage(Arrays.toString(mygui_.calcRoi_));
		ret = mCalc.GetZPosition(pix, mygui_.calcRoi_, -1);
		return (double[]) ret[0];
	}
}

class TestAnalyzer extends TaggedImageAnalyzer {
	private ScriptInterface gui_;
	private static TestAnalyzer instance_;
	private double x_ = 320;
	private double y_ = 240;
	private Random rand_;
	private OverlayRender render_;

	protected TestAnalyzer(ScriptInterface gui) {
		gui_ = gui;
		rand_ = new Random();
		render_ = OverlayRender.getInstance(gui);
	}

	static TestAnalyzer getInstance(ScriptInterface gui) {
		if (instance_ == null)
			instance_ = new TestAnalyzer(gui);
		return instance_;
	}

	@Override
	protected void analyze(TaggedImage taggedImage) {
		if (taggedImage == null || taggedImage == TaggedImageQueue.POISON)
			return;

		String acqName;
		long index;
		try {
			if (!taggedImage.tags.has("FrameIndex"))
				index = 0;
			else
				index = taggedImage.tags.getLong("FrameIndex");
			x_ += rand_.nextDouble() - 0.5;
			y_ += rand_.nextDouble() - 0.5;
			acqName = taggedImage.tags.getString("AcqName");
		} catch (JSONException e) {
			e.printStackTrace();
			return;
		}

		ArrayList<RenderItem> list = new ArrayList<OverlayRender.RenderItem>();
		list.add(RenderItem.createInstance(new Point2D.Float((float) x_,
				(float) y_), String.format("(%f, %f)", x_, y_)));
		boolean update = acqName.equals(MMStudioMainFrame.SIMPLE_ACQ) ? true
				: false;
		try {
			render_.render(acqName, list, index, update);
		} catch (MMScriptException e) {
		}
	}

}