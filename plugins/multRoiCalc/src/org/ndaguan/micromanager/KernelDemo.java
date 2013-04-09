package org.ndaguan.micromanager;

import java.util.Random;

public class KernelDemo {
	private Random rand_;

	public KernelDemo(){
		rand_ = new Random();
	}

	double[] gosseCenter(Object image, double[][] roi){
		int size = roi.length;
		double[] ret = new double[size*3];
		for (int i = 0; i <  size; i++) {
			for (int k = 0; k < 2; k++) {
				ret[i*3+k] = roi[i][k]+Math.round(rand_.nextGaussian());
			}
			ret[i*3+2] =Math.round(rand_.nextGaussian());
		}
		return ret;
	}	

	double[] calibration(Object image,  double[][] roi,int zIndex,double zPos){
		
		return new double[]{roi[0][0]*10,roi[0][1]*10};
	}

	double[] getZPosition(Object image, double[][] roi){
		int size = roi.length;
		double[] ret = new double[size*3];
		for (int i = 0; i <  size; i++) {
			for (int k = 0; k < 2; k++) {
				ret[i*3+k] = roi[i][k]+Math.round(rand_.nextGaussian());
			}
			ret[i*3+2] =Math.round(rand_.nextGaussian());
		}
		return ret;
	}
}
