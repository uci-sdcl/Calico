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

import java.awt.BasicStroke;
import java.awt.Stroke;

import calico.components.CConnector;
import calico.components.composable.ComposableElement;
import calico.controllers.CConnectorController;
import calico.networking.netstuff.ByteUtils;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;

public class LineStyleElement extends ComposableElement {

	private Stroke originalStroke, newStroke;
	
	public LineStyleElement(long uuid, long cuuid, Stroke newStroke, Stroke originalStroke) {
		super(uuid, cuuid);

		this.newStroke = newStroke;
		this.originalStroke = originalStroke;
	}

	
	public CalicoPacket getPacket(long uuid, long cuuid)
	{
		BasicStroke n = (BasicStroke)newStroke;
		BasicStroke o = (BasicStroke)originalStroke;
		int nDashLength = (n.getDashArray() == null) ? 0 : n.getDashArray().length;
		int oDashLength = (o.getDashArray() == null) ? 0 : o.getDashArray().length;
		
		int packetSize = ByteUtils.SIZE_OF_INT  					//Command
				+ ByteUtils.SIZE_OF_INT  							//Element Type
				+ (2 * ByteUtils.SIZE_OF_LONG) 						//UUID & CUUID
				+ (6 * ByteUtils.SIZE_OF_INT) 						//Line Width, End Cap, Line Join, MiterLimit, Dash Array Length, Dash Phase
				+ (nDashLength * ByteUtils.SIZE_OF_INT) //Dash Array
				+ (6 * ByteUtils.SIZE_OF_INT) 						//Line Width, End Cap, Line Join, MiterLimit, Dash Array Length, Dash Phase
				+ (oDashLength * ByteUtils.SIZE_OF_INT); //Dash Array

		
		CalicoPacket packet = new CalicoPacket(packetSize);
		
		packet.putInt(NetworkCommand.ELEMENT_ADD);
		packet.putInt(ComposableElement.TYPE_LINESTYLE);
		packet.putLong(uuid);
		packet.putLong(cuuid);
		
		//New stroke
		packet.putFloat(n.getLineWidth());
		packet.putInt(n.getEndCap());
		packet.putInt(n.getLineJoin());
		packet.putFloat(n.getMiterLimit());
		packet.putInt(nDashLength);
		for (int i = 0; i < nDashLength; i++)
		{
			packet.putFloat(n.getDashArray()[i]);
		}
		packet.putFloat(n.getDashPhase());
		
		//Original stroke
		packet.putFloat(o.getLineWidth());
		packet.putInt(o.getEndCap());
		packet.putInt(o.getLineJoin());
		packet.putFloat(o.getMiterLimit());
		packet.putInt(oDashLength);
		for (int i = 0; i < oDashLength; i++)
		{
			packet.putFloat(o.getDashArray()[i]);
		}
		packet.putFloat(o.getDashPhase());
		
		
		return packet;
	}
	
	public CalicoPacket getPacket()
	{
		return getPacket(this.uuid, this.cuuid);
	}
}
