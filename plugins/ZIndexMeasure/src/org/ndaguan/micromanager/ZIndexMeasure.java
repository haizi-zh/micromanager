package org.ndaguan.micromanager;
import ij.IJ;
import ij.WindowManager;

import java.util.concurrent.TimeUnit;

import mmcorej.CMMCore;
import mmcorej.TaggedImage;

import org.micromanager.MMStudioMainFrame;
import org.micromanager.api.DataProcessor;
import org.micromanager.api.MMPlugin;
import org.micromanager.api.ScriptInterface;
import org.micromanager.utils.MMException;

public class ZIndexMeasure implements MMPlugin {
	public CMMCore core_;
	public myCalculator mCalc = null;
	public MMStudioMainFrame gui_;
	private myGUI mygui_;
	private static ZIndexMeasure instance_;
	private DataProcessor<TaggedImage> processor_;

	private String zstage_;
	private String xystage_;

	public double currxpos_ = 0;
	public double currypos_ = 0;
	public double currzpos_ = 0;

	// MyAnalyzer
	public boolean isSetScale = false;
	public boolean isInstalCallback = false;
	public boolean isAcquisitionRunning = false;
	public boolean isCalibration = false;
	private TCPServer tcpServer_;
	private int port_=50501;

	public static ZIndexMeasure getInstance() {
		return instance_;
	}

	public void setApp(ScriptInterface app) {
		gui_ = (MMStudioMainFrame) app;
		core_ = gui_.getMMCore();
		processor_ = new AcqAnalyzer(app);
		instance_ = this;
		mygui_ = new myGUI();
		mygui_.GUIInitialization();

		xystage_ = core_.getXYStageDevice();
		zstage_ = core_.getFocusDevice();

		try {
			currxpos_ = core_.getXPosition(xystage_);
			currypos_ = core_.getYPosition(xystage_);
			currzpos_ = core_.getPosition(zstage_);
		} catch (Exception e1) {
			mygui_.log("GET POSTION ERR");
		}
		mCalc = new myCalculator();
		tcpServer_ = new TCPServer(core_,port_);
		mygui_.log("tcpServer ini ok");
		tcpServer_.start();
		mygui_.log("mCalc ini ok");

	}

