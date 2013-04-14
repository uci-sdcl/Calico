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
package calico.plugins.palette.menuitems;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

import edu.umd.cs.piccolo.nodes.PImage;

import calico.CalicoOptions.menu.menubar;
import calico.inputhandlers.InputEventInfo;
import calico.plugins.palette.*;
import calico.plugins.palette.iconsets.CalicoIconManager;

public abstract class PaletteBarMenuItem extends PImage {
	
	private Image background = null;
	
	private BufferedImage bgBuf = null;
	private int buttonBorder = 5;
	
	public PaletteBarMenuItem()
	{
		this.background = calico.iconsets.CalicoIconManager.getIconImage("menu.button_bg");
		
		bgBuf = new BufferedImage(88,66, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D) bgBuf.getGraphics();
		g.setBackground(new Color(83,83,83));
		g.drawImage(this.background, null, null);
	}
	
	public abstract void onClick(InputEventInfo ev);

	public void setImage(Image img)
	{
		int width = PalettePlugin.PALETTE_ITEM_WIDTH;
		int height = PalettePlugin.PALETTE_ITEM_HEIGHT;
		BufferedImage finalImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB );
		Graphics2D g = (Graphics2D) finalImage.getGraphics();
		g.setBackground(new Color(83,83,83));
		//g.drawImage(this.background, null, null);

		g.drawImage(this.bgBuf.getSubimage(0,0, menubar.defaultSpriteSize,menubar.defaultSpriteSize).getScaledInstance(width, height, BufferedImage.SCALE_SMOOTH), null, null);

		
		BufferedImage unscaledImage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
		Graphics2D gUnscaledImage = (Graphics2D)unscaledImage.getGraphics();
		gUnscaledImage.drawImage(img, 0, 0, null);
		
		g.drawImage(unscaledImage.getScaledInstance(width-buttonBorder*2, height-buttonBorder*2, BufferedImage.SCALE_SMOOTH),  buttonBorder, buttonBorder, null);
		super.setImage((Image)finalImage);
		
		this.invalidatePaint();
		//this.foreground.setImage(img);
	}
	
}
