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
package calico.plugins.iip.controllers;

import calico.components.CCanvas;
import calico.controllers.CCanvasController;
import calico.plugins.iip.components.CIntentionCell;

/**
 * Implements this plugin's interface point to the <code>CCanvas</code> structure.
 * 
 * @author Byron Hawkins
 */
public class IntentionalInterfacesCanvasContributor implements CCanvas.ContentContributor
{
	public static IntentionalInterfacesCanvasContributor getInstance()
	{
		return INSTANCE;
	}

	public static void initialize()
	{
		INSTANCE = new IntentionalInterfacesCanvasContributor();
	}

	private static IntentionalInterfacesCanvasContributor INSTANCE;

	private IntentionalInterfacesCanvasContributor()
	{
		CCanvasController.addContentContributor(this);
	}

	/**
	 * Notify the <code>CCanvasController</code> that some change has been made to <code>canvasId</code>.
	 */
	public void notifyContentChanged(long canvasId)
	{
		CCanvasController.notifyContentChanged(this, canvasId);
	}

	@Override
	public void contentChanged(long canvas_uuid)
	{
		CIntentionCell cell = CIntentionCellController.getInstance().getCellByCanvasId(canvas_uuid);
		if (cell == null)
		{
			return;
		}

		IntentionGraphController.getInstance().contentChanged(canvas_uuid);
	}

	@Override
	public void clearContent(long canvas_uuid)
	{
//		CCanvasLinkController.getInstance().clearLinks(canvas_uuid);

		long cellId = CIntentionCellController.getInstance().getCellByCanvasId(canvas_uuid).getId();
		CIntentionCellController.getInstance().clearCell(cellId);
	}
}
