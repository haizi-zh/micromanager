package org.ndaguan.micromanager.mmtracker;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import mmcorej.CMMCore;

/**
 * @author Administrator
 *TCPServer.java
 *Acting as a server to communicate with the remote/local PC
 */
class TCPClient {
	private static TCPClient instance_;

	private boolean isRunning_ = false;

	private int port_;
	private final int BUFFER_SIZE = 1024;
	private PackageAnalyzer packAnalyzer;

	private String LastErr = null;

	private String address_;

	private Socket socket;

	public enum ECMDLIST {
		QPOS, MOV, MVR, SVO, QSVO, CST, QCST, SVA, QSVA, PLM, QPLM, NLM, QNLM
	};

	public static void main(String[] argv) throws Exception {
		TCPClient tcp = new TCPClient("localhost", 50501);

		tcp.setXYPosition("",3,4);
		TimeUnit.MILLISECONDS.sleep(100);

	}

	public static TCPClient getInstance(String address, int port) {
		if(instance_ == null)
			try {
				instance_ = new TCPClient(address, port);
			} catch (IOException e) {
				MMT.logError("TCPClient error"+e.toString());
			}
		return instance_;
	}
	public static TCPClient getInstance() {
		
		return instance_;
	}
	public TCPClient(String address, int port) throws UnknownHostException, IOException {
		address_ = address;
		port_ = port;
		socket= new Socket(address_, port_);
		packAnalyzer = new PackageAnalyzer();
	}
   
	private void SendCommand(Object[] data) throws IOException {
		byte[] rawData = new byte[BUFFER_SIZE];
		int len = packAnalyzer.packData(data, rawData);
		OutputStream outPutSteam = socket.getOutputStream();
		outPutSteam.write(rawData, 0, len);
		outPutSteam.flush();
	}

	public void setXYPosition(String xyStage_, double xpos, double yPosition) throws IOException {
		SendCommand(new Object[]{2,1,3,4});
	}
	
	public double[] getXYPosition(String xyStage_) {
		// TODO Auto-generated method stub
		return null;
	}
	public void setXYZPosition(String xyStage_, double xpos, double yPos, double zPos) {
		// TODO Auto-generated method stub
	}
	
	public double[] getXYZPosition(String xyStage_) {
		// TODO Auto-generated method stub
		return null;
	}

	public double getYPosition(String xyStage_) {
		// TODO Auto-generated method stub
		return 0;
	}

	public void setYPosition(String xyStage_, double ypos) {
		// TODO Auto-generated method stub
		
	}
	
	public double getXPosition(String xyStage_) {
		// TODO Auto-generated method stub
		return 0;
	}
	public void setXPosition(String xyStage_,double xpos) {
		// TODO Auto-generated method stub
	}

	public double getZPosition(String xyStage_) {
		// TODO Auto-generated method stub
		return 0;
	}
	public void setZPosition(String xyStage_,double xpos) {
		// TODO Auto-generated method stub
	}
}