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

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.print.attribute.standard.SheetCollate;

import org.apache.commons.lang.ArrayUtils;

import calico.Calico;
import calico.controllers.CCanvasController;
import calico.inputhandlers.CalicoInputManager;
import calico.networking.Networking;
import calico.networking.PacketHandler;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;
import calico.plugins.iip.IntentionalInterfacesNetworkCommands;
import calico.plugins.iip.components.CCanvasLink;
import calico.plugins.iip.components.CCanvasLinkAnchor;
import calico.plugins.iip.components.CCanvasLinkArrow;
import calico.plugins.iip.components.CIntentionCell;
import calico.plugins.iip.components.CIntentionType;
import calico.plugins.iip.components.graph.IntentionGraph;
import calico.plugins.iip.inputhandlers.CIntentionCellInputHandler;

/**
 * Maintains this plugins internal model of CICs in the intenttion graph.
 * 
 * @author Byron Hawkins
 */
public class CIntentionCellController
{
	public static CIntentionCellController getInstance()
	{
		return INSTANCE;
	}

	public static void initialize()
	{
		INSTANCE = new CIntentionCellController();
	}

	private static CIntentionCellController INSTANCE;

	/**
	 * Map of all CICs in the Intention View, indexed by id.
	 */
	private static Long2ReferenceArrayMap<CIntentionCell> cells = new Long2ReferenceArrayMap<CIntentionCell>();
	/**
	 * Map of all CICs in the Intention View, indexed by associated canvas id.
	 */
	private static Long2ReferenceArrayMap<CIntentionCell> cellsByCanvasId = new Long2ReferenceArrayMap<CIntentionCell>();

	/**
	 * Clear all content from this CIC, including tags and title.
	 */
	public void clearCell(long cellId)
	{
		CIntentionCell cell = cells.get(cellId);
		for (CIntentionType type : IntentionCanvasController.getInstance().getActiveIntentionTypes())
		{
			if (cell.getIntentionTypeId() == type.getId())
			{
				toggleCellIntentionType(cellId, type.getId(), false, true);
			}
		}

		if (cell.hasUserTitle())
		{
			setCellTitle(cellId, CIntentionCell.DEFAULT_TITLE, true);
		}
	}

	/**
	 * Get the CIC at <code>point</code> in the Intention View coordinate space, according to the intersection rules of
	 * <code>CIntentionCell.contains()</code>, or <code>-1L</code> if no cell is there.
	 */
	public long getCellAt(Point point)
	{
		for (CIntentionCell cell : cells.values())
		{
			if (cell.contains(point))
			{
				return cell.getId();
			}
		}
		return -1L;
	}
	
	/**
	 * Get the CIC at <code>point</code> in the Intention View coordinate space, according to the intersection rules of
	 * <code>CIntentionCell.contains()</code>, or <code>-1L</code> if no cell is there.
	 * 
	 * CellIdToIgnore is helpful when cells may be overlapping.
	 */
	public long getCellAt(Point point, long cellIdToIgnore)
	{
		for (CIntentionCell cell : cells.values())
		{
			if (cell.contains(point) && cell.getId() != cellIdToIgnore)
			{
				return cell.getId();
			}
		}
		return -1L;
	}

	/**
	 * Get the canvas id for the root canvas of the cluster containing <code>memberCanvasId</code>.
	 */
	public long getClusterRootCanvasId(long memberCanvasId)
	{
		long parentCanvasId = -1L;
		for (long anchorId : CCanvasLinkController.getInstance().getAnchorIdsByCanvasId(memberCanvasId))
		{
			CCanvasLinkAnchor anchor = CCanvasLinkController.getInstance().getAnchor(anchorId);
			if (anchor.getLink().getAnchorB() == anchor)
			{
				parentCanvasId = anchor.getOpposite().getCanvasId();
				break;
			}
		}

		if (parentCanvasId < 0L)
		{
			return memberCanvasId;
		}

		return getClusterRootCanvasId(parentCanvasId);
	}
	
	public long getCIntentionCellParent(long memberCanvasId)
	{
		long parentCanvasId = -1L;
		for (long anchorId : CCanvasLinkController.getInstance().getAnchorIdsByCanvasId(memberCanvasId))
		{
			CCanvasLinkAnchor anchor = CCanvasLinkController.getInstance().getAnchor(anchorId);
			if (anchor.getLink().getAnchorB() == anchor)
			{
				parentCanvasId = anchor.getOpposite().getCanvasId();
				break;
			}
		}

		if (parentCanvasId < 0L)
		{
			parentCanvasId = 0l;
		}
		
		return parentCanvasId;
	}
	
	public static void updateCells()
	{
		long[] keySet = cells.keySet().toLongArray();
		for(int i=0;i<keySet.length;i++)
		{
//			cells.get(keySet[i]).up;
				
		}
	}
	
	public boolean isRootCanvas(long memberCanvasId)
	{
		return getCIntentionCellParent(memberCanvasId) == 0l;
	}
	
