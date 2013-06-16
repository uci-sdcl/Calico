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
package calico.inputhandlers.canvas;

import calico.*;

import calico.components.*;
import calico.components.menus.*;
import calico.components.piemenu.*;
import calico.components.piemenu.canvas.DeleteAreaButton;
import calico.components.piemenu.groups.GroupCopyButton;
import calico.components.piemenu.groups.GroupPasteButton;
import calico.components.piemenu.groups.GroupSetPermanentButton;
import calico.components.piemenu.groups.GroupShrinkToContentsButton;
import calico.controllers.*;
import calico.iconsets.CalicoIconManager;
import calico.input.CInputMode;
import calico.inputhandlers.*;
import calico.modules.*;
import calico.networking.*;
import calico.networking.netstuff.*;
import calico.utils.*;


import java.awt.geom.*;
import java.awt.*;

import java.net.URL;
import java.util.*;

import org.apache.log4j.*;

import edu.umd.cs.piccolo.event.*;
import edu.umd.cs.piccolo.nodes.PImage;
import java.lang.Math;

// implements PenListener
public class CCanvasExpertModeInputHandler extends CalicoAbstractInputHandler
{
	public static Logger logger = Logger.getLogger(CCanvasExpertModeInputHandler.class.getName());

	public static final double CREATE_GROUP_MIN_DIST = 15.0;
	
	private InputEventInfo lastEvent = null;
	
	private CCanvasInputHandler parentHandler = null;
	
//	private Point currentMouseLocation = null;
//	private Point pressedMouseLocation = null;
	private InputEventInfo pressPoint = null;

//	public static class MenuTimer extends TickerTask
//	{
//		private Point point;
//		private long cuuid;
//		
//		public MenuTimer(Point p, long c)
//		{
//			point = p;
//			cuuid = c;
//		}
//		
//		public boolean runtask()
//		{
//			PieMenu.displayPieMenu(point, 
//					new GroupSetPermanentButton(cuuid),
//					new GroupShrinkToContentsButton(cuuid),
//					new DeleteAreaButton(cuuid)
//				);
//			return false;
//		}
//	}
	
	
	

	private boolean hasSentGroupPress = false;
	
	private Point mouseDown;
	private Point mouseUp;

	public CCanvasExpertModeInputHandler(long cuid, CCanvasInputHandler parent)
	{
		canvas_uid = cuid;
		parentHandler = parent;
	}


	
	public void actionPressed(InputEventInfo e)
	{
		this.pressPoint = e;
//		this.currentMouseLocation = new Point(e.getX(), e.getY());
//		this.pressedMouseLocation = new Point(e.getX(), e.getY());
		this.hasSentGroupPress = false;
		if(e.isLeftButtonPressed())
		{
			this.parentHandler.routeToHandler_actionPressed(CInputMode.STROKE, this.pressPoint);
		}
		lastEvent = e;
		mouseDown = e.getPoint();
		
	}
	

	public void actionDragged(InputEventInfo e)
	{

//		this.currentMouseLocation = new Point(e.getX(), e.getY());
		
		
		
		if(e.isLeftButton()) // we are waiting for arrow mode
		{
			this.parentHandler.routeToHandler_actionDragged(CInputMode.STROKE, e);
//			if(this.pressedMouseLocation.distance(this.currentMouseLocation)>=CalicoOptions.core.max_hold_distance) // WE EXCEEDED THE THRESHOLD
//			{	
//				logger.debug("NOT GOING TO ENTER ARROW MODE - MOVED TOO FAR");
				
				// resend the event
				//this.parentHandler.routeToHandler_actionPressed(CInputMode.STROKE, this.pressPoint);
//			}
		}
		else if(e.isLeftButton()) // not ArrowMode
		{
			// draw stroke
			this.parentHandler.routeToHandler_actionDragged(CInputMode.STROKE, e);
		}
		else if(e.isRightButtonPressed())
		{
			if(!this.hasSentGroupPress)
			{
				this.hasSentGroupPress = true;
				lastEvent.setButtonAndMask(InputEventInfo.BUTTON_LEFT);
				this.parentHandler.routeToHandler_actionPressed(CInputMode.SCRAP, lastEvent);
			}
			
			e.setButtonAndMask(InputEventInfo.BUTTON_LEFT);
			this.parentHandler.routeToHandler_actionDragged(CInputMode.SCRAP, e);
		}
		
		
		
		lastEvent = e;
		
	}//dragged
	
	public void actionScroll(InputEventInfo e)
	{
	}
	

	public void actionReleased(InputEventInfo e)
	{
		mouseUp = e.getPoint();
//		this.currentMouseLocation = new Point(e.getX(), e.getY());
		
		// reset this (maybe they just tapped it accidentally)
		if(e.isLeftButton() || e.isRightButton())
		{
			//logger.debug("LEFT BUTTON ELSE RELEASE");
			this.parentHandler.routeToHandler_actionReleased(CInputMode.STROKE, e);
		}
		else if(e.isRightButton())
		{
			// finish scrap
			/*e.setButtonAndMask(InputEventInfo.BUTTON_LEFT);
			logger.debug("RELEASE EXPERT RIGHT BUTTON: "+e.isLeftButtonPressed());
			this.parentHandler.routeToHandler_actionReleased(CInputMode.SCRAP, e);
			if (e.menuShown == false 
				&& mouseDown.distance(mouseUp) < CalicoOptions.pen.doubleClickTolerance)
			{
				CalicoAbstractInputHandler.clickMenu(0l, 0l, mouseDown);
			}*/
		}
		else
		{
			logger.debug("EXPERT RELEASED ELSE");
		}
		
//		long currTime = (new Date()).getTime();
		lastEvent = e;
		
		//	super.mouseReleased(e);
	}
	
}
