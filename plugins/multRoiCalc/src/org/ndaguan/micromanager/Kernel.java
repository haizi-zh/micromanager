package org.ndaguan.micromanager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.solvers.LaguerreSolver;
import org.apache.commons.math3.exception.NoBracketingException;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.ndaguan.micromanager.OverlayRender.RenderItem;


public class Kernel {

	private boolean isCalibrated = false;
	private KernelDemo demo;
	private Preferences preferences_;
	private ArrayList<RenderItem> roiList_;
	private Function function_;
	private static Kernel kernel_;
	public Kernel(Function function,
			ArrayList<RenderItem> roiList, Preferences preferences){
		function_ = function;
		roiList_ = roiList;
		preferences_ = preferences;
		this.initialize(new double[]{preferences_.beanRadiusPiexl_,
				preferences_.rInterpStep_,
				preferences_.bitDepth_,
				preferences_.halfQuadWindow_,
				preferences_.imgwidth_,
				preferences_.imgheight_,
				preferences_.zStart_,
				preferences_.zCalRange_,
				preferences_.zCalStep_,
				preferences_.preservation1_,
				preferences_.preservation2_});
	}
	public Kernel() {
		// TODO Auto-generated constructor stub
	}
 
	public static  Kernel getInstance() {
		return kernel_;
	}
	public static Kernel getInstance(Function function,
			ArrayList<RenderItem> roiList, Preferences preferences) {
		if(kernel_ == null){
			kernel_ = new Kernel(function,roiList, preferences);
			return kernel_;
		}
		else{
			return kernel_;
		}
		
	}
	static {
		System.loadLibrary("Kernel");
	}

	public double[] getXYPosition(Object image){
		double[] ret = gosseCenter(image,function_.getRoiList());
		for (int i = 0; i < ret.length/3; i++) {
			if(Math.floor(ret[3*i]) == -1)
				continue;
			roiList_.get(i).itemdata_.setItemData(new double[]{ret[3*i],ret[3*i+1],0,0,0});		
			//double intensity = ret[3*i+2];
		}
		return ret;
	}

	public double[] calibration(Object image, int zIndex,double zPos){
		double[] ret = calibration(image,function_.getRoiList(),zIndex,zPos);
		for (int i = 0; i < ret.length/2; i++) {
			roiList_.get(i).itemdata_.setItemData(new double[]{ret[2*i],ret[2*i+1],0,0,0});		
		}
		return ret;
	}

	public double[] getPosition(Object image){		
		if (!isCalibrated) {
			double[] ret = getXYPosition(image);
			function_.reSetFocusRoi();
			return ret;
		}
		double xPhys;
		double yPhys;
		double zPhys;
		double[] force = new double[]{0,0};
		double[] pixelToPhys = preferences_.pixelToPhys_;
		double[] ret = getZPosition(image,function_.getRoiList());
		for (int i = 0; i < ret.length/3; i++) {
			if((ret[3*i])+1 < 0.001)
				continue;
			xPhys = pixelToPhys[0] + pixelToPhys[1] * ret[i*3];
			yPhys = pixelToPhys[2] + pixelToPhys[3] * ret[i*3+1];
			zPhys =  ret[i*3+2];

			addRoiStatsValue(i,xPhys,yPhys,zPhys);

			if (isEnoughDataToCalcForce())
			{
				force = calcForces(roiList_.get(i).itemdata_.stats_);
				@SuppressWarnings("unused")
				double[] skrewneww = calcSkrewness(roiList_.get(i).itemdata_.stats_,roiList_.get(i).itemdata_.statCross_);
			}
			function_.reSetFocusRoi();
			saveValueAndUpdate(i,new double[]{xPhys,yPhys,zPhys,force[0],force[1]});
		}
		return ret;
	}

	private void saveValueAndUpdate(int i,double[] data) {
		roiList_.get(i).itemdata_.setItemData(data);		
	}

	private boolean isEnoughDataToCalcForce() {
		return roiList_.get(0).itemdata_.stats_[0].getN() >= preferences_.minAnalyzeWindow_;
	}
	private void addRoiStatsValue(int i,double xPhys,double yPhys, double zPhys) {
		roiList_.get(i).itemdata_.stats_[0].addValue(xPhys * 1000);
		roiList_.get(i).itemdata_.stats_[1].addValue(yPhys * 1000);
		roiList_.get(i).itemdata_.stats_[2].addValue(zPhys * 1000);
		roiList_.get(i).itemdata_.statCross_.addValue(xPhys * yPhys * 1e6);
	}

	private double[] calcSkrewness(DescriptiveStatistics[] stats,DescriptiveStatistics statCross) {
		double[] skrewness = new double[2];

		double[] stds = new double[2];
		for (int i = 0; i < stds.length; i++) {
			stds[i] = stats[i].getStandardDeviation();
		}

		double n = statCross.getN();
		skrewness[0] = stds[0] / stds[1];
		skrewness[1] = (statCross.getMean() - stats[0].getMean()
				* stats[1].getMean())
				* n / (n - 1) / (stds[0] * stds[1]);
		return skrewness;
	}


