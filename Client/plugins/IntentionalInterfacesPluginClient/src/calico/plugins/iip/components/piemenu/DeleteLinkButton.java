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
package calico.plugins.iip.components.piemenu;

import calico.components.bubblemenu.BubbleMenu;
import calico.components.piemenu.PieMenuButton;
import calico.inputhandlers.InputEventInfo;
import calico.plugins.iip.components.CCanvasLink;
import calico.plugins.iip.controllers.CCanvasLinkController;
import calico.plugins.iip.controllers.CIntentionCellController;
import calico.plugins.iip.iconsets.CalicoIconManager;

/**
 * Simple button for deleting a link. The layout is reconstructed from the set of CICs and arrows on every iteration, so
 * it does not need to be specifically updated.
 * 
 * @author Byron Hawkins
 */
public class DeleteLinkButton extends PieMenuButton
{
	private CCanvasLink link = null;

	public DeleteLinkButton()
	{
		// "" creates a big red X
//		super(CalicoIconManager.getIconImage("intention.canvas-link-delete"));
		super(CalicoIconManager.getIconImage("intention.delete-canvas"));
	}

	public void setContext(CCanvasLink link)
	{
		this.link = link;
	}

	@Override
	public void onReleased(InputEventInfo event)
	{
		if (link == null)
		{
			System.out.println("Warning: delete link button displayed without having been prepared with a link!");
			return;
		}

		long targetCanvas = link.getAnchorB().getCanvasId();
		long root = CIntentionCellController.getInstance().getClusterRootCanvasId(targetCanvas);
		
		
		System.out.println("Delete the link from canvas #" + link.getAnchorA().getCanvasId() + " to canvas #" + link.getAnchorB().getCanvasId());
		CCanvasLinkController.getInstance().deleteLink(link.getId(), false);
		CCanvasLinkController.getInstance().createLink(root, targetCanvas);
		
		BubbleMenu.clearMenu();
	}
}
