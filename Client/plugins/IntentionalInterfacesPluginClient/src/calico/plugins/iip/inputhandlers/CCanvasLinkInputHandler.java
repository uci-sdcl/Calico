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
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Timer;

import calico.components.bubblemenu.BubbleMenu;
import calico.components.menus.ContextMenu;
import calico.components.piemenu.PieMenu;
import calico.components.piemenu.PieMenuButton;
import calico.controllers.CCanvasController;
import calico.inputhandlers.CalicoAbstractInputHandler;
import calico.inputhandlers.InputEventInfo;
import calico.plugins.iip.components.CCanvasLink;
import calico.plugins.iip.components.CCanvasLinkArrow;
import calico.plugins.iip.components.graph.IntentionGraph;
import calico.plugins.iip.components.piemenu.DeleteLinkButton;
import calico.plugins.iip.components.piemenu.PieMenuTimerTask;
import calico.plugins.iip.components.piemenu.SetLinkLabelButton;
import calico.plugins.iip.components.piemenu.iip.CreateIntentionArrowPhase;
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
 * Custom <code>CalicoInputManager</code>handler for events related to arrows in the Intention View. The main Calico
 * event handling mechanism will determine whether input relates to an arrow by calling
 * <code>IntentionalInterfacesPerspective.getEventTarget()</code>. When that method returns an arrow, the associated
 * input event will be sent here.
 * 
 * The only supported operation is press&hold on an arrow to obtain a pie menu for it. An arrow is highlighted on
 * press, and the menu appears after the timer's duration of 200ms expires.
 * 
 * Moving arrows by dragging either endpoint has been supported in past versions.
 * 
 * @author Byron Hawkins
 */
public class CCanvasLinkInputHandler extends CalicoAbstractInputHandler implements ContextMenu.Listener
{
	public static CCanvasLinkInputHandler getInstance()
	{
		return INSTANCE;
	}

	private static final CCanvasLinkInputHandler INSTANCE = new CCanvasLinkInputHandler();

	private static final double MOVE_THRESHOLD = 10.0;

	private enum State
	{
		IDLE,
		ACTIVATED,
		DRAG,
		PIE;
	}

	/**
	 * Identifies the currently pressed link.
	 */
	private long currentLinkId;

	/**
	 * State token, protected voluntarily under <code>stateLock</code>.
	 */
	private State state = State.IDLE;
	/**
	 * Voluntary lock for <code>state</code>.
	 */
	private final Object stateLock = new Object();

	/**
	 * Governs the press&hold delay for the pie menu.
	 */
	private final PieMenuTimer pieMenuTimer = new PieMenuTimer();

	/**
	 * Delete button in the pie menu for arrows.
	 */
	private final DeleteLinkButton deleteLinkButton = new DeleteLinkButton();
	/**
	 * Button for setting link labels, which appears in the pie menu for arrows.
	 */
	private final SetLinkLabelButton setLinkLabelButton = new SetLinkLabelButton();

	/**
	 * Identifies the pixel position of the mouse at the time drag was initiated. Obsolete.
	 */
	private Point mouseDragAnchor;

	/**
	 * State flag indicating whether the currently active press occurred nearest the head or tail of the arrow. This
	 * flag has no meaning at times when no arrow input sequence is in progress.
	 */
	private boolean isNearestSideA;
	
	private static final int BUBBLE_MENU_TYPE_ID = BubbleMenu.registerType(new BubbleMenuComponentType());

	private CCanvasLinkInputHandler()
	{
		PieMenu.addListener(this);
	}

	/**
	 * Activate the input sequence for the arrow representing <code>currentLinkId</code>, with initial mouse contact at
	 * <code>point</code>.
	 */
	public void setCurrentLinkId(long currentLinkId, Point point)
	{
		this.currentLinkId = currentLinkId;

		CCanvasLink link = CCanvasLinkController.getInstance().getLinkById(currentLinkId);
		isNearestSideA = CCanvasLinkController.getInstance().isNearestSideA(currentLinkId, point);

		deleteLinkButton.setContext(link);
		setLinkLabelButton.setContext(link);

		IntentionGraphController.getInstance().getArrowByLinkId(currentLinkId).setHighlighted(true);
	}

