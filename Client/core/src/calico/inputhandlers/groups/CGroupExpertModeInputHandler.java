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
package calico.inputhandlers.groups;

import java.awt.*;

import calico.components.bubblemenu.BubbleMenu;
import calico.components.decorators.CListDecorator;
import calico.controllers.*;
import calico.iconsets.*;
import calico.input.CInputMode;
import calico.inputhandlers.*;
import calico.utils.*;

import org.apache.log4j.Logger;


public class CGroupExpertModeInputHandler extends CalicoAbstractInputHandler
{
	public static Logger logger = Logger.getLogger(CGroupExpertModeInputHandler.class.getName());
	
	private long uuid = 0L;
	
	private Point currentMouseLocation = null;
	
	private boolean isWaitingRightHold = false;// are we waiting for the right click hold to set
	private RightClickTimerTicker currentRightClickTimer = null;
	
	public static class RightClickTimerTicker extends TickerTask
	{
		private CGroupExpertModeInputHandler handler = null;
		
		public RightClickTimerTicker(CGroupExpertModeInputHandler handler)
		{
			this.handler = handler;
		}
		
		public boolean runtask()
		{
			if(this.handler.getAwaitingRightClickMode(this))
			{
				this.handler.setRightClickHoldMode();
			}
			return false;
		}
	}//RightClickTimerTicker
	
	private boolean isInRightClickMode = false;
	private boolean onePressActionPerformed = false;
	
	private CGroupInputHandler parentHandler = null;

	private InputEventInfo releasePoint;
	
	public CGroupExpertModeInputHandler(long u, CGroupInputHandler par)
	{
		uuid = u;
		parentHandler = par;
	}
	
	
	public boolean getAwaitingRightClickMode(RightClickTimerTicker taskObj)
	{
		// we dont want to honor requests from old crap
		if(this.currentRightClickTimer!=taskObj)
		{
			logger.debug("OLD TIMER TASK REQUEST");
			return false;
		}
		
		return this.isWaitingRightHold;
	}
	
	public void setRightClickHoldMode()
	{
		this.isInRightClickMode = true;
		logger.debug("ENABLING RIGHT CLICK MODE");
		CalicoInputManager.drawCursorImage(CGroupController.groupdb.get(this.uuid).getCanvasUID(),
				CalicoIconManager.getIconImage("scrap.move"), this.currentMouseLocation);
	}
	
	public void actionPressed(InputEventInfo e)
	{
		this.currentMouseLocation = new Point(e.getX(), e.getY());
		
		this.canvas_uid = CGroupController.groupdb.get(this.uuid).getCanvasUID();
		
		/*if(e.isRightButtonPressed())
		{
			this.isWaitingRightHold = true;
			
			this.currentRightClickTimer = new RightClickTimerTicker(this);
			Ticker.scheduleIn(CalicoOptions.core.hold_time, this.currentRightClickTimer );
		}
		else*/ if(e.isLeftButtonPressed())
		{

			if (e.isLeftButtonPressed() && CGroupController.groupdb.get(uuid) instanceof CListDecorator
				&& ((CListDecorator)CGroupController.groupdb.get(uuid)).getGroupCheckMarkAtPoint(e.getPoint()) != 0)
			{
				CListDecorator list = (CListDecorator)CGroupController.groupdb.get(uuid);
				long grp = list.getGroupCheckMarkAtPoint(e.getPoint());
				CGroupDecoratorController.list_set_check(uuid, CCanvasController.getCurrentUUID(), list.getParentUUID(), grp, !list.isChecked(grp));
				onePressActionPerformed = true;
			}
			else
			{
				if (BubbleMenu.activeUUID == uuid)
				{
					calico.inputhandlers.groups.CGroupScrapModeInputHandler.startDrag = true;
//					CGroupController.show_group_bubblemenu(uuid);
//					CCanvasStrokeModeInputHandler.deleteSmudge = true;
				}
				else
				{
					CalicoInputManager.rerouteEvent(this.canvas_uid, e);
				}
//				this.parentHandler.routeToHandler_actionPressed(CInputMode.SCRAP, this.pressPoint);
//				PLayer layer = CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getLayer();
//				MenuTimer menuTimer = new CalicoAbstractInputHandler.MenuTimer(this, 0l, 100l, CalicoOptions.core.max_hold_distance, 1000,
//						pressPoint.getPoint(), 0l, layer);
//				Ticker.scheduleIn(250, menuTimer);
				this.parentHandler.routeToHandler_actionPressed(CInputMode.SCRAP, e);
				
			}
		}
	}