	private double[] calcForces(DescriptiveStatistics[] stats) {


		LaguerreSolver solver = new LaguerreSolver();
		double[] forces = new double[2];
		for (int i = 0; i < forces.length; i++) {
			double variance = stats[i].getVariance();

			double a = 4 * preferences_.persistance_ * preferences_.contourLength_/ variance;
			double b = 4 * preferences_.persistance_ *preferences_.beanRadius_ / variance;
			PolynomialFunction func = new PolynomialFunction(new double[] { b,
					a - 2 * b - 6, b - 2 * a + 9, a - 4 });
			try {
				forces[i] = (solver.solve(100, func, 0, 1, 0.8) * a + b) * preferences_.kT_
						/ (4 * preferences_.persistance_);
			} catch (NoBracketingException e) {

			} finally {

			}
		}
		return forces;
	}

	//native method
	/**
	 * @param image
	 * @param roi
	 * @param opt
	 * @return ret[0]: xCenter[i],yCenter[i] as pixel,intensity of roi[i],...;
	 */
	private native double[] gosseCenter(Object image, double[][] roi);
	/**
	 * @param image
	 * @param roi
	 * @param opt
	 * @param zIndex
	 * @return ret[] xCenter[i],yCenter[i] as pixel,...;
	 */
	private native double[] calibration(Object image,  double[][] roi, int zIndex,double zPos);
	/**
	 * @param image
	 * @param roi
	 * @param opt
	 * @return ret[] xCenter[i],yCenter[i],zCenter[i] as pixel,...;
	 */
	private native double[] getZPosition(Object image, double[][] roi);

	public native void releaseBuffer();
	public native void deleteRoi(int index);
	public native void initialize(double[] opt_);

	public  static void main(String[] args) {
		boolean showReturn = true;
		int roiNum =6;
		double radius = 50;
		double rInterStep = 0.5;
		int zRange = 5;
		int zStep = 1;
		int imgBoder = 300;
		int center = imgBoder/2;
		int bitDepth = 64;
		int polarIntegralPartNum = 10;
		int zIndexCorrPartNum = 2;
		double halfQuadWidth =  2;
		double zStart  = 0;
		double imgHeight = imgBoder;
		double imgWidth = imgBoder;
		
		double[][] roilist_ = new double[roiNum][2];
		for (int i = 0; i < roilist_.length; i++) {
			for (int j = 0; j < roilist_[i].length; j++) {
				roilist_[i][j] = center ;
			}

		}

		Kernel kernel_ = new Kernel();
		kernel_.initialize(new double[]{radius,rInterStep,bitDepth,halfQuadWidth ,imgWidth,imgHeight,zStart,zRange,zStep,polarIntegralPartNum,zIndexCorrPartNum});
		long timeStart = 0;
		//		double[] ret = kernel_.gosseCenter(image , roilist_);
		//calibration
		for (int i = 0; i < zRange; i++) {
			Object image = getImg(i+1,bitDepth);
			timeStart = System.nanoTime();
			kernel_.calibration(image, roilist_,i,i);
			System.out.print(String.format("calibration running:\t%d/%d\ttime consume:\t%.2f\tms\r\n",i,zRange,(System.nanoTime()-timeStart)/1e6));

		}

	 	kernel_.deleteRoi(1);
	 	kernel_.deleteRoi(3);
	 	kernel_.deleteRoi(4);
		for (int i = 0; i < zRange; i++) {
			Object image = getImg(i+1.5,bitDepth);
			timeStart = System.nanoTime();
			double[] ret = kernel_.getZPosition(image,roilist_);

			if(showReturn){
				for (int j = 0; j < ret.length/3; j++) {
					if(ret[3*j] <0.001)
						continue;
					System.out.print(String.format("x%d:\t%f\ty:\t%f\tzget:\t%.4f\tzset:\t%f\ttime consume:\t%.2f\r\n",j,ret[3*j],ret[3*j+1],ret[3*j+2],i+0.5,(System.nanoTime()-timeStart)/1e6));
				}

			}
		}
	

	}
	private static Object getImg(Object nameext,int bitDepth)  {
		String[] img_ = getimgString(nameext);
		switch(bitDepth){
		case 8:
			byte[] imgb = new byte[img_.length];
			for (int i = 0; i < img_.length; i++) {
				imgb[i] = (byte) Double.parseDouble(img_[i]);
			}
			return imgb;	
		case 16:
			short[] imgs = new short[img_.length];
			for (int i = 0; i < img_.length; i++) {
				imgs[i] = (short) Double.parseDouble(img_[i]);
			}
			return imgs;	
		case 32:
			float[] imgf = new float[img_.length];
			for (int i = 0; i < img_.length; i++) {
				imgf[i] = (float) Double.parseDouble(img_[i]);
			}
			return imgf;	
		case 64:
			double[] imgd = new double[img_.length];
			for (int i = 0; i < img_.length; i++) {
				imgd[i] = Double.parseDouble(img_[i]);
			}
			return imgd;	
		}
		return null;

	}

	private static String[] getimgString(Object nameext)  {
		File imgFile = new File("G:/CalImages/img"+nameext+".txt");
		if(!imgFile.exists())
			return null;		
		try {
			BufferedReader in;

			in = new BufferedReader(new FileReader(imgFile));

			String line;
			if((line = in.readLine()) == null)
			{
				in.close();
				return null;
			}

			String[] temp = line.split(","); 

			in.close();
			return temp;
		} catch (FileNotFoundException e) {
			return null;
		} catch (IOException e) {
			return null;
		} 

	}

}
