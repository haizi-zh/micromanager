package org.ndaguan.micromanager;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
public class ItemData{
	double x_;
	double y_;
	double z_;
	double fx_;
	double fy_;
    DescriptiveStatistics statCross_;
	DescriptiveStatistics[] stats_;
	private int windowSize_ = 1000;
	public ItemData(double x,double y,double z,double fx,double fy){
		x_ = x;
		y_ = y;
		z_ = z;
		fx_ = fx;
		fy_ = fy;
		stats_ = new DescriptiveStatistics[3];
		for (int i = 0; i < stats_.length; i++) {
			stats_[i] = new DescriptiveStatistics(windowSize_);
		}
		statCross_ = new DescriptiveStatistics(windowSize_);
	}
	public ItemData(double[] itemData){
		x_ = itemData[0];
		y_ = itemData[1];
		z_ = itemData[2];
		fx_ = itemData[3];
		fy_ = itemData[4];
		stats_ = new DescriptiveStatistics[2];
		for (int i = 0; i < stats_.length; i++) {
			stats_[i] = new DescriptiveStatistics(windowSize_);
		}
		statCross_ = new DescriptiveStatistics(windowSize_);
	}
	public void setItemData(double[] itemData){
		x_ = itemData[0];
		y_ = itemData[1];
		z_ = itemData[2];
		fx_ = itemData[3];
		fy_ = itemData[4];
	}
	public double[] getItemData(){
		return new double[]{x_,y_,z_,fx_,fy_};
	}
}
