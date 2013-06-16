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
package calico.controllers;

import it.unimi.dsi.fastutil.longs.Long2ReferenceArrayMap;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.net.MalformedURLException;
import java.net.URL;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;
import org.imgscalr.Scalr;

import sun.awt.image.ImageFetchable;
import sun.awt.image.ToolkitImage;

import calico.*;
import calico.components.CGroup;
import calico.networking.Networking;
import calico.networking.netstuff.ByteUtils;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;


public class CImageController
{
	public static Long2ReferenceArrayMap<CGroup> groups = new Long2ReferenceArrayMap<CGroup>();

	private static Logger logger = Logger.getLogger(CImageController.class.getName());

	
	public static void setup()
	{
	
	}
	
	/**
	 * This will download an image and provide a local version to clients
	 * @param url
	 * @throws IOException 
	 */
	public static String download_image(long uuid, String url) throws IOException
	{
		if (imageExists(uuid))
		{
			try
			{
				(new File(getImagePath(uuid))).delete();
			}
			catch (Exception ioe)
			{
				
			}
		}
		
		if (!(new File(CalicoOptions.images.download_folder)).exists())
			(new File(CalicoOptions.images.download_folder)).mkdir();
		
		
		URL urlobj = new URL(url);
		String fileExt = getFileExtension(url);
		
	    // TODO: NEED TO DOWNLOAD THE IMAGE CONTENT AND WRITE TO A FILE
	    String filePath = CalicoOptions.images.download_folder + Long.toString(uuid) + "." + fileExt;
	    
	    System.out.println(filePath);
	    File imageFile = new File(filePath);
	    if (!imageFile.exists())
	    	imageFile.createNewFile();
	    
		InputStream is = urlobj.openStream();
		OutputStream os = new FileOutputStream(imageFile);

		byte[] b = new byte[2048];
		int length;

		while ((length = is.read(b)) != -1) {
			os.write(b, 0, length);
		}

		is.close();
		os.close();
	   
		return Long.toString(uuid)+"."+fileExt;
	}
	
	public static void save_to_disk(long uuid, String name, byte[] image) throws IOException {
		
		if (imageExists(uuid))
		{
			try
			{
				(new File(getImagePath(uuid))).delete();
			}
			catch (Exception ioe)
			{
				
			}
		}
		
		if (!(new File(CalicoOptions.images.download_folder + "/")).exists())
			(new File(CalicoOptions.images.download_folder)).mkdir();
		
		String fileExt = getFileExtension(name);
		
		String filePath = CalicoOptions.images.download_folder + Long.toString(uuid) + "." + fileExt;
		
	    File imageFile = new File(filePath);
	    if (!imageFile.exists())
	    	imageFile.createNewFile();
	    
		OutputStream os = new FileOutputStream(imageFile);
	    
	    os.write(image);
	    
	    os.close();
	    
	}
	
	public static String getImagePath(final long imageUUID)
	{
		File[] files = (new File(CalicoOptions.images.download_folder + "/")).listFiles(new FilenameFilter() {
	           public boolean accept(File dir, String name) {
	                return name.toLowerCase().startsWith(Long.toString(imageUUID) + ".");
	                }
	           }
	        );
		
//	    String filePath = CalicoOptions.images.download_folder + Long.toString(imageUUID) + "." + ext;
//		File imageFile = new File(filePath);
		if (files != null && files.length > 0)
			return files[0].getAbsolutePath();
		else
			return null;
	    
//	    return imageFile.exists();
	}
	
	public static String getImageLocalPath(final long uuid)
	{
		File[] files = (new File(CalicoOptions.images.download_folder + "/")).listFiles(new FilenameFilter() {
	           public boolean accept(File dir, String name) {
	                return name.toLowerCase().startsWith(Long.toString(uuid) + ".");
	                }
	           }
	        );
		
		String localPath = files[0].getPath();
		if (File.separatorChar != '/')
		{
			localPath = localPath.replace(File.separatorChar, '/');
		}
		
		return localPath;
	}
	
	public static boolean imageExists(long imageUUID)
	{
		return getImagePath(imageUUID) != null;
	}

