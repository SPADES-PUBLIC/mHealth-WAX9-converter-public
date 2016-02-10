/******************************************************************************************
 *  
 * Copyright (c) 2015 EveryFit, Inc.
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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class WAX9File {
	private static final String MHEALTH_TIMESTAMP_FILE_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";
	private static final String MHEALTH_TIMESTAMP_DATA_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
	private static final String MHEALTH_TIMEZONE_FILE_FORMAT = "Z";
	private static final long MILLIS_IN_HOUR = 3600000L;
	
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
	
	private String metadata = null;
	
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
		int i = 0;
		
		// read metadata
		List<Byte> metadataBytes = new ArrayList<Byte>();
		while ((datum = inputFileStream.read()) != -1) {
			currentByte = (byte)datum;
			if (currentByte == SLIP_END) break;
			metadataBytes.add(currentByte);
		}
		
		byte[] metaArr = new byte[metadataBytes.size()];
		for (Byte b : metadataBytes) {
			metaArr[i++] = b;
		}
		metadata = new String(metaArr, StandardCharsets.UTF_8);
		if (datum == -1) return;
		
		// read payload
		boolean packetRead = false;
		i = 0;
		
		// NOTE: For the first packet size, assume extended format size.
		// Then get correct packet size upon upon subsequent loops. 
		byte[] payload = new byte[WAX9Packet.EXTENDED_PACKET_SIZE];
		
		// collect the current byte from metadata reading
		payload[i++] = currentByte;
		
		while ((datum = inputFileStream.read()) != -1) {
			
			byte b = (byte)datum;
			switch (b) {
				case SLIP_END:
					if (i > 0) {
						packetRead = true;
					}
					break;
					
				case SLIP_ESC:
					// read next byte to determine action
					datum = inputFileStream.read();
					b = (byte)datum;
					
					if (b == SLIP_ESC_END) {
						b = SLIP_END;
					} else if (b == SLIP_ESC_ESC) {
						b = SLIP_ESC;
					}
					break;
			}
			payload[i++] = b;
			
			// still have more to read
			if (!packetRead) continue;
			
			// we've 'completed' the packet. lets write and reset
			boolean isExtendedPacket = i > WAX9Packet.STANDARD_PACKET_SIZE;
			WAX9Packet packet = new WAX9Packet(payload, isExtendedPacket);
			writeContentsToFile(packet);
			i = 0;
			payload = new byte[isExtendedPacket ? WAX9Packet.EXTENDED_PACKET_SIZE  : WAX9Packet.STANDARD_PACKET_SIZE];
			packetRead = false;
		}
				
		closeStreams();
	}	
	
	/**
	 * Opens a new file writer for outputting contents
	 * @param filename - The name of the file to open
	 * @throws IOException - Failed to open the output stream
	 */
	private void openNewFileWriter(String filename) throws IOException  {
		closeFileWriter();
		writer = new FileWriter(outputDirectory.getAbsolutePath() + filename);
		
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
	
	private void writeContentsToFile(WAX9Packet packet) throws IOException {
		if (lastWrittenPacket == null) {
			String filename = getMHealthFileName(packet.timestamp,  "ACCEL",  "serialNumber",  "tzOffset");
			openNewFileWriter(filename);
		} else if (splitFile) {
			// determine if we should open a new output file
			long currentHr = getHourFromTimestamp(packet.timestamp);
			long prevHr = getHourFromTimestamp(lastWrittenPacket.timestamp);
			if (currentHr != prevHr) {
				String filename = getMHealthFileName(packet.timestamp,  "sensorType",  "serialNumber",  "tzOffset"); 
				openNewFileWriter(filename);
			}
		}
		
		String csvLine = createCSVLine(packet);
		writer.write(csvLine);
		lastWrittenPacket = packet;
	}
	
	private String createCSVLine(WAX9Packet packet) {
		SimpleDateFormat sdf = new SimpleDateFormat(MHEALTH_TIMESTAMP_DATA_FORMAT);
		Date datetime = new Date(packet.timestamp);
		String ax = String.valueOf(packet.accelX);
		String ay = String.valueOf(packet.accelY);
		String az = String.valueOf(packet.accelZ);
		return String.join(",", sdf.format(datetime), ax, ay, az);
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
	
	private long getHourFromTimestamp(final long timestamp) {
		return (Math.round(timestamp)/MILLIS_IN_HOUR)*MILLIS_IN_HOUR;
	}
	
	private String getMHealthFileName(final long timestamp, final String sensorType, final String serialNumber, final String timezoneOffset) {
		SimpleDateFormat sdf = new SimpleDateFormat(MHEALTH_TIMESTAMP_FILE_FORMAT);
		Date datetime = new Date(timestamp);
		
		return String.format("%s.%s.%s-%s.csv", sensorType, serialNumber, sdf.format(datetime), timezoneOffset);
	}
	
	private static FileInputStream openInputFile(String filename) throws IOException {
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
		
		
		/*
		 * TODO: Determine if we need this for checking the file
		 * 
		DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
		int test = in.readInt();
		in.close();
		boolean isZipFile=(test == ZIP_INDICATOR);
		if (!isZipFile)
			return false;

		//Check if the file contains the necessary Actigraph files
		ZipFile zip = new ZipFile(file);
		boolean hasLogData=false;
		boolean hasInfoData=false;
		for (Enumeration<?> e = zip.entries(); e.hasMoreElements();) {
			ZipEntry entry = (ZipEntry) e.nextElement();
			if (entry.toString().equals("log.bin"))
				hasLogData=true;
			if (entry.toString().equals("info.txt"))
				hasInfoData=true;
		}
		zip.close();

		if (hasLogData && hasInfoData)
			return true;

		return  false;
		*/
		
		return new FileInputStream(file);
	}
	
	private static File openOutputDirectory(String outputDirectory) throws IOException {
		File directory = new File(outputDirectory);
		if(!directory.exists()) {
			throw new IOException("Failed to find the output directory" + outputDirectory);
		}
		
		if(!directory.isDirectory()) {
			throw new IOException(outputDirectory + " is not a directory.");
		}
		
		return directory;
	}
}
