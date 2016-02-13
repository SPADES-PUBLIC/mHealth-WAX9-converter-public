package com.qmedic.data.converter.wax9;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.accessibility.AccessibleExtendedText;
import javax.activity.ActivityCompletedException;

/**
 * The WAX9 Settings associated with the binary file
 * @see http://axivity.com/files/resources/WAX9_Developer_Guide_3.pdf, p.9-10
 */
public class WAX9Settings {
	/**
	 * The raw bytes that creates the rawMetadata
	 */
	private byte[] rawBytes = null;
	
	/**
	 * The raw timestamp (in milliseconds)
	 */
	public final long rawTimestamp;
	
	/**
	 * The start timestamp of the WAX9 File
	 */
	public final Date timestamp;
	
	/**
	 * The string representation of the rawBytes
	 */
	private String rawMetadata = null;
	
	/**
	 * The hardware version
	 */
	private String hardwareVersion = null;
	
	/**
	 * The firmware version
	 */
	private String firmwareVersion = null;
	
	/**
	 * The chipset of the WAX9 device
	 */
	private String chipset = null;
	
	/**
	 * The device ID
	 */
	private String deviceID = null;
	public String getDeviceID() { return deviceID; }
	
	/**
	 * The device MAC address
	 */
	private String mac = null;
	
	/**
	 * The device name
	 */
	private String name = null;
	
	/**
	 * The accelerometer active state
	 */
	private Boolean accelEnabled = null;
	
	/**
	 * The accelerometer sampling rate (Hz)
	 */
	private Integer accelerometerRate = null;
	
	/**
	 * The accelerometer range (g)
	 */
	private Integer accelerometerRange = null;
	
	/**
	 * The gyroscope active state
	 */
	private Boolean gyroEnabled = null;
	
	/**
	 * The gyroscope sampling rate (Hz)
	 */
	private Integer gyroscopeRate = null;
	
	/**
	 * The gyroscope rang (dps)
	 */
	private Integer gyroscopeRange = null;
	
	/**
	 * The magnetometer active state
	 */
	private Boolean magEnabled = null;
	
	/**
	 * The magnetometer rate (Hz)
	 */
	private Integer magnetometerRange = null;
	
	/**
	 * The output data rate (Hz)
	 */
	private Integer outputDataRate = null;
	
	/**
	 * The output data mode
	 */
	private String outputDataMode = null;
	
	/**
	 * The sleep mode setting
	 */
	private Integer sleepModeSetting = null;
	
	/**
	 * The inactivity timeout value(s)
	 */
	private String inactivityTimeoutValue = null;
	
	public WAX9Settings(final byte[] bytes) {
		rawBytes = bytes;
		rawMetadata = new String(bytes, StandardCharsets.UTF_8);
		
		String[] lines = rawMetadata.split("\n");
		rawTimestamp = Long.parseUnsignedLong(lines[0].trim());
		timestamp = Date.from(Instant.ofEpochMilli(rawTimestamp));

		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			if (line.startsWith("WAX9")) {
				String[] deviceParts = extractString(line, "WAX9,").split(",");
				hardwareVersion = extractString(deviceParts[0], "HW:");
				firmwareVersion = extractString(deviceParts[1], "FW:");
				chipset = extractString(deviceParts[2], "CS:");
				continue;
			}
			
			if (line.startsWith("ID")) {
				deviceID = extractString(line, "ID:");
				continue;
			}
			
			if (line.startsWith("NAME")) {
				String rawName = extractString(line, "NAME:");
				int index = rawName.indexOf(",");
				name = rawName.substring(0, index);
				continue;
			}			
			
			if (line.startsWith("MAC")) {
				mac = extractString(line, "MAC:");
				continue;
			}
			
			if (line.startsWith("ACCEL")) {
				String[] accelParts = extractString(line, "ACCEL:").split(",");
				accelEnabled = accelParts[0].trim().equals("1");
				accelerometerRate = Integer.parseInt(accelParts[1].trim());
				accelerometerRange = Integer.parseInt(accelParts[2].trim());
				continue;
			}
			
			if (line.startsWith("GYRO")) {
				String[] gyroParts = extractString(line, "GYRO:").split(",");
				gyroEnabled = gyroParts[0].trim().equals("1");
				gyroscopeRate = Integer.parseInt(gyroParts[1].trim());
				gyroscopeRange = Integer.parseInt(gyroParts[2].trim());
				continue;
			}
			
			if (line.startsWith("MAG")) {
				String[] magParts = extractString(line, "MAG:").split(",");
				magEnabled = magParts[0].trim().equals("1");
				magnetometerRange = Integer.parseInt(magParts[1].trim());
				continue;
			}
			
			if (line.startsWith("RATEX")) {
				outputDataRate = Integer.parseInt(extractString(line, "RATEX:"));
				continue;
			}
			
			if (line.startsWith("DATA")) {
				outputDataMode = extractString(line, "DATA MODE:");
				continue;
			}
			
			if (line.startsWith("SLEEP")) {
				sleepModeSetting = Integer.parseInt(extractString(line, "SLEEP MODE:"));
				continue;
			}
			
			if (line.startsWith("INACTIVE")) {
				inactivityTimeoutValue = extractString(line, "INACTIVE:");
				continue;
			}
		}
	}
	
	private String extractString(String text, String skipChars) {
		int numChars = skipChars.length();
		int startIndex = text.indexOf(skipChars);
		if (startIndex == -1) return null;
		
		return text.substring(startIndex + numChars).trim();
	}
	
	/**
	 * Convert accelerometer value to standard unit (g)
	 * @param accel - The raw accelerometer value
	 * @return The converted accelerometer value in unit (g)
	 * @see http://axivity.com/files/resources/WAX9_Developer_Guide_3.pdf, p. 19
	 */
	public double convertAccelerometerValueToG(short accel) {
		double divisor = 1;
		
		switch (accelerometerRange) {
			case 2:
				divisor = 16384;
				break;
			case 4:
				divisor = 8192;
				break;
			case 8:
				divisor = 4096;
				break;
			default:
				throw new UnsupportedOperationException("Undefined accelerometer conversion for range " + accelerometerRange);
		}
		
		return accel / divisor;
	}
}
