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

import java.awt.event.MouseListener;

import calico.CalicoDraw;
import calico.components.grid.CGrid;
import calico.inputhandlers.InputEventInfo;
import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolo.PNode;

public class GridPerspective extends CalicoPerspective
{
	private static final GridPerspective INSTANCE = new GridPerspective();

	public static GridPerspective getInstance()
	{
		return INSTANCE;
	}
	
	@Override
	protected void displayPerspective(long contextCanvasId)
	{
	}

	protected void drawPieMenu(PNode pieCrust)
	{
		//CGrid.getInstance().getCamera().addChild(pieCrust);
		CalicoDraw.addChildToNode(CGrid.getInstance().getCamera(), pieCrust);
		CGrid.getInstance().getCamera().repaintFrom(pieCrust.getBounds(), pieCrust);
	}

	protected boolean hasPhasicPieMenuActions()
	{
		return false;
	}

	protected boolean processToolEvent(InputEventInfo event)
	{
 
		if(CGrid.getInstance().isPointOnMenuBar(event.getPoint())) {
			//if (mousePressed != 0)
			CGrid.getInstance().clickMenuBar(event, event.getPoint());
			return true;
		}

		return false;
	}

	@Override
	protected long getEventTarget(InputEventInfo event)
	{
		return 0L; // i.e., the grid
	}

	@Override
	protected boolean showBubbleMenu(PNode bubbleHighlighter, PNode bubbleContainer)
	{
		return false;
	}

	protected void addMouseListener(MouseListener listener)
	{
		CGrid.getInstance().addMouseListener(listener);
	}

	protected void removeMouseListener(MouseListener listener)
	{
		CGrid.getInstance().removeMouseListener(listener);
	}

	@Override
	public boolean isNavigationPerspective()
	{
		return true;
	}

	@Override
	public void tickerUpdate() {
		CGrid.getInstance().updateCells();
		
	}

	@Override
	public PLayer getContentLayer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PLayer getToolsLayer() {
		// TODO Auto-generated method stub
		return null;
	}
}
