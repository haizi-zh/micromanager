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

import javax.swing.SwingUtilities;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;

/**
 * @author Administrator
 *
 */
public  class RoiItem {
	private static int counter = 0;
	private Color itemColor_;
	private int index_ = 0;

	private  boolean isSelected_  = false;
	private  boolean isFocus_ = true;

	private double x_ = 0 ;
	private double xPhy_ = 0 ;
	private double y_ = 0 ;
	private double yPhy_ = 0 ;
	private double zPhy_ = 0 ;
	private double fx_ = 0 ;
	private double fy_ = 0 ;
	private double skrewness_ = 0;
	private double stdXdY_= 0;

	private Writer dataFileWriter_;
	private double[][] calProfile_ = null;
	private ChartManager chart_ = null;
	private DescriptiveStatistics statCross_;

	private DescriptiveStatistics[] XYZStatis_;
	private DescriptiveStatistics[] miniXYZStatis_;
	private int chartWindowLen;

	public static RoiItem createInstance(double[] itemData,String titleName) {
		return new RoiItem(itemData,titleName);
	}
	public boolean isFocus(){
		return isFocus_;
	}
	public void setFocus(boolean flag){
		isFocus_ = flag;
	}
	private RoiItem( double[] itemData,String titleName) {
		index_ = counter;
		counter ++;
		isSelected_ = false;
		setItemColor(Color.GREEN);
		chart_ = new ChartManager(MMT.CHARTLIST,(int) MMT.VariablesNUPD.chartWindowSize.value(),String.format("%s-----%d",titleName,index_));

		x_ = itemData[0];
		y_ = itemData[1];

		int windowSize_ = (int) MMT.VariablesNUPD.frameToCalcForce.value();
		int miniWindowSize_ = (int) MMT.VariablesNUPD.chartStatisWindow.value();

		XYZStatis_ = new DescriptiveStatistics[2];
		miniXYZStatis_ = new DescriptiveStatistics[3];
		for (int i = 0; i < XYZStatis_.length; i++) {
			XYZStatis_[i] = new DescriptiveStatistics(windowSize_);
		}
		for (int i = 0; i < miniXYZStatis_.length; i++) {
			miniXYZStatis_[i] = new DescriptiveStatistics(miniWindowSize_);
		}
		statCross_ = new DescriptiveStatistics(windowSize_);



	}
	public void setWidowSize(int size){
		for(DescriptiveStatistics stat: XYZStatis_)
			stat.setWindowSize(size);
	}
	 
	public void setWidowSize(double size){
		for(DescriptiveStatistics stat: XYZStatis_)
			stat.setWindowSize((int)size);
	}
	public void setChartDrawingWidowSize(int size){
		chart_.setChartDrawingWindowSize(size);
	}
	
	public void setChartWidowSize(int size){
		for(DescriptiveStatistics stat: miniXYZStatis_)
			stat.setWindowSize(size);
	}
	public void setChartWidowSize(double size){
		for(DescriptiveStatistics stat: miniXYZStatis_)
			stat.setWindowSize((int)size);
	}

