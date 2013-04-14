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
package calico.components.menus;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.SwingUtilities;

import calico.CalicoDraw;
import calico.CalicoOptions;
import calico.CalicoOptions.menu.menubar;
import calico.iconsets.CalicoIconManager;
import calico.inputhandlers.InputEventInfo;

import edu.umd.cs.piccolo.nodes.*;
import edu.umd.cs.piccolox.nodes.PComposite;


public class CanvasMenuButton extends PImage
{	
	protected long cuid = 0L;
	private static final long serialVersionUID = 1L;
	
	private int buttonBorder = 3;
	
	private Image background = null;
	
	private BufferedImage bgBuf = null;
	
	private boolean isSelected = false;
	
	protected boolean isPressed = false;
	protected String iconString;
	public static CanvasMenuButton activeMenuButton = null;
	
	public CanvasMenuButton()
	{
		this.background = CalicoIconManager.getIconImage("menu.button_bg");
			
		bgBuf = new BufferedImage(88,66, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D) bgBuf.getGraphics();
		g.setBackground(new Color(83,83,83));
		g.drawImage(this.background, null, null);
	}
	
	public void setSelected(boolean selected)
	{
		this.isSelected = selected;
		//setTransparency(CalicoOptions.menu.menubar.transparency_disabled);
	}
	
	public void actionMouseClicked()
	{
		
	}
	
	public void actionMouseClicked(InputEventInfo ev)
	{
		
	}
	
	public void setImage(Image img)
	{
		int width = menubar.defaultIconDimension;
		int height = menubar.defaultIconDimension;
		BufferedImage finalImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB );
		Graphics2D g = (Graphics2D) finalImage.getGraphics();
		g.setBackground(new Color(83,83,83));
		//g.drawImage(this.background, null, null);
		if(this.isSelected)
		{
			g.drawImage(this.bgBuf.getSubimage(0,44, menubar.defaultSpriteSize,menubar.defaultSpriteSize).getScaledInstance(width, height, BufferedImage.SCALE_SMOOTH), null, null);
		}
		else
		{
			Image bgBuf = this.bgBuf.getSubimage(0,0, menubar.defaultSpriteSize,menubar.defaultSpriteSize).getScaledInstance(width, height, BufferedImage.SCALE_SMOOTH);
			AffineTransform rot90Degrees = AffineTransform.getRotateInstance(Math.PI/2, menubar.defaultIconDimension/2, menubar.defaultIconDimension/2);
			g.drawImage(bgBuf, rot90Degrees, null);
//			g.rotate(Math.PI/2, menubar.defaultSpriteSize/2, menubar.defaultSpriteSize/2);
		}
		
		BufferedImage unscaledImage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
		Graphics2D gUnscaledImage = (Graphics2D)unscaledImage.getGraphics();
		gUnscaledImage.drawImage(img, 0, 0, null);
		
		g.drawImage(unscaledImage.getScaledInstance(width-buttonBorder*2, height-buttonBorder*2, BufferedImage.SCALE_SMOOTH),  buttonBorder, buttonBorder, null);
		super.setImage((Image)finalImage);
		
		//this.invalidatePaint();
		CalicoDraw.invalidatePaint(this);
		//this.foreground.setImage(img);
	}
	
	public void onMouseDown()
	{
		activeMenuButton = this;
		highlight_on();
	}
	
	public void highlight_on()
	{
		if (!isPressed)
		{
			isPressed = true;
			setSelected(true);
			final CanvasMenuButton tempButton = this;
			SwingUtilities.invokeLater(
					new Runnable() { public void run() { 
						double tempX = tempButton.getX();
						double tempY = tempButton.getY();
								
						setImage(CalicoIconManager.getIconImage(iconString));
						tempButton.setX(tempX);
						tempButton.setY(tempY);
					}});
			//CalicoDraw.setNodeX(this, tempX);
			//CalicoDraw.setNodeY(this, tempY);
			//this.repaintFrom(this.getBounds(), this);
			CalicoDraw.repaintNode(this);
		}
	}
	
	public void onMouseUp()
	{
		activeMenuButton = null;
		highlight_off();
	}
	
	public void highlight_off()
	{
		if (isPressed)
		{
			isPressed = false;
			setSelected(false);
			final CanvasMenuButton tempButton = this;
			SwingUtilities.invokeLater(
					new Runnable() { public void run() { 
						double tempX = tempButton.getX();
						double tempY = tempButton.getY();
								
						setImage(CalicoIconManager.getIconImage(iconString));
						tempButton.setX(tempX);
						tempButton.setY(tempY);
					}});
			//CalicoDraw.setNodeX(this, tempX);
			//CalicoDraw.setNodeY(this, tempY);
			//this.repaintFrom(this.getBounds(), this);
			CalicoDraw.repaintNode(this);
		}
	}
	
	/*
	public void setBounds(Rectangle rect)
	{
		this.foreground.setBounds(rect);
		this.background.setBounds(rect);
		super.setBounds(rect);
	}*/
}