	public void PullMagnet() {
		(new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					double currMP285zpos = core_.getPosition("MP285 Z Stage");
					core_.setPosition("MP285 Z Stage", currMP285zpos - mygui_.Mstep_);
					mygui_.log(String.format("Set MP285 ZStage to:%f",
							currMP285zpos - mygui_.Mstep_));
				} catch (Exception e) {
					mygui_.log("Set MP285 ZStage ERR" + e.toString());
				}
			}
		})).start();
	}

	// (new Thread(new Runnable() { @Override public void run() { test1(); }
	// })).start();
	public void StartCalibration() {
		mygui_
		.log("Calibration Start......Checking up in IJ log for more infomation");
		if (!isSetScale) {
			mygui_.log("Setscale first!");
			return;
		}
		if (gui_.getAcquisitionEngine().isAcquisitionRunning()) {
			gui_.getAcquisitionEngine().stop(true);
		}
		if (gui_.isLiveModeOn()) {
			gui_.enableLiveMode(false);
		}
		double temp;
		double detal;
		try {
			currzpos_ = core_.getPosition(zstage_);
			for (int z = 0; z < mygui_.calPos_.length; z++) {
				setZPosition(mygui_.calPos_[z]);
				temp = core_.getPosition(zstage_);
				detal = mygui_.calPos_[z] - temp;
				if (detal > 0.002 || detal < -0.002) {
					IJ.log(String.format(
							"Warning:set z position at%f,return %f detal =%f",
							mygui_.calPos_[z], temp, detal));
				}
				IJ.log(String.format("Calibrating:%d/%d\r\n", z,
						mygui_.calPos_.length));
				Object[] ret_ = null;
				double[] outpos = new double[2];
				gui_.snapSingleImage();
				ret_ = mCalc.Calibration(WindowManager.getCurrentImage()
						.getProcessor().getPixels(), mygui_.calcRoi_, z);
				outpos[0] = ((double[]) ret_[0])[0];
				outpos[1] = ((double[]) ret_[0])[1];
				mygui_.reSetROI((int) outpos[0], (int) outpos[1]);
				IJ.log(String.format("xpos:%f--ypos:%f\r\n", outpos[0],
						outpos[1]));
			}
			setZPosition(currzpos_);// turn back to the first place,Always 5
			gui_.snapSingleImage();
			mygui_.log("Calibration OK......");
		} catch (Exception e) {
			mygui_.log("Calibration False! god knows why......" + e.toString());
			e.printStackTrace();
		}
		try {
			Testing();
		} catch (Exception e) {
			e.printStackTrace();
		}
		isCalibration = true;
	}

	public void dispose() {
		mCalc.DeleteData();
		if (isInstalCallback) {
			gui_.getAcquisitionEngine().removeImageProcessor(processor_);
			isInstalCallback = false;
			mygui_.log("Processor dettached.");
		}

	}

	public void show() {
				if ((!gui_.getAcquisitionEngine().isAcquisitionRunning())
						&& (!gui_.isLiveModeOn())) {
					gui_.enableLiveMode(true);
					mygui_.Live.setText("Stop Live");
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

		if (!isCalibration) {
			mygui_.log("Start Calibration first!");
			return;
		}

		if (isInstalCallback) {
			mygui_.log("Call back is installed! mission abort");
		} else {
			gui_.getAcquisitionEngine().addImageProcessor(processor_);
			isInstalCallback = true;
			mygui_.log("Call back install,Start capture...");
		}

		if (!gui_.getAcquisitionEngine().isAcquisitionRunning()) {
			gui_.getAcquisitionEngine().enableFramesSetting(true);
			gui_.getAcquisitionEngine().setSaveFiles(true);
			gui_.getAcquisitionEngine().setRootName(mygui_.StoragePath_);
			gui_.getAcquisitionEngine().setFrames(mygui_.Frame2Acq_,
					mygui_.TimeIntervals_);
			try {
				gui_.getAcquisitionEngine().acquire();
			} catch (MMException e) {
				mygui_.log("Image Acquistion False");
			}
		}
//		
//		mygui_.dataSeries_.delete(0, mygui_.dataSeries_.getItemCount() - 1);
	}

	public void UninstallCallback() {

		if (isInstalCallback) {
			gui_.getAcquisitionEngine().removeImageProcessor(processor_);
			isInstalCallback = false;
			mygui_.log("Call back uninstal,Stop capture");
		} else {
			mygui_.log("Repeat!");
		}
		gui_.getAcquisitionEngine().stop(true);
	}


	public void setXPosition(double xpos) throws Exception {

		core_.setXYPosition(xystage_, xpos, core_.getYPosition(xystage_));
		TimeUnit.MILLISECONDS.sleep(mygui_.sleeptime_);
	}

	public void setYPosition(double ypos) throws Exception {

		core_.setXYPosition(xystage_, core_.getXPosition(xystage_), ypos);
		TimeUnit.MILLISECONDS.sleep(mygui_.sleeptime_);
	}

	public void setZPosition(double zpos) throws Exception {

		core_.setPosition(zstage_, zpos);
		TimeUnit.MILLISECONDS.sleep(mygui_.sleeptime_);
	}

	public void setRXPosition(double xpos) throws Exception {

		core_.setRelativeXYPosition(xystage_, xpos, 0);
		TimeUnit.MILLISECONDS.sleep(mygui_.sleeptime_);
	}

	public void setRYPosition(double ypos) throws Exception {

		core_.setRelativeXYPosition(xystage_,0, ypos);
		TimeUnit.MILLISECONDS.sleep(mygui_.sleeptime_);
	}


	public void setRZPosition(double zpos) throws Exception {

		core_.setRelativePosition(zstage_, zpos);
		TimeUnit.MILLISECONDS.sleep(mygui_.sleeptime_);
	}



	public void Debug() throws Exception {
		// (new Thread(new Runnable() {@Override public void run() {try {
		// //Maininstance_.debug();
		// } catch (Exception e) {e.printStackTrace();}}})).start();

	}

	private void Testing() throws Exception {// to verify if this stuff
		// workable
		mygui_
		.log("Test begin......checking out the IJ log for move infomation.");
		int len = mygui_.calPos_.length;
		double pos[] = new double[4];
		IJ.log(String.format("Testing:\r\n#index,#real,#get,#detal"));
		for (int i = 0; i < len; i++) {// get XYZPostion
			setZPosition(mygui_.calPos_[i]);
			double zpos = core_.getPosition(zstage_);
			pos = getXYZPositon();
			mygui_.dataSeries_.add(i, pos[2]);
			IJ
			.log(String.format("%d,%f,%f,%f", i, zpos, pos[2], zpos
					- pos[2]));
		}
		setZPosition(currzpos_);// turn back to the first place,Always 5
		mygui_.log("Test over ");
	}

	// -----------------------------------------------------------------------------------------DEBUG
	public double[] getXYZPositon() {
		gui_.snapSingleImage();
		Object[] ret = null;
		ret = mCalc.GetZPosition(WindowManager.getCurrentImage().getProcessor()
				.getPixels(), mygui_.calcRoi_,-1);
		return (double[]) ret[0];
	}
}