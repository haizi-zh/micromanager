package org.ndaguan.micromanager.mmtracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JFrame;

import mmcorej.CMMCore;

import org.micromanager.MMStudioMainFrame;
import org.micromanager.api.MMPlugin;
import org.micromanager.api.ScriptInterface;

public class MMTracker implements MMPlugin{

	private MMStudioMainFrame app_;
	private CMMCore core_;
	private List<RoiItem> roiList_;
	private Preferences preferences_;
	private OverlayRender render_;
	private Listener listener_;
	private MMTFrame frame_;
	private Kernel kernel;
	private AcqAnalyzer analyzer_;
	private CalibrateAnalyzer calAnalyzer_;
	private TestAnalyzer testAnalyzer_;
	private TCPServer tcpServer_;
	private Function function_;
	private static MMTracker instance_;

	/**
	 * @param args
	 */
	public static void main(String[] args) {

	}


	@Override
	public void dispose() {

	}


	@Override
	public void setApp(ScriptInterface app) {
		app_ = (MMStudioMainFrame) app;
		core_ = app_.getCore();
		instance_ = this;		
	}

	@Override
	public void show() {
		if(frame_ == null){
			//data 
			preferences_ = Preferences.getInstance();
			roiList_ = Collections.synchronizedList(new ArrayList<RoiItem>());
			//operation
			render_ = OverlayRender.getInstance(app_,preferences_);
			listener_ =Listener.getInstance(app_);
			frame_ = MMTFrame.getInstance(app_,listener_,preferences_);
			kernel = Kernel.getInstance(preferences_,roiList_);
			analyzer_ = AcqAnalyzer.getInstance(kernel,listener_,roiList_,render_,preferences_);
			calAnalyzer_ = CalibrateAnalyzer.getInstance(kernel);
			testAnalyzer_ = TestAnalyzer.getInstance(kernel);
			tcpServer_ = TCPServer.getInstance(app_.getMMCore(), 50501);
			tcpServer_.start();	
			function_ = Function.getInstance(app_,roiList_);
			function_.installAcqAnalyzer(true);
			MMT.xyStage_ = core_.getXYStageDevice();
			MMT.zStage_ = core_.getFocusDevice();
			frame_.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);	
			frame_.setVisible(true);
		}
		else{
			frame_.setVisible(true);
		}

		if ((!app_.getAcquisitionEngine().isAcquisitionRunning())
				&& (!app_.isLiveModeOn())) {
			app_.enableLiveMode(true);
		}
		try {
			function_.setInitStagePosition(50,50,5);
		} catch (Exception e) {
			MMT.logError("Stage init error");
		}		
	}

	public MMTracker getInstance(){
		return instance_;
	}
	public TestAnalyzer getTestAnalyzer(){
		return testAnalyzer_;
	}
	public CalibrateAnalyzer getCalAnalyzer(){
		return calAnalyzer_;
	}
	public AcqAnalyzer getAcqAnalyzer(){
		return analyzer_;
	}
	public void configurationChanged() {}
	public String getDescription() {return null;}
	public String getInfo() {return null;}
	public String getVersion() {return null;}
	public String getCopyright() {return null;}
}
