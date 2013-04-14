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
package calico.inputhandlers;

import java.awt.*;

import calico.*;
import calico.components.*;
import calico.components.bubblemenu.BubbleMenu;
import calico.components.piemenu.*;
import calico.controllers.CGroupController;
import calico.controllers.CStrokeController;
import calico.input.CInputManager;
import calico.input.CInputMode;
import calico.inputhandlers.groups.*;
import calico.modules.*;
import calico.networking.*;
import calico.networking.netstuff.*;

import java.awt.Color;
import java.awt.geom.*;
import java.util.*;

import org.apache.log4j.Logger;

import edu.umd.cs.piccolo.*;
import edu.umd.cs.piccolo.event.*;
import edu.umd.cs.piccolo.nodes.*;
import edu.umd.cs.piccolo.util.*;
import edu.umd.cs.piccolox.nodes.PLine;


public class CGroupInputHandler extends CalicoAbstractInputHandler
{	
	private long uuid = 0L;
	
	
	private CGroupExpertModeInputHandler modehandler_expert = null;
	private CGroupScrapModeInputHandler modehandler_scrap = null;
	private CGroupArrowModeInputHandler modehandler_arrow = null;
	private CGroupEraseModeInputHandler modehandler_delete = null;
	private CGroupStrokeModeInputHandler modehandler_stroke = null;
	private CGroupPointerModeInputHandler modehandler_pointer = null;

	/*
	private static final double DRAG_THRESHOLD = 10.0;
	
	
	private InputEventInfo lastEvent = null;

	private boolean hasStartedGroup = false;
	private boolean hasStartedBge = false;
	private boolean isRCMove = false;// is right click move?
	private boolean weDidSomething = false;
	
	
	private boolean serverNotifiedOfMove = false;

	private Point pressPoint = null;
	
	private Point lastMovePoint = new Point(0,0);
	
	
	
	private Timer menuTimer = null;
	
	private class MenuTimer extends TimerTask
	{
		private Point point = null;
		
		public MenuTimer(Point mp)
		{
			point = mp;
		}
		
		public void run()
		{
			logger.debug("MENU TIMER RUN"+point.toString());
			
			PieMenu.displayPieMenu(point, 
					new GroupDeleteButton(uuid), 
					new GroupDropButton(uuid),
					new GroupSetPermanentButton(uuid),
					new ArrowSetAnchorButton(point)
			);
			CCanvasController.canvasdb.get(CGroupController.groupdb.get(uuid).getCanvasUID()).repaint();
			
			logger.debug("MENU TIMER RUN DONE "+pressPoint.toString());
			this.cancel();
		}
	
	}
	
	private boolean hasNotPassedThreshold = true;
	

	private Polygon draggedCoords = null;
	*/
	
	public CGroupInputHandler(long u)
	{
		uuid = u;
		
		modehandler_expert = new CGroupExpertModeInputHandler(uuid, this);
		modehandler_scrap = new CGroupScrapModeInputHandler(uuid, this);

		modehandler_arrow = new CGroupArrowModeInputHandler(uuid, this);// NEEDS A REAL HANDLER
		modehandler_delete = new CGroupEraseModeInputHandler(uuid, this);
		modehandler_stroke = new CGroupStrokeModeInputHandler(uuid, this);
		modehandler_pointer = new CGroupPointerModeInputHandler(uuid, this);
		
	}

	public void actionPressed(InputEventInfo e)
	{
		CalicoInputManager.lockInputHandler(uuid);
		e.group = uuid;
		
		if(CalicoOptions.canvas.lowquality_on_interaction)
		{
			//CCanvasController.canvasdb.get(canvas_uid).setInteracting(true);
		}
		
		CGroupController.checkToRemoveLastTempGroup(this.uuid);
		
		switch(CalicoDataStore.Mode)
		{
			case EXPERT:modehandler_expert.actionPressed(e);break;
			case ARROW:modehandler_arrow.actionPressed(e);break;
			case SCRAP:modehandler_scrap.actionPressed(e);break;
			case DELETE:modehandler_delete.actionPressed(e);break;
			case STROKE:modehandler_stroke.actionPressed(e);break;
			case POINTER:modehandler_pointer.actionPressed(e);break;
		}
	}//actionPressed

