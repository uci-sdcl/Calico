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

import calico.networking.netstuff.CalicoPacket;

public class CIntentionType
{
	public static final int AVAILABLE_COLOR_COUNT = 9;

	private final long uuid;
	private String name;
	private int colorIndex;
	private String description;
	
	public static long noTagIntentionType = 0l;

	public CIntentionType(long uuid, String name, int colorIndex, String description)
	{
		this.uuid = uuid;
		this.name = name;
		this.colorIndex = colorIndex;
		this.description = description;
	}

	public long getId()
	{
		return uuid;
	}

	public String getName()
	{
		return name;
	}
	
	public String getDescription()
	{
		return description;
	}

	public void setName(String name)
	{
		this.name = name;
	}
	
	public void setDescription(String description)
	{
		this.description = description;
	}

	public int getColorIndex()
	{
		return colorIndex;
	}

	public void setColorIndex(int colorIndex)
	{
		this.colorIndex = colorIndex;
	}

	public CalicoPacket getState()
	{
		return CalicoPacket.getPacket(IntentionalInterfacesNetworkCommands.CIT_CREATE, uuid, name, colorIndex, description);
	}
}
