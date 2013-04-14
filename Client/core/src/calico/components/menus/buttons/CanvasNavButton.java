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
package calico.components.menus.buttons;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.*;

import calico.*;
import calico.components.*;
import calico.components.grid.*;
import calico.components.menus.CanvasMenuButton;
import calico.controllers.CCanvasController;
import calico.iconsets.CalicoIconManager;
import calico.inputhandlers.InputEventInfo;
import calico.modules.*;
import calico.networking.*;

import edu.umd.cs.piccolo.*;
import edu.umd.cs.piccolo.util.*;
import edu.umd.cs.piccolo.nodes.*;
import edu.umd.cs.piccolox.nodes.PLine;
import edu.umd.cs.piccolox.pswing.*;

import java.net.*;

import edu.umd.cs.piccolo.event.*;



public class CanvasNavButton extends CanvasMenuButton
{
	private static final long serialVersionUID = 1L;
	
	private long canvasuid = 0L;
	private int button_type = 1;
	
	public static final int TYPE_UP		= 1 << 0;
	public static final int TYPE_DOWN	= 1 << 1;
	public static final int TYPE_LEFT	= 1 << 2;
	public static final int TYPE_RIGHT	= 1 << 3;
		
	public CanvasNavButton(long cuid, int type)
	{
		super();
		
		button_type = type;
		canvasuid = cuid;
		
		try
		{
			switch(type)
			{
				case CanvasNavButton.TYPE_DOWN:
					iconString = "arrow.down";
					break;
					
				case CanvasNavButton.TYPE_UP:
					iconString = "arrow.up";
					break;
					
				case CanvasNavButton.TYPE_LEFT:
					iconString = "arrow.left";
					break;
					
				case CanvasNavButton.TYPE_RIGHT:
					iconString = "arrow.right";
					break;
			}
			
			setImage(CalicoIconManager.getIconImage(iconString));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
	}
	
	private static void loadCanvas(int x, int y)
	{
		long cuid = CGrid.getCanvasAtPos(x, y);
		
		if(cuid==0L)
		{
			// Error
			return;
		}
		CCanvasController.unloadCanvasImages(CCanvasController.getCurrentUUID());
		CCanvasController.loadCanvas(cuid);
	}
	
	public void actionMouseClicked(InputEventInfo event)//long cuid, int type)
	{
		if (event.getAction() == InputEventInfo.ACTION_PRESSED)
		{
			super.onMouseDown();
		}
		else if (event.getAction() == InputEventInfo.ACTION_RELEASED && isPressed)
		{
			// Grid Size
			int gridx = CGrid.GridCols-1;
			int gridy = CGrid.GridRows-1;
					
			// Canvas Coords
			int xpos = CGrid.getCanvasColumn(canvasuid);
			int ypos = CGrid.getCanvasRow(canvasuid);
			
			switch(button_type)
			{
				case CanvasNavButton.TYPE_DOWN:
					if((ypos+1)<=gridy)
					{
						
						
						loadCanvas(xpos,ypos+1);
						
					}
					else
					{
						loadCanvas(xpos,0);
					}
					break;
					
				case CanvasNavButton.TYPE_UP:
					if((ypos-1)>=0)
					{
						loadCanvas(xpos,ypos-1);
					}
					else
					{
						loadCanvas(xpos,gridy);
					}
					break;
					
				case CanvasNavButton.TYPE_LEFT:
					if((xpos-1)>=0)
					{
						loadCanvas(xpos-1,ypos);
					}
					else
					{
						loadCanvas(gridx,ypos);
					}
					break;
					
				case CanvasNavButton.TYPE_RIGHT:
					if((xpos+1)<=gridx)
					{
						loadCanvas(xpos+1,ypos);
					}
					else
					{
						loadCanvas(0,ypos);
					}
					break;
			}
			
			super.onMouseUp();
		}
	}
	/*public void actionMousePressed()//long cuid, int type)
	{
		actionMouseClicked();
	}*/
	
}
