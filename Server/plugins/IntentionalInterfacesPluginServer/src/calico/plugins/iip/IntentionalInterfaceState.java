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

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;

import calico.networking.netstuff.CalicoPacket;
import calico.plugins.iip.graph.layout.CIntentionLayout;

public class IntentionalInterfaceState
{
	private final List<CalicoPacket> packets = new ObjectArrayList<CalicoPacket>();
	
	private final List<CalicoPacket> cellPackets = new ObjectArrayList<CalicoPacket>();
	private final List<CalicoPacket> linkPackets = new ObjectArrayList<CalicoPacket>();
	
	public void reset()
	{
		packets.clear();
		cellPackets.clear();
		linkPackets.clear();
	}
	
	public void addCellPacket(CalicoPacket packet)
	{
		cellPackets.add(packet);
	}
	
	public void addLinkPacket(CalicoPacket packet)
	{
		linkPackets.add(packet);
	}
	
	public void setTopologyPacket(CalicoPacket packet)
	{
		packets.add(packet);
	}
	
	public void setClusterGraphPacket(CalicoPacket packet)
	{
		packets.add(packet);
	}
	
	public void setTopologyBoundsPacket(CalicoPacket packet)
	{
		packets.add(packet);
	}
	
	public CalicoPacket[] getAllPackets()
	{

		packets.addAll(cellPackets);
		packets.addAll(linkPackets);
		packets.add(CalicoPacket.getPacket(IntentionalInterfacesNetworkCommands.CIC_UPDATE_FINISHED, IntentionalInterfacesNetworkCommands.CIC_UPDATE_FINISHED));
		
		return packets.toArray(new CalicoPacket[0]);
	}
}
