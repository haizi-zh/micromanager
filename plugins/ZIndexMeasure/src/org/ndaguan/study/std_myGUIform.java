package org.ndaguan.study;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
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
 

public class std_myGUIform {
	// GUI
	private JTextArea LogWindow = new JTextArea(50, 40);
	public JTextField Msg0 = new JTextField(36);
	public JTextField Msg1 = new JTextField(36);

	private JTextField Raduis = new JTextField(1);
	private JTextField DNALen = new JTextField(1);
	private JTextField ZScale = new JTextField(1);	
	private JTextField Frame2Acq = new JTextField(1);
	private JTextField TimeIntervals = new JTextField(1);
	private JTextField ZStep = new JTextField(1);

	private JTextField FrameCalcForce = new JTextField(1);
	private JTextField StoragePath = new JTextField(1);
	private JTextField SavePath = new JTextField(1);
	private JTextField RInterpStep = new JTextField(1);
	private JTextField HalfCorrWindow = new JTextField(1);
	private JButton Live;
	private JButton Pause;
	private JButton Start;

 
	public int MAX_LEN = 200000;
	 
	// opt:Radius,RInterstep,bitDepth,halfQuadWidth,imgWidth,imgHeight,zStart,zScale,zStep
	public double[] calcOpt_ = null;
	public double Radius_ = 75;
	public double RInterpStep_ = 0.5;
	public double BitDepth_ = 16;
	public double HalfQuadWindow_ = 2;
	public double Imgwidth_ = 640;
	public double Imgheight_ = 480;
	public double ZStart_ = 0;
	public double ZScale_ = 8;
	public double ZStep_ = 0.1;	
	public double DNALen_ = 2.4;// um
	
	private double Temperature_ = 300;// K
	private double DNAPersLen_ = 50;// nm

	public double FrameCalcForce_ = 1000;
	public String StoragePath_ = "E:/Users/n~daguan/AcquisitionData";
	public String SavePath_ = "E:/Users/ResultLog.txt";
	public int Frame2Acq_ = 100000;
	public int TimeIntervals_ = 5;
	// ROI
 
	public int[] calcRoi_ = null;
	private int roix_ = 0;
	private int roiy_ = 0;
	private int roiwidth_ = 40;
	private int roiheight_ = 40;
		// other
	public XYSeries dataSeries_;

	private int ChartMaxItemCount = 1000;
	private JFreeChart chart = null;
 
	private static std_myGUIform instance_;
	private boolean isAcqRunning_ = false;
	private boolean isLiveRunning_= false;
	private boolean isPause_= false;
	private int selectItem;
 
	 public static void main(String args[]) {

		 std_myGUIform mf = new std_myGUIform();
		 
		}
	public static std_myGUIform getInstance() {return instance_;}

	private JPanel createChartPanel() {
		XYSeriesCollection dataset_;
		dataSeries_ = new XYSeries("Z");
		dataSeries_.setMaximumItemCount(ChartMaxItemCount);
		dataset_ = new XYSeriesCollection();
		dataset_.addSeries(dataSeries_);
		chart = ChartFactory.createXYLineChart("ZIndexMeasure", "-Time",
				"-ZPosition", dataset_, PlotOrientation.VERTICAL, true, true,
				false);
		ChartPanel panel = new ChartPanel(chart, true);
		return panel;
	}

	public void updateData(boolean mode){//true refresh GUI false get data
		if(mode){

			Raduis.setText(String.format("%f", Radius_));
			DNALen.setText(String.format("%f", DNALen_));
			ZScale.setText(String.format("%f", ZScale_));
			Frame2Acq.setText(String.format("%d", Frame2Acq_));
			TimeIntervals.setText(String.format("%d", TimeIntervals_));

			ZStep.setText(String.format("%f", ZStep_));
			FrameCalcForce.setText(String.format("%f", FrameCalcForce_));


			StoragePath.setText(String.format("%s", StoragePath_));
			SavePath.setText(String.format("%s", SavePath_));
			RInterpStep.setText(String.format("%f", RInterpStep_));
			HalfCorrWindow.setText(String.format("%d", HalfQuadWindow_));

			Msg0.setText(String.format("Radius =%f  ,Scale = %f, Step = %f",
					Radius_, ZScale_, ZStep_));
			Msg1.setText(String.format(
					"index =%d  ,xpos = %d, ypos = %d,zpos = %d", 0, 0, 0, 0));
			log("");
		}
		else{
			Radius_ = Double.parseDouble(Raduis.getText());
			DNALen_ = Double.parseDouble(DNALen.getText());
			ZScale_ = Double.parseDouble(ZScale.getText());
			ZStep_ = Double.parseDouble(ZStep.getText());
			FrameCalcForce_ = Integer.parseInt(FrameCalcForce.getText());

			Frame2Acq_ = Integer.parseInt(Frame2Acq.getText());
			TimeIntervals_ = Integer.parseInt(TimeIntervals.getText());
			StoragePath_ = StoragePath.getText();
			SavePath_ = SavePath.getText();

			RInterpStep_ = Double.parseDouble(RInterpStep.getText());
			HalfQuadWindow_ = Integer.parseInt(HalfCorrWindow.getText());
		}
	}
	public std_myGUIform() {
	 
		GUIInitialization();
		updateData(true);
	} 

