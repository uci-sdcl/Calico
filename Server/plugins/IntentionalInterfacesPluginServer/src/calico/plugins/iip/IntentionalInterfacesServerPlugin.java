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

import java.awt.Color;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import calico.CalicoServer;
import calico.CanvasThread;
import calico.ProcessQueue;
import calico.clients.Client;
import calico.clients.ClientManager;
import calico.components.CCanvas;
import calico.controllers.CCanvasController;
import calico.events.CalicoEventHandler;
import calico.events.CalicoEventListener;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;
import calico.plugins.AbstractCalicoPlugin;
import calico.plugins.CalicoPluginManager;
import calico.plugins.CalicoStateElement;
import calico.plugins.iip.controllers.CCanvasLinkController;
import calico.plugins.iip.controllers.CIntentionCellController;
import calico.plugins.iip.graph.layout.CIntentionClusterLayout;
import calico.plugins.iip.graph.layout.CIntentionLayout;
import calico.utils.CalicoBackupHandler;
import calico.uuid.UUIDAllocator;

public class IntentionalInterfacesServerPlugin extends AbstractCalicoPlugin implements CalicoEventListener, CalicoStateElement
{
	private final IntentionalInterfaceState state = new IntentionalInterfaceState();

	public IntentionalInterfacesServerPlugin()
	{
		PluginInfo.name = "Intentional Interfaces";
	}

	public void onPluginStart()
	{
		initializeCanvases();		
		
		CalicoEventHandler.getInstance().addListener(NetworkCommand.CANVAS_CREATE, this, CalicoEventHandler.PASSIVE_LISTENER);
		CalicoEventHandler.getInstance().addListener(NetworkCommand.CANVAS_DELETE, this, CalicoEventHandler.PASSIVE_LISTENER);
		CalicoEventHandler.getInstance().addListener(NetworkCommand.RESTORE_START, this, CalicoEventHandler.PASSIVE_LISTENER);

		for (Integer event : this.getNetworkCommands())
		{
			System.out.println("IntentionalInterfacesPlugin: attempting to listen for " + event.intValue());
			CalicoEventHandler.getInstance().addListener(event.intValue(), this, CalicoEventHandler.ACTION_PERFORMER_LISTENER);
		}

		// create the default intention types
		CIntentionType.noTagIntentionType = 
				CIntentionCellController.getInstance().createIntentionType(
						UUIDAllocator.getUUID(), "no tag", 0, "something unrelated").getId();
		
		CIntentionCellController.getInstance().createIntentionType(UUIDAllocator.getUUID(), "alternative", 1, "a new alternative");
		CIntentionCellController.getInstance().createIntentionType(UUIDAllocator.getUUID(), "abstraction", 2, "some part in detail");
		CIntentionCellController.getInstance().createIntentionType(UUIDAllocator.getUUID(), "continuation", 3, "more of the same");
		CIntentionCellController.getInstance().createIntentionType(UUIDAllocator.getUUID(), "perspective", 4, "a different view");
		
		


		CalicoPluginManager.registerCalicoStateExtension(this);

		for (long canvasId : CCanvasController.canvases.keySet())
		{
			createIntentionCell(canvasId);
			CIntentionLayout.getInstance().insertCluster(canvasId);
		}
		
		layoutGraph();
	}

	private void initializeCanvases() {
		CCanvas initialCanvas1 = new CCanvas(UUIDAllocator.getUUID());
		CCanvas initialCanvas2 = new CCanvas(UUIDAllocator.getUUID());
		CCanvas initialCanvas3 = new CCanvas(UUIDAllocator.getUUID());
		CCanvas initialCanvas4 = new CCanvas(UUIDAllocator.getUUID());
		CCanvasController.canvases.put(initialCanvas1.getUUID(), initialCanvas1);
		CCanvasController.canvases.put(initialCanvas2.getUUID(), initialCanvas2);
		CCanvasController.canvases.put(initialCanvas3.getUUID(), initialCanvas3);
		CCanvasController.canvases.put(initialCanvas4.getUUID(), initialCanvas4);
	}

