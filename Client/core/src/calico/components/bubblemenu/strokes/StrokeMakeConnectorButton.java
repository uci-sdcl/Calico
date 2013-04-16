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
package calico.components.bubblemenu.strokes;

import java.awt.Polygon;

import calico.Calico;
import calico.CalicoDataStore;
import calico.components.piemenu.PieMenuButton;
import calico.controllers.CCanvasController;
import calico.controllers.CConnectorController;
import calico.controllers.CStrokeController;
import calico.inputhandlers.InputEventInfo;

public class StrokeMakeConnectorButton extends PieMenuButton
{
	private boolean isActive = false;
	
	public StrokeMakeConnectorButton(long uuid)
	{
		super("connector.create");
		this.uuid = uuid;
	}
	
	public void onPressed(InputEventInfo ev)
	{
		if (!CStrokeController.exists(uuid) || isActive)
		{
			return;
		}
		
		isActive = true;
		super.onPressed(ev);
	}
	
	public void onReleased(InputEventInfo ev)
	{
		if (CStrokeController.exists(uuid))
		{
			long new_uuid = Calico.uuid();
			//Create the connector
			
			Polygon rawPolygon = CStrokeController.strokes.get(uuid).getRawPolygon();
			CStrokeController.strokes.get(uuid).highlight_off();
			CStrokeController.delete(uuid);
			CConnectorController.create(new_uuid, CCanvasController.getCurrentUUID(), CalicoDataStore.PenColor, 
					CalicoDataStore.PenThickness, rawPolygon, 0l, 0l);
						
			CConnectorController.show_stroke_bubblemenu(new_uuid, false);
			
		}
		
		
		ev.stop();
		
		Calico.logger.debug("CLICKED MAKE CONNECTOR BUTTON");
		
		isActive = false;
	}
	
}
