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
import calico.input.CInputMode;
import calico.inputhandlers.InputEventInfo;
import calico.modules.*;
import calico.networking.*;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;

import edu.umd.cs.piccolo.*;
import edu.umd.cs.piccolo.util.*;
import edu.umd.cs.piccolo.nodes.*;
import edu.umd.cs.piccolox.nodes.PLine;
import edu.umd.cs.piccolox.pswing.*;

import java.net.*;

import edu.umd.cs.piccolo.event.*;


public class MBModeChangeButton extends CanvasMenuButton
{
	private static final long serialVersionUID = 1L;
		
	private long cuid = 0L;
	
	private CInputMode type = null;
		
	public MBModeChangeButton(long c, CInputMode t)
	{
		super();
		cuid = c;
		type = t;
		try
		{
			//setPaint(Color.BLACK);//CalicoOptions.getColor("canvas.menubar.button.background_color"));
			
			if(CalicoDataStore.Mode==type)
			{
				setSelected(true);
				//setTransparency(CalicoOptions.menu.menubar.transparency_disabled);
			}
			
			switch(type)
			{
				case ARROW:
					iconString = "mode.arrow";
					break;
					
				case DELETE:
					iconString = "mode.delete";
					
					break;
					
				case EXPERT:
					iconString = "mode.expert";
					break;
					
				case SCRAP:
					iconString = "mode.scrap";
					break;
					
				case STROKE:
					iconString = "mode.stroke";
					break;
					
				case POINTER:
					iconString = "mode.pointer";
					break;
			}
			
			setImage(CalicoIconManager.getIconImage(iconString));
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
	}
	
	
	public void actionMouseClicked(InputEventInfo event)
	{
		if (event.getAction() == InputEventInfo.ACTION_PRESSED)
		{
			super.onMouseDown();
		}
		else if (event.getAction() == InputEventInfo.ACTION_RELEASED && isPressed)
		{
			Calico.logger.debug("Changing Mode to "+type);
			
			if(type==CInputMode.DELETE)
			{
				//CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getBlobs();
				//return;
			}
			
			if(CalicoDataStore.Mode==type)
			{
				switch(type)
				{
					case ARROW:MessageObject.showError("Already in Arrow Mode");break;
					case DELETE:MessageObject.showError("Already in Eraser Mode");break;
					case EXPERT:MessageObject.showError("Already in Expert Mode");break;
					case SCRAP:MessageObject.showError("Already in Scrap Mode");break;
					case STROKE:MessageObject.showError("Already in Stroke Mode");break;
					case POINTER:MessageObject.showError("Already in Pointer Mode");break;
				}
			}
			else
			{
			
				//CalicoDataStore.Mode = type;
				CalicoDataStore.set_Mode(type);
				CCanvasController.redrawMenuBars();
				
				
				switch(type)
				{
					case ARROW:MessageObject.showNotice("Switching to Arrow Mode");break;
					case DELETE:MessageObject.showNotice("Switching to Eraser Mode");break;
					case EXPERT:MessageObject.showNotice("Switching to Expert Mode");/*StatusMessage.popup("You are entering expert mode. Be advised.");*/break;
					case SCRAP:MessageObject.showNotice("Switching to Scrap Mode");break;
					case STROKE:MessageObject.showNotice("Switching to Stroke Mode");break;
					case POINTER:MessageObject.showNotice("Switching to Pointer Mode");break;
				}
				
			}
			
			isPressed = false;
		}
	
		
	}
	
}
