package org.ndaguan.micromanager.mmtracker;

import java.awt.Color;
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

/**
 * @author Administrator
 *
 */
public  class RoiItem {
	private static int counter = 0;
	private Color itemColor_;
	int index_ = 0;
	
	public boolean isSelected_  = false;
	public boolean isFocus_ = true;

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
	DescriptiveStatistics statCross_;
	DescriptiveStatistics[] stats_;

	private int windowSize_ = 2000;
	private Preferences preferences_;

	public static RoiItem createInstance(Preferences preferences,double[] itemData,String titleName) {
		return new RoiItem(preferences,itemData,titleName);
	}
	private RoiItem(Preferences preferences, double[] itemData,String titleName) {
		index_ = counter;
		counter ++;

		preferences_ = preferences;
		isSelected_ = false;
		setItemColor(Color.GREEN);
		chart_ = new ChartManager(MMT.CHARTLIST,1000,String.format("%s-----%d",titleName,index_));

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
			dataFileWriter_.flush();
			dataFileWriter_.close();
			dataFileWriter_ = null;
		}
		if(chart_ != null){
			for (int i = 0; i < MMT.CHARTLIST.length - 1; i++) {
				if(MMT.CHARTLIST[i].equals("Chart-Testing"))
					continue;
				chart_.getDataSeries().get(MMT.CHARTLIST[i]).clear();
			}
		}

		for (DescriptiveStatistics stat : stats_)
			stat.clear();

		statCross_.clear();
	}


	public boolean writeData(String acqName,long frameNum_,double elapsed) throws IOException{
		if (dataFileWriter_ == null) {
			Calendar cal = new GregorianCalendar();
			DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
			File dir = new File(new File(preferences_.userDataDir_, "MTTracker"),
					dateFormat.format(cal.getTime()));

			dir.mkdirs();

			dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
			File file = new File(dir, dateFormat.format(cal.getTime()) + "_"
					+ acqName + "_bean_" + String.format("%d", index_) + "_" + ".txt");
			dataFileWriter_ = new BufferedWriter(new FileWriter(file));

			dataFileWriter_
			.write("Frame, Timestamp, XPos/pixel,XPos/um, YPos/pixel, YPos/um, ZPos/um,ForceX/pN,ForceY/pN,Std(x/y),skrewnessy\r\n");
			dataFileWriter_.flush();
		}
		else{
			dataFileWriter_
			.write(String.format("%d,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f\r\n",frameNum_,elapsed,x_,xPhy_,y_,yPhy_,z_,fx_,fy_,stdXdY_,skrewness_));
			dataFileWriter_.flush();
		}
		return true;

	}
}