	public static String getFileExtension(String url) {
		int mid= url.lastIndexOf(".");
	    String fileExt=url.substring(mid+1,url.length());
		return fileExt;
	}
	
	/**
	 * Just an alias to download_image, but catches the exception
	 * @param url
	 * @return
	 */
	public static String download_image_no_exception(long uuid, String url)
	{
		try
		{
			return download_image(uuid, url);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
	

	
	public static void resizeImageAndSend(final File imageOnDisk)
	{
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				double maxHeight = 1024, maxWidth = 1280;

				Image tempImage;
				try {
					tempImage = ImageIO.read(imageOnDisk);

					ImageLoadedTrigger trigger = new ImageLoadedTrigger();
					tempImage.getWidth(trigger);
					
					double scaledWidth = tempImage.getWidth(null);
					double scaledHeight = tempImage.getHeight(null);

					if (tempImage.getHeight(null) > maxHeight)
					{
						scaledWidth *= maxHeight / scaledHeight;
						scaledHeight = maxHeight;
					}
					if (tempImage.getWidth(null) > maxWidth)
					{
						scaledHeight *= maxWidth / scaledWidth;
						scaledWidth = maxWidth;
					}

					BufferedImage buffered = (BufferedImage)tempImage;
					BufferedImage thumbnail = Scalr.resize(buffered, (int)scaledWidth, (int)scaledHeight);

					//		int imageType = BufferedImage.TYPE_INT_ARGB;
					//    	BufferedImage scaledBI = new BufferedImage(scaledWidth, scaledHeight, imageType);
					//    	Graphics2D g = scaledBI.createGraphics();
					//   		g.setComposite(AlphaComposite.Src);
					//
					//    	g.drawImage(tempImage, 0, 0, scaledWidth, scaledHeight, null);
					//		g.dispose();

					ImageIO.write(thumbnail, "png", imageOnDisk);
		            Networking.send(CImageController.getImageTransferPacket(Calico.uuid(), CCanvasController.getCurrentUUID(), 
		            		50, 50, imageOnDisk));

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			}
		}).start();

	}
	
	public static CalicoPacket getImageTransferPacket(long uuid, long cuuid, int x, int y, File imageOnDisk)
	{		
		byte[] bytes = getBytesFromDisk(imageOnDisk);
		
		if (bytes == null || bytes.length == 0)
			return null;
		
		
		int numPackets = 1;
		for(int i=0;i<bytes.length;i=i+CalicoOptions.network.cluster_size)
		{
			numPackets++;// APPENDs
		}
		
		CalicoPacket packet = new CalicoPacket(ByteUtils.SIZE_OF_INT + ByteUtils.SIZE_OF_LONG + ByteUtils.SIZE_OF_INT *2 
				+ CalicoPacket.getSizeOfPacket(new Object[] {imageOnDisk.getName()})
				+ ByteUtils.SIZE_OF_INT + bytes.length * ByteUtils.SIZE_OF_BYTE);
		packet.putInt(NetworkCommand.IMAGE_TRANSFER);
		packet.putLong(uuid);
		packet.putLong(cuuid);
		packet.putLong(-32l);
		packet.putInt(x);
		packet.putInt(y);
		packet.putString(imageOnDisk.getName());
		packet.putInt(bytes.length);
		packet.putBytes(bytes);
		
		return packet;

		
//		CalicoPacket[] packets = new CalicoPacket[numPackets];
//		int packetIndex = 1;
//		for(int i=0;i<bytes.length;i=i+CalicoOptions.network.cluster_size)
//		{
//			int numingroup = (i+CalicoOptions.network.cluster_size) > bytes.length ? (bytes.length-i) : CalicoOptions.network.cluster_size;
//			packets[packetIndex] = new CalicoPacket(1 * numingroup * ByteUtils.SIZE_OF_BYTE);
//			
//			for (int j = 0; j < numingroup; j++)
//			{
//				packets[packetIndex].putBy
//			}
//		}
		
		
		
//		return null;
	}
	