	public void actionDragged(InputEventInfo e)
	{
		if (onePressActionPerformed)
			return;
		this.currentMouseLocation = new Point(e.getX(), e.getY());
		
		
//		if(e.isRightButton() && this.isInRightClickMode)/////////////////////////////////////////////
//		{
////			this.drawRightClickIcon(this.currentMouseLocation);
//			/*
//			if(this.pressPoint!=null)
//			{
//				this.parentHandler.routeToHandler_actionPressed(CInputMode.SCRAP, this.pressPoint);
//				this.pressPoint = null;
//			}
//			
//			this.parentHandler.routeToHandler_actionDragged(CInputMode.SCRAP, e);*/
//		}
//		else if(e.isRightButtonPressed() && this.isWaitingRightHold)/////////////////////////////////////////////
//		{
//			/*if(this.pressedMouseLocation.distance(this.currentMouseLocation)>=CalicoOptions.core.max_hold_distance) // WE EXCEEDED THE THRESHOLD
//			{
//				this.isWaitingRightHold = false;
//				logger.debug("NOT GOING TO ENTER RIGHTCLICK MODE - MOVED TOO FAR");
//
//				this.pressPoint.setButtonAndMask(InputEventInfo.BUTTON_LEFT);
//				this.parentHandler.routeToHandler_actionPressed(CInputMode.SCRAP, this.pressPoint);
//			}*/
//		}
		if(e.isLeftButtonPressed() &&
				(calico.inputhandlers.groups.CGroupScrapModeInputHandler.dragging
				|| calico.inputhandlers.groups.CGroupScrapModeInputHandler.startDrag))
//				e.isRightButtonPressed())/////////////////////////////////////////////
		{
			// Reroute to canvas handler
			/*e.setButtonAndMask(InputEventInfo.BUTTON_LEFT);*/
			if (calico.controllers.CGroupController.restoreOriginalStroke)
				calico.controllers.CGroupController.restoreOriginalStroke = false;
			this.parentHandler.routeToHandler_actionDragged(CInputMode.SCRAP, e);
		}
		else if(e.isLeftButtonPressed())/////////////////////////////////////////////
		{
			CalicoInputManager.rerouteEvent(this.canvas_uid, e);
		}
	}


	public void actionReleased(InputEventInfo e)
	{
		calico.inputhandlers.groups.CGroupScrapModeInputHandler.startDrag = false;
		this.releasePoint = e;
		CalicoInputManager.unlockHandlerIfMatch(this.uuid);
		
		if (onePressActionPerformed)
		{
			onePressActionPerformed = false;
			return;
		}
		this.currentMouseLocation = new Point(e.getX(), e.getY());
		

		if(calico.inputhandlers.groups.CGroupScrapModeInputHandler.dragging)//e.isRightButton() && this.isInRightClickMode)
		{
			/*this.isInRightClickMode = false;

			this.parentHandler.routeToHandler_actionReleased(CInputMode.SCRAP, e);*/
		}
//		else if(e.isRightButton() && this.isWaitingRightHold)
//		{
//			/*logger.debug("WOULD SHOW MENU");
//			
//			long stroke = CStrokeController.getPotentialScrap(e.getPoint());
//			if (!CGroupController.groupdb.get(uuid).isPermanent())
//				stroke = 0;
//			
//			if (stroke != 0l)
//				CalicoAbstractInputHandler.clickMenu(0l, 0l, e.getPoint());
//			else
//				CGroupController.show_group_piemenu(uuid, e.getGlobalPoint());*/
//
//		}
//		else if(e.isRightButton() && !this.isInRightClickMode)
//		{
//			/*e.setButtonAndMask(InputEventInfo.BUTTON_LEFT);
//			this.parentHandler.routeToHandler_actionReleased(CInputMode.SCRAP, e);*/
//		}
		else if(e.isLeftButton() || e.isRightButton())
		{
			CalicoInputManager.rerouteEvent(this.canvas_uid, e);
		}
		
		this.isWaitingRightHold = false;
		calico.inputhandlers.groups.CGroupScrapModeInputHandler.dragging = false;
		
	}

}
