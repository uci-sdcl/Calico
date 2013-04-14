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
package calico.plugins.iip.components.menus.buttons;

import calico.components.menus.CanvasMenuButton;
import calico.controllers.CCanvasController;
import calico.perspectives.CanvasPerspective;
import calico.plugins.iip.components.canvas.CanvasInputProximity;
import calico.plugins.iip.components.graph.IntentionGraph;
import calico.plugins.iip.controllers.CCanvasLinkController;
import calico.plugins.iip.controllers.CIntentionCellController;
import calico.plugins.iip.controllers.CIntentionCellFactory;
import calico.plugins.iip.iconsets.CalicoIconManager;

/**
 * Simple button to create a new canvas in its own cluster. The layout will automatically put a canvas with no incoming
 * arrows into its own cluster, so the button simply requests a new canvas.
 * 
 * @author Byron Hawkins
 */
public class NewClusterCanvasButton extends CanvasMenuButton
{
	public NewClusterCanvasButton()
	{
		setImage(CalicoIconManager.getIconImage("intention.new-canvas"));
		
		if (IntentionGraph.getInstance().getFocus() == IntentionGraph.Focus.WALL)
			setTransparency(.7f);
	}

	@Override
	public void actionMouseClicked()
	{
		if (IntentionGraph.getInstance().getFocus() == IntentionGraph.Focus.WALL)
			return;
		
		IntentionGraph.getInstance().createNewClusterCanvas();
	}




}
