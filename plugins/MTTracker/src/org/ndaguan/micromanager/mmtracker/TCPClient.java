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

import org.ndaguan.micromanager.mmtracker.TCPServer.ECMDLIST;

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

		tcp.setPosition("",3,4);
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
 

	public void setPosition(String xyStage_, double xpos, double ypos) {//set x  y
		// TODO Auto-generated method stub
		
	}

	public void setPosition(String zStage_, double zPos) {//set z
		// TODO Auto-generated method stub
		
	}

	public double[] getPosition() throws IOException {//return x y z
		byte CMD = (byte) ECMDLIST.QPOS.ordinal();
		SendCommand(new Object[]{CMD,0});
		
		return null;
	}
	protected void ReadMessage(Socket socket) {
		// COMMUNICATION HERE!!!!
		try {
			InputStream inStream = socket.getInputStream();
			byte[] rawData = new byte[BUFFER_SIZE];
			ByteBuffer buffer = ByteBuffer.wrap(rawData);
			while (true) {
				if (socket.isClosed()) {
					MMT.logMessage("Socket closed");
					break;
				}

				int[] offset = new int[1];
				offset[0] = 0;
				Arrays.fill(rawData, (byte) 0);
				if (inStream.read(rawData, offset[0], 2) == -1)
					break;

				if (!packAnalyzer.checkStart(rawData, offset)) {
					offset[0] += 2;
					continue;
				}
				offset[0] += 2;
				// Frame length
				inStream.read(rawData, offset[0], 2);

				short length = buffer.getShort(offset[0]);
				offset[0] += 2;
				// checksum
				inStream.read(rawData, offset[0], length - offset[0]);
				if (!packAnalyzer.checksum(rawData, length))
					continue;
				// OPERATION
				phaseData(socket, rawData, length, offset);
				offset[0] += 16;// Skip Checksum
			}

			socket.shutdownInput();
			socket.shutdownOutput();
		} catch (IOException e) {
			return;
		}
	}

}