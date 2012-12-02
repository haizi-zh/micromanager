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


	private double[] forceOpt_ = null;
	private double DNALen_;
	private double Temperature_;
	private double DNAPersLen_;


	// ROI
	public Rectangle roi_rectangle;
	public int[] calcRoi_ = null;
	private int roix_ = 0;
	private int roiy_ = 0;
	private int roiwidth_ = 40;
	private int roiheight_ = 40;
	public double FrameCalcForce_ = 1000;
	public int Mstep_ = 100;// um
	// other

	public int currFrame = 1;
	public long sleeptime_ = 20;
	public int Frame2Acq_ = 100000;
	public int TimeIntervals_ = 5;
	double[] calPos_;

	// XY calibration positions
	double[][] calPosXY_;

	public boolean F_L_Flag_ = false;



	private long begin;
	final Object lock = new Object();
	public MyForm myForm_;

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				MyGUI frame = new MyGUI(mainInstance_);				 			 
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
		if (roi_rectangle.x + getRadius_() * 2 + 5 >= this.Imgwidth_)
			return false;
		if (roi_rectangle.y + getRadius_() * 2 + 5 >= this.Imgheight_)
			return false;

		return true;
	}

	public void reSetROI(int CenterX, int CenterY) {

		int ROIBounder = 10;
		int BallSqrt = (int) ((ROIBounder + getRadius_()) * 2);
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

	public MyGUI(ZIndexMeasure mainInstance) {
		//GUIInitialization();
		mainInstance_ = mainInstance;
		instance_ = this;

		myForm_ = new MyForm(this);
		myForm_.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);	
		myForm_.setVisible(true);
		
		setDefaultPrefer(myForm_.preferDailogBox.getPreferData());
		SetScale(myForm_.preferDailogBox.getPreferData());
		roi_rectangle = new Rectangle();
		calcOpt_ = new double[] { getRadius_(), RInterpStep_, BitDepth_,
				HalfQuadWindow_, Imgwidth_, Imgheight_, ZStart_, ZScale_,
				ZStep_, getDNALen_(), Temperature_, DNAPersLen_, FrameCalcForce_ };
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
			FrameCalcForce_ = preferData.get("FrameCalcF");
			Temperature_ = 300;// K
			DNAPersLen_ = 50;// nm

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

			// calculate force
			setDNALen_(2.4);// um
			Temperature_ = 300;// K
			DNAPersLen_ = 50;// nm
		}


	}


	public void SetScale(HashMap<String, Double> hashMap) {
		myForm_.log("SetScale Start......");
		if (WindowManager.getCurrentImage() == null) {
			myForm_.log("god work without image");
			return;
		}
		if (WindowManager.getCurrentImage().getRoi() == null) {
			myForm_.log("please set ROI,the tool is locate in the imagej main frame");
			return;
		}

		try {
			mainInstance_.updatePositions();
		} catch (Exception e) {
			myForm_.log(e.toString());
			return;
		}

		roi_rectangle = WindowManager.getCurrentImage().getRoi().getBounds();
		int CenterX = roi_rectangle.x + roi_rectangle.width / 2;
		int CenterY = roi_rectangle.y + roi_rectangle.height / 2;
		reSetROI(CenterX, CenterY);

		currFrame = 1;

		ImageProcessor ip = WindowManager.getCurrentImage().getProcessor();
		Imgwidth_ = ip.getWidth();
		Imgheight_ = ip.getHeight();
		BitDepth_ = WindowManager.getCurrentImage().getBitDepth();


		ZStart_ = mainInstance_.currzpos_ - ZScale_ / 2;
		mainInstance_.getProcessor().setBaseDir(myForm_.preferDailogBox.getDataDir_());

		F_L_Flag_ = myForm_.isMagnetAuto();

		reSetOpt();
		setCalProfile();

		mainInstance_.isSetScale = true;
		mainInstance_.isCalibrated = false;

	}

	private void setCalProfile() {
		int nSteps = (int) (ZScale_ / ZStep_);
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
		mainInstance_.mCalc.DataInit(calcOpt_);
	}
	
	// opt_[13]
	// :radius,rInterStep,bitDepth,halfQuadWidth,imgWidth,imgHeight,zStart,zScale,zStep
	// ��DNALen��Temperature��DNAPersLen,frame2calcForce
	private void reSetOpt() {
		calcOpt_[0] = getRadius_();
		calcOpt_[1] = RInterpStep_;
		calcOpt_[2] = BitDepth_;
		calcOpt_[3] = HalfQuadWindow_;
		calcOpt_[4] = Imgwidth_;
		calcOpt_[5] = Imgheight_;
		calcOpt_[6] = ZStart_;
		calcOpt_[7] = ZScale_;
		calcOpt_[8] = ZStep_;
		// force
		calcOpt_[9] = getDNALen_();
		calcOpt_[10] = Temperature_;
		calcOpt_[11] = DNAPersLen_;
		calcOpt_[12] = FrameCalcForce_;
	}
	
	public double getCurrCenter(int currTab) {
		// TODO Auto-generated method stub
		return 0;
	}

	public void calibrate() {
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
		// TODO Auto-generated method stub
		print(String.format("MultiAcq--%s",this.myForm_.isMagnetAuto()?"True":"False"));
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


	public void setDNALen_(double dNALen_) {
		DNALen_ = dNALen_;
	}
}
