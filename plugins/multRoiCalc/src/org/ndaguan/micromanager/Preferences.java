package org.ndaguan.micromanager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Preferences {

	public final long sleeptime_ = 30;

	private static Preferences instance_;

	double beanRadius_ = 1400;
	double beanRadiusPiexl_ =40;
	double rInterpStep_ = 0.5;
	double bitDepth_ = 16;
	double halfQuadWindow_ = 5;
	double imgwidth_ = 512;
	double imgheight_ = 512;
	double zStart_ = 0;
	double zCalStep_ = 10;
	double zCalRange_ = 2000;
	double xyCalRange_ = 2000;
	double magnetMoveStep_ = 200;
	double persistance_  = 50;
	double contourLength_ = 16700;
	double kT_ = 4.2;
	int frameToAcq_ = 20000;
	int frameToCalcForce_ = 1000;
	double preservation1_ = 4;
	double preservation2_ = 4;
	String userDataDir_ = "";

	public double[] pixelToPhys_;

	public long minAnalyzeWindow_;

	public boolean isCalibrated_ = false;

	public boolean resetData_ = false;

	public int calPosLen_;

	public boolean isInstalCallback_=false;

	public static Preferences getInstance() {
		if(instance_ == null)
			instance_ = new Preferences();		
		return instance_;
	}
	public Preferences() {
		minAnalyzeWindow_ = 1000;
		pixelToPhys_ = new double[]{0,0.075,0,0.075};
		calPosLen_ = (int) (zCalRange_/zCalStep_);
		double[] data = null;
		try {data = getUserData();} catch (IOException e) {}
		if(data != null){
			beanRadiusPiexl_ = data[0];
			beanRadius_ = data[1];
			rInterpStep_ = data[2];
			halfQuadWindow_ = data[3];
			zCalRange_ = data[4];
			zCalStep_ = data[5];
			xyCalRange_  = data[6];
			persistance_ = data[7];
			contourLength_  = data[8];
			frameToAcq_ = (int) data[9];
			frameToCalcForce_ = (int) data[10];
			magnetMoveStep_ = data[11];
			preservation1_ = data[12];
			preservation2_ = data[13];

			calPosLen_ = (int) (zCalRange_/zCalStep_);
		}
	}
	private double[] getUserData() throws IOException {
		File loginDataFile = new File(System.getProperty("user.home")+"/ZIndexMeasure/userData.txt");
		if(!loginDataFile.exists())
			return null;

		BufferedReader in = new BufferedReader(new FileReader(loginDataFile)); 
		String line;
		if((line = in.readLine()) == null)
		{
			in.close();
			return null;
		}

		String[] temp = line.split("\t"); 
		double[] userDataSet = new double[14];
		for (int i = 0; i < userDataSet.length; i++) {
			userDataSet[i] = Double.parseDouble(temp[i]);				
		}
		if((line = in.readLine()) != null)
			userDataDir_ = line;

		in.close();
		return userDataSet;
	}

	public void saveUserData(){
		try {save();} catch (IOException e) {e.printStackTrace();}
	}
	private void save() throws IOException{
		File dir = new File(System.getProperty("user.home"),"ZIndexMeasure");
		if(!dir.isFile())
			dir.mkdirs();

		File loginDataFile = new File(System.getProperty("user.home")+"/ZIndexMeasure/userData.txt");
		FileWriter out = new FileWriter((loginDataFile)); 
		String temp = new String(String.format("%f\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t%d\t%d\t%f\t%f\t%f\t\r\n%s",beanRadiusPiexl_,beanRadius_,rInterpStep_,halfQuadWindow_,zCalRange_,zCalStep_,xyCalRange_ ,persistance_,contourLength_ ,frameToAcq_,frameToCalcForce_,magnetMoveStep_,preservation1_,preservation2_,userDataDir_));
		out.write(temp);
		out.close(); 
	}
	public double[] getPosOption() {
		return new double[]{beanRadiusPiexl_,
				rInterpStep_,
				bitDepth_,
				halfQuadWindow_,
				imgwidth_,
				imgheight_,
				zStart_,
				zCalRange_,
				zCalStep_,
				preservation1_,
				preservation2_};
	}

}
