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
package calico.components.composable.connectors;

import java.awt.Color;

import calico.components.CConnector;
import calico.components.composable.Composable;
import calico.components.composable.ComposableElement;
import calico.controllers.CConnectorController;
import calico.networking.netstuff.ByteUtils;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;

public class ColorElement extends ComposableElement {

	private Color newColor, originalColor;
	
	public ColorElement(long uuid, long cuuid, Color newColor, Color originalColor) {
		super(uuid, cuuid);
		
		this.newColor = newColor;
		this.originalColor = originalColor;
	}

	@Override
	public void applyElement() {
		if (!CConnectorController.exists(cuuid))
			return;
		
		CConnector connector = CConnectorController.connectors.get(cuuid);
		connector.setColor(newColor);
	}

	@Override
	public void removeElement() {
		if (!CConnectorController.exists(cuuid))
			return;
		
		CConnector connector = CConnectorController.connectors.get(cuuid);
		connector.setColor(originalColor);
	}
	
	public Composable getComposable()
	{
		return CConnectorController.connectors.get(cuuid);
	}
	
	public CalicoPacket getPacket(long uuid, long cuuid)
	{
		int packetSize = ByteUtils.SIZE_OF_INT + ByteUtils.SIZE_OF_INT + (2 * ByteUtils.SIZE_OF_LONG) + (2 * ByteUtils.SIZE_OF_INT);
		
		CalicoPacket packet = new CalicoPacket(packetSize);
		
		packet.putInt(NetworkCommand.ELEMENT_ADD);
		packet.putInt(ComposableElement.TYPE_COLOR);
		packet.putLong(uuid);
		packet.putLong(cuuid);
		packet.putColor(newColor);
		packet.putColor(originalColor);
		
		return packet;
	}
	
	public CalicoPacket getPacket()
	{
		return getPacket(this.uuid, this.cuuid);
	}
}
