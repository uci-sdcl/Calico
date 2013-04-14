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
package calico.plugins.iip.components.piemenu.iip;

import java.util.List;

import calico.components.bubblemenu.BubbleMenu;
import calico.components.piemenu.PieMenuButton;
import calico.inputhandlers.InputEventInfo;
import calico.plugins.iip.components.CCanvasLink;
import calico.plugins.iip.components.graph.IntentionGraph;
import calico.plugins.iip.controllers.CCanvasLinkController;
import calico.plugins.iip.controllers.CIntentionCellController;
import calico.plugins.iip.controllers.IntentionGraphController;
import calico.plugins.iip.iconsets.CalicoIconManager;
import calico.plugins.iip.inputhandlers.CIntentionCellInputHandler;

/**
 * Bubble menu button to delete a canvas. The layout always rebuilds itself on the basis of canvases and arrows, so the
 * only action taken by this button is to delete the canvas.
 * 
 * @author Byron Hawkins
 */
public class DeleteCanvasButton extends PieMenuButton
{
	public DeleteCanvasButton()
	{
		super(CalicoIconManager.getIconImage("intention.delete-canvas"));
	}

	@Override
	public void onReleased(InputEventInfo event)
	{
		super.onReleased(event);
		
		long activeCanvasId = CIntentionCellController.getInstance().getCellById(CIntentionCellInputHandler.getInstance().getActiveCell()).getCanvasId();
//		long rootCanvasId = CIntentionCellController.getInstance().getClusterRootCanvasId(activeCanvasId);
		//if this canvas has children, add them to the root (rather than creating new clusters)
		long parentCanvas = CIntentionCellController.getInstance().getCIntentionCellParent(activeCanvasId);
		List<Long> anchors = CCanvasLinkController.getInstance().getAnchorIdsByCanvasId(activeCanvasId);
		
		//delete the original link
		Long[] list = new Long[1];
		list = CCanvasLinkController.getInstance().getAnchorIdsByCanvasId(activeCanvasId).toArray(list);
		for (Long l : list)
			if (CCanvasLinkController.getInstance().getAnchor(l.longValue()).getLink().getAnchorB().getId() == l.longValue())
				CCanvasLinkController.getInstance().deleteLink(
						CCanvasLinkController.getInstance().getAnchor(l.longValue()).getLink().getId(), false);
		
		for (Long anchorId : anchors)
		{
			CCanvasLink link = CCanvasLinkController.getInstance().getAnchor(anchorId.longValue()).getLink();
			if (link.getAnchorA().getCanvasId() == activeCanvasId)
			{
				CCanvasLinkController.getInstance().createLink(parentCanvas, link.getAnchorB().getCanvasId());
			}
		}
		
		
		
		IntentionGraph.getInstance().deleteCanvasAndRemoveExtraClusters(activeCanvasId);
		
		BubbleMenu.clearMenu();
		CIntentionCellInputHandler.getInstance().setCurrentCellId(0l);
	}




}
