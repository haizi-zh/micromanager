package org.ndaguan.micromanager.mmtracker;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
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
class TCPServer {
	private static TCPServer instance_;

	private boolean isRunning_ = false;

	private int port_;
	private ExecutorService exec_;
	private final int BUFFER_SIZE = 1024;

	private String zstage_;
	private String xystage_;
	private CMMCore core_;
	private PackageAnalyzer packAnalyzer;

	private String LastErr = null;

	public enum ECMDLIST {
		QPOS, MOV, MVR, SVO, QSVO, CST, QCST, SVA, QSVA, PLM, QPLM, NLM, QNLM
	};

	public static void main(String[] argv) throws Exception {
		CMMCore core = new CMMCore();
		TCPServer tcp = new TCPServer(core, 50501);
		tcp.start();
		TimeUnit.MILLISECONDS.sleep(1000000);

	}

	public static TCPServer getInstance(CMMCore core, int port) {
		if(instance_ == null)
			instance_ = new TCPServer(core, port);
		return instance_;
	}
	public static TCPServer getInstance() {
		
		return instance_;
	}
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
		ServerSocket svr = null;
		try {
			svr = new ServerSocket(port_);
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
			try {
				svr.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
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
				if (socket.isClosed()) {
					core_.logMessage("Socket closed");
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
			socket.close();
		} catch (IOException e) {
			return;
		}

		core_.logMessage("Socket thread exited.");
	}

	private void phaseData(Socket socket, byte[] rawData, short length,
			int[] offset) throws IOException {
		// DATA FORMAT:
		// START_FLAG, DATA_LEN, CMD_FLAG, PARA_NUM, [PARAS], MD5
		// 0XFFFE(16bit-short),(8bit-short),(8bit-char),(8bit-byte),(random-bit,random-type),(16bit-byte);
		byte CMD = rawData[offset[0]];
		offset[0]++;
		ECMDLIST val = ECMDLIST.values()[CMD];
		switch (val) {
		case QPOS:
			QueryPosition(socket);
			break;
		case MOV:
			setPositions(packAnalyzer.unpackParas(rawData, offset), true,
					socket);
			break;
		case MVR:
			setPositions(packAnalyzer.unpackParas(rawData, offset), false,
					socket);
			break;
		case SVO:
			setServoMode(packAnalyzer.unpackParas(rawData, offset), socket);
			break;
		case QSVO:
			QueryServoMode(socket);
			break;
		case CST:
			SetCST(packAnalyzer.unpackParas(rawData, offset), socket);
			break;
		case QCST:
			QueryCST(socket);
			break;
		case SVA:
			setOpenLoopValue(packAnalyzer.unpackParas(rawData, offset), socket);
			break;
		case QSVA:
			queryOpenLoopValue(socket);
			break;

		case PLM:
			setPLM(packAnalyzer.unpackParas(rawData, offset), socket);
			break;
		case QPLM:
			QueryPLM(socket);
			break;
		case NLM:
			setNLM(packAnalyzer.unpackParas(rawData, offset), socket);
			break;
		case QNLM:
			QueryNLM(socket);
			break;

		default:
			break;
		}

	}

	private void QueryPosition(Socket socket) throws IOException {
		byte CMD = (byte) ECMDLIST.QPOS.ordinal();
		short paraNum = 3;
		try {
			double currxpos_ = core_.getXPosition(xystage_);
			double currypos_ = core_.getYPosition(xystage_);
			double currzpos_ = core_.getPosition(zstage_);

			short paralen = 8;
			respond(socket, new Object[] { CMD, paraNum + 1, 1, true, paralen,
					currxpos_, paralen, currypos_, paralen, currzpos_ });
		} catch (Exception e) {
			String msg = e.getMessage();
			respond(socket,
					new Object[] { CMD, 2, 1, false, msg.length(), msg });
		}

	}

	private void setOpenLoopValue(Object[] para, Socket socket)
			throws IOException {
		if (para == null) {
			setLastErr("paraNum undefined!");
			return;
		}

		HashMap<Integer, Object> paraMap = extractPara(para);
		String[] props = new String[] { "X", "Y", "Z" };
		for (int i = 0; i < props.length; i++)
			props[i] = "OpenLoopValue" + props[i];

		try {
			for (Integer axis : paraMap.keySet()) {
				String label = (axis == 2) ? zstage_ : xystage_;
				core_.setProperty(label, props[axis],
						(paraMap.get(axis)).toString());
			}
			respond(socket, new Object[] { (byte) ECMDLIST.SVA.ordinal(), 1, 1,
					true });
		} catch (Exception e) {
			String msg = e.getMessage();
			respond(socket, new Object[] { (byte) ECMDLIST.SVA.ordinal(), 2, 1,
					false, msg.length(), msg });
		}
	}

	private void queryOpenLoopValue(Socket socket) throws IOException {
		byte CMD = (byte) ECMDLIST.QSVA.ordinal();
		short paraNum = 3;

		try {
			double openLoopValueX = Double.parseDouble(core_.getProperty(
					xystage_, "OpenLoopValueX"));
			double openLoopValueY = Double.parseDouble(core_.getProperty(
					xystage_, "OpenLoopValueY"));
			double openLoopValueZ = Double.parseDouble(core_.getProperty(
					zstage_, "OpenLoopValueZ"));

			short paralen = 8;

			respond(socket, new Object[] { CMD, paraNum + 1, 1, true, paralen,
					openLoopValueX, paralen, openLoopValueY, paralen,
					openLoopValueZ });
		} catch (Exception e) {
			String msg = e.getMessage();
			respond(socket,
					new Object[] { CMD, 2, 1, false, msg.length(), msg });
		}

	}

	private void setServoMode(Object[] para, Socket socket) throws IOException {
		if (para == null) {
			setLastErr("paraNum undefined!");
			return;
		}

		HashMap<Integer, Object> paraMap = extractPara(para);
		String[] props = new String[] { "X", "Y", "Z" };
		for (int i = 0; i < props.length; i++)
			props[i] = "ServoMode" + props[i];
		try {
			for (Integer axis : paraMap.keySet()) {
				String label = (axis == 2) ? zstage_ : xystage_;
				core_.setProperty(label, props[axis],
						(Boolean) paraMap.get(axis) ? "True" : "False");
			}
			respond(socket, new Object[] { (byte) ECMDLIST.SVO.ordinal(), 1, 1,
					true });
		} catch (Exception e) {
			String msg = e.getMessage();
			respond(socket, new Object[] { (byte) ECMDLIST.SVO.ordinal(), 2, 1,
					false, msg.length(), msg });
		}

	}

	private void QueryServoMode(Socket socket) throws IOException {
		byte CMD = (byte) ECMDLIST.QSVO.ordinal();
		short paraNum = 3;
		try {

			boolean servoModeX = core_.getProperty(xystage_, "ServoModeX")
					.equals("True");
			boolean servoModeY = core_.getProperty(xystage_, "ServoModeY")
					.equals("True");
			boolean servoModeZ = core_.getProperty(zstage_, "ServoModeZ")
					.equals("True");

			short paralen = 1;

			respond(socket, new Object[] { CMD, paraNum + 1, 1, true, paralen,
					servoModeX, paralen, servoModeY, paralen, servoModeZ });
		} catch (Exception e) {
			String msg = e.getMessage();
			respond(socket,
					new Object[] { CMD, 2, 1, false, msg.length(), msg });
		}

	}

	private void QueryPLM(Socket socket) throws IOException {
		byte CMD = (byte) ECMDLIST.QPLM.ordinal();
		short paraNum = 3;
		try {
			float xAxis = 100;
			float yAxis = 100;
			float zAxis = 10;

			short paralen = 4;

			respond(socket, new Object[] { CMD, paraNum + 1, 1, true, paralen,
					xAxis, paralen, yAxis, paralen, zAxis });
		} catch (Exception e) {
			String msg = e.getMessage();
			respond(socket,
					new Object[] { CMD, 2, 1, false, msg.length(), msg });
		}
	}

	private void QueryNLM(Socket socket) throws IOException {
		byte CMD = (byte) ECMDLIST.QNLM.ordinal();
		short paraNum = 3;
		try {
			float xAxis = 0;
			float yAxis = 0;
			float zAxis = 0;

			short paralen = 4;

			respond(socket, new Object[] { CMD, paraNum + 1, 1, true, paralen,
					xAxis, paralen, yAxis, paralen, zAxis });

		} catch (Exception e) {
			String msg = e.getMessage();
			respond(socket,
					new Object[] { CMD, 2, 1, false, msg.length(), msg });
		}

	}

	private void respond(Socket socket, Object[] data) throws IOException {
		byte[] rawData = new byte[BUFFER_SIZE];
		int len = packAnalyzer.packData(data, rawData);
		OutputStream outPutSteam = socket.getOutputStream();
		outPutSteam.write(rawData, 0, len);
		outPutSteam.flush();
	}

	/**
	 * @param para
	 * @param flag
	 *            : true set absPosition,false:set rePostition
	 * @throws IOException
	 */
	private void setPositions(Object[] para, boolean flag, Socket socket)
			throws IOException {
		if (para == null) {
			setLastErr("paraNum undefined!");
			return;
		}

		HashMap<Integer, Object> paraMap = extractPara(para);

		Integer xAxis = Integer.valueOf(0);
		Integer yAxis = Integer.valueOf(1);
		Integer zAxis = Integer.valueOf(2);
		byte CMD = (byte) (flag ? ECMDLIST.MOV : ECMDLIST.MVR).ordinal();
		try {
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
			if (bm == 0) {
				respond(socket, new Object[] { CMD, 1, 1, true });
				return;
			}

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

			respond(socket, new Object[] { CMD, 1, 1, true });
		} catch (Exception e) {
			String msg = e.getMessage();
			respond(socket,
					new Object[] { CMD, 2, 1, false, msg.length(), msg });
		}
	}

	private HashMap<Integer, Object> extractPara(Object[] para) {
		// Extract axis
		HashMap<Integer, Object> paraMap = new HashMap<Integer, Object>();
		for (int i = 1; i < para.length; i += 4)
			paraMap.put(((Number) para[i]).intValue(), para[i + 2]);
		return paraMap;
	}

	private void SetCST(Object[] unpackParas, Socket socket) throws IOException {
		respond(socket, new Object[] { (byte) ECMDLIST.CST.ordinal(), 1, 1,
				true, });
	}

	private void QueryCST(Socket socket) throws IOException {// watch out
		byte CMD = (byte) ECMDLIST.QCST.ordinal();
		short paraNum = 3;
		try {
			double servoModeX = 100;// Double.parseDouble(core_.getProperty(xystage_,
			// "QCSTX"));
			double servoModeY = 100;// Double.parseDouble(core_.getProperty(xystage_,
			// "QCSTY"));
			double servoModeZ = 100;// Double.parseDouble(core_.getProperty(zstage_,
			// "QCSTZ"));
			short paralen = 8;
			respond(socket, new Object[] { CMD, paraNum + 1, 1, true, paralen,
					servoModeX, paralen, servoModeY, paralen, servoModeZ });
		} catch (Exception e) {
			String msg = e.getMessage();
			respond(socket,
					new Object[] { CMD, 2, 1, false, msg.length(), msg });
		}
	}

	private void setNLM(Object[] unpackParas, Socket socket) throws IOException {
		respond(socket, new Object[] { (byte) ECMDLIST.CST.ordinal(), 1, 1,
				true, });
	}

	private void setPLM(Object[] unpackParas, Socket socket) throws IOException {
		respond(socket, new Object[] { (byte) ECMDLIST.CST.ordinal(), 1, 1,
				true, });
	}

	public boolean isRunning() {
		return isRunning_;
	}

	public String getLastErr() {
		return LastErr;
	}

	public void setLastErr(String errCode) {
		LastErr = errCode;
	}


}