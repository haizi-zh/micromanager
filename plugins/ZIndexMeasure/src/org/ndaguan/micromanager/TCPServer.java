package org.ndaguan.micromanager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import mmcorej.CMMCore;

class TCPServer {
	private boolean isRunning_ = false;

	private int port_;
	private ExecutorService exec_;
	private final int BUFFER_SIZE = 1024;
	 
	private String zstage_;
	private String xystage_;
	private CMMCore core_;
	private static packageAnalyzer packAnalyzer;

	public static void main(String[] argv) throws Exception {
		// CMMCore core = new CMMCore();
		// core.loadSystemConfiguration("MMConfig_demo.cfg");
		packAnalyzer = new packageAnalyzer();
		TCPServer tcpServer_ = new TCPServer(50501);
		tcpServer_.start();
		TimeUnit.HOURS.sleep(24);
	}

	public final String[] CMDLIST = new String[] { "QPOS", "MOV", "MVR", "SVO",
			"QSVO", "SVA", "QSVA", "QPLM","QNLM", "CST", "QCST" };

	public  enum ECMDLIST { QPOS, MOV, MVR, SVO,QSVO, SVA, QSVA, QPLM,QNLM, CST, QCST };

	public TCPServer(CMMCore core, int port) {
		core_ = core;
		xystage_ = core_.getXYStageDevice();
		zstage_ = core_.getFocusDevice();
		port_ = port;
	}

	public TCPServer(int port) {
		port_ = port;
	}

