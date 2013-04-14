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

import java.awt.Point;

import calico.components.piemenu.PieMenuButton;
import calico.inputhandlers.InputEventInfo;
import calico.plugins.iip.components.CCanvasLink;
import calico.plugins.iip.components.CIntentionCell;
import calico.plugins.iip.controllers.CIntentionCellController;
import calico.plugins.iip.iconsets.CalicoIconManager;
import calico.plugins.iip.inputhandlers.CIntentionCellInputHandler;

/**
 * Bubble menu button to create a link from the selected canvas to another existing canvas. Delegates to
 * <code>CreateIntentionArrowPhase</code> for specific behavior.
 * 
 * @author Byron Hawkins
 */
public class CreateLinkButton extends PieMenuButton
{
	public CreateLinkButton()
	{
		super(CalicoIconManager.getIconImage("intention.link-canvas"));
	}

	@Override
	public void onPressed(InputEventInfo event)
	{
		super.onPressed(event);

		CIntentionCell cell = CIntentionCellController.getInstance().getCellById(CIntentionCellInputHandler.getInstance().getActiveCell());
		
		if (cell != null)
		{

			Point rootPoint = new Point((int)cell.getGlobalBounds().getCenterX(), (int)cell.getGlobalBounds().getCenterY());
			CreateIntentionArrowPhase.INSTANCE.startCreate(
					CIntentionCellController.getInstance().getCellById(CIntentionCellInputHandler.getInstance().getActiveCell()), rootPoint,
					CreateIntentionArrowPhase.NewLinkMode.LINK_EXISTING);
		}
	}
}
