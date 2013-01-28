package org.ndaguan.study;

import ij.IJ;
import ij.WindowManager;
import ij.process.ImageProcessor;

import java.awt.BorderLayout;
import java.awt.Color;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class std_MyGUI{
	// main instance
//	private ZIndexMeasure Maininstance_ = null;
	private static std_MyGUI instance_;
	public int MAX_LEN = 200000;
	// GUI
	private JTextArea LogWindow = new JTextArea(50, 40);
	public JTextField Msg0 = new JTextField(36);
	public JTextField Msg1 = new JTextField(36);
	public JButton Pause;
	public JButton Live;

	public XYSeries dataSeries_;
	// opt:Radius,RInterstep,bitDepth,halfQuadWidth,imgWidth,imgHeight,zStart,zScale,zStep
	public double[] calcOpt_ = null;
	private double Radius_ = 75;
	private double RInterpStep_ = 0.5;
	private double BitDepth_ = 16;
	private double HalfQuadWindow_ = 2;
	private double Imgwidth_ = 640;
	private double Imgheight_ = 480;
	private double ZStart_ = 0;
	private double ZScale_ = 4;
	private double xyCalRange_ = 4;
	private double ZStep_ = 0.5;

	private JTextField Raduis = new JTextField(1);
	private JTextField RInterpStep = new JTextField(1);
	private JTextField HalfCorrWindow = new JTextField(1);
	private JTextField FrameCalcForce = new JTextField(1);
	private JTextField Frame2Acq = new JTextField(1);
	private JTextField TimeIntervals = new JTextField(1);
	private JTextField StoragePath = new JTextField(1);
	// private JTextField SavePath = new JTextField(1);
	private JTextField ZScale = new JTextField(1);
	private JTextField ZStep = new JTextField(1);
	private JTextField DNALen = new JTextField(1);
	private JTextField Mstep = new JTextField(1);
	private JTextField F_L_Flag = new JTextField(1);

	// calculate force
	public double[] forceOpt_ = null;
	private double DNALen_ = 2.4;// um
	private double Temperature_ = 300;// K
	private double DNAPersLen_ = 50;// nm

	// ROI
	public Rectangle roi_rectangle;
	public int[] calcRoi_ = null;
	private int roix_ = 0;
	private int roiy_ = 0;
	private int roiwidth_ = 40;
	private int roiheight_ = 40;

	// other
	public double FrameCalcForce_ = 1000;
	public int currFrame = 1;
	public long sleeptime_ = 20;
	// public String StoragePath_ = "E:/Users/n~daguan/AcquisitionData";
	// public String SavePath_ = "E:/Users/ResultLog.txt";
	public int Frame2Acq_ = 100000;
	public int TimeIntervals_ = 5;
	double[] calPos_;
	// XY calibration positions
	double[][] calPosXY_;
	public int Mstep_ = 100;// um
	public int F_L_Flag_ = 0;

	// public BufferedWriter writer = null;
	public JFreeChart chart = null;
	private int ChartMaxItemCount = 1000;
	private int isPause = 0;
	// Temp
	private long begin;
	final Object lock = new Object();
	private std_MyForm myForm_;

 

	

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				std_MyGUI frame = new std_MyGUI();				 			 
			}
		});

	}
	
	public static std_MyGUI getInstance() {
		return instance_;
	}

 

	public Boolean IsBallInImg() {
		if (roi_rectangle.x <= 5)
			return false;
		if (roi_rectangle.y <= 5)
			return false;
		if (roi_rectangle.x + Radius_ * 2 + 5 >= this.Imgwidth_)
			return false;
		if (roi_rectangle.y + Radius_ * 2 + 5 >= this.Imgheight_)
			return false;

		return true;
	}

	public void reSetROI(int CenterX, int CenterY) {

//		int ROIBounder = 10;
//		int BallSqrt = (int) ((ROIBounder + Radius_) * 2);
//		roi_rectangle.width = BallSqrt;
//		roi_rectangle.height = BallSqrt;
//		roi_rectangle.x = (int) (CenterX - BallSqrt / 2);
//		roi_rectangle.y = (int) (CenterY - BallSqrt / 2);
//
//		if (!this.IsBallInImg()) {
//			log("THE BALL IS OUT OF THIS IMAGE,I GONA GIVE UP TRACKING....");
//			return;
//		}
//		calcRoi_[0] = roi_rectangle.x;
//		calcRoi_[1] = roi_rectangle.y;
//		calcRoi_[2] = roi_rectangle.width;
//		calcRoi_[3] = roi_rectangle.height;
//		WindowManager.getCurrentImage().setRoi(roi_rectangle);
	}

	public std_MyGUI() {
		myForm_ = new std_MyForm(this);
		myForm_.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);	
		myForm_.setVisible(true);	
