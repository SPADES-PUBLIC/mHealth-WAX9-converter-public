package com.qmedic.data.converter.wax9;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.accessibility.AccessibleExtendedText;
import javax.activity.ActivityCompletedException;

public class WAX9Settings {
	private byte[] rawBytes = null;
	private String rawMetadata = null;
	private String hardwareVersion = null;
	private String firmwareVersion = null;
	private String chipset = null;
	private String deviceID = null;
	private String mac = null;
	private String name = null;
	
	private Boolean accelEnabled = null;
	private Integer accelerometerRate = null;
	private Integer accelerometerRange = null;
	
	private Boolean gyroEnabled = null;
	private Integer gyroscopeRate = null;
	private Integer gyroscopeRange = null;
	
	private Boolean magEnabled = null;
	private Integer magnetometerRange = null;
	
	private Integer outputDataRate = null;
	private Integer outputDataMode = null;
	
	private Integer sleepModeSetting = null;
	private String inactivityTimeoutValue = null;
	
	public WAX9Settings(final byte[] bytes) {
		rawBytes = bytes;
		rawMetadata = new String(bytes, StandardCharsets.UTF_8);
		
		String[] lines = rawMetadata.split("\n");
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
				outputDataMode = Integer.parseInt(extractString(line, "DATA MODE:"));
				continue;
			}
			
			if (line.startsWith("INACTIVE")) {
				inactivityTimeoutValue = extractString(line, "INACTIVE:");
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