	@Override
	public void handleCalicoEvent(int event, CalicoPacket p, Client c)
	{
		if (event == NetworkCommand.RESTORE_START)
		{
			clearState();
			return;
		}

		if (IntentionalInterfacesNetworkCommands.Command.isInDomain(event))
		{
			
//			if (IntentionalInterfacesNetworkCommands.Command.forId(event) != null) 
//				logger.debug("RX "+IntentionalInterfacesNetworkCommands.Command.forId(event).toString());

			switch (IntentionalInterfacesNetworkCommands.Command.forId(event))
			{
				case CIC_CREATE:
					CIC_CREATE(p, c);
					break;
				case CIC_MOVE:
					CIC_MOVE(p, c);
					break;
				case CIC_SET_TITLE:
					CIC_SET_TITLE(p, c, true);
					break;
				case CIC_TAG:
					CIC_TAG(p, c);
					break;
				case CIC_UNTAG:
					CIC_UNTAG(p, c, true);
					break;
				case CIC_DELETE:
					CIC_DELETE(p, c);
					break;
				case CIC_CLUSTER_GRAPH:
					CIC_CLUSTER_GRAPH(p, c);
					break;
				case CIT_CREATE:
					CIT_CREATE(p, c);
					break;
				case CIT_RENAME:
					CIT_RENAME(p, c);
					break;
				case CIT_SET_COLOR:
					CIT_SET_COLOR(p, c);
					break;
				case CIT_DELETE:
					CIT_DELETE(p, c);
					break;
				case CLINK_CREATE:
					CLINK_CREATE(p, c);
					break;
				case CLINK_MOVE_ANCHOR:
					CLINK_MOVE_ANCHOR(p, c);
					break;
				case CLINK_LABEL:
					CLINK_LABEL(p, c);
					break;
				case CLINK_DELETE:
					CLINK_DELETE(p, c, true);
					break;
				case CIC_SET_PIN:
					CIC_SET_PIN(p, c);
					break;
				case EXECUTE_II_EVENT_DISPATCHER_EVENTS:
					EXECUTE_II_EVENT_DISPATCHER_EVENTS(p,c);
					break;
				case CIT_SET_DESCRIPTION:
					CIT_SET_DESCRIPTION(p,c);
					break;
			}
		}
		else
		{
			p.rewind();
			p.getInt();
			long canvasId = p.getLong();

			switch (event)
			{
				case NetworkCommand.CANVAS_CREATE:
					long originatingCanvasId = p.getLong();
					createIntentionCell(canvasId);
					if (originatingCanvasId > 0L)
					{
						long rootCanvas = CIntentionLayout.getInstance().getRootCanvasId(originatingCanvasId);
						CCanvasLinkController.getInstance().createLink(rootCanvas, canvasId);
//						CIntentionCellController.getInstance().get
//						CIntentionLayout.getInstance().insertCluster(originatingCanvasId, canvasId);
					}
					else
					{
						CIntentionLayout.getInstance().insertCluster(canvasId);
					}
					
					layoutGraph();
	
					break;
				case NetworkCommand.CANVAS_DELETE:
					CANVAS_DELETE(p, c, canvasId);
					break;
			}
		}
	}

	private static void createIntentionCell(long canvasId)
	{
		CIntentionCell cell = new CIntentionCell(UUIDAllocator.getUUID(), canvasId);
		CIntentionCellController.getInstance().addCell(cell);

		CalicoPacket p = cell.getCreatePacket();
		forward(p);
	}

	private static void clearState()
	{
		CIntentionCellController.getInstance().clearState();
		CCanvasLinkController.getInstance().clearState();
	}

