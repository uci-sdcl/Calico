/*******************************************************************************
 * Copyright (c) 2013, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 * 
 * None of the name of the Regents of the University of California, or the names of its
 * contributors may be used to endorse or promote products derived from this software without specific
 * prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package calico.utils;

import it.unimi.dsi.fastutil.io.*;

import java.util.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;

import org.apache.commons.vfs.*;
import org.apache.commons.vfs.provider.bzip2.*;

import calico.*;
import calico.clients.*;
import calico.components.*;
import calico.controllers.*;
import calico.networking.netstuff.*;
import calico.plugins.CalicoPluginManager;
import calico.plugins.CalicoStateElement;
import calico.uuid.UUIDAllocator;

public class CalicoBackupHandler
{
	/*
	 * BACKUP FILE FORMAT
	 * 	BACKUP_FILE_START()
	 *  BACKUP_FILE_ATTR(STRING,STRING) [Any number of these]
	 *  
	 *  <THE ACTUAL CONTENT>
	 *  
	 *  BACKUP_FILE_END()
	 */
	private static boolean backupRestorationInProgress = false;
	
	public static void writeAutoBackupFile(String file) throws FileNotFoundException, IOException 
	{
		FileObject backupFile = COptions.fs.resolveFile(file);
		
		FileObject backupFileTemp = COptions.fs.resolveFile(file+".tmp");
		backupFileTemp.createFile();
		FileContent content = backupFileTemp.getContent();
		OutputStream fos = content.getOutputStream();
		writeBackupStream(fos);
		backupFileTemp.close();
		
		boolean canRead = true;
		try{
			backupFileTemp.moveTo(backupFile);
		}
		catch (Exception e)
		{
			canRead = false;
		}
		if (!canRead)
		{
			boolean writtenFile = false;
			int altCounter = 1;
			while (!writtenFile || altCounter > 50)
			{
				try {
					FileObject backupFileAlt = COptions.fs.resolveFile(COptions.server.backup.backup_file_alt + "_" + altCounter++ + ".csb");
					backupFileTemp.moveTo(backupFileAlt);
					writtenFile = true;
				}
				catch (Exception e)
				{
					
				}
				
			}
		}
		backupFileTemp.delete();

		backupFile.close();
	}
	
	
	public static void writeBackupFile(String file) throws FileNotFoundException, IOException 
	{
		FileObject backupFile = COptions.fs.resolveFile(file);
		backupFile.createFile();
		
		
		FileContent content = backupFile.getContent();
	
		OutputStream fos = content.getOutputStream();

		writeBackupStream(fos);

		backupFile.close();
	}
	
	
	public static void writePacketToStream(OutputStream out, CalicoPacket packet) throws IOException
	{
		byte[] packetSizeBuffer = new byte[ByteUtils.SIZE_OF_INT];
		ByteUtils.writeInt(packetSizeBuffer, packet.getBufferSize(), 0);
		out.write(packetSizeBuffer);
		
		out.write(packet.getBuffer());
	}
	
	public static void writeBackupStream(OutputStream fos) throws IOException
	{
		
		writePacketToStream(fos, CalicoPacket.command(NetworkCommand.BACKUP_FILE_START));

		
		// Attributes
		writePacketToStream(fos, CalicoPacket.getPacket(NetworkCommand.BACKUP_FILE_ATTR, "CreatedBy", System.getProperty("user.name","unknown")));
		writePacketToStream(fos, CalicoPacket.getPacket(NetworkCommand.BACKUP_FILE_ATTR, "TestAttr", "Test Value"));
		writePacketToStream(fos, CalicoPacket.getPacket(NetworkCommand.BACKUP_FILE_ATTR, "NextUUID", Long.toString(UUIDAllocator.getUUID())));
		
		writePacketToStream(fos, CalicoPacket.getPacket(NetworkCommand.RESTORE_START));
		
		long[] groups = CGroupController.groups.keySet().toLongArray();
		for (int i = 0; i < groups.length; i++)
			if (CGroupController.groups.get(groups[i]) instanceof CGroupImage)
			{	
				CGroup group = CGroupController.groups.get(groups[i]);
				String imagePath = CImageController.getImagePath(groups[i]);
				String imageName = group.getUUID() + "." + CImageController.getFileExtension(imagePath);
				byte[] imageBytes = CImageController.getBytesFromDisk(imagePath);
				
				CalicoPacket packet = new CalicoPacket(ByteUtils.SIZE_OF_INT + ByteUtils.SIZE_OF_LONG * 3 
						+ CalicoPacket.getSizeOfPacket(new Object[] {imageName})
						+ ByteUtils.SIZE_OF_INT + imageBytes.length * ByteUtils.SIZE_OF_BYTE);
				packet.putInt(NetworkCommand.IMAGE_TRANSFER_FILE);
				packet.putLong(group.getUUID());
				packet.putLong(group.getCanvasUUID());
				packet.putLong(group.getParentUUID());
				packet.putString(imageName);
				packet.putInt(imageBytes.length);
				packet.putBytes(imageBytes);
				
				
				writePacketToStream(fos, packet);
			}
				
		long[] canvasids = CCanvasController.canvases.keySet().toLongArray();
		
		
		// Canvas List
		for(int j=0;j<canvasids.length;j++)
		{
			writePacketToStream(fos, CCanvasController.canvases.get(canvasids[j]).getInfoPacket());
			
			CalicoPacket[] packets = CCanvasController.canvases.get(canvasids[j]).getUpdatePackets();
			for(int i=0;i<packets.length;i++)
			{
				writePacketToStream(fos, packets[i]);
			}
			
				
		}//canvases
		
		//Calico State Elements
		for (CalicoStateElement elements : CalicoPluginManager.calicoStateExtensions)
		{
			CalicoPacket[] packets = elements.getCalicoStateElementUpdatePackets();
			for (int j = 0; j < packets.length; j++)
				writePacketToStream(fos, packets[j]);
		}
		
		
		
		writePacketToStream(fos, CalicoPacket.command(NetworkCommand.BACKUP_FILE_END));
		
	}///////////////
	
	public static void restoreBackupStream(InputStream inputStream) throws IOException, CalicoInvalidBackupException
	{
		byte[] packetSizeBuffer = new byte[ByteUtils.SIZE_OF_INT];
		byte[] packetBuffer = null;
		int packetSize = 0;
		backupRestorationInProgress = true;
		Properties props = new Properties();
		
		// Check the first packet
		BinIO.loadBytes(inputStream, packetSizeBuffer);
		packetSize = ByteUtils.readInt(packetSizeBuffer, 0);
		if(packetSize!=ByteUtils.SIZE_OF_INT)
		{
			throw new CalicoInvalidBackupException("Invalid Starting (expected "+ByteUtils.SIZE_OF_INT+", received "+packetSize+")");
		}
		
		packetBuffer = new byte[packetSize];
		BinIO.loadBytes(inputStream,packetBuffer);
		if(ByteUtils.readInt(packetBuffer, 0)!=NetworkCommand.BACKUP_FILE_START)
		{
			throw new CalicoInvalidBackupException("Invalid Starting (expected "+NetworkCommand.BACKUP_FILE_START+", received "+ByteUtils.readInt(packetBuffer, 0)+")");	
		}
				
		
		while(inputStream.available()>=ByteUtils.SIZE_OF_INT)
		{
			// Get the size of the next packet
			BinIO.loadBytes(inputStream, packetSizeBuffer);
			packetSize = ByteUtils.readInt(packetSizeBuffer, 0);
			
			if(packetSize>=ByteUtils.SIZE_OF_INT)
			{
				// Read the packet
				packetBuffer = new byte[packetSize];
				BinIO.loadBytes(inputStream,packetBuffer);
				
				// Are we done?
				if(ByteUtils.readInt(packetBuffer, 0)==NetworkCommand.BACKUP_FILE_END)
				{
					//return;
				}
				
				// Make the packet
				CalicoPacket packet = new CalicoPacket(packetBuffer);
				packetBuffer = null;
				int command = packet.getInt();
				if(command==NetworkCommand.BACKUP_FILE_ATTR)
				{
					String key = packet.getString();
					String value = packet.getString();
					props.setProperty(key, value);
				}
				else
				{
					// HANDLE THE PACKET
					ProcessQueue.receive(command, null, packet);
				}
				
				CalicoServer.logger.debug("BACKUP: "+packet.toString());
			}//if size>4
		}//while avail

		String nextuuid = props.getProperty("NextUUID","28");
		
		CalicoServer.logger.debug("SETTING NEXT UUID TO BE "+nextuuid);
		
		UUIDAllocator.restoreUUIDAllocator(Long.parseLong(nextuuid));
		backupRestorationInProgress = false;
		
	}//restoreBackup
	
	
	public static void restoreBackupFile(String file) throws FileSystemException, IOException, CalicoInvalidBackupException
	{
		FileObject backupFile = COptions.fs.resolveFile(file);
		restoreBackupStream(backupFile.getContent().getInputStream());
		backupFile.close();
	}//restore
	
	public static Properties getBackupFileInfo(String file) throws FileSystemException, IOException, CalicoInvalidBackupException
	{
		Properties props = new Properties();
		
		FileObject backupFile = COptions.fs.resolveFile(file);
		

		
		FileContent content = backupFile.getContent();
		InputStream inputStream = content.getInputStream();
		
		byte[] packetSizeBuffer = new byte[ByteUtils.SIZE_OF_INT];
		byte[] packetBuffer = null;
		int packetSize = 0;
		
		// Check the first packet
		BinIO.loadBytes(inputStream, packetSizeBuffer);
		packetSize = ByteUtils.readInt(packetSizeBuffer, 0);
		if(packetSize!=ByteUtils.SIZE_OF_INT)
		{
			throw new CalicoInvalidBackupException("Invalid Starting (expected "+ByteUtils.SIZE_OF_INT+", received "+packetSize+")");
		}
		
		packetBuffer = new byte[packetSize];
		BinIO.loadBytes(inputStream,packetBuffer);
		if(ByteUtils.readInt(packetBuffer, 0)!=NetworkCommand.BACKUP_FILE_START)
		{
			throw new CalicoInvalidBackupException("Invalid Starting (expected "+NetworkCommand.BACKUP_FILE_START+", received "+ByteUtils.readInt(packetBuffer, 0)+")");	
		}
		
		while(inputStream.available()>=ByteUtils.SIZE_OF_INT)
		{
			// Get the size of the next packet
			BinIO.loadBytes(inputStream, packetSizeBuffer);
			packetSize = ByteUtils.readInt(packetSizeBuffer, 0);
			
			if(packetSize>=ByteUtils.SIZE_OF_INT)
			{
				// Read the packet
				packetBuffer = new byte[packetSize];
				BinIO.loadBytes(inputStream,packetBuffer);
				
				// Are we done?
				if(ByteUtils.readInt(packetBuffer, 0)==NetworkCommand.BACKUP_FILE_ATTR)
				{
					// Make the packet
					CalicoPacket packet = new CalicoPacket(packetBuffer);
					packet.getInt();
					packetBuffer = null;
					String key = packet.getString();
					String value = packet.getString();
					props.setProperty(key, value);
				}
				else
				{
					return props;
				}
				
			}//if size>4
		}//while avail
		
		
		backupFile.close();

		return props;
	}
	
	public static boolean isBackupRestorationInProgress()
	{
		return backupRestorationInProgress;
	}
	
	
	
}//
