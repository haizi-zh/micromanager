package org.ndaguan.micromanager;

import ij.WindowManager;
import ij.process.ImageProcessor;

import java.awt.Rectangle;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
//import org.ndaguan.study.std_MyGUI;
import org.micromanager.MMStudioMainFrame;
import org.micromanager.utils.MMException;

public class MyGUI {
	// main instance
	private static ZIndexMeasure mainInstance_ = null;
	private static MyGUI instance_;
	public int MAX_LEN = 200000;

	// opt:Radius,RInterstep,bitDepth,halfQuadWidth,imgWidth,imgHeight,zStart,zScale,zStep
	private double[] calcOpt_ = null;
	private double Radius_ ;
	private double RInterpStep_;
	private double BitDepth_;
	private double HalfQuadWindow_;
	private double Imgwidth_;
	private double Imgheight_;
	private double ZStart_;
	private double ZScale_;
	private double xyCalRange_;
	private double ZStep_;


	 

	// ROI
	public Rectangle roi_rectangle;
	public int[] calcRoi_ = null;
	private int roix_ = 0;
	private int roiy_ = 0;
	private int roiwidth_ = 40;
	private int roiheight_ = 40;
	public double movingWindowLen_ = 1000;
	public int Mstep_ = 100;// um
	// other

	public int currFrame = 1;
	public long sleeptime_ = 20;
	public int Frame2Acq_ = 100000;
	public int TimeIntervals_ = 5;
	double[] calPos_;

	// XY calibration positions
	double[][] calPosXY_;

