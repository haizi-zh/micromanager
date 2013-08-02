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
				minInterval = 1;
				maxInterval = 200; 
				saveUserData();
				return;
			}
			BufferedReader in;
			in = new BufferedReader(new FileReader(dataFile));
			String line;
			if((line = in.readLine()) != null)
			{
				String[] temp = line.split(","); 
				int i = 0;
				comId = temp[i++];
				acceleration = Integer.parseInt(temp[i++]);
				minInterval = Integer.parseInt(temp[i++]);
				maxInterval = Integer.parseInt(temp[i++]);
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
			sData +=  comId+ " , ";
			sData +=  Integer.toString(acceleration)+ " , ";
			sData +=  Integer.toString(minInterval)+ " , ";
			sData +=  Integer.toString(maxInterval);
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
		int pulseToUm = 10;
		int pluseToSend =  (int) (distance/pulseToUm);

		maxInterval = 200;//ms
		minInterval = 1;//ms
		acceleration = 20;
		long interval = 0;

		for (int i = 0; i < pluseToSend; i++) {
			interval = Math.max(minInterval, maxInterval);
			maxInterval -= acceleration;
			serialPort.setDTR(true);//yellow
			TimeUnit.MILLISECONDS.sleep(interval);
			serialPort.setDTR(false);
			TimeUnit.MILLISECONDS.sleep(interval);
		}

		deviceIsBusy = false;

	}


}
