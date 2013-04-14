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
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.SwingUtilities;

import calico.CalicoDraw;
import calico.components.CCanvas.Layer;
import calico.controllers.CCanvasController;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;
import calico.perspectives.CalicoPerspective;
import calico.perspectives.CalicoPerspective.PerspectiveChangeListener;
import calico.perspectives.CanvasPerspective;
import edu.umd.cs.piccolo.util.PAffineTransform;
import edu.umd.cs.piccolo.util.PPaintContext;

public class CanvasViewScrap extends CGroup 
	implements CCanvas.ContentContributor, PerspectiveChangeListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private long targetCanvasUUID = 0l;
	private int lastSignature = 0;
	public static final int ROUNDED_RECTANGLE_OVERFLOW = 4;
	public static final int CELL_MARGIN = 6;
	public static final double INITIAL_SIZE_RATIO = 0.33d;
	
	protected Image image;

	public CanvasViewScrap(long uuid, long cuid, long targetCanvas)
	{
		super(uuid, cuid);
		this.targetCanvasUUID = targetCanvas;


		networkLoadCommand = NetworkCommand.CANVASVIEW_SCRAP_LOAD;
		CCanvasController.addContentContributor(this);

		
	}
	
	@Override
	public void finish(boolean fade) {
		super.finish(fade);
		
		setImage();
	}

//	public CanvasViewScrap(long uuid, long cuid, long puid, Image img,
//			int imgX, int imgY, int imageWidth, int imageHeight) {
//		super(uuid, cuid, puid, img, imgX, imgY, imageWidth, imageHeight);
//		// TODO Auto-generated constructor stub
//		
//		
//	}

	//set the image from the other canvases
	public void setImage()
	{
		final long canvasId = this.cuid;
		SwingUtilities.invokeLater(
				new Runnable() { public void run() { 
					render();
					CCanvasController.canvasdb.get(canvasId).getLayer(Layer.CONTENT).repaint();
				}});
//		CalicoDraw.repaint(this);
		
	}

	public void updateCell()
	{
		if (CCanvasController.getCurrentUUID() != this.cuid)
			return;
		
		//render if changed
		int sig = CCanvasController.get_signature(targetCanvasUUID);
		if(sig!=this.lastSignature)
		{			
			setImage();
		}
	}

	public void render()
	{
		if (CCanvasController.getCurrentUUID() != this.cuid)
			return;
		
		Rectangle tBounds = this.getPathReference().getBounds();
		Image img = CCanvasController.image(targetCanvasUUID, tBounds.width, tBounds.height);
//		Image img = getCanvasImage(canvasUID);
		this.lastSignature = CCanvasController.get_signature(targetCanvasUUID);

		Rectangle rect = new Rectangle(0, 0, tBounds.width, tBounds.height);
		RoundRectangle2D rRect = new RoundRectangle2D.Double(2, 2, tBounds.width-ROUNDED_RECTANGLE_OVERFLOW, tBounds.height-ROUNDED_RECTANGLE_OVERFLOW, 8, 8);
		Area border = new Area(rect);
		border.subtract(new Area(rRect));

		BufferedImage bimg = new BufferedImage(tBounds.width, tBounds.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D)bimg.createGraphics();
		g.setComposite(AlphaComposite.Src);
		g.drawImage(img, 2, 2, tBounds.width, tBounds.height,null);
		g.setColor(Color.green);
		g.fill(border);
		g.setColor(new Color(100,100,100));
		g.draw(rRect);

//		if (CCanvasController.canvasdb.get(canvasUID).getLockValue())
//		{
//			g.setStroke(new BasicStroke(4));
//		}

		g.setColor(Color.black);
		g.drawRoundRect(	2,//new Double(getBounds().x).intValue() + 2,
							2,//new Double(getBounds().y).intValue() + 2,
							tBounds.width - ROUNDED_RECTANGLE_OVERFLOW,
							tBounds.height - ROUNDED_RECTANGLE_OVERFLOW,
							10,
							10);
		g.dispose();

		for(int i = 0; i < bimg.getHeight(); i++)
		{
			for(int j = 0; j < bimg.getWidth(); j++)
			{
				if(bimg.getRGB(j, i) == Color.green.getRGB())
				{
					//bimg.setRGB(j, i, 0x8F1C1C);
					bimg.setRGB(j, i, 0xFFFFFF);
				}
			}
		}

		image = bimg;
//		setImage( bimg );
		//setImage( img );

//		setBounds( tBounds.getX(),tBounds.getY(),tBounds.getWidth(),tBounds.getHeight());
		//CalicoDraw.setNodeBounds(this, tBounds.getX(),tBounds.getY(),tBounds.getWidth(),tBounds.getHeight());

		//CalicoDraw.repaint(this);
//		CalicoDataStore.gridObject.repaint();

//		updatePresenceText();
//		updateCanvasLockIcon();
	}/////
	
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
				null);
		g2.setComposite(temp);
		paintContext.popTransform(piccoloTransform);
	}

	@Override
	public void recomputeBounds()
	{
		super.recomputeBounds();
		setImage();
//		image = CCanvasController.image(this.targetCanvasUUID);
		CalicoDraw.repaint(this);
	}
	
	public CalicoPacket[] getUpdatePackets(long uuid, long cuid, long puid, int dx, int dy, boolean captureChildren) {
		CalicoPacket[] packet = super.getUpdatePackets(uuid, cuid, puid, dx, dy, captureChildren);
		
		packet[0].putLong(targetCanvasUUID);
		
		return packet;
	}

	@Override
	public void contentChanged(long canvas_uuid) {
		if (CCanvasController.getCurrentUUID() == this.cuid)
			updateCell();
		
	}

	@Override
	public void clearContent(long canvas_uuid) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void perspectiveChanged(final CalicoPerspective perspective) {
		final long canvasId = this.cuid;
		SwingUtilities.invokeLater(
				new Runnable() { public void run() { 
					if (perspective instanceof CanvasPerspective
							&& CCanvasController.getCurrentUUID() == canvasId)
						updateCell();
				}
				});
		
	}
	
	public static double getDefaultWidth()
	{
		return (calico.CalicoDataStore.ScreenWidth * CanvasViewScrap.INITIAL_SIZE_RATIO);
		
	}
	
	public static double getDefaultHeight()
	{
		return (calico.CalicoDataStore.ScreenHeight * CanvasViewScrap.INITIAL_SIZE_RATIO);
	}







}