	public void start() {
		if (isRunning_)
			return;

		exec_ = Executors.newCachedThreadPool(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setDaemon(true);
				return t;
			}
		});

		exec_.submit(new Runnable() {
			@Override
			public void run() {
				newListener();
			}
		});

		isRunning_ = true;
	}

	public void stop() throws InterruptedException {
		if (!isRunning_)
			return;

		exec_.shutdownNow();
		exec_.awaitTermination(1, TimeUnit.SECONDS);

		isRunning_ = false;
	}

	protected void newListener() {
		try {
			ServerSocket svr = new ServerSocket(port_);
			while (true) {
				final Socket socket = svr.accept();
				Runnable r = new Runnable() {
					@Override
					public void run() {
						subThread(socket);
					}
				};
				exec_.submit(r);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void subThread(Socket socket) {
		// COMMUNICATION HERE!!!!
		InputStream inStream;

		try {
			inStream = socket.getInputStream();
			byte[] rawData = new byte[BUFFER_SIZE];
			ByteBuffer buffer = ByteBuffer.wrap(rawData);
			while (true) {
				
				int[] offset = new int[1];
				offset[0] = 0;
				inStream.read(rawData, offset[0], 2);
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
				offset[0]+=16;//Checksum

			}
		} catch (IOException e) {
			return;
		}

	}

	private void phaseData(Socket socket, byte[] rawData, short length,
			int[] offset) {
		// DATA FORMAT:
		// START_FLAG, DATA_LEN, CMD_FLAG, PARA_NUM, [PARAS], MD5
		// 0XFFFE(16bit-short),(16bit-short),(8bit-char),(8bit-byte),(random-bit,random-type),(16bit-byte);

		byte CMD = rawData[offset[0]];
		offset[0]++;

		switch (CMDLIST[CMD]) {
		case "QPOS":
			QueryPosition(socket);
			break;
		case "MOV":
			setPositions(packAnalyzer.unpackParas(rawData, offset), true);
			break;
		case "MVR":
			setPositions(packAnalyzer.unpackParas(rawData, offset), false);
			break;
		case "SVA":
			setOpenLoopValue(packAnalyzer.unpackParas(rawData, offset));
			break;
		case "QSVA":
			queryOpenLoopValue(socket);
			break;
		case "SVO":
			setServoMode(packAnalyzer.unpackParas(rawData, offset));
			break;
		case "QSVO":
			QueryServoMode(socket);
			break;
		case "QPLM":
			QueryPLM(socket);
			break;
		case "QNLM":
			QueryNLM(socket);
			break;
		case "CST":
			SetCST(packAnalyzer.unpackParas(rawData, offset));
			break;
		case "QCST":
			QueryCST(socket);
			break;
		default:
			break;
		}

	}
	
	private void QueryPosition(Socket socket) {
		try {
			byte[] rawData = new byte[BUFFER_SIZE];
			byte CMD = 0;
			short paraNum = 3;
			double currxpos_ = 10;// core_.getXPosition(xystage_);
			double currypos_ = 11;// core_.getYPosition(xystage_);
			double currzpos_ = 12;// core_.getPosition(zstage_);

			short paralen = 8;
			
			Object[] data = new Object[] { CMD, paraNum, paralen, currxpos_,
					paralen, currypos_, paralen, currzpos_ };
			int len = packAnalyzer.packData(data, rawData);
			OutputStream outPutSteam = socket.getOutputStream();
			outPutSteam.write(rawData, 0, len);
			outPutSteam.flush();
			outPutSteam.close();

		} catch (Exception e) {
			setErrCode("get StagePosition ERR");
			e.printStackTrace();
		}

	}


	private void setPosition(Object axi, Object pos_, boolean flag)
			throws Exception {
		double pos = Double.parseDouble(pos_.toString());
		switch ((int) (Double.parseDouble(axi.toString()))) {
		case 0:
			if (flag)
				setXPosition(pos);
			else
				setRXPosition(pos);
			break;

		case 1:
			if (flag)
				setYPosition(pos);
			else
				setRYPosition(pos);
			break;

		case 2:
			if (flag)
				setZPosition(pos);
			else
				setRZPosition(pos);
			break;

		default:
			setErrCode("axis undefined!");
			break;

		}
	}
	

	public void setXPosition(double xpos) throws Exception {

		System.out.print(String.format("\r\nSetXPosition:\t%f", xpos));
		if (true)
			return;
		core_.setXYPosition(xystage_, xpos, core_.getYPosition(xystage_));

	}

	public void setYPosition(double ypos) throws Exception {
		System.out.print(String.format("\r\nSetYPosition:\t%f", ypos));
		if (true)
			return;
		core_.setXYPosition(xystage_, core_.getXPosition(xystage_), ypos);

	}

	public void setZPosition(double zpos) throws Exception {
		System.out.print(String.format("\r\nSetZPosition:\t%f", zpos));
		if (true)
			return;
		core_.setPosition(zstage_, zpos);

	}

	public void setRXPosition(double xpos) throws Exception {
		System.out.print(String.format("\r\nSetRXPosition:\t%f", xpos));
		if (true)
			return;
		core_.setRelativeXYPosition(xystage_, xpos, 0);

	}

	public void setRYPosition(double ypos) throws Exception {
		System.out.print(String.format("\r\nSetYPosition:\t%f", ypos));
		if (true)
			return;
		core_.setRelativeXYPosition(xystage_, 0, ypos);

	}

	public void setRZPosition(double zpos) throws Exception {
		System.out.print(String.format("\r\nSetZPosition:\t%f", zpos));
		if (true)
			return;
		core_.setRelativePosition(zstage_, zpos);

	}
	
	private void setOpenLoopValue(Object[] para) {
		if (para == null) {
			setErrCode("paraNum undefined!");
			return;
		}
		switch (para.length) {
		case 4:
			try {
				setOpenLoopValue(para[1], para[3]);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				setErrCode("setOpenLoopValue ERR!");
				e.printStackTrace();
			}
			break;
		case 8:
			try {
				setOpenLoopValue(para[1], para[3]);
				setOpenLoopValue(para[5], para[7]);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				setErrCode("setOpenLoopValue ERR!");
				e.printStackTrace();
			}
			break;
		case 12:
			try {
				setOpenLoopValue(para[1], para[3]);
				setOpenLoopValue(para[5], para[7]);
				setOpenLoopValue(para[9], para[11]);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				setErrCode("setOpenLoopValue ERR!");
				e.printStackTrace();
			}
			break;
		}
		
	}
	private void setOpenLoopValue(Object axi, Object value) {
		 
		double value_ = Double.parseDouble(value.toString());
		
		switch ((int) (Double.parseDouble(axi.toString()))) {
		case 0:			
			System.out.print(String.format("\r\ncore_.setProperty(xystage_, \"OpenLoopValueX\", %f)", value_));
			//core_.setProperty(xystage_, "OpenLoopValueX", value_);
			break;

		case 1:
			System.out.print(String.format("\r\ncore_.setProperty(xystage_, \"OpenLoopValueY\", %f)", value_));
			//core_.setProperty(xystage_, "OpenLoopValueY", value_);
			break;

		case 2:
			System.out.print(String.format("\r\ncore_.setProperty(zstage_, \"OpenLoopValueZ\", %f)", value_));
			//core_.setProperty(zstage_, "OpenLoopValueZ", value_);
			break;

		default:
			setErrCode("axis undefined!");
			break;

		}
		
	}
	private void queryOpenLoopValue(Socket socket) {
		try {
			byte[] rawData = new byte[BUFFER_SIZE];
			byte CMD = (byte)ECMDLIST.SVA.ordinal();
			short paraNum = 3;
 
			float openLoopValueX = 333;//	Double.parseDouble(core_.getProperty(xystage_, "OpenLoopValueX"));
			float openLoopValueY = 444;//	Double.parseDouble(core_.getProperty(xystage_, "OpenLoopValueY"));
			float openLoopValueZ = 555;//	Double.parseDouble(core_.getProperty(zstage_, "OpenLoopValueZ"));
			
			core_.getProperty(xystage_, "OpenLoopValueX");
						
			short paralen = 4;
			
			Object[] data = new Object[] { CMD, paraNum, paralen, openLoopValueX,paralen, openLoopValueY, paralen, openLoopValueZ };
			int len = packAnalyzer.packData(data, rawData);
			OutputStream outPutSteam = socket.getOutputStream();
			outPutSteam.write(rawData, 0, len);
			outPutSteam.flush();
			outPutSteam.close();

		} catch (Exception e) {
			setErrCode("get StagePosition ERR");
			e.printStackTrace();
		}
		
	}
	private void setServoMode(Object[] para) {
		if (para == null) {
			setErrCode("paraNum undefined!");
			return;
		}
		switch (para.length) {
		case 4:
			try {
				setServoMode(para[1], para[3]);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				setErrCode("setServoMode ERR!");
				e.printStackTrace();
			}
			break;
		case 8:
			try {
				setServoMode(para[1], para[3]);
				setServoMode(para[5], para[7]);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				setErrCode("setServoMode ERR!");
				e.printStackTrace();
			}
			break;
		case 12:
			try {
				setServoMode(para[1], para[3]);
				setServoMode(para[5], para[7]);
				setServoMode(para[9], para[11]);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				setErrCode("setServoMode ERR!");
				e.printStackTrace();
			}
			break;
		}
		
		
	}
		
	private void setServoMode(Object axi, Object value) {
	double value_ = Double.parseDouble(value.toString());
		
		switch ((int) (Double.parseDouble(axi.toString()))) {
		case 0:			
			System.out.print(String.format("\r\ncore_.setProperty(xystage_, \"ServoModeX\", %f)", value_));
			//core_.setProperty(xystage_, "ServoModeX", value_);
			break;

		case 1:
			System.out.print(String.format("\r\ncore_.setProperty(xystage_, \"ServoModeY\", %f)", value_));
			//core_.setProperty(xystage_, "ServoModeY", value_);
			break;

		case 2:
			System.out.print(String.format("\r\ncore_.setProperty(zstage_, \"ServoModeZ\", %f)", value_));
			//core_.setProperty(zstage_, "ServoModeZ", value_);
			break;

		default:
			setErrCode("axis undefined!");
			break;

		}
		
	}

	private void QueryServoMode(Socket socket) {
		try {
			byte[] rawData = new byte[BUFFER_SIZE];
			byte CMD = (byte)ECMDLIST.QSVO.ordinal();
			short paraNum = 3;
								
			boolean servoModeX = true;//	core_.getProperty(xystage_, "ServoModeX");
			boolean servoModeY = true;//	core_.getProperty(xystage_, "ServoModeY");
			boolean servoModeZ = true;//	core_.getProperty(zstage_, "ServoModeZ");
						
			short paralen = 8;
			
			Object[] data = new Object[] { CMD, paraNum, paralen, servoModeX,paralen, servoModeY, paralen, servoModeZ };
			int len = packAnalyzer.packData(data, rawData);
			OutputStream outPutSteam = socket.getOutputStream();
			outPutSteam.write(rawData, 0, len);
			outPutSteam.flush();
			outPutSteam.close();

		} catch (Exception e) {
			setErrCode("get StagePosition ERR");
			e.printStackTrace();
		}
		
	}

	
	
	private void QueryPLM(Socket socket) {
		try {
			byte[] rawData = new byte[BUFFER_SIZE];
			byte CMD = (byte)ECMDLIST.QPLM.ordinal();
			short paraNum = 3;
			//PARA start								
			float xAxis = 100;
			float yAxis = 100;
			float zAxis = 10;

			short paralen = 4;
			
			Object[] data = new Object[] { CMD, paraNum, paralen, xAxis,paralen, yAxis, paralen, zAxis };
			int len = packAnalyzer.packData(data, rawData);
			OutputStream outPutSteam = socket.getOutputStream();
			outPutSteam.write(rawData, 0, len);
			outPutSteam.flush();
			outPutSteam.close();

		} catch (Exception e) {
			setErrCode("QueryPLM ERR");
			e.printStackTrace();
		}
		
	}

	private void QueryNLM(Socket socket) {
		try {
			byte[] rawData = new byte[BUFFER_SIZE];
			byte CMD = (byte)ECMDLIST.QNLM.ordinal();
			short paraNum = 3;
			//PARA start								
			float xAxis = 0;
			float yAxis = 0;
			float zAxis = 0;

			short paralen = 4;
			
			Object[] data = new Object[] { CMD, paraNum, paralen, xAxis,paralen, yAxis, paralen, zAxis };
			int len = packAnalyzer.packData(data, rawData);
			OutputStream outPutSteam = socket.getOutputStream();
			outPutSteam.write(rawData, 0, len);
			outPutSteam.flush();
			outPutSteam.close();

		} catch (Exception e) {
			setErrCode("QueryPLM ERR");
			e.printStackTrace();
		}
		
		
	}


	/**
	 * @param para
	 * @param flag
	 *            : true set absPosition,false:set rePostition
	 */
	private void setPositions(Object[] para, boolean flag) {
		if (para == null) {
			setErrCode("paraNum undefined!");
			return;
		}
		switch (para.length) {
		case 4:
			try {
				setPosition(para[1], para[3], flag);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				setErrCode("setPosition ERR!");
				e.printStackTrace();
			}
			break;
		case 8:
			try {
				setPosition(para[1], para[3], flag);
				setPosition(para[5], para[7], flag);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				setErrCode("setPosition ERR!");
				e.printStackTrace();
			}
			break;
		case 12:
			try {
				setPosition(para[1], para[3], flag);
				setPosition(para[5], para[7], flag);
				setPosition(para[9], para[11], flag);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				setErrCode("setPosition ERR!");
				e.printStackTrace();
			}
			break;
		}

	}


	private void setErrCode(String string) {
		// TODO Auto-generated method stub

	}

	
	
	private void SetCST(Object[] unpackParas) {
		// TODO Auto-generated method stub
		
	}

	private void QueryCST(Socket socket) {//watch out
		try {
			byte[] rawData = new byte[BUFFER_SIZE];
			byte CMD = (byte)ECMDLIST.QCST.ordinal();
			short paraNum = 3;
								
			boolean servoModeX = true;//	core_.getProperty(xystage_, "QCSTX");
			boolean servoModeY = true;//	core_.getProperty(xystage_, "QCSTY");
			boolean servoModeZ = true;//	core_.getProperty(zstage_, "QCSTZ");
						
			short paralen = 8;
			
			Object[] data = new Object[] { CMD, paraNum, paralen, servoModeX,paralen, servoModeY, paralen, servoModeZ };
			int len = packAnalyzer.packData(data, rawData);
			OutputStream outPutSteam = socket.getOutputStream();
			outPutSteam.write(rawData, 0, len);
			outPutSteam.flush();
			outPutSteam.close();

		} catch (Exception e) {
			setErrCode("get StagePosition ERR");
			e.printStackTrace();
		}
		
	}



	private void packData(double[] pos, byte[] rawData) {
		// TODO Auto-generated method stub

	}

	public boolean isRunning() {
		return isRunning_;
	}

}