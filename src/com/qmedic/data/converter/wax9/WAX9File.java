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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class WAX9File {
	private static final String MHEALTH_TIMESTAMP_FILE_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";
	private static final String MHEALTH_TIMESTAMP_DATA_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
	private static final String MHEALTH_DECIMAL_FORMAT = "0.000";
	
	// SLIP escape chars -  see http://tools.ietf.org/html/rfc1055 
	private final static byte SLIP_END = 	 (byte)0xC0; // 0300
	private final static byte SLIP_ESC = 	 (byte)0xDB; // 0333
	private final static byte SLIP_ESC_END = (byte)0xDC; // 0334
	private final static byte SLIP_ESC_ESC = (byte)0xDD; // 0335
	
	private final File outputDirectory;
	
	private FileWriter writer;
	private FileInputStream inputFileStream;
	
	private boolean splitFile = false;	
	private WAX9Packet lastWrittenPacket = null;
	
	private WAX9Settings settings = null;
	
	public WAX9File(final String inputFile, final String outputDirectoryPath) throws IOException {
		this.inputFileStream = openInputFile(inputFile);
		
		String dirName = outputDirectoryPath.charAt(0) == '/' ? 
			outputDirectoryPath.substring(1) :
			outputDirectoryPath;
		this.outputDirectory = openOutputDirectory(dirName);
	}
	
	/**
	 * Sets whether if file splitting should occur when writing contents
	 * @param shouldEnable
	 */
	public void enableSplitFile(boolean shouldEnable) {
		splitFile = shouldEnable;
	}
	
	/**
	 * Process the provided input file, writing the results to the specified output directory
	 * @throws IOException
	 */
	public void processFile() throws IOException {
		int datum = -1;
		byte currentByte = 0x00;	
		long totalBytesRead = 0;
		
		// read metadata
		List<Byte> metadataBytes = new ArrayList<Byte>();
		while ((datum = inputFileStream.read()) != -1) {
			currentByte = (byte)datum;
			if (currentByte == SLIP_END) break;
			metadataBytes.add(currentByte);			
		}
		if (datum == -1) return;
		totalBytesRead += metadataBytes.size();
		
		settings = new WAX9Settings(toPrimitiveByteArray(metadataBytes));
		
		// read payload
		boolean packetComplete = false;
		List<Byte> rawPacket = new ArrayList<Byte>();
		rawPacket.add(currentByte); // collect currentByte from metadata reading
		
		while ((datum = inputFileStream.read()) != -1) {
			currentByte = (byte)datum;
			
			switch (currentByte) {
				case SLIP_END:
					// ensure that this is not the first 'end' byte
					packetComplete = rawPacket.size() > 0;
					break;
					
				case SLIP_ESC:
					// read next byte to determine byte conversion
					datum = inputFileStream.read();
					currentByte = (byte)datum;
					totalBytesRead++;
					
					if (currentByte == SLIP_ESC_END) {
						currentByte = SLIP_END;
					} else if (currentByte == SLIP_ESC_ESC) {
						currentByte = SLIP_ESC;
					}
					break;
			}
			rawPacket.add(currentByte);
			if (!packetComplete) continue;
			
			// Packet completed --> lets write and reset
			WAX9Packet packet = new WAX9Packet(toPrimitiveByteArray(rawPacket), settings);
			writeContentsToFile(packet);
			
			totalBytesRead += rawPacket.size();
			rawPacket.clear();
			packetComplete = false;
			
			// Print progress
			if (totalBytesRead % 1000 == 0) {
				System.out.format("\rConverting sample...%dK", totalBytesRead/1000);
			}
		}
				
		closeStreams();
		System.out.println("\nDone");
	}
	
	/**
	 * Opens a new file writer for outputting contents
	 * @param filename - The name of the file to open
	 * @throws IOException - Failed to open the output stream
	 */
	private void openNewFileWriter(final String filename) throws IOException  {
		closeFileWriter();
		
		writer = new FileWriter(outputDirectory.getAbsolutePath() + "\\" + filename);
		writer.append("HEADER_TIME_STAMP,X,Y,Z\n");
	}
	
	/**
	 * Closes the file writer
	 */
	private void closeFileWriter() {
		if (this.writer == null) return;
		
		// clean up
		try {
			this.writer.flush();
			this.writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		this.writer = null;
	}
	
	/**
	 * Writes the contents of the packet to the file
	 * @param packet - The packet to write to file
	 * @throws IOException - Throw if unable to open a new file or write to file
	 */
	private void writeContentsToFile(final WAX9Packet packet) throws IOException {
		if (lastWrittenPacket == null) {
			String filename = getMHealthFileName(packet.timestamp);
			openNewFileWriter(filename);
		} else if (splitFile) {
			// determine if we should open a new output file
			int currentHr = getHourFromTimestamp(packet.timestamp);
			int prevHr = getHourFromTimestamp(lastWrittenPacket.timestamp);
			if (currentHr != prevHr) {
				String filename = getMHealthFileName(packet.timestamp); 
				openNewFileWriter(filename);
			}
		}
		
		String csvLine = createCSVLine(packet) + "\n";
		writer.write(csvLine);
		lastWrittenPacket = packet;
	}
	
	/**
	 * Create a mHealth-compatible CSV line from a WAX9 Packet
	 * @param packet - The packet to create a csv line from
	 * @return  The CSV values
	 */
	private String createCSVLine(final WAX9Packet packet) {
		StringBuilder sb = new StringBuilder();
		
		SimpleDateFormat sdf = new SimpleDateFormat(MHEALTH_TIMESTAMP_DATA_FORMAT);
		sb.append(sdf.format(packet.timestamp) + ",");
		
		double ax = settings == null ? packet.accelX : settings.convertAccelerometerValueToG(packet.accelX);
		sb.append(decimalFormat(ax) + ",");
		
		double ay = settings == null ? packet.accelY : settings.convertAccelerometerValueToG(packet.accelY);
		sb.append(decimalFormat(ay) + ",");
		
		double az = settings == null ? packet.accelZ : settings.convertAccelerometerValueToG(packet.accelZ);
		sb.append(decimalFormat(az));
		
		return sb.toString();
	}
	
	/**
	 * Method for closing all open streams (input streams, file streams, etc...)
	 */
	private void closeStreams() {
		closeFileWriter();
		
		if (inputFileStream == null) return;
		try {
			inputFileStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Generates a timestamp-based filename 
	 * @param timestamp The timestamp to base the filename on
	 * @return The filename
	 */
	private String getMHealthFileName(final Date timestamp) {
		SimpleDateFormat sdf = new SimpleDateFormat(MHEALTH_TIMESTAMP_FILE_FORMAT);
		return String.format("WAX9.%s.%s.%s-%s.csv", "ACCEL", settings.getDeviceID(), sdf.format(timestamp), "UTC");
	}
	
	private byte[] toPrimitiveByteArray(final List<Byte> bytes) {
		int size = bytes.size();
		byte[] out = new byte[size];
		for (int i = 0; i < size; i++) {
			out[i] = bytes.get(i);
		}
		return out;
	}
	
	private static FileInputStream openInputFile(final String filename) throws IOException {
		File file = new File(filename);
		if(!file.exists()) {
			throw new IOException("Failed to find the input file" + filename);
		}
		
		if(file.isDirectory()) {
			throw new IOException(filename + " is a directory. Input must be a file.");
		}
		if(!file.canRead()) {
			throw new IOException("Cannot read file "+file.getAbsolutePath());
		}
		if(file.length() < 4) {
			throw new IOException("File is empty");
		}
		
		return new FileInputStream(file);
	}
	
	private static File openOutputDirectory(final String outputDirectory) throws IOException {
		File directory = new File(outputDirectory);
		if(!directory.exists()) {
			throw new IOException("Failed to find the output directory" + outputDirectory);
		}
		
		if(!directory.isDirectory()) {
			throw new IOException(outputDirectory + " is not a directory.");
		}
		
		return directory;
	}
	
	private static DecimalFormat decimalFormatter = null;	
    public static String decimalFormat(final double val) {
    	if (decimalFormatter == null) {
    		decimalFormatter = new DecimalFormat(MHEALTH_DECIMAL_FORMAT);
    		decimalFormatter.setRoundingMode(RoundingMode.HALF_UP);
    	}
    	
    	return decimalFormatter.format(val);
    }
    
	private int getHourFromTimestamp(final Date timestamp) {
		Calendar cal = Calendar.getInstance(); 
		cal.setTime(timestamp);
		return cal.get(Calendar.MINUTE);
	}
}
