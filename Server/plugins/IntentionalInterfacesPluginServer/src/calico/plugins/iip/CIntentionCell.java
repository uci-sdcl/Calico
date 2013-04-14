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
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import calico.components.CCanvas;
import calico.networking.netstuff.CalicoPacket;
import calico.plugins.iip.controllers.CCanvasLinkController;
import calico.plugins.iip.controllers.CIntentionCellController;
import calico.plugins.iip.graph.layout.CIntentionLayout;
import edu.umd.cs.piccolo.util.PBounds;

public class CIntentionCell
{
	public static final String DEFAULT_TITLE = "<default>";

	private long uuid;
	private long canvas_uuid;
	private final Point location;
	private String title;
	private boolean isPinned = false;
	private Long intentionTypeId = null;
	
	/**
	 * Used by layout manager for pinning
	 */
	private double ratioX = 0;
	/**
	 * Used by layout manager for pinning
	 */
	private double ratioY = 0;

	public CIntentionCell(long uuid, long canvasId)
	{
		this.uuid = uuid;
		this.canvas_uuid = canvasId;
//		this.location = new Point(-(CIntentionLayout.INTENTION_CELL_SIZE.width / 2), -(CIntentionLayout.INTENTION_CELL_SIZE.height / 2));
		this.location = new Point(Integer.MIN_VALUE, Integer.MIN_VALUE);
		this.title = DEFAULT_TITLE;
	}

	public long getId()
	{
		return uuid;
	}

	public long getCanvasId()
	{
		return canvas_uuid;
	}

	public Point getLocation()
	{
		return location;
	}

	/**
	 * If different than the current location, set the location of the CIC and return true.
	 */
	public boolean setLocation(int x, int y)
	{
		if ((location.x == x) && (location.y == y))
		{
			return false;
		}

		location.x = x;
		location.y = y;
		
		
		Rectangle r = CIntentionLayout.getInstance().getClusterBounds(
				calico.plugins.iip.graph.layout.CIntentionLayout.getInstance().getRootCanvasId(this.canvas_uuid));
		
		if (r != null)
		{
			this.ratioX = (location.getX() - r.getX()) / r.getWidth();
			this.ratioY = (location.getY() - r.getY()) / r.getHeight();
		}

		return true;
	}
	
	public void setLocationBasedOnRatio(Rectangle r)
	{
		int newX = (int)(r.getWidth() * ratioX + r.getX());
		int newY = (int)(r.getHeight() * ratioY + r.getY());
		
		setLocation(newX, newY);
	}
	
	public Point getLocationBasedOnRatio(Rectangle r)
	{
		int newX = (int)(r.getWidth() * ratioX + r.getX());
		int newY = (int)(r.getHeight() * ratioY + r.getY());
		return new Point(newX,newY);
	}

	public String getTitle()
	{
		return title;
	}

	public boolean hasUserTitle()
	{
		return !title.equals(DEFAULT_TITLE);
	}

	public void setTitle(String title)
	{
		this.title = title;
	}
	
	public boolean hasIntentionType()
	{
		return (intentionTypeId != null);
	}
	
	public Long getIntentionTypeId()
	{
		return intentionTypeId;
	}

	public void setIntentionType(long intentionTypeId)
	{
		if (intentionTypeId == -1)
			intentionTypeId = CIntentionType.noTagIntentionType;
		this.intentionTypeId = intentionTypeId;
	}

	public void clearIntentionType()
	{
		intentionTypeId = null;
	}

	public CalicoPacket getCreatePacket()
	{
		return CalicoPacket.getPacket(IntentionalInterfacesNetworkCommands.CIC_CREATE, uuid, canvas_uuid, location.x, location.y, title, isPinned);
	}

	public void populateState(IntentionalInterfaceState state)
	{
		state.addCellPacket(getCreatePacket());

		if (intentionTypeId != null)
		{
			state.addCellPacket(CalicoPacket.getPacket(IntentionalInterfacesNetworkCommands.CIC_TAG, uuid, intentionTypeId));
		}
	}
	
	/**
	 * Get the center point of this CIC in Intention View coordinates.
	 */
	public Point2D getCenter()
	{
		Rectangle2D.Double rect = new Rectangle2D.Double(location.x, location.y, 
				CIntentionLayout.INTENTION_CELL_SIZE.width,
				CIntentionLayout.INTENTION_CELL_SIZE.height);
		return new Point2D.Double(rect.getCenterX(), rect.getCenterY());
	}

	public Rectangle2D copyBounds() {
		return 	new Rectangle2D.Double(location.x, location.y, 
				CIntentionLayout.INTENTION_CELL_SIZE.width,
				CIntentionLayout.INTENTION_CELL_SIZE.height);
	}

	public void setIsPinned(boolean pinValue) {
		isPinned = pinValue;
		
		long[] children = CIntentionCellController.getInstance().getCIntentionCellChildren(getCanvasId());
		if (children.length > 0)
			 CIntentionCellController.getInstance().getCellByCanvasId(children[0]).setIsPinned(pinValue);
	}

	public boolean isPinned() {
		return isPinned;
	}
}
