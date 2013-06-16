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

import javax.swing.SwingUtilities;

import calico.CalicoDraw;
import calico.CalicoOptions;
import calico.controllers.CCanvasController;
import calico.controllers.CHistoryController;
import calico.controllers.CCanvasController.HistoryFrame;
import calico.inputhandlers.CalicoAbstractInputHandler;
import calico.inputhandlers.InputEventInfo;
import calico.inputhandlers.PressAndHoldAction;
import calico.inputhandlers.CalicoAbstractInputHandler.MenuTimer;
import calico.perspectives.CalicoPerspective;
import calico.plugins.iip.components.canvas.CanvasTitleDialog;
import calico.plugins.iip.components.canvas.CanvasTitleDialog.Action;
import calico.plugins.iip.components.graph.CIntentionTopology;
import calico.plugins.iip.components.graph.IntentionGraph;
import calico.plugins.iip.components.graph.CIntentionTopology.Cluster;
import calico.plugins.iip.components.graph.IntentionGraph.Layer;
import calico.plugins.iip.controllers.CIntentionCellController;
import calico.utils.Ticker;
import edu.umd.cs.piccolo.PLayer;

/**
 * Custom <code>CalicoInputManager</code>handler for events related to open space in the Intention View. The main Calico
 * event handling mechanism will determine whether input relates to the view by calling
 * <code>IntentionalInterfacesPerspective.getEventTarget()</code>. When that method returns the Intention View, the
 * associated input event will be sent here.
 * 
 * The only supported operation is to pan the Intention View by dragging it.
 * 
 * @author Byron Hawkins
 */
public class IntentionGraphInputHandler extends CalicoAbstractInputHandler implements PressAndHoldAction
{
	private enum State
	{
		IDLE,
		PAN,
		BUTTON;
	}

	long lastAction = 0;
	private State state = State.IDLE;
	private Point lastMouse, mouseDown, mouseUp;

	@Override
	public void actionPressed(InputEventInfo event)
	{
		lastAction = 0;
		lastMouse = event.getPoint();
		mouseDown = event.getPoint();
		
		long clusterIdWithTitleTextAtPoint = IntentionGraph.getInstance().getClusterWithTitleTextAtPoint(getLastPoint());
		
		if (clusterIdWithTitleTextAtPoint != 0l)
		{
			PLayer layer = IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.TOOLS);
			MenuTimer menuTimer = new CalicoAbstractInputHandler.MenuTimer(this, 0l, 100l, CalicoOptions.core.max_hold_distance, 1000,
					mouseDown, 0l, layer);
			if (event.isLeftButton())
				Ticker.scheduleIn(250, menuTimer);
			state = State.BUTTON;
		}

	}

	@Override
	public void actionReleased(InputEventInfo event)
	{
		if (state == State.BUTTON && event.isRightButton())
		{
			pressAndHoldCompleted();
		}
		else if (state == State.PAN)
		{

		}
		else
		{

			Point2D local = IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT).globalToLocal(new Point(event.getPoint()));
			long clusterId = IntentionGraph.getInstance().getClusterAt(local);

			long clusterIdWithWallTextAtPoint = IntentionGraph.getInstance().getClusterWithWallTextAtPoint(getLastPoint());


			if (IntentionGraph.getInstance().getFocus() == IntentionGraph.Focus.CLUSTER
					&& IntentionGraph.getInstance().getClusterInFocus(this).createCanvasIconContainsPoint(event.getPoint()))
			{
				final long newCanvasId = IntentionGraph.getInstance().createNewClusterCanvas();		

				CHistoryController.getInstance().push(new CCanvasController.HistoryFrame(newCanvasId));
			}
			else if (clusterIdWithWallTextAtPoint > 0l)
			{
				IntentionGraph.getInstance().setFocusToWall();
			}
			else if (clusterId > 0
					&& (IntentionGraph.getInstance().getClusterInFocus(this) == null
					|| IntentionGraph.getInstance().getClusterInFocus(this) != null
					&& clusterId != IntentionGraph.getInstance().getClusterInFocus(this).getRootCanvasId()))
				IntentionGraph.getInstance().setFocusToCluster(clusterId, false);
			//			IntentionGraph.getInstance().zoomToCluster(clusterId);
			else if (IntentionGraph.getInstance().getClusterInFocus(this) != null
					&& clusterId == IntentionGraph.getInstance().getClusterInFocus(this).getRootCanvasId())
			{
				//do nothing
			}
			else
				IntentionGraph.getInstance().setFocusToWall(true);
			//			IntentionGraph.getInstance().fitContents();
		}
		
		lastAction = 1;
		state = State.IDLE;
		lastMouse = event.getPoint();
		mouseUp = event.getPoint();
	}

	@Override
	public void actionDragged(InputEventInfo event)
	{
		
		if (state == State.IDLE 
				&& getDraggedDistance() > 10)
		{
			state = State.PAN;
		}
		else if (state == State.PAN
				&& IntentionGraph.getInstance().getLayer(Layer.TOPOLOGY).getScale() != IntentionGraph.getInstance().getDefaultScale())
		{
			double xMouseDelta = event.getGlobalPoint().x - lastMouse.x;
			double yMouseDelta = event.getGlobalPoint().y - lastMouse.y;

			double scale = IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT).getScale();

			xMouseDelta /= scale;
			yMouseDelta /= scale;

			

			IntentionGraph.getInstance().translate(xMouseDelta, yMouseDelta);
		}
		
		/*
		
		double xMouseDelta = event.getGlobalPoint().x - lastMouse.x;
		double yMouseDelta = event.getGlobalPoint().y - lastMouse.y;

		double scale = IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT).getScale();

		xMouseDelta /= scale;
		yMouseDelta /= scale;

		

		IntentionGraph.getInstance().translate(xMouseDelta, yMouseDelta);
		*/
		
		lastMouse = event.getPoint();
	}

	@Override
	public double getDraggedDistance() {
		return mouseDown.distance(lastMouse);
	}

	@Override
	public long getLastAction() {
		return lastAction;
	}

	@Override
	public Point getLastPoint() {
		return lastMouse;
	}

	@Override
	public Point getMouseDown() {
		return mouseDown;
	}

	@Override
	public Point getMouseUp() {
		return mouseUp;
	}

	@Override
	public void openMenu(long arg0, long arg1, Point arg2) {
		
	}

	@Override
	public void pressAndHoldAbortedEarly() {
		
	}

	@Override
	public void pressAndHoldCompleted() {
		long clusterIdWithTitleAtPoint = IntentionGraph.getInstance().getClusterWithTitleTextAtPoint(getLastPoint());
		
		if (clusterIdWithTitleAtPoint != 0l)
		{
			calico.plugins.iip.components.canvas.CanvasTitlePanel.setCanvasTitleText(clusterIdWithTitleAtPoint);
		}
		
	}
	
}
