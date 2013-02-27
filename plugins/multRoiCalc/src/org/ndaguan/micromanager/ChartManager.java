package org.ndaguan.micromanager;

import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class ChartManager extends JFrame  {
	private String[] dataSet;
	private JFreeChart chart;
	private HashMap<String, XYSeries> dataSeries_;
	private HashMap<String, JFreeChart> chartSeries_;
	private XYSeriesCollection dataset_;
	private int ChartMaxItemCount = 2000;
	private final int DEFAULT_WIDTH = 660;
	int tapSize = 600;
	private final int DEFAULT_HEIGHT =(int)( DEFAULT_WIDTH*0.618);

	public HashMap<String, XYSeries> getDataSeries(){
		return dataSeries_;
	}
	public HashMap<String, JFreeChart> getChartSeries(){
		return chartSeries_;
	}
	
	public void setMaxCount(String chartName,int acount)
	{
		dataSeries_.get(chartName).setMaximumItemCount(acount);
	}

	public  static void main(String[] arg){
		String[] sdataSet = new String[]{"Chart-Z","Chart-X","Chart-Y","Chart-FX","Chart-FY"};
		String titleName = "bean1";
		ChartManager chartManager = new ChartManager(sdataSet,2000,titleName );
		chartManager.setVisible(true);
	}

	ChartManager(String[] dataSet_, int maxCount, String titleName){
		ChartMaxItemCount = maxCount;
		dataSet =dataSet_;
		dataSeries_ = new HashMap<String,XYSeries>();
		chartSeries_ = new HashMap<String,JFreeChart>();
		setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
		this.setDefaultCloseOperation(HIDE_ON_CLOSE);
		this.setTitle(titleName);
		initialize();
		
	}
	void initialize(){
		//tabbedPane
		final JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.setBounds(0,0, tapSize, (int)(tapSize*0.618));
		getContentPane().add(tabbedPane);
		final JScrollPane scrollPane = new JScrollPane();

		for (int i = 0; i <  dataSet.length; i++) {
			tabbedPane.addTab( dataSet[i], null, createChartPanel(dataSet[i]), null);
		}
	}

	private JPanel createChartPanel(String tableName) {
		if(dataSeries_.containsKey(tableName))
			return null;

		final XYSeries temp_ =  new XYSeries(tableName);

		temp_.setMaximumItemCount(ChartMaxItemCount );
		dataset_ = new XYSeriesCollection();
		dataset_.addSeries(temp_);
		chart = ChartFactory.createXYLineChart(tableName, "-Time",
				"-value", dataset_, PlotOrientation.VERTICAL, true, true,
				false);

		chartSeries_.put(tableName, chart);
		dataSeries_.put(tableName,temp_);	

		ChartPanel cPanel = new ChartPanel(chart, true);
		cPanel.setBounds(10, 10, tapSize, (int)(tapSize*0.6));

		final JSlider slider = new JSlider(JSlider.VERTICAL);
		slider.setMinimum(1);
		slider.setValue(50);
		slider.setMaximum(100);
		slider.setBounds(0, 0, 10, (int)(tapSize*0.618));

		final JSlider hslider = new JSlider(JSlider.HORIZONTAL);
		hslider.setMinimum(1);
		hslider.setValue(50);
		hslider.setMaximum(100);
		hslider.setBounds(15, 0,tapSize-20,10);

		JPanel panel = new JPanel();
		panel.setLayout(null);
		panel.add(slider);
		panel.add( cPanel);
		panel.add(hslider);		

		return panel;
	}
}
