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
package calico.plugins.iip.inputhandlers;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Timer;

import javax.swing.SwingUtilities;

import calico.CalicoDraw;
import calico.components.bubblemenu.BubbleMenu;
import calico.components.menus.ContextMenu;
import calico.components.piemenu.PieMenuButton;
import calico.controllers.CCanvasController;
import calico.inputhandlers.CalicoAbstractInputHandler;
import calico.inputhandlers.InputEventInfo;
import calico.networking.Networking;
import calico.networking.netstuff.CalicoPacket;
import calico.plugins.iip.IntentionalInterfacesClientPlugin;
import calico.plugins.iip.IntentionalInterfacesNetworkCommands;
import calico.plugins.iip.components.CIntentionCell;
import calico.plugins.iip.components.graph.IntentionGraph;
import calico.plugins.iip.components.piemenu.PieMenuTimerTask;
import calico.plugins.iip.components.piemenu.iip.CreateLinkButton;
import calico.plugins.iip.components.piemenu.iip.DeleteCanvasButton;
import calico.plugins.iip.components.piemenu.iip.SetCanvasTitleButton;
import calico.plugins.iip.components.piemenu.iip.UnpinCanvas;
import calico.plugins.iip.components.piemenu.iip.ZoomToBranchButton;
import calico.plugins.iip.components.piemenu.iip.ZoomToCenterRingButton;
import calico.plugins.iip.components.piemenu.iip.ZoomToClusterButton;
import calico.plugins.iip.controllers.CCanvasLinkController;
import calico.plugins.iip.controllers.CIntentionCellController;
import calico.plugins.iip.controllers.IntentionGraphController;
import edu.umd.cs.piccolo.util.PBounds;

/**
 * Custom <code>CalicoInputManager</code>handler for events related to CICs in the Intention View. The main Calico event
 * handling mechanism will determine whether input relates to a CIC by calling
 * <code>IntentionalInterfacesPerspective.getEventTarget()</code>. When that method returns a CIC, the associated input
 * event will be sent here.
 * 
 * The current behavior is to enter a canvas on tap, show a bubble menu for the CIC on press&hold, and move the CIC when
 * dragged more than 20 pixels.
 * 
 * @author Byron Hawkins
 */
public class CIntentionCellInputHandler extends CalicoAbstractInputHandler implements ContextMenu.Listener
{
	public static CIntentionCellInputHandler getInstance()
	{
		return INSTANCE;
	}

	private static final CIntentionCellInputHandler INSTANCE = new CIntentionCellInputHandler();

	private static final double DRAG_THRESHOLD = 20.0;
	

	private enum State
	{
		IDLE,
		PRESSED,
		ACTIVATED,
		DRAG,
		MENU,
		PIN;
	}

	/**
	 * Identifies the CIC which input is currently operating on, if any.
	 */
	private long currentCellId;

	/**
	 * State token, voluntarily protected under the <code>stateLock</code>.
	 */
	private State state = State.IDLE;
	/**
	 * Voluntary lock for the <code>state</code>.
	 */
	private final Object stateLock = new Object();

	/**
	 * Keeps the initial mouse position at the moment a drag was initiated. The value has no meaning when no drag is in
	 * progress.
	 */
	private Point mouseDragAnchor;
	/**
	 * Keeps the initial CIC position at the moment a drag was initiated. The value has no meaning when no drag is in
	 * progress.
	 */
	private Point2D cellDragAnchor;

	/**
	 * Time governing the display delay for the bubble menu.
	 */
	private final BubbleMenuTimer bubbleMenuTimer = new BubbleMenuTimer();
	
	private Point lastLocalMousePoint = null;

	private long mouseDownTime;

	private CIntentionCellInputHandler()
	{
		BubbleMenu.addListener(this);
	}

