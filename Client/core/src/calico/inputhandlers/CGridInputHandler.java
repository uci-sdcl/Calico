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

import java.awt.Point;
import java.awt.Polygon;
import java.util.Date;
import java.util.Random;

import calico.Calico;
import calico.CalicoOptions;
import calico.components.grid.CGrid;
import calico.components.piemenu.PieMenu;
import calico.components.piemenu.grid.CopyCanvasButton;
import calico.components.piemenu.grid.CutCanvasButton;
import calico.components.piemenu.grid.DeleteCanvasButton;
import calico.components.piemenu.grid.LockCanvasButton;
import calico.components.piemenu.grid.UnlockCanvasButton;
import calico.controllers.CCanvasController;
import calico.modules.StatusMessage;
import calico.utils.Geometry;
import calico.utils.Ticker;



// implements PenListener
public class CGridInputHandler extends CalicoAbstractInputHandler
	implements PressAndHoldAction
{
	public final static long inputHandlerUUID = 0L;
	
	//private Point viewPortDragPoint=null;
	private boolean draggingViewpoint=false;
	public boolean triggerLoadCanvas = false;
	
	//for double click
	private long prevClickTime = -1l;
	private Point prevClickPoint;
	private Point mouseDown;
	private long mousePressed;
	
	private long lastPressedTime;
	private long lastReleasedTime;
	
	private long currentAction = 0l;
	private Point lastPoint = null;
	
	private Polygon dragHistory;
	
	private CalicoAbstractInputHandler.MenuTimer menuTimer;
	
	public long mousePressedAt()
	{
		return mousePressed;
	}
	
	public void actionPressed(InputEventInfo ev)
	{
		if (PieMenu.isPieMenuActive())
			return;
		
		triggerLoadCanvas = true;
		mouseDown = ev.getPoint();
		mousePressed = (new Date()).getTime();
		currentAction = (new Random()).nextLong();
		dragHistory = new Polygon();
		dragHistory.addPoint(ev.getPoint().x, ev.getPoint().y);
		
		if (!isClickValid(ev))
			return;
				
		if(ev.getButton()==InputEventInfo.BUTTON_LEFT){
//			CGrid.getInstance().moveViewPortToPoints(ev.getPoint());
			mouseDown = ev.getPoint();
			long canvasClicked = CGrid.getCanvasAtPoint( ev.getGlobalPoint() );
//			LoadCanvasTimerTicker timer = new LoadCanvasTimerTicker(this, canvasClicked, mousePressed);
//			Ticker.scheduleIn(CalicoOptions.core.hold_time, timer);
			
			if (menuTimer != null)
				menuTimer.terminate();
			menuTimer = new CalicoAbstractInputHandler.MenuTimer(this, currentAction, CalicoOptions.core.hold_time/2, CalicoOptions.core.max_hold_distance, 0, ev.getPoint(), 0, CGrid.getInstance().getLayer());
			Ticker.scheduleIn(CalicoOptions.core.hold_time, menuTimer);
		}
		
		lastPressedTime = (new Date()).getTime();
		lastPoint = ev.getPoint();
		
		
	}

	public void actionDragged(InputEventInfo ev)
	{
		 
		if(CGrid.draggingCell){			
			CGrid.getInstance().moveDraggedCell(ev.getX(), ev.getY());
			//send message to execute the action once group is dropped
		}
		lastPoint = ev.getPoint();
		dragHistory.addPoint(ev.getPoint().x, ev.getPoint().y);
	}

	public void actionReleased(InputEventInfo ev)
	{	
		mousePressed = 0l;
		currentAction = 0;
		
		//If its a left click: dive into viewport or single canvas
		//if we were dragging a cell we will remove it and then check which action to execute
		if(CGrid.draggingCell){			
			
			//check which action to execute (copy/move)
			if(CGrid.canvasAction!=CGrid.NO_ACTION){
				CGrid.getInstance().execActionCanvas(ev);				
			}
			CGrid.getInstance().removeDraggedCell();
		}
		else if(ev.getButton()==InputEventInfo.BUTTON_RIGHT){
			openMenu(ev.getGlobalPoint());
		}
		else if (triggerLoadCanvas)
		{
			long canvasClicked = CGrid.getCanvasAtPoint( mouseDown );
			if (canvasClicked != 0l)
				CCanvasController.loadCanvas(canvasClicked);
		}

		if (isClickValid(ev))
			return;
		

		
		prevClickTime = (new Date()).getTime();
		prevClickPoint = ev.getPoint();
		
		lastReleasedTime = (new Date()).getTime();
		lastPoint = null;
		
		
	}//

	private boolean isClickValid(InputEventInfo ev) {
		if(Calico.isGridLoading)
		{
			StatusMessage.popup("The grid is loading, please wait.");
			return false;
		}

		if(CGrid.getInstance().isPointOnMenuBar(ev.getPoint())) {
			//if (mousePressed != 0)
			getMenuBarClick(ev, ev.getPoint());
			return false;
		}
		
		return true;
	}
	
	private void openMenu(Point point)
	{
		triggerLoadCanvas = false;
		long canvasClicked = CGrid.getCanvasAtPoint( point );
		boolean lockStatus = CCanvasController.canvasdb.get(canvasClicked).getLockValue();
		PieMenu.displayPieMenu(point, 
				new UnlockCanvasButton(),
				new LockCanvasButton(),
				new CutCanvasButton(),
				new CopyCanvasButton(),	
				new DeleteCanvasButton()
			);
		
		mousePressed = 0l;
		currentAction = 0;
	}
	
	private void getMenuBarClick(InputEventInfo event, Point point)
	{
		 
		if(CGrid.getInstance().isPointOnMenuBar(point))
		{
			CGrid.getInstance().clickMenuBar(event, point);
		}
	}
	
	@Override
	public void pressAndHoldCompleted() {
		// Do nothing
		
	}

	@Override
	public long getLastAction() {
		
		return currentAction;
		
	}

	@Override
	public Point getLastPoint() {
		
		return lastPoint;
		
	}

	@Override
	public Point getMouseDown() {
		
		return mouseDown;
	}

	@Override
	public Point getMouseUp() {

		return null;
	}

	@Override
	public void openMenu(long potScrap, long group, Point point) {
		
		this.openMenu(mouseDown);
		
	}
	
	@Override
	public void pressAndHoldAbortedEarly()
	{
	}
	
	@Override
	public double getDraggedDistance()
	{
		if (dragHistory == null)
			return java.lang.Double.MAX_VALUE;
		
		return Geometry.getPolygonLength(dragHistory);
	}
	
	
}
