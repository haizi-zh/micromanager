package org.ndaguan.micromanager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Preferences {

	public final long sleeptime_ = 30;

	private static Preferences instance_;

	public  String magnetXYstage_ = "MP285 XY Stage";
	public  String magnetFocusStage_ = "MP285 Z Stage";
	
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
	double xyCalRange_ = 5;//piexl
	double magnetMoveStep_ = 200;
	double persistance_  = 50;
	double contourLength_ = 16700;
	double kT_ = 4.2;
	double frameToAcq_ = 20000;
	double frameToCalcForce_ = 1000;
	double zIndexCorrPartNum = 4;
	double polarIntegralPartNum = 4;
	String userDataDir_ = "";

	public String[] PREFERENCENAME= new String[]{
			"BeanRadius",
			"DNALength",
			"ZCalScale",
			"ZCalStep",
			"RinterStep",
			"HalfCorrWin",
			"MagnetStep",
			"FrameToAcq",
			"FrameToCalcForce",
			"BeanRadiuPixel",
			"polarIntegralPartNum",
			"zIndexCorrPartNum"
			};
	public String[] PREFERENCEUNIT= new String[]{
			"/nM",
			"/nM",
			"/nM",
			"/nM",
			"/pixel",
			"",
			"/uM",
			"",
			"",
			"/pixel",
			"",
			""
			};
	
	public int[] PREFERENCEPRECISE= new int[]{
			0,
			0,
			0,
			0,
			2,
			0,
			1,
			0,
			0,
			0,
			0,
			0
			};

	public double[] pixelToPhys_;

	public long minAnalyzeWindow_;

	public boolean isCalibrated_ = false;

	public boolean resetData_ = false;

	public int calPosLen_;

	public boolean isInstalCallback_=false;

	public String acqName_ = "";



	public static Preferences getInstance() {
		if(instance_ == null)
			instance_ = new Preferences();		
		return instance_;
	}
	public Preferences() {
		minAnalyzeWindow_ = 500;
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
			polarIntegralPartNum = data[12];
			zIndexCorrPartNum = data[13];

			calPosLen_ = (int) (zCalRange_/zCalStep_);
		}
	}
	private double[] getUserData() throws IOException {
		File loginDataFile = new File(System.getProperty("user.home")+"/multZIndexMeasure/userData.txt");
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

 
	public void save() throws IOException{
		File dir = new File(System.getProperty("user.home"),"multZIndexMeasure");
		if(!dir.isFile())
			dir.mkdirs();

		File loginDataFile = new File(System.getProperty("user.home")+"/multZIndexMeasure/userData.txt");
		FileWriter out = new FileWriter((loginDataFile)); 
		String temp = new String(String.format("%f\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t\r\n%s",beanRadiusPiexl_,beanRadius_,rInterpStep_,halfQuadWindow_,zCalRange_,zCalStep_,xyCalRange_ ,persistance_,contourLength_ ,frameToAcq_,frameToCalcForce_,magnetMoveStep_,polarIntegralPartNum,zIndexCorrPartNum,userDataDir_));
		out.write(temp);
		out.close(); 
	}
	public void onDataChange(double[] data) {
		beanRadius_ = data[0];
		contourLength_ = data[1];
		zCalRange_ = data[2];
		zCalStep_ = data[3];
		rInterpStep_ = data[4];
		halfQuadWindow_ = data[5];
		magnetMoveStep_ = data[6];
		frameToAcq_ = data[7];
		frameToCalcForce_ = data[8];
		beanRadiusPiexl_ = data[9];
		polarIntegralPartNum = data[10];
		zIndexCorrPartNum = data[11];
		try {save();} catch (IOException e) {e.printStackTrace();}
	}
	public double[] getData() {
		// TODO Auto-generated method stub
		double[] data = new double[]{
				beanRadius_,
				contourLength_,
				zCalRange_,
				zCalStep_,
				rInterpStep_,
				halfQuadWindow_,
				magnetMoveStep_,
				frameToAcq_,
				frameToCalcForce_,
				beanRadiusPiexl_,
				polarIntegralPartNum,
				zIndexCorrPartNum
		};
		return data;
	}
	public int[] getDataPrecise() {
		// TODO Auto-generated method stub
		return this.PREFERENCEPRECISE;
	}

}
