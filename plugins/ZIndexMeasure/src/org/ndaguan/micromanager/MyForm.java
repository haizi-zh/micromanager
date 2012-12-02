package org.ndaguan.micromanager;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;


public class MyForm extends JFrame {
	final String[] PARALIST  = new String[]{"BallRadius","DNALength","ZCalScale_","ZCalStep","RinterStep","HalfCorrWin","MagnetStep","Frame2Acq","FrameCalcF","ITEM0","ITEM1","ITEM2"};
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JFreeChart chart = null;
	private int ChartMaxItemCount = 1000;

	private final int DEFAULT_WIDTH = 600+30;
	private final int DEFAULT_HEIGHT =(int)( DEFAULT_WIDTH*0.618);
	private final int DEFAULT_LOCATION_X = 0;
	private final int DEFAULT_LOCATION_Y = 0;
	private final String DEFAULT_TITLE = "Welcome......";
	private final String DEFAULT_IMAGE = "z:/default.gif";
	private final int DEFAULT_CLOSE_OPERATION =JFrame.HIDE_ON_CLOSE;
	private int[] tapSize = null;

	private ButtonGroup buttonGroup_0 = new ButtonGroup();
	ActionListener menuListener = null;
	public PreferDailogBox preferDailogBox;
	private JTextArea LogWindow = new JTextArea(40, (int)(40*0.618));
	private int currTab;
	private HashMap<String, XYSeries> dataSeries_;
	private XYSeriesCollection dataset_;
	private String[] dataSet;
	private JRadioButtonMenuItem MagnetAuto;
	private JRadioButtonMenuItem MagnetManual;
	private static MyGUI mygui_;

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {

				MyForm frame = new MyForm(mygui_);
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);	
				frame.setVisible(true);		


			}
		});

	}

	public MyForm(MyGUI mygui) {

		mygui_ = mygui;
		tapSize = new int[]{600,900,800};
		setDataSeries_(new HashMap<String,XYSeries>());
		dataSet  = new String[]{"Chart-Z","Chart-X","Chart-Y","Chart-XY","Chart-Z-STD","Chart-X-STD","Chart-Y-STD"};

		preferDailogBox = new PreferDailogBox(this,mygui_);
		initialize();
		//Set Data

		//Set Looking
		setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
		setLocation(DEFAULT_LOCATION_X,DEFAULT_LOCATION_Y);
		setTitle(DEFAULT_TITLE);
		Toolkit kit = Toolkit.getDefaultToolkit();
		Image img = kit.getImage(DEFAULT_IMAGE);
		setIconImage(img);
		setDefaultCloseOperation(DEFAULT_CLOSE_OPERATION);
	}


	private void initialize() {
		menuListener = new ActionListener() {public void actionPerformed(ActionEvent e) {PhraseActionEvent(e);}};

		getContentPane().setLayout(null);

		final JMenu Operation = new JMenu("Operation");//Operation

		final JMenuItem Calibrate = new JMenuItem("Calibrate");
		final JMenuItem Live = new JMenuItem("Live");
		final JMenuItem MultiAcq = new JMenuItem("MultiAcq");

		final JMenu Option = new JMenu("Option");//Option		
		final JMenuItem Preferences = new JMenuItem("Preferences");		
		final JMenu Magnet = new JMenu("Magnet");
		MagnetAuto = new JRadioButtonMenuItem("Auto");		
		MagnetManual = new JRadioButtonMenuItem("Manual");
		buttonGroup_0.add(MagnetAuto);
		buttonGroup_0.add(MagnetManual);

		Magnet.add(MagnetAuto);
		Magnet.addSeparator();
		Magnet.add(MagnetManual);				 

		Calibrate.addActionListener(menuListener);
		Live.addActionListener(menuListener);
		MultiAcq.addActionListener(menuListener);		

		Preferences.addActionListener(menuListener);	


		Operation.add(Calibrate);		 
		Operation.addSeparator();	 		
		Operation.add(Live);
		Operation.addSeparator();	 	
		Operation.add(MultiAcq);

		Option.add(Preferences);
		Option.addSeparator();			
		Option.add(Magnet);

		final JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		menuBar.add(Operation);
		menuBar.add(Option);
		
		final int toolItemWidth = 20;
		final int toolItemHeight = 20;
		final int toolbarheight = toolItemHeight+10;
		final JToolBar toolBar = new JToolBar();
		int itemNum = 5;
		toolBar.setBounds(0,0, toolItemWidth*itemNum , toolbarheight);
		
		Toolkit kit = Toolkit.getDefaultToolkit();
		final	Image imgC = kit.getImage("Z:/C.gif");
		final	Image imgL = kit.getImage("Z:/L.gif");
		final	Image imgM = kit.getImage("Z:/M.gif");
		final	Image imgS = kit.getImage("Z:/S.gif");
		final	Image imgH = kit.getImage("Z:/H.gif");
		JButton butC = new JButton();
		JButton butL = new JButton();
		JButton butM = new JButton();
		JButton butS = new JButton();
		JButton butH = new JButton();
		 

		int offsetx = 0;
		butC.setIcon(new javax.swing.ImageIcon(imgC)); // NOI18N
		butC.setToolTipText("Calibrate");
		butC.setFocusable(false);
		butC.setBounds(0, offsetx, toolItemWidth, toolItemHeight);
		offsetx += toolItemWidth;
		
		butL.setIcon(new javax.swing.ImageIcon(imgL)); // NOI18N
		butL.setToolTipText("Calibrate");
		butL.setFocusable(false);
		butL.setBounds(0, offsetx, toolItemWidth, toolItemHeight);
		offsetx += toolItemWidth;
		
		butM.setIcon(new javax.swing.ImageIcon(imgM)); // NOI18N
		butM.setToolTipText("Calibrate");
		butM.setFocusable(false);
		butM.setBounds(0, offsetx, toolItemWidth, toolItemHeight);
		offsetx += toolItemWidth;
		
		butS.setIcon(new javax.swing.ImageIcon(imgS)); // NOI18N
		butS.setToolTipText("Calibrate");
		butS.setFocusable(false);
		butS.setBounds(0, offsetx, toolItemWidth, toolItemHeight);
		offsetx += toolItemWidth;

		butH.setIcon(new javax.swing.ImageIcon(imgH)); // NOI18N
		butH.setToolTipText("Calibrate");
		butH.setFocusable(false);
		butH.setBounds(0, offsetx, toolItemWidth, toolItemHeight);
		offsetx += toolItemWidth;


		toolBar.add(butL);
		toolBar.add(butC);
		toolBar.add(butM);
		toolBar.add(butS);
		toolBar.add(butH);
		getContentPane().add(toolBar);
		//tabbedPane
		final JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.setBounds(0,toolbarheight, tapSize[0], (int)(tapSize[0]*0.52));
		getContentPane().add(tabbedPane);

		final JScrollPane scrollPane = new JScrollPane();
		tabbedPane.addTab("Log", null, scrollPane, null);

		scrollPane.setViewportView(LogWindow);
		for (int i = 0; i <  dataSet.length; i++) {
			tabbedPane.addTab( dataSet[i], null, createChartPanel(dataSet[i]), null);
		}




		tabbedPane.addChangeListener(new ChangeListener() { 

			public void stateChanged(ChangeEvent e)
			{
				int ind=tabbedPane.getSelectedIndex();
				int width = tapSize[1];
				switch(ind){
				case 0:
					width =tapSize[0];

					setCurrTab(0);
					break;
				case 1:

					setCurrTab(1);
					break;
				case 2:

					setCurrTab(2);
					break;

				case 3:
					setCurrTab(3);

					break;

				case 4:
					setCurrTab(4);

					break;

				case 5:
					setCurrTab(5);

					break;

				case 6:
					setCurrTab(6);

					break;

				case 7:
					setCurrTab(7);

					break;
				case 8:
					setCurrTab(8);
					break;
				}

				tabbedPane.setBounds(0, toolbarheight, width,(int)(width*0.618+40));		
				setBounds(0, 0, width+40, (int)((width)*0.618+120));

			}
		});







	}

	private JPanel createChartPanel(String tableName) {
		if(this.getDataSeries_().containsKey(tableName))
			return null;

		final XYSeries temp_ =  new XYSeries(tableName);

		temp_.setMaximumItemCount(ChartMaxItemCount);
		dataset_ = new XYSeriesCollection();
		dataset_.addSeries(temp_);
		chart = ChartFactory.createXYLineChart(tableName, "-Time",
				"-value", dataset_, PlotOrientation.VERTICAL, true, true,
				false);
		getDataSeries_().put(tableName,temp_);	
		ChartPanel cPanel = new ChartPanel(chart, true);
		cPanel.setBounds(10, 10, tapSize[1], (int)(tapSize[1]*0.6));

		final JSlider slider = new JSlider(JSlider.VERTICAL);
		slider.setMinimum(1);
		slider.setValue(50);
		slider.setMaximum(100);
		slider.setBounds(0, 0, 10, (int)(tapSize[1]*0.618));

		final JSlider hslider = new JSlider(JSlider.HORIZONTAL);
		hslider.setMinimum(1);
		hslider.setValue(50);
		hslider.setMaximum(100);
		hslider.setBounds(15, 0,tapSize[1]-20,10);
		hslider.addChangeListener(new ChangeListener() { 

			public void stateChanged(ChangeEvent e)
			{
				int ind=hslider.getValue();
				double center = mygui_.getCurrCenter(getCurrTab());
				chart.getXYPlot().getDomainAxis().setRange(center - 5/ind, center + 5/ind);
				System.out.println(center - 5/ind);
				temp_.add(ind, ind);
			}
		}
				);

		slider.addChangeListener(new ChangeListener() { 

			public void stateChanged(ChangeEvent e)
			{
				int ind=slider.getValue();
				if(ind == 0)
					return;
				double center = mygui_.getCurrCenter(getCurrTab());
				chart.getXYPlot().getRangeAxis()
				.setRange(center - 5/ind, center + 5/ind);
				System.out.println(String.format("%f---%f,---%d",center - 5/ind,center + 5/ind,ind));
				temp_.add(ind, ind);
			}
		}
				);

		JPanel panel = new JPanel();
		panel.setLayout(null);
		panel.add(slider);
		panel.add( cPanel);
		panel.add(hslider);		




		return panel;
	}
	private void PhraseActionEvent(ActionEvent e){
		if (e.getActionCommand().equals("Preferences")) {
			(new Thread(new Runnable() {
				@Override
				public void run() {
					preferDailogBox.UpdateData(false);//GUI  update
					preferDailogBox.frame.setVisible(true);
				}
			})).start();

		}
		if (e.getActionCommand().equals("Calibrate")) {
			(new Thread(new Runnable() {
				@Override
				public void run() {
					mygui_.calibrate();
				}
			})).start();

		}

		if (e.getActionCommand().equals("Live")) {
			(new Thread(new Runnable() {
				@Override
				public void run() {
					mygui_.live();
				}
			})).start();

		}

		if (e.getActionCommand().equals("MultiAcq")) {
			(new Thread(new Runnable() {
				@Override
				public void run() {
					mygui_.MultiAcq();
				}
			})).start();

		}
	}

	public void log(final String str) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				LogWindow.setText(String.format("%s\r\n  #%s#   %s ",
						LogWindow.getText(), getTime(),str));
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


	public int getCurrTab() {
		return currTab;
	}

	public void setCurrTab(int currTab) {
		this.currTab = currTab;
	}

	public boolean isMagnetAuto() {
		return MagnetAuto.isSelected();
	}

	public void setMagnetAuto(boolean isMagnetAuto) {
		MagnetManual.setSelected(isMagnetAuto);
	}

	public HashMap<String, XYSeries> getDataSeries_() {
		return dataSeries_;
	}

	public void setDataSeries_(HashMap<String, XYSeries> dataSeries_) {
		this.dataSeries_ = dataSeries_;
	}

	public class Login  {
		private double data[][] = null;
		private double[][] userDataSet;
		private File loginDataFile;
		public Login() throws NumberFormatException, IOException{
			loginDataFile = new File(System.getProperty("user.home")+"ZIndexMeasure","LoginData.txt");
			initialize(getUserList());
		}

		private void initialize(String[] userList) {


		}

		private String[] getUserList() throws IOException {	

			if(!loginDataFile.exists())
				return null;

			BufferedReader in = new BufferedReader(new FileReader(loginDataFile)); 
			String line;
			if((line = in.readLine()) == null)
				return null;

			String[] userNameList = line.split("\t"); 
			userDataSet = new double[userNameList.length][12];
			return userNameList;

		}


	}

	public class PreferDailogBox {
		public JFrame frame ;
		final private int ITEMWIDTH = 100;
		final private int ITEMHEIGHT = 20;

		private JTextField BallRadius;
		private JTextField DNALength;
		private JComboBox<String> ZCalScale;
		private JTextField ZCalStep;
		private JTextField RinterStep;
		private JTextField HalfCorrWin;
		private JTextField MagnetStep;
		private JTextField Frame2Acq;
		private JTextField FrameCalcF;
		private JTextField ITEM0;
		private JTextField ITEM1;
		private JTextField ITEM2;

		private double BallRadius_;
		private double DNALength_;
		private double ZCalScale_;
		private double ZCalStep_;
		private double RinterStep_;
		private double HalfCorrWin_;
		private double MagnetStep_;
		private double Frame2Acq_;
		private double FrameCalcF_;
		private double ITEM0_;
		private double ITEM1_;
		private double ITEM2_;

		private HashMap<String,Double> Opt_ = null;
		private ActionListener DialogListener;
		private String dataDir_  ;
		private MyGUI mygui_;
		private MyForm myform_;

		public PreferDailogBox(MyForm myform,MyGUI mygui) {
			myform_ = myform;
			mygui_ = mygui;
			Opt_   = new HashMap<String,Double>();

			double[] temp = null;
			try {
				temp = getUserData();
			} catch (IOException e) {

				e.printStackTrace();
			}
			setDefautPrefer(temp);
			initialize();
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					UpdateData(false);//GUI  update
					//					frame.setVisible(true);
				}
			});



		}
		//		private void print() {
		//			System.out.println(string);
		//			"BallRadius","DNALength","ZCalScale_","ZCalStep","RinterStep","HalfCorrWin",MagnetStep","Frame2Acq","FrameCalcF","ITEM0","ITEM1",ITEM2"
		//			String[] para  = new String[]{"BallRadius","DNALength","ZCalScale_","ZCalStep","RinterStep","HalfCorrWin","MagnetStep","Frame2Acq","FrameCalcF","ITEM0","ITEM1","ITEM2"};
		//			for (int i = 0; i < para.length; i++) {
		//				print(String.format("\rOpt_.put(\"%s\",%s_);",para[i],para[i]));
		//			}
		//			for (int i = 0; i < para.length; i++) {
		//				print(String.format("\r%s_ = Double.parseDouble(%s.getText());",para[i],para[i]));
		//			}
		//			print(String.format("\r\n\r\n"));
		//			for (int i = 0; i < para.length; i++) {
		//				print(String.format("\r%s.setText(String.format(\"dfdsfsdaf\",%s_));",para[i],para[i]));
		//			}
		//		}


		private double[] getUserData() throws IOException{
			File loginDataFile = new File(System.getProperty("user.home")+"ZIndexMeasure-userData.txt");
			if(!loginDataFile.exists())
				return null;

			BufferedReader in = new BufferedReader(new FileReader(loginDataFile)); 
			String line;
			if((line = in.readLine()) == null)
				return null;

			String[] temp = line.split("\t"); 
			double[] userDataSet = new double[12];
			for (int i = 0; i < userDataSet.length; i++) {
				userDataSet[i] = Double.parseDouble(temp[i]);				
			}
			if((line = in.readLine()) != null)
				this.dataDir_ = line;
			return userDataSet;
		}

		private void setUserData() throws IOException{
			File loginDataFile = new File(dataDir_+"ZIndexMeasure-userData.txt");
			FileWriter out = new FileWriter((loginDataFile)); 
			String temp = new String(String.format("%f\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t\r\n",BallRadius_,DNALength_,ZCalScale_,ZCalStep_,RinterStep_,HalfCorrWin_,MagnetStep_,Frame2Acq_,FrameCalcF_,ITEM0_,ITEM1_,ITEM2_,dataDir_));
			out.write(temp);
			out.close(); 
		}

		private void setDefautPrefer(double[] data) {
			if(data == null || data.length<12){
				BallRadius_ = 1.4;
				DNALength_ = 1;
				ZCalScale_  = 6;
				ZCalStep_ = 0.1;
				RinterStep_ = 0.5;
				HalfCorrWin_ = 2;
				MagnetStep_ = 200;
				Frame2Acq_ = 20000;
				FrameCalcF_ = 500;
				ITEM0_ = -1;
				ITEM1_ = -1;
				ITEM2_ = -1;
				this.dataDir_ = System.getProperty("user.home");
			}else{
				BallRadius_ = data[0];
				DNALength_ = data[1];
				ZCalScale_  = data[2];
				ZCalStep_ = data[3];
				RinterStep_ = data[4];
				HalfCorrWin_ = data[5];
				MagnetStep_ = data[6];
				Frame2Acq_ = data[7];
				FrameCalcF_ = data[8];
				ITEM0_ = data[9];
				ITEM1_ = data[10];
				ITEM2_ = data[11];
			}

			packPreferData();
		}

		private void UpdateData(boolean flag) {


			if(flag){//flush

				BallRadius_ = Double.parseDouble(BallRadius.getText());

				DNALength_ = Double.parseDouble(DNALength.getText());

				ZCalScale_ = (double)ZCalScale.getSelectedIndex()+2;

				ZCalStep_ = Double.parseDouble(ZCalStep.getText());

				RinterStep_ = Double.parseDouble(RinterStep.getText());

				HalfCorrWin_ = Double.parseDouble(HalfCorrWin.getText());

				MagnetStep_ = Double.parseDouble(MagnetStep.getText());

				Frame2Acq_ = Double.parseDouble(Frame2Acq.getText());

				FrameCalcF_ = Double.parseDouble(FrameCalcF.getText());

				ITEM0_ = Double.parseDouble(ITEM0.getText());

				ITEM1_ = Double.parseDouble(ITEM1.getText());

				ITEM2_ = Double.parseDouble(ITEM2.getText());

				try {
					setUserData();
				} catch (IOException e) {
					e.printStackTrace();
				}
				packPreferData();		
			}
			else{//refresh GUI

				BallRadius.setText(String.format("%.2f",BallRadius_));

				DNALength.setText(String.format("%.2f",DNALength_));

				ZCalScale.setSelectedIndex((int)(ZCalScale_  - 2));

				ZCalStep.setText(String.format("%.2f",ZCalStep_));

				RinterStep.setText(String.format("%.2f",RinterStep_));

				HalfCorrWin.setText(String.format("%.0f",HalfCorrWin_));

				MagnetStep.setText(String.format("%.1f",MagnetStep_));

				Frame2Acq.setText(String.format("%.0f",Frame2Acq_));

				FrameCalcF.setText(String.format("%.0f",FrameCalcF_));

				ITEM0.setText(String.format("%.0f",ITEM0_));

				ITEM1.setText(String.format("%.0f",ITEM1_));

				ITEM2.setText(String.format("%.0f",ITEM2_));
			}

		}

		private void initialize(){

			DialogListener = new ActionListener() {
				public void actionPerformed(ActionEvent e) 
				{PhraseActionEvent(e);}};

				Toolkit kit = Toolkit.getDefaultToolkit();
				Dimension screen = kit.getScreenSize();
				frame = new JFrame();
				frame.getContentPane().setLayout(null);
				int frameWidth = (int)(ITEMWIDTH*4.4);
				int frameHeight = ITEMHEIGHT*14;
				frame.setBounds((int)(screen.width -frameWidth)/2,(int)(screen.height-frameHeight)/2,frameWidth ,frameHeight);
				frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
				final JTabbedPane tabbedPane = new JTabbedPane();
				tabbedPane.setBounds(0,0,(int)(ITEMWIDTH*4.2), ITEMHEIGHT*12);
				frame.getContentPane().add(tabbedPane);

				final JPanel panel = new JPanel();
				panel.setLayout(null);
				tabbedPane.addTab("Preferences", null, panel, null);

				final JLabel label = new JLabel("BallRadius/um");
				label.setBounds(0,0, ITEMWIDTH,ITEMHEIGHT);
				panel.add(label);
				BallRadius = new JTextField();
				BallRadius.setBounds(0, ITEMHEIGHT, ITEMWIDTH,ITEMHEIGHT);
				panel.add(BallRadius);

				final JLabel label1 = new JLabel("DNALength/um");
				label1.setBounds(ITEMWIDTH,0, ITEMWIDTH,ITEMHEIGHT);
				panel.add(label1);
				DNALength = new JTextField();
				DNALength.setBounds(ITEMWIDTH, ITEMHEIGHT, ITEMWIDTH,ITEMHEIGHT);
				panel.add(DNALength);

				final JLabel label2 = new JLabel("ZCalScale/um");
				label2.setBounds(ITEMWIDTH*2,0, ITEMWIDTH,ITEMHEIGHT);
				panel.add(label2);

				ZCalScale =new JComboBox<String>();		 
				ZCalScale.setModel(new DefaultComboBoxModel(new String[] { "2", "3",
						"4", "5", "6", "7", "8" }));
				ZCalScale.setBounds(ITEMWIDTH*2, ITEMHEIGHT, ITEMWIDTH,ITEMHEIGHT);
				panel.add(ZCalScale);


				final JLabel label3 = new JLabel("Frame2Acq");
				label3.setBounds(ITEMWIDTH*3, 0, ITEMWIDTH,ITEMHEIGHT);
				panel.add(label3);

				Frame2Acq = new JTextField();
				Frame2Acq.setBounds(ITEMWIDTH*3, ITEMHEIGHT, ITEMWIDTH,ITEMHEIGHT);
				panel.add(Frame2Acq);

				final JSeparator separator = new JSeparator();
				separator.setBounds(0,(int)(ITEMHEIGHT*2), ITEMWIDTH*4, 50);
				panel.add(separator);

				final JLabel label4 = new JLabel("ZCalStep/uM");
				label4.setBounds( 0, ITEMHEIGHT*3, ITEMWIDTH,ITEMHEIGHT);
				panel.add(label4);
				ZCalStep = new JTextField();
				ZCalStep.setBounds(0, ITEMHEIGHT*4, ITEMWIDTH,ITEMHEIGHT);
				panel.add(ZCalStep);

				final JLabel label5 = new JLabel("RinterStep/pixel");
				label5.setBounds( ITEMWIDTH, ITEMHEIGHT*3, ITEMWIDTH,ITEMHEIGHT);
				panel.add(label5);
				RinterStep = new JTextField();
				RinterStep.setBounds(ITEMWIDTH, ITEMHEIGHT*4, ITEMWIDTH,ITEMHEIGHT);
				panel.add(RinterStep);

				final JLabel label6 = new JLabel("HalfCorrWin");
				label6.setBounds( ITEMWIDTH*2, ITEMHEIGHT*3, ITEMWIDTH,ITEMHEIGHT);
				panel.add(label6);
				HalfCorrWin = new JTextField();
				HalfCorrWin.setBounds(ITEMWIDTH*2, ITEMHEIGHT*4, ITEMWIDTH,ITEMHEIGHT);
				panel.add(HalfCorrWin);

				final JLabel label7 = new JLabel("MagnetStep/uM");
				label7.setBounds( ITEMWIDTH*3, ITEMHEIGHT*3, ITEMWIDTH,ITEMHEIGHT);
				panel.add(label7);
				MagnetStep = new JTextField();
				MagnetStep.setBounds(ITEMWIDTH*3, ITEMHEIGHT*4, ITEMWIDTH,ITEMHEIGHT);
				panel.add(MagnetStep);
				final JSeparator separator1 = new JSeparator();
				separator1.setBounds(0,(int)(ITEMHEIGHT*5), ITEMWIDTH*4, 50);
				panel.add(separator1);

				final JLabel label8 = new JLabel("ITEM0");
				label8.setBounds( 0, ITEMHEIGHT*6, ITEMWIDTH,ITEMHEIGHT);
				panel.add(label8);
				ITEM0 = new JTextField();
				ITEM0.setBounds(0, ITEMHEIGHT*7, ITEMWIDTH,ITEMHEIGHT);
				panel.add(ITEM0);

				final JLabel label9 = new JLabel("ITEM1");
				label9.setBounds( ITEMWIDTH, ITEMHEIGHT*6, ITEMWIDTH,ITEMHEIGHT);
				panel.add(label9);
				ITEM1 = new JTextField();
				ITEM1.setBounds(ITEMWIDTH, ITEMHEIGHT*7, ITEMWIDTH,ITEMHEIGHT);
				panel.add(ITEM1);

				final JLabel label10 = new JLabel("ITEM2");
				label10.setBounds( ITEMWIDTH*2, ITEMHEIGHT*6, ITEMWIDTH,ITEMHEIGHT);
				panel.add(label10);
				ITEM2 = new JTextField();
				ITEM2.setBounds(ITEMWIDTH*2, ITEMHEIGHT*7, ITEMWIDTH,ITEMHEIGHT);
				panel.add(ITEM2);

				final JLabel label11 = new JLabel("FrameCalcF");
				label11.setBounds( ITEMWIDTH*3, ITEMHEIGHT*6, ITEMWIDTH,ITEMHEIGHT);
				panel.add(label11);
				FrameCalcF = new JTextField();
				FrameCalcF.setBounds(ITEMWIDTH*3, ITEMHEIGHT*7, ITEMWIDTH,ITEMHEIGHT);
				panel.add(FrameCalcF);
				final JSeparator separator2 = new JSeparator();
				separator2.setBounds(0,(int)(ITEMHEIGHT*8), ITEMWIDTH*4, 50);
				panel.add(separator2);

				final JButton OK = new JButton("OK");
				OK.setBounds(0,  (int)(ITEMHEIGHT*8.5),ITEMWIDTH,(int)(ITEMHEIGHT*1.5));
				panel.add(OK);
				final JButton Cancel = new JButton("Cancel");
				Cancel.setBounds(ITEMWIDTH, (int)(ITEMHEIGHT*8.5), ITEMWIDTH,(int)(ITEMHEIGHT*1.5));
				panel.add(Cancel);
				final JButton SelectDir = new JButton("SelectDir");
				SelectDir.setBounds(ITEMWIDTH*2, (int)(ITEMHEIGHT*8.5), ITEMWIDTH,(int)(ITEMHEIGHT*1.5));
				panel.add(SelectDir);
				final JButton OpenDir = new JButton("OpenDir");
				OpenDir.setBounds(ITEMWIDTH*3, (int)(ITEMHEIGHT*8.5), ITEMWIDTH,(int)(ITEMHEIGHT*1.5));
				panel.add(OpenDir);

				OK.addActionListener(DialogListener);
				Cancel.addActionListener(DialogListener);
				SelectDir.addActionListener(DialogListener);
				OpenDir.addActionListener(DialogListener);
		}

		private void PhraseActionEvent(ActionEvent e){
			if (e.getActionCommand().equals("OK")) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						myform_.log(String.format("Preferences Change,reCalibrate is recommend ."));
						mygui_.SetScale(getPreferData());
						UpdateData(true);//flush
						frame.setVisible(false);
					}
				});

			}
			if (e.getActionCommand().equals("Cancel")) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						frame.setVisible(false);
					}
				});

			}

			if (e.getActionCommand().equals("SelectDir")) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						JFileChooser fileChooser = new JFileChooser(".");		 
						fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
						fileChooser.setDialogTitle("打开文件夹");
						int ret = fileChooser.showOpenDialog(null);
						if (ret == JFileChooser.APPROVE_OPTION) {
							setDataDir_(fileChooser.getSelectedFile().getAbsolutePath());
							myform_.log(String.format("Current DataDir is:%s.",getDataDir_()));
						}
					}
				});

			}

			if (e.getActionCommand().equals("OpenDir")) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						try {
							Runtime.getRuntime().exec("explorer /select, "+getDataDir_());
						} catch (IOException e) {
							e.printStackTrace();
						}
						frame.setVisible(false);
					}
				});

			}


		}


		public HashMap<String, Double> getPreferData() {
			return Opt_;
		}
		public void packPreferData() {

			Opt_.put("BallRadius",BallRadius_);

			Opt_.put("DNALength",DNALength_);

			Opt_.put("ZCalScale",ZCalScale_);

			Opt_.put("ZCalStep",ZCalStep_);

			Opt_.put("RinterStep",RinterStep_);

			Opt_.put("HalfCorrWin",HalfCorrWin_);

			Opt_.put("MagnetStep",MagnetStep_);

			Opt_.put("Frame2Acq",Frame2Acq_);

			Opt_.put("FrameCalcF",FrameCalcF_);

			Opt_.put("ITEM0",ITEM0_);

			Opt_.put("ITEM1",ITEM1_);

			Opt_.put("ITEM2",ITEM2_);


		}


		public String getDataDir_() {
			return dataDir_;
		}


		public void setDataDir_(String dataDir_) {
			this.dataDir_ = dataDir_;
		}
	}



}