	/**
	 * Get the link that the current input sequence is operating on, <code>-1L</code> if no arrow input sequence is in
	 * progress.
	 */
	public long getActiveLink()
	{
		if (state == State.IDLE)
		{
			return -1L;
		}

		return currentLinkId;
	}

	@Override
	public void actionDragged(InputEventInfo ev)
	{
		// no moving arrows anymore
	}

	@Override
	public void actionPressed(InputEventInfo event)
	{
		if (event.isLeftButtonPressed())
		{
			synchronized (stateLock)
			{
				state = State.ACTIVATED;
			}

			mouseDragAnchor = event.getGlobalPoint();

			Point2D point = IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.TOOLS).globalToLocal(event.getGlobalPoint());
			pieMenuTimer.start(new Point((int) point.getX(), (int) point.getY()));
		}
	}

	@Override
	public void actionReleased(InputEventInfo event)
	{
		synchronized (stateLock)
		{
			state = State.IDLE;
		}
		IntentionGraphController.getInstance().getArrowByLinkId(currentLinkId).setHighlighted(false);
	}

	@Override
	public void menuCleared(ContextMenu menu)
	{
		if ((state == State.PIE) && (menu == ContextMenu.PIE_MENU))
		{
			state = State.IDLE;

			CCanvasLinkArrow arrow = IntentionGraphController.getInstance().getArrowByLinkId(currentLinkId);
			if (arrow != null)
			{
				arrow.setHighlighted(false);
			}
		}
	}

	@Override
	public void menuDisplayed(ContextMenu menu)
	{
	}

	/**
	 * Displays the pie menu after press is held for 200ms.
	 * 
	 * @author Byron Hawkins
	 */
	private class PieMenuTimer extends Timer
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
					if (state == State.ACTIVATED)
					{
						state = State.PIE;

						startAnimation(IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.TOOLS), point);
					}
				}
			}

			@Override
			protected void animationCompleted()
			{
				if (state == State.PIE)
				{
					//getActiveLink;
					
					long anchorACanvas = CCanvasLinkController.getInstance().getLinkById(getActiveLink()).getAnchorA().getCanvasId();;
					boolean isAnchorACanvasRootCanvas = CIntentionCellController.getInstance().isRootCanvas(anchorACanvas);
					
					if (!isAnchorACanvasRootCanvas)
						BubbleMenu.displayBubbleMenu(CCanvasLinkInputHandler.getInstance().getActiveLink(), 
								true, BUBBLE_MENU_TYPE_ID, deleteLinkButton);
//						PieMenu.displayPieMenu(point, setLinkLabelButton, deleteLinkButton);
//					else
//						PieMenu.displayPieMenu(point, setLinkLabelButton);
				}
			}
		}
	}
	
	/**
	 * Integration point for a CICLink with the bubble menu.
	 * 
	 * @author Byron Hawkins
	 */
	private static class BubbleMenuComponentType implements BubbleMenu.ComponentType
	{
		@Override
		public PBounds getBounds(long uuid)
		{
			CCanvasLink link = CCanvasLinkController.getInstance().getLinkById(CCanvasLinkInputHandler.getInstance().getActiveLink());
			final Line2D hitTestLink = new Line2D.Double();
			hitTestLink.setLine(
					IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT).localToGlobal(new Point(link.getAnchorA().getPoint())),
					IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT).localToGlobal(new Point(link.getAnchorB().getPoint())));
			PBounds bounds;
			
			bounds = new PBounds(hitTestLink.getBounds2D());
			return bounds;
		}

		@Override
		public void highlight(boolean b, long uuid)
		{
//			if ( CCanvasLinkController.getInstance().getLinkById(uuid) != null)
//				CCanvasLinkInputHandler.getInstance().high(b, uuid);
		}

		@Override
		public int getButtonPosition(String buttonClassname)
		{
			if (buttonClassname.equals(DeleteLinkButton.class.getName()))
			{
				return 1;
			}
			
			
			return 0;
		}
	}
}
