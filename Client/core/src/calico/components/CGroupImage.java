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
package calico.components;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.imageio.ImageIO;

import sun.java2d.pipe.AlphaColorPipe;

import calico.CalicoDataStore;
import calico.CalicoDraw;
import calico.CalicoOptions;
import calico.components.grid.CGrid;
import calico.controllers.CCanvasController;
import calico.controllers.CImageController;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;

import edu.umd.cs.piccolo.util.PAffineTransform;
import edu.umd.cs.piccolo.util.PPaintContext;

public class CGroupImage extends CGroup implements ImageObserver {

	protected String imgURL;
	protected String serverLocalPath;
	protected Image image;
	protected boolean isDownloading = false;
	

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public CGroupImage(long uuid, long cuid, Image image)
	{
		super(uuid, cuid, 0L, true);
		imgURL = null; // have an image already, no loading here.
		this.image = image;

		Rectangle bounds = new Rectangle(0, 0, image.getWidth(null), image.getHeight(null));
		setShapeToRoundedRectangle(bounds, 0);
	}

	public CGroupImage(long uuid, long cuid, long puid, String img, int port, String localPath,
			int imgX, int imgY, int imageWidth, int imageHeight) {
		super(uuid, cuid, puid, true);
//		this.transparency = 1.0f;
		
		Rectangle bounds = new Rectangle(imgX, imgY, imageWidth, imageHeight);
		setShapeToRoundedRectangle(bounds, 0);
		
		this.serverLocalPath = localPath;
//		image.getWidth(this);
		
		//The server won't always report the correct host name.
		//The client now uses the host name specified by the user upon connecting.
		
		if (CImageController.imageExists(uuid))
		{
			setImage();
		}
		else
		{
			if (img.length() > 0)
			{
				this.imgURL = "http://" + CalicoDataStore.ServerHost + ":" + port + "/" + localPath;
			}
			else
			{
				this.imgURL = img;
			}
			
			Runnable runnable = new LoadImageThread(this, this.imgURL);
			//Runnable runnable = new LoadImageThread(this, img);
			Thread thread = new Thread(runnable);
			thread.start();
		}
	}

	public CGroupImage(long uuid, long cuid, long puid, Image img, int imgX,
			int imgY, int imageWidth, int imageHeight) {
		super(uuid, cuid, puid, true);
		this.transparency = 1.0f;
		this.image = img;
		Rectangle bounds = new Rectangle(imgX, imgY, imageWidth, imageHeight);
		setShapeToRoundedRectangle(bounds);
	}

	public CalicoPacket[] getUpdatePackets(long uuid, long cuid, long puid,
			int dx, int dy, boolean captureChildren) {
		Rectangle bounds = this.getRawPolygon().getBounds();
/*		CalicoPacket packet = CalicoPacket.getPacket(
				NetworkCommand.GROUP_IMAGE_LOAD, uuid, cuid, puid, "", 0, "", bounds.x
						+ dx, bounds.y + dy, bounds.width, bounds.height,
				this.isPermanent, captureChildren, this.rotation, this.scaleX,
				this.scaleY);	
*/
		
//		String urlToImage = CImageController.getImageURL(this.uuid);
//		String imageLocalPath = CImageController.getImageLocalPath(this.uuid);		
		CalicoPacket packet = CalicoPacket.getPacket(NetworkCommand.GROUP_IMAGE_LOAD,
				uuid,
				cuid,
				puid,
				imgURL,
				CalicoDataStore.ServerHTTPPort,
				serverLocalPath,
//				this.imgURL,
				bounds.x + dx,	//going to be honest here, I'm not sure exactly why the + 10's are necessary, but they are
				bounds.y + dy, // 
				bounds.width,
				bounds.height,
				this.isPermanent,
				captureChildren,
				this.rotation,
				this.scaleX,
				this.scaleY);		
		
		/*try {
			//Load the image from the file because it may not already be in memory.
			Image tempImage = ImageIO.read(new File(CImageController.getImagePath(this.uuid)));
			//Add the image to the packet
			packet.putImage(tempImage);
			
			//Release the image from memory.
			tempImage.flush();
			tempImage = null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		

		return new CalicoPacket[] { packet };
	}

	@Override
	public CalicoPacket[] getUpdatePackets(boolean captureChildren) {
		return getUpdatePackets(this.uuid, this.cuid, this.puid, 0, 0,
				captureChildren);
	}
	
	public void downloadImage(String imgURL)
	{
		//Always download the image first
		if (imgURL.length() > 0)
			CImageController.download_image_no_exception(uuid, imgURL);
	}

	public void setImage() {	
		try {

			if (!isDownloading && CImageController.imageExists(uuid))
				image = ImageIO.read(new File(CImageController
						.getImagePath(uuid)));
			/*else
			{
				URL url = new URL(imgURL);
				image = ImageIO.read(url);
			}*/
//				image = Toolkit.getDefaultToolkit().createImage(image);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (OutOfMemoryError e) {
			e.printStackTrace();
		}
		
		// image =
		// Toolkit.getDefaultToolkit().createImage(CImageController.getImagePath(uuid));

	}
	
	public void unloadImage()
	{
		if (image != null)
		{
			image.flush();
			image = null;
		}
	}

	protected void paint(final PPaintContext paintContext) {
		
		super.paint(paintContext);
		// setTransparency(1.0f);

		final Graphics2D g2 = paintContext.getGraphics();
		// g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
		// RenderingHints.VALUE_ANTIALIAS_ON);

		PAffineTransform piccoloTransform = getPTransform();
		paintContext.pushTransform(piccoloTransform);
		g2.setColor(Color.BLACK);
		Rectangle bounds = this.getRawPolygon().getBounds();
		Composite temp = g2.getComposite();
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
		g2.drawImage(image, bounds.x, bounds.y, bounds.width, bounds.height,
				this);
		g2.setComposite(temp);
		paintContext.popTransform(piccoloTransform);
	}

	@Override
	public boolean imageUpdate(Image img, int infoflags, int x, int y,
			int width, int height) {

		// redrawAll();
		// this.setPaintInvalid(true);
		// CCanvasController.canvasdb.get(cuid).repaint();
		//repaint();
		if (CCanvasController.getCurrentUUID() == getCanvasUID())
			CalicoDraw.repaint(this);
		return x == 0 || y == 0;
	}

	public Image getImage() {
		return image;
	}
	
	private class LoadImageThread implements Runnable
	{
		CGroupImage group;
		String imgURL;
		public LoadImageThread (CGroupImage group, String imgURL)
		{
			this.group = group;
			this.imgURL = imgURL;
		}

		@Override
		public void run() {
			group.isDownloading = true;
			group.downloadImage(imgURL);
			group.isDownloading = false;
			//If the image group is on a different canvas than the one being viewed, don't load it
			if (group.cuid == CCanvasController.getCurrentUUID())
				group.setImage();	
			// GridRemoval: CGrid.getInstance().updateCell(group.cuid);
			if (CCanvasController.getCurrentUUID() == getCanvasUID())
				CalicoDraw.repaint(group);
		}
		
	}
	
	@Override
	public int get_signature()
	{
		return 0;
	}

}
