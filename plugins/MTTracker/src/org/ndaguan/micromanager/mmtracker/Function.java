package org.ndaguan.micromanager.mmtracker;

import ij.ImagePlus;
import ij.WindowManager;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import mmcorej.CMMCore;

import org.micromanager.MMStudioMainFrame;
import org.micromanager.utils.MMScriptException;

public class Function {

	private static Function instance_;
	private MMStudioMainFrame gui_;
	private CMMCore core_;

	private double currxpos_;
	private double currypos_;
	private double currzpos_;

	public static boolean isCalibrationRunning = false;
	public static boolean isInstalCallback;
	public static boolean isCapture_ = false;
	public static boolean isTestingRunning_ = false;
	public static boolean isAcqAnalyzerInstall_  = false;
	private boolean isCalibrateAnalyzerInstall_ = false;

	private long sleeptime_ = 30;
	private List<RoiItem> roiList_;
	private Preferences preferences_;
	private Kernel kernel_;
	private boolean isTestingAnalyzerInstall_ = false;
	public void clearROI(){
		Kernel.getInstance().roiList_.clear();
	}
	public Function(MMStudioMainFrame gui, List<RoiItem> roiList) {
		core_ = gui.getMMCore();
		gui_ = gui;
		roiList_ = roiList;
		preferences_ = Preferences.getInstance();
		kernel_ = Kernel.getInstance();

	}
	public Function(List<RoiItem> roiList) {
		roiList_ = roiList;
		preferences_ = Preferences.getInstance();
		kernel_ = Kernel.getInstance();
		
	}

	public static Function getInstance(MMStudioMainFrame gui,List<RoiItem> roiList) {
		if(instance_ == null)
			instance_ = new Function(gui,roiList);

		return instance_;
	}
	public static Function getInstance(){
		return instance_;
	}

	void dataReset() {
		if(Listener.getInstance().isRunning()){
			Listener.getInstance().stop();
		}	
		if (roiList_ == null || roiList_.size() == 0)
			return ;

		Iterator<RoiItem> it = roiList_.iterator();
		while (it.hasNext()) {		
			RoiItem item = it.next();
			try {
				item.dataClean();
			} catch (IOException e) {
				e.printStackTrace();
			}		

		}
	}