	public boolean isChildOfRootCanvas(long memberCanvasId)
	{
		boolean isRootChildCanvas = CIntentionCellController.getInstance().isRootCanvas(
				CIntentionCellController.getInstance().getCIntentionCellParent(memberCanvasId));
		return isRootChildCanvas;
	}
	
	public long[] getCIntentionCellChildren(long memberCanvasId)
	{
		ArrayList<Long> children = new ArrayList<Long>();
		
		for (long anchorId : CCanvasLinkController.getInstance().getAnchorIdsByCanvasId(memberCanvasId))
		{
			CCanvasLinkAnchor anchor = CCanvasLinkController.getInstance().getAnchor(anchorId);
			if (anchor.getLink().getAnchorA() == anchor)
			{
				children.add(new Long(anchor.getOpposite().getCanvasId()));
			}
		}

		if (children.size() < 0)
		{
			return new long[] { 0 };
		}
		
		long[] childrenAsLongs = ArrayUtils.toPrimitive(children.toArray(new Long[children.size()]));
		
		return childrenAsLongs;
	}
	
	/**
	 * Initialize all CICs.
	 */
	public void initializeDisplay()
	{
		for (CIntentionCell cell : cells.values())
		{
			cell.initialize();
		}
	}

	/**
	 * Return the number of canvases which are tagged with <code>typeId</code>.
	 */
	public int countIntentionTypeUsage(long typeId)
	{
		int count = 0;
		for (CIntentionCell cell : cells.values())
		{
			if (cell.getIntentionTypeId() == typeId)
			{
				count++;
			}
		}
		return count;
	}

	/**
	 * Untag all canvases which are currently tagged with <code>typeId</code>.
	 */
	public void removeIntentionTypeReferences(long typeId)
	{
		for (CIntentionCell cell : cells.values())
		{
			if (cell.getIntentionTypeId() == typeId)
			{
				cell.clearIntentionType();
			}
		}
	}

	/**
	 * Enable or disable iconification mode. This feature is obsolete.
	 */
	public void activateIconifyMode(boolean b)
	{
		IntentionGraph.getInstance().activateIconifyMode(b);

		for (CIntentionCell cell : cells.values())
		{
			cell.updateIconification();
		}
	}

	/**
	 * Notify all CICs that the user presence in at least one CIC has changed (not sure why it doesn't just update the
	 * changed CICs--probably an unchaged CIC will not do anything anyway).
	 */
	public void updateUserLists()
	{
		for (CIntentionCell cell : cells.values())
		{
			cell.updateUserList();
		}
	}

	/**
	 * Delete <code>canvsaId</code>, sending the command directly to the server. Removal of visual components and
	 * related plugin model elements will occur on each client when the server broadcasts deletion.
	 */
	public void deleteCanvas(long canvasId)
	{
		CalicoPacket packet = new CalicoPacket();
		packet.putInt(NetworkCommand.CANVAS_DELETE);
		packet.putLong(canvasId);

		packet.rewind();
		PacketHandler.receive(packet);
		Networking.send(packet);
	}

	/**
	 * Move the pixel position of the Piccolo component of CIC <code>cellId</code> in the Intention View to
	 * <code>x, y</code>. Does not contact the server.
	 */
	public void moveCellLocal(long cellId, double x, double y)
	{
		cells.get(cellId).setLocation(x, y);
		IntentionGraphController.getInstance().localUpdateAttachedArrows(cellId, x, y);
	}

	/**
	 * Move the CIC <code>cellId</code> to <code>x, y</code> in the Intention View's coordinate space, sending the
	 * command directly to the server and adjusting no visual components. Each client will render the change when the
	 * server broadcasts it.
	 */
	public void moveCell(long cellId, double x, double y)
	{
		CalicoPacket packet = new CalicoPacket();
		packet.putInt(IntentionalInterfacesNetworkCommands.CIC_MOVE);
		packet.putLong(cellId);
		packet.putInt((int) x);
		packet.putInt((int) y);

		packet.rewind();
		PacketHandler.receive(packet);
		Networking.send(packet);
	}

	/**
	 * Change the title of the canvas associated with <code>cellId</code> by dropping a command in this client's
	 * incoming command pipeline. If <code>!local</code>, the command will also be sent to the server and broadcast to
	 * all other clients.
	 */
	public void setCellTitle(long cellId, String title, boolean local)
	{
		CalicoPacket packet = new CalicoPacket();
		packet.putInt(IntentionalInterfacesNetworkCommands.CIC_SET_TITLE);
		packet.putLong(cellId);
		packet.putString(title);

		packet.rewind();
		PacketHandler.receive(packet);
		if (!local)
		{
			Networking.send(packet);
		}
	}

