package org.ndaguan.micromanager;

import java.awt.geom.Point2D;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import mmcorej.CMMCore;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.jfree.data.xy.XYSeries;
import org.micromanager.MMStudioMainFrame;
import org.micromanager.api.MMPlugin;
import org.micromanager.api.ScriptInterface;
import org.micromanager.utils.MMScriptException;
import org.zephyre.micromanager.ZIndexMeasure.OverlayRender;
import org.zephyre.micromanager.ZIndexMeasure.OverlayRender.RenderItem;

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
	private  String xyStage_;

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

	public int mp285StepCounter = 0;
	public boolean isUserStop = false;
	public boolean isCalibrationRunning = false;
	private SimpleRegression regrY;
	private SimpleRegression regrX;
	private OverlayRender render;
	public boolean isInstallCallback_ = false;
	public boolean isTestingRunning = false;
	private TestAnalyzer testProcessor_;
	public int calibrationState_ = 0;
	private boolean isTestInstallCallback_;

	public static ZIndexMeasure getInstance() {
		return instance_;
	}

	public void setApp(ScriptInterface app) {
		if(instance_ == null){
			gui_ = (MMStudioMainFrame) app;
			core_ = gui_.getMMCore();
			instance_ = this;
			mygui_ =MyGUI.getInstance(this,gui_);
			mygui_.myForm_.setVisible(true);

			processor_ = AcqAnalyzer.getInstance(gui_, this, mygui_);
			mCalc = myCalculator.getInstance();
			tcpServer_ = new TCPServer(core_, port_);
			tcpServer_.start();
			//

			zDataSeries_ = mygui_.myForm_.getDataSeries_().get("Chart-Z");
			calfileSeries_ = mygui_.myForm_.getDataSeries_().get("Chart-Calfile");	

			corrSeries_ = mygui_.myForm_.getDataSeries_().get("Chart-Corr");

			xyStage_ = core_.getXYStageDevice();
			zStage_ = core_.getFocusDevice();
			mygui_.myForm_.log("tcpServer ini ok");
			mygui_.myForm_.log("mCalc ini ok");

			try {
				updatePositions();
			} catch (Exception e1) {
				mygui_.myForm_.log("GET POSTION ERR");
			}
		}
		else{
			mygui_.myForm_.setVisible(true);
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
					if(currMP285zpos - mygui_.Mstep_ >0){
						core_.setPosition("MP285 Z Stage",0);
						mygui_.myForm_.log(String.format("Step:%d\tMove MP285to:%fuM and this is the end",
								mp285StepCounter,currMP285zpos,0));
						mygui_.myForm_.setMagnetAuto(false);
						return;
					}
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
	public void setXYZCalPosition(int z) 
	{
		try {
			core_.setXYPosition(xyStage_, calPosXY_[0][z], calPosXY_[1][z]);
			setZPosition(getCalPosZ_()[z]);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setTestingPosition(int z) 
	{
		try {
			setZPosition(getCalPosZ_()[z]+0.01);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void calibration(Object pix,int z){
		try {
			double zStagePos = core_.getPosition(zStage_);
			double[] xStagePos = new double[1];
			double[] yStagePos = new double[2];
			core_.getXYPosition(xyStage_, xStagePos, yStagePos);


			Object[] ret_ = null;
			double xloc;
			double yloc;

			ret_ = mCalc.Calibration(pix, mygui_.calcRoi_, z);

			xloc = ((double[]) ret_[0])[0];
			yloc = ((double[]) ret_[0])[1];
			final double[] calfile = (double[]) ret_[2];

			// XY calibration					 
			regrX.addData( xloc,xStagePos[0]);
			regrY.addData(yloc,yStagePos[0]);
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					calfileSeries_.clear();
					for(int i = 0;i<calfile.length;i++){
						calfileSeries_.add(i, calfile[i]);
					}
				}
			});
			mygui_.reSetROI((int) xloc, (int) yloc);

			ArrayList<RenderItem> list = new ArrayList<OverlayRender.RenderItem>();
			list.add(RenderItem.createInstance(new Point2D.Float(
					(float) xloc, (float) yloc), String.format(
							"[%d/%d](%.2f, %.2f)", z,this.getCalibrateLenth(),xloc, yloc)));

			render.render(MMStudioMainFrame.SIMPLE_ACQ, list, 0, true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void StartCalibration() {

		if(!isCalibrationRunning){
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
			if(regrX != null)
			{
				regrX.clear();
				regrY.clear();
			}else{
				regrX = new SimpleRegression();
				regrY = new SimpleRegression();
			}

			try {
				updatePositions();
				render = OverlayRender.getInstance(gui_);

				isCalibrationRunning = true;

				setXYZCalPosition(0);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			InstallCallback();
			gui_.snapSingleImage();

		}
		else{
			isCalibrationRunning = false;
		}
	}
	public void testing() {
		stageGoHome();
		gui_.logMessage("Test begin......checking out the Cart-Z for more information.");
		UninstallCallback();
		installTestCallback();
		//IJ.log(String.format("Testing:\r\n#index,#real,#get,#delta"));	

		int len = mygui_.getcalPosLen();
		mygui_.myForm_.getTabbedPane().setSelectedIndex(3);
		setTestingPosition(0);
		isTestingRunning = true;
		gui_.snapSingleImage();

	}

	public double[] getPixelToPyhs() {
		double[] vec = new double[4];
		vec[0] = regrX.getIntercept();
		vec[1] = regrX.getSlope();
		vec[2] = regrY.getIntercept();
		vec[3] = regrY.getSlope();
		return vec;
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
		if(!isInstallCallback_){
			gui_.getAcquisitionEngine().addImageProcessor(processor_);
			isInstallCallback_ = true;
			mygui_.myForm_.log("Call back install,Start capture...");
		}

	}

	public void installTestCallback(){
		if(!isTestInstallCallback_){
			testProcessor_ = TestAnalyzer.getInstance(gui_, this);
			isTestInstallCallback_ = true;
			gui_.getAcquisitionEngine().addImageProcessor(testProcessor_);
			mygui_.myForm_.log("testCall back install,Start capture...");
		}
	}
	public void UninstallTestCallback(){
		if(isTestInstallCallback_){
			testProcessor_ = TestAnalyzer.getInstance(gui_, this);
			gui_.getAcquisitionEngine().removeImageProcessor(testProcessor_);
			isTestInstallCallback_ = false;
			mygui_.myForm_.log("testCall back uninstal,Stop capture");
		}
	}
	public void UninstallCallback() {
		if(isInstallCallback_){
			gui_.getAcquisitionEngine().removeImageProcessor(processor_);
			isInstallCallback_ = false;
			mygui_.myForm_.log("Call back uninstal,Stop capture");
		}
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

	public void SaveTestingData(Object pix,final int index){
		double zpos0 = 0;
		try {
			zpos0 = core_.getPosition(zStage_);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		final double zpos = zpos0;
		Object[] ret = null;
		ret = mCalc.GetZPosition(pix, mygui_.calcRoi_, -1);		
		final double[] pos = (double[]) ret[0];
		final double[] corrProfile = (double[]) ret[2];
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				zDataSeries_.add(zpos, zpos - pos[2]);
				gui_.logMessage(String.format(
						"i=%d, zpos=%f, pos[2]=%f, delta=%f", index, zpos,
						pos[2], zpos - pos[2]));
				corrSeries_.clear();
				for(int i = 0;i<corrProfile.length;i++){
					corrSeries_.add(calPosZ_[i], corrProfile[i]);
				}
			}
		});
		

		double xloc = pos[0];
		double yloc = pos[1];
		mygui_.reSetROI((int) xloc, (int) yloc);

		ArrayList<RenderItem> list = new ArrayList<OverlayRender.RenderItem>();
		list.add(RenderItem.createInstance(new Point2D.Float(
				(float) xloc, (float) yloc), String.format(
						"[%d/%d](%.2f, %.2f,.2%f)",index,this.getCalibrateLenth(), xloc, yloc,pos[2])));

		try {
			render.render(MMStudioMainFrame.SIMPLE_ACQ, list, 0, true);
		} catch (MMScriptException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}



	// -----------------------------------------------------------------------------------------DEBUG
	public double[] getXYZPositon() throws Exception {
		gui_.logMessage("Localization...");
		Object[] ret = null;
		Object pix = null;

		TimeUnit.MICROSECONDS.sleep(200);
		pix = gui_.getPixels();
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

	public int getCalibrateLenth() {
		// TODO Auto-generated method stub
		return this.calPosZ_.length;
	}

	public void stageGoHome() {
		try {
			core_.setXYPosition(xyStage_, currxpos_,currypos_);
			setZPosition(currzpos_);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void testtingEnd() {
		UninstallTestCallback();
		mygui_.myForm_.setCalIcon(true);
		stageGoHome();
	}


}
