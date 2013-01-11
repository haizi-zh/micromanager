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

import javax.swing.SwingUtilities;

import mmcorej.CMMCore;
import mmcorej.TaggedImage;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.jfree.chart.JFreeChart;
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
	private double[] calPosZ_;
	// XY calibration positions
	double[][] calPosXY_;

	/**
	 * Update stage positions used for calibration.
	 * 
	 * @param xy
	 * @param z
	 */
	public void updateCalPos(double[][] xy, double[] z) {
		setCalPosZ_(z);
		calPosXY_ = xy;
	}

	public double currxpos_ = 0;
	public double currypos_ = 0;
	public double currzpos_ = 0;

	// MyAnalyzer
	public boolean isSetScale = false;
	public boolean isAcquisitionRunning = false;
	public boolean isCalibrated = false;
	private TCPServer tcpServer_;
	private int port_ = 50501;
	private XYSeries zDataSeries_;
	private XYSeries calfileSeries_;
	private XYSeries corrSeries_;
	private XYSeries calProgressSeries_;
	private XYSeries corrProgressSeries_;
	public int mp285StepCounter = 0;
	public boolean isUserStop = false;
	public boolean isCalibrationRunning = false;

	public static ZIndexMeasure getInstance() {
		return instance_;
	}

	public void setApp(ScriptInterface app) {
		gui_ = (MMStudioMainFrame) app;
		core_ = gui_.getMMCore();
		instance_ = this;

		if (mygui_ == null) {
			mygui_ = new MyGUI(this,gui_);
			zDataSeries_ = mygui_.myForm_.getDataSeries_().get("Chart-Z");
			calfileSeries_ = mygui_.myForm_.getDataSeries_().get("Chart-Calfile");	

			calProgressSeries_ = mygui_.myForm_.getDataSeries_().get("Chart-Calfile-pro");	
			corrProgressSeries_ = mygui_.myForm_.getDataSeries_().get("Chart-Corr-pro");	

			corrSeries_ = mygui_.myForm_.getDataSeries_().get("Chart-Corr");

			processor_ = AcqAnalyzer.getInstance(app, this, mygui_);
			mCalc = new myCalculator();
			tcpServer_ = new TCPServer(core_, port_);
			mygui_.myForm_.log("tcpServer ini ok");
			tcpServer_.start();
			mygui_.myForm_.log("mCalc ini ok");
			//

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
		mp285StepCounter ++;	
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					gui_.logMessage(String.format("\t\t\t\t\t\t\t\t\tstatue:\t%d", mp285StepCounter));
					double currMP285zpos = core_.getPosition("MP285 Z Stage");
					core_.setPosition("MP285 Z Stage", currMP285zpos
							- mygui_.Mstep_);
					mygui_.myForm_.log(String.format("Step:%d\tMove MP285to:%fuM",
							mp285StepCounter,currMP285zpos,currMP285zpos - mygui_.Mstep_));
				} catch (Exception e) {
					mygui_.myForm_.log("Set MP285 ZStage ERR" + e.toString());
				}
			}
		});
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
		mygui_.myForm_.log("Calibration Start......");
		if (!isSetScale) {
			mygui_.myForm_.log("statu\tfalse\tfalseSetscale first!");
			return;
		}
		if (gui_.getAcquisitionEngine().isAcquisitionRunning()) {
			gui_.getAcquisitionEngine().stop(true);
		}
		if (gui_.isLiveModeOn()) {
			gui_.enableLiveMode(false);
		}
		isCalibrationRunning = true;
		double temp;
		double delta;
		try {
			updatePositions();
			OverlayRender render = OverlayRender.getInstance(gui_);

			// Polyfit to calibrate xy axes
			double[][] stagePos = new double[2][];
			double[][] loc = new double[2][];
			for (int i = 0; i < 2; i++) {
				stagePos[i] = new double[getCalPosZ_().length];
				loc[i] = new double[getCalPosZ_().length];
			}

			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {					
					calProgressSeries_.clear();				 
				}
			});
			for (int z = 0; z < getCalPosZ_().length; z++) {
				if(isUserStop){
					isUserStop = false;					
					core_.setXYPosition(xyStage_, currxpos_, currypos_);
					setZPosition(currzpos_);// turn back to the first place,Always 5
					return;
				}
				// Calibration on X/Y/Z
				core_.setXYPosition(xyStage_, calPosXY_[0][z], calPosXY_[1][z]);
				setZPosition(getCalPosZ_()[z]);

				temp = core_.getPosition(zStage_);
				double[] tempX = new double[1];
				double[] tempY = new double[2];
				core_.getXYPosition(xyStage_, tempX, tempY);


				Object[] ret_ = null;
				double[] outpos = new double[2];
				gui_.snapSingleImage();
				Object pix = core_.getTaggedImage().pix;
				gui_.logMessage(Arrays.toString(mygui_.calcRoi_));
				ret_ = mCalc.Calibration(pix, mygui_.calcRoi_, z);
				outpos[0] = ((double[]) ret_[0])[0];
				outpos[1] = ((double[]) ret_[0])[1];
				final double[] calfile = (double[]) ret_[2];

				final double fz = z*calfile.length/getCalPosZ_().length;
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						calProgressSeries_.add(fz, 0);
						calfileSeries_.clear();
						for(int i = 0;i<calfile.length;i++){
							calfileSeries_.add(i, calfile[i]);
						}
					}
				});

				mygui_.reSetROI((int) outpos[0], (int) outpos[1]);

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
			isUserStop = false;
			isCalibrationRunning = false;
			
		} catch (Exception e) {
			mygui_.myForm_.log("Calibration False!\t" + e.toString());
			e.printStackTrace();
		}
	}

	public void dispose() {
		mCalc.DeleteData();		
		gui_.getAcquisitionEngine().removeImageProcessor(processor_);			
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

		gui_.getAcquisitionEngine().addImageProcessor(processor_);
		mygui_.myForm_.log("Call back install,Start capture...");

	}

	public void UninstallCallback() {
		gui_.getAcquisitionEngine().removeImageProcessor(processor_);
		mygui_.myForm_.log("Call back uninstal,Stop capture");
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
		mygui_.myForm_.log("Test begin......checking out the Cart-Z for more information.");
		//IJ.log(String.format("Testing:\r\n#index,#real,#get,#delta"));	
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				zDataSeries_.clear();
				corrProgressSeries_.clear();	
			}
		});
		int len = mygui_.getcalPosLen();
		mygui_.myForm_.getTabbedPane().setSelectedIndex(3);
		for (int i =(int)(len*1/10); i < (int)(len*9/10); i++) {// get XYZPostion
			if(isUserStop){
				isUserStop = false;
				core_.setXYPosition(xyStage_, currxpos_, currypos_);
				setZPosition(currzpos_);// turn back to the first place,Always 5
				return;
			}
			gui_.logMessage(String.format("Set z position: %f", getCalPosZ_()[i]));
			setZPosition(getCalPosZ_()[i]-0.010);
			final double zpos = core_.getPosition(zStage_);
			gui_.logMessage(String.format("Set z position done: %f", zpos));
			gui_.snapSingleImage();
			final double[] pos = getXYZPositon();
			final int index = i;

			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					corrProgressSeries_.add(zpos,0.5);
					zDataSeries_.add(zpos, zpos - pos[2]);
					gui_.logMessage(String.format(
							"i=%d, zpos=%f, pos[2]=%f, delta=%f", index, zpos,
							pos[2], zpos - pos[2]));
				}
			});
		}
		setZPosition(currzpos_);// turn back to the first
		// place,Always 5
		isUserStop = false;
		isCalibrationRunning = false;
		mygui_.myForm_.setCalIcon(true);
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
		final double[] corrProfile = (double[]) ret[2];
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				corrSeries_.clear();
				for(int i = 0;i<corrProfile.length;i++){
					corrSeries_.add(calPosZ_[i], corrProfile[i]);
				}
			}
		});
		return (double[]) ret[0];
	}

	public double[] getCalPosZ_() {
		return calPosZ_;
	}

	public void setCalPosZ_(double[] calPosZ_) {
		this.calPosZ_ = calPosZ_;
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