	// this is called only during restore
	/**
	 * @param p
	 * @param c
	 */
	private static void CIC_CREATE(CalicoPacket p, Client c)
	{
		p.rewind();
		IntentionalInterfacesNetworkCommands.Command.CIC_CREATE.verify(p);

		long uuid = p.getLong();
		long canvasId = p.getLong();
		int x = p.getInt();
		int y = p.getInt();
		String title = p.getString();

		CIntentionCell cell = new CIntentionCell(uuid, canvasId);
		cell.setLocation(x, y);
		cell.setTitle(title);

		CIntentionCellController.getInstance().addCell(cell);
		// clusters will be restored with the serialized graph
	}

	private static void CANVAS_DELETE(CalicoPacket p, Client c, long canvasId)
	{
		CIntentionCell cell = CIntentionCellController.getInstance().getCellByCanvasId(canvasId);
		CIntentionCellController.getInstance().removeCellById(cell.getId());
		deleteAllLinks(canvasId, true); // also removes the cluster, if `canvasId represented a cluster root

		CalicoPacket cicDelete = CalicoPacket.getPacket(IntentionalInterfacesNetworkCommands.CIC_DELETE, cell.getId());
		forward(cicDelete);

		layoutGraph();
	}

	private static void deleteAllLinks(long canvasId, boolean forward)
	{
		long rootCanvasId = CIntentionLayout.getInstance().getRootCanvasId(canvasId);
		List<Long> linkIds = CCanvasLinkController.getInstance().getLinkIdsForCanvas(canvasId);
		if (linkIds.isEmpty())
		{
			CIntentionLayout.getInstance().removeClusterIfAny(canvasId);
			return;
		}

		if (canvasId == rootCanvasId)
		{ // assign the first linked canvas to take the place of the deleted cluster root
			CCanvasLink firstDeletedLink = deleteLink(linkIds.get(0), forward);
			long assignedCanvasContext = firstDeletedLink.getAnchorB().getCanvasId();
			CIntentionLayout.getInstance().replaceCluster(canvasId, assignedCanvasContext);

			for (int i = 1; i < linkIds.size(); i++)
			{ // create a new cluster for each other canvas that was linked from `canvasId
				CCanvasLink deletedLink = deleteLink(linkIds.get(i), forward);
				CIntentionLayout.getInstance().insertCluster(deletedLink.getAnchorA().getCanvasId(), deletedLink.getAnchorB().getCanvasId());
			}
		}
		else
		{
			long incomingLinkId = CCanvasLinkController.getInstance().getIncomingLink(canvasId);
			deleteLink(incomingLinkId, forward);
			linkIds.remove(incomingLinkId);

			for (Long linkId : linkIds)
			{// create a new cluster for each canvas that was linked from `canvasId
				CCanvasLink deletedLink = deleteLink(linkId, forward);
				CIntentionLayout.getInstance().insertCluster(rootCanvasId, deletedLink.getAnchorB().getCanvasId());
			}
		}
	}

	private static CCanvasLink deleteLink(long linkId, boolean forward)
	{
		if (forward)
		{
			CalicoPacket packet = new CalicoPacket();
			packet.putInt(IntentionalInterfacesNetworkCommands.CLINK_DELETE);
			packet.putLong(linkId);
			forward(packet);
		}

		return CCanvasLinkController.getInstance().removeLinkById(linkId);
	}

	private static void CIC_MOVE(CalicoPacket p, Client c)
	{
		p.rewind();
		IntentionalInterfacesNetworkCommands.Command.CIC_MOVE.verify(p);

		long uuid = p.getLong();
		CIntentionCell cell = CIntentionCellController.getInstance().getCellById(uuid);

		int x = p.getInt();
		int y = p.getInt();
		cell.setLocation(x, y);

		forward(p, c);
	}

	private static void CIC_SET_TITLE(CalicoPacket p, Client c, boolean forward)
	{
		p.rewind();
		IntentionalInterfacesNetworkCommands.Command.CIC_SET_TITLE.verify(p);

		long uuid = p.getLong();
		CIntentionCell cell = CIntentionCellController.getInstance().getCellById(uuid);

		cell.setTitle(p.getString());

		if (forward)
		{
			forward(p, c);
		}
	}

