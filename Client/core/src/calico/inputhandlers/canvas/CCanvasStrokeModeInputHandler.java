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
import calico.components.bubblemenu.BubbleMenu;
import calico.components.decorators.CListDecorator;
import calico.components.piemenu.*;
import calico.components.piemenu.canvas.DeleteAreaButton;
import calico.components.piemenu.groups.GroupCreateTempButton;
import calico.controllers.CConnectorController;
import calico.controllers.CGroupController;
import calico.controllers.CStrokeController;
import calico.controllers.CCanvasController;
import calico.iconsets.CalicoIconManager;
import calico.input.CInputMode;
import calico.inputhandlers.CCanvasInputHandler;
import calico.inputhandlers.CGroupInputHandler;
import calico.inputhandlers.CalicoAbstractInputHandler;
import calico.inputhandlers.CalicoInputManager;
import calico.inputhandlers.InputEventInfo;
import calico.inputhandlers.PressAndHoldAction;
import calico.inputhandlers.groups.CGroupExpertModeInputHandler;
import calico.utils.Geometry;
import calico.utils.Ticker;

import java.awt.*;
import java.awt.geom.Arc2D.Double;
import java.util.*;
import org.apache.log4j.*;

import edu.umd.cs.piccolo.PLayer;

// implements PenListener
public class CCanvasStrokeModeInputHandler extends CalicoAbstractInputHandler
	implements PressAndHoldAction
{
	public static Logger logger = Logger.getLogger(CCanvasStrokeModeInputHandler.class.getName());
	
	private boolean hasStartedBge = false;
	
	private boolean hasBeenPressed = false;

	private InputEventInfo lastPoint = null;
	private long lastPointTime = -1l;
	private long lastStroke = 0l;
	
	public Point mouseDown;
	public Point mouseUp;
	public static boolean mouseMoved = false;
	
	CalicoAbstractInputHandler.MenuTimer menuTimer;
	private CCanvasInputHandler parentHandler = null;

	private long activeGroup;
	
	public static boolean deleteSmudge = false;
	
	public void openMenu(long potScrap, long group, Point point)
	{
		CalicoAbstractInputHandler.clickMenu(potScrap, group, point);
		
		if (CGroupController.exists(CGroupController.getCurrentUUID()) 
				&& !CGroupController.groupdb.get(CGroupController.getCurrentUUID()).isPermanent()
				&& point != null
				&& CGroupController.groupdb.get(CGroupController.getCurrentUUID()).containsPoint(point.x, point.y))
		{
			this.activeGroup = CGroupController.getCurrentUUID();
			calico.inputhandlers.groups.CGroupScrapModeInputHandler.startDrag = true;
			CCanvasStrokeModeInputHandler.deleteSmudge = true;
			
			CalicoAbstractInputHandler handler = CalicoInputManager.getInputHandler(this.activeGroup);
			if (handler instanceof CGroupInputHandler)
			{
				CalicoInputManager.unlockHandlerIfMatch(CCanvasController.getCurrentUUID());
				CalicoInputManager.lockInputHandler(this.activeGroup);
				((CGroupInputHandler)handler).routeToHandler_actionPressed(calico.input.CInputMode.EXPERT, lastPoint);
			}
		}
	
	}
	
	public CCanvasStrokeModeInputHandler(long cuid, CCanvasInputHandler parent)
	{
		canvas_uid = cuid;
		parentHandler = parent;
		this.setupModeIcon("mode.stroke");
	}

	@Deprecated
	private void getMenuBarClick(Point point)
	{
		if(CCanvasController.canvasdb.get(canvas_uid).isPointOnMenuBar(point))
		{
			CCanvasController.canvasdb.get(canvas_uid).clickMenuBar(null, point);
		}
	}



	public void actionPressed(InputEventInfo e)
	{
		//Tablets might not get the released event if the stylus is moved off the edge.
		//Make sure any previous stroke is finished before starting the next
		if (hasBeenPressed)
		{
			InputEventInfo lastEvent = new InputEventInfo();
			lastEvent.setPoint(lastPoint.getPoint());
			lastEvent.setButton(InputEventInfo.BUTTON_LEFT);
			actionReleased(lastEvent);
		}
		
		mouseMoved = false;
//		CalicoInputManager.drawCursorImage(canvas_uid,
//				CalicoIconManager.getIconImage("mode.stroke"), e.getPoint());
		
		hasBeenPressed = true;
		long uuid = 0l;
		
		if(e.isLeftButton())
		{
			int x = e.getX();
			int y = e.getY();
			uuid = Calico.uuid();
			CStrokeController.setCurrentUUID(uuid);
			CStrokeController.start(uuid, canvas_uid, 0L);
			CStrokeController.append(uuid, x, y);
			hasStartedBge = true;
			mouseDown = e.getPoint();
			
			boolean triggered = triggeredCanvasObject(e, uuid);
			
			lastPoint = e;
			lastPointTime = System.currentTimeMillis();
			lastStroke = uuid;
			
			if (triggered && e.isLeftButton())
				Ticker.scheduleIn(CalicoOptions.core.hold_time, menuTimer);
//			else if (triggered && e.isRightButton())
//			{
//				pressAndHoldCompleted();
//				calico.inputhandlers.groups.CGroupScrapModeInputHandler.startDrag = false;
//				CCanvasStrokeModeInputHandler.deleteSmudge = true;
//				hasBeenPressed = false;
////				openMenu(0l, this.activeGroup, getLastPoint());
//			}
			
			
//			menuThread = new DisplayMenuThread(this, e.getGlobalPoint(), e.group);		
//			Ticker.scheduleIn(CalicoOptions.core.hold_time, menuThread);
		}
		
		lastPoint = e;
		lastPointTime = System.currentTimeMillis();
		lastStroke = uuid;
	}

	private boolean triggeredCanvasObject(InputEventInfo e, long uuid) {
		long potentialConnector;
		long connector;
		long potentialScrap;
		
		boolean triggered = false;
		
		if ((potentialScrap = CStrokeController.getPotentialScrap(e.getPoint())) > 0l)
		{
			PLayer layer = CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getLayer();
			menuTimer = new CalicoAbstractInputHandler.MenuTimer(this, uuid, CalicoOptions.core.hold_time/2, CalicoOptions.core.max_hold_distance, CalicoOptions.core.hold_time, e.getPoint(), potentialScrap, layer);
//				Ticker.scheduleIn(CalicoOptions.core.hold_time, menuTimer);
			triggered = true;
			this.activeGroup = potentialScrap;
		} 
		else if ((potentialConnector = CStrokeController.getPotentialConnector(e.getPoint(), 20)) > 0l)
		{
			PLayer layer = CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getLayer();
			menuTimer = new CalicoAbstractInputHandler.MenuTimer(this, uuid, CalicoOptions.core.hold_time/2, CalicoOptions.core.max_hold_distance, CalicoOptions.core.hold_time, e.getPoint(), potentialConnector, layer);
			triggered = true;
//				Ticker.scheduleIn(CalicoOptions.core.hold_time, menuTimer);
			this.activeGroup = potentialConnector;
		}
		else if ((connector = CConnectorController.getNearestConnector(e.getPoint(), 20)) > 0l)
		{
			PLayer layer = CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getLayer();
			menuTimer = new CalicoAbstractInputHandler.MenuTimer(this, uuid, CalicoOptions.core.hold_time/2, CalicoOptions.core.max_hold_distance, CalicoOptions.core.hold_time, e.getPoint(), connector, layer);
			triggered = true;
//				Ticker.scheduleIn(CalicoOptions.core.hold_time, menuTimer);
			this.activeGroup = connector;
		}
		else if (!BubbleMenu.isBubbleMenuActive() &&
				(this.activeGroup = CGroupController.get_smallest_containing_group_for_point(CCanvasController.getCurrentUUID(), e.getPoint())) != 0l)
		{
			PLayer layer = CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getLayer();
			menuTimer = new CalicoAbstractInputHandler.MenuTimer(this, uuid, CalicoOptions.core.hold_time/2, CalicoOptions.core.max_hold_distance, CalicoOptions.core.hold_time,
					e.getPoint(), this.activeGroup, layer);
//				Ticker.scheduleIn(CalicoOptions.core.hold_time, menuTimer);
			triggered = true;
			
		}
		return triggered;
	}

	public void actionDragged(InputEventInfo e)
	{
		if (mouseMoved == false && mouseDown != null && mouseDown.distance(e.getPoint()) > 5)
		{
			mouseMoved = true;

		}
		/*if(BubbleMenu.isBubbleMenuActive())
		{
			return;
		}*/
		
//		this.hideModeIcon(e.getPoint());

		int x = e.getX();
		int y = e.getY();

		
		if(e.isLeftButtonPressed() && hasStartedBge)
		{
			//This next part is for line smoothing
			int xPrev = lastPoint.getPoint().x;
			int yPrev = lastPoint.getPoint().y;
			int newX = (int)((double)xPrev + (double)x)/2;
			int newY = (int)((double)yPrev + (double)y)/2;
			CStrokeController.append(CStrokeController.getCurrentUUID(), newX, newY);
			CStrokeController.append(CStrokeController.getCurrentUUID(), x, y);

		}
		else if(e.isLeftButtonPressed() && !hasStartedBge)
		{
			long uuid = Calico.uuid();
			CStrokeController.setCurrentUUID(uuid);
			CStrokeController.start(uuid, canvas_uid, 0L);
			CStrokeController.append(uuid, x, y);
			hasStartedBge = true;
			hasBeenPressed = true;
		}
		
		lastPoint = e;
		lastPointTime = System.currentTimeMillis();
	}
	
	public void actionScroll(InputEventInfo e)
	{
	}
	

	public void actionReleased(InputEventInfo e)
	{
		/*if(BubbleMenu.isBubbleMenuActive())
		{
			return;
		}*/
		mouseUp = e.getPoint();
//		this.hideModeIcon();
		
		CalicoInputManager.unlockHandlerIfMatch(canvas_uid);

		mouseMoved = true;

		
		int x = e.getX();
		int y = e.getY();
		
		if (e.isLeftButton())
		{
			long strokeUID = CStrokeController.getCurrentUUID();
			boolean isPotentialScrap = false;
			if(hasStartedBge)
//			{
//				bguid = Calico.uuid();
//				CStrokeController.setCurrentUUID(bguid);
//				CStrokeController.start(bguid, canvas_uid, 0L);
//			}
//			else
			{
				CStrokeController.append(strokeUID, x, y);
				 isPotentialScrap = CStrokeController.isPotentialScrap(strokeUID);
				//if it's a circle-scrap, we don't want to broadcast it to the server!
//				if (isPotentialScrap)
//					CStrokeController.no_notify_finish(bguid);
//				else
					CStrokeController.finish(strokeUID);
			}
			
//			long nearestConnector = CConnectorController.getNearestConnector(e.getPoint(), 20);
//			if (nearestConnector > 0l)
//				deleteSmudge = true;

			hasStartedBge = false;
			boolean isSmudge = false;
			if (/*this.activeGroup != 0l
					|| */BubbleMenu.activeUUID != 0l)
			{
				CGroupController.move_end(BubbleMenu.activeUUID, e.getPoint().x, e.getPoint().y);
				if (BubbleMenu.highlightedParentGroup != 0l)
				{
					CGroupController.groupdb.get(BubbleMenu.highlightedParentGroup).highlight_off();
					CGroupController.groupdb.get(BubbleMenu.highlightedParentGroup).highlight_repaint();

					BubbleMenu.highlightedParentGroup = 0l;
				}
				
//				CalicoInputManager.rerouteEvent(this.activeGroup, e);
//				this.parentHandler.routeToHandler_actionReleased(CInputMode.EXPERT, e);
			}
			else if (CStrokeController.exists(strokeUID))
			{
				if (CStrokeController.strokes.get(strokeUID).getWidth() <= 10 &&
						CStrokeController.strokes.get(strokeUID).getHeight() <= 10)
				{
					isSmudge = true;
				}
				if (isSmudge && deleteSmudge)
				{
					CStrokeController.delete(strokeUID);
				}
			}
			
			
			if (isPotentialScrap)
			{
				/*if (CStrokeController.isPotentialScrap(CStrokeController.getCurrentUUID()))
				{
					strokeUID = CStrokeController.getCurrentUUID();
				}*/
				CalicoAbstractInputHandler.clickMenu(strokeUID, 0l, mouseDown);
			}
//			else if (nearestConnector > 0l && isSmudge)
//			{
//				CConnectorController.show_stroke_bubblemenu(nearestConnector, false);
//			}
			else if (CStrokeController.exists(strokeUID))
			{
				
				Polygon poly = CStrokeController.strokes.get(strokeUID).getRawPolygon();
				long guuidA = CGroupController.get_smallest_containing_group_for_point(CCanvasController.getCurrentUUID(), new Point(poly.xpoints[0],poly.ypoints[0]));
				long guuidB = CGroupController.get_smallest_containing_group_for_point(CCanvasController.getCurrentUUID(), new Point(poly.xpoints[poly.npoints-1], poly.ypoints[poly.npoints-1]));
				if (guuidA != 0l && guuidB != 0l 
						&& !(guuidA == guuidB && CGroupController.groupdb.get(guuidA).containsShape(poly))
						&& !(CGroupController.groupdb.get(guuidA) instanceof CListDecorator) && !(CGroupController.groupdb.get(guuidB) instanceof CListDecorator))
				{
					/**
					 * Refactored so that the originating group can choose what to do. 
					 */
					CalicoAbstractInputHandler groupInputHandler = CalicoInputManager.getInputHandler(guuidA);
					if (groupInputHandler instanceof CGroupInputHandler)
						((CGroupInputHandler)CalicoInputManager.getInputHandler(guuidA)).actionStrokeToAnotherGroup(strokeUID, guuidB);
					//CConnectorController.no_notify_create(Calico.uuid(), CCanvasController.getCurrentUUID(), 0l, CalicoDataStore.PenColor, CalicoDataStore.PenThickness, guuidA, guuidB, strokeUID);
					//CStrokeController.delete(strokeUID);
				}
			}
		}
		else if (e.isRightButton())
		{
			boolean triggered = triggeredCanvasObject(e, 0l);
			
			if (triggered && e.isRightButton())
				{
					lastPoint = e;
					CStrokeController.setCurrentUUID(0l);
					pressAndHoldCompleted();
					calico.inputhandlers.groups.CGroupScrapModeInputHandler.startDrag = false;
					CCanvasStrokeModeInputHandler.deleteSmudge = true;
					hasBeenPressed = false;
//					openMenu(0l, this.activeGroup, getLastPoint());
				}
		}

		deleteSmudge = false;
		hasBeenPressed = false;
		lastPoint = null;
		lastPointTime = 0l;
		lastStroke = 0l;
	}
	
	public Point getLastPoint()
	{
		return lastPoint.getPoint();
	}
	
	public long getLastPointTime()
	{
		return lastPointTime;
	}
	
	public long getLastAction()
	{
		return lastStroke;
	}
	
	public void pressAndHoldCompleted()
	{
//		CStrokeController.no_notify_delete(CStrokeController.getCurrentUUID());
		CStrokeController.delete(CStrokeController.getCurrentUUID());
		
		////////////////////////
		if (this.activeGroup != 0)
		{
//			CGroupController.move_start(this.activeGroup);
			calico.inputhandlers.groups.CGroupScrapModeInputHandler.startDrag = true;
			
//			this.parentHandler.routeToHandler_actionPressed(CInputMode.SCRAP, this.lastPoint);
			if (CConnectorController.exists(this.activeGroup))
				CConnectorController.show_stroke_bubblemenu(this.activeGroup, false);
			else if (CGroupController.exists(this.activeGroup))
				CGroupController.show_group_bubblemenu(this.activeGroup);
			else if (CStrokeController.exists(this.activeGroup))
			{
				long potentialConnector = CStrokeController.getPotentialConnector(lastPoint.getPoint(), 20);
				if (potentialConnector > 0l)
					calico.controllers.CStrokeController.show_stroke_bubblemenu(potentialConnector, false);
				long potentialScrap = CStrokeController.getPotentialScrap(lastPoint.getPoint());
				if (potentialConnector == 0l && potentialScrap > 0l)
				{
					// {
					CStroke stroke = CStrokeController.strokes.get(potentialScrap);
					long previewScrap = stroke.createTemporaryScrapPreview(false);
					CGroupController.show_group_bubblemenu(previewScrap, PieMenuButton.SHOWON_SCRAP_CREATE, true);
				}
			}
			CCanvasStrokeModeInputHandler.deleteSmudge = true;			
//			CStrokeController.no_notify_delete(CStrokeController.getCurrentUUID());
//			CStrokeController.setCurrentUUID(0l);
		}
	}
	
	public Point getMouseDown()
	{
		return mouseDown;
	}
	
	public Point getMouseUp()
	{
		return mouseUp;
	}
	
	public void pressAndHoldAbortedEarly()
	{
		//Do nothing
	}
	
	public double getDraggedDistance()
	{
		if (lastStroke == 0l || !CStrokeController.exists(lastStroke))
			return java.lang.Double.MAX_VALUE;
		
		
		return CStrokeController.strokes.get(lastStroke).getLength();
	}
}
