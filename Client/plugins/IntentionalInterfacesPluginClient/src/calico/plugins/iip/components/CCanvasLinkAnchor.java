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
package calico.plugins.iip.components;

import java.awt.Point;

import calico.components.arrow.AbstractArrowAnchorPoint;
import calico.plugins.iip.controllers.CIntentionCellController;

/**
 * Represents an endpoint of a <code>CCanvasLink</code> in this plugin's internal model of the intention graph. This
 * class has no visual counterpart, it is for modeling purposes only.
 * 
 * @author Byron Hawkins
 */
public class CCanvasLinkAnchor extends AbstractArrowAnchorPoint
{
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

	private final long uuid;
	/**
	 * Identifies the canvas corresponding to the CIC at which this anchor is attached, or <code>-1L</code> if this
	 * anchor is not attached to any CIC.
	 */
	private long canvas_uuid;
	/**
	 * If this anchor is attached to a specific scrap, for the "design inside" feature, that scrap's id is specified
	 * here. Otherwise <code>group_uuid</code> will be <code>0L</code>.
	 */
	private long group_uuid;
	/**
	 * Specifies whether this arrow is floating or attached to a CIC.
	 */
	private ArrowEndpointType type;

	/**
	 * The link for which this endoint acts as an anchor.
	 */
	private CCanvasLink link;

	private CCanvasLinkAnchor(long uuid, long canvas_uuid, ArrowEndpointType type)
	{
		super();

		this.uuid = uuid;
		this.canvas_uuid = canvas_uuid;
		this.type = type;
	}

	public CCanvasLinkAnchor(long uuid, long canvas_uuid, int x, int y)
	{
		this(uuid, canvas_uuid, ArrowEndpointType.INTENTION_CELL);

		this.point.setLocation(x, y);
	}

	public CCanvasLinkAnchor(long uuid, int x, int y)
	{
		this(uuid, -1L, ArrowEndpointType.FLOATING);

		this.point.setLocation(x, y);
	}

	public long getId()
	{
		return uuid;
	}

	public CCanvasLinkAnchor getOpposite()
	{
		if (link.getAnchorA() == this)
		{
			return link.getAnchorB();
		}
		else
		{
			return link.getAnchorA();
		}
	}

	public long getCanvasId()
	{
		return canvas_uuid;
	}

	/**
	 * True when this anchor is attached to a specific scrap, for the "design inside" feature.
	 */
	public boolean hasGroup()
	{
		return group_uuid > 0L;
	}

	public long getGroupId()
	{
		return group_uuid;
	}

	/**
	 * Attach this anchor to the scrap <code>group_uuid</code>.
	 */
	public void setGroupId(long group_uuid)
	{
		this.group_uuid = group_uuid;
	}

	public ArrowEndpointType getArrowEndpointType()
	{
		return type;
	}

	public Point getPoint()
	{
		return point;
	}

	public CCanvasLink getLink()
	{
		return link;
	}

	void setLink(CCanvasLink link)
	{
		this.link = link;
	}

	/**
	 * Move this anchor to pixel coordinates <code>(x, y)</code> in the Intention View coordinate space (shared by
	 * CICs), and attach it to <code>canvas_uuid</code> (which may be <code>-1L</code> to indicate a floating anchor).
	 */
	public void move(long canvas_uuid, ArrowEndpointType type, int x, int y)
	{
		this.canvas_uuid = canvas_uuid;
		this.type = type;
		point.x = x;
		point.y = y;
	}
}
