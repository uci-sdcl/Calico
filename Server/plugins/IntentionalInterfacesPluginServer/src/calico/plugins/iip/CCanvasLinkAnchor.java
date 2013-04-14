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

import java.awt.Point;

public class CCanvasLinkAnchor
{
	public enum Type
	{
		FLOATING,
		INTENTION_CELL;
	}

	private long uuid;
	private long link_uuid;
	private long canvas_uuid;
	private Type type;
	private Point point;

	private long group_uuid;
	
	/**
	 * Simple enum specifying whether an endpoint is attached to any <code>CIntentionCell</code>.
	 * 
	 * @author Byron Hawkins
	 */
	public enum ArrowEndpointType
	{
		FLOATING,
		INTENTION_CELL;
	}

	public CCanvasLinkAnchor(long uuid, long link_uuid, long canvas_uuid)
	{
		this.uuid = uuid;
		this.link_uuid = link_uuid;
		this.canvas_uuid = canvas_uuid;
		type = Type.FLOATING;
		point = new Point();

		this.group_uuid = 0L;
	}

	public CCanvasLinkAnchor(long uuid, long link_uuid, long canvas_uuid, Type type, int x, int y)
	{
		this(uuid, link_uuid, canvas_uuid);

		this.type = type;
		point.x = x;
		point.y = y;
	}

	public CCanvasLinkAnchor(long uuid, long link_uuid, long canvas_uuid, Type type, int x, int y, long group_uuid)
	{
		this(uuid, link_uuid, canvas_uuid, type, x, y);

		this.group_uuid = group_uuid;
	}

	public long getId()
	{
		return uuid;
	}
	
	public long getLinkId()
	{
		return link_uuid;
	}

	public long getCanvasId()
	{
		return canvas_uuid;
	}

	public long getGroupId()
	{
		return group_uuid;
	}

	public Type getType()
	{
		return type;
	}

	public Point getPoint()
	{
		return point;
	}
	
	public void move(long canvas_uuid, Type type, int x, int y)
	{
		this.canvas_uuid = canvas_uuid;
		this.type = type;
		point.x = x;
		point.y = y;
	}
}