	private long begin;
	final Object lock = new Object();
	public MyForm myForm_;
	private double ballRadiusPix = 100.0;
	private boolean isInstall = false;
	private MMStudioMainFrame gui_;
	private int calPosLen;
	private double DNALen_;
	private double zInterpStep_;
	 

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				//	MyGUI frame = new MyGUI(mainInstance_,gui_);				 			 
			}
		});

	}


	public static MyGUI getInstance() {
		return instance_;
	}

	public Boolean IsBallInImg() {
		if (roi_rectangle.x <= 5)
			return false;
		if (roi_rectangle.y <= 5)
			return false;
		if (roi_rectangle.x + 2*getRadius_()*ballRadiusPix + 5 >= this.Imgwidth_)
			return false;
		if (roi_rectangle.y + 2*getRadius_()*ballRadiusPix + 5 >= this.Imgheight_)
			return false;

		return true;
	}

	public void reSetROI(int CenterX, int CenterY) {

		int ROIBounder = 10;
		int BallSqrt = (int) ((ROIBounder + getRadius_()*ballRadiusPix ) * 2);
		roi_rectangle.width = BallSqrt;
		roi_rectangle.height = BallSqrt;
		roi_rectangle.x = (int) (CenterX - BallSqrt / 2);
		roi_rectangle.y = (int) (CenterY - BallSqrt / 2);

		if (!this.IsBallInImg()) {
			myForm_.log("THE BALL IS OUT OF THIS IMAGE,I GONA GIVE UP TRACKING....");
			return;
		}
		calcRoi_[0] = roi_rectangle.x;
		calcRoi_[1] = roi_rectangle.y;
		calcRoi_[2] = roi_rectangle.width;
		calcRoi_[3] = roi_rectangle.height;
		WindowManager.getCurrentImage().setRoi(roi_rectangle);
	}

	public MyGUI(ZIndexMeasure mainInstance,MMStudioMainFrame gui) {
		//GUIInitialization();
		mainInstance_ = mainInstance;
		gui_ = (MMStudioMainFrame) gui;
		instance_ = this;

		myForm_ = new MyForm(this);
		myForm_.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);	
		myForm_.setVisible(true);

		setDefaultPrefer(myForm_.preferDailogBox.getPreferData());
		roi_rectangle = new Rectangle();
		
		setCalcOpt_(new double[] { getRadius_(), RInterpStep_, BitDepth_,
				HalfQuadWindow_, Imgwidth_, Imgheight_, ZStart_, ZScale_,
				ZStep_,movingWindowLen_,zInterpStep_ });
		calcRoi_ = new int[] { roix_, roiy_, roiwidth_, roiheight_ };

	}


	private void setDefaultPrefer(HashMap<String, Double> preferData) {
		if(preferData != null){
			setRadius_(preferData.get("BallRadius"));
			RInterpStep_ = preferData.get("RinterStep");
			HalfQuadWindow_ = preferData.get("HalfCorrWin");		 
			ZScale_ = preferData.get("ZCalScale");
			ZStep_ = preferData.get("ZCalStep");
			setDNALen_(preferData.get("DNALength"));		
			movingWindowLen_ = preferData.get("FrameCalcF");
			ballRadiusPix = preferData.get("ballRadiusPix");
			zInterpStep_ = preferData.get("ITEM1");
			ZStart_ = 0;
			xyCalRange_ = 2;
		}else{	  
			setRadius_(75);
			RInterpStep_ = 0.5;
			BitDepth_ = 16;
			HalfQuadWindow_ = 2;
			Imgwidth_ = 640;
			Imgheight_ = 480;
			ZStart_ = 0;
			ZScale_ = 4;
			xyCalRange_ = 2;
			ZStep_ = 0.5;
			setDNALen_(2.4);// um
			movingWindowLen_ = 500;
			ballRadiusPix = 50.0;
			zInterpStep_ = 5;
		}


	}


	public void SetScale() {
		myForm_.log("SetScale Start......");
		if (WindowManager.getCurrentImage() == null) {
			myForm_.log("god work without image");
			return;
		}
		if (WindowManager.getCurrentImage().getRoi() == null) {
			myForm_.log("please set ROI");
			return;
		}

		try {
			mainInstance_.updatePositions();
		} catch (Exception e) {
			myForm_.log(e.toString());
			return;
		}



		currFrame = 1;
		setDefaultPrefer(myForm_.preferDailogBox.getPreferData());
		ImageProcessor ip = WindowManager.getCurrentImage().getProcessor();
		Imgwidth_ = ip.getWidth();
		Imgheight_ = ip.getHeight();
		BitDepth_ = WindowManager.getCurrentImage().getBitDepth();


		ZStart_ = mainInstance_.currzpos_ - ZScale_ / 2;
		mainInstance_.getProcessor().setBaseDir(myForm_.preferDailogBox.getDataDir_());

		roi_rectangle = WindowManager.getCurrentImage().getRoi().getBounds();
		int CenterX = roi_rectangle.x + roi_rectangle.width / 2;
		int CenterY = roi_rectangle.y + roi_rectangle.height / 2;
		reSetROI(CenterX, CenterY);

		reSetOpt();
		setCalProfile();

		mainInstance_.isSetScale = true;
		mainInstance_.isCalibrated = false;

	}

	private void setCalProfile() {
		int nSteps = (int) (ZScale_ / ZStep_);
		this.calPosLen = nSteps;
		double[] calPosZ = new double[nSteps];
		double[][] calPosXY = new double[2][];
		for (int i = 0; i < nSteps; i++) {
			calPosZ[i] = mainInstance_.currzpos_ - ZScale_ / 2 + i * ZStep_;
		}

		// Set the x/y positions
		double[] xyStartPoint = new double[] { mainInstance_.currxpos_,
				mainInstance_.currypos_ };
		int midPoint = nSteps / 2;
		for (int i = 0; i < 2; i++) {
			calPosXY[i] = new double[nSteps];
			for (int j = 0; j < midPoint; j++)
				calPosXY[i][j] = xyStartPoint[i] + xyCalRange_ / midPoint * j;
			for (int j = midPoint; j < nSteps; j++)
				calPosXY[i][j] = calPosXY[i][midPoint - 1] - xyCalRange_
				/ midPoint * (j - midPoint);
		}

		mainInstance_.updateCalPos(calPosXY, calPosZ);
	}

	// opt_[13]
	// :radius,rInterStep,bitDepth,halfQuadWidth,imgWidth,imgHeight,zStart,zScale,zStep
	// ��DNALen��Temperature��DNAPersLen,frame2calcForce
	private void reSetOpt() {
		getCalcOpt_()[0] = ballRadiusPix;
		getCalcOpt_()[1] = RInterpStep_;
		getCalcOpt_()[2] = BitDepth_;
		getCalcOpt_()[3] = HalfQuadWindow_;
		getCalcOpt_()[4] = Imgwidth_;
		getCalcOpt_()[5] = Imgheight_;
		getCalcOpt_()[6] = ZStart_;
		getCalcOpt_()[7] = ZScale_;
		getCalcOpt_()[8] = ZStep_;
		// force		
		getCalcOpt_()[9] = movingWindowLen_;
		getCalcOpt_()[10] = zInterpStep_;
	}

	public double getCurrCenter(int currTab) {
		// TODO Auto-generated method stub
		return 0;
	}

	public void calibrate() {
		mainInstance_.mCalc.DataInit(this.calcOpt_);
		myForm_.getTabbedPane().setSelectedIndex(2);
		mainInstance_.StartCalibration();
		
		mainInstance_.InstallCallback();
	}


	private void print(String format) {
		// TODO Auto-generated method stub
		System.out.println(format);
	}

	public void live() {
		if (mainInstance_.gui_.getAcquisitionEngine()
				.isAcquisitionRunning()) {
			myForm_.log("Acquistion is running,mission abort!");
			return;
		}
		if (mainInstance_.gui_.isLiveModeOn()) {
			mainInstance_.gui_.enableLiveMode(false);
		} else {
			mainInstance_.gui_.enableLiveMode(true);
		}
	}

	public void MultiAcq() {
		gui_.getAcquisitionEngine().enableFramesSetting(true);
		gui_.getAcquisitionEngine().setSaveFiles(true);
		//		gui_.getAcquisitionEngine().setRootName(mygui_.StoragePath_);
		//		gui_.getAcquisitionEngine().setFrames(mygui_.Frame2Acq_,
		//				mygui_.TimeIntervals_);

		try {
			gui_.getAcquisitionEngine().acquire();
		} catch (MMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


	public double getRadius_() {
		return Radius_;
	}


	public void setRadius_(double radius_) {
		Radius_ = radius_;
	}


	public double getDNALen_() {
		return DNALen_;
	}
	public void installCallback() {
		if(!isInstall ){
			myForm_.log("Install");
			mainInstance_.InstallCallback();
			myForm_.getTabbedPane().setSelectedIndex(2);
			isInstall = true;
		}
		else
		{	
			myForm_.log("UnInstall");
			mainInstance_.UninstallCallback();
			myForm_.getTabbedPane().setSelectedIndex(0);
			isInstall = false;
		}
	}


	public void setDNALen_(double dNALen_) {
		DNALen_ = dNALen_;
	}


	public int getcalPosLen() {
		return calPosLen;
	}


	public double[] getCalcOpt_() {
		return calcOpt_;
	}


	public void setCalcOpt_(double[] calcOpt_) {
		this.calcOpt_ = calcOpt_;
	}
}
