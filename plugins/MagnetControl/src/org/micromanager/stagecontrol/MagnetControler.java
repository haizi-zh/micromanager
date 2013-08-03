package org.micromanager.stagecontrol;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

import javax.comm.CommPortIdentifier;
import javax.comm.PortInUseException;
import javax.comm.SerialPort;

public class MagnetControler {

	private static MagnetControler instance_;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		 MagnetControler mc = MagnetControler.getInstance();
		 try {
			mc.moveMagnet(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private boolean deviceIsBusy;
	private SerialPort serialPort;
	private int acceleration;
	private int minInterval;
	private int maxInterval;
	private String comId;
	private double pulseToUm;

	public static MagnetControler getInstance(){
		if(instance_ == null)
			instance_ = new MagnetControler();
		return instance_;
	}
	
	public MagnetControler(){
		getUserData();
		initComn();
	}

	private void getUserData() {
		try {
			File dataFile = new File(System.getProperty("user.home")+"/MagnetControl/userPreferences.txt");
			if(!dataFile.exists()){
				comId = "COM1";
				acceleration = 20;
				minInterval = 10;
				maxInterval = 200; 
				pulseToUm = 2.6;
				saveUserData();
				return;
			}
			BufferedReader in;
			in = new BufferedReader(new FileReader(dataFile));
			String line;
			if((line = in.readLine()) != null)
			{
				String[] var = line.split(";"); 
				String[] value = new String[2];
				int i = 0;
				
				value = var[i++].split(":");
				comId = value[1];
				value = var[i++].split(":");
				acceleration = Integer.parseInt(value[1]);
				value = var[i++].split(":");
				minInterval = Integer.parseInt(value[1]);
				value = var[i++].split(":");
				maxInterval = Integer.parseInt(value[1]);
				value = var[i++].split(":");
				pulseToUm = Double.parseDouble(value[1]);
			}
			in.close();
		} catch (IOException e) {
			return;
		} 
	}


	public void saveUserData(){
		try {
			File dir = new File(System.getProperty("user.home"),"MagnetControl");
			if(!dir.isFile())
				dir.mkdirs();

			File loginDataFile = new File(System.getProperty("user.home")+"/MagnetControl/userPreferences.txt");
			FileWriter out = new FileWriter((loginDataFile)); 
			String sData = "";
			sData += "comId:"+comId+";";
			sData += "acceleration:"+Integer.toString(acceleration)+ ";";
			sData += "maxInterval:"+Integer.toString(minInterval)+ ";";
			sData += "maxInterval:"+Integer.toString(maxInterval)+ ";";
			sData += "pulseToUm:"+Double.toString(pulseToUm);
			out.write(sData);
			out.close(); 
		} catch (IOException e) {
			return;
		}
	}
	private void initComn() {
		Enumeration portList = CommPortIdentifier.getPortIdentifiers();
		while (portList.hasMoreElements()) {
			CommPortIdentifier portId = (CommPortIdentifier) portList.nextElement();
			if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				if (portId.getName().equals(comId)) {
					try {
						serialPort = (SerialPort)
								portId.open("", 2000);
					} catch (PortInUseException e) {}

				}
			}
		}
	}

	public void moveMagnet(double z) throws InterruptedException {
		if(deviceIsBusy)
			return;

		deviceIsBusy = true;

		if(z<0){
			serialPort.setRTS(true);//BLUE
		}else{
			serialPort.setRTS(false);//BLUE
		}
		TimeUnit.MILLISECONDS.sleep(50);
		double distance = Math.abs(z);
		int pluseToSend =  (int) (distance/pulseToUm);
		long interval = 0;
		int mInterval = maxInterval;
		for (int i = 0; i < pluseToSend; i++) {
			interval = Math.max(minInterval, mInterval);
			mInterval -= acceleration;
			serialPort.setDTR(true);//yellow
			TimeUnit.MICROSECONDS.sleep(interval);
			serialPort.setDTR(false);
			TimeUnit.MICROSECONDS.sleep(interval);
			System.out.print(String.format("%d/%d\r\n", i,pluseToSend));
		}

		deviceIsBusy = false;

	}


}
