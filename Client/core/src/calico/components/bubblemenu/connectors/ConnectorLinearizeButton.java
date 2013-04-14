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
package calico.components.bubblemenu.connectors;

import calico.Calico;
import calico.components.bubblemenu.BubbleMenu;
import calico.components.piemenu.PieMenuButton;
import calico.controllers.CConnectorController;
import calico.inputhandlers.InputEventInfo;

public class ConnectorLinearizeButton extends PieMenuButton
{
	private boolean isActive = false;
	
	public ConnectorLinearizeButton(long uuid)
	{
		super("arrow.status");
		this.uuid = uuid;
	}
	
	public void onPressed(InputEventInfo ev)
	{
		if (!CConnectorController.exists(uuid) || isActive)
		{
			return;
		}
		
		isActive = true;
		super.onPressed(ev);
	}
	
	public void onReleased(InputEventInfo ev)
	{
		if (CConnectorController.exists(uuid))
		{
			CConnectorController.connectors.get(uuid).highlight_off();
			CConnectorController.linearize(uuid);
			CConnectorController.connectors.get(uuid).highlight_on();
			BubbleMenu.moveIconPositions(CConnectorController.connectors.get(uuid).getBounds());
		}
		
		ev.stop();
		
		Calico.logger.debug("CLICKED LINEARIZE CONNECTOR BUTTON");
		
		isActive = false;
	}
	
}