	public void PullMagnet() {
		(new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					double currMP285zpos = core_.getPosition(MMT.magnetZStage_);
					double z = preferences_.magnetStepSize_;
					double target =  currMP285zpos - z;
					if(target>0){
						target = 0;
						MMTFrame.getInstance().MagnetManual.setSelected(true);
					}
					core_.setPosition(MMT.magnetZStage_,target);
					MMT.logMessage(String.format("Set MP285 ZStage\t%f\t->\t%f",
							currMP285zpos , core_.getPosition(MMT.magnetZStage_)));
				} catch (Exception e) {
					MMT.logError("Set MP285 ZStage ERR" + e.toString());
				}
			}
		})).start();
	}

	public int getFocusRoiIndex() {
		int index =0;
		if (roiList_ == null || roiList_.size() == 0)
			return -1;
		Iterator<RoiItem> it = roiList_.iterator();
		while (it.hasNext()) {		
			RoiItem item = it.next();			
			if(item.isFocus_){
				return index;
			}
			index++;		
		}
		return -1;
	}

	public boolean overlapCheck(int xCenter,int yCenter) {
		if (roiList_ == null || roiList_.size() == 0)
			return false;
		Iterator<RoiItem> it = roiList_.iterator();
		while (it.hasNext()) {		
			RoiItem item = it.next();
			int x = (int) item.x_;
			int y = (int) item.y_;
			int dx = xCenter - x;
			int dy = yCenter - y;
			double r = Math.sqrt(dx*dx + dy*dy);			
			if(r <2 * (int) preferences_.beanRadiuPixel_){
				return true;
			}		
		}
		return false;
	}

	public int getCurrentRoiIndex(Point loc) {
		int index =0;
		if (roiList_ == null || roiList_.size() == 0)
			return -1;

		Iterator<RoiItem> it = roiList_.iterator();
		int beanRadius = preferences_.beanRadiuPixel_;
		while (it.hasNext()) {		
			RoiItem item = it.next();
			int x =(int) item.x_;
			int y = (int) item.y_;
			Rectangle roi = new Rectangle(x-beanRadius,y-beanRadius,2*beanRadius,2*beanRadius);
			if(roi.contains(loc)){
				return index;
			}
			index++;		
		}
		return -1;
	}

	public int getReferenceRoiIndex() {
		int index =0;
		if (roiList_ == null || roiList_.size() == 0)
			return -1;

		Iterator<RoiItem> it = roiList_.iterator();
		while (it.hasNext()) {		
			RoiItem item = it.next();			
			if(item.isSelected_){
				return index;
			}
			index++;		
		}
		return -1;
	}

	public void reSetFocusRoi() {
		int focus = getFocusRoiIndex();
		if(focus != -1){
			final int xCenter = (int) roiList_.get(focus).x_;	
			final int yCenter = (int) roiList_.get(focus).y_;	
			final int beanRadius = preferences_.beanRadiuPixel_;
			SwingUtilities.invokeLater(new Runnable(){
				@Override
				public void run() {
					ImagePlus ip = WindowManager.getCurrentImage();
					if(ip != null)
						ip.setRoi(xCenter-beanRadius,yCenter-beanRadius,2*beanRadius,2*beanRadius);
				}

			});
		}
	}


	public void setFocusdRoi(Point point) {
		int focus = getFocusRoiIndex();
		if(focus != -1){
			roiList_.get(focus).isFocus_ = false;
		}

		int index = getCurrentRoiIndex(point);
		if( index != -1){
			roiList_.get(index).isFocus_ = true;
		}
	}

	public void moveFocusdRoi(int dx, int dy) {
		int index = getFocusRoiIndex();
		if( index != -1){
			roiList_.get(index).x_ += dx;
			roiList_.get(index).y_ += dy;
		}	
	}

	public void addRoi(Rectangle rectangle) {
		int xCenter = rectangle.x+rectangle.width/2;
		int yCenter = rectangle.y+rectangle.height/2;
		if(!overlapCheck(xCenter,yCenter))//selected area exist ROI
		{	
			int index = getFocusRoiIndex();
			if(index != -1)
			{
				roiList_.get(index).isFocus_ = false;
			}
			roiList_.add(RoiItem.createInstance(preferences_,new double[]{xCenter,yCenter,0,0,0,},AcqAnalyzer.getInstance().acqName_));
			if(kernel_.isCalibrated_)
			{
				kernel_.calProfiles.add(kernel_.calProfiles.get(0));
			}
		}
	}

	public void deleteRoi() {
		int index = getFocusRoiIndex();
		if( index != -1){
			if(Kernel.getInstance().isCalibrated_){
				Kernel.getInstance().calProfiles.remove(index);
			}
			roiList_.remove(index);
			SwingUtilities.invokeLater(new Runnable(){
				@Override
				public void run() {
					WindowManager.getCurrentImage().setRoi(new Rectangle(0,0,0,0));
				}

			});
		}	
	}

	public void selectRoiAsReference() {
		int index = getFocusRoiIndex();
		if( index != -1){
			int preReferenceIndex = getReferenceRoiIndex();
			if(preReferenceIndex != -1){
				roiList_.get(preReferenceIndex).setSelect(false);
			}
			roiList_.get(index).setSelect(true);
		}
	}

	public void showChartManager() {
		int focus = getFocusRoiIndex();
		if(focus != -1){
			roiList_.get(focus).chart_.setVisible(true);
		}

	}

	public void reDraw(String acqName, long frameNum_, boolean update) throws MMScriptException {
		reSetFocusRoi();
		OverlayRender.getInstance().render(acqName, roiList_, frameNum_, update);
	}

	public void showPreferencesDialog() {
		MMTFrame.getInstance().preferDailog.setVisible(true);
	}


	public void installAcqAnalyzer(boolean flag) {
		if(flag){
			if (isAcqAnalyzerInstall_)
				return;
			else {
				gui_.getAcquisitionEngine().addImageProcessor(AcqAnalyzer.getInstance());
				isAcqAnalyzerInstall_ = true;
				MMTFrame.getInstance().LiveView.setText("StopCapture");
				MMT.logMessage("Call back install,Start capture");
			}
		}else{
			if (isAcqAnalyzerInstall_) {
				gui_.getAcquisitionEngine().removeImageProcessor(AcqAnalyzer.getInstance());
				isAcqAnalyzerInstall_ = false;
				MMTFrame.getInstance().LiveView.setText("LiveView");
				MMT.logMessage("Call back uninstal,Stop capture");
			} 
			else
				return;
		}

	}
	public void installTestingAnalyzer(boolean flag) {
		if(flag){
			if (isTestingAnalyzerInstall_)
				return;
			else {
				gui_.getAcquisitionEngine().addImageProcessor(TestAnalyzer.getInstance());
				isTestingAnalyzerInstall_ = true;
				MMTFrame.getInstance().LiveView.setText("StopCapture");
				MMT.logMessage("Call back install,Start capture");
			}
		}else{
			if (isTestingAnalyzerInstall_) {
				gui_.getAcquisitionEngine().removeImageProcessor(TestAnalyzer.getInstance());
				isTestingAnalyzerInstall_ = false;
				MMTFrame.getInstance().LiveView.setText("LiveView");
				MMT.logMessage("Call back uninstal,Stop capture");
			} 
			else
				return;
		}
	}
	public void installCalibrateAnalyzer(boolean flag) {
		if(flag){
			if (isCalibrateAnalyzerInstall_)
				return;
			else{
				gui_.getAcquisitionEngine().addImageProcessor(CalibrateAnalyzer.getInstance());
				isCalibrateAnalyzerInstall_ = true;
				MMTFrame.getInstance().LiveView.setText("StopCapture");
				MMT.logMessage("Call back install,Start capture");
			}
		}else{
			if (isCalibrateAnalyzerInstall_) {
				gui_.getAcquisitionEngine().removeImageProcessor(CalibrateAnalyzer.getInstance());
				isCalibrateAnalyzerInstall_ = false;
				MMTFrame.getInstance().LiveView.setText("LiveView");
				MMT.logMessage("Call back uninstal,Stop capture");
			} 
			else
				return;
		}

	}

	public void multiAcq() {
		gui_.getAcqDlg().setVisible(true);
	}


	public void setXYZCalPosition(int z) throws Exception 
	{
		setStageZPosition(kernel_.zPosProfiles[z]);
		if(MMT.xyStage_ != null)
			setStageXYPosition(kernel_.xPosProfiles[z],kernel_.yPosProfiles[z]);

	}
	public void setZCalPosition(int z) throws Exception {
		setStageZPosition(kernel_.zPosProfiles[z]);		
	}
	public double[] getStagePostion() throws Exception
	{
		double temp = getStageZPosition();
		double[] XY = getStageXYPosition();
		return new double[]{XY[0],XY[1],temp};
	}

	public void updateCalibrationProfile(){
		double[][] cal = new double[ (int) (preferences_.calRange_/preferences_.calStepSize_)][(int) (preferences_.beanRadiuPixel_/preferences_.rInterStep_)];
		kernel_.zPosProfiles = new double[ (int) (preferences_.calRange_/preferences_.calStepSize_)];

		for (int i = 0; i < roiList_.size(); i++) {
			kernel_.calProfiles.add(cal);
		}
		int calSize = (int) (preferences_.calRange_/preferences_.calStepSize_);
		try {
			updatePositions();
		} catch (Exception e) {
			e.printStackTrace();
		}
		int midPoint = (int) (preferences_.calRange_ / 2);
		for (int i = 0; i < calSize; i++) {
			kernel_.zPosProfiles[i] = currzpos_ - midPoint + i * preferences_.calStepSize_;
		}
		midPoint = (int) (calSize / 2);
		if(MMT.xyStage_ != null){
			kernel_.xPosProfiles = new double[ (int) (preferences_.calRange_/preferences_.calStepSize_)];
			kernel_.yPosProfiles = new double[ (int) (preferences_.calRange_/preferences_.calStepSize_)];

			for (int j = 0; j < midPoint; j++){
				kernel_.xPosProfiles[j] =  currxpos_ + preferences_.calStepSize_ * j;
				kernel_.yPosProfiles[j] =  currypos_ + preferences_.calStepSize_ * j;
			}
			for (int j = midPoint; j < calSize; j++){
				kernel_.xPosProfiles[j] = kernel_.xPosProfiles[midPoint - 1] - preferences_.calStepSize_ * (j - midPoint);
				kernel_.yPosProfiles[j] = kernel_.yPosProfiles[midPoint - 1] - preferences_.calStepSize_ * (j - midPoint);
			}
		}
	}

	public void snapImage(){
		gui_.snapSingleImage();		
	}

	public void calibrate() {
		if(roiList_ ==null ||roiList_.size() <= 0)return;
		if(!isCalibrationRunning){
			MMT.logMessage("Calibration Start......");

			if (gui_.getAcquisitionEngine().isAcquisitionRunning()) {
				gui_.getAcquisitionEngine().stop(true);
			}

			if (gui_.isLiveModeOn()) {
				gui_.enableLiveMode(false);
			}

			installAcqAnalyzer(false);
			installCalibrateAnalyzer(true);

			updateCalibrationProfile();
			MMTFrame.getInstance().setCalibrateIcon(false);
			isCalibrationRunning = true;
			try {
				setXYZCalPosition(0);
			} catch (Exception e) {
				MMT.logError("Set xyz postion error");
				return;
			}
			gui_.snapSingleImage();

		}
		else{
			isCalibrationRunning = false;
			installAcqAnalyzer(true);
			installCalibrateAnalyzer(false);
			MMTFrame.getInstance().setCalibrateIcon(true);
		}
	}


	public void liveView() {
		if(gui_.isLiveModeOn()){
			gui_.enableLiveMode(false);
			gui_.enableLiveMode(true);
		}else{
			gui_.enableLiveMode(true);
		}
	}
	public void updateTestingChart(final double currZpos) {
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run() {
				for(int i = 0;i<roiList_.size();i++)
				{
					roiList_.get(i).chart_.getDataSeries().get("Chart-Testing").add(currZpos,roiList_.get(i).z_ - currZpos);
				}
			}
		});
	}
	public void cleanTestingData() {
		for(int i = 0;i<roiList_.size();i++)
		{
			roiList_.get(i).chart_.getDataSeries().get("Chart-Debug").clear();
		}
	}
	public void updateChart(final long frameNum) {
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run() {
				for (int i = 0; i < roiList_.size(); i++) {
					roiList_.get(i).chart_.getDataSeries().get("Chart-X").add(frameNum,roiList_.get(i).xPhy_);
					roiList_.get(i).chart_.getDataSeries().get("Chart-Y").add(frameNum,roiList_.get(i).yPhy_);
					roiList_.get(i).chart_.getDataSeries().get("Chart-Z").add(frameNum,roiList_.get(i).z_);
					roiList_.get(i).chart_.getDataSeries().get("Chart-FX").add(frameNum,roiList_.get(i).fx_);
					roiList_.get(i).chart_.getDataSeries().get("Chart-FY").add(frameNum,roiList_.get(i).fy_);
					roiList_.get(i).chart_.getDataSeries().get("Chart-STDXDY").add(frameNum,roiList_.get(i).stdXdY_);
					roiList_.get(i).chart_.getDataSeries().get("Chart-SKREWNESS").add(frameNum,roiList_.get(i).skrewness_);

					
					double pointNum = 0.01;
					double meanx = ((int) (roiList_.get(i).stats_[0].getMean()/10))*pointNum ;//GUI unit uM
					double meany = ((int) (roiList_.get(i).stats_[1].getMean()/10))*pointNum ;//GUI unit uM
					double meanz = ((int) (roiList_.get(i).stats_[2].getMean()/pointNum))*pointNum ;//GUI unit uM
				 

					double stdx = roiList_.get(i).stats_[0].getStandardDeviation()/10e3;
					double stdy = roiList_.get(i).stats_[1].getStandardDeviation()/10e3;
					double stdz = roiList_.get(i).stats_[2].getStandardDeviation();


					double scalex = stdx*2;
					double scaley = stdy*2;
					double scalez = stdz*2;

					if(scalex <0.025)
						scalex = 0.025;
					if(scaley <0.025)
						scaley = 0.025;
					if(scalez <0.025)
						scalez = 0.025;
					roiList_.get(i).chart_.getChartSeries().get("Chart-X").getXYPlot().getRangeAxis().setRange(meanx - scalex,meanx + scalex);
					roiList_.get(i).chart_.getChartSeries().get("Chart-Y").getXYPlot().getRangeAxis().setRange(meany - scaley,meany +  scaley);
					roiList_.get(i).chart_.getChartSeries().get("Chart-Z").getXYPlot().getRangeAxis().setRange(meanz -  scalez,meanz +  scalez);
				}

			}

		});

	}


	public void ShowMagnetManualDialBox() {
		MMTFrame.getInstance().MagnetAuto.setSelected(false);
		MMTFrame.getInstance().myStageControlFrame_.setVisible(true);
	}

	public void setAutoContrast() {
		try {
			gui_.setContrastBasedOnFrame(AcqAnalyzer.getInstance().acqName_, 0, 0);
		} catch (MMScriptException e) {
			MMT.logError("Set auto contrast false");
		}		
	}

	public void showGui() {
		gui_.setVisible(!gui_.isVisible());
	}

	public void showIJ() {
		ij.ImageJ.getFrames()[0].setVisible(!ij.ImageJ.getFrames()[0].isVisible());
	}

	public void updatePositions() throws Exception  {
		currxpos_ = getStageXPosition();
		currypos_ = getStageYPosition();
		currzpos_ = getStageZPosition();
	}

	public void StageGoHome() throws Exception  {
		setStageXYPosition( currxpos_,currypos_);
		setStageZPosition(currzpos_);
	}

	public void setInitStagePosition(int i, int j, int k) throws Exception {
		setStageXYPosition(i, j);
		setStageZPosition(k);
	}

	public void setStageXPosition(double xpos) throws Exception {
		if(MMT.xyStage_ != null){
			core_.setXYPosition(MMT.xyStage_, xpos, core_.getYPosition(MMT.xyStage_));
			TimeUnit.MILLISECONDS.sleep(sleeptime_);
		}
	}
	public double getStageXPosition() throws Exception{
		if(MMT.xyStage_ != null)
			return core_.getXPosition(MMT.xyStage_);
		else 
			return 0;
	}
	public void setStageYPosition(double ypos) throws Exception {
		if(MMT.xyStage_ != null){
			core_.setXYPosition(MMT.xyStage_, core_.getXPosition(MMT.xyStage_), ypos);
			TimeUnit.MILLISECONDS.sleep(sleeptime_);
		}
	}
	public double getStageYPosition() throws Exception{
		if(MMT.xyStage_ != null)
			return core_.getYPosition(MMT.xyStage_);
		else
			return 0;
	}
	public void setStageXYPosition(double xpos,double ypos) throws Exception {
		if(MMT.xyStage_ != null){
			core_.setXYPosition(MMT.xyStage_,xpos, ypos);
			TimeUnit.MILLISECONDS.sleep(sleeptime_);
		}
	}
	public double[] getStageXYPosition() throws Exception {
		if(MMT.xyStage_ != null){
			double[] xpos = new  double[1];
			double[] ypos = new  double[1];
			core_.getXYPosition(MMT.xyStage_, xpos, ypos);
			return new double[]{xpos[0],ypos[0]};
		}
		else
			return null;
	}

	public double getStageZPosition() throws  Exception {
		return  core_.getPosition(MMT.zStage_);
	}
	public void setStageZPosition(double zPos) throws Exception {
		core_.setPosition(MMT.zStage_, zPos);
		TimeUnit.MICROSECONDS.sleep(2000);
	}
	public void liveCapture() {
		installAcqAnalyzer(!isAcqAnalyzerInstall_);
		liveView();
	}
	public void cleanStaticData() {
		for(int i = 0;i<roiList_.size();i++){
			roiList_.get(i).stats_[0].clear();
			roiList_.get(i).stats_[1].clear();
			roiList_.get(i).stats_[2].clear();
		}
	}
}
