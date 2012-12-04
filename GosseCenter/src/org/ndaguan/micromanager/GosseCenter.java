package org.ndaguan.micromanager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import mmcorej.CMMCore;

import org.jfree.data.xy.XYSeries;
import org.micromanager.MMStudioMainFrame;
import org.micromanager.api.MMPlugin;
import org.micromanager.api.ScriptInterface;

public class GosseCenter implements MMPlugin {
	public CMMCore core_;
	public myCalculator mCalc = null;
	public MMStudioMainFrame gui_;
	private MyGUI mygui_;

	private AcqAnalyzer processor_;
	private boolean isInstalCallback;

	public AcqAnalyzer getProcessor() {
		return processor_;
	}

	public void setApp(ScriptInterface app) {
		gui_ = (MMStudioMainFrame) app;
		core_ = gui_.getMMCore();

		if (mygui_ == null) {
			mygui_ = new MyGUI(this,gui_);
			processor_ = AcqAnalyzer.getInstance(app, this, mygui_);
			mCalc = new myCalculator();
			mygui_.myForm_.log("mCalc ini ok");
		}

	}



	public static void main(String[] argv) {
		System.out.println(System.getProperty("user.home"));
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
		Calendar cal = new GregorianCalendar();
		System.out.println(dateFormat.format(cal.getTime()));
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

		if ((!gui_.getAcquisitionEngine().isAcquisitionRunning())
				&& (!gui_.isLiveModeOn())) {

			gui_.enableLiveMode(true);

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
		if (isInstalCallback) {
			mygui_.myForm_.log("statu:false\tCall back is installed! mission abort");
		} else {
			gui_.getAcquisitionEngine().addImageProcessor(processor_);
			isInstalCallback = true;
			mygui_.myForm_.log("statu:ok\tCall back install,Start capture...");
		}

	}

	public void UninstallCallback() {

		if (isInstalCallback) {
			gui_.getAcquisitionEngine().removeImageProcessor(processor_);
			isInstalCallback = false;
			mygui_.myForm_.log("Call back uninstal,Stop capture");
		} else {
			mygui_.myForm_.log("statu:UnInstall Callback false");
		}
	}


}