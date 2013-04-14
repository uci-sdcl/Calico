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

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.SwingUtilities;

import calico.Calico;
import calico.CalicoDraw;
import calico.controllers.CCanvasController;
import calico.controllers.CConnectorController;
import calico.inputhandlers.CalicoInputManager;
import calico.networking.Networking;
import calico.networking.PacketHandler;
import calico.networking.netstuff.CalicoPacket;
import calico.plugins.iip.IntentionalInterfacesClientPlugin;
import calico.plugins.iip.IntentionalInterfacesNetworkCommands;
import calico.plugins.iip.components.CCanvasLink;
import calico.plugins.iip.components.CCanvasLinkAnchor;
import calico.plugins.iip.components.CCanvasLinkAnchor.ArrowEndpointType;
import calico.plugins.iip.components.CCanvasLinkArrow;
import calico.plugins.iip.components.CIntentionCell;
import calico.plugins.iip.components.graph.IntentionGraph;
import calico.plugins.iip.inputhandlers.CCanvasLinkInputHandler;

/**
 * Maintains this plugin's internal model of links in the intention graph.
 * 
 * Every link contains two anchors for its head and tail, each of which may be attached to a CIC. The link anchors are
 * not physically attached to the endpoints, so anytime a CIC moves, the link anchor position must be updated
 * accordingly. Each link position will be adjusted by the <code>IntentionGraphController</code> such that it appears
 * attached to the edge of its CIC.
 * 
 * @author Byron Hawkins
 */
public class CCanvasLinkController
{
	public static CCanvasLinkController getInstance()
	{
		return INSTANCE;
	}

	public static void initialize()
	{
		INSTANCE = new CCanvasLinkController();
	}

	private static CCanvasLinkController INSTANCE;

	/**
	 * Map of all links in the intention graph, indexed by id
	 */
	private static Long2ReferenceArrayMap<CCanvasLink> linksById = new Long2ReferenceArrayMap<CCanvasLink>();
	/**
	 * Map of all link anchors in the intention graph, indexed by id
	 */
	private static Long2ReferenceArrayMap<CCanvasLinkAnchor> anchorsById = new Long2ReferenceArrayMap<CCanvasLinkAnchor>();
	/**
	 * Map of all link anchors in the intention graph, indexed by the canvas id of the canvas to which the anchor is
	 * attached.
	 */
	private static Long2ReferenceArrayMap<List<Long>> anchorsIdsByCanvasId = new Long2ReferenceArrayMap<List<Long>>();

	/**
	 * User interaction state marker, indicating the "from" canvas of the last link that was traversed via the
	 * <code>CanvasLinkPanel</code>. A value of <code>0L</code> indicates that some other navigation event has occured
	 * since the last link traversal (if any), such that no link traversal is in effect. This feature is obsolete.
	 */
	private long traversedLinkSourceCanvas = 0L;
	/**
	 * Complement of <code>traversedLinkSourceCanvas</code>, for the "to" canvas.
	 */
	private long traversedLinkDestinationCanvas = 0L;

	/**
	 * Arrow colors are derived from the canvas tags (i.e., <code>CIntentionType</code>s). The initialization sequence
	 * is unreliable, so this state flag marks whether the colors for all existing arrows have been assigned to them
	 * yet.
	 */
	private boolean arrowColorsInitialized = false;

	public void initializeArrowColors()
	{
		for (CCanvasLinkAnchor anchor : anchorsById.values())
		{
			if (anchor.getLink().getAnchorA() == anchor)
			{
				continue;
			}

			CIntentionCell cell = CIntentionCellController.getInstance().getCellByCanvasId(anchor.getCanvasId());
			if (cell == null)
			{
				continue;
			}
			long canvasIntentionTypeId = cell.getIntentionTypeId();
			Color color = Color.black; //IntentionCanvasController.getInstance().getIntentionTypeColor(canvasIntentionTypeId);
			CCanvasLinkArrow arrow = IntentionGraphController.getInstance().getArrowByLinkId(anchor.getLink().getId());
			arrow.setColor(color);
			arrow.redraw();
		}

		arrowColorsInitialized = true;
	}

	/**
	 * Return true when the last navigation action taken by the user was a traversal of a link.
	 */
	public boolean hasTraversedLink()
	{
		return traversedLinkSourceCanvas > 0L;
	}

	public long getTraversedLinkSourceCanvas()
	{
		return traversedLinkSourceCanvas;
	}

	/**
	 * Indicate to this controller that a link was just traversed by the user. It is assumed that the link referred to
	 * by <code>anchor</code> is attached on both ends.
	 * 
	 * @param anchor
	 *            the "from" anchor of the traversed link.
	 */
	public void traverseLinkToCanvas(CCanvasLinkAnchor anchor)
	{
		this.traversedLinkSourceCanvas = anchor.getCanvasId();
		this.traversedLinkDestinationCanvas = anchor.getOpposite().getCanvasId();
		CCanvasController.loadCanvas(traversedLinkDestinationCanvas);
	}

	/**
	 * Indicate to this controller that <code>canvasId</code> has just been displayed in the Canvas View.
	 */
	public void showingCanvas(long canvasId)
	{
		if (canvasId != traversedLinkDestinationCanvas)
		{
			traversedLinkSourceCanvas = traversedLinkDestinationCanvas = 0L;
		}
	}

	/**
	 * Indicate to this controller that the tag for the canvsa associated with <code>cell</code> has just changed. This
	 * controller responds by updating the color of the corresonding incoming arrow to <code>cell</code> (if any).
	 */
	public void canvasIntentionTypeChanged(CIntentionCell cell)
	{
		if (!arrowColorsInitialized)
		{
			return;
		}

		Color color = IntentionCanvasController.getInstance().getIntentionTypeColor(cell.getIntentionTypeId());
		List<Long> anchorIds = anchorsIdsByCanvasId.get(cell.getCanvasId());
		if (anchorIds != null)
		{
			for (Long anchorId : anchorIds)
			{
				CCanvasLinkAnchor anchor = anchorsById.get(anchorId);
				if (anchor.getLink().getAnchorB() == anchor)
				{
					CCanvasLinkArrow arrow = IntentionGraphController.getInstance().getArrowByLinkId(anchor.getLink().getId());
					arrow.setColor(Color.black/*color*/);
					arrow.redraw();
				}
			}
		}
	}

	public CCanvasLinkAnchor getAnchor(long uuid)
	{
		return anchorsById.get(uuid);
	}

	/**
	 * Get the ids of all anchors attached to <code>canvas_uuid</code>.
	 */
	public List<Long> getAnchorIdsByCanvasId(long canvas_uuid)
	{
		List<Long> anchorIds = anchorsIdsByCanvasId.get(canvas_uuid);
		if (anchorIds == null)
		{
			anchorIds = new ArrayList<Long>();
			anchorsIdsByCanvasId.put(canvas_uuid, anchorIds);
		}
		return anchorIds;
	}

	/**
	 * Get the first link intersecting <code>point</code>, according to the intersection rules of
	 * <code>CCanvasLink.contains()</code> (if any link is at that <code>point</code>).
	 */
	public long getLinkAt(Point point)
	{
		for (CCanvasLink link : linksById.values())
		{
			if (link.contains(point))
			{
				return link.getId();
			}
		}
		return -1L;
	}

	/**
	 * Add a link to this plugin's internal model and also install its arrow in the Intention View.
	 */
	public void addLink(CCanvasLink link)
	{
		linksById.put(link.getId(), link);

		addAnchor(link.getAnchorA());
		addAnchor(link.getAnchorB());

		IntentionGraphController.getInstance().addLink(link);

		CalicoInputManager.addCustomInputHandler(link.getId(), CCanvasLinkInputHandler.getInstance());

		notifyCanvasContentChange(link);
	}

	private void addAnchor(CCanvasLinkAnchor anchor)
	{
		anchorsById.put(anchor.getId(), anchor);
		if (anchor.getArrowEndpointType() == ArrowEndpointType.INTENTION_CELL)
		{
			getAnchorIdsByCanvasId(anchor.getCanvasId()).add(anchor.getId());
		}
	}

	/**
	 * Move a link anchor, possibly changing the canvas to which it is attached. This method updates all visual
	 * components associated with the anchor.
	 */
	public void localMoveLinkAnchor(long anchor_uuid, long canvas_uuid, CCanvasLinkAnchor.ArrowEndpointType type, int x, int y)
	{
		final CCanvasLinkAnchor anchor = anchorsById.get(anchor_uuid);
		long originalCanvasId = anchor.getCanvasId();

		if (anchor.getCanvasId() != canvas_uuid)
		{
			// no problem if it gets mapped to the 0 canvas (i.e., it is a floating anchor)
			getAnchorIdsByCanvasId(anchor.getCanvasId()).remove(anchor.getId());
			getAnchorIdsByCanvasId(canvas_uuid).add(anchor.getId());
		}

		anchor.move(canvas_uuid, type, x, y);

		/*
		 * Given that the positions of the CIntentionCells are calculated on a delay, we must update the position
		 * of the arrow inside the SwingUtilities thread (otherwise we run into a race condition).
		 */
//		SwingUtilities.invokeLater(
		IntentionalInterfacesClientPlugin.addNewEventDispatcherEvent(
				new Runnable() { public void run() { 
					IntentionGraphController.getInstance().updateLinkArrow(anchor.getLink());
				}});
//		IntentionGraphController.getInstance().updateLinkArrow(anchor.getLink());
	}

	/**
	 * Move the pixel position of a link anchor, without making any change to its canvas attachment (if any).
	 */
	public void moveLinkAnchor(CCanvasLinkAnchor anchor, Point2D newPosition)
	{
		CalicoPacket packet = new CalicoPacket();
		packet.putInt(IntentionalInterfacesNetworkCommands.CLINK_MOVE_ANCHOR);
		packet.putLong(anchor.getId());
		packet.putLong(anchor.getCanvasId());

		packet.putInt(anchor.getArrowEndpointType().ordinal());
		if (anchor.getArrowEndpointType() == ArrowEndpointType.FLOATING)
		{
			packet.putInt((int) newPosition.getX());
			packet.putInt((int) newPosition.getY());
		}
		else
		{
			packet.putInt(0);
			packet.putInt(0);
		}

		packet.rewind();
		PacketHandler.receive(packet);
//		Networking.send(packet);
	}

	public CCanvasLink getLinkById(long uuid)
	{
		return linksById.get(uuid);
	}

	/**
	 * Discern whether an input event at <code>point</code> is nearest to the head or tail of the arrow referred to by
	 * <code>uuid</code>.
	 */
	public boolean isNearestSideA(long uuid, Point2D point)
	{
		CCanvasLink link = linksById.get(uuid);
		if (link == null)
		{
			return false;
		}

		point = IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT).globalToLocal(new Point2D.Double(point.getX(), point.getY()));
		double a = point.distance(link.getAnchorA().getPoint());
		double b = point.distance(link.getAnchorB().getPoint());
		return a < b;
	}

	/**
	 * Remove a link and its anchors from the plugin's data model and all visual components.
	 */
	public void removeLinkById(long uuid)
	{
		CCanvasLink link = linksById.remove(uuid);
		if (link == null)
			return;
		IntentionGraphController.getInstance().removeLink(link);
		removeLinkAnchor(link.getAnchorA().getId());
		removeLinkAnchor(link.getAnchorB().getId());

		CalicoInputManager.removeCustomInputHandler(uuid);
		notifyCanvasContentChange(link);
	}

	private void removeLinkAnchor(long uuid)
	{
		CCanvasLinkAnchor anchor = anchorsById.remove(uuid);
		getAnchorIdsByCanvasId(anchor.getCanvasId()).remove(anchor.getId());
	}

	private void notifyCanvasContentChange(CCanvasLink link)
	{
		if (link != null)
		{
			notifyCanvasContentChange(link.getAnchorA());
			notifyCanvasContentChange(link.getAnchorB());
		}
	}

	private void notifyCanvasContentChange(CCanvasLinkAnchor anchor)
	{
		if (anchor.getArrowEndpointType() == ArrowEndpointType.INTENTION_CELL)
		{
			IntentionalInterfacesCanvasContributor.getInstance().notifyContentChanged(anchor.getCanvasId());
		}
	}

	/**
	 * Create an empty canvas and a new link to it from <code>fromCanvasId</code>, positioning the new canvas such that
	 * the new link's arrowhead sits adjacent to the canvas at exactly <code>xLinkEndpoint, yLinkEndpoint</code>.
	 */
	public void createLinkToEmptyCanvas(long fromCanvasId, double xLinkEndpoint, double yLinkEndpoint, boolean copy)
	{
		long toCanvasId = createLinkToEmptyCanvas(fromCanvasId);
		if (toCanvasId == 0L)
		{
			return;
		}
		IntentionCanvasController.getInstance().copyCanvas(fromCanvasId, toCanvasId);

		Point2D cellOrigin = IntentionGraphController.getInstance().alignCellEdgeAtLinkEndpoint(fromCanvasId, xLinkEndpoint, yLinkEndpoint);

		CIntentionCellController.getInstance().moveCell(CIntentionCellController.getInstance().getCellByCanvasId(toCanvasId).getId(), cellOrigin.getX(),
				cellOrigin.getY());
	}

	public long createLinkToEmptyCanvas(long fromCanvasId)
	{
		long toCanvasId = CIntentionCellFactory.getInstance().createNewCell().getCanvasId();
		createLink(fromCanvasId, toCanvasId);

		return toCanvasId;
	}

	/**
	 * Detach <code>anchor</code> and place it at <code>x,y</code>.
	 */
	public void orphanLink(CCanvasLinkAnchor anchor, double x, double y)
	{
		CalicoPacket packet = new CalicoPacket();
		packet.putInt(IntentionalInterfacesNetworkCommands.CLINK_MOVE_ANCHOR);
		packet.putLong(anchor.getId());
		packet.putLong(-1L);
		packet.putInt(CCanvasLinkAnchor.ArrowEndpointType.FLOATING.ordinal());
		packet.putInt((int) x);
		packet.putInt((int) y);

		packet.rewind();
		PacketHandler.receive(packet);
		Networking.send(packet);
	}

	/**
	 * Move <code>anchor</code> from its current canvas attachment (if any) to <code>canvasId</code>. The change will be
	 * sent directly to the server with no coordinates for the new anchor position. When the server broadcasts the
	 * change to clients, each client will position the anchor according to policy.
	 */
	public void moveLink(CCanvasLinkAnchor anchor, long canvasId)
	{
		CalicoPacket packet = new CalicoPacket();
		packet.putInt(IntentionalInterfacesNetworkCommands.CLINK_MOVE_ANCHOR);
		packet.putLong(anchor.getId());
		packet.putLong(canvasId);
		packet.putInt(CCanvasLinkAnchor.ArrowEndpointType.INTENTION_CELL.ordinal());
		packet.putInt(0);
		packet.putInt(0);

		packet.rewind();
		PacketHandler.receive(packet);
		Networking.send(packet);
	}

	/**
	 * Create a link from <code>fromCanvasId</code> with no canvas attached to the arrowhead, and place the arrowhead at
	 * <code>x, y</code>.
	 */
	public void createOrphanedLink(long fromCanvasId, double x, double y)
	{
		CalicoPacket packet = new CalicoPacket();
		packet.putInt(IntentionalInterfacesNetworkCommands.CLINK_CREATE);
		packet.putLong(Calico.uuid());
		packAnchor(packet, fromCanvasId, IntentionGraphController.getInstance().getArrowAnchorPosition(fromCanvasId, x, y));
		packAnchor(packet, -1L, CCanvasLinkAnchor.ArrowEndpointType.FLOATING, (int) x, (int) y);
		packet.putString(""); // empty label

		packet.rewind();
		PacketHandler.receive(packet);
		Networking.send(packet);
	}

	/**
	 * Create a new link, sending the request directly to the server. Instantiation and placement of rendering
	 * components will occur on each client when the server broadcasts the new link.
	 */
	public void createLink(long fromCanvasId, long toCanvasId)
	{
		if (canvasBisDescendentOfCanvasA(fromCanvasId, toCanvasId))
			return;
		
		CalicoPacket packet = new CalicoPacket();
		long[] originalChildren = CIntentionCellController.getInstance().getCIntentionCellChildren(fromCanvasId);

		packet.putInt(IntentionalInterfacesNetworkCommands.CLINK_CREATE);
		packet.putLong(Calico.uuid());
		packAnchor(packet, fromCanvasId, IntentionGraphController.getInstance().getArrowAnchorPosition(fromCanvasId, toCanvasId));
		packAnchor(packet, toCanvasId, IntentionGraphController.getInstance().getArrowAnchorPosition(toCanvasId, fromCanvasId));
		packet.putString(""); // empty label
		
		packet.rewind();
		PacketHandler.receive(packet);
		Networking.send(packet);
		
		//if A already has child, then set old child to be child of new child
//		long[] originalChildren;
		if (!CIntentionCellController.getInstance().isRootCanvas(fromCanvasId)
				&& (originalChildren).length > 0)
		{
			for (int i = 0; i < originalChildren.length; i++)
			{
				//delete the original link
				Long[] list = new Long[1];
				list = CCanvasLinkController.getInstance().getAnchorIdsByCanvasId(originalChildren[i]).toArray(list);
				for (Long l : list)
					if (CCanvasLinkController.getInstance().getAnchor(l.longValue()).getLink().getAnchorB().getId() == l.longValue())
						CCanvasLinkController.getInstance().deleteLink(
								CCanvasLinkController.getInstance().getAnchor(l.longValue()).getLink().getId(), false);
				
				//make original link child of final descendent of new child
				long child = toCanvasId;
				long parent = toCanvasId;
				while (child != 0l)
				{
					long[] children = CIntentionCellController.getInstance().getCIntentionCellChildren(child);
					parent = child;
					child = 0;
					if (children.length > 0)
					{	
						child = children[0];
					}
				}
				
				long leafNodeChild = parent;
				
				packet = new CalicoPacket();
				packet.putInt(IntentionalInterfacesNetworkCommands.CLINK_CREATE);
				packet.putLong(Calico.uuid());
				packAnchor(packet, leafNodeChild, IntentionGraphController.getInstance().getArrowAnchorPosition(leafNodeChild, originalChildren[i]));
				packAnchor(packet, originalChildren[i], IntentionGraphController.getInstance().getArrowAnchorPosition(originalChildren[i], leafNodeChild));
				packet.putString(""); // empty label
				
				packet.rewind();
				PacketHandler.receive(packet);
				Networking.send(packet);
			}
			
		}
	}
	
	public boolean canvasBisDescendentOfCanvasA(long fromCanvasId, long toCanvasId)
	{
		//make original link child of final descendent of new child
		long child = fromCanvasId;
		while (child != 0l)
		{
			long[] children = CIntentionCellController.getInstance().getCIntentionCellChildren(child);
			child = 0;
			if (children.length > 0)
			{	
				child = children[0];
			}
			if (child == toCanvasId)
				return true;
		}
		
		return false;
	}
	
//	Alternative code: steals one child rather than whole string.
//	public void createLink(long fromCanvasId, long toCanvasId)
//	{
//		//In comments, Canvas A represents fromCanvasId, Canvas B represents toCanvasId.
//		
//		//Get some values that we will need later:
//		//   get children of Canvas A (this preserves the original children of Canvas A, which get lost in the next command). 
//		CalicoPacket packet;// = new CalicoPacket();
//		long[] originalChildren = CIntentionCellController.getInstance().getCIntentionCellChildren(fromCanvasId);
//		//  get canvas B's parent
//		long toCanvasIdOriginalParent = CIntentionCellController.getInstance().getCIntentionCellParent(toCanvasId);
//		//  get canvas B's children
//		long[] toCanvasIdOriginalChildren = CIntentionCellController.getInstance().getCIntentionCellChildren(toCanvasId);
//		
//		//Create link from Canvas A to Canvas B. This command deletes the parent link for Canvas B.
//		packet = new CalicoPacket();
//		packet.putInt(IntentionalInterfacesNetworkCommands.CLINK_CREATE);
//		packet.putLong(Calico.uuid());
//		packAnchor(packet, fromCanvasId, IntentionGraphController.getInstance().getArrowAnchorPosition(fromCanvasId, toCanvasId));
//		packAnchor(packet, toCanvasId, IntentionGraphController.getInstance().getArrowAnchorPosition(toCanvasId, fromCanvasId));
//		packet.putString(""); // empty label
//		
//		packet.rewind();
//		PacketHandler.receive(packet);
//		Networking.send(packet);
//		
//		if (CIntentionCellController.getInstance().isRootCanvas(fromCanvasId))
//			return;
//				
//		
//		for (int i = 0; i < toCanvasIdOriginalChildren.length; i++)
//		{
//			if (toCanvasIdOriginalParent == 0l
////					|| CIntentionCellController.getInstance().isRootCanvas(toCanvasIdOriginalParent)
//					|| toCanvasIdOriginalChildren[i] == 0l)
//				continue;
//			
//			//Make parent of Canvas B link to Canvas B's child. Do this for all children of Canvas B.			
//			packet = new CalicoPacket();
//			packet.putInt(IntentionalInterfacesNetworkCommands.CLINK_CREATE);
//			packet.putLong(Calico.uuid());
//			packAnchor(packet, toCanvasIdOriginalParent, IntentionGraphController.getInstance().getArrowAnchorPosition(toCanvasIdOriginalParent, toCanvasIdOriginalChildren[i]));
//			packAnchor(packet, toCanvasIdOriginalChildren[i], IntentionGraphController.getInstance().getArrowAnchorPosition(toCanvasIdOriginalChildren[i], toCanvasIdOriginalParent));
//			packet.putString(""); // empty label
//			
//			packet.rewind();
//			PacketHandler.receive(packet);
//			Networking.send(packet);
//		}
//		
//		for (int i = 0; i < originalChildren.length; i++)
//		{
//			if (toCanvasId == 0l || originalChildren[i] == 0l)
//				continue;
//			
//			//Move the children of Canvas A z 			
//			packet = new CalicoPacket();
//			packet.putInt(IntentionalInterfacesNetworkCommands.CLINK_CREATE);
//			packet.putLong(Calico.uuid());
//			packAnchor(packet, toCanvasId, IntentionGraphController.getInstance().getArrowAnchorPosition(toCanvasId, originalChildren[i]));
//			packAnchor(packet, originalChildren[i], IntentionGraphController.getInstance().getArrowAnchorPosition(originalChildren[i], toCanvasId));
//			packet.putString(""); // empty label
//			
//			packet.rewind();
//			PacketHandler.receive(packet);
//			Networking.send(packet);
//		}
//	}

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
		packet.putLong(Calico.uuid());
		packet.putLong(canvas_uuid);

		packet.putInt(type.ordinal());
		if (type == ArrowEndpointType.FLOATING)
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

	public void setLinkLabel(long uuid, String label)
	{
		CalicoPacket packet = new CalicoPacket();
		packet.putInt(IntentionalInterfacesNetworkCommands.CLINK_LABEL);
		packet.putLong(uuid);
		packet.putString(label);

		packet.rewind();
		PacketHandler.receive(packet);
		Networking.send(packet);
	}

	/**
	 * Delete a link, sending the command directly to the server. Removal of visual components will occur in each client
	 * when the server broadcasts the link removal.
	 */
	public void deleteLink(long uuid, boolean local)
	{
		CalicoPacket packet = new CalicoPacket();
		packet.putInt(IntentionalInterfacesNetworkCommands.CLINK_DELETE);
		packet.putLong(uuid);

		packet.rewind();
		PacketHandler.receive(packet);
		if (!local)
		{
			Networking.send(packet);
		}
	}

	/**
	 * Return true if any links are attached to <code>canvas_uuid</code>.
	 */
	boolean hasLinks(long canvas_uuid)
	{
		for (CCanvasLink link : linksById.values())
		{
			if (link.getAnchorA().getCanvasId() == canvas_uuid)
			{
				return true;
			}
			if (link.getAnchorB().getCanvasId() == canvas_uuid)
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Remove all links attached to <code>canvas_uuid</code>, delegating to <code>deleteLink()</code> for each link.
	 */
	void clearLinks(long canvas_uuid)
	{
		Set<Long> linkIdsToDelete = new HashSet<Long>();
		for (long anchorId : getAnchorIdsByCanvasId(canvas_uuid))
		{
			CCanvasLinkAnchor anchor = anchorsById.get(anchorId);
			linkIdsToDelete.add(anchor.getLink().getId());
		}

		for (long linkId : linkIdsToDelete)
		{
			deleteLink(linkId, true);
		}
	}
}
