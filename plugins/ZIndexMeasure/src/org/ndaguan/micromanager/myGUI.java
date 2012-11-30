package org.ndaguan.micromanager;
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

import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class myGUI {
	// main instance
	private ZIndexMeasure Maininstance_ = null;
	private static myGUI instance_;
	public int MAX_LEN = 200000;
	// GUI
	private JFrame f_ = new JFrame("ZIndexMeasure");
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
	private double ZStep_ = 0.5;

	private JTextField Raduis = new JTextField(1);
	private JTextField RInterpStep = new JTextField(1);
	private JTextField HalfCorrWindow = new JTextField(1);
	private JTextField FrameCalcForce = new JTextField(1);
	private JTextField Frame2Acq = new JTextField(1);
	private JTextField TimeIntervals = new JTextField(1);
	private JTextField StoragePath = new JTextField(1);
	private JTextField SavePath = new JTextField(1);
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
	public String StoragePath_ = "E:/Users/n~daguan/AcquisitionData";
	public String SavePath_ = "E:/Users/ResultLog.txt";
	public int Frame2Acq_ = 100000;
	public int TimeIntervals_ = 5;
	public double[] calPos_;
	public int Mstep_ = 100;//um
	public int F_L_Flag_ = 0;

	private FileWriter FileOut = null;
	public BufferedWriter writer = null;
	public JFreeChart chart  = null;
	private int ChartMaxItemCount = 1000;
	private int isPause = 0;
	// Temp
	private long begin;
	final Object lock = new Object();

	public static myGUI getInstance() {
		return instance_;
	}

	private JPanel createChartPanel() {
		XYSeriesCollection dataset_;
		dataSeries_ = new XYSeries("Z");
		dataSeries_.setMaximumItemCount(ChartMaxItemCount);
		dataset_ = new XYSeriesCollection();
		dataset_.addSeries(dataSeries_);
		chart = ChartFactory.createXYLineChart("ZIndexMeasure",
				"-Time", "-ZPosition", dataset_, PlotOrientation.VERTICAL,
				true, true, false);

		ChartPanel panel = new ChartPanel(chart, true);
		return panel;
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

		int ROIBounder = 10;
		int BallSqrt = (int) ((ROIBounder + Radius_) * 2);
		roi_rectangle.width = BallSqrt;
		roi_rectangle.height = BallSqrt;
		roi_rectangle.x = (int) (CenterX - BallSqrt / 2);
		roi_rectangle.y = (int) (CenterY - BallSqrt / 2);

		if (!this.IsBallInImg()) {
			log("THE BALL IS OUT OF THIS IMAGE,I GONA GIVE UP TRACKING....");
			return;
		}
		calcRoi_[0] = roi_rectangle.x;
		calcRoi_[1] = roi_rectangle.y;
		calcRoi_[2] = roi_rectangle.width;
		calcRoi_[3] = roi_rectangle.height;
		WindowManager.getCurrentImage().setRoi(roi_rectangle);
	}

	public myGUI() {
		instance_ = this;
		Maininstance_ = ZIndexMeasure.getInstance();
		roi_rectangle = new Rectangle();

		calcOpt_ = new double[] { Radius_, RInterpStep_, BitDepth_,
				HalfQuadWindow_, Imgwidth_, Imgheight_, ZStart_, ZScale_,
				ZStep_, DNALen_, Temperature_, DNAPersLen_, FrameCalcForce_ };
		calcRoi_ = new int[] { roix_, roiy_, roiwidth_, roiheight_ };

		LogWindow.setText("Welcome");
		Raduis.setText(String.format("%f", Radius_));
		DNALen.setText(String.format("%f", DNALen_));
		ZScale.setText(String.format("%f", ZScale_));
		ZStep.setText(String.format("%f", ZStep_));
		RInterpStep.setText(String.format("%f", RInterpStep_));
		HalfCorrWindow.setText(String.format("%f", HalfQuadWindow_));
		FrameCalcForce.setText(String.format("%f", FrameCalcForce_));
		Frame2Acq.setText(String.format("%d", Frame2Acq_));
		Mstep.setText(String.format("%d", Mstep_));
		F_L_Flag.setText(String.format("%d",F_L_Flag_ ));
		TimeIntervals.setText(String.format("%d", TimeIntervals_));
		StoragePath.setText(String.format("%s", StoragePath_));
		SavePath.setText(String.format("%s", SavePath_));

		Msg0.setText(String.format("Radius =%f  ,Scale = %f, Step = %f",
				Radius_, ZScale_, ZStep_));
		Msg1.setText(String.format(
				"index =%d  ,xpos = %d, ypos = %d,zpos = %d", 0, 0, 0, 0));

	}

	public void SetScale() {
		log("SetScale Start......");
		if (WindowManager.getCurrentImage() == null) {
			log("please open an image,use the live button");
			return;
		}
		if (WindowManager.getCurrentImage().getRoi() == null) {
			log("please set ROI,the tool is locate in the imagej main frame");
			return;
		}

		roi_rectangle = WindowManager.getCurrentImage().getRoi().getBounds();
		int CenterX = roi_rectangle.x + roi_rectangle.width / 2;
		int CenterY = roi_rectangle.y + roi_rectangle.height / 2;
		reSetROI(CenterX, CenterY);

		currFrame = 1;
		Radius_ = Double.parseDouble(Raduis.getText());
		DNALen_ = Double.parseDouble(DNALen.getText());
		RInterpStep_ = Double.parseDouble(RInterpStep.getText());
		HalfQuadWindow_ = Double.parseDouble(HalfCorrWindow.getText());
		ImageProcessor ip = WindowManager.getCurrentImage().getProcessor();
		Imgwidth_ = ip.getWidth();
		Imgheight_ = ip.getHeight();
		BitDepth_ = WindowManager.getCurrentImage().getBitDepth();
		ZScale_ = Double.parseDouble(ZScale.getText());
		ZStep_ = Double.parseDouble(ZStep.getText());
		FrameCalcForce_ = Double.parseDouble(FrameCalcForce.getText());
		ZStart_ = Maininstance_.currzpos_ - ZScale_ / 2;

		Frame2Acq_ = Integer.parseInt(Frame2Acq.getText());
		TimeIntervals_ = Integer.parseInt(TimeIntervals.getText());
		StoragePath_ = StoragePath.getText();
		SavePath_ = SavePath.getText();
		Mstep_ = Integer.parseInt(Mstep.getText());
		F_L_Flag_ = Integer.parseInt(F_L_Flag.getText());
		try {
			FileOut = new FileWriter(new File(SavePath_));
			writer = new BufferedWriter(FileOut);
			writer
			.write("Frame, XPos/pixel, YPos/pixel, ZPos/uM,<StdXPos>/nM,<StdYPos>/nM,<StdZPos>/nM,meanX/pixel,meanY/pixel,meanZ/pixel,ForceX/pN,ForceY/pN\r\n");
			writer.flush();
		} catch (IOException e) {
			log("Create File ERR  " + e.toString());
			return;
		}
		reSetOpt();
		setCalProfile();

		Maininstance_.isSetScale = true;
		Maininstance_.isCalibrated = false;

	}

	private void setCalProfile() {
		int calProfiley = (int) (ZScale_ / ZStep_);
		calPos_ = new double[calProfiley];
		for (int i = 0; i < calProfiley; i++) {
			calPos_[i] = Maininstance_.currzpos_ - ZScale_ / 2 + i * ZStep_;
		}

		Maininstance_.mCalc.DataInit(calcOpt_);
		Maininstance_.isSetScale = true;
		Msg0.setText(String.format("Radius =%f  ,Scale = %f, Step = %f",
				Radius_, ZScale_, ZStep_));
		log("Scale Seted\t");
		log(String.format("from   %f  um to  %f um,by %f um\r\n", -ZScale_ / 2,
				ZScale_ / 2, ZStep_));
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

	public void GUIInitialization() {

		// Add GUI DATA
		JSeparator s1 = new JSeparator(JSeparator.HORIZONTAL);

		// top
		JPanel top = new JPanel(new BorderLayout());
		Box Top = Box.createVerticalBox();
		JScrollPane taJsp = new JScrollPane(createChartPanel());
		taJsp.setPreferredSize(new Dimension(800, 600));
		Top.add(taJsp);

		// center
		Color bg = new Color(18, 25, 19);
		JPanel center = new JPanel(new BorderLayout());
		Msg0.setBackground(bg);
		Msg0.setEnabled(false);
		Top.add(Msg0);
		Msg1.setBackground(bg);
		Msg1.setEnabled(false);
		Top.add(Msg1);

		// bottom
		JPanel bottom = new JPanel(new BorderLayout());
		LogWindow.setText("welcome .....");
		LogWindow.setLineWrap(true);
		Box Bottom = Box.createVerticalBox();
		Bottom.add(LogWindow);
		JScrollPane Jsp = new JScrollPane(Bottom);
		Jsp.setPreferredSize(new Dimension(500, 150));

		Top.add(Jsp);
		top.add(Top);

		// middle = top + center +bottom
		JPanel middle = new JPanel(new BorderLayout());
		middle.add(top, BorderLayout.NORTH);
		middle.add(center, BorderLayout.CENTER);
		middle.add(bottom, BorderLayout.SOUTH);

		// left
		JPanel left = new JPanel(new BorderLayout());
		// left Top
		JPanel lefttop = new JPanel(new BorderLayout());
		Box LeftTop = Box.createVerticalBox();
		LeftTop.add(new JLabel("--------*--------"));
		JLabel lable1 = new JLabel("Initial");
		LeftTop.add(lable1);
		LeftTop.add(new JLabel("-----------------"));
		lefttop.add(LeftTop, BorderLayout.NORTH);
		lefttop.add(s1, BorderLayout.SOUTH);
		// left center
		Box LeftCenter = Box.createVerticalBox();

		LeftCenter.add(new JLabel("BallRadius/pixel"));
		LeftCenter.add(Raduis);
		LeftCenter.add(new JLabel("DNAlen/"));
		LeftCenter.add(DNALen);
		LeftCenter.add(new JLabel("ZScele/um"));
		LeftCenter.add(ZScale);

		LeftCenter.add(new JLabel("Frame2Acq/F"));
		LeftCenter.add(Frame2Acq);
		LeftCenter.add(new JLabel("TimeIntervals/ms"));
		LeftCenter.add(TimeIntervals);
		LeftCenter.add(new JLabel("--------*--------"));
		LeftCenter.add(new JLabel("AdvanceSetup"));
		LeftCenter.add(new JLabel("-----------------"));
		LeftCenter.add(new JLabel("ZStep/um"));
		LeftCenter.add(ZStep);
		LeftCenter.add(new JLabel("frameCalcForce"));
		LeftCenter.add(FrameCalcForce);
		LeftCenter.add(new JLabel("Mstep"));
		LeftCenter.add(Mstep);

		LeftCenter.add(new JLabel("F_L_Flag"));
		LeftCenter.add(F_L_Flag);
		LeftCenter.add(new JLabel("StoragePath"));
		LeftCenter.add(StoragePath);
		LeftCenter.add(new JLabel("SavePath"));
		LeftCenter.add(SavePath);

		JPanel leftcenter = new JPanel(new BorderLayout());
		leftcenter.add(LeftCenter);
		// left Bottom
		JPanel leftbottom = new JPanel(new BorderLayout());
		JButton Set = new JButton("Set");
		JButton Calibration = new JButton("Calibration");
		JButton Start = new JButton("Start");
		Pause = new JButton("Pause");
		Live = new JButton("Live");
		JButton Stop = new JButton("Stop");
		JButton Save = new JButton("Save");

		Box LeftBottom = Box.createVerticalBox();
		LeftCenter.add(new JLabel("--------*--------"));
		LeftBottom.add(Live);
		LeftBottom.add(Set);
		LeftBottom.add(Calibration);
		LeftBottom.add(Start);
		LeftBottom.add(Pause);
		LeftBottom.add(Stop);
		LeftBottom.add(Save);
		leftbottom.add(LeftBottom);

		// left end
		left.add(lefttop, BorderLayout.NORTH);
		left.add(leftcenter, BorderLayout.CENTER);
		left.add(leftbottom, BorderLayout.SOUTH);

		// main frame
		Container con = f_.getContentPane();
		con.setLayout(new BorderLayout());
		con.add(left, BorderLayout.WEST);
		con.add(middle, BorderLayout.CENTER);

		f_.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		f_.pack();
		f_.setVisible(true);
		JFrame.setDefaultLookAndFeelDecorated(false);

		ActionListener menuListener = new ActionListener() {

			public void actionPerformed(ActionEvent e) {

				try {

					if (e.getActionCommand().equals("Start")) {
						log(String.format("Atempt to InstallCallback at  %s ",
								getTime()));

						Frame2Acq_ = Integer.parseInt(Frame2Acq.getText());
						TimeIntervals_ = Integer.parseInt(TimeIntervals
								.getText());
						StoragePath_ = StoragePath.getText();

						Maininstance_.InstallCallback();
					} else if (e.getActionCommand().equals("Stop")) {
						if (!Maininstance_.gui_.getAcquisitionEngine()
								.isAcquisitionRunning()) {
							log("Acquisition is not running ,abort!");
							return;
						} else {
							log(String.format(
									"Atempt to UninstallCallback at  %s ",
									getTime()));
							Maininstance_.UninstallCallback();
						}
					} else if (e.getActionCommand().equals("Pause")
							|| e.getActionCommand().equals("Resume")) {

						if (!Maininstance_.gui_.getAcquisitionEngine()
								.isAcquisitionRunning()) {
							log("Acquisition is not running ,abort!");
							return;
						}

						if (isPause == 0) {
							Maininstance_.gui_.getAcquisitionEngine().setPause(
									true);
							Pause.setText("Resume");
							log(String.format("Atempt to Pause at  %s ",
									getTime()));
							isPause = 1;
						} else {
							Maininstance_.gui_.getAcquisitionEngine().setPause(
									false);
							Pause.setText("Pause");
							log(String.format("Atempt to Resume at  %s ",
									getTime()));
							isPause = 0;
						}
					} else if (e.getActionCommand().equals("Live")
							|| e.getActionCommand().equals("Stop Live")) {
						if (Maininstance_.gui_.getAcquisitionEngine()
								.isAcquisitionRunning()) {
							log("Acquistion is running,mission abort!");
							return;
						}
						if (Maininstance_.gui_.isLiveModeOn()) {
							Maininstance_.gui_.enableLiveMode(false);
							Live.setText("Live");
						} else {
							Live.setText("Stop Live");
							Maininstance_.gui_.enableLiveMode(true);
						}
					} else if (e.getActionCommand().equals("Set")) {
						log(String.format("Atempt to SetScale at %s ",
								getTime()));
						SetScale();
					} else if (e.getActionCommand().equals("Calibration")) {
						log(String.format("Atempt to StartCalibration at %s ",
								getTime()));
						SetScale();
						(new Thread(new Runnable() {
							@Override
							public void run() {
								Maininstance_.StartCalibration();
							}
						})).start();

					}
				} catch (Exception ee) {
					log("GUIINI ERR ! " + ee.toString());
				}
			}
		};

		Start.addActionListener(menuListener);
		Live.addActionListener(menuListener);
		Pause.addActionListener(menuListener);
		Save.addActionListener(menuListener);
		Stop.addActionListener(menuListener);
		Set.addActionListener(menuListener);
		Calibration.addActionListener(menuListener);

	}

	public void log(String str) {
		LogWindow.setText(String.format("%s\r\n     %s ", LogWindow.getText(),
				str));
		LogWindow.setCaretPosition(LogWindow.getText().length());
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

	// private void saveFile() {
	// JFileChooser file = new JFileChooser();
	// if (file.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
	// File fl = file.getSelectedFile();
	// try {
	// FileWriter out = new FileWriter(fl);
	// out
	// .write("#frame,#xPos/pixel,#yPos/pixel,#zPos/pixel,#force/pN\r\n");
	// for (int i = 0; i < this.resultSave.length; i++) {
	// for (int j = 0; j < this.resultSave[i].length; j++) {
	// out.write(this.resultSave[i][j] + ",");
	// }
	// out.write("\r\n");
	// }
	// out.close();
	// } catch (IOException e) {
	// log("File save error");
	// }
	// }// if Open File
	// }
}