	public String getMsg(){
		return String.format("\\(%.2f, %.2f,%.2f)/",xPhy_,yPhy_,zPhy_);
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

	public void dataClean(boolean flag){

		if (dataFileWriter_ != null) {
			try {
				dataFileWriter_.close();
			} catch (IOException e) {
				MMT.logError("DataWriter close false!"+e.toString());
			}
			dataFileWriter_ = null;
		}
		if(chart_ != null){
			for (int i = 0; i < MMT.CHARTLIST.length - 1; i++) {
				if(!flag && MMT.CHARTLIST[i].equals("Chart-Testing"))
					continue;
				chart_.getDataSeries().get(MMT.CHARTLIST[i]).clear();
			}
		}

		for (DescriptiveStatistics stat : XYZStatis_)
			stat.clear();
		for (DescriptiveStatistics stat : miniXYZStatis_)
			stat.clear();

		statCross_.clear();
	}
	public void clearStaticData() {
		for (DescriptiveStatistics stat : XYZStatis_)
			stat.clear();
		for (DescriptiveStatistics stat : miniXYZStatis_)
			stat.clear();
		statCross_.clear();
	}

	public boolean writeData(String acqName,long frameNum_,double elapsed) throws IOException{
		if (dataFileWriter_ == null) {
			Calendar cal = new GregorianCalendar();
			DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
			File dir = new File(new File(MMTFrame.getInstance().preferDailog.userDataDir_, "MTTracker"),
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
			.write(String.format("%d,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f\r\n",frameNum_,elapsed,x_,xPhy_,y_,yPhy_,zPhy_,fx_,fy_,stdXdY_,skrewness_));
		}
		return true;

	}


	public double[] getMean() {
		double pointNum = 0.01;
		return new double[]{((int)(miniXYZStatis_[0].getMean()/pointNum))*pointNum,((int)(miniXYZStatis_[1].getMean()/pointNum))*pointNum,((int)(miniXYZStatis_[2].getMean()/pointNum))*pointNum};
	}
	private double[] getStandardDeviation() {
		return new double[]{miniXYZStatis_[0].getStandardDeviation(),miniXYZStatis_[1].getStandardDeviation(),miniXYZStatis_[2].getStandardDeviation()};
	}
	public double[] getDrawScale() {
		double min = 0.05;
		double[] std = getStandardDeviation();
		for(int i = 0;i<std.length;i++)
			std[i] = std[i]*6;
		return new double[]{std[0]<min?min:std[0],std[1]<min?min:std[1],std[2]<min?min:std[2]};
	}

	public void updateDataSeries(final long frameNum) {
		if(!chart_.isVisible())return;
		boolean flag = false;
		if(frameNum%MMT.VariablesNUPD.frameToRefreshChart.value() == 0)
			flag = true;
		else
			flag = false;

		final boolean update = flag;
		final int selectedIndex = chart_.getSelectedTap();
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run() {
				double data[] = getItemData();
				for(int i = 0;i<data.length;i++){
					chart_.getDataSeries().get(MMT.CHARTLIST[i]).add(frameNum,data[i],update&&(i == selectedIndex));
				}

				if(update){
					double[] mean = getMean();
					double[] drawScale = getDrawScale();
					chart_.getChartSeries().get("Chart-X").getXYPlot().getRangeAxis().setRange(mean[0] - drawScale[0],mean[0] + drawScale[0]);
					chart_.getChartSeries().get("Chart-Y").getXYPlot().getRangeAxis().setRange(mean[1] - drawScale[1],mean[1] + drawScale[1]);
					chart_.getChartSeries().get("Chart-Z").getXYPlot().getRangeAxis().setRange(mean[2] - drawScale[2],mean[2] + drawScale[2]);
				}
			}

		});

	}
	private double[] getItemData(){
		return new double[]{zPhy_,xPhy_,yPhy_,fx_,fy_,stdXdY_,skrewness_};
	}
	public double[] getXY() {
		return new double[]{x_,y_};
	}
	public double getZ() {
		return zPhy_;
	}
	public void setXY(double xPos, double yPos) {//moving ROI
		x_ = xPos;
		y_ = yPos;
	}
	public void setXY(double[] pos){
		//pixel
		x_ = pos[0];
		y_ = pos[1];
		//uM
		xPhy_ = MMT.VariablesNUPD.pixelToPhysX.value() * x_;
		yPhy_ = MMT.VariablesNUPD.pixelToPhysY.value() * y_;
		//nM:calculate Force with a bigger windowSize
		XYZStatis_[0].addValue(xPhy_*1000);
		XYZStatis_[1].addValue(yPhy_*1000);
		statCross_.addValue(xPhy_ * yPhy_ * 1e6);
		//uM:get mean&standardDeviation  to update chart with a smaller windowSize;
		miniXYZStatis_[0].addValue(xPhy_);
		miniXYZStatis_[1].addValue(yPhy_);

	}
	public void setZ(double zpos) {
		zPhy_ = zpos;
		miniXYZStatis_[2].addValue(zPhy_);
	}
	public boolean isSelected() {
		return isSelected_;
	}
	public void setSelected(boolean flag){
		isSelected_ = flag;
	}
	public void setChartVisible(boolean flag) {
		chart_.setVisible(flag);
	}
	public void addChartData(String string, double x, double y) {
		XYSeries dataSeries = chart_.getDataSeries().get(string);
		if(dataSeries != null)
			dataSeries.add(x,y);		
	}
	public void clearChart(String string) {
		final XYSeries dataSeries = chart_.getDataSeries().get(string);
		if(dataSeries != null){
			SwingUtilities.invokeLater(new Runnable(){
				@Override
				public void run() {
					dataSeries.clear();	
				}});
		}

	}
	public DescriptiveStatistics[] getStats() {
		return XYZStatis_;
	}
	public DescriptiveStatistics getStatCross() {
		return statCross_;
	}
	public void setForce(double[] force) {
		fx_ = force[0];
		fy_ = force[1];
	}
	public void setSkrewness(double[] skrewneww) {
		stdXdY_ = skrewneww[0];
		skrewness_ = skrewneww[1];
	}
	public double[] getXYZPhy() {
		return new double[]{xPhy_,yPhy_,zPhy_};
	}
 
	public String getName() {
		return String.format("%d      %d",index_,index_);
	}
	public void updateCalProfile(int index, double[] posProfile) {
		calProfile_[index] = posProfile;
	}
	public void InitializeCalProflie(double[][] cal) {
		calProfile_ = cal;
	}
	public void clearCalProfile() {
		calProfile_ = null;		
	}
	public double[][] getCalProfile() {
		return calProfile_;
	}
	public void setSelectTap(String string) {
		int i=0;
		for(String s:MMT.CHARTLIST){
			if(s.equals(string)){
				chart_.setSelectTap(i);
				break;
			}
			else{
				i++;
			}
		}
	}

}