	private void GUIInitialization() {

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
		LeftCenter.add(new JLabel("AdvanceSetup"));
		LeftCenter.add(new JLabel("-----------------"));
		LeftCenter.add(new JLabel("ZStep/um"));
		LeftCenter.add(ZStep);
		LeftCenter.add(new JLabel("frameCalcForce"));
		LeftCenter.add(FrameCalcForce);
		LeftCenter.add(new JLabel("StoragePath"));
		LeftCenter.add(StoragePath);
		LeftCenter.add(new JLabel("SavePath"));
		LeftCenter.add(SavePath);
		LeftCenter.add(new JLabel("RInterpStep"));
		LeftCenter.add(RInterpStep);
		LeftCenter.add(new JLabel("HalfCorrWindow"));
		LeftCenter.add(HalfCorrWindow);

		JPanel leftcenter = new JPanel(new BorderLayout());
		leftcenter.add(LeftCenter);
		// left Bottom
		JPanel leftbottom = new JPanel(new BorderLayout());
		JButton Set = new JButton("Set");
		Start = new JButton("Start");
		Pause = new JButton("Pause");
		Live = new JButton("Live");

		Box LeftBottom = Box.createVerticalBox();
		LeftBottom.add(Live);
		LeftBottom.add(Set);
		LeftBottom.add(Start);
		LeftBottom.add(Pause);

		leftbottom.add(LeftBottom);

		// left end
		left.add(lefttop, BorderLayout.NORTH);
		left.add(leftcenter, BorderLayout.CENTER);
		left.add(leftbottom, BorderLayout.SOUTH);

		// main frame
		JFrame f_ = new JFrame("ZIndexMeasure");
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

					if (e.getActionCommand().equals("Set")) {

					} else if (e.getActionCommand().equals("Start")
							|| e.getActionCommand().equals("Stop")) {
						if(!isAcqRunning_){
//							AcqStart();
							isAcqRunning_ = true;
							Start.setText("Stop");
							log(String.format("AcqStart() at  %s ",getTime()));
						}
						else{
//							AcqStop();
							isAcqRunning_ = false;
							Start.setText("Start");
							log(String.format("AcqStop() at  %s ",getTime()));
						}
					} else if (e.getActionCommand().equals("Pause")
							|| e.getActionCommand().equals("Resume")) {

						if(!isPause_){
//							AcqPause();
							isPause_ = true;
							Start.setText("Resume");
							log(String.format("AcqPause() at  %s ",getTime()));
						}
						else{
//							AcqResume();
							isPause_ = false;
							Start.setText("Pause");
							log(String.format("AcqResume() at  %s ",getTime()));
						}

					} else if (e.getActionCommand().equals("Live")
							|| e.getActionCommand().equals("StopLive")) {
						if(!isLiveRunning_){
//							AcqLive();
							isLiveRunning_ = true;
							Start.setText("StopLive");
						}
						else{
//							AcqStopLive();
							isLiveRunning_ = false;
							Start.setText("Live");
						}
					}
				} catch (Exception ee) {
					log("GUIINI ERR ! " + ee.toString());
				}
			}
		};

		Start.addActionListener(menuListener);
		Live.addActionListener(menuListener);
		Pause.addActionListener(menuListener);
		Set.addActionListener(menuListener);

	}

	public void showPopMenu(MouseEvent e,int cX,int cY,int itemIndex){

		selectItem = itemIndex;
		JPopupMenu popup = new JPopupMenu();
		JMenuItem Delete = new JMenuItem("Delete");
		JMenuItem Calibration = new JMenuItem("Calibration");


		popup.add(Calibration);
		popup.add(Delete);

		ActionListener menuListener = new ActionListener() {

			public void actionPerformed(ActionEvent e) {

				try {

					if (e.getActionCommand().equals("Calibration")) {
						log(String.format("Calibration() at  %s ",getTime()));

//						Calibration(selectItem);
					} 
					else if (e.getActionCommand().equals("Delete")) {

						log(String.format("Delete() at  %s ",getTime()));
//						Delete(selectItem);
					}

				} catch (Exception ee) {
					log("GUIINI ERR ! " + ee.toString());
				}
			}
		};

		Calibration.addActionListener(menuListener);

		Delete.addActionListener(menuListener);

		popup.show(e.getComponent(),cX,cY);
	}

	public void log(String str) {
		if(LogWindow.getText().length() == 0 && str.isEmpty())
			LogWindow.setText("welcome....");
		if(!str.isEmpty())
		{LogWindow.setText(String.format("%s\r\n     %s ", LogWindow.getText(),
				str));
		LogWindow.setCaretPosition(LogWindow.getText().length());
		}
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


}
