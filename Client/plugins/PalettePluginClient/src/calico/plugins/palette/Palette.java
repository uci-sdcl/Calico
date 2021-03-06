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
package calico.plugins.palette;

import java.util.ArrayList;
import calico.Calico;
import calico.events.CalicoEventHandler;
import calico.networking.Networking;
import calico.networking.netstuff.ByteUtils;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;

public class Palette {
	
	long uuid;
	ArrayList<CalicoPacket> paletteItems;
	
	public Palette(long paletteUUID)
	{
		this.uuid = paletteUUID;
		paletteItems = new ArrayList<CalicoPacket>();
	}
	
	public void setPaletteItems(CalicoPacket[] items)
	{
		
	}
	
	public void setPaletteItem(CalicoPacket p, int index)
	{
		
	}
	
	public void addPaletteItemToPalette(CalicoPacket p)
	{
		paletteItems.add(p);
	}
	
	public ArrayList<CalicoPacket> getPaletteItems()
	{
		return paletteItems;
	}

	public long getUUID() {

		return uuid;
		
	}

	public boolean contains(long itemUUID) {
		for (CalicoPacket p : paletteItems)
		{
			p.rewind();
			p.getInt();
			if (p.getLong() == itemUUID)
				return true;
		}
		return false;
	}

	public CalicoPacket getItem(long paletteItemUUID) {
		for (CalicoPacket p : paletteItems)
		{
			p.rewind();
			p.getInt();
			if (p.getLong() == paletteItemUUID)
				return p;
		}
		return null;
	}
	
	public CalicoPacket getUpdatePacket()
	{
		//get size of packet
		int size = ByteUtils.SIZE_OF_INT * 2 + ByteUtils.SIZE_OF_LONG;
		for (CalicoPacket cp : paletteItems)
		{
			size += ByteUtils.SIZE_OF_INT;
			size += cp.getLength();
		}

		//Construct packet
		CalicoPacket p = new CalicoPacket(size);
		p.putInt(PaletteNetworkCommands.PALETTE_LOAD);
		p.putLong(uuid);
		p.putInt(paletteItems.size());
		for (CalicoPacket cp : paletteItems)
		{
			p.putInt(cp.getLength());
			p.putByte(cp.getBuffer());
		}
		
		return p;
	}
	
	

}
