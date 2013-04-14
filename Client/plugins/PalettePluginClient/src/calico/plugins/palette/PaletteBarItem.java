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
package calico.plugins.palette;

import java.awt.Color;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import calico.CalicoDataStore;
import calico.components.piemenu.PieMenu;
import calico.controllers.CCanvasController;
import calico.plugins.palette.iconsets.CalicoIconManager;

import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.event.PInputEventListener;
import edu.umd.cs.piccolo.nodes.PImage;

public class PaletteBarItem extends PImage {
	
	private long paletteUUID;
	private long uuid;
	private Image img;
	
	public PaletteBarItem(long paletteUUID, long paletteItemUUID, Image img)
	{
		this.paletteUUID = paletteUUID;
		this.uuid = paletteItemUUID;
		BufferedImage bimage = new BufferedImage(PalettePlugin.PALETTE_ITEM_WIDTH, PalettePlugin.PALETTE_ITEM_HEIGHT, BufferedImage.TYPE_INT_ARGB);
//		this.img = img;
		
		if (img == null)
		{
			
//			try
//			{
//				img = CalicoIconManager.getIconImage("");
//			}
//			catch(Exception e)
//			{
//				e.printStackTrace();
//			}
		}
		
		bimage.getGraphics().setColor(Color.white);
		bimage.getGraphics().fillRect(0, 0, PalettePlugin.PALETTE_ITEM_WIDTH, PalettePlugin.PALETTE_ITEM_HEIGHT);
		bimage.getGraphics().drawImage(img, 0, 0, null);
		setBounds(0,0,PalettePlugin.PALETTE_ITEM_WIDTH, PalettePlugin.PALETTE_ITEM_HEIGHT);
		
		setImage(bimage);
		
		
		
		this.setPaint(Color.white);
		

		
	}
	
	public long getUUID()
	{
		return uuid;
	}
	
}