	/**
	 * Toggle tag <code>typeId</code> of the canvas associated with <code>cellId</code> by dropping a command in this
	 * client's incoming command pipeline. If <code>!local</code>, the command will also be sent to the server and
	 * broadcast to all other clients.
	 */
	public void toggleCellIntentionType(long cellId, long typeId, boolean add, boolean local)
	{
		CalicoPacket packet = new CalicoPacket();
		packet.putInt(add ? IntentionalInterfacesNetworkCommands.CIC_TAG : IntentionalInterfacesNetworkCommands.CIC_UNTAG);
		packet.putLong(cellId);
		packet.putLong(typeId);

		packet.rewind();
		PacketHandler.receive(packet);
		if (!local)
		{
			Networking.send(packet);
		}
	}

	/**
	 * Delete the plugin model elements and visual components associated with <code>cellId</code>.
	 */
	public void localDeleteCell(long cellId)
	{
		CIntentionCell cell = cells.remove(cellId);
		cellsByCanvasId.remove(cell.getCanvasId());
		cell.delete();

//		IntentionGraph.getInstance().repaint();
	}

	/**
	 * Create and install plugin model elements and visual components for new CIC <code>cell</code>.
	 */
	public void addCell(CIntentionCell cell)
	{
		cells.put(cell.getId(), cell);
		cellsByCanvasId.put(cell.getCanvasId(), cell);

		CalicoInputManager.addCustomInputHandler(cell.getId(), CIntentionCellInputHandler.getInstance());
	}

	public CIntentionCell getCellById(long uuid)
	{
		if (uuid < 0L)
		{
			return null;
		}
		return cells.get(uuid);
	}

	public CIntentionCell getCellByCanvasId(long canvas_uuid)
	{
		return cellsByCanvasId.get(canvas_uuid);
	}

	// debug info
	public String listVisibleCellAddresses()
	{
		StringBuilder buffer = new StringBuilder("{");
		List<CIntentionCell> orderedCells = new ArrayList<CIntentionCell>(cells.values());
		Collections.sort(orderedCells, new CellAddressSorter());

		for (CIntentionCell cell : orderedCells)
		{
			buffer.append(CCanvasController.canvasdb.get(cell.getCanvasId()).getIndex());
			buffer.append(", ");
		}
		if (buffer.length() > 1)
		{
			buffer.setLength(buffer.length() - 2);
		}
		buffer.append("}");

		return buffer.toString();
	}

	// for debug info
	private static class CellAddressSorter implements Comparator<CIntentionCell>
	{
		public int compare(CIntentionCell first, CIntentionCell second)
		{
			return CCanvasController.canvasdb.get(first.getCanvasId()).getIndex() - CCanvasController.canvasdb.get(second.getCanvasId()).getIndex();
		}
	}
	
	/**
	 * Discern whether an arrow exists from <code>target</code> to <code>canvasIdOfAnchorA</code>. This is used to avoid
	 * creating cycles in the graph of arrows.
	 */
	public boolean isParent(long targetCanvasId, long canvasIdOfParent)
	{
		CIntentionCell parent = getCellByCanvasId(canvasIdOfParent);
		CCanvasLinkAnchor incomingAnchor = null;
		for (Long anchorId : CCanvasLinkController.getInstance().getAnchorIdsByCanvasId(targetCanvasId))
		{
			CCanvasLinkAnchor anchor = CCanvasLinkController.getInstance().getAnchor(anchorId);
			if (anchor.getLink().getAnchorB() == anchor)
			{
				incomingAnchor = anchor;
				break;
			}
		}

		if (incomingAnchor == null)
		{
			return false;
		}

		if (incomingAnchor.getOpposite().getCanvasId() == parent.getCanvasId())
		{
			System.out.println("Cycle detected on canvas id " + parent.getCanvasId());
			return true;
		}

		return isParent(incomingAnchor.getOpposite().getCanvasId(), parent.getCanvasId());
	}

	public void hideCellsOutsideOfCluster(long cluster) {
		// 

		for (CIntentionCell cell : cells.values())
		{
			if (this.getClusterRootCanvasId(cell.getCanvasId()) != cluster)
			{
				cell.hide();
			}
			else
				cell.show();
		}
		
		long[] arrows = calico.plugins.iip.controllers.IntentionGraphController.getInstance().getArrowLinkKeySet();
		for (int i = 0; i < arrows.length; i++)
		{
			CCanvasLinkArrow arrow = calico.plugins.iip.controllers.IntentionGraphController.getInstance().getArrowByLinkId(
					arrows[i]);
			if (this.getClusterRootCanvasId(arrow.getAnchorA().getCanvasId()) != cluster)
			{
				arrow.setVisible(false);
			}
			else
				arrow.setVisible(true);
		}

	}

	public void showAllCells() {
		// TODO Auto-generated method stub
		for (CIntentionCell cell : cells.values())
		{

			cell.show();

		}
		
		long[] arrows = calico.plugins.iip.controllers.IntentionGraphController.getInstance().getArrowLinkKeySet();
		for (int i = 0; i < arrows.length; i++)
		{
			CCanvasLinkArrow arrow = calico.plugins.iip.controllers.IntentionGraphController.getInstance().getArrowByLinkId(
					arrows[i]);
			arrow.setVisible(true);
		}
	}
}