	private static void CIC_TAG(CalicoPacket p, Client c)
	{
		p.rewind();
		IntentionalInterfacesNetworkCommands.Command.CIC_TAG.verify(p);

		long uuid = p.getLong();
		long typeId = p.getLong();

		CIntentionCell cell = CIntentionCellController.getInstance().getCellById(uuid);
		cell.setIntentionType(typeId);

		forward(p, c);
	}

	private static void CIC_UNTAG(CalicoPacket p, Client c, boolean forward)
	{
		p.rewind();
		IntentionalInterfacesNetworkCommands.Command.CIC_UNTAG.verify(p);

		long uuid = p.getLong();
		long typeId = p.getLong();

		CIntentionCell cell = CIntentionCellController.getInstance().getCellById(uuid);
		cell.clearIntentionType();

		if (forward)
		{
			forward(p, c);
		}
	}

	private static void CIC_DELETE(CalicoPacket p, Client c)
	{
		throw new UnsupportedOperationException("It is no longer allowed to delete a CIC separately from its CCanvas.");
		/**
		 * <pre>
		p.rewind();
		IntentionalInterfacesNetworkCommands.Command.CIC_DELETE.verify(p);

		long uuid = p.getLong();
		CIntentionCellController.getInstance().removeCellById(uuid);

		layoutGraph();

		forward(p, c);
		 */
	}

	private static void CIC_CLUSTER_GRAPH(CalicoPacket p, Client c)
	{
		p.rewind();
		IntentionalInterfacesNetworkCommands.Command.CIC_CLUSTER_GRAPH.verify(p);

		CIntentionLayout.getInstance().inflateStoredClusterGraph(p.getString());
	}

	private static void CIT_CREATE(CalicoPacket p, Client c)
	{
		p.rewind();
		IntentionalInterfacesNetworkCommands.Command.CIT_CREATE.verify(p);

		long uuid = p.getLong();
		String name = p.getString();
		int colorIndex = p.getInt();
		String description = "";
		if (p.remaining() > 0)
			description = p.getString();

		CIntentionType type = CIntentionCellController.getInstance().createIntentionType(uuid, name, colorIndex, description);

		CalicoPacket colored = CalicoPacket.getPacket(IntentionalInterfacesNetworkCommands.CIT_CREATE, uuid, name, type.getColorIndex(), description);
		ClientManager.send(colored);
	}

	private static void CIT_RENAME(CalicoPacket p, Client c)
	{
		p.rewind();
		IntentionalInterfacesNetworkCommands.Command.CIT_RENAME.verify(p);

		long uuid = p.getLong();
		String name = p.getString();
		CIntentionCellController.getInstance().renameIntentionType(uuid, name);

		forward(p, c);
	}
	
	private static void CIT_SET_DESCRIPTION(CalicoPacket p, Client c)
	{
		
		p.rewind();
		IntentionalInterfacesNetworkCommands.Command.CIT_SET_DESCRIPTION.verify(p);

		long uuid = p.getLong();
		String name = p.getString();
		CIntentionCellController.getInstance().setIntentionTypeDescription(uuid, name);

		forward(p, c);
	}

	private static void CIT_SET_COLOR(CalicoPacket p, Client c)
	{
		p.rewind();
		IntentionalInterfacesNetworkCommands.Command.CIT_SET_COLOR.verify(p);

		long uuid = p.getLong();
		int color = p.getInt();
		CIntentionCellController.getInstance().setIntentionTypeColor(uuid, color);

		forward(p, c);
	}

	private static void CIT_DELETE(CalicoPacket p, Client c)
	{
		p.rewind();
		IntentionalInterfacesNetworkCommands.Command.CIT_DELETE.verify(p);

		long uuid = p.getLong();

		CIntentionCellController.getInstance().removeIntentionType(uuid);

		forward(p, c);
	}

