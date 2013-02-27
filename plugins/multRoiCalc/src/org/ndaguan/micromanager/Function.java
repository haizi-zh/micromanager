package org.ndaguan.micromanager;

import ij.ImagePlus;
import ij.WindowManager;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import mmcorej.CMMCore;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.micromanager.MMStudioMainFrame;
import org.micromanager.utils.MMScriptException;

public class Function {

	private static Function instance_;
	private MMStudioMainFrame gui_;
	private boolean isSetScale;
	public boolean isInstalCallback;
	private CMMCore core_;
	private double[] calPosZ_;
	private double[][] calPosXY_;
	private double currxpos_;
	private double currypos_;
	private double currzpos_;
	private String xyStage_;
	private String zStage_;
	private Preferences preferences_;
	private ArrayList<RoiItem> roiList_;
	private OverlayRender render_;
	private double[][] stagePos;
	private double[][] loc;
	private SimpleRegression regrX;
	private SimpleRegression regrY;

	public Function(MMStudioMainFrame gui,ArrayList<RoiItem> roiList, Preferences preferences, OverlayRender render) {
		core_ = gui.getMMCore();
		gui_ = gui;
		xyStage_ = core_.getXYStageDevice();
		zStage_ = core_.getFocusDevice();
		roiList_ = roiList;
		preferences_ =  preferences;
		render_ = render;
		try {
			updatePositions();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Function getInstance(MMStudioMainFrame gui,ArrayList<RoiItem> roiList, Preferences preferences, OverlayRender render_) {
		if(instance_ == null)
			instance_ = new Function(gui,roiList,preferences,render_);

		return instance_;
	}

	public void updateCalPos(double[][] xy, double[] z) {
		calPosZ_ = z;
		calPosXY_ = xy;
	}

	public void updatePositions() throws Exception {
		core_.setXYPosition(xyStage_, 50, 50);
		core_.setPosition(zStage_, 5);
		currxpos_ = core_.getXPosition(xyStage_);
		currypos_ = core_.getYPosition(xyStage_);
		currzpos_ = core_.getPosition(zStage_);
	}
	public void setXPosition(double xpos) throws Exception {

		core_.setXYPosition(xyStage_, xpos, core_.getYPosition(xyStage_));
		TimeUnit.MILLISECONDS.sleep(preferences_.sleeptime_);
	}

	public void setYPosition(double ypos) throws Exception {

		core_.setXYPosition(xyStage_, core_.getXPosition(xyStage_), ypos);
		TimeUnit.MILLISECONDS.sleep(preferences_.sleeptime_);
	}

	public void setZPosition(double zpos) throws Exception {

		core_.setPosition(zStage_, zpos);
		TimeUnit.MILLISECONDS.sleep(preferences_.sleeptime_);
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
			if(item.isdelete_)continue;
			try {
				item.dataClean();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}		

		}
	}



	private void testing() throws Exception {// to verify if this stuff

		int len = (int) (preferences_.zCalRange_/preferences_.zCalStep_);
		for (int i = 1; i < len-4; i++) {// get XYZPostion
			setZPosition(calPosZ_[i]);
			double zpos = core_.getPosition(zStage_);
			debugMessage(String.format("Set z position done: %f", zpos));
			gui_.snapSingleImage();
			Object pix = core_.getTaggedImage().pix;

			Kernel.getInstance().getPosition(pix,i,"test",0);
			reDraw(MMStudioMainFrame.SIMPLE_ACQ, i,true);
		}
		setZPosition(currzpos_);// turn back to the first
		logMessage("Test over ");
	}

	// -----------------------------------------------------------------------------------------DEBUG
	void logError(String string) {
		System.out.print(String.format("Err!!!\t%s\r\n",string));
		ZIndexMeasureFrame.getInstance().infomation_.setText(string);
	}
	void logMessage(String string) {
		System.out.print(String.format("Msg>>\t%s\r\n",string));
		ZIndexMeasureFrame.getInstance().infomation_.setText(string);
	}
	void debugMessage(String string) {
		System.out.print(String.format("Msg>>%s\t\r\n",string));

	}
	public void PullMagnet() {
		(new Thread(new Runnable() {
			private String focusStage_  = "MP285 Z Stage";

			@Override
			public void run() {
				try {
//					if (!core_.deviceBusy(focusStage_)){
						double currMP285zpos = core_.getPosition(focusStage_);
						double z = preferences_.magnetMoveStep_;
						z = Math.abs(z);
						
						core_.setPosition(focusStage_, currMP285zpos - z);

						logMessage(String.format("Set MP285 ZStage\t%f\t->\t%f",
								currMP285zpos , core_.getPosition(focusStage_)));
//					}
				} catch (Exception e) {
					logMessage("Set MP285 ZStage ERR" + e.toString());
				}
			}
		})).start();
	}



	public synchronized double[][] getRoiList(){
		int size = roiList_.size();
		double[][] roiList = new double[size][2];//x,y,z,fx,fy,isSelected
		for (int i = 0; i <size; i++) {
			if(roiList_.get(i).isdelete_)continue;
			roiList[i][0] = roiList_.get(i).x_;
			roiList[i][1] = roiList_.get(i).y_;
		}
		return roiList;
	}

	public synchronized int getFocusRoiIndex() {
		int index =0;
		if (roiList_ == null || roiList_.size() == 0)
			return -1;
		Iterator<RoiItem> it = roiList_.iterator();
		while (it.hasNext()) {		
			RoiItem item = it.next();			
			if(item.isdelete_){
				index++;	
				continue;
			}
			if(item.isFocus_){
				return index;
			}
			index++;		
		}
		return -1;
	}

	public synchronized boolean overlapCheck(int xCenter,int yCenter) {
		if (roiList_ == null || roiList_.size() == 0)
			return false;

		Iterator<RoiItem> it = roiList_.iterator();
		while (it.hasNext()) {		
			RoiItem item = it.next();
			if(item.isdelete_)continue;
			int x = (int) item.x_;
			int y = (int) item.y_;
			int dx = xCenter - x;
			int dy = yCenter - y;
			double r = Math.sqrt(dx*dx + dy*dy);			
			if(r <2 * (int) preferences_.beanRadiusPiexl_){
				return true;
			}		
		}
		return false;
	}

	public synchronized int getCurrentRoiIndex(Point loc) {
		int index =0;
		if (roiList_ == null || roiList_.size() == 0)
			return -1;

		Iterator<RoiItem> it = roiList_.iterator();
		int beanRadius = (int) preferences_.beanRadiusPiexl_;
		while (it.hasNext()) {		
			RoiItem item = it.next();
			if(item.isdelete_){
				index++;	
				continue;
			}
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

	public synchronized int getReferenceRoiIndex() {
		int index =0;
		if (roiList_ == null || roiList_.size() == 0)
			return -1;

		Iterator<RoiItem> it = roiList_.iterator();
		while (it.hasNext()) {		
			RoiItem item = it.next();			
			if(item.isdelete_){
				index++;	
				continue;
			}
			if(item.isSelected_){
				return index;
			}
			index++;		
		}
		return -1;
	}

	public synchronized void reSetFocusRoi() {
		int focus = getFocusRoiIndex();
		if(focus != -1){
			int xCenter = (int) roiList_.get(focus).x_;	
			int yCenter = (int) roiList_.get(focus).y_;	
			int beanRadius = (int) preferences_.beanRadiusPiexl_;
			ImagePlus ip = WindowManager.getCurrentImage();
			if(ip != null)
				ip.setRoi(xCenter-beanRadius,yCenter-beanRadius,2*beanRadius,2*beanRadius);
		}
	}


	public synchronized void setFocusdRoi(Point point) {
		int focus = getFocusRoiIndex();
		if(focus != -1){
			roiList_.get(focus).isFocus_ = false;
		}

		int index = getCurrentRoiIndex(point);
		if( index != -1){
			roiList_.get(index).isFocus_ = true;
		}
	}

	public synchronized void moveFocusdRoi(int dx, int dy) {
		int index = getFocusRoiIndex();
		if( index != -1){
			roiList_.get(index).x_ += dx;
			roiList_.get(index).y_ += dy;
		}	
	}

	public synchronized void addRoi(Rectangle rectangle) {
		int xCenter = rectangle.x+rectangle.width/2;
		int yCenter = rectangle.y+rectangle.height/2;

		if(!overlapCheck(xCenter,yCenter))//selected area exist ROI
		{	
			int index = getFocusRoiIndex();
			if(index != -1)
			{
				roiList_.get(index).isFocus_ = false;
			}
			int num = roiList_.size();
			String  titleName = String.format("bean-%d-data",num+1);
			roiList_.add(RoiItem.createInstance(new double[]{xCenter,yCenter,0,0,0,},false,titleName));

		}
	}

	public synchronized void deleteRoi() {
		int index = getFocusRoiIndex();
		if( index != -1){
			if(preferences_.isCalibrated_){
				try {
					roiList_.get(index).isdelete_ = true;
					Kernel.getInstance().deleteRoi(index);
					roiList_.get(index).dataClean();					
				} catch (IOException e) {
					logError("Write data error!");
				}
			}else{
				roiList_.remove(index);
			}
			WindowManager.getCurrentImage().setRoi(new Rectangle(0,0,0,0));

		}	
	}

	public synchronized void selectRoiAsReference() {
		int index = getFocusRoiIndex();
		if( index != -1){
			int preReferenceIndex = getReferenceRoiIndex();
			if(preReferenceIndex != -1){
				roiList_.get(preReferenceIndex).setSelect(false);
			}
			roiList_.get(index).setSelect(true);
		}
	}

	public synchronized void showChartManager() {
		int focus = getFocusRoiIndex();
		if(focus != -1){
			roiList_.get(focus).chart_.setVisible(true);
		}

	}
	public void updateRoiList(int i, double xpos, double ypos,double intesty) {
		roiList_.get(i).x_ = xpos;
		roiList_.get(i).y_ = ypos;
		double[] pixelToPhys = preferences_.pixelToPhys_;
		double xPhys = pixelToPhys[0] + pixelToPhys[1] * xpos;
		double yPhys = pixelToPhys[2] + pixelToPhys[3] * ypos;
		roiList_.get(i).xPhy_ = xPhys;
		roiList_.get(i).yPhy_ = yPhys;
		roiList_.get(i).z_ = intesty;
		

	}
	public synchronized void reDraw(String acqName, long frameNum_, boolean update) throws MMScriptException {
		reSetFocusRoi();
		render_.render(acqName, roiList_, frameNum_, update);
	}

	public void showPreferencesDialog() {

		ZIndexMeasureFrame.getInstance().preferDailog.setVisible(true);
	}
	public void setScale() {
		// TODO Auto-generated method stub

	}

	public void installCallback() {
		if (preferences_.isInstalCallback_) {
			gui_.getAcquisitionEngine().removeImageProcessor(AcqAnalyzer.getInstance());
			preferences_.isInstalCallback_ = false;
			ZIndexMeasureFrame.getInstance().LiveView.setText("StopCapture");
			logMessage("Call back uninstal,Stop capture");
		} else {
			gui_.getAcquisitionEngine().addImageProcessor(AcqAnalyzer.getInstance());
			preferences_.isInstalCallback_ = true;
			ZIndexMeasureFrame.getInstance().LiveView.setText("LiveView");
			logMessage("Call back install,Start capture");
		}
	}

	public void multiAcq() {
		gui_.getAcqDlg().setVisible(true);
	}
	private void updateCalPos(){
		int len = (int) (preferences_.zCalRange_/preferences_.zCalStep_);
		calPosZ_ = new double[len];
		calPosXY_ = new double[2][];
		for (int i = 0; i < len; i++) {
			calPosZ_[i] = (currzpos_*1e3 - preferences_.zCalRange_ / 2 + i * preferences_.zCalStep_)/1e3;
		}

		// Set the x/y positions
		double[] xyStartPoint = new double[] { currxpos_,
				currypos_ };
		int midPoint = len / 2;
		for (int i = 0; i < 2; i++) {
			calPosXY_[i] = new double[len];
			for (int j = 0; j < midPoint; j++)
				calPosXY_[i][j] = xyStartPoint[i] + preferences_.xyCalRange_ / midPoint * j;
			for (int j = midPoint; j < len; j++)
				calPosXY_[i][j] = calPosXY_[i][midPoint - 1] - preferences_.xyCalRange_
				/ midPoint * (j - midPoint);
		}

	}
	public void calibrate() {
		if(roiList_ ==null ||roiList_.size() <= 0)return;
		logMessage("Calibration Start......");

		if (gui_.getAcquisitionEngine().isAcquisitionRunning()) {
			gui_.getAcquisitionEngine().stop(true);
		}

		if (gui_.isLiveModeOn()) {
			gui_.enableLiveMode(false);
		}
		if(preferences_.isInstalCallback_)
			installCallback();

		try {
			updatePositions();
			updateCalPos();
			regrX = new SimpleRegression();
			regrY = new SimpleRegression();
			for (int z = 0; z < calPosZ_.length; z++) {
				// Calibration on X/Y/Z
				core_.setXYPosition(xyStage_, calPosXY_[0][z], calPosXY_[1][z]);
				setZPosition(calPosZ_[z]);

				double temp = core_.getPosition(zStage_);
				double[] tempX = new double[1];
				double[] tempY = new double[1];
				core_.getXYPosition(xyStage_, tempX, tempY);
				debugMessage(String.format("xpos:\t%f\typos:\t%f\tzpos:\t%f\r\n", tempX[0], tempY[0],calPosZ_[z]));
				gui_.snapSingleImage();

				Object pix = core_.getTaggedImage().pix;
			 
				 
				double[] ret_ = Kernel.getInstance().calibration(pix, z,temp);
				// XY calibration
				regrX.addData(ret_[0], tempX[0]);
				regrY.addData(ret_[1], tempY[0]);
			}
			core_.setXYPosition(xyStage_, currxpos_, currypos_);
			setZPosition(currzpos_);// turn back to the first place,Always 5

			double[] vec = new double[4];
			vec[0] = regrX.getIntercept();
			vec[1] = regrX.getSlope();
			vec[2] = regrY.getIntercept();
			vec[3] = regrY.getSlope();
			preferences_.pixelToPhys_ = vec;


			gui_.snapSingleImage();
			logMessage("Calibration OK......");
			ZIndexMeasureFrame.getInstance().LiveView.setEnabled(true);
			ZIndexMeasureFrame.getInstance().MACQ.setEnabled(true);
			testing();
			
			for(int  i = 0;i < roiList_.size();i++){
				roiList_.get(i).chart_.setVisible(true);
			}
			preferences_.isCalibrated_ = true;
			preferences_.resetData_ = true;
			if(!preferences_.isInstalCallback_)
				installCallback();
		} catch (Exception e) {
			logError("Calibration False!\t" + e.toString());
		}
	}



	public void liveView() {

		if(gui_.isLiveModeOn()){
			gui_.enableLiveMode(false);
		}
		else{
			gui_.enableLiveMode(true);
		}
	}

	public synchronized int getRoiSize() {
		// TODO Auto-generated method stub
		return roiList_.size();
	}

	public synchronized boolean isRoidelete(int i) {
		// TODO Auto-generated method stub
		return roiList_.get(i).isdelete_;
	}

	public  synchronized DescriptiveStatistics[] getStats(int i) {
		// TODO Auto-generated method stub
		return roiList_.get(i).stats_;
	}
	public  synchronized DescriptiveStatistics getStatCross(int i) {
		// TODO Auto-generated method stub
		return roiList_.get(i).statCross_;
	}

	public synchronized void saveValueAndUpdate(int i,String fileName,long frameNum_,double eclipsed,  double[] ret) throws IOException {
		if(getRoiSize() <= 0)return;
		roiList_.get(i).setItemData(ret);	
		String filename = String.format("%s_bean_%d", fileName,i);
		roiList_.get(i).writeData(preferences_.userDataDir_,filename ,frameNum_, eclipsed);
	
		roiList_.get(i).chart_.getDataSeries().get("Chart-X").add(frameNum_,ret[1]*1000);
		roiList_.get(i).chart_.getDataSeries().get("Chart-Y").add(frameNum_,ret[3]*1000);
		roiList_.get(i).chart_.getDataSeries().get("Chart-Z").add(frameNum_,ret[4]*1000);
		roiList_.get(i).chart_.getDataSeries().get("Chart-FX").add(frameNum_,ret[5]);
		roiList_.get(i).chart_.getDataSeries().get("Chart-FY").add(frameNum_,ret[6]);
		roiList_.get(i).chart_.getDataSeries().get("Chart-STDXDY").add(frameNum_,ret[7]);
		roiList_.get(i).chart_.getDataSeries().get("Chart-SKREWNESS").add(frameNum_,ret[8]);
		
		if (isEnoughDataToCalcForce())
		{
		double meanx = roiList_.get(i).stats_[0].getMean();
		double meany = roiList_.get(i).stats_[1].getMean();
		double meanz = roiList_.get(i).stats_[2].getMean();
		
		double stdx = roiList_.get(i).stats_[0].getStandardDeviation();
		double stdy = roiList_.get(i).stats_[1].getStandardDeviation();
		double stdz = roiList_.get(i).stats_[2].getStandardDeviation();
		
		roiList_.get(i).chart_.getChartSeries().get("Chart-X").getXYPlot().getRangeAxis().setRange(meanx - stdx*3,meanx + stdx*3);
		roiList_.get(i).chart_.getChartSeries().get("Chart-Y").getXYPlot().getRangeAxis().setRange(meany - stdy*3,meany + stdy*3);
		roiList_.get(i).chart_.getChartSeries().get("Chart-Z").getXYPlot().getRangeAxis().setRange(meanz - stdz*3,meanz + stdz*3);
		}
	}
	synchronized boolean isEnoughDataToCalcForce() {
		return roiList_.get(0).stats_[0].getN() >= preferences_.minAnalyzeWindow_;
	}
	synchronized void addRoiStatsValue(int i,double xPhys,double yPhys, double zPhys) {
		roiList_.get(i).stats_[0].addValue(xPhys * 1000);
		roiList_.get(i).stats_[1].addValue(yPhys * 1000);
		roiList_.get(i).stats_[2].addValue(zPhys * 1000);
		roiList_.get(i).statCross_.addValue(xPhys * yPhys * 1e6);
	}

	public synchronized void getPosition(Object pix, long frameNum_,String fileName,double elapsed) {
		Kernel.getInstance().getPosition(pix,frameNum_,fileName,elapsed);		
	}

	public void ShowMagnetManualDialBox() {
		ZIndexMeasureFrame.getInstance().MagnetAuto.setSelected(false);
		ZIndexMeasureFrame.getInstance().myStageControlFrame_.setVisible(true);
	}

	public void setAutoContrast() {
		try {
			gui_.setContrastBasedOnFrame(preferences_.acqName_, 0, 0);
		} catch (MMScriptException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

	public void liveCapture() {
		gui_.enableLiveMode(false);
		installCallback();
		gui_.enableLiveMode(true);
	}

	public void showGui() {
		gui_.setVisible(!gui_.isVisible());
	}
	public void showIJ() {
		ij.ImageJ.getFrames()[0].setVisible(!ij.ImageJ.getFrames()[0].isVisible());
	}

}
