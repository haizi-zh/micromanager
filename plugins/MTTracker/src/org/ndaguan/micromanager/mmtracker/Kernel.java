package org.ndaguan.micromanager.mmtracker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.SwingUtilities;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.solvers.LaguerreSolver;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.exception.NoBracketingException;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

public class Kernel {
	private long timeStart;

	private static Kernel kernel_;
	private DescriptiveStatistics statis_;
	private DescriptiveStatistics sumX_;
	private DescriptiveStatistics sumY_;
	private FastFourierTransformer FFT_;
	private SplineInterpolator interpolator;
	private PearsonsCorrelation pearCorrelation_;

	public  List< double[][]> calProfiles;
	private SimpleRegression regrX;
	private SimpleRegression regrY;
	public List<RoiItem> roiList_;

	public  double[] zPosProfiles;
	public  double[] xPosProfiles;
	public  double[] yPosProfiles;
	public int imageWidth = 512;
	public int imageHeight = 512;
	private Preferences preferences_;
	public boolean isCalibrated_ = false;
	private double[] pixelToPhys;

	public double[] zTestingPosProfiles;
	public Kernel(Preferences preferences, List<RoiItem> roiList){
		preferences_ = preferences;
		roiList_ = roiList;
		FFT_ = new FastFourierTransformer(DftNormalization.STANDARD);//gosseCenter
		interpolator = new SplineInterpolator(); // gosseCenter and getZlocation
		statis_ = new DescriptiveStatistics();	//get zScore of posProfile
		sumX_ = new DescriptiveStatistics();	//gosseCenter
		sumY_ = new DescriptiveStatistics();	//gosseCenter
		pearCorrelation_ = new PearsonsCorrelation();// getZlocation
		calProfiles = Collections.synchronizedList(new ArrayList< double[][]>());
		regrX = new SimpleRegression(); //XY calibration
		regrY = new SimpleRegression(); //XY calibration

	}
	public static  Kernel getInstance(Preferences preferences, List<RoiItem> roiList) 
	{
		if(kernel_ == null)
			kernel_ = new Kernel(preferences,roiList);
		return kernel_;
	}
	public static Kernel getInstance() {
		return kernel_;
	}

