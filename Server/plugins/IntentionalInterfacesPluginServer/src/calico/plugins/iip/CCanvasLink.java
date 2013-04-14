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
package calico.plugins.iip;

import java.util.concurrent.atomic.AtomicInteger;

import calico.networking.netstuff.CalicoPacket;

public class CCanvasLink
{
	private static final AtomicInteger INDEX_COUNTER = new AtomicInteger();
	
	private long uuid;
	private int index; // not visible to clients

	private CCanvasLinkAnchor anchorA;
	private CCanvasLinkAnchor anchorB;
	
	private String label;

	public CCanvasLink(long uuid, CCanvasLinkAnchor anchorA, CCanvasLinkAnchor anchorB)
	{
		this.uuid = uuid;
		this.index = INDEX_COUNTER.getAndIncrement();
		this.anchorA = anchorA;
		this.anchorB = anchorB;
		this.label = "";
	}
	
	public long getId()
	{
		return uuid;
	}
	
	public int getIndex()
	{
		return index;
	}

	public CCanvasLinkAnchor getAnchorA()
	{
		return anchorA;
	}
	
	public CCanvasLinkAnchor getAnchorB()
	{
		return anchorB;
	}
	
	public String getLabel()
	{
		return label;
	}
	
	public void setLabel(String label)
	{
		this.label = label;
	}

	public CalicoPacket getState()
	{
		return CalicoPacket.getPacket(
				IntentionalInterfacesNetworkCommands.CLINK_CREATE,
				uuid,
				anchorA.getId(),
				anchorA.getCanvasId(),
				anchorA.getType().ordinal(),
				anchorA.getPoint().x,
				anchorA.getPoint().y,
				anchorA.getGroupId(),
				anchorB.getId(),
				anchorB.getCanvasId(),
				anchorB.getType().ordinal(),
				anchorB.getPoint().x,
				anchorB.getPoint().y,
				anchorB.getGroupId(),
				label
		);
	}
}
