package org.ndaguan.micromanager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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
	private PackageAnalyzer packAnalyzer;

	public static void main(String[] argv) throws Exception {
		System.out.print(System.getProperty("user.dir"));
		CMMCore core = new CMMCore();
		core.loadSystemConfiguration("MMConfig_demo.cfg");
		TCPServer tcpServer_ = new TCPServer(core, 50501);
		System.out.print(core.getLoadedDevices());
		tcpServer_.start();
		TimeUnit.HOURS.sleep(24);
	}

	public final String[] CMDLIST = new String[] { "QPOS", "MOV", "MVR", "SVO",
			"QSVO", "CST", "QCST", "SVA", "QSVA", "PLM", "QPLM", "NLM", "QNLM" };

	public enum ECMDLIST {
		QPOS, MOV, MVR, SVO, QSVO, CST, QCST, SVA, QSVA, PLM, QPLM, NLM, QNLM
	};

	public TCPServer(CMMCore core, int port) {
		core_ = core;
		xystage_ = core_.getXYStageDevice();
		zstage_ = core_.getFocusDevice();
		port_ = port;
		packAnalyzer = new PackageAnalyzer();
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

		try {
			InputStream inStream = socket.getInputStream();
			byte[] rawData = new byte[BUFFER_SIZE];
			ByteBuffer buffer = ByteBuffer.wrap(rawData);
			while (true) {

				int[] offset = new int[1];
				offset[0] = 0;
				Arrays.fill(rawData, (byte) 0);
				int cnt = inStream.read(rawData, offset[0], 2);
				if (cnt == 0)
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
				offset[0] += 16;// Checksum

			}
		} catch (IOException e) {
			return;
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void phaseData(Socket socket, byte[] rawData, short length,
			int[] offset) throws Exception {
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
			byte CMD = (byte) ECMDLIST.QPOS.ordinal();
			short paraNum = 3;
			double currxpos_ = core_.getXPosition(xystage_);
			double currypos_ = core_.getYPosition(xystage_);
			double currzpos_ = core_.getPosition(zstage_);

			short paralen = 8;

			Object[] data = new Object[] { CMD, paraNum, paralen, currxpos_,
					paralen, currypos_, paralen, currzpos_ };
			int len = packAnalyzer.packData(data, rawData);
			OutputStream outPutSteam = socket.getOutputStream();
			outPutSteam.write(rawData, 0, len);
			outPutSteam.flush();
			// outPutSteam.close();

		} catch (Exception e) {
			setErrCode("get StagePosition ERR");
			e.printStackTrace();
		}

	}

	private void setPosition(Object axis, Object pos_, boolean flag)
			throws Exception {
		double pos = Double.parseDouble(pos_.toString());
		switch ((int) (Double.parseDouble(axis.toString()))) {
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
		core_.setXYPosition(xystage_, xpos, core_.getYPosition(xystage_));

	}

	public void setYPosition(double ypos) throws Exception {
		double xpos = core_.getXPosition(xystage_);
		System.out.print(String.format("\r\nSetYPosition:\t(%f, %f)", xpos,
				ypos));
		core_.setXYPosition(xystage_, xpos, ypos);

	}

	public void setZPosition(double zpos) throws Exception {
		System.out.print(String.format("\r\nSetZPosition:\t%f", zpos));
		core_.setPosition(zstage_, zpos);

	}

	public void setRXPosition(double xpos) throws Exception {
		System.out.print(String.format("\r\nSetRXPosition:\t%f", xpos));
		core_.setRelativeXYPosition(xystage_, xpos, 0);

	}

	public void setRYPosition(double ypos) throws Exception {
		System.out.print(String.format("\r\nSetYPosition:\t%f", ypos));
		core_.setRelativeXYPosition(xystage_, 0, ypos);

	}

	public void setRZPosition(double zpos) throws Exception {
		System.out.print(String.format("\r\nSetZPosition:\t%f", zpos));
		core_.setRelativePosition(zstage_, zpos);

	}

	private void setOpenLoopValue(Object[] para) throws Exception {
		if (para == null) {
			setErrCode("paraNum undefined!");
			return;
		}

		HashMap<Integer, Object> paraMap = extractPara(para);
		String[] props = new String[] { "X", "Y", "Z" };
		for (int i = 0; i < props.length; i++)
			props[i] = "OpenLoopValue" + props[i];
		for (Integer axis : paraMap.keySet()) {
			String label = (axis == 2) ? zstage_ : xystage_;
			core_.setProperty(label, props[axis],
					((Number) paraMap.get(axis)).doubleValue());
		}

//		switch (para.length) {
//		case 4:
//			try {
//				setOpenLoopValue(para[1], para[3]);
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				setErrCode("setOpenLoopValue ERR!");
//				e.printStackTrace();
//			}
//			break;
//		case 8:
//			try {
//				setOpenLoopValue(para[1], para[3]);
//				setOpenLoopValue(para[5], para[7]);
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				setErrCode("setOpenLoopValue ERR!");
//				e.printStackTrace();
//			}
//			break;
//		case 12:
//			try {
//				setOpenLoopValue(para[1], para[3]);
//				setOpenLoopValue(para[5], para[7]);
//				setOpenLoopValue(para[9], para[11]);
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				setErrCode("setOpenLoopValue ERR!");
//				e.printStackTrace();
//			}
//			break;
//		}

	}

	private void setOpenLoopValue(Object axi, Object value) throws Exception {

		double value_ = Double.parseDouble(value.toString());

		switch ((int) (Double.parseDouble(axi.toString()))) {
		case 0:
			System.out.print(String.format(
					"\r\ncore_.setProperty(xystage_, \"OpenLoopValueX\", %f)",
					value_));
			core_.setProperty(xystage_, "OpenLoopValueX", value_);
			break;

		case 1:
			System.out.print(String.format(
					"\r\ncore_.setProperty(xystage_, \"OpenLoopValueY\", %f)",
					value_));
			core_.setProperty(xystage_, "OpenLoopValueY", value_);
			break;

		case 2:
			System.out.print(String.format(
					"\r\ncore_.setProperty(zstage_, \"OpenLoopValueZ\", %f)",
					value_));
			core_.setProperty(zstage_, "OpenLoopValueZ", value_);
			break;

		default:
			setErrCode("axis undefined!");
			break;

		}

	}

	private void queryOpenLoopValue(Socket socket) {
		try {
			byte[] rawData = new byte[BUFFER_SIZE];
			byte CMD = (byte) ECMDLIST.SVA.ordinal();
			short paraNum = 3;

			double openLoopValueX = Double.parseDouble(core_.getProperty(
					xystage_, "OpenLoopValueX"));
			double openLoopValueY = Double.parseDouble(core_.getProperty(
					xystage_, "OpenLoopValueY"));
			double openLoopValueZ = Double.parseDouble(core_.getProperty(
					zstage_, "OpenLoopValueZ"));

			short paralen = 8;

			Object[] data = new Object[] { CMD, paraNum, paralen,
					openLoopValueX, paralen, openLoopValueY, paralen,
					openLoopValueZ };
			int len = packAnalyzer.packData(data, rawData);
			OutputStream outPutSteam = socket.getOutputStream();
			outPutSteam.write(rawData, 0, len);
			outPutSteam.flush();
			// outPutSteam.close();

		} catch (Exception e) {
			setErrCode("get StagePosition ERR");
			e.printStackTrace();
		}

	}

	private void setServoMode(Object[] para) throws Exception {
		if (para == null) {
			setErrCode("paraNum undefined!");
			return;
		}

		HashMap<Integer, Object> paraMap = extractPara(para);
		String[] props = new String[] { "X", "Y", "Z" };
		for (int i = 0; i < props.length; i++)
			props[i] = "ServoMode" + props[i];
		for (Integer axis : paraMap.keySet()) {
			String label = (axis == 2) ? zstage_ : xystage_;
			core_.setProperty(label, props[axis],
					(Boolean) paraMap.get(axis) ? "True" : "False");
		}

//		switch (para.length) {
//		case 4:
//			try {
//				setServoMode(para[1], para[3]);
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				setErrCode("setServoMode ERR!");
//				e.printStackTrace();
//			}
//			break;
//		case 8:
//			try {
//				setServoMode(para[1], para[3]);
//				setServoMode(para[5], para[7]);
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				setErrCode("setServoMode ERR!");
//				e.printStackTrace();
//			}
//			break;
//		case 12:
//			try {
//				setServoMode(para[1], para[3]);
//				setServoMode(para[5], para[7]);
//				setServoMode(para[9], para[11]);
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				setErrCode("setServoMode ERR!");
//				e.printStackTrace();
//			}
//			break;
//		}

	}

	private void setServoMode(Object axi, Object value) {
		double value_ = Double.parseDouble(value.toString());

		switch ((int) (Double.parseDouble(axi.toString()))) {
		case 0:
			System.out.print(String.format(
					"\r\ncore_.setProperty(xystage_, \"ServoModeX\", %f)",
					value_));
			// core_.setProperty(xystage_, "ServoModeX", value_);
			break;

		case 1:
			System.out.print(String.format(
					"\r\ncore_.setProperty(xystage_, \"ServoModeY\", %f)",
					value_));
			// core_.setProperty(xystage_, "ServoModeY", value_);
			break;

		case 2:
			System.out.print(String.format(
					"\r\ncore_.setProperty(zstage_, \"ServoModeZ\", %f)",
					value_));
			// core_.setProperty(zstage_, "ServoModeZ", value_);
			break;

		default:
			setErrCode("axis undefined!");
			break;

		}

	}

	private void QueryServoMode(Socket socket) {
		try {
			byte[] rawData = new byte[BUFFER_SIZE];
			byte CMD = (byte) ECMDLIST.QSVO.ordinal();
			short paraNum = 3;

			boolean servoModeX = core_.getProperty(xystage_, "ServoModeX")
					.equals("True");
			boolean servoModeY = core_.getProperty(xystage_, "ServoModeY")
					.equals("True");
			boolean servoModeZ = core_.getProperty(zstage_, "ServoModeZ")
					.equals("True");

			short paralen = 8;

			Object[] data = new Object[] { CMD, paraNum, paralen, servoModeX,
					paralen, servoModeY, paralen, servoModeZ };
			int len = packAnalyzer.packData(data, rawData);
			OutputStream outPutSteam = socket.getOutputStream();
			outPutSteam.write(rawData, 0, len);
			outPutSteam.flush();
			// outPutSteam.close();

		} catch (Exception e) {
			setErrCode("get StagePosition ERR");
			e.printStackTrace();
		}

	}

	private void QueryPLM(Socket socket) {
		try {
			byte[] rawData = new byte[BUFFER_SIZE];
			byte CMD = (byte) ECMDLIST.QPLM.ordinal();
			short paraNum = 3;
			// PARA start
			float xAxis = 100;
			float yAxis = 100;
			float zAxis = 10;

			short paralen = 4;

			Object[] data = new Object[] { CMD, paraNum, paralen, xAxis,
					paralen, yAxis, paralen, zAxis };
			int len = packAnalyzer.packData(data, rawData);
			OutputStream outPutSteam = socket.getOutputStream();
			outPutSteam.write(rawData, 0, len);
			outPutSteam.flush();
			// outPutSteam.close();

		} catch (Exception e) {
			setErrCode("QueryPLM ERR");
			e.printStackTrace();
		}

	}

	private void QueryNLM(Socket socket) {
		try {
			byte[] rawData = new byte[BUFFER_SIZE];
			byte CMD = (byte) ECMDLIST.QNLM.ordinal();
			short paraNum = 3;
			// PARA start
			float xAxis = 0;
			float yAxis = 0;
			float zAxis = 0;

			short paralen = 4;

			Object[] data = new Object[] { CMD, paraNum, paralen, xAxis,
					paralen, yAxis, paralen, zAxis };
			int len = packAnalyzer.packData(data, rawData);
			OutputStream outPutSteam = socket.getOutputStream();
			outPutSteam.write(rawData, 0, len);
			outPutSteam.flush();
			// outPutSteam.close();

		} catch (Exception e) {
			setErrCode("QueryPLM ERR");
			e.printStackTrace();
		}

	}

	/**
	 * @param para
	 * @param flag
	 *            : true set absPosition,false:set rePostition
	 * @throws Exception
	 */
	private void setPositions(Object[] para, boolean flag) throws Exception {
		if (para == null) {
			setErrCode("paraNum undefined!");
			return;
		}

		HashMap<Integer, Object> paraMap = extractPara(para);

		Integer xAxis = Integer.valueOf(0);
		Integer yAxis = Integer.valueOf(1);
		Integer zAxis = Integer.valueOf(2);
		if (paraMap.containsKey(zAxis)) {
			double pos = ((Number) paraMap.get(zAxis)).doubleValue();
			if (flag)
				core_.setPosition(zstage_, pos);
			else
				core_.setRelativePosition(zstage_, pos);
		}

		int bm = 0;
		bm += paraMap.containsKey(xAxis) ? 1 : 0;
		bm += paraMap.containsKey(yAxis) ? 2 : 0;
		if (bm == 0)
			return;

		double[] xpos = new double[] { 0 };
		double[] ypos = new double[] { 0 };
		if (bm != 3 && flag)
			core_.getXYPosition(xystage_, xpos, ypos);
		if ((bm & 1) != 0)
			xpos[0] = ((Number) paraMap.get(xAxis)).doubleValue();
		if ((bm & 2) != 0)
			ypos[0] = ((Number) paraMap.get(yAxis)).doubleValue();

		if (flag)
			core_.setXYPosition(xystage_, xpos[0], ypos[0]);
		else
			core_.setRelativeXYPosition(xystage_, xpos[0], ypos[0]);

		// switch (para.length) {
		// case 4:
		// try {
		// setPosition(para[1], para[3], flag);
		// } catch (Exception e) {
		// // TODO Auto-generated catch block
		// setErrCode("setPosition ERR!");
		// e.printStackTrace();
		// }
		// break;
		// case 8:
		// try {
		// setPosition(para[1], para[3], flag);
		// setPosition(para[5], para[7], flag);
		// } catch (Exception e) {
		// // TODO Auto-generated catch block
		// setErrCode("setPosition ERR!");
		// e.printStackTrace();
		// }
		// break;
		// case 12:
		// try {
		// setPosition(para[1], para[3], flag);
		// setPosition(para[5], para[7], flag);
		// setPosition(para[9], para[11], flag);
		// } catch (Exception e) {
		// // TODO Auto-generated catch block
		// setErrCode("setPosition ERR!");
		// e.printStackTrace();
		// }
		// break;
		// }

	}

	private HashMap<Integer, Object> extractPara(Object[] para) {
		// Extract axis
		HashMap<Integer, Object> paraMap = new HashMap<Integer, Object>();
		for (int i = 1; i < para.length; i += 4)
			paraMap.put(Integer.valueOf((int) para[i]), para[i + 2]);
		return paraMap;
	}

	private void setErrCode(String string) {
		// TODO Auto-generated method stub

	}

	private void SetCST(Object[] unpackParas) {
		// TODO Auto-generated method stub

	}

	private void QueryCST(Socket socket) {// watch out
		try {
			byte[] rawData = new byte[BUFFER_SIZE];
			byte CMD = (byte) ECMDLIST.QCST.ordinal();
			short paraNum = 3;

			boolean servoModeX = true;// core_.getProperty(xystage_, "QCSTX");
			boolean servoModeY = true;// core_.getProperty(xystage_, "QCSTY");
			boolean servoModeZ = true;// core_.getProperty(zstage_, "QCSTZ");

			short paralen = 8;

			Object[] data = new Object[] { CMD, paraNum, paralen, servoModeX,
					paralen, servoModeY, paralen, servoModeZ };
			int len = packAnalyzer.packData(data, rawData);
			OutputStream outPutSteam = socket.getOutputStream();
			outPutSteam.write(rawData, 0, len);
			outPutSteam.flush();
			// outPutSteam.close();

		} catch (Exception e) {
			setErrCode("get StagePosition ERR");
			e.printStackTrace();
		}

	}

	public boolean isRunning() {
		return isRunning_;
	}

}