	public void actionDragged(InputEventInfo e)
	{
		e.group = uuid;
		CalicoInputManager.group = uuid;
		switch(CalicoDataStore.Mode)
		{
			case EXPERT:modehandler_expert.actionDragged(e);break;
			case ARROW:modehandler_arrow.actionDragged(e);break;
			case SCRAP:modehandler_scrap.actionDragged(e);break;
			case DELETE:modehandler_delete.actionDragged(e);break;
			case STROKE:modehandler_stroke.actionDragged(e);break;
			case POINTER:modehandler_pointer.actionDragged(e);break;
		}
	}//actionDragged
	
	public void actionScroll(InputEventInfo e)
	{
		e.group = uuid;
		CalicoInputManager.group = uuid;
		switch(CalicoDataStore.Mode)
		{
			case EXPERT:modehandler_expert.actionScroll(e);break;
			case ARROW:modehandler_arrow.actionScroll(e);break;
			case SCRAP:modehandler_scrap.actionScroll(e);break;
			case DELETE:modehandler_delete.actionScroll(e);break;
			case STROKE:modehandler_stroke.actionScroll(e);break;
			case POINTER:modehandler_pointer.actionScroll(e);break;
		}
	}//actionScroll
	

	public void actionReleased(InputEventInfo e)
	{
		CalicoInputManager.unlockHandlerIfMatch(uuid);
		e.group = uuid;
		CalicoInputManager.group = 0l;
		if(CalicoOptions.canvas.lowquality_on_interaction)
		{
			//CCanvasController.canvasdb.get(canvas_uid).setInteracting(false);
		}
		
		
		routeToHandler_actionReleased(CalicoDataStore.Mode, e);
	}//actionReleased
	
	
	public void routeToHandler_actionDragged(CInputMode modeFlag, InputEventInfo e)
	{
		switch(modeFlag)
		{
			case EXPERT:modehandler_expert.actionDragged(e);break;
			case ARROW:modehandler_arrow.actionDragged(e);break;
			case SCRAP:modehandler_scrap.actionDragged(e);break;
			case DELETE:modehandler_delete.actionDragged(e);break;
			case STROKE:modehandler_stroke.actionDragged(e);break;
			case POINTER:modehandler_pointer.actionDragged(e);break;
		}
	}
	
	public void routeToHandler_actionPressed(CInputMode modeFlag, InputEventInfo e)
	{
		switch(modeFlag)
		{
			case EXPERT:modehandler_expert.actionPressed(e);break;
			case ARROW:modehandler_arrow.actionPressed(e);break;
			case SCRAP:modehandler_scrap.actionPressed(e);break;
			case DELETE:modehandler_delete.actionPressed(e);break;
			case STROKE:modehandler_stroke.actionPressed(e);break;
			case POINTER:modehandler_pointer.actionPressed(e);break;
		}
	}
	
	public void routeToHandler_actionReleased(CInputMode modeFlag, InputEventInfo e)
	{
		switch(modeFlag)
		{
			case EXPERT:modehandler_expert.actionReleased(e);break;
			case ARROW:modehandler_arrow.actionReleased(e);break;
			case SCRAP:modehandler_scrap.actionReleased(e);break;
			case DELETE:modehandler_delete.actionReleased(e);break;
			case STROKE:modehandler_stroke.actionReleased(e);break;
			case POINTER:modehandler_pointer.actionReleased(e);break;
		}
	}
	
	
	/**
	* Refactored so that the originating group can choose what to do. 
	* 
	* Will likely be refactored later so that this action can be decided using compositional notations
   */
  public void actionStrokeToAnotherGroup(long strokeUID, long targetGroup) {
	  CStrokeController.show_stroke_bubblemenu(strokeUID, false);
  }
	
