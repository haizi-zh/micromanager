package org.ndaguan.micromanager;

import ij.ImagePlus;
import ij.WindowManager;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import mmcorej.CMMCore;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.micromanager.MMStudioMainFrame;
import org.ndaguan.micromanager.OverlayRender.RenderItem;

public class Function {

	private static Function instance_;
	private MMStudioMainFrame gui_;
	private ZIndexMeasureFrame myFrame_;
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
	private ArrayList<RenderItem> roiList_;
	private boolean isCalibration;

	public Function(MMStudioMainFrame gui,ArrayList<RenderItem> roiList, Preferences preferences) {
		core_ = gui.getMMCore();
		gui_ = gui;
		roiList_ = roiList;
		preferences_ =  preferences;
	}

	public static Function getInstance(MMStudioMainFrame gui,ArrayList<RenderItem> roiList, Preferences preferences) {
		if(instance_ == null)
			instance_ = new Function(gui,roiList,preferences);

		return instance_;
	}

	public void updateCalPos(double[][] xy, double[] z) {
		calPosZ_ = z;
		calPosXY_ = xy;
	}

	public void updatePositions() throws Exception {
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

	public void setRXPosition(double xpos) throws Exception {

		core_.setRelativeXYPosition(xyStage_, xpos, 0);
		TimeUnit.MILLISECONDS.sleep(preferences_.sleeptime_);
	}

	public void setRYPosition(double ypos) throws Exception {

		core_.setRelativeXYPosition(xyStage_, 0, ypos);
		TimeUnit.MILLISECONDS.sleep(preferences_.sleeptime_);
	}

	public void setRZPosition(double zpos) throws Exception {

		core_.setRelativePosition(zStage_, zpos);
		TimeUnit.MILLISECONDS.sleep(preferences_.sleeptime_);
	}	

	void dataReset() {
		if(Listener.getInstance().isRunning()){
			Listener.getInstance().stop();
		}		
	}



	public double[][] getRoiList(){
		int size = roiList_.size();
		double[][] roiList = new double[size][6];//x,y,z,fx,fy,isSelected
		for (int i = 0; i <size; i++) {
			roiList[i][0] = roiList_.get(i).itemdata_.x_;
			roiList[i][1] = roiList_.get(i).itemdata_.y_;
			if(roiList_.get(i).isSelected_){
				roiList[i][5] = 1;
			}
			else{
				roiList[i][5] = 0;
			}
		}
		return roiList;
	}
	public void StartCalibration() {
		myFrame_.log("Calibration Start......");
		if (!isSetScale) {
			myFrame_.log("statu\tfalse\tfalseSetscale first!");
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
			OverlayRender render = OverlayRender.getInstance();

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



				double[] outpos = new double[2];
				gui_.snapSingleImage();
				Object pix = core_.getTaggedImage().pix;
				double[] ret_ = null;//Kernel.getInstance().calibrate(pix, z);
				outpos[0] = ret_[0];
				outpos[1] = ret_[0];
				///////////////for

				render.render(MMStudioMainFrame.SIMPLE_ACQ, roiList_, 0, true);

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
			preferences_.pixelToPhys_ = vec;

			gui_.logMessage(String.format("Xx: %s", Arrays.toString(loc[0])));
			gui_.logMessage(String.format("Xy: %s",
					Arrays.toString(stagePos[0])));
			gui_.logMessage(String.format("Yx: %s", Arrays.toString(loc[1])));
			gui_.logMessage(String.format("Yy: %s",
					Arrays.toString(stagePos[1])));
			gui_.logMessage(String.format("XY Calibration: %s",
					Arrays.toString(vec)));

			gui_.snapSingleImage();
			ZIndexMeasureFrame.getInstance().log("Calibration OK......");

			testing();
			preferences_.isCalibrated_ = true;
			preferences_.resetData_ = true;
		} catch (Exception e) {
			ZIndexMeasureFrame.getInstance().log("Calibration False!\t" + e.toString());
			e.printStackTrace();
		}
	}

	private void testing() throws Exception {// to verify if this stuff
		// workable
		ZIndexMeasureFrame.getInstance().log("Test begin......checking out the Cart-Z for more information.");
		//IJ.log(String.format("Testing:\r\n#index,#real,#get,#delta"));	
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				//zDataSeries_.clear();
			}
		});
		int len = preferences_.calPosLen_;

		for (int i = 1; i < len-4; i++) {// get XYZPostion
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
					//	zDataSeries_.add(zpos, zpos - pos[2]);
					gui_.logMessage(String.format(
							"i=%d, zpos=%f, pos[2]=%f, delta=%f", index, zpos,
							pos[2], zpos - pos[2]));
				}
			});
		}
		setZPosition(currzpos_);// turn back to the first
		// place,Always 5

		ZIndexMeasureFrame.getInstance().log("Test over ");
	}

	// -----------------------------------------------------------------------------------------DEBUG
	public double[] getXYZPositon() throws Exception {
		gui_.logMessage("Localization...");
		double[] ret = null;
		Object pix = null;
		pix = core_.getTaggedImage().pix;
		ret = Kernel.getInstance().getPosition(pix);
		return ret;
	}
	public void InstallCallback() {

		if (preferences_.isInstalCallback_) {
			ZIndexMeasureFrame.getInstance().log("statu:false\tCall back is installed! mission abort");
		} else {
			gui_.getAcquisitionEngine().addImageProcessor(AcqAnalyzer.getInstance());
			preferences_.isInstalCallback_ = true;
			ZIndexMeasureFrame.getInstance().log("statu:ok\tCall back install,Start capture...");
		}

	}

	public void UninstallCallback() {

		if (preferences_.isInstalCallback_) {
			gui_.getAcquisitionEngine().removeImageProcessor(AcqAnalyzer.getInstance());
			preferences_.isInstalCallback_ = false;
			ZIndexMeasureFrame.getInstance().log("Call back uninstal,Stop capture");
		} else {
			ZIndexMeasureFrame.getInstance().log("statu:UnInstall Callback false");
		}
	}

	public void PullMagnet() {
		(new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					double currMP285zpos = core_.getPosition("MP285 Z Stage");
					core_.setPosition("MP285 Z Stage", currMP285zpos
							- preferences_.magnetMoveStep_);
					ZIndexMeasureFrame.getInstance().log(String.format("Set MP285 ZStage to:%f",
							currMP285zpos - preferences_.magnetMoveStep_));
				} catch (Exception e) {
					ZIndexMeasureFrame.getInstance().log("Set MP285 ZStage ERR" + e.toString());
				}
			}
		})).start();
	}


	public int getFocusRoiIndex() {
		int index =0;
		if (roiList_ == null || roiList_.size() == 0)
			return -1;
		Iterator<RenderItem> it = roiList_.iterator();
		while (it.hasNext()) {		
			RenderItem item = it.next();			
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

		Iterator<RenderItem> it = roiList_.iterator();
		while (it.hasNext()) {		
			RenderItem item = it.next();
			int x = (int) item.itemdata_.x_;
			int y = (int) item.itemdata_.y_;
			int dx = xCenter - x;
			int dy = yCenter - y;
			double r = Math.sqrt(dx*dx + dy*dy);			
			if(r <2 * (int) preferences_.beanRadiusPiexl_){
				return true;
			}		
		}
		return false;
	}
	public int getCurrentRoiIndex(Point loc) {
		int index =0;
		if (roiList_ == null || roiList_.size() == 0)
			return -1;

		Iterator<RenderItem> it = roiList_.iterator();
		int beanRadius = (int) preferences_.beanRadiusPiexl_;
		while (it.hasNext()) {		
			RenderItem item = it.next();
			int x =(int) item.itemdata_.x_;
			int y = (int) item.itemdata_.y_;

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

		Iterator<RenderItem> it = roiList_.iterator();
		while (it.hasNext()) {		
			RenderItem item = it.next();			
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
			int xCenter = (int) roiList_.get(focus).itemdata_.x_;	
			int yCenter = (int) roiList_.get(focus).itemdata_.y_;	
			int beanRadius = (int) preferences_.beanRadiusPiexl_;
			ImagePlus ip = WindowManager.getCurrentImage();
			if(ip != null)
				ip.setRoi(xCenter-beanRadius,yCenter-beanRadius,2*beanRadius,2*beanRadius);
		}
	}
	public void showPreferencesDialog() {
		ZIndexMeasureFrame.getInstance().preferDailog.frame.setVisible(true);
	}
	public void setScale() {
		// TODO Auto-generated method stub

	}
	public void installCallback() {
		// TODO Auto-generated method stub

	}
	public void multiAcq() {
		// TODO Auto-generated method stub

	}
	public void calibrate() {
		// TODO Auto-generated method stub

	}
	public void liveView() {
		// TODO Auto-generated method stub

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
			roiList_.get(index).itemdata_.x_ += dx;
			roiList_.get(index).itemdata_.y_ += dy;
		}	
	}
	public void addRoi(Rectangle rectangle) {
		int xCenter = rectangle.x+rectangle.width/2;
		int yCenter = rectangle.y+rectangle.height/2;

		if(!overlapCheck(xCenter,yCenter))//selected area exist ROI
		{	
			roiList_.add(RenderItem.createInstance(new double[]{xCenter,yCenter,0,0,0,},false));

		}
	}
	public void deleteRoi() {
		int index = getFocusRoiIndex();
		if( index != -1){
			roiList_.remove(index);
			WindowManager.getCurrentImage().setRoi(new Rectangle(0,0,0,0));
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

	public void onDataChange(Preferences preferences) {
		 if(isCalibration){
			 logErr("data change,you may need to recalibration");
		 }
		 else{
			 Kernel.getInstance().initialize(preferences.getPosOption());
		 }
	}

	private void logErr(String string) {
		// TODO Auto-generated method stub
		
	}

}
