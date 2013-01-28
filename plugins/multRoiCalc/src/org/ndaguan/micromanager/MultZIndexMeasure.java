package org.ndaguan.micromanager;

import java.util.ArrayList;

import javax.swing.JFrame;

import org.micromanager.MMStudioMainFrame;
import org.micromanager.api.MMPlugin;
import org.micromanager.api.ScriptInterface;
import org.ndaguan.micromanager.OverlayRender.RenderItem;

public class MultZIndexMeasure implements MMPlugin {
	private static MultZIndexMeasure instance_;
	MMStudioMainFrame gui_;	
	private AcqAnalyzer processor_;
	private TCPServer tcpServer_;
	private int port_ = 50501;
	private ZIndexMeasureFrame myFrame;
	Function function_;

	public Listener listener_;
	public AcqAnalyzer analyzer_; 
	public static String menuName = "MultZIndexMeasure";
	public static String tooltipDescription = "MultZIndexMeasure";

	public ArrayList<RenderItem> roiList_ ;
	public Preferences preferences_;
	public OverlayRender render_;
	public Kernel kernel;
	public static MultZIndexMeasure getInstance() {
		return instance_;
	}

	public void setApp(ScriptInterface app) {
		gui_ = (MMStudioMainFrame) app;
		instance_ = this;
		preferences_ = Preferences.getInstance();
		roiList_ = new ArrayList<OverlayRender.RenderItem>();
		render_ = OverlayRender.getInstance(gui_,preferences_);
		function_ = Function.getInstance(gui_,roiList_,preferences_);
		kernel = Kernel.getInstance(function_,roiList_,preferences_);
		listener_ =Listener.getInstance(gui_,function_);
		analyzer_ = AcqAnalyzer.getInstance(kernel,listener_,roiList_,function_,render_);
		myFrame = ZIndexMeasureFrame.getInstance(listener_,function_);
		tcpServer_ = TCPServer.getInstance(gui_.getMMCore(), port_);
		tcpServer_.start();	
	}

	public void dispose() {
		kernel.releaseBuffer();
		if (function_.isInstalCallback) {
			gui_.getAcquisitionEngine().removeImageProcessor(processor_);
			function_.isInstalCallback = false;
			myFrame.log("Processor dettached.");
		}
	}

	public void show() {
		function_.InstallCallback();
		myFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);	
		myFrame.setVisible(true);
		if ((!gui_.getAcquisitionEngine().isAcquisitionRunning())
				&& (!gui_.isLiveModeOn())) {
			gui_.enableLiveMode(true);
		}
	}

	public void configurationChanged() {}
	public String getDescription() {return null;}
	public String getInfo() {return null;}
	public String getVersion() {return null;}
	public String getCopyright() {return null;}

}

