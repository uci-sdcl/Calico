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
package calico.plugins.iip.controllers;

import it.unimi.dsi.fastutil.longs.Long2ReferenceArrayMap;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import calico.ProcessQueue;
import calico.networking.netstuff.CalicoPacket;
import calico.plugins.iip.CCanvasLink;
import calico.plugins.iip.CCanvasLinkAnchor;
import calico.plugins.iip.IntentionalInterfaceState;
import calico.plugins.iip.IntentionalInterfacesNetworkCommands;
import calico.plugins.iip.IntentionalInterfacesServerPlugin;
import calico.plugins.iip.graph.layout.CIntentionLayout;
import calico.uuid.UUIDAllocator;

public class CCanvasLinkController
{
	public static CCanvasLinkController getInstance()
	{
		return INSTANCE;
	}

	private static final CCanvasLinkController INSTANCE = new CCanvasLinkController();

	private static Long2ReferenceArrayMap<CCanvasLink> links = new Long2ReferenceArrayMap<CCanvasLink>();
	private static Long2ReferenceArrayMap<CCanvasLinkAnchor> linkAnchors = new Long2ReferenceArrayMap<CCanvasLinkAnchor>();
	private static Long2ReferenceArrayMap<Collection<Long>> anchorIdsByCanvasId = new Long2ReferenceArrayMap<Collection<Long>>();

	public void populateState(IntentionalInterfaceState state)
	{
		for (CCanvasLink link : links.values())
		{
			state.addLinkPacket(link.getState());
		}
	}

	public void clearState()
	{
		links.clear();
		linkAnchors.clear();
		anchorIdsByCanvasId.clear();
	}

	public CCanvasLinkAnchor getAnchor(long anchorId)
	{
		return linkAnchors.get(anchorId);
	}

	public CCanvasLink getLink(long linkId)
	{
		return links.get(linkId);
	}

	public Long getIncomingLink(long canvasId)
	{
		Collection<Long> anchorIds = anchorIdsByCanvasId.get(canvasId);
		if (anchorIds == null)
		{
			return null;
		}

		for (Long anchorId : anchorIdsByCanvasId.get(canvasId))
		{
			if (isDestination(anchorId))
			{
				return linkAnchors.get(anchorId).getLinkId();
			}
		}
		return null;
	}

	public CCanvasLinkAnchor getOpposite(long anchorId)
	{
		CCanvasLinkAnchor anchor = linkAnchors.get(anchorId);
		CCanvasLink link = links.get(anchor.getLinkId());
		if (link.getAnchorA() == anchor)
		{
			return link.getAnchorB();
		}
		else
		{
			return link.getAnchorA();
		}
	}

	public boolean isDestination(long anchorId)
	{
		CCanvasLinkAnchor anchor = linkAnchors.get(anchorId);
		CCanvasLink link = links.get(anchor.getLinkId());
		return (link.getAnchorB() == anchor);
	}

	public boolean isConnectedDestination(long anchorId)
	{
		if (!isDestination(anchorId))
		{
			return false;
		}

		return getOpposite(anchorId).getCanvasId() >= 0;
	}

	public void addLink(CCanvasLink link)
	{
		links.put(link.getId(), link);

		addLinkAnchor(link.getAnchorA());
		addLinkAnchor(link.getAnchorB());
	}

	private void addLinkAnchor(CCanvasLinkAnchor anchor)
	{
		linkAnchors.put(anchor.getId(), anchor);
		getAnchorIdsForCanvasId(anchor.getCanvasId()).add(anchor.getId());
	}

	public CCanvasLink getLinkById(long uuid)
	{
		return links.get(uuid);
	}

	public CCanvasLink removeLinkById(long uuid)
	{
		CCanvasLink link = links.remove(uuid);
		removeLinkAnchor(link.getAnchorA());
		removeLinkAnchor(link.getAnchorB());
		return link;
	}

	private void removeLinkAnchor(CCanvasLinkAnchor anchor)
	{
		linkAnchors.remove(anchor.getId());
		getAnchorIdsForCanvasId(anchor.getCanvasId()).remove(anchor.getId());
	}