	public boolean getPosition(Object image){	

		double[] force = new double[]{0,0};
		double[] skrewneww = new double[]{0,0};
		boolean ret;
		if(roiList_.size() <= 0)return false;

		ret = gosseCenter(image);

		if (isCalibrated_ ) {
			MMT.frameIndex++;
			double[] currProfiles = new double[(int) (preferences_.beanRadiuPixel_/preferences_.rInterStep_)];
			for (int k = 0; k < roiList_.size(); k++) {
				int roiX = (int) (roiList_.get(k).x_ - preferences_.beanRadiuPixel_);
				int roiY = (int) (roiList_.get(k).y_ - preferences_.beanRadiuPixel_);
				if(RoiOutOfImage(roiX,roiY)){
					roiList_.remove(k);
					if(isCalibrated_){
						calProfiles.remove(k);
						if(roiList_.size() == 0){
							isCalibrated_ = false;
						}
					}
					return false;
				}
				currProfiles = polarIntegral(image,roiList_.get(k).x_,roiList_.get(k).y_);
				final  double[] posProfile = currProfiles;
				if(MMT.debug && MMT.frameIndex%preferences_.showDebugTime == 0){
					final int roiIndex = k;
					roiList_.get(roiIndex).chart_.getDataSeries().get("Chart-PosProfile").clear();
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							for (int j = 0; j < posProfile.length; j++) {
								roiList_.get(roiIndex).chart_.getDataSeries().get("Chart-PosProfile").add(j,posProfile[j]);
							}
						}
					});
				}
				ret = getZLocation(k,currProfiles);
			}	
		}

		for (int i = 0; i < roiList_.size(); i++) {//force 
			if (roiList_.get(0).stats_[0].getN() >= preferences_.frameToCalcForce_)
			{
				force = calcForces(roiList_.get(i).stats_);
				skrewneww = calcSkrewness(roiList_.get(i).stats_,roiList_.get(i).statCross_);
				roiList_.get(i).fx_ = force[0];
				roiList_.get(i).fy_ = force[1];
				roiList_.get(i).stdXdY_ =  skrewneww[0];
				roiList_.get(i).skrewness_ =  skrewneww[1];
			}

		}
		return ret;
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
		double persistance = preferences_.persistance_;
		double contourLength = preferences_.contourLen_*1000;
		double beanRadius = preferences_.beanRadius_*1000;
		double kT = preferences_.kT_;

		for (int i = 0; i < forces.length; i++) {
			double variance = stats[i].getVariance();
			double a = 4 * persistance * contourLength/ variance;
			double b = 4 * persistance * beanRadius / variance;
			PolynomialFunction func = new PolynomialFunction(new double[] { b,
					a - 2 * b - 6, b - 2 * a + 9, a - 4 });
			try {
				forces[i] = (solver.solve(100, func, 0, 1, 0.8) * a + b) * kT
						/ (4 * persistance);
			} catch (NoBracketingException e) {

			} finally {

			}
		}
		return forces;
	}


	public  static void main(String[] args) {
		List<RoiItem> rt = Collections.synchronizedList(new ArrayList<RoiItem>());
		Preferences pr = new Preferences();
		Kernel kl = new Kernel(pr,rt);

		kl.imageWidth = 300;
		kl.imageHeight = 300;

		pr.beanRadiuPixel_ = 100;
		pr.calRange_ = 9;
		pr.calStepSize_ = 1;
		pr.pixelToPhysX_ = 1;
		pr.pixelToPhysY_ = 1;
		pr.userDataDir_ = "Z:\\";
		pr.frameToCalcForce_ = 50;
		pr.xFactor_ = 1;
		Function fc = new Function( rt);
		//		rt.add(RoiItem.createInstance(pr,new double[]{160,160},"bean1"));
		rt.add(RoiItem.createInstance(pr,new double[]{130,130},"bean2"));

		int bitDepth = 32;
		boolean flag = false;
		if(flag)
			for (int i = 0; i < pr.calRange_; i++) {
				Object image = getImg(i+1,bitDepth);
				long timeStart = System.nanoTime();
				boolean ret = kl.getPosition(image);
				if(!ret)continue;
				System.out.print(String.format("\r\ntotal:%f",(System.nanoTime() - timeStart)/10e6));
				for (int k = 0; k < rt.size(); k++) {
					double xpos = rt.get(k).x_;
					double ypos = rt.get(k).y_;
					double xphy = rt.get(k).xPhy_;
					double yphy = rt.get(k).yPhy_;
					System.out.print(String.format("\r\nx:\t%f\ty:%f\r\nxphy:\t%f\typhy:\t%f", xpos,ypos,xphy,yphy));

				}
			}

		if(!flag){
			for (int i = 0; i < rt.size(); i++) {
				rt.get(i).chart_.setVisible(true);
			}
			kl.updateCalibrationProfile();
			for (int i = 0; i < pr.calRange_; i++) {
				Object image = getImg(i+1,bitDepth);
				kl.calibration(image,i,i+1,150,150);
			}
			kl.isCalibrated_ = true;
			for (int jj = 0; jj < 1000; jj++) {

				for (int i = 0; i < pr.calRange_; i++) {
					double img = 2 + 1.2;				
					Object image = getImg(img,bitDepth);
					double timeConsume = System.nanoTime();
					kl.getPosition(image);
					fc.updateChart(jj);
					try {
						kl.saveRoiData("Acq",i,(System.nanoTime() -timeConsume)/10e6 );
					} catch (IOException e) {
						MMT.logError("Save data err");
					}
					timeConsume = (System.nanoTime() -timeConsume)/10e6 ;

					for (int k = 0; k < rt.size(); k++) {
						double xphy = rt.get(k).xPhy_;
						double yphy = rt.get(k).yPhy_;
						double zphy = rt.get(k).z_;
						System.out.print(String.format("\r\n\r\nxphy:\t%.3f\typhy:\t%.3f\tzphy:\t%.3f\tzset:\t%.3f\tdelta:\t%f\ttime consume:\t%f",xphy,yphy,zphy,img,zphy-img,timeConsume));
					}

				}

			}
			try {
				kl.dataCleanUp();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}


	}


	private boolean dataCleanUp() throws IOException {
		for (int i = 0; i < roiList_.size(); i++) {
			roiList_.get(i).dataClean();
		}
		return true;
	}
	public boolean saveRoiData(String acqName,long frameNum, double elapsed) throws IOException {
		for (int i = 0; i < roiList_.size(); i++) {
			roiList_.get(i).writeData(acqName, frameNum, elapsed);
		}
		return true;
	}
	private static double[] zeroPadding(double[] signal) {
		double log2 = Math.log10(signal.length*2 - 1)/Math.log10(2);
		double[] ret = new double[(int) Math.pow(2, Math.floor(log2)+1)];
		for (int i = 0; i < signal.length; i++) {
			ret[i] = signal[i];
		}
		for (int i = signal.length; i < ret.length; i++) {
			ret[i] = 0.0;
		}
		return ret;
	} 
	private double[] polarIntegral(Object image,double xpos,double ypos){
		double S00 = 0, S01 =0, S10 = 0, S11 =0;				 
		double[] profile = new double[(int) (preferences_.beanRadiuPixel_/preferences_.rInterStep_)];
		statis_.clear();
		switch(image.getClass().getName()){
		case "[D":
			profile[0] = ((double[]) image)[(int)xpos + ((int)ypos)* imageWidth];
			for(int i = 1;i< preferences_.beanRadiuPixel_/preferences_.rInterStep_ ;i++)
			{
				double sumr = 0;
				double r =i*preferences_.rInterStep_;
				double dTheta = 1/r;
				int nTheta =(int) (2*3.141592653579/dTheta);
				for(int j = 0;j<nTheta;j++)
				{

					double x = (xpos+preferences_.xFactor_*r*Math.cos(dTheta*j));
					double y = (ypos+preferences_.yFactor_*r*Math.sin(dTheta*j));
					int x0 = (int)x;
					int y0 = (int)y;
					int x1 = x0 +1;
					int y1 = y0 +1;
					double dx = x - x0;
					double dy = y - y0;

					S00 = ((double[]) image)[x0 + y0* imageWidth];
					S01 = ((double[]) image)[x1 + y0* imageWidth];
					S10 = ((double[]) image)[x0 + y1* imageWidth];
					S11 = ((double[]) image)[x1 + y1* imageWidth];
					double Sxy = S00*(1-dx)*(1-dy)+S01*dy*(1-dx)+S10*dx*(1-dy) +S11*dx*dy;
					sumr += Sxy;
				}
				profile[i] =sumr/nTheta;
				statis_.addValue(profile[i]);
			}
			break;
		case "[F":
			profile[0] = ((float[]) image)[(int)xpos + ((int)ypos)* imageWidth];
			for(int i = 1;i< preferences_.beanRadiuPixel_/preferences_.rInterStep_ ;i++)
			{
				double sumr = 0;
				double r =i*preferences_.rInterStep_;
				double dTheta = 1/r;
				int nTheta =(int) (2*3.141592653579/dTheta);
				for(int j = 0;j<nTheta;j++)
				{

					double x = (xpos+preferences_.xFactor_*r*Math.cos(dTheta*j));
					double y = (ypos+preferences_.yFactor_*r*Math.sin(dTheta*j));
					int x0 = (int)x;
					int y0 = (int)y;
					int x1 = x0 +1;
					int y1 = y0 +1;
					double dx = x - x0;
					double dy = y - y0;

					S00 = ((float[]) image)[x0 + y0* imageWidth];
					S01 = ((float[]) image)[x1 + y0* imageWidth];
					S10 = ((float[]) image)[x0 + y1* imageWidth];
					S11 = ((float[]) image)[x1 + y1* imageWidth];
					double Sxy = S00*(1-dx)*(1-dy)+S01*dy*(1-dx)+S10*dx*(1-dy) +S11*dx*dy;
					sumr += Sxy;
				}
				profile[i] =sumr/nTheta;
				statis_.addValue(profile[i]);
			}
			break;
		case "[S":
			profile[0] = ((short[]) image)[(int)xpos + ((int)ypos)* imageWidth];
			for(int i = 1;i< preferences_.beanRadiuPixel_/preferences_.rInterStep_ ;i++)
			{
				double sumr = 0;
				double r =i*preferences_.rInterStep_;
				double dTheta = 1/r;
				int nTheta =(int) (2*3.141592653579/dTheta);
				for(int j = 0;j<nTheta;j++)
				{

					double x = (xpos+preferences_.xFactor_*r*Math.cos(dTheta*j));
					double y = (ypos+preferences_.yFactor_*r*Math.sin(dTheta*j));
					int x0 = (int)x;
					int y0 = (int)y;
					int x1 = x0 +1;
					int y1 = y0 +1;
					double dx = x - x0;
					double dy = y - y0;

					S00 = ((short[]) image)[x0 + y0* imageWidth];
					S01 = ((short[]) image)[x1 + y0* imageWidth];
					S10 = ((short[]) image)[x0 + y1* imageWidth];
					S11 = ((short[]) image)[x1 + y1* imageWidth];
					double Sxy = S00*(1-dx)*(1-dy)+S01*dy*(1-dx)+S10*dx*(1-dy) +S11*dx*dy;
					sumr += Sxy;
				}
				profile[i] =sumr/nTheta;
				statis_.addValue(profile[i]);
			}
			break;
		case "[B":
			profile[0] = ((byte[]) image)[(int)xpos + ((int)ypos)* imageWidth];
			for(int i = 1;i< preferences_.beanRadiuPixel_/preferences_.rInterStep_ ;i++)
			{
				double sumr = 0;
				double r =i*preferences_.rInterStep_;
				double dTheta = 1/r;
				int nTheta =(int) (2*3.141592653579/dTheta);
				for(int j = 0;j<nTheta;j++)
				{

					double x = (xpos+preferences_.xFactor_*r*Math.cos(dTheta*j));
					double y = (ypos+preferences_.yFactor_*r*Math.sin(dTheta*j));
					int x0 = (int)x;
					int y0 = (int)y;
					int x1 = x0 +1;
					int y1 = y0 +1;
					double dx = x - x0;
					double dy = y - y0;

					S00 = ((byte[]) image)[x0 + y0* imageWidth];
					S01 = ((byte[]) image)[x1 + y0* imageWidth];
					S10 = ((byte[]) image)[x0 + y1* imageWidth];
					S11 = ((byte[]) image)[x1 + y1* imageWidth];
					double Sxy = S00*(1-dx)*(1-dy)+S01*dy*(1-dx)+S10*dx*(1-dy) +S11*dx*dy;
					sumr += Sxy;
				}
				profile[i] =sumr/nTheta;
				statis_.addValue(profile[i]);
			}
			break;
		default:
			return null;
		}



		double mean = statis_.getMean();
		double std = statis_.getStandardDeviation();
		double std2 = std*std;
		for (int i = 0; i < profile.length; i++) {
			profile[i] = (profile[i] - mean)/std2;
		}
		return profile;
	}
	public void updateCalibrationProfile(){
		double[][] cal = new double[ (int) (preferences_.calRange_/preferences_.calStepSize_)][(int) (preferences_.beanRadiuPixel_/preferences_.rInterStep_)];

		zPosProfiles = new double[ (int) (preferences_.calRange_/preferences_.calStepSize_)];
		xPosProfiles = new double[ (int) (preferences_.calRange_/preferences_.calStepSize_)];
		yPosProfiles = new double[ (int) (preferences_.calRange_/preferences_.calStepSize_)];
		calProfiles.clear();
		for (int i = 0; i < roiList_.size(); i++) {
			calProfiles.add(cal);
		}
	}

	public  boolean calibration(Object image,int index,double currXPos,double currYPos,double currZPos) {
		boolean ret = gosseCenter(image);
		if(index == 0){
			regrX.clear();
			regrX.clear();
		}
		regrX.addData(roiList_.get(0).x_,currXPos);
		regrY.addData(roiList_.get(0).y_,currYPos);
		zPosProfiles [index] = currZPos;
		for (int k = 0; k < roiList_.size(); k++) {
			calProfiles.get(k)[index] = polarIntegral(image,roiList_.get(k).x_,roiList_.get(k).y_);
		}
		return ret;
	}

	private boolean getZLocation(int roiIndex, double[] currrProfiles) {
		double max = 0;
		int index = 0;
		double pos = 0;
		double[] yArray = new double[calProfiles.get(0).length];
		for (int j = 0; j < yArray.length; j++) {//range
			double value = pearCorrelation_.correlation(currrProfiles, calProfiles.get(roiIndex)[j]);
			yArray[j] = value;
			if(value>max){
				max = value;
				index = j;
			}
		}

		if(MMT.debug && MMT.frameIndex%preferences_.showDebugTime == 0){
			final double[] y = yArray;
			final int roi = roiIndex;
			roiList_.get(roiIndex).chart_.getDataSeries().get("Chart-Corr").clear();
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					for (int j = 0; j < y.length; j++) {
						roiList_.get(roi).chart_.getDataSeries().get("Chart-Corr").add(j,y[j]);
					}
				}
			});
		}
		UnivariateFunction function = interpolator.interpolate(zPosProfiles, yArray);

		max = 0;
		int start = index-1;
		if(start<0)
			start = 0;
		int end = start+2;

		if(end>=zPosProfiles.length)
			end = zPosProfiles.length-1;
		for (double j = zPosProfiles[start]; j <  zPosProfiles[end]; j+= preferences_.precision_) {
			double value  = function.value(j);
			if(value >max){
				max = value;
				pos = j;
			}
		}
		roiList_.get(roiIndex).stats_[2].addValue(pos);//zPos
		roiList_.get(roiIndex).z_ = pos;
		return true;
	}
	public  void setPixelToPhys() {
		preferences_.pixelToPhysX_ = regrX.getSlope();
		preferences_.pixelToPhysY_ = regrY.getSlope();
		preferences_.saveUserData();
	}
	public boolean gosseCenter(Object image){
		int roiNum = roiList_.size();
		double xPhys = 0;
		double yPhys = 0;
		for (int i = 0; i < roiNum; i++) {

			int roiX = (int) (roiList_.get(i).x_ - preferences_.beanRadiuPixel_);
			int roiY = (int) (roiList_.get(i).y_ - preferences_.beanRadiuPixel_);
			if(RoiOutOfImage(roiX,roiY)){
				roiList_.remove(i);
				if(isCalibrated_){
					calProfiles.remove(i);
					if(roiList_.size() == 0){
						isCalibrated_ = false;
					}
				}
				return false;
			}
			double[][] sumXY = getXYSum(image, roiX,roiY);
			if(sumXY == null)
				return false;

			double xPos = getCurveCenter(sumXY[0])+ roiX;
			double yPos= getCurveCenter(sumXY[1])+ roiY;

			roiList_.get(i).x_ = xPos;
			roiList_.get(i).y_ = yPos;

			xPhys = preferences_.pixelToPhysX_ * xPos;
			yPhys = preferences_.pixelToPhysY_ * yPos;

			roiList_.get(i).xPhy_ = xPhys;
			roiList_.get(i).yPhy_ = yPhys;

			roiList_.get(i).stats_[0].addValue(xPhys*1000);//xPos nM
			roiList_.get(i).stats_[1].addValue(yPhys*1000);//yPos uM
			if(!isCalibrated_)
			{
				roiList_.get(i).stats_[2].addValue(sumXY[2][0]);//intense
				roiList_.get(i).z_ = sumXY[2][0];
			}
			roiList_.get(i).statCross_.addValue(xPhys * yPhys * 10e6);//crossStd




		}
		return true;
	}

	private boolean RoiOutOfImage(int roiX, int roiY) {
		if(roiX<0)
			return true;
		if(roiY<0)
			return true;
		if(roiX+2*preferences_.beanRadiuPixel_ >imageWidth)
			return true;
		if(roiY+2*preferences_.beanRadiuPixel_ >imageHeight)
			return true;
		return false;
	}
	void start(){
		timeStart = System.nanoTime();
	}
	void end(){
		System.out.print(String.format("\r\ntimeConsume:%f",(System.nanoTime() - timeStart)/10e6));
	}
	private double getCurveCenter(double[] curve) {
		double[] signal = zeroPadding(curve);
		Complex[] fRespns = FFT_.transform(signal, TransformType.FORWARD);
		for (int i = 0; i < fRespns.length; i++) {
			fRespns[i] = fRespns[i].multiply(fRespns[i]);
		}
		Complex[] convResult = FFT_.transform(fRespns, TransformType.INVERSE);
		int convLen = curve.length * 2 -1;
		double max = 0;
		int index = 0;

		double yArray[] = new double[convLen];
		for (int i = 0; i < convLen; i++) {
			double value = convResult[i].getReal();
			yArray[i] = value;
			if(value > max){
				max = value;
				index = i;
			}
		}

		double[] xArray = new double[convLen];
		for (int i=0; i<convLen; i++)
			xArray[i] = i;
		UnivariateFunction function = interpolator.interpolate(xArray, yArray);
		double pos = 0;
		max = 0;
		int start = index-1;
		if(start<0)
			start = 0;
		int end = start+2;

		if(end>=xArray.length)
			end = xArray.length-1;

		for (double i = xArray[start]; i < xArray[end]; i+= preferences_.precision_) {
			double value  = function.value(i);
			if(value >max){
				max = value;
				pos = i;
			}
		}

		return (pos+1)/2;
	}
	private double[][] getXYSum(Object image,int roiX,int roiY){

		int roiBorder = 2*preferences_.beanRadiuPixel_;
		int sRoi = roiBorder*roiBorder;
		double[][] sumXY = new double[3][roiBorder];
		sumX_.clear();
		sumY_.clear();
		switch(image.getClass().getName()){
		case "[D":
			for (int x = 0; x < roiBorder; x++) {
				for (int y = 0; y < roiBorder; y++) {
					double gray = (double) ((double[])image)[(y+roiY)*imageWidth+(x+roiX)];
					sumXY[0][x] += gray;
					sumXY[1][y] += gray;
					sumXY[2][0] += gray/sRoi;
				}
				sumX_.addValue(sumXY[0][x]);
			}
			break;
		case "[F":
			for (int x = 0; x < roiBorder; x++) {
				for (int y = 0; y < roiBorder; y++) {
					double gray = (double) ((float[])image)[(y+roiY)*imageWidth+(x+roiX)];
					sumXY[0][x] += gray;
					sumXY[1][y] += gray;
					sumXY[2][0] += gray/sRoi;
				}
				sumX_.addValue(sumXY[0][x]);
			}
			break;
		case "[S":
			for (int x = 0; x < roiBorder; x++) {
				for (int y = 0; y < roiBorder; y++) {
					double gray = (double) ((short[])image)[(y+roiY)*imageWidth+(x+roiX)];
					sumXY[0][x] += gray;
					sumXY[1][y] += gray;
					sumXY[2][0] += gray/sRoi;
				}
				sumX_.addValue(sumXY[0][x]);
			}
			break;
		case "[B":
			for (int x = 0; x < roiBorder; x++) {
				for (int y = 0; y < roiBorder; y++) {
					double gray = (double) ((byte[])image)[(y+roiY)*imageWidth+(x+roiX)];
					sumXY[0][x] += gray;
					sumXY[1][y] += gray;
					sumXY[2][0] += gray/sRoi;
				}
				sumX_.addValue(sumXY[0][x]);
			}
			break;
		default:
			return null;
		}
		for (int i = 0; i < sumXY[0].length; i++) {
			sumY_.addValue(sumXY[1][i]);
		}
		double meanx = sumX_.getMean();
		double meany = sumY_.getMean();
		double stdx = sumX_.getStandardDeviation();
		double stdx2 = stdx*stdx;
		double stdy = sumY_.getStandardDeviation();
		double stdy2 = stdy*stdy;
		for (int i = 0; i < sumXY[0].length; i++) {
			sumXY[0][i] = (sumXY[0][i] - meanx)/stdx2;
			sumXY[1][i] = (sumXY[1][i] - meany)/stdy2;
		}
		return sumXY;
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
		File imgFile = new File("F:/Development/CalImages/img"+nameext+".txt");
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
