package org.ndaguan.micromanager.mmtracker;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
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

	private String address_;

	private Socket socket;

	public enum ECMDLIST {
		QPOS, MOV, MVR, SVO, QSVO, CST, QCST, SVA, QSVA, PLM, QPLM, NLM, QNLM
	};

	public static void main(String[] argv) throws Exception {
		TCPClient tcp = new TCPClient("localhost", 50501);

		double[] aa = tcp.getPosition();
		MMT.debugMessage("getPosition\t"+String.valueOf(aa[0])+"\t"+String.valueOf(aa[1])+"\t"+String.valueOf(aa[2]));
		//		tcp.setRelativePosition("zStage", 100,200);
		//		tcp.setRelativePosition("zStage", 300);
		//		
		//		tcp.setPosition("zStage", 100,200);
		//		tcp.setPosition("zStage", 300);

	}

	public static TCPClient getInstance(String address, int port) throws UnknownHostException, IOException {
		if(instance_ == null)
			instance_ = new TCPClient(address, port);
		return instance_;
	}
	public static TCPClient getInstance() {

		return instance_;
	}
	public TCPClient(String address, int port) throws UnknownHostException, IOException {
		address_ = address;
		port_ = port;
		socket= new Socket(address_, port_);
		isRunning_ = true;
		packAnalyzer = new PackageAnalyzer();
	}

	private void SendCommand(Object[] data) throws IOException {
		byte[] rawData = new byte[BUFFER_SIZE];
		int len = packAnalyzer.packData(data, rawData);
		OutputStream outPutSteam = socket.getOutputStream();
		outPutSteam.write(rawData, 0, len);
		outPutSteam.flush();
	}

	public void setPosition( double xpos, double ypos,double zpos) throws IOException {
		byte CMD = (byte) ECMDLIST.MOV.ordinal();
		int paraNum = 6;
		short paralen = 8;
		SendCommand(new Object[] { CMD, paraNum ,  paralen,0, paralen,	xpos, paralen,1, paralen,	ypos, paralen,2, paralen,	zpos});	
		Object[] ret = ReadMessage(socket);
		if(ret == null || !((boolean) ret[1]))
			MMT.logError("SetPosition Error!");
			 
	}
	public void setPosition(String xyStage_, double xpos, double ypos) throws IOException {//set x  y
		byte CMD = (byte) ECMDLIST.MOV.ordinal();
		int paraNum = 4;
		short paralen = 8;
		SendCommand(new Object[] { CMD, paraNum ,  paralen,0, paralen,	xpos, paralen,1, paralen,	ypos});
		Object[] ret = ReadMessage(socket);
		if(ret == null || !((boolean) ret[1]))
			MMT.logError("SetPosition Error!");
	}
	public void setRelativePosition(String xyStage_, double xpos, double ypos) throws IOException {//set x  y
		byte CMD = (byte) ECMDLIST.MVR.ordinal();
		int paraNum = 4;
		short paralen = 8;
		SendCommand(new Object[] { CMD, paraNum ,  paralen,0, paralen,	xpos, paralen,1, paralen,	ypos});
		Object[] ret = ReadMessage(socket);
		if(ret == null ||  !((boolean) ret[1]))
			MMT.logError("SetPosition Error!");
	}

	public void setPosition(String zStage_, double zpos) throws IOException {//set z
		byte CMD = (byte) ECMDLIST.MOV.ordinal();
		int paraNum = 2;
		short paralen = 8;
		SendCommand(new Object[] { CMD, paraNum ,  paralen,2, paralen,	zpos});
		Object[] ret = ReadMessage(socket);
		if(ret == null || !((boolean) ret[1]))
			MMT.logError("SetPosition Error!");
	}
	public void setRelativePosition(String zStage_, double zpos) throws IOException {//set z
		byte CMD = (byte) ECMDLIST.MVR.ordinal();
		int paraNum = 2;
		short paralen = 8;
		SendCommand(new Object[] { CMD, paraNum ,  paralen,2, paralen,	zpos});
		Object[] ret = ReadMessage(socket);
		if(ret == null ||  !((boolean) ret[1]))
			MMT.logError("SetPosition Error!");
	}

	public double[] getPosition() throws IOException {//return x y z
		byte CMD = (byte) ECMDLIST.QPOS.ordinal();
		SendCommand(new Object[]{CMD,0});
		Object[] ret = ReadMessage(socket);
		if(ret != null && (boolean) ret[1])
			return new double[]{(double) ret[3],(double) ret[5],(double) ret[7]};
		return null;
	}
	protected Object[] ReadMessage(Socket socket) {
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
				offset[0] += 1;//skip CMD
				return packAnalyzer.unpackParas(rawData, offset);
			}

			socket.shutdownInput();
			socket.shutdownOutput();
			return null;
		} catch (IOException e) {
			return null ;
		}
	}

	public boolean isRunning() {
		// TODO Auto-generated method stub
		return isRunning_;
	}
	public void stop() throws IOException{
		if(!isRunning_)
			return;
		else
		{
			socket.shutdownInput();
			socket.shutdownOutput();
			socket.close();
			isRunning_ = false;
			instance_ = null;
			packAnalyzer = null;
		}
	}


}