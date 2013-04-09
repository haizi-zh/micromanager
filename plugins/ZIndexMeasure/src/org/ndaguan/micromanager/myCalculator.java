package org.ndaguan.micromanager;
public class myCalculator {
	private static myCalculator instance_;
 
	public static myCalculator getInstance() {
		if(instance_ == null)
			instance_ = new myCalculator();
		return instance_;

	}
	static {
		System.loadLibrary("myCalculator");
	}

	public String getErrCode(int errCode) {
		String[] err_string = new String[] { "ERR_OK", "ERR_BALL_OUT_OF_IMAGE",
				"ERR_GET_CENTER_FALSE", "ERR_UNSUPORT_IMAGE_FORMAT" };
		return err_string[errCode];
	}

	/*
	 * Function Calibration:Get a Calibration curve with the input image and
	 * ROI,this curve,i.e.curve[z] is storage in the DLL stack and expecting
	 * GetZPosition function. Inputs: image:full image(float[][])
	 * roi:x,y,width,height
	 * opt:Radius,RInterstep,bitDepth,halfQuadWidth,imgWidth,imgHeight outputs:
	 * Object[0]:xPos,yPos Object[1]:Time Cost in this function,err_code
	 */
	public native Object[] Calibration(Object image_, int[] roi_, int zX_);

	/*
	 * Function GetForce:Get Force with the x (or y) position ,x (or y)Position
	 * data is storage in the DLL stack Inputs: xdata:x opt:DNA
	 * length/uM,temperature/K,P/50 manometer default. Outputs:
	 * Object[0]:Force/pN Object[1]:Time Cost in this function,err_code
	 */
	public native double[] GetForce(double[] data_, double[] opt_);

	/*
	 * Function:GetZPosition, Get ZPosition with the input
	 * image(calibration&correlation with calibration profile which storage in
	 * DLL stack inputs: image:full image��float[][]) roi:x,y,width,height
	 * opt:Radius,RInterstep,bitDepth,halfQuadWidth,imgWidth,imgHeight
	 * calProfile:the calibration file storage in ram calPro:z position scale
	 * corrOpt_:calProfile length last zPos,divide number,threshold outputs:
	 * Object[0]:x,y,z Object[1]:Time Cost in this function,err_code
	 */
	public native Object[] GetZPosition(Object image_, int[] roi_,int index);

	/*
	 * GossCenter get the ball center,return xPos ,yPos image: full
	 * image(float[][]/short[]) roi:x,y,width,height
	 * opt:Radius,RInterstep,bitDepth
	 * ,halfQuadWidth,imgWidth,imgHeight,zX,zN,zLen,zSize Outputs:
	 * Object[0]:xPos,yPos Object[1]:Time Cost in this function,err_code
	 * Opt:bitDepth,halfQuadWidth,imgWidth,imgHeight
	 */
	public native Object[] GosseCenter(Object image_, int[] roi_,int[] opt_);

	/*
	 * DataInit set up static memory size opt:
	 * zStart,zScale,zStep,Radius,RInterstep Outputs: void
	 */
	public native void DataInit(double[] opt_);

	public native void DeleteData();

	public native void SetBitDepth(int bitDepth_);



}