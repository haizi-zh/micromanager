package org.ndaguan.micromanager.mmtracker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author Administrator
 *
 */
public class Preferences {

	public double beanRadius_;
	public double contourLen_;
	public double calRange_;
	public double calStepSize_;
	public double rInterStep_;
	public int magnetStepSize_;
	public int frameToCalcForce_;
	public int beanRadiuPixel_;
	public double persistance_;
	public double kT_;
	public double pixelToPhysX_;
	public double pixelToPhysY_;
	public double precision_;
	public double xFactor_;
	public double yFactor_;
	
	private static Preferences instance_;

	String userDataDir_ = "";


	public static Preferences getInstance() {
		if(instance_ == null)
			instance_ = new Preferences();		
		return instance_;
	}

	public Preferences() {
		beanRadius_ = 1.4;
		contourLen_ = 1;
		calRange_ = 2;
		calStepSize_ = 0.1;
		rInterStep_ = 0.5;
		magnetStepSize_ = 100;
		frameToCalcForce_ = 1000;
		beanRadiuPixel_ = 40;
		persistance_ = 50;
		kT_ = 4.2;
		pixelToPhysX_ = 0.075;
		pixelToPhysY_ = 0.075;
		precision_ = 0.01;
		xFactor_ = 1;
		yFactor_ = 1;

		double[] data = getUserData();
		if(data != null){
			int i = 0;
			beanRadius_ = data[i++];
			contourLen_ = data[i++];
			calRange_ = data[i++];
			calStepSize_ = data[i++];
			rInterStep_ = data[i++];
			magnetStepSize_ = (int) data[i++];
			frameToCalcForce_ = (int) data[i++];
			beanRadiuPixel_ = (int) data[i++];
			persistance_ = data[i++];
			kT_ = data[i++];
			pixelToPhysX_ = data[i++];
			pixelToPhysY_ = data[i++];
			precision_ = data[i++];
			xFactor_ = data[i++];
			yFactor_ = data[i];
		}
		saveUserData();

	}
	public void onDataChange(double[] data) {
		if(data != null){
			int i = 0;
			beanRadius_ = data[i++];
			contourLen_ = data[i++];
			calRange_ = data[i++];
			calStepSize_ = data[i++];
			rInterStep_ = data[i++];
			magnetStepSize_ = (int) data[i++];
			frameToCalcForce_ = (int) data[i++];
			beanRadiuPixel_ = (int) data[i++];
			persistance_ = data[i++];
			kT_ = data[i++];
			pixelToPhysX_ = data[i++];
			pixelToPhysY_ = data[i++];
			precision_ = data[i++];
			xFactor_ = data[i++];
			yFactor_ = data[i];
		}
		saveUserData();
	}

	public double[] getUserData() {
		try {
			File loginDataFile = new File(System.getProperty("user.home")+"/MMTracker/userData.txt");
			if(!loginDataFile.exists())
				return null;
			BufferedReader in;
			in = new BufferedReader(new FileReader(loginDataFile));
			String line;
			if((line = in.readLine()) == null)
			{
				in.close();
				return null;
			}
			String[] temp = line.split(","); 
			double[] userDataSet = new double[MMT.VARNAME.length];
			for (int i = 0; i < userDataSet.length; i++) {
				userDataSet[i] = Double.parseDouble(temp[i]);				
			}
			if((line = in.readLine()) != null)
				userDataDir_ = line;
			in.close();
			return userDataSet;
		} catch (IOException e) {
			MMT.logError("read user data false");
			return null;
		} 
	}


	void saveUserData(){
		try {
			File dir = new File(System.getProperty("user.home"),"MMTracker");
			if(!dir.isFile())
				dir.mkdirs();

			File loginDataFile = new File(System.getProperty("user.home")+"/MMTracker/userData.txt");
			FileWriter out = new FileWriter((loginDataFile)); 
			String sData = "";

			sData += Double.toString( (double) beanRadius_) + " , ";
			sData += Double.toString( (double) contourLen_) + " , ";
			sData += Double.toString( (double) calRange_) + " , ";
			sData += Double.toString( (double) calStepSize_) + " , ";
			sData += Double.toString( (double) rInterStep_) + " , ";
			sData += Double.toString( (double) magnetStepSize_) + " , ";
			sData += Double.toString( (double) frameToCalcForce_) + " , ";
			sData += Double.toString( (double) beanRadiuPixel_) + " , ";
			sData += Double.toString( (double) persistance_) + " , ";
			sData += Double.toString( (double) kT_) + " , ";
			sData += Double.toString( (double) pixelToPhysX_) + " , ";
			sData += Double.toString( (double) pixelToPhysY_) + " , ";
			sData += Double.toString( (double) precision_) + " , ";
			sData += Double.toString( (double) xFactor_) + " , ";
			sData += Double.toString( (double) yFactor_) + " , ";
			sData += "\r\n"+ userDataDir_;
			out.write(sData);
			out.close(); 
		} catch (IOException e) {
			MMT.logError("save user data err");
		}
	}

}