	public Collection<Long> getAnchorIdsForCanvasId(long canvasId)
	{
		Collection<Long> anchorIds = anchorIdsByCanvasId.get(canvasId);
		if (anchorIds == null)
		{
			anchorIds = new ArrayList<Long>();
			anchorIdsByCanvasId.put(canvasId, anchorIds);
		}
		return anchorIds;
	}

	public void moveLinkAnchor(long anchor_uuid, long canvas_uuid, CCanvasLinkAnchor.Type type, int x, int y)
	{
		if (!linkAnchors.containsKey(anchor_uuid))
		{
			System.out.println("Warning, attempting to access non-existing anchor in calico.plugins.iip.controllers.CCanvasLinkController.moveLinkAnchor(long, long, Type, int, int)"
					+ "\n\t" + anchor_uuid + ", " + canvas_uuid + ", " + type + ", " + x + ", " + y);
			return;
		}
		
		CCanvasLinkAnchor anchor = linkAnchors.get(anchor_uuid);
		boolean changedCanvas = (canvas_uuid != anchor.getCanvasId());
		if (changedCanvas)
		{
			throw new UnsupportedOperationException("Moving arrows from one canvas to another is not presently supported.");
			// getAnchorIdsForCanvasId(anchor.getCanvasId()).remove(anchor.getId());
		}
		anchor.move(canvas_uuid, type, x, y);
		/**
		 * <pre>
		if (changedCanvas)
		{
			getAnchorIdsForCanvasId(anchor.getCanvasId()).add(anchor.getId());
			IntentionalInterfacesServerPlugin.layoutGraph();
		}
		 */
	}

	public List<Long> getLinkIdsForCanvas(long canvasId)
	{
		List<Long> linkIds = new ArrayList<Long>();
		for (Long anchorId : getAnchorIdsForCanvasId(canvasId))
		{
			linkIds.add(linkAnchors.get(anchorId).getLinkId());
		}
		return linkIds;
	}
	
	/**
	 * Create a new link, sending the request directly to the server. Instantiation and placement of rendering
	 * components will occur on each client when the server broadcasts the new link.
	 */
	public void createLink(long fromCanvasId, long toCanvasId)
	{
		CalicoPacket packet = new CalicoPacket();
		packet.putInt(IntentionalInterfacesNetworkCommands.CLINK_CREATE);
		packet.putLong(UUIDAllocator.getUUID());
		packAnchor(packet, fromCanvasId, CIntentionLayout.getInstance().getArrowAnchorPosition(fromCanvasId, toCanvasId));
		packAnchor(packet, toCanvasId, CIntentionLayout.getInstance().getArrowAnchorPosition(toCanvasId, fromCanvasId));
		packet.putString(""); // empty label

		packet.rewind();
		ProcessQueue.receive(IntentionalInterfacesNetworkCommands.CLINK_CREATE, null, packet);
//		Networking.send(packet);
	}
	
	private void packAnchor(CalicoPacket packet, long canvas_uuid, Point2D position)
	{
		packAnchor(packet, canvas_uuid, CCanvasLinkAnchor.ArrowEndpointType.INTENTION_CELL, (int) position.getX(), (int) position.getY(), 0L);
	}

	private void packAnchor(CalicoPacket packet, long canvas_uuid, Point2D position, long group_uuid)
	{
		packAnchor(packet, canvas_uuid, CCanvasLinkAnchor.ArrowEndpointType.INTENTION_CELL, (int) position.getX(), (int) position.getY(), group_uuid);
	}

	private void packAnchor(CalicoPacket packet, long canvas_uuid, CCanvasLinkAnchor.ArrowEndpointType type, int x, int y)
	{
		packAnchor(packet, canvas_uuid, type, x, y, 0L);
	}

	private void packAnchor(CalicoPacket packet, long canvas_uuid, CCanvasLinkAnchor.ArrowEndpointType type, int x, int y, long group_uuid)
	{
		packet.putLong(UUIDAllocator.getUUID());
		packet.putLong(canvas_uuid);

		packet.putInt(type.ordinal());
		if (type == CCanvasLinkAnchor.ArrowEndpointType.FLOATING)
		{
			packet.putInt(x);
			packet.putInt(y);
		}
		else
		{
			packet.putInt(0);
			packet.putInt(0);
		}

		packet.putLong(group_uuid);
	}
}
