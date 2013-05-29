package org.ndaguan.micromanager.mmtracker;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
 
/**
 * @author Administrator
 *PackageAnalyzer.java
 *analyze the TCP/IP package to drive the pizeo
 */
public class PackageAnalyzer {
	private final short START_FLAG = (short) 0xFFFE;
	private static HashMap<Class<?>, Byte> typeMap_;
	static {
		typeMap_ = new HashMap<Class<?>, Byte>();
		typeMap_.put(Boolean.class, (byte) '?');
		typeMap_.put(Byte.class, (byte) 'c');
		typeMap_.put(Short.class, (byte) 'H');
		typeMap_.put(Integer.class, (byte) 'I');
		typeMap_.put(Float.class, (byte) 'f');
		typeMap_.put(Double.class, (byte) 'd');
		typeMap_.put(String.class, (byte) 's');
	}

	/**
	 * @param rawData
	 * @param offset
	 * @return Object[para1.len,para1,para2.len,para2,etc.]
	 * @throws IOException
	 */
	public Object[] unpackParas(byte[] rawData, int[] offset) {

		byte paraNum = rawData[offset[0]];
		offset[0]++;
		if (paraNum < 1)
			return null;

		ByteBuffer buffer = ByteBuffer.wrap(rawData);
		Object ret[] = new Object[paraNum * 2];

		for (int i = 0; i < paraNum; i++) {
			Object[] temp = unpackPara(buffer, rawData, offset);
			ret[i * 2] = temp[0];
			ret[i * 2 + 1] = temp[1];
		}
		return ret;
	}

	private Object[] unpackPara(ByteBuffer buffer, byte[] rawData, int[] offset) {
		// PARA FORMAT:
		// START_FLAG, TYPE, LEN, [DATA]
		// OX00(8bit-byte),(8bit-char),(16bit-short),(define as LEN and TYPE)

		Object[] ret = new Object[2];
		offset[0]++;// Header
		char paraType = (char) rawData[offset[0]];
		offset[0]++;
		short paraLen = buffer.getShort(offset[0]);
		ret[0] = paraLen;
		offset[0] += 2;
		switch (paraType) {
		case '?':
			ret[1] = false;
			if (rawData[offset[0]] == (byte) 1)
				ret[1] = true;
			offset[0]++;
			break;
		case 'c':
			ret[1] = rawData[offset[0]];
			offset[0]++;
			break;
		case 'H':
			ret[1] = buffer.getShort(offset[0]);
			offset[0] += 2;
			break;
		case 'I':
			ret[1] = buffer.getInt(offset[0]);
			offset[0] += 4;
			break;
		case 'f':
			ret[1] = buffer.getFloat(offset[0]);
			offset[0] += 4;
			break;
		case 'd':
			ret[1] = buffer.getDouble(offset[0]);
			offset[0] += 8;
			break;
		default:
			ret[1] = -1;
			offset[0] += paraLen;
			break;
		}
		return ret;
	}

	public int packData(Object[] data, byte[] rawData) {
		ByteBuffer buffer = ByteBuffer.wrap(rawData);
		int offset = 0;

		// ------------------------------------------------HeaderStart
		buffer.putShort(START_FLAG);// start*2
		offset += 2;
		buffer.putShort((short) 0);// DataLen*2 set 0 first
		offset += 2;
		// CMD
		buffer.put(Byte.parseByte(data[0].toString()));
		offset++;
		// num para
		buffer.put(Byte.parseByte(data[1].toString()));
		offset++;

		// ------------------------------------------------paraStart
		for (int i = 0; i < Integer.parseInt(data[1].toString()); i++) {
			offset = packPara(i, data, rawData, buffer, offset);
		}
		buffer.putShort(2, (short) (offset + 16));// DataLen*2 set 0 first
		byte[] md5 = MD5(rawData, 0, offset);
		System.arraycopy(md5, 0, rawData, offset, md5.length);
		return (int) (offset + md5.length);
	}

	private int packPara(int i, Object[] data, byte[] rawData,
			ByteBuffer buffer, int offset) {
		buffer.put((byte) 0);
		offset++;

		Byte paraType = typeMap_.get(data[2 + i * 2 + 1].getClass());
		buffer.put(paraType);
		offset++;

		buffer.putShort(Short.parseShort(data[2 + i * 2].toString()));// ParaLen*2
		offset += 2;

		if (paraType == (byte) '?') {
			buffer.put(data[2 + i * 2 + 1].toString().equals("true") ? (byte) 1
					: (byte) 0);
			offset++;
		} else if (paraType == (byte) 'c') {
			buffer.put(Byte.parseByte(data[2 + i * 2 + 1].toString()));
			offset++;
		} else if (paraType == (byte) 'H') {
			buffer.putShort(Short.parseShort(data[2 + i * 2 + 1].toString()));// PARA*2
			offset += 2;
		} else if (paraType == (byte) 'I') {
			buffer.putInt(Integer.parseInt(data[2 + i * 2 + 1].toString()));// PARA*4
			offset += 4;
		} else if (paraType == (byte) 'f') {
			buffer.putFloat(Float.parseFloat(data[2 + i * 2 + 1].toString()));// PARA*4
			offset += 4;
		} else if (paraType == (byte) 'd') {
			buffer.putDouble(Double.parseDouble(data[2 + i * 2 + 1].toString()));// PARA*8
			offset += 8;
		} else if (paraType == (byte) 's') {
			byte[] strBuf = ((String) data[2 + i * 2 + 1]).getBytes();
			buffer.put(strBuf);
			offset += strBuf.length;
		}
		return offset;
	}

	public final byte[] MD5(byte[] buff, int offset, int len) {

		try {
			byte[] btInput = new byte[len];

			System.arraycopy(buff, offset, btInput, 0, len);
			MessageDigest mdInst = MessageDigest.getInstance("MD5");
			mdInst.update(btInput);
			return mdInst.digest();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public boolean checkStart(byte[] bf, int[] offset) {
		ByteBuffer buffer = ByteBuffer.wrap(bf);
		if (buffer.getShort(offset[0]) != START_FLAG) {
			return false;
		}
		return true;

	}

	public boolean checksum(byte[] bf, int len) {
		byte[] orignCS = new byte[16];
		System.arraycopy(bf, len - 16, orignCS, 0, 16);
		return (Arrays.equals(orignCS, MD5(bf, 0, len - 16)));
	}
}
