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

import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.ImageObserver;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.commons.vfs.*;
import org.apache.log4j.Logger;

import calico.COptions;
import calico.components.CGroup;
import calico.uuid.UUIDAllocator;

public class CImageController
{
	public static Long2ReferenceArrayMap<CGroup> groups = new Long2ReferenceArrayMap<CGroup>();

	private static Logger logger = Logger.getLogger(CImageController.class.getName());

	
	public static void setup()
	{
	
	}
	
	public static String download_image(long uuid, String url) throws IOException
	{
//		if (imageExists(uuid))
//		{
//			try
//			{
//				(new File(getImagePath(uuid))).delete();
//			}
//			catch (Exception ioe)
//			{
//				
//			}
//		}
		
		if (!(new File(COptions.server.images.download_folder + "/")).exists())
			(new File(COptions.server.images.download_folder)).mkdir();
		
		
		URL urlobj = new URL(url);
		String fileExt = getFileExtension(url);
		
	    // TODO: NEED TO DOWNLOAD THE IMAGE CONTENT AND WRITE TO A FILE
	    String filePath = COptions.server.images.download_folder + Long.toString(uuid) + "." + fileExt;
	    File imageFile = new File(filePath);
	    
	    if (!imageFile.exists())
	    {
			FileObject backupFile = COptions.fs.resolveFile(filePath);
			backupFile.createFile();
//	    	imageFile.createNewFile();
	    }
	    
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
		
		if (!(new File(COptions.server.images.download_folder + "/")).exists())
			(new File(COptions.server.images.download_folder)).mkdir();
		
		String fileExt = getFileExtension(name);
		
		String filePath = COptions.server.images.download_folder + Long.toString(uuid) + "." + fileExt;
		
		FileObject backupFile = COptions.fs.resolveFile(filePath);
		if (!backupFile.exists())
			backupFile.createFile();
		backupFile.close();
	    
	    File imageFile = new File(filePath);
	    
	    OutputStream os = new FileOutputStream(imageFile);
	    
	    os.write(image);
	    
	    os.close();
	    
	}
	
	public static String getImagePath(final long imageUUID)
	{
		File[] files = (new File(COptions.server.images.download_folder + "/")).listFiles(new FilenameFilter() {
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
	
	public static boolean imageExists(long imageUUID)
	{
		return getImagePath(imageUUID) != null;
	}
	
	public static byte[] getBytesFromDisk(String fileLocation) {
		byte[] bytes = null;
		try
		{
			File imageOnDisk = new File(fileLocation);
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

	public static String getFileExtension(String url) {
		int mid= url.lastIndexOf(".");
	    String fileExt=url.substring(mid+1,url.length());
		return fileExt;
	}
	
	public static Dimension getImageSize(String image)
	{
		int width = -1; 
		int height = -1;
		FileInputStream fis;
		try {
			fis = new FileInputStream(image);
		
			ImageInputStream in = ImageIO.createImageInputStream(fis);
			try {
			        final Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
			        if (readers.hasNext()) {
			                ImageReader reader = (ImageReader) readers.next();
			                try {
			                        reader.setInput(in);
			                        width = reader.getWidth(0);
			                        height = reader.getHeight(0);
			                } finally {
			                        reader.dispose();
			                }
			        }
			} finally {
			        if (in != null) in.close();
			        if (fis != null) fis.close();
			        if (width == -1 && height == -1)
			        {
			        	System.out.println("Could not retrieve image dimensions");
			        	throw new Exception();
			        }
			}
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return new Dimension(width, height);
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
	
	public static ImageInitializer getImageInitializer(long uuid, long cuid, String imageURL, int x, int y)
	{
		return (new CImageController()).new ImageInitializer(uuid, cuid, imageURL, x, y);
	}
	
	public static String getImageURL(final long uuid)
	{
		/*File[] files = (new File(COptions.server.images.download_folder + "/")).listFiles(new FilenameFilter() {
	           public boolean accept(File dir, String name) {
	                return name.toLowerCase().startsWith(Long.toString(uuid) + ".");
	                }
	           }
	        );
		
		String localPath = files[0].getPath();
		if (File.separatorChar != '/')
		{
			localPath = localPath.replace(File.separatorChar, '/');
		}*/
		String localPath = getImageLocalPath(uuid);
		
		String ipaddress = "0.0.0.0";
		try
		{
			ipaddress = InetAddress.getLocalHost().getCanonicalHostName();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
//		String localPath = getImagePath(uuid);
		return "http://" + ipaddress + ":" + COptions.admin.serversocket.getLocalPort() + "/" + localPath;
	}
	
	public static String getImageLocalPath(final long uuid)
	{
		File[] files = (new File(COptions.server.images.download_folder + "/")).listFiles(new FilenameFilter() {
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
			CGroupController.createImageGroup(uuid, cuid, 0L, imageURL, this.x, this.y, width, height);		
			return false;
		}
		
	}
	
}
