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
package calico.plugins.iip.perspectives;

import java.awt.event.MouseListener;

import javax.swing.SwingUtilities;

import calico.CalicoDataStore;
import calico.CalicoDraw;
import calico.controllers.CCanvasController;
import calico.controllers.CHistoryController;
import calico.inputhandlers.InputEventInfo;
import calico.networking.Networking;
import calico.networking.PacketHandler;
import calico.networking.netstuff.CalicoPacket;
import calico.perspectives.CalicoPerspective;
import calico.plugins.iip.IntentionalInterfacesNetworkCommands;
import calico.plugins.iip.components.CIntentionCell;
import calico.plugins.iip.components.canvas.CanvasTagPanel;
import calico.plugins.iip.components.canvas.CanvasTitlePanel;
import calico.plugins.iip.components.graph.IntentionGraph;
import calico.plugins.iip.controllers.CCanvasLinkController;
import calico.plugins.iip.controllers.CIntentionCellController;
import calico.plugins.iip.controllers.IntentionGraphController;
import calico.plugins.iip.inputhandlers.CCanvasLinkInputHandler;
import calico.plugins.iip.inputhandlers.CIntentionCellInputHandler;
import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolo.PNode;

/**
 * Integration point for the Intention View with the <code>CalicoPerspective.Registry</code>. When the user requests to
 * see the Intention View, <code>displayPerspective()</code> is called to install the Intention View in the display
 * area.
 * 
 * User input in the IntentionView is initially sent here to determine which object it most specifically relates to.
 * 
 * Navigation history for the Intention View is implemented here.
 * 
 * @author Byron Hawkins
 */
public class IntentionalInterfacesPerspective extends CalicoPerspective
{
	private static final IntentionalInterfacesPerspective INSTANCE = new IntentionalInterfacesPerspective();

	public static IntentionalInterfacesPerspective getInstance()
	{
		return INSTANCE;
	}

	private boolean notYetDisplayed = true;

	public void displayPerspective(final long contextCanvasId)
	{
		final boolean initializing = notYetDisplayed;

		// CHistoryController.getInstance().push(new HistoryFrame(contextCanvasId));
		
		final CIntentionCell cell = CIntentionCellController.getInstance().getCellByCanvasId(contextCanvasId);
		
		if (contextCanvasId == IntentionGraph.WALL)
		{
			IntentionGraph.getInstance().setFocus_primitive(IntentionGraph.Focus.WALL, 0l);
		}
		else if (cell != null)
		{
			IntentionGraph.getInstance().setFocus_primitive(IntentionGraph.Focus.CLUSTER, cell.getCanvasId());
		}

		SwingUtilities.invokeLater(
				new Runnable() { public void run() { 
					CalicoDataStore.calicoObj.getContentPane().removeAll();
					CalicoDataStore.calicoObj.getContentPane().add(IntentionGraph.getInstance().getComponent());
					CalicoDataStore.calicoObj.pack();
					CalicoDataStore.calicoObj.setVisible(true);
					CalicoDataStore.calicoObj.repaint();
					

					
					activate();
				}});
		

		
		if (!initializing)
		{
			SwingUtilities.invokeLater(new Runnable() {
				public void run()
				{
//					CIntentionCell cell = CIntentionCellController.getInstance().getCellByCanvasId(contextCanvasId);
					/*if (cell == null)
					{
//						NICKNICKNICK
//						IntentionGraph.getInstance().fitContents();
					}
					else*/ if (contextCanvasId == IntentionGraph.WALL)
					{
						IntentionGraph.getInstance().setFocusToWall();
					}
					else if (cell != null)
					{
//						long cellId = cell.getId();
//						IntentionGraph.getInstance().zoomToCell(cellId);
//						IntentionGraph.getInstance().fitContents();
						long canvasId = cell.getCanvasId();
						IntentionGraph.getInstance().setFocusToCluster(canvasId, true);
//						IntentionGraph.getInstance().zoomToCluster(canvasId);
					}
				}
			});
		}

	}

	@Override
	public void activate()
	{
		if (notYetDisplayed)
		{
			notYetDisplayed = false;
			IntentionGraphController.getInstance().initializeDisplay();
			CIntentionCellController.getInstance().initializeDisplay();
		}

		super.activate();
		
		CalicoPacket packet = CalicoPacket.getPacket(IntentionalInterfacesNetworkCommands.II_PERSPECTIVE_ACTIVATED);
		packet.rewind();
		
		PacketHandler.receive(packet);
		Networking.send(CalicoPacket.getPacket(IntentionalInterfacesNetworkCommands.II_PERSPECTIVE_ACTIVATED));
	}

	@Override
	protected void addMouseListener(MouseListener listener)
	{
		IntentionGraph.getInstance().addMouseListener(listener);
	}

	@Override
	protected void removeMouseListener(MouseListener listener)
	{
		IntentionGraph.getInstance().removeMouseListener(listener);
	}

	@Override
	protected void drawPieMenu(PNode pieCrust)
	{
		IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.TOOLS).addChild(pieCrust);
		IntentionGraph.getInstance().repaint();
	}

	@Override
	protected long getEventTarget(InputEventInfo event)
	{
		long target_uuid = CIntentionCellInputHandler.getInstance().getActiveCell();
		if (target_uuid >= 0L)
		{
			return target_uuid;
		}

		target_uuid = CCanvasLinkInputHandler.getInstance().getActiveLink();
		if (target_uuid >= 0L)
		{
			return target_uuid;
		}

		target_uuid = CCanvasLinkController.getInstance().getLinkAt(event.getGlobalPoint());
		if (target_uuid >= 0L)
		{
			CCanvasLinkInputHandler.getInstance().setCurrentLinkId(target_uuid, event.getGlobalPoint());
			return target_uuid;
		}

		target_uuid = CIntentionCellController.getInstance().getCellAt(event.getGlobalPoint());
		if (target_uuid >= 0L)
		{
			CIntentionCellInputHandler.getInstance().setCurrentCellId(target_uuid);
			return target_uuid;
		}

		// look for arrows, CICs, else:
		return IntentionGraph.getInstance().getId();
	}

	@Override
	protected boolean hasPhasicPieMenuActions()
	{
		return true;
	}

	@Override
	protected boolean processToolEvent(InputEventInfo event)
	{
		return IntentionGraph.getInstance().processToolEvent(event);
	}

	@Override
	protected boolean showBubbleMenu(PNode bubbleHighlighter, PNode bubbleContainer)
	{
		CalicoDraw.addChildToNode(IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.TOOLS), bubbleHighlighter);
//		IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.TOOLS).addChild(bubbleHighlighter);
		CalicoDraw.addChildToNode(IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.TOOLS), bubbleContainer);
//		IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.TOOLS).addChild(bubbleContainer);
		return true;
	}

	@Override
	public boolean isNavigationPerspective()
	{
		return true;
	}

	private class HistoryFrame extends CHistoryController.Frame
	{
		private final long contextCanvasId;

		public HistoryFrame(long contextCanvasId)
		{
			this.contextCanvasId = contextCanvasId;
		}

		protected void restore()
		{
			displayPerspective(contextCanvasId);
		}
	}

	@Override
	public void tickerUpdate() {
		calico.plugins.iip.controllers.CIntentionCellController.updateCells();
		
	}

	@Override
	public PLayer getContentLayer() {
		return IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT);
	}

	@Override
	public PLayer getToolsLayer() {
		return IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.TOOLS);
	}
}
