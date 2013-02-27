package org.ndaguan.micromanager;

import ij.WindowManager;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public  class RoiItem {
	private Color itemColor_;
	boolean isSelected_;
	boolean isFocus_ = true;
	public boolean isdelete_ = false;

	double x_ = 0 ;
	double xPhy_ = 0 ;
	double y_ = 0 ;
	double yPhy_ = 0 ;
	double z_ = 0 ;
	double fx_ = 0 ;
	double fy_ = 0 ;
	double skrewness_ = 0;
	double stdXdY_= 0;

	Writer dataFileWriter_;

	ChartManager chart_ = null;
	private String[] sdataSet;
	DescriptiveStatistics statCross_;
	DescriptiveStatistics[] stats_;
	
	private int windowSize_ = 1000;

	public static RoiItem createInstance(double[] itemData,boolean isSelected,String titleName) {
		return new RoiItem(itemData,isSelected,titleName);
	}
	private RoiItem(double[] itemData,boolean isSelected,String titleName) {
		if(isSelected){
			setItemColor(Color.RED);
		}
		else{
			setItemColor(Color.GREEN);
		}

		sdataSet = new String[]{"Chart-Z","Chart-X","Chart-Y","Chart-FX","Chart-FY","Chart-STDXDY","Chart-SKREWNESS"};
		chart_ = new ChartManager(sdataSet,1000,titleName);

		x_ = itemData[0];
		y_ = itemData[1];

		stats_ = new DescriptiveStatistics[3];
		for (int i = 0; i < stats_.length; i++) {
			stats_[i] = new DescriptiveStatistics(windowSize_);
		}
		statCross_ = new DescriptiveStatistics(windowSize_);

	}
	public String getMsg(){
		return String.format("(%.2f, %.2f,%.2f)(%.2f,%.2f)",xPhy_,yPhy_,z_,fx_,fy_);
	}
	public void setSelect(boolean isSelected){
		isSelected_ = isSelected;
		if(isSelected){
			setItemColor(Color.RED);
		}
		else{
			setItemColor(Color.GREEN);
		}

	}
	public Color getItemColor() {
		return itemColor_;
	}

	public void setItemColor(Color clr) {
		itemColor_ = clr;
	}

	public void setItemData(double[] itemData){
		x_ = itemData[0];
		xPhy_ = itemData[1];
		y_ = itemData[2];
		yPhy_ = itemData[3];
		z_ = itemData[4];
		fx_ = itemData[5];
		fy_ = itemData[6];
		stdXdY_ = itemData[7];
		skrewness_ = itemData[8];
	}
	public double[] getItemData(){
		return new double[]{x_,xPhy_,y_,yPhy_,z_,fx_,fy_};
	}
	public void dataClean() throws IOException{

		if (dataFileWriter_ != null) {
			dataFileWriter_.close();
			dataFileWriter_ = null;
		}
		if(chart_ != null){
			chart_.getDataSeries().get("Chart-X").clear();
			chart_.getDataSeries().get("Chart-Y").clear();
			chart_.getDataSeries().get("Chart-Z").clear();
			chart_.getDataSeries().get("Chart-FX").clear();
			chart_.getDataSeries().get("Chart-FY").clear();
			chart_.getDataSeries().get("Chart-STDXDY").clear();
			chart_.getDataSeries().get("Chart-SKREWNESS").clear();
		}
		for (DescriptiveStatistics stat : stats_)
			stat.clear();
		statCross_.clear();
	}
	
	public void writeData(String dirName,String fileName,long frameNum_,double elapsed) throws IOException{
		if (dataFileWriter_ == null) {
			Calendar cal = new GregorianCalendar();
			DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
			File dir = new File(new File(dirName, "ZIndexMeasure"),
					dateFormat.format(cal.getTime()));
			dir.mkdirs();

			dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
			File file = new File(dir, dateFormat.format(cal.getTime()) + "_"
					+ fileName + ".txt");
			dataFileWriter_ = new BufferedWriter(new FileWriter(file));
			dataFileWriter_
			.write("Frame, Timestamp, XPos/pixel,XPos/um, YPos/pixel, YPos/um, ZPos/um,ForceX/pN,ForceY/pN,Std(x/y),skrewnessy\r\n");
			dataFileWriter_.flush();
		}
		else{
			dataFileWriter_
			.write(String.format("%d,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f\r\n",frameNum_,elapsed,x_,xPhy_,y_,yPhy_,z_,fx_,fy_,stdXdY_,skrewness_));
		
		}
	}
}
