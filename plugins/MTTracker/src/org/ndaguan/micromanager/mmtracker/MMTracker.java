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
	private OverlayRender render_;
	private Listener listener_;
	private MMTFrame frame_;
	private Kernel kernel;
	private GetXYPositionAnalyzer xyAnalyzer_;
	private GetXYZPositionAnalyzer xyzAnalyzer_;
	private CalibrateAnalyzer calAnalyzer_;
	private TestAnalyzer testAnalyzer_;
	private TCPServer tcpServer_;
	private TCPClient tcpClient_;
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
			LoginDailog pre = new LoginDailog();
			pre.setVisible(true);
			
		}
		else{
			frame_.setVisible(true);
		}
	}
	public void initialize(){
		roiList_ = Collections.synchronizedList(new ArrayList<RoiItem>());
		//operation
		render_ = OverlayRender.getInstance(app_);
		listener_ =Listener.getInstance(app_);
		frame_ = MMTFrame.getInstance(app_,listener_);
		kernel = Kernel.getInstance(roiList_);
		xyAnalyzer_ = GetXYPositionAnalyzer.getInstance(kernel,listener_,roiList_,render_);
		xyzAnalyzer_ = GetXYZPositionAnalyzer.getInstance(kernel,listener_,roiList_,render_);
		calAnalyzer_ = CalibrateAnalyzer.getInstance(kernel);
		testAnalyzer_ = TestAnalyzer.getInstance(kernel);
		function_ = Function.getInstance(app_,roiList_);
		function_.installAnalyzer("XYACQ");
		frame_.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);	
		frame_.setVisible(true);
		MMT.xyStage_ = core_.getXYStageDevice();
		MMT.zStage_ = core_.getFocusDevice();
		if ((!app_.getAcquisitionEngine().isAcquisitionRunning())
				&& (!app_.isLiveModeOn())) {
			app_.enableLiveMode(true);
		}
	}
	public static MMTracker getInstance(){
		return instance_;
	}
	public TestAnalyzer getTestAnalyzer(){
		return testAnalyzer_;
	}
	public CalibrateAnalyzer getCalAnalyzer(){
		return calAnalyzer_;
	}
	public GetXYPositionAnalyzer getAcqAnalyzer(){
		return xyAnalyzer_;
	}
	public GetXYZPositionAnalyzer getXYZAnalyzer(){
		return xyzAnalyzer_;
	}
	public void configurationChanged() {}
	public String getDescription() {return null;}
	public String getInfo() {return null;}
	public String getVersion() {return null;}
	public String getCopyright() {return null;}

	public List<RoiItem> getRoiList() {
		return roiList_;		
	}
	public TCPClient getTcpClient() {
		return tcpClient_;
	}
	public void setTcpClient(TCPClient tcpClient_) {
		this.tcpClient_ = tcpClient_;
	}
	public TCPServer getTcpServer() {
		return tcpServer_;
	}
	public void setTcpServer(TCPServer tcpServer_) {
		this.tcpServer_ = tcpServer_;
	}
}
