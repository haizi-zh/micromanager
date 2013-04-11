package org.ndaguan.micromanager.mmtracker;

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
	public static int frameIndex = 0;
	
	public static String[] VARNAME = new String[]{
		"beanRadius",
		"contourLen",
		"calRange",
		"calStepSize",
		"rInterStep",
		"magnetStepSize",
		"frameToCalcForce",
		"beanRadiuPixel",
		"persistance",
		"kT",
		"pixelToPhysX",
		"pixelToPhysY",
		"precision",
		"xFactor",
	"yFactor"};
	public static String [][] VariablesNUPD = new String [][]{
		//name unit precision,default value,important

		{ "beanRadius","/uM" ,"3" ,"1.4" ,"1" },
		{ "contourLen","/uM" ,"3" ,"1" ,"1" },
		{ "calRange","/uM" ,"3" ,"2" ,"1" },
		{ "calStepSize","/uM" ,"3" ,"0.1" ,"1" },
		{ "rInterStep","/pixel" ,"2" ,"0.5" ,"0" },
		{ "magnetStepSize","/uM" ,"0" ,"100" ,"1" },
		{ "frameToCalcForce","/f" ,"0" ,"1000" ,"1" },
		{ "beanRadiuPixel","/pixel" ,"0" ,"40" ,"1" },
		{ "persistance","uM" ,"0" ,"0.05" ,"0" },
		{ "kT","pN/nM" ,"1" ,"4.2" ,"0" },
		{ "pixelToPhysX","(Um/pixel)" ,"3" ,"0.75" ,"0" },
		{ "pixelToPhysY","(Um/pixel)" ,"3" ,"0.75" ,"0" },
		{ "precision","/uM" ,"3" ,"0.001" ,"1" },
		{ "xFactor","" ,"3" ,"1" ,"0" },
		{ "yFactor","" ,"3" ,"1" ,"0" }
	};

	public static String[] UNIT = new String[]{
		"/uM","/uM","/uM","/uM","/pixel","/uM","",
		"/pixel","nM","pN/nM","(Um/pixel)","(Um/pixel)","/uM","",""};
	public static int[] PRECISION = new int[]{
		3,3,3,3,2,0,0,0,0,1,3,3,4,3,3};


	public static String[] CHARTLIST = new String[]{
		"Chart-Z","Chart-X","Chart-Y","Chart-FX","Chart-FY",
		"Chart-STDXDY","Chart-SKREWNESS","Chart-Testing","Chart-Corr","Chart-PosProfile"
	};


	public static void logError(String string) 
	{
		System.out.print(String.format("Err!!!\t%s\r\n",string));
	}
	public static void debugError(String string) 
	{
		System.out.print(String.format("Err!!!\t%s\r\n",string));
	}
	public static void logMessage(String string) 
	{
		System.out.print(String.format("Msg>>\t%s\r\n",string));
	}
	public static void debugMessage(String string) 
	{
		System.out.print(String.format("Msg>>%s\t\r\n",string));
	}
}
