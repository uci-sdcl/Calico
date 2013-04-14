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

import java.awt.Font;
import java.awt.Point;
import java.awt.Polygon;

import calico.components.CConnector;
import calico.components.composable.ComposableElement;
import calico.controllers.CConnectorController;
import calico.networking.netstuff.ByteUtils;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.nodes.PText;

public class CardinalityElement extends ComposableElement {

	private int type;
	private String text;
	private Font font;
	
	public CardinalityElement(long uuid, long cuuid, int type, String text, Font font) {
		super(uuid, cuuid);
		this.type = type;
		this.text = text;
		this.font = font;
	}

	
	public CalicoPacket getPacket(long uuid, long cuuid)
	{
		int packetSize = ByteUtils.SIZE_OF_INT 				//Command
					+ ByteUtils.SIZE_OF_INT 				//Element Type
					+ (2 * ByteUtils.SIZE_OF_LONG) 			//UUID & CUUID
					+ ByteUtils.SIZE_OF_INT					//Anchor Type
					+ CalicoPacket.getSizeOfString(text)	//Cardinality Text
					+ CalicoPacket.getSizeOfString(font.getName()) //Font name
					+ (2 * ByteUtils.SIZE_OF_INT);			//Font style and size
		
		CalicoPacket packet = new CalicoPacket(packetSize);
		
		packet.putInt(NetworkCommand.ELEMENT_ADD);
		packet.putInt(ComposableElement.TYPE_CARDINALITY);
		packet.putLong(uuid);
		packet.putLong(cuuid);
		packet.putInt(type);
		packet.putString(text);
		packet.putString(font.getName());
		packet.putInt(font.getStyle());
		packet.putInt(font.getSize());
		
		return packet;
	}
	
	public CalicoPacket getPacket()
	{
		return getPacket(this.uuid, this.cuuid);
	}
}
