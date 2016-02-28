/******************************************************************************************
 * 
 * Copyright (c) 2016 EveryFit, Inc.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * 
 * Authors:
 *  - Markland, Errol
 * 
 ******************************************************************************************/
package com.qmedic.data.converter.wax9;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * The WAX9 Settings associated with the binary file
 * 
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
	 * The time zone offset of the WAX9 File
	 */
	private String timezoneOffset = null;
	public String getTimezoneOffset() {
		return timezoneOffset;
	}

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
	public String getFirmwareVersion() {
		return firmwareVersion;
	}

	/**
	 * The chipset of the WAX9 device
	 */
	private String chipset = null;

	/**
	 * The device ID
	 */
	private String deviceID = null;

	public String getDeviceID() {
		return deviceID;
	}

	/**
	 * The device MAC address
	 */
	private String mac = null;
	public String getMACAddress() {
		return mac;
	}

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
		rawTimestamp = Long.parseLong(lines[0].trim());
		timestamp = new Date(rawTimestamp);
		
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
		
		if (timezoneOffset == null) {
			// default to timezone of timestamp (which defaults to local timezone of parser, if not specified)
			timezoneOffset = new SimpleDateFormat("Z").format(timestamp);
		}
	}

	private String extractString(final String text, final String skipChars) {
		int numChars = skipChars.length();
		int startIndex = text.indexOf(skipChars);
		if (startIndex == -1)
			return null;

		return text.substring(startIndex + numChars).trim();
	}

	/**
	 * Convert accelerometer value to SI unit g
	 * 
	 * @param accel
	 *            - The raw accelerometer value
	 * @return The converted accelerometer value in SI unit g
	 * @see http://axivity.com/files/resources/WAX9_Developer_Guide_3.pdf, p. 19
	 */
	public double convertAccelerometerValueToG(final short accel) {
		if (accelEnabled == null || !accelEnabled)
			return (double) accel;

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
				throw new UnsupportedOperationException(
						"Undefined accelerometer conversion for range " + accelerometerRange);
		}

		return (double) accel / divisor;
	}
		
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(rawTimestamp + "\n");
		sb.append(String.format("WAX9, HW: %s, FW: %s, CS: %s\n", hardwareVersion, firmwareVersion, chipset));
		sb.append(String.format("ID: %s\n", deviceID));
		sb.append(String.format("NAME: %s\n", name));
		sb.append(String.format("MAC: %s\n", mac));
		sb.append(String.format("ACCEL: %d, %d, %d\n", accelEnabled ? 1 : 0, accelerometerRate, accelerometerRange));
		sb.append(String.format("GYRO: %d, %d, %d\n", gyroEnabled ? 1 : 0, gyroscopeRate, gyroscopeRange));
		sb.append(String.format("MAG: %d, %d\n", magEnabled ? 1 : 0, magnetometerRange));
		sb.append(String.format("RATEX: %d\n", outputDataRate));
		sb.append(String.format("DATA MODE: %s\n", outputDataMode));
		sb.append(String.format("SLEEP MODE:%d\n", sleepModeSetting));
		sb.append(String.format("INACTIVE:%s", inactivityTimeoutValue));
		return sb.toString();
	}
}