//
//		instance_ = this;
//		Maininstance_ = ZIndexMeasure.getInstance();
//		roi_rectangle = new Rectangle();
//
//		// Get user home directory
//		calcOpt_ = new double[] { Radius_, RInterpStep_, BitDepth_,
//				HalfQuadWindow_, Imgwidth_, Imgheight_, ZStart_, ZScale_,
//				ZStep_, DNALen_, Temperature_, DNAPersLen_, FrameCalcForce_ };
//		calcRoi_ = new int[] { roix_, roiy_, roiwidth_, roiheight_ };
//
//		LogWindow.setText("Welcome");
//		Raduis.setText(String.format("%f", Radius_));
//		DNALen.setText(String.format("%f", DNALen_));
//		ZScale.setText(String.format("%f", ZScale_));
//		ZStep.setText(String.format("%f", ZStep_));
//		RInterpStep.setText(String.format("%f", RInterpStep_));
//		HalfCorrWindow.setText(String.format("%f", HalfQuadWindow_));
//		FrameCalcForce.setText(String.format("%f", FrameCalcForce_));
//		Frame2Acq.setText(String.format("%d", Frame2Acq_));
//		Mstep.setText(String.format("%d", Mstep_));
//		F_L_Flag.setText(String.format("%d", F_L_Flag_));
//		TimeIntervals.setText(String.format("%d", TimeIntervals_));
//		StoragePath
//				.setText(String.format("%s", System.getProperty("user.home")));
//		// SavePath.setText(String.format("%s", baseDir_));
//
//		Msg0.setText(String.format("Radius =%f  ,Scale = %f, Step = %f",
//				Radius_, ZScale_, ZStep_));
//		Msg1.setText(String.format(
//				"index =%d  ,xpos = %d, ypos = %d,zpos = %d", 0, 0, 0, 0));

	}

	public void SetScale(HashMap<String, Double> hashMap) {
//		log("SetScale Start......");
//		if (WindowManager.getCurrentImage() == null) {
//			log("only god would work without an image");
//			return;
//		}
//		if (WindowManager.getCurrentImage().getRoi() == null) {
//			log("please set ROI");
//			return;
//		}
//
//		try {
//			Maininstance_.updatePositions();
//		} catch (Exception e) {
//			log(e.toString());
//			return;
//		}
//
//		roi_rectangle = WindowManager.getCurrentImage().getRoi().getBounds();
//		int CenterX = roi_rectangle.x + roi_rectangle.width / 2;
//		int CenterY = roi_rectangle.y + roi_rectangle.height / 2;
//		reSetROI(CenterX, CenterY);
//
//		currFrame = 1;
//		Radius_ = Double.parseDouble(Raduis.getText());
//		DNALen_ = Double.parseDouble(DNALen.getText());
//		RInterpStep_ = Double.parseDouble(RInterpStep.getText());
//		HalfQuadWindow_ = Double.parseDouble(HalfCorrWindow.getText());
//		ImageProcessor ip = WindowManager.getCurrentImage().getProcessor();
//		Imgwidth_ = ip.getWidth();
//		Imgheight_ = ip.getHeight();
//		BitDepth_ = WindowManager.getCurrentImage().getBitDepth();
//		ZScale_ = Double.parseDouble(ZScale.getText());
//		ZStep_ = Double.parseDouble(ZStep.getText());
//		FrameCalcForce_ = Double.parseDouble(FrameCalcForce.getText());
//		ZStart_ = Maininstance_.currzpos_ - ZScale_ / 2;
//
//		Frame2Acq_ = Integer.parseInt(Frame2Acq.getText());
//		TimeIntervals_ = Integer.parseInt(TimeIntervals.getText());
//		Maininstance_.getProcessor().setBaseDir(StoragePath.getText());
//		// baseDir_ = SavePath.getText();
//		Mstep_ = Integer.parseInt(Mstep.getText());
//		F_L_Flag_ = Integer.parseInt(F_L_Flag.getText());
//
//		reSetOpt();
//		setCalProfile();
//
//		Maininstance_.isSetScale = true;
//		Maininstance_.isCalibrated = false;

	}

	private void setCalProfile() {
//		int calProfiley = (int) (ZScale_ / ZStep_);
//		calPos_ = new double[calProfiley];
//		for (int i = 0; i < calProfiley; i++) {
//			calPos_[i] = Maininstance_.currzpos_ - ZScale_ / 2 + i * ZStep_;
//		}
//
//		// Set the x/y positions
//		calPosXY_ = new double[2][];
//		double[] xyStartPoint = new double[] { Maininstance_.currxpos_,
//				Maininstance_.currypos_ };
//		for (int i = 0; i < 2; i++) {
//			calPosXY_[i] = new double[calPos_.length];
//			for (int j = 0; j < calPos_.length; j++)
//				calPosXY_[i][j] = xyStartPoint[i] + xyCalRange_
//						/ calPos_.length * j;
//		}
//
//		Maininstance_.mCalc.DataInit(calcOpt_);
//		Maininstance_.isSetScale = true;
//		Msg0.setText(String.format("Radius =%f  ,Scale = %f, Step = %f",
//				Radius_, ZScale_, ZStep_));
//		log("Scale Seted\t");
//		log(String.format("from   %f  um to  %f um,by %f um\r\n", -ZScale_ / 2,
//				ZScale_ / 2, ZStep_));
	}

	// opt_[13]
	// :radius,rInterStep,bitDepth,halfQuadWidth,imgWidth,imgHeight,zStart,zScale,zStep
	// ��DNALen��Temperature��DNAPersLen,frame2calcForce
	private void reSetOpt() {
		calcOpt_[0] = Radius_;
		calcOpt_[1] = RInterpStep_;
		calcOpt_[2] = BitDepth_;
		calcOpt_[3] = HalfQuadWindow_;
		calcOpt_[4] = Imgwidth_;
		calcOpt_[5] = Imgheight_;
		calcOpt_[6] = ZStart_;
		calcOpt_[7] = ZScale_;
		calcOpt_[8] = ZStep_;
		// force
		calcOpt_[9] = DNALen_;
		calcOpt_[10] = Temperature_;
		calcOpt_[11] = DNAPersLen_;
		calcOpt_[12] = FrameCalcForce_;
	}

	 

	public void log(final String str) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				LogWindow.setText(String.format("%s\r\n     %s ",
						LogWindow.getText(), str));
				LogWindow.setCaretPosition(LogWindow.getText().length());
			}
		});
	}

	private String getTime() {
		Calendar theCa;
		String nowT;
		theCa = new GregorianCalendar();
		theCa.getTime();
		nowT = theCa.get(Calendar.MONTH) + "/" + theCa.get(Calendar.DATE)
				+ "  "
				+ (24 * theCa.get(Calendar.AM) % 24 + theCa.get(Calendar.HOUR))
				+ ":" + theCa.get(Calendar.MINUTE) + ":"
				+ theCa.get(Calendar.SECOND);
		return nowT;
	}

	long now() {
		return System.currentTimeMillis();//
	}

	public void start() {
		begin = now();
	}

	public void end(String str) {
		long end = now();
		log(String.format("%s#%d", str, end - begin));
	}



	public double getCurrCenter(int currTab) {
		// TODO Auto-generated method stub
		return 0;
	}

	public void calibrate() {
		// TODO Auto-generated method stub
		print(String.format("CALIBRATE--%s",this.myForm_.isMagnetAuto()?"True":"False"));
	}

	
	private void print(String format) {
		// TODO Auto-generated method stub
		System.out.println(format);
	}

	public void live() {
		// TODO Auto-generated method stub
		print(String.format("live--%s",this.myForm_.isMagnetAuto()?"True":"False"));
	}

	public void MultiAcq() {
		// TODO Auto-generated method stub
		print(String.format("MultiAcq--%s",this.myForm_.isMagnetAuto()?"True":"False"));
	}

 
}
