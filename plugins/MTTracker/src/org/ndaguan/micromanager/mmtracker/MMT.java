package org.ndaguan.micromanager.mmtracker;

import java.awt.Color;

public class MMT {
	public static final String menuName = "MultZIndexMeasure";
	public static final String tooltipDescription = "MultZIndexMeasure";
	public static String DEFAULT_TITLE = "Magnetic Tweezers Images Analyzer(SM4.IOP.CAS.CN)";

	public static  String magnetXYstage_ = "MP285 XY Stage";
	public static  String magnetZStage_ = "MP285 Z Stage";
	public static  boolean debug = true;
	public static  String xyStage_ = null; 
	public static  String zStage_ = null;
	
	public static boolean isAnalyzerBusy_ = false;
	public static boolean isCalibrationRunning_ = false;
	public static boolean isTestingRunning_ = false;
	public static boolean isGetXYPositionRunning_ = false;
	public static boolean isGetXYZPositionRunning_ = false;
	
	public static Object Acqlock = 0;
	public static int calibrateIndex_ = 0;
	public static int testingIndex_ = 0;
	public static int currentframeIndex_ = 0;
	public static double currentframeToRefreshImage_ = 0;
	public static String lastError_ = "No Error";
	
	
	public static String[] CHARTLIST = new String[]{
		"Chart-Z","Chart-X","Chart-Y","Chart-FX","Chart-FY",
		"Chart-STDXDY","Chart-SKREWNESS","Chart-Testing","Chart-Corr","Chart-PosProfile"
	};

	public static void logError(String string) 
	{
		MMTFrame.getInstance().infomation_.setForeground(new Color(255,0,0));
		MMTFrame.getInstance().infomation_.setText("Error!\t"+string);
		System.out.print(String.format("Err!!!\t%s\r\n",string));
	}
	public static void debugError(String string) 
	{
		System.out.print(String.format("Err!!!\t%s\r\n",string));
	}
	
	public static void logMessage(String string) 
	{
		MMTFrame.getInstance().infomation_.setForeground(new Color(0,0,0));
		MMTFrame.getInstance().infomation_.setText("Msg:\t"+string);
		System.out.print(String.format("Msg>>\t%s\r\n",string));
	}
	public static void debugMessage(String string) 
	{
		System.out.print(String.format("Msg>>%s\t\r\n",string));
	}
	
	public static int[] unEditAfterCalbration = new int[]{3,5,6,17};
	public static enum VariablesNUPD {
		//general
		beanRadiuPixel("/pixel ",55,0,1),
		frameToCalcForce("/f ",1000,0,1),
		magnetStepSize("/uM ",100,0,1),
		beanRadius("/uM ",1.4,0.001,1),
		contourLen("/uM ",1,0.001,1),
		calRange("/uM ",3,0.01,1),
		calStepSize("/uM ",0.1,0.01,1),
		chartWindowSize("",1000,0,1),
		//advance
		persistance("/uM ",0.05,0.001,0),
		kT("pN/nM ",4.2,0.001,0),
		pixelToPhysX("(Um/pixel) ",0.075,0.0001,0),
		pixelToPhysY("(Um/pixel) ",0.075,0.0001,0),
		precision("/uM",0.0001,0.0001,0),
		showDebugTime("",10000,0,0),
		testingPrecision("",0.05,0.01,0),
		xFactor(" ",1,0.0001,0),
		yFactor(" ",1,0.0001,0),
		rInterStep("/pixel ",0.2,0.1,0),
		saveFile(" ",1,0,0),
		frameToRefreshChart("",50,0,0),
		frameToRefreshImage("",50,0,0),
		responceXY("",0,0,0),
		chartStatisWindow("",200,0,0);
	 
		private String unit;
		private double value;
		private double presicion;
		private  int important;
		VariablesNUPD(String u,double v,double p,int i) {
			unit = u;
			value = v;
			presicion = p;
			important = i;
			
		}
	 
		public double value() {
			return value;
		}
		public void value(double v) {
			value = v;
		}
		public String getUnit() {
			return unit;
		}
		public double getPresicion() {
			return presicion;
		}
		public int getImp() {
			return important;
		}
		
	};
}
