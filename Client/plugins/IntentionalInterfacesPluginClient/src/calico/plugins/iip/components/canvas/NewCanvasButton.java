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
package calico.plugins.iip.components.canvas;

import calico.components.menus.CanvasMenuButton;
import calico.controllers.CCanvasController;
import calico.perspectives.CanvasPerspective;
import calico.plugins.iip.controllers.CCanvasLinkController;
import calico.plugins.iip.controllers.CIntentionCellController;
import calico.plugins.iip.controllers.CIntentionCellFactory;
import calico.plugins.iip.controllers.IntentionCanvasController;
import calico.plugins.iip.iconsets.CalicoIconManager;

/**
 * Simple button for copying a canvas, delegating to <code>CIntentionCellFactory</code> to create a new canvas/CIC pair.
 * 
 * @author Byron Hawkins
 */
public class NewCanvasButton extends CanvasMenuButton
{
	private static final long serialVersionUID = 1L;

	private long currentCanvasId;

	public NewCanvasButton()
	{
		this(0L);
	}

	/**
	 * Invoked via reflection in CanvasStatusBar
	 */
	public NewCanvasButton(long canvas_uuid)
	{
		try
		{
			this.currentCanvasId = canvas_uuid;

			setImage(CalicoIconManager.getIconImage("intention.new-canvas"));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		if (CIntentionCellController.getInstance().isRootCanvas(canvas_uuid))
			this.setTransparency(.5f);
	}

	public void actionMouseClicked()
	{
		long currentCell = CCanvasController.getCurrentUUID();
		long parentCanvasId = calico.plugins.iip.controllers.IntentionCanvasController.getParentCanvasId(currentCell);
		
		if (parentCanvasId == 0)
			return;
		
		long newCanvasId = CIntentionCellFactory.getInstance()
				.createNewCell(CCanvasController.getCurrentUUID(), CanvasInputProximity.forPosition(getBounds().getX())).getCanvasId();
		
//		IntentionCanvasController.getInstance().collapseLikeIntentionTypes();
//		CCanvasLinkController.getInstance().createLink(/*parentCanvasId*/ CIntentionCellController.getInstance().getClusterRootCanvasId(currentCell), newCanvasId);
		
		if (CanvasPerspective.getInstance().isActive())
		{
			CCanvasController.loadCanvas(newCanvasId);
		}
		

	}
}
