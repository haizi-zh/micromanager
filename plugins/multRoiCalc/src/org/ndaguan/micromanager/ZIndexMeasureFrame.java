package org.ndaguan.micromanager;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
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


public class ZIndexMeasureFrame extends JFrame {
	private static ZIndexMeasureFrame instance_;


	private static final long serialVersionUID = 1L;
	final String[] PARALIST  = new String[]{"BallRadius","DNALength","ZCalScale_","ZCalStep","RinterStep","HalfCorrWin","MagnetStep","Frame2Acq","FrameCalcF","ITEM0","ITEM1","ITEM2"};
	private JFreeChart chart = null;

	private int ChartMaxItemCount = 1000;

	private final int DEFAULT_WIDTH = 630;
	private final int DEFAULT_HEIGHT =(int)( DEFAULT_WIDTH*0.618);
	private final int DEFAULT_LOCATION_X = 0;
	private final int DEFAULT_LOCATION_Y = 0;
	private final String DEFAULT_TITLE = "Welcome......";
	private final String DEFAULT_IMAGE = "icon/I.gif";
	private final int DEFAULT_CLOSE_OPERATION =JFrame.HIDE_ON_CLOSE;
	private int[] tapSize = null;

	private ButtonGroup buttonGroup_0 = new ButtonGroup();
	ActionListener menuListener = null;

	private JTextArea LogWindow = new JTextArea(40, (int)(40*0.618));
	private int currTab;
	private HashMap<String, XYSeries> dataSeries_;
	private HashMap<String, JFreeChart> chartSeries_;
	private XYSeriesCollection dataset_;
	private String[] dataSet;
	private JRadioButtonMenuItem MagnetAuto;
	private JRadioButtonMenuItem MagnetManual;

	private static Listener listener_;
	public PreferDailog preferDailog;
	private Function function_;

	 
	public static ZIndexMeasureFrame getInstance(Listener listener, Function function) {
		if(instance_ == null)
			instance_ = new ZIndexMeasureFrame(listener, function);
		return instance_;
	}
	public static ZIndexMeasureFrame getInstance() {		
		return instance_;
	}

	public ZIndexMeasureFrame(Listener listener,Function function) {

		listener_ = listener;
		function_ = function;
		tapSize = new int[]{600,900,800};
		setDataSeries_(new HashMap<String,XYSeries>());
		setChartSeries_(new HashMap<String,JFreeChart>());
		dataSet  = new String[]{"Chart-Z","Chart-X","Chart-Y","Chart-XY","Chart-Z-STD","Chart-X-STD","Chart-Y-STD"};

		preferDailog = new PreferDailog(function);
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

		Calibrate.addActionListener(listener_);
		Live.addActionListener(listener_);
		MultiAcq.addActionListener(listener_);		
		Preferences.addActionListener(listener_);

		Calibrate.setToolTipText("Calibrate");
		Live.setToolTipText("Live view");
		MultiAcq.setToolTipText("Mutil-ACQ with the default preferences");
		Preferences.setToolTipText("Preferences");

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

		final int toolItemWidth = 120;
		final int toolItemHeight = 20;
		final int toolbarheight = toolItemHeight+10;
		final JToolBar toolBar = new JToolBar();
		int itemNum = 5;
		toolBar.setBounds(0,0, toolItemWidth*itemNum , toolbarheight);

		Toolkit kit = Toolkit.getDefaultToolkit();
		final	Image imgC = kit.getImage("icon/C.gif");
		final	Image imgL = kit.getImage("icon/L.gif");
		final	Image imgM = kit.getImage("icon/M.gif");
		final	Image imgS = kit.getImage("icon/S.gif");
		final	Image imgH = kit.getImage("icon/H.gif");
		final	Image imgSet = kit.getImage("icon/set.gif");
		final	Image imgI = kit.getImage("icon/I.gif");

		JButton butC = new JButton();
		JButton butL = new JButton();
		JButton butM = new JButton();
		JButton butS = new JButton();
		JButton butSet = new JButton();
		JButton butH = new JButton();
		JButton butI = new JButton();


		int offsety = 0;
		butI.setIcon(new javax.swing.ImageIcon(imgI)); // NOI18N
		butI.setToolTipText("Install callback");
		butI.setFocusable(false);
		butI.setBounds(offsety,0, toolItemWidth, toolItemHeight);
		offsety += toolItemWidth;	

		butSet.setIcon(new javax.swing.ImageIcon(imgSet)); // NOI18N
		butSet.setToolTipText("Set up");
		butSet.setFocusable(false);
		butSet.setBounds(offsety,0, toolItemWidth, toolItemHeight);
		offsety += toolItemWidth;




		butC.setIcon(new javax.swing.ImageIcon(imgC)); // NOI18N
		butC.setToolTipText("Calibrate");
		butC.setFocusable(false);
		butC.setBounds(offsety,0, toolItemWidth, toolItemHeight);
		offsety += toolItemWidth;

		butL.setIcon(new javax.swing.ImageIcon(imgL)); // NOI18N
		butL.setToolTipText("Live view");
		butL.setFocusable(false);
		butL.setBounds(offsety,0, toolItemWidth, toolItemHeight);
		offsety += toolItemWidth;

		butM.setIcon(new javax.swing.ImageIcon(imgM)); // NOI18N
		butM.setToolTipText("Mutil-ACQ with the default preferences");
		butM.setFocusable(false);
		butM.setBounds(offsety,0, toolItemWidth, toolItemHeight);
		offsety += toolItemWidth;

		butS.setIcon(new javax.swing.ImageIcon(imgS)); // NOI18N
		butS.setToolTipText("Rectangle tool for select a ROI");
		butS.setFocusable(false);
		butS.setBounds(offsety,0, toolItemWidth, toolItemHeight);
		offsety += toolItemWidth;

		butH.setIcon(new javax.swing.ImageIcon(imgH)); // NOI18N
		butH.setToolTipText("Hand tool for moving the xyStage");
		butH.setFocusable(false);
		butH.setBounds(offsety,0, toolItemWidth, toolItemHeight);
		offsety += toolItemWidth;


		butL.addActionListener(listener_); 		 
		butC.addActionListener(listener_); 
		butM.addActionListener(listener_);  
		butSet.addActionListener(listener_); 		 
		butS.addActionListener(listener_); 			 
		butH.addActionListener(listener_);  
		butI.addActionListener(listener_); 

		toolBar.add(butI);
		toolBar.add(butSet);
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

		getChartSeries_().put(tableName, chart);
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
			}
		}
				);

		slider.addChangeListener(new ChangeListener() { 

			public void stateChanged(ChangeEvent e)
			{
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

	public HashMap<String, JFreeChart> getChartSeries_() {
		return chartSeries_;
	}

	public void setChartSeries_(HashMap<String, JFreeChart> chartSeries_) {
		this.chartSeries_ = chartSeries_;
	}

}
