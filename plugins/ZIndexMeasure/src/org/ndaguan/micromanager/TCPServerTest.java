package org.ndaguan.micromanager;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class TCPServerTest {
	private static PackageAnalyzer packAnalyzer;
	private static int port_ = 50501;
	private static byte[] rawData;
	private static int BUFFER_SIZE = 1024;
	private static int cmdListLen = 13;

	public enum ECMDLIST {
		QPOS, MOV, MVR, SVO, QSVO, CST, QCST, SVA, QSVA, PLM, QPLM, NLM, QNLM
	};

	public static void main(String[] args) {
		packAnalyzer = new PackageAnalyzer();
		rawData = new byte[BUFFER_SIZE];
		byte CMD = -1;
		try {
			Socket s = new Socket("127.0.0.1", port_);
			OutputStream outStream = s.getOutputStream();
			short paraNum =6;

			short para1len = 1;//para1
			byte para1 = 0;

			short para2len = 1;//para2
			byte para2 = 10;
			boolean bpara2 = true;

			short para3len = 1;//para3
			byte para3 = 1;

			short para4len = 1;//para4
			byte para4 = 8;
			boolean bpara4 = false;

			short para5len = 1;//para5
			byte para5 = 2;

			short para6len = 1;//para6
			byte para6 = 6;
			boolean bpara6 = true;

			for (int i = 0; i < cmdListLen; i++) {
				System.out.print("\r\n"+i);
				CMD = (byte)i;
				Object[] data = null;

				if(CMD == (byte) ECMDLIST.SVO.ordinal()){
					data= new Object[] { CMD, paraNum, para1len, para1,
							para2len, bpara2, para3len, para3, para4len, bpara4,
							para5len, para5, para6len, bpara6 };}
				else{
					data = new Object[] { CMD, paraNum, para1len, para1,
							para2len, para2, para3len, para3, para4len, para4,
							para5len, para5, para6len, para6 };
				}

				int len = packAnalyzer.packData(data, rawData);
				outStream.write(rawData, 0, len);
				outStream.flush();

				if(CMD == ECMDLIST.MOV.ordinal())
					continue;
				if(CMD == ECMDLIST.MVR.ordinal())
					continue;
				if(CMD == ECMDLIST.NLM.ordinal())
					continue;
				if(CMD == ECMDLIST.PLM.ordinal())
					continue;
				if(CMD == ECMDLIST.CST.ordinal())
					continue;
				if(CMD == ECMDLIST.SVA.ordinal())
					continue;
				if(CMD == ECMDLIST.SVO.ordinal())
					continue;


				int[] offset = new int[1];
				offset[0] = 0;

				InputStream inStream = s.getInputStream();
				Arrays.fill(rawData, (byte) 0);
				ByteBuffer buffer = ByteBuffer.wrap(rawData);
				int cnt = inStream.read(rawData, offset [0], 2);
				if (cnt == 0)
					return;
				if (!packAnalyzer.checkStart(rawData, offset)) {
					offset[0] += 2;
					return;
				}
				offset[0] += 2;
				// Frame length
				inStream.read(rawData, offset[0], 2);
				short length = buffer.getShort(offset[0]);
				offset[0] += 2;
				// checksum
				inStream.read(rawData, offset[0], length - offset[0]);
				if (!packAnalyzer.checksum(rawData, length))
					return;
				phaseData(rawData, length, offset);

			}
			s.close();
			TimeUnit.HOURS.sleep(24);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private static void phaseData( byte[] rawData, short length,
			int[] offset) throws Exception {
		// DATA FORMAT:
		// START_FLAG, DATA_LEN, CMD_FLAG, PARA_NUM, [PARAS], MD5
		// 0XFFFE(16bit-short),(8bit-short),(8bit-char),(8bit-byte),(random-bit,random-type),(16bit-byte);
		//		if(true)
		//		return;
		byte CMD = rawData[offset[0]];
		offset[0]++;
		ECMDLIST val = ECMDLIST.values()[CMD];
		Object[] ret  = null;
		String[] axisName = new String[]{"X","Y","Z"};
		switch (val) {

		case QPOS:
			ret = packAnalyzer.unpackParas(rawData, offset);
			for (int i = 0; i <(int) ret.length/2; i++) {
				System.out.print(String.format("\r\nGetPosition of axis\t%s\tvalue\t%f", axisName[i],ret[i*2+1]) );
			}
			break;
		case QSVA:
			ret = packAnalyzer.unpackParas(rawData, offset);
			for (int i = 0; i <(int) ret.length/2; i++) {
				System.out.print(String.format("\r\nGetOpenLoopValue at axis \t%s\tvalue\t%f", axisName[i],ret[i*2+1]) );
			}
			break;
		case QSVO:
			ret = packAnalyzer.unpackParas(rawData, offset);
			for (int i = 0; i <(int) ret.length/2; i++) {
				System.out.print(String.format("\r\nGetServoMode at axis \t%s\tvalue\t%s", axisName[i],(boolean) ret[i*2+1]?"True":"False") );
			}
			break;
		case QPLM:
			ret = packAnalyzer.unpackParas(rawData, offset);
			for (int i = 0; i <(int) ret.length/2; i++) {
				System.out.print(String.format("\r\nGetPLM of axis\t%s\tvalue\t%f", axisName[i],ret[i*2+1]) );
			}
			break;
		case QNLM:
			ret = packAnalyzer.unpackParas(rawData, offset);
			for (int i = 0; i <(int) ret.length/2; i++) {
				System.out.print(String.format("\r\nGetNLM of axis\t%s\tvalue\t%f", axisName[i],ret[i*2+1]) );
			}
			break;
		case QCST:
			ret = packAnalyzer.unpackParas(rawData, offset);
			for (int i = 0; i <(int) ret.length/2; i++) {
				System.out.print(String.format("\r\nGetCST of axis\t%s\tvalue\t%f", axisName[i],ret[i*2+1]) );
			}
			break;
		case MOV:			
			break;
		case MVR:
			break;		
		case SVA:
			break;
		case SVO:
			break;
		case CST:
			break;
		default:
			break;
		}

	}

}
