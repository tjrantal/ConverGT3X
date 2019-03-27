/*
The software is licensed under a Creative Commons Attribution 3.0 Unported License.
Copyright (C) 2015 Timo Rantalainen, tjrantal at gmail dot com
*/

/*
	Actigraph GT3X+ file reader (https://github.com/actigraph/GT3X-File-Format) for the log.bin data
*/

package com.video4coach;
import java.io.*;
import java.util.*;

public class LogBinFileReader{
	private byte[] data;
	private ArrayList<ArrayList<Short>> tempAccelerations;	//Y, X, Z
	public short[][] accelerations;	//This will be returned to matlab
	private byte[] sensorValue;
	private byte[] sensorValue2;
	public byte[] headerData;	//To be used by the subclass LogRecordHeader
	public byte[] shortBytes;
	public byte[] tStampBytes;
	public LogBinFileReader(String fileIn, String fileOut){
		sensorValue = new byte[3];
		sensorValue2= new byte[2];
		headerData = new byte[8];
		shortBytes = new byte[2];
		tStampBytes = new byte[4];
		try {
			FileInputStream fi = new FileInputStream(fileIn);
			BufferedOutputStream bo = new BufferedOutputStream(new FileOutputStream(fileOut));	
			int dataLength =fi.available();
						
			/*Read packages here
				package format https://github.com/actigraph/GT3X-File-Format
				offset
				0 1 byte	= separator 1E
				1 1 byte	= package ID, I'm only interested in 0, which is activity https://github.com/actigraph/GT3X-File-Format/blob/master/LogRecords/Activity.md
				2 4 bytes	= timestamp in Unix time format (4 bytes)
				6 2 bytes 	= Size (n) uint16
				8 n bytes	= Payload
				8+4 1 byte	= Checksum
				
			*/
			LogBinFileReader.LogRecordHeader logrecord;
			while (fi.available() > 0){
				//Check that parsing is successful, should have 1E to indicate start of package
				logrecord = this.new LogRecordHeader(fi);
				/*Extract accelerations here*/
				if (logrecord.type == 0x00){
					int valuesInRecord = (int) logrecord.size*8/12;
					int valuesExtracted = 0;
					int direction = 0;	//Used to keep track of which dimension to extract
					//Loop through the payload to extract the accelerometry values
					short valueBits;
					while (valuesExtracted <valuesInRecord){
						//The values are 12 bit back-to-back -> 2 values take 3 bytes
						if (valuesExtracted % 2 == 0){
							fi.read(sensorValue);	 //Read the next 3 bytes
							valueBits =(short) (((sensorValue[0]& 0xff)<<4)| ((sensorValue[1] & 0xf0)>>4));
						} else {
							valueBits =(short) (((sensorValue[1]& 0x0f)<<8)| (sensorValue[2] & 0xff));
						}
						++valuesExtracted;
						if (valueBits > 2047){
							valueBits |=0xf000;	//Set the sign
						}
						//Assign the value to the correct dimension
						//tempAccelerations.get(direction).add(valueBits);
						++direction;
						shortBytes[0] = (byte) (valueBits & 0x00FF);	//LSB first (java is MSB)
						shortBytes[1] = (byte) ((valueBits & 0xFF00)>>8);	//MSB second
						bo.write(shortBytes);

						if (direction>2){
							direction = 0;
							
							
							tStampBytes[0] = (byte) (logrecord.timeStamp &	0x000000FF);	//LSB first (java is MSB)
							tStampBytes[1] = (byte) ((logrecord.timeStamp &	0x0000FF00)>>8);	//MSB second
							tStampBytes[2] = (byte) ((logrecord.timeStamp &	0x00FF0000)>>16);	//LSB first (java is MSB)
							tStampBytes[3] = (byte) ((logrecord.timeStamp &	0xFF000000)>>24);	//LSB first (java is MSB)
							bo.write(tStampBytes);
						}else{
						}
					}

				}else{
					
					//Implement ACTIVITY2 here
					if (logrecord.type == 0x1A){
						int valuesInRecord = (int) logrecord.size*8/16;
						int valuesExtracted = 0;
						int direction = 0;	//Used to keep track of which dimension to extract
						//Loop through the payload to extract the accelerometry values
						short valueBits;
						
						while (valuesExtracted <valuesInRecord){
							fi.read(shortBytes);	 //Read the next 2 bytes
							++valuesExtracted;
							bo.write(shortBytes);	//Pass values through as is
							++direction;
							if (direction>2){
								direction = 0;
								//Write time stamp
								tStampBytes[0] = (byte) (logrecord.timeStamp &	0x000000FF);	//LSB first (java is MSB)
								tStampBytes[1] = (byte) ((logrecord.timeStamp &	0x0000FF00)>>8);	//MSB second
								tStampBytes[2] = (byte) ((logrecord.timeStamp &	0x00FF0000)>>16);	//LSB first (java is MSB)
								tStampBytes[3] = (byte) ((logrecord.timeStamp &	0xFF000000)>>24);	//LSB first (java is MSB)
								bo.write(tStampBytes);
							}
						}
					}else{
						//Skip all other log records
						fi.skip(logrecord.size);
					}
				}
				//discard checksum
				fi.skip(1);	//Read one byte (checksum)
			}
			System.out.println("");
			bo.close();	//Done writing...
			//bf.close();	//Done writing...
			fi.close();	//Close the file
		} catch (Exception err){System.err.println("Error: "+err.getMessage());}
	}
	
	private int min(int[] a){
		return min(min(a[0],a[1]),a[2]);
	}
	
	private int min(int a,int b){
		return a < b ? a:b;
	}
	/*Helper class to contain log record header*/
	public class LogRecordHeader{
		public int separator;
		public int type;
		public int timeStamp;
		public int size;
		//public byte[] headerData;
		LogRecordHeader(FileInputStream fi){
			//headerData = new byte[8];
			try{
				fi.read(headerData);
			}catch (Exception err){System.err.println("Error: "+err.getMessage());}
			separator	=headerData[0];
			type		= headerData[1];
			timeStamp	= (int) (((0xff & headerData[5])<<24) | ((0xff & headerData[4])<<16) | ((0xff & headerData[3])<<8) | (0xff & headerData[2]));
			size		=(int) (((0xff & headerData[7])<<8) | (0xff & headerData[6]));
		}
	}
	
	public static void main(String[] a){
		LogBinFileReader lbr = new LogBinFileReader(a[0],a[1]);
	}
}