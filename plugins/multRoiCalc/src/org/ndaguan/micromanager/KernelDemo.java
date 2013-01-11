package org.ndaguan.micromanager;

import java.util.Random;

public class KernelDemo {
	private Random rand_;

	public KernelDemo(){
		rand_ = new Random();
	}

	Object[] GosseCenter(Object image, double[][] roi,double[] opt){
		int size = roi.length;
		double[][] ret = new double[size][2];
		for (int i = 0; i < ret.length; i++) {
			for (int j = 0; j < ret[0].length; j++) {
				ret[i][j] = roi[i][j]+Math.round(rand_.nextGaussian());;
			}
		}
		return ret;
	}	

	Object[] Calibration(Object image,  double[][] roi,double[] opt, int zIndex){
		return roi;
	}

	Object[] GetZPosition(Object image, double[][] roi,double[] opt){
		int size = roi.length;
		double[][] ret = new double[size][5];
		for (int i = 0; i < ret.length; i++) {
			for (int j = 0; j < 2; j++) {
				ret[i][j] = roi[i][j]+Math.round(rand_.nextGaussian()/2);
			}
			ret[i][2] = Math.round(2 * rand_.nextGaussian());
			ret[i][3] = Math.round(2 * rand_.nextGaussian());
			ret[i][4] = Math.round(2 * rand_.nextGaussian());
		}

		return ret;
	}


}
