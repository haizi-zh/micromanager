package org.ndaguan.micromanager.stagecontrol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

import javax.comm.CommPortIdentifier;
import javax.comm.PortInUseException;
import javax.comm.SerialPort;
import javax.comm.UnsupportedCommOperationException;

public class SigmaKoki {

	private SerialPort serialPort;
	private OutputStream outputStream;
	private int baudRate = 9600;
	private String comId = "COM4";
	private InputStream inputStream;
	private String lastError = "No error";
	private boolean isDeviceReady;
	private static SigmaKoki instance;
	public static SigmaKoki getInstance()
	{
		if(instance == null)
			instance  = new SigmaKoki();
		return instance;
	}
	public SigmaKoki() {
		isDeviceReady = true;
		if(!initCom())
		{
			LogMessage(lastError);
			isDeviceReady = false;
		}
		else if(!initStage()){
			LogMessage(lastError);
			isDeviceReady = false;
			 
		}
	}

	public boolean isDeviceReady()
	{
		return isDeviceReady;
	}
	public boolean setPosition(double step)//um
	{
		int stepnM = (int) (step*1000);
		try {

			String flag = "+";
			if(stepnM<0){
				flag = "-";
				stepnM *= -1;
			}
			if(sendCommand("\r\nA:1"+flag+"P"+stepnM+"\r\nG:\r\n"))
				return true;
			else 
				return false;
		} catch (IOException e) {
			lastError = e.toString();
			return false;
		}
	}
	public boolean setRelativeStagePosition(double step) {
		int stepnM = (int) (step*1000);
		try {

			String flag = "+";
			if(stepnM<0){
				flag = "-";
				stepnM *= -1;
			}
			if(sendCommand("\r\nM:1"+flag+"P"+stepnM+"\r\nG:\r\n"))
				return true;
			else 
				return false;
		} catch (IOException e) {
			lastError = e.toString();
			return false;
		}

	}

	public double getPosition()
	{
		try {
			sendCommand("\r\nQ:\r\n");
			String buf = readAnswer();
			String[] temp = buf.split(",");
			return (double) Integer.parseInt(temp[0])/1000;
		} catch (IOException e) {
			lastError = e.toString();
			return -1;
		}
	}


	private void LogMessage(Object msg)
	{
		System.out.print(msg+"\r\n");
	}

	private boolean sendCommand(String command) throws IOException
	{
		outputStream.write(command.getBytes());
		return true;
	}
	private boolean checkStage()
	{
		if(readAnswer().equals("OK"))
			return true;
		if(readAnswer().equals("NG"))
			return false;
		if(readAnswer().isEmpty())
			return false;
		return true;

	}
	private boolean initStage()

	{
		try {
			sendCommand("\r\n?:N\r\n");
			String buf = readAnswer();
			LogMessage(buf);
			if(buf.isEmpty())
			{
				lastError = "Nothing read from "+comId;
				return false;
			}
			if(buf.equals("FINE-01r")){
				return true;
			}else{
				lastError = "UnKnown error";
				return false;
			}
			
		} catch (IOException e) {
			lastError = e.toString();
			return false;
		}
	}
	private String readAnswer()
	{
		byte[] readBuffer = new byte[20];

		try {
			TimeUnit.MILLISECONDS.sleep(50);
			while (inputStream.available() > 0) {
				int numBytes = inputStream.read(readBuffer);
			}
			String answer = new String(readBuffer);
			String[] temp = answer.split("\r\n");
			int line = temp.length;
			return temp[0].trim();
		}catch (IOException e) {
			lastError = e.toString();
			return null;
		} catch (InterruptedException e) {
			lastError = e.toString();
			return null;
		}
	}

	private boolean initCom()
	{
		Enumeration portList = CommPortIdentifier.getPortIdentifiers();

		while (portList.hasMoreElements()) {
			CommPortIdentifier portId = (CommPortIdentifier) portList.nextElement();
			if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				if (portId.getName().equals(comId)) {
					try {
						serialPort = (SerialPort)
								portId.open("SigmaKoki", 2000);
					} catch (PortInUseException e) {
						lastError = e.toString();
						return false;
					}

					try {
						outputStream = serialPort.getOutputStream();
						inputStream = serialPort.getInputStream();
					} catch (IOException e) {
						lastError = e.toString();
						return false;
					}
					try {
						serialPort.setSerialPortParams(baudRate ,
								SerialPort.DATABITS_8,
								SerialPort.STOPBITS_1,
								SerialPort.PARITY_NONE);
					} catch (UnsupportedCommOperationException e) {
						lastError = e.toString();
						return false;
					}
					return true;
				}
			}
		}
		lastError = "Can not find " + comId;
		return false;

	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SigmaKoki sk = new SigmaKoki();
		if(sk.isDeviceReady()){
			sk.setPosition(0.5);
			sk.LogMessage(sk.getPosition());
		}else{
			sk.LogMessage("Device is not ready");
		}
	}

}