	/**
	 * Initiate the input sequence on <code>currentCellId</code>. The sequence will terminate on input release.
	 */
	public void setCurrentCellId(long currentCellId)
	{
		this.currentCellId = currentCellId;

//		CIntentionCellController.getInstance().getCellById(currentCellId).setHighlighted(true);
	}

	/**
	 * Get the CIC which is currently the subject of input, or <code>-1L</code> if no input sequence is presently active
	 * on a CIC.
	 */
	public long getActiveCell()
	{
		if (state == State.IDLE)
		{
			return -1L;
		}

		return currentCellId;
	}

	private void moveCurrentCell(Point destination, boolean local)
	{
		double xMouseDelta = (destination.x - mouseDragAnchor.x) / IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT).getScale();
		double yMouseDelta = (destination.y - mouseDragAnchor.y) / IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT).getScale();

		if (local)
		{
			CIntentionCellController.getInstance().moveCellLocal(currentCellId, cellDragAnchor.getX() + xMouseDelta, cellDragAnchor.getY() + yMouseDelta);
		}
		else
		{
			CIntentionCellController.getInstance().moveCell(currentCellId, cellDragAnchor.getX() + xMouseDelta, cellDragAnchor.getY() + yMouseDelta);
			Networking.send(CalicoPacket.getPacket(IntentionalInterfacesNetworkCommands.EXECUTE_II_EVENT_DISPATCHER_EVENTS, IntentionalInterfacesNetworkCommands.EXECUTE_II_EVENT_DISPATCHER_EVENTS));
		}
	}

	@Override
	public void actionDragged(InputEventInfo event)
	{
		lastLocalMousePoint = event.getPoint();
		synchronized (stateLock)
		{
			switch (state)
			{
				case ACTIVATED:
					if (event.getGlobalPoint().distance(mouseDragAnchor) >= DRAG_THRESHOLD)
					{
						state = State.DRAG;
						CIntentionCellController.getInstance().getCellById(currentCellId).moveToFront();
						CIntentionCellController.getInstance().getCellById(currentCellId).setDragging(true);
//						CIntentionCellController.getInstance().getCellById(currentCellId).setIsPinned(true);
//						Networking.send(IntentionalInterfacesNetworkCommands.CIC_SET_PIN, currentCellId, 1);
					}
					else
					{
						break;
					}
				case DRAG:
					CIntentionCell cell = CIntentionCellController.getInstance().getCellById(currentCellId);
					long potentialTargetCell = CIntentionCellController.getInstance().getCellAt(event.getPoint(), currentCellId);
					long targetCluster = IntentionGraph.getInstance().getClusterAt(event.getPoint());
					long originalRoot = CIntentionCellController.getInstance().getClusterRootCanvasId(cell.getCanvasId());
//					if (potentialTargetCell > 0l
//							&& potentialTargetCell != currentCellId
//							&& !CIntentionCellController.getInstance().isParent(
//									CIntentionCellController.getInstance().getCellById(potentialTargetCell).getCanvasId(), cell.getCanvasId()))
//					{
//						CIntentionCellController.getInstance().getCellById(currentCellId).setCellIcon(CIntentionCell.CellIconType.CREATE_LINK);
//					}
//					else 
						if (CIntentionCellController.getInstance().getCIntentionCellParent(cell.getCanvasId()) != originalRoot
							&& IntentionGraph.getInstance().ringContainsPoint(originalRoot, event.getGlobalPoint(), 0))
					{
						CIntentionCellController.getInstance().getCellById(currentCellId).setCellIcon(CIntentionCell.CellIconType.DELETE_LINK);
					}
					else
					{
						CIntentionCellController.getInstance().getCellById(currentCellId).setCellIcon(CIntentionCell.CellIconType.NONE);
					}
					
					moveCurrentCell(event.getGlobalPoint(), true);
//					calico.components.bubblemenu.BubbleMenu.updateContainerBounds();
					PBounds local = cell.getGlobalBounds();
					if (CCanvasController.exists(CCanvasController.getCurrentUUID()))	
						local =  new PBounds(CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getLayer().globalToLocal(local));
					BubbleMenu.moveIconPositions(cell.getBounds());
					
			}
			IntentionalInterfacesClientPlugin.executeEventDispatcherEvents();
		}
	}

	@Override
	public void actionPressed(InputEventInfo event)
	{
		lastLocalMousePoint = event.getPoint();
		mouseDownTime = System.currentTimeMillis();
		if (event.isLeftButtonPressed() || event.isRightButton())
		{
			synchronized (stateLock)
			{
				if (state == State.MENU)
					state = State.ACTIVATED;
				else
					state = State.PRESSED;
//				state = State.ACTIVATED;
			}

			mouseDragAnchor = event.getGlobalPoint();
			if (CIntentionCellController.getInstance().getCellById(currentCellId) == null)
				return;
			
			if (CIntentionCellController.getInstance().getCellById(currentCellId).pinImageContainsPoint(mouseDragAnchor))
			{
				CIntentionCellController.getInstance().getCellById(currentCellId).setPinHighlighted(true);
				state = State.PIN;
			}
			
			cellDragAnchor = CIntentionCellController.getInstance().getCellById(currentCellId).getLocation();

			Point2D point = IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.TOOLS).globalToLocal(event.getGlobalPoint());
			if (state == State.PRESSED && event.isLeftButton())
				bubbleMenuTimer.start(new Point((int) point.getX(), (int) point.getY()));
		}
	}

	@Override
	public void actionReleased(InputEventInfo event)
	{
		lastLocalMousePoint = event.getPoint();
		CIntentionCell cell = CIntentionCellController.getInstance().getCellById(currentCellId);
		if (cell == null)
			return;
		cell.setDragging(false);
		cell.setCellIcon(CIntentionCell.CellIconType.NONE);
		cell.setPinHighlighted(false);

		synchronized (stateLock)
		{
			switch (state)
			{
				case DRAG:
					moveCurrentCell(event.getGlobalPoint(), false);
					Point2D local = IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT).globalToLocal(new Point(event.getPoint()));
					final long clusterId = IntentionGraph.getInstance().getClusterAt(local);
//					long potentialTargetCell = CIntentionCellController.getInstance().getCellAt(event.getPoint(), currentCellId);
					long originalRoot = CIntentionCellController.getInstance().getClusterRootCanvasId(cell.getCanvasId());
					
					if (clusterId == originalRoot
							&& CIntentionCellController.getInstance().getCIntentionCellParent(cell.getCanvasId()) != originalRoot
							&& IntentionGraph.getInstance().ringContainsPoint(originalRoot, event.getPoint(), 0))
					{
						CCanvasLinkController.getInstance().createLink(originalRoot, cell.getCanvasId());
					}
					//the CIC has been moved to a new cluster
					else if (clusterId != 0l && clusterId != originalRoot)
					{
						if (cell.getIsPinned())
						{
							cell.setIsPinned(false);
							Networking.send(IntentionalInterfacesNetworkCommands.CIC_SET_PIN, currentCellId, 0);
						}
						
						int numChildrenInOriginal = IntentionGraph.getInstance().getNumBaseClusterChildren(originalRoot);
						int numChildrenInTarget = IntentionGraph.getInstance().getNumBaseClusterChildren(clusterId);
						CCanvasLinkController.getInstance().createLink(clusterId, cell.getCanvasId());
						if (numChildrenInOriginal == 1)
							IntentionGraph.getInstance().removeExtraCluster(originalRoot);
						if (numChildrenInTarget == 0 && numChildrenInOriginal > 1)
							SwingUtilities.invokeLater(
									new Runnable() { public void run() { 
										IntentionGraph.getInstance().createClusterIfNoEmptyClusterExists(clusterId);
									}});
							
					}
					else
					{
						if (!CIntentionCellController.getInstance().getCellById(currentCellId).getIsPinned())
						{
							CIntentionCellController.getInstance().getCellById(currentCellId).setIsPinned(true);
							Networking.send(IntentionalInterfacesNetworkCommands.CIC_SET_PIN, currentCellId, 1);
						}
					}
					state = State.MENU;
					break;
				case PRESSED:
					if (event.isRightButton())
					{
						pressAndHoldCompleted();
						state = State.MENU;
					}
					else if (event.getGlobalPoint().distance(mouseDragAnchor) < DRAG_THRESHOLD
							&& cell.getVisible())
					{
						CCanvasController.loadCanvas(cell.getCanvasId());
					}
					else
					{
						Point2D localC = IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT).globalToLocal(new Point(event.getPoint()));
						long clusterIdC = IntentionGraph.getInstance().getClusterAt(localC);
						if (clusterIdC > 0)
							IntentionGraph.getInstance().setFocusToCluster(clusterIdC, true);
					}
					break;
				case PIN:
//					if (cell.pinImageContainsPoint(event.getGlobalPoint()))
					cell.setIsPinned(false);
					Networking.send(IntentionalInterfacesNetworkCommands.CIC_SET_PIN, currentCellId, 0);
					break;
				case ACTIVATED:
					if (System.currentTimeMillis() - mouseDownTime < 300l
							&& event.getGlobalPoint().distance(mouseDragAnchor) < DRAG_THRESHOLD
							&& cell.getVisible())
						CCanvasController.loadCanvas(cell.getCanvasId());
					else
						state = State.MENU;
					break;
			}

			if (state != State.MENU)
			{
				state = State.IDLE;
				cell.setHighlighted(false);
			}
			
		}
	}

	@Override
	public void menuCleared(ContextMenu menu)
	{
		if ((state == State.MENU) && (menu == ContextMenu.BUBBLE_MENU)
				&& CIntentionCellController.getInstance().getCellById(currentCellId) != null)
		{
			state = State.IDLE;
			CIntentionCellController.getInstance().getCellById(currentCellId).setHighlighted(false);
		}
	}

	@Override
	public void menuDisplayed(ContextMenu menu)
	{
	}

	/**
	 * Show the bubble menu after a press&hold delay of 200ms, unless the input is released or dragged more than 20
	 * pixels before the timer expires.
	 * 
	 * @author Byron Hawkins
	 */
	private class BubbleMenuTimer extends Timer
	{
		private Point point;

		void start(Point point)
		{
			this.point = point;

			schedule(new Task(), 200L);
		}

		private class Task extends PieMenuTimerTask
		{
			@Override
			public void run()
			{
				synchronized (stateLock)
				{
					CIntentionCell cell = CIntentionCellController.getInstance().getCellById(currentCellId);
					if (state == State.PRESSED
							&& cell.contains(lastLocalMousePoint))
					{
						startAnimation(IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.TOOLS), point);
					}
				}
			}

			@Override
			protected void animationCompleted()
			{
				synchronized (stateLock)
				{
					pressAndHoldCompleted();
				}
			}




		}
	}
	
	private void pressAndHoldCompleted() {
		CIntentionCell cell = CIntentionCellController.getInstance().getCellById(currentCellId);
		if (state == State.PRESSED
				&& cell.contains(lastLocalMousePoint))
		{
			state = State.MENU;
			
			cell.showBubbleMenu();

			state = State.ACTIVATED;
//				state = State.DRAG;
//				CIntentionCellController.getInstance().getCellById(currentCellId).moveToFront();
//				CIntentionCellController.getInstance().getCellById(currentCellId).setDragging(true);
////				CIntentionCellController.getInstance().getCellById(currentCellId).setIsPinned(true);
////				Networking.send(IntentionalInterfacesNetworkCommands.CIC_SET_PIN, currentCellId, 1);
		}
	}


}
