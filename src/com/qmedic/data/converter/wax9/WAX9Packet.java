package com.qmedic.data.converter.wax9;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * The WAX9 packet
 * @see http://axivity.com/files/resources/WAX9_Developer_Guide_3.pdf, pg. 7, Binary Stream
 */
public class WAX9Packet {
	static final byte STANDARD_FORMAT = 0x01;
	static final byte EXTENDED_FORMAT = 0x02;
	
	static final int STANDARD_PACKET_SIZE = 28;
	static final int EXTENDED_PACKET_SIZE = 36;
	
	private static final int TIMESTAMP_SCALE_FACTOR = 65536; 
	
	/**
	 * Whether if the packet is in standard or extended format
	 */
	public final byte format;
	
	/**
	 * The size of the WAX9 packet 
	 */
	public final int packetSize;
	
	/**
	 *  The unsigned 16-bit sample number (stored in 32-bits)
	 */
	public final int sampleNumber;
	
	/**
	 * The unsigned 32-bit timestamp of the sample (stored in 64-bits) 
	 * NOTE: Timestamp is in 1/65536 of a second
	 */
	public final long rawTimestamp;
	
	/**
	 * The timestamp in milliseconds
	 */
	public final long timestamp;
	
	/**
	 * The x acceleratometer value
	 */
	public final short accelX;
	
	/**
	 * The y acceleratometer value
	 */
	public final short accelY;
	
	/**
	 * The z acceleratometer value
	 */
	public final short accelZ;
	
	/**
	 * The x gyroscope value
	 */
	public final short gyroX;
	
	/**
	 * The y gyroscope value
	 */
	public final short gyroY;
	
	/**
	 * The z gyroscope value
	 */
	public final short gyroZ;
	
	/** 
	 * The x magenetoscope value
	 */
	public final short magnetX;
	
	/** 
	 * The y magenetoscope value
	 */
	public final short magnetY;
	
	/** 
	 * The z magenetoscope value
	 */
	public final short magnetZ;
	
	/**
	 * The unsigned 16-bit battery voltage in millivolts (stored in 32-bits)
	 */
	public Integer volts;
	
	/**
	 * Signed temperature in 0.1 degree step
	 */
	public Short temperature;
	
	/**
	 * Unsigned barometric pressure in pascals (stored in 64-bits)
	 */
	public Long pressure;
	
	public WAX9Packet(byte[] bytes, boolean isExtended) throws IOException {
		format = bytes[2];
		switch (format) {
			case STANDARD_FORMAT:
				if (isExtended) throw new IOException("Expected extended WAX9 packet, but got standard size.");
				
				packetSize = STANDARD_PACKET_SIZE;
				volts = null;
				temperature = null;
				pressure = null;
				break;
				
			case EXTENDED_FORMAT:
				if (!isExtended) throw new IOException("Expected standard WAX9 packet, but got extended size.");
				
				packetSize = EXTENDED_PACKET_SIZE;
				volts = getIntFromBytes(bytes, 27, true);
				temperature = getShortFromBytes(bytes, 29);
				pressure = getLongFromBytes(bytes, 31, true);
				break;
				
			default:
				packetSize = -1;
				break;
		}
		
		sampleNumber = getIntFromBytes(bytes, 3, true);
		
		rawTimestamp = getLongFromBytes(bytes, 5, true);
		timestamp = (rawTimestamp / TIMESTAMP_SCALE_FACTOR) * 1000;
		
		accelX = getShortFromBytes(bytes, 9);
		accelY = getShortFromBytes(bytes, 11);
		accelZ = getShortFromBytes(bytes, 13);
		gyroX = getShortFromBytes(bytes, 15);
		gyroY = getShortFromBytes(bytes, 17);
		gyroZ = getShortFromBytes(bytes, 19);
		magnetX = getShortFromBytes(bytes, 21);
		magnetY = getShortFromBytes(bytes, 23);
		magnetZ = getShortFromBytes(bytes, 25);
	}
		
	private static short getShortFromBytes(byte[] bytes, int offset) {
		return getValueFromBytes(short.class, bytes, offset).shortValue();
	}
	
	private static int getIntFromBytes(byte[] bytes, int offset) {
		return getIntFromBytes(bytes, offset, false);
	}
	
	private static int getIntFromBytes(byte[] bytes, int offset, boolean upscaleFromShort)
	{
		if (!upscaleFromShort) return getValueFromBytes(int.class, bytes, offset).intValue();
		
		short val = getShortFromBytes(bytes, offset);
		return val & 0xffff;
	}
	
	private static long getLongFromBytes(byte[] bytes, int offset) {
		return getLongFromBytes(bytes, offset);
	}
	
	private static long getLongFromBytes(byte[] bytes, int offset, boolean upscaleFromInt) {
		if (!upscaleFromInt) return getValueFromBytes(long.class, bytes, offset).longValue();
		
		int val = getIntFromBytes(bytes, offset);
		return val & 0x00000000ffffffffL;
	}
	
	private static Number getValueFromBytes(Class<? extends Number> type, byte[] bytes, int offset)
	{
		int numBytes = 1;
		if (short.class.equals(type)) 	 numBytes = 2;
		else if (int.class.equals(type)) numBytes = 4;
		else if (int.class.equals(type)) numBytes = 8;
		else throw new UnsupportedOperationException("Must define operation for " + type.getName());
		
		byte[] payload = new byte[numBytes];
		for (int i = 0; i < numBytes; i++) {
			payload[i] = bytes[offset + i];
		}
		
		ByteBuffer bb = ByteBuffer.wrap(payload);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		
		Number num = null;
		if (short.class.equals(type)) {
			num = bb.getShort();
		} else if (int.class.equals(type)) {
			num = bb.getInt();
		} else if (long.class.equals(type)) {
			num = bb.getLong();
		}
		
		return num;
	}
			
}
