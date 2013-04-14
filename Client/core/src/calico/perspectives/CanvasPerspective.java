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
package calico.perspectives;

import java.awt.Point;
import java.awt.event.MouseListener;

import calico.CalicoDraw;
import calico.components.CCanvas;
import calico.controllers.CArrowController;
import calico.controllers.CCanvasController;
import calico.controllers.CGroupController;
import calico.inputhandlers.InputEventInfo;
import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolo.PNode;

public class CanvasPerspective extends CalicoPerspective
{
	private static final CanvasPerspective INSTANCE = new CanvasPerspective();

	public static CanvasPerspective getInstance()
	{
		return INSTANCE;
	}
	
	@Override
	protected void displayPerspective(long contextCanvasId)
	{
		CCanvasController.loadCanvas(contextCanvasId);
	}
	
	protected boolean showBubbleMenu(PNode bubbleHighlighter, PNode bubbleContainer)
	{
		//CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getCamera().addChild(bubbleHighlighter);
		//CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getCamera().addChild(bubbleContainer);
		CalicoDraw.addChildToNode(CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getCamera(), bubbleHighlighter);
		CalicoDraw.addChildToNode(CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getCamera(), bubbleContainer);
		return true;
	}

	protected void drawPieMenu(PNode pieCrust)
	{
		//CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getCamera().addChild(pieCrust);
		CalicoDraw.addChildToNode(CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getCamera(), pieCrust);
		CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).repaint();
	}

	protected boolean hasPhasicPieMenuActions()
	{
		return true;
	}

	protected boolean processToolEvent(InputEventInfo event)
	{
		if (CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).isPointOnMenuBar(event.getGlobalPoint()))
		{
			if (event.getAction() == InputEventInfo.ACTION_PRESSED || event.getAction() == InputEventInfo.ACTION_RELEASED)
				CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).clickMenuBar(event, event.getGlobalPoint());

			return true;
		}
		else
		{
			return false;
		}
	}

	/**
	 * What UUID arrow is located at the requested X,Y coordinate
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	private long getArrowAtPoint(int x, int y)
	{
		long[] arrowlist = CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getChildArrows();
		if (arrowlist.length > 0)
		{
			for (int i = 0; i < arrowlist.length; i++)
			{
				if (CArrowController.arrows.get(arrowlist[i]).containsMousePoint(new Point(x, y)))
				{
					return arrowlist[i];
				}// if contained
			}// for groups

		}// if grplist>0
		return 0L;
	}

	@Override
	protected long getEventTarget(InputEventInfo event)
	{
		// check for arrows
		long arrowAtPoint = getArrowAtPoint(event.getX(), event.getY());
		if (arrowAtPoint != 0L)
		{
			return arrowAtPoint;
		}

		// Check to see if any groups fit in to the point
		long smallestGroupUUID = CGroupController.get_smallest_containing_group_for_point(CCanvasController.getCurrentUUID(), event.getPoint());
		if (smallestGroupUUID != 0L)
		{
			// we found the smallest group that contains the coord. So run her action listener
			return smallestGroupUUID;
		}

		// Set a default, if all else fails, we go to the canvas
		return CCanvasController.getCurrentUUID();
	}

	protected void addMouseListener(MouseListener listener)
	{
		CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).addMouseListener(listener);
	}

	protected void removeMouseListener(MouseListener listener)
	{
		CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).removeMouseListener(listener);
	}

	@Override
	public boolean isNavigationPerspective()
	{
		return false;
	}

	@Override
	public void tickerUpdate() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public PLayer getContentLayer() {

		return CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getLayer(CCanvas.Layer.CONTENT);
	}

	@Override
	public PLayer getToolsLayer() {
		return CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getLayer(CCanvas.Layer.TOOLS);
	}
}