	public static CalicoPacket getImageTransferFilePacket(long uuid, long cuuid, File imageOnDisk)
	{		
		byte[] bytes = getBytesFromDisk(imageOnDisk);
		
		if (bytes == null || bytes.length == 0)
			return null;
		
		
		int numPackets = 1;
		for(int i=0;i<bytes.length;i=i+CalicoOptions.network.cluster_size)
		{
			numPackets++;// APPENDs
		}
		
		CalicoPacket imagePacket = CalicoPacket.getPacket(NetworkCommand.IMAGE_TRANSFER_FILE,
				uuid,
				cuuid,
				0l,
				imageOnDisk.getName(),
				bytes.length);
		imagePacket.putBytes(bytes);
		
		
/*		CalicoPacket packet = new CalicoPacket(ByteUtils.SIZE_OF_INT + ByteUtils.SIZE_OF_LONG + ByteUtils.SIZE_OF_INT *2 
				+ CalicoPacket.getSizeOfPacket(new Object[] {imageOnDisk.getName()})
				+ ByteUtils.SIZE_OF_INT + bytes.length * ByteUtils.SIZE_OF_BYTE);
		packet.putInt(NetworkCommand.IMAGE_TRANSFER_FILE);
		packet.putLong(uuid);
		packet.putLong(cuuid);
		packet.putLong(0l);
		packet.putString(imageOnDisk.getName());
		packet.putInt(bytes.length);
		packet.putBytes(bytes);
*/		
		return imagePacket;

		
//		CalicoPacket[] packets = new CalicoPacket[numPackets];
//		int packetIndex = 1;
//		for(int i=0;i<bytes.length;i=i+CalicoOptions.network.cluster_size)
//		{
//			int numingroup = (i+CalicoOptions.network.cluster_size) > bytes.length ? (bytes.length-i) : CalicoOptions.network.cluster_size;
//			packets[packetIndex] = new CalicoPacket(1 * numingroup * ByteUtils.SIZE_OF_BYTE);
//			
//			for (int j = 0; j < numingroup; j++)
//			{
//				packets[packetIndex].putBy
//			}
//		}
		
		
		
//		return null;
	}

	public static byte[] getBytesFromDisk(File imageOnDisk) {
		byte[] bytes = null;
		try
		{
//			File imageOnDisk = new File(fileLocation);
			InputStream is = new FileInputStream(imageOnDisk);
			long length = imageOnDisk.length();
			if (length > Integer.MAX_VALUE)
			{
				throw new IOException("Image file is too large: is " + length + ", max is " + Integer.MAX_VALUE);
			}
			
			bytes = new byte[(int)length];
			
			int offset = 0;
			int numRead = 0;
			while (offset < bytes.length
					&& (numRead = is.read(bytes, offset, bytes.length-offset)) >= 0)
			{
				offset += numRead;
			}
			
			if (offset < bytes.length)
			{
				throw new IOException("Could not completely read file " + imageOnDisk.getName());
			}
			
			is.close();
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return bytes;
	}
	
	public static ImageInitializer getImageInitializer(long uuid, long cuid, String imageURL, int x, int y)
	{
		return (new CImageController()).new ImageInitializer(uuid, cuid, imageURL, x, y);
	}
	
	class ImageInitializer implements ImageObserver
	{
		long uuid;
		long cuid;
		String imageURL;
		int x, y;
		
		public ImageInitializer(long uuid, long cuid, String imageURL, int x, int y) 
		{
			this.uuid = uuid;
			this.cuid = cuid;
			this.imageURL = imageURL;
			this.x = x;
			this.y = y;
		}
		//I'm just going to assume that when this instance gets called, the image is loaded and ready to roll
		@Override
		public boolean imageUpdate(Image img, int infoflags, int x,
				int y, int width, int height) {
			CGroupController.create_image_group(uuid, cuid, 0L, imageURL, 0, "", this.x, this.y, width, height);		
			return false;
		}
		
	}
	

	
}

class ImageLoadedTrigger implements ImageObserver {
	
	public boolean triggered = false;
	public int width = 0;
	public int height = 0;
	
	@Override
	public boolean imageUpdate(Image img, int infoflags, int x, int y,
			int width, int height) {
			this.width = width;
			this.height = height;
			triggered = true;
		return false;
	}
};