	private static CCanvasLinkAnchor unpackAnchor(long link_uuid, CalicoPacket p)
	{
		long uuid = p.getLong();
		long canvas_uuid = p.getLong();
		CCanvasLinkAnchor.Type type = CCanvasLinkAnchor.Type.values()[p.getInt()];
		int x = p.getInt();
		int y = p.getInt();
		long group_uuid = p.getLong();
		return new CCanvasLinkAnchor(uuid, link_uuid, canvas_uuid, type, x, y, group_uuid);
	}

	private static void CLINK_CREATE(CalicoPacket p, Client c)
	{
		p.rewind();
		IntentionalInterfacesNetworkCommands.Command.CLINK_CREATE.verify(p);

		long uuid = p.getLong();
		CCanvasLinkAnchor anchorA = unpackAnchor(uuid, p);
		CCanvasLinkAnchor anchorB = unpackAnchor(uuid, p);

		if (!(CCanvasController.canvases.containsKey(anchorA.getCanvasId()) && CCanvasController.canvases.containsKey(anchorB.getCanvasId())))
		{
			// the canvas has been deleted
			return;
		}

		Long incomingLinkId = CCanvasLinkController.getInstance().getIncomingLink(anchorB.getCanvasId());
		if (incomingLinkId == null)
		{ // the canvas is not linked, so it must be a cluster root, and now it won't be
			CIntentionLayout.getInstance().removeClusterIfAny(anchorB.getCanvasId());
		}
		else
		{ // the canvas is linked already, so steal it
			CCanvasLinkController.getInstance().removeLinkById(incomingLinkId);
			CalicoPacket deleteIncoming = CalicoPacket.getPacket(IntentionalInterfacesNetworkCommands.CLINK_DELETE, incomingLinkId);
			forward(deleteIncoming);
		}

		CCanvasLink link = new CCanvasLink(uuid, anchorA, anchorB);
		CCanvasLinkController.getInstance().addLink(link);

		layoutGraph();

		forward(p, c);
	}

	private static void CLINK_MOVE_ANCHOR(CalicoPacket p, Client c)
	{
		p.rewind();
		IntentionalInterfacesNetworkCommands.Command.CLINK_MOVE_ANCHOR.verify(p);

		long anchor_uuid = p.getLong();
		long canvas_uuid = p.getLong();
		CCanvasLinkAnchor.Type type = CCanvasLinkAnchor.Type.values()[p.getInt()];
		int x = p.getInt();
		int y = p.getInt();

		CCanvasLinkController.getInstance().moveLinkAnchor(anchor_uuid, canvas_uuid, type, x, y);

		forward(p, c);
	}

	private static void CLINK_LABEL(CalicoPacket p, Client c)
	{
		p.rewind();
		IntentionalInterfacesNetworkCommands.Command.CLINK_LABEL.verify(p);

		long uuid = p.getLong();
		CCanvasLink link = CCanvasLinkController.getInstance().getLinkById(uuid);
		link.setLabel(p.getString());

		forward(p, c);
	}

	private static void CLINK_DELETE(CalicoPacket p, Client c, boolean forward)
	{
		p.rewind();
		IntentionalInterfacesNetworkCommands.Command.CLINK_DELETE.verify(p);

		long uuid = p.getLong();
		CCanvasLink deletedLink = CCanvasLinkController.getInstance().removeLinkById(uuid);
		CIntentionLayout.getInstance().insertCluster(deletedLink.getAnchorA().getCanvasId(), deletedLink.getAnchorB().getCanvasId());

		layoutGraph();

		if (forward)
		{
			forward(p, c);
		}
	}
	
	private static void CIC_SET_PIN(CalicoPacket p, Client c)
	{
		p.rewind();
		IntentionalInterfacesNetworkCommands.Command.CIC_SET_PIN.verify(p);
		
		long cic_id = p.getLong();
		boolean pinValue = p.getInt() != 0;
		
		CIntentionCellController.getInstance().getCellById(cic_id).setIsPinned(pinValue);
		layoutGraph();
		
		forward(p, c);
	}
	
	private static void EXECUTE_II_EVENT_DISPATCHER_EVENTS(CalicoPacket p, Client c)
	{
		p.rewind();
		IntentionalInterfacesNetworkCommands.Command.EXECUTE_II_EVENT_DISPATCHER_EVENTS.verify(p);
		
		forward(p, c);
	}
	
	

	private static void layoutGraph()
	{
		List<CIntentionClusterLayout> clusterLayouts = CIntentionLayout.getInstance().layoutGraph();
		ArrayList<CalicoPacket> packetsToSend = new ArrayList<CalicoPacket>();
		for (CIntentionClusterLayout clusterLayout : clusterLayouts)
		{
			for (CIntentionClusterLayout.CanvasPosition canvas : clusterLayout.getCanvasPositions())
			{
				CIntentionCell cell = CIntentionCellController.getInstance().getCellByCanvasId(canvas.canvasId);
				if (cell.setLocation(canvas.location.x, canvas.location.y))
				{
					CalicoPacket p = new CalicoPacket();
					p.putInt(IntentionalInterfacesNetworkCommands.CIC_MOVE);
					p.putLong(cell.getId());
					p.putInt(cell.getLocation().x);
					p.putInt(cell.getLocation().y);
//					forward(p);
					packetsToSend.add(p);
				}
			}
		}

//		forward(CIntentionLayout.getInstance().getTopology().createPacket());
		packetsToSend.add(CIntentionLayout.getInstance().getTopology().createPacket());
		
		CalicoPacket p = new CalicoPacket();
		p.putInt(NetworkCommand.CHUNK_DATA);
		p.putInt(packetsToSend.size());
		
		for(int j=0;j<packetsToSend.size();j++)
		{
			byte[] bytes = packetsToSend.get(j).export();
			p.putInt(bytes.length);
			p.putByte(bytes);
		}
		forward(p);
		
//		double sqrt = Math.sqrt((double)CIntentionLayout.getInstance().getClusterCount());
//		if (sqrt > Math.floor(sqrt))
//		{
//			CalicoPacket canvasCreatePacket = CalicoPacket.getPacket(NetworkCommand.CANVAS_CREATE, calico.uuid.UUIDAllocator.getUUID(), 0l);
//			canvasCreatePacket.rewind();
//			canvasCreatePacket.getInt();
//			
//			long canvasThreadIndex = 1l;
//			synchronized(CalicoServer.canvasThreads)
//			{
//				if (!CalicoServer.canvasThreads.containsKey(canvasThreadIndex))
//				{
//					try {
//						CalicoServer.canvasThreads.put(canvasThreadIndex, new CanvasThread(canvasThreadIndex));
//					} catch (IOException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//				}
//				CalicoServer.canvasThreads.get(canvasThreadIndex).addPacketToQueue(NetworkCommand.CANVAS_CREATE, null, canvasCreatePacket);
//			}
//			
//			
////			ProcessQueue.receive(NetworkCommand.CANVAS_CREATE, c, canvasCreatePacket);
////			layoutGraph();
//		}
	}

	private static void forward(CalicoPacket p)
	{
		forward(p, null);
	}

	private static void forward(CalicoPacket p, Client c)
	{
		if (c == null)
		{
			ClientManager.send(p);
		}
		else
		{
			ClientManager.send_except(c, p);
		}
	}

	@Override
	public CalicoPacket[] getCalicoStateElementUpdatePackets()
	{
		state.reset();
		
		Rectangle rect = CIntentionLayout.getInstance().getTopologyBounds();
		state.setTopologyBoundsPacket(CalicoPacket.getPacket(IntentionalInterfacesNetworkCommands.WALL_BOUNDS, rect.x, rect.y, rect.width, rect.height));		
		CIntentionCellController.getInstance().populateState(state);
		CCanvasLinkController.getInstance().populateState(state);
		CIntentionLayout.getInstance().populateState(state);

		return state.getAllPackets();
	}

	public Class<?> getNetworkCommandsClass()
	{
		return IntentionalInterfacesNetworkCommands.class;
	}
}