	/*
	public void actionPressed(InputEventInfo e)
	{
		if(CArrowController.getOutstandingAnchorPoint()!=null)
		{
			return;
		}
		
		
		// DO NOT SET "setHandled = true
		//e.setHandled(true);
		lastEvent = e;
		CGroupController.no_notify_bold(uuid);
		
		hasStartedBge = false;
		hasStartedGroup = false;
		
		// Check if right click is for movement
		if( CGroupController.groupdb.get(uuid).getRightClickMode()==CGroup.RIGHTCLICK_MODE_MOVE)
		{
			isRCMove = true;
			serverNotifiedOfMove = false;
		}
		else
		{
			isRCMove = false;
		}
		
		weDidSomething = false;
		CalicoInputManager.lockInputHandler(uuid);
		
		
		draggedCoords = new Polygon();
		draggedCoords.addPoint(e.getX(), e.getY());
		
		if(e.isRightButtonPressed())
		{
			hasNotPassedThreshold = true;
			//menuTimer = new Timer("CGroupMenuTimer",true);
			//menuTimer.schedule(new MenuTimer(e.getPoint()), 1500);
		}
		pressPoint = e.getPoint();
	}


	public void actionDragged(InputEventInfo e)
	{
		if(CArrowController.getOutstandingAnchorPoint()!=null)
		{
			return;
		}
		
		

		boolean isRightButton = (InputEventInfo.BUTTON_RIGHT==e.getButton());
		boolean isLeftButton = (InputEventInfo.BUTTON_LEFT==e.getButton());
		
		
		// Ignore the distance if we are still within the presspoint
		if(isRightButton && hasNotPassedThreshold && pressPoint.distance(e.getPoint())<CalicoOptions.getFloat("scrap.drag.threshold"))
		{
			draggedCoords.addPoint(e.getX(), e.getY());
			return;
			//menuTimer.cancel();
			
		}
		else
		{
			hasNotPassedThreshold = false;
		}
		
		
		int x = e.getX();
		int y = e.getY();
		

		weDidSomething = true;
		if(e.isMiddleButtonPressed())
		{

			draggedCoords.addPoint(e.getX(), e.getY());
			long[] bgelist = CGroupController.groupdb.get(uuid).getBGElementList();
			if(bgelist.length>0)
			{
				for(int i=0;i<bgelist.length;i++)
				{
					if(BGElementController.exists(bgelist[i]) && BGElement.countIntersections(BGElementController.bgelements.get(bgelist[i]).getPolygon(), draggedCoords )>0)
					{
						BGElementController.delete(bgelist[i]);
					}
				}
			}
		}
		else if(isLeftButton && hasStartedBge)
		{
			BGElementController.append(BGElementController.getCurrentUUID(), x, y);
		}
		else if(isLeftButton && !hasStartedBge)
		{
			long bguuid = Calico.uuid();
			BGElementController.setCurrentUUID(bguuid);
			BGElementController.start(bguuid, CCanvasController.getCurrentUUID(), uuid);
			BGElementController.append(bguuid, pressPoint.x, pressPoint.y);
			BGElementController.append(bguuid, x, y);
			hasStartedBge = true;
		}
		else if(isRCMove && e.getButton()==InputEventInfo.BUTTON_RIGHT)
		{
			if(!serverNotifiedOfMove)
			{
				Networking.send(CalicoPacket.getPacket(NetworkCommand.GROUP_MOVE_START,uuid));
				serverNotifiedOfMove = true;
				
			}
			
			CalicoInputManager.lockInputHandler(uuid);
			Point delta = e.getDelta(lastEvent);
			lastMovePoint.translate(delta.x, delta.y);
			CGroupController.move(uuid, delta.x, delta.y);
			lastEvent = e;
		}
		
		else if(!isRCMove && isRightButton && hasStartedGroup)
		{
			CGroupController.append(CGroupController.getCurrentUUID(), x, y);
		}
		else if(!isRCMove && isRightButton && !hasStartedGroup)
		{
			long cguuid = Calico.uuid();
			CGroupController.start(cguuid, CCanvasController.getCurrentUUID(),0L, x, y);
			CGroupController.setCurrentUUID(cguuid);
			
			hasStartedGroup = true;
		}
		else if(e.getButton()==InputEventInfo.BUTTON_MIDDLE)
		{
			// Middle
			weDidSomething = false;
		}
		else
		{
			// left
			weDidSomething = false;
		}
	}


	public void actionReleased(InputEventInfo e)
	{
		//TESTING
		// DO NOT SET "setHandled = true
		//e.setHandled(true);
		//System.out.println("UNLOCKING HANDLER");
		
		//menuTimer.cancel();
		
		if(CArrowController.getOutstandingAnchorPoint()!=null)
		{
			Point start = CArrowController.getOutstandingAnchorPoint();
			Point end = e.getPoint();
			long startGroupUID = CalicoInputManager.getSmallestGroupAtPoint(start.x, start.y);
			long endGroupUID = CalicoInputManager.getSmallestGroupAtPoint(end.x, end.y);
			if(endGroupUID==startGroupUID && endGroupUID!=0L)
			{
				return;
			}

			long curCanvasUID = CCanvasController.getCurrentUUID();
			int startType = CArrow.TYPE_GROUP;
			int endType = CArrow.TYPE_GROUP;
			
			if(startGroupUID==0L)
			{
				startGroupUID = curCanvasUID;
				startType = CArrow.TYPE_CANVAS;
			}
			if(endGroupUID==0L)
			{
				endGroupUID = curCanvasUID;
				endType = CArrow.TYPE_CANVAS;
			}

			// ARROW!
			CArrowController.start(Calico.uuid(), curCanvasUID, 
					startType, startGroupUID, start,
					endType, endGroupUID, end
				);
		
			CArrowController.setOutstandingAnchorPoint(null);

			//CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).menuBar.redrawArrowIndicator();
			
			return;
		}
		
		CalicoInputManager.unlockHandlerIfMatch(uuid);
		CGroupController.no_notify_unbold(uuid);

		if(weDidSomething)
		{
			int x = e.getX();
			int y = e.getY();
			
			if(e.getButton() == InputEventInfo.BUTTON_LEFT && hasStartedBge)
			{
				long bguid = BGElementController.getCurrentUUID();
				BGElementController.append(bguid, x, y);
				BGElementController.finish(bguid);
				hasStartedBge = false;
				
				// Is the pen able to create arrows?
				if(CalicoOptions.getBoolean("arrow.enable_pen_create"))
				{
					// CHECK TO SEE IF ITS AN ARROW
					Polygon poly = BGElementController.bgelements.get(bguid).getPolygon();
					long endGroupUID = CalicoInputManager.getSmallestGroupAtPoint(poly.xpoints[poly.npoints-1], poly.ypoints[poly.npoints-1]);
					if(endGroupUID!=uuid)
					{
						BGElementController.delete(bguid);
						
						// ARROW!
						CArrowController.start(Calico.uuid(), CCanvasController.getCurrentUUID(), 
								CArrow.TYPE_GROUP, uuid, new Point(poly.xpoints[0], poly.ypoints[0]),
								CArrow.TYPE_GROUP, endGroupUID, new Point(poly.xpoints[poly.npoints-1], poly.ypoints[poly.npoints-1])
							);
					}
				}
				
			}
			else if(e.getButton()==InputEventInfo.BUTTON_RIGHT && hasStartedGroup)
			{
				CGroupController.append(CGroupController.getCurrentUUID(), x, y);
				CGroupController.finish(CGroupController.getCurrentUUID());
				CGroupController.setCurrentUUID(0L);
				hasStartedGroup = false;
			}
		
			if(e.getButton()==InputEventInfo.BUTTON_RIGHT)
			{
				// Are we done drawing/moving?
				if(isRCMove)
				{
					// Finished move
					Networking.send(CalicoPacket.getPacket(NetworkCommand.GROUP_MOVE_END,
						uuid,
						lastMovePoint.x,
						lastMovePoint.y
					));
					
					lastMovePoint = new Point(0,0);
				}
				
				CGroupController.groupdb.get(uuid).resetRightClickMode();
			}
			
			
			if(!CGroupController.groupdb.get(uuid).isPermanent())
			{
				CGroupController.drop(uuid);
			}
			
			
		}
		else if(!weDidSomething && e.getButton()==InputEventInfo.BUTTON_RIGHT)
		{
			PieMenuButton button = null;
			if(CalicoOptions.getInt("group.default_rightclick_mode")==CGroup.RIGHTCLICK_MODE_DRAWGROUP)
			{
				button = new GroupMoveButton(uuid);
			}
			else
			{
				button = new GroupDrawButton(uuid);
			}
			
			PieMenu.displayPieMenu(e.getPoint(), 
					new GroupDeleteButton(uuid), 
					button, 
					new GroupDropButton(uuid),
					new GroupSetPermanentButton(uuid),
					new GroupRectifyButton(uuid),
					new GroupCirclifyButton(uuid),
					new GroupDuplicateButton(uuid),
					new GroupChangeChildrenColorButton(uuid),
					new ArrowSetAnchorButton(e.getPoint()),
					new GroupCopyButton(uuid)
			);
		}
		
		
	}*/	

}
