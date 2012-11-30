package org.ndaguan.micromanager;

public class CMMCore {

	public void loadSystemConfiguration(String string) {
		System.out.print("loadSystemConfiguration\r\n");
	}

	public String getLoadedDevices() {
		return "Device a\tDevice b\tDevice c\t";
	}

	public double getXPosition(String xystage_) {
		return 3;
	}

	public double getYPosition(String xystage_) {
		return 4;
	}

	public double getPosition(String zstage_) {
		return 5;
	}

	public String getXYStageDevice() {
		return "xystage";
	}

	public String getFocusDevice() {
		return "zstage";
	}



	public String getProperty(String xystage_, String string) {
		if( string.equals("ServoModeX") || string.equals("ServoModeY") || string.equals("ServoModeZ"))
			return "True";
		else
			return "100.0";
	}

	public void setProperty(String label, String string, String string2) {
		System.out.print(String.format("\r\nCall Function setProperty: %s\t#%s",string, string2));	
	}

	public void setPosition(String zstage_, double pos) {
		System.out.print(String.format("\r\nCall Function setPosition: %s\t#zpos:%f",zstage_, pos));			
	}

	public void setRelativePosition(String zstage_, double pos) {
		System.out.print(String.format("\r\nCall Function setRelativePosition: %s\t#zpos:%f",zstage_, pos));		
	}

	public void getXYPosition(String xystage_, double[] xpos, double[] ypos) {
		xpos[0] = 3.3;
		ypos[0] = 4.4;
		System.out.print(String.format("\r\nCall Function getXYPosition: #xpos:%f\t#ypos:%f",xpos[0],ypos[0]));

	}

	public void setXYPosition(String xystage_, double d, double e) {
		System.out.print(String.format("\r\nCall Function setXYPosition: #xpos:%f\t#ypos:%f",d,e));
	}

	public void setRelativeXYPosition(String xystage_, double d, double e) {
		System.out.print(String.format("\r\nCall Function setRelativeXYPosition: #xpos:%f\t#ypos:%f",d,e));
	}

	public void logMessage(String string) {
		//System.out.print(string);
	}

}
