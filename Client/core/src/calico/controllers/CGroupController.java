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
package calico.controllers;

import it.unimi.dsi.fastutil.longs.Long2ReferenceAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import calico.Calico;
import calico.CalicoDataStore;
import calico.CalicoDraw;
import calico.CalicoOptions.arrow;
import calico.components.arrow.AnchorPoint;
import calico.components.arrow.CArrow;
import calico.components.CConnector;
import calico.components.CGroup;
import calico.components.CGroupImage;
import calico.components.CanvasViewScrap;
import calico.components.bubblemenu.BubbleMenu;
import calico.components.composable.ComposableElement;
import calico.components.composable.ComposableElementController;
import calico.components.decorators.CGroupDecorator;
import calico.components.piemenu.PieMenu;
import calico.components.piemenu.PieMenuButton;
import calico.inputhandlers.CalicoInputManager;
import calico.networking.Networking;
import calico.networking.PacketHandler;
import calico.networking.netstuff.ByteUtils;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;

/**
 * This handles all canvas requests
 * CGroupController
 * @author Mitch Dempsey
 */
public class CGroupController
{
	public static Logger logger = Logger.getLogger(CGroupController.class.getName());
	
	public static Long2ReferenceOpenHashMap<CGroup> groupdb = new Long2ReferenceOpenHashMap<CGroup>();
	
	private static LongArraySet delete_groups = new LongArraySet();
		
	private static long currentGroupUUID = 0L;
	private static long lastGroupUUID = 0L;
	
	private static long group_copy_uuid = 0L;
	public static boolean restoreOriginalStroke = false;
	//public static long originalStroke = 0l;
	public static CalicoPacket originalStroke = null;
	
	private static List<Listener> listeners = new ArrayList<Listener>();

	public static interface Listener
	{
		void groupMoved(long uuid);
		
		void groupDeleted(long uuid);
	}
	
	public static void setCopyUUID(long u)
	{
		group_copy_uuid = u;
	}
	public static long getCopyUUID()
	{
		return group_copy_uuid;
	}
	
	public static void addListener(Listener listener)
	{
		listeners.add(listener);
	}
	
	public static void removeListener(Listener listener)
	{
		listeners.remove(listener);
	}
	
	private static void informListenersOfMove(long uuid)
	{
		for (Listener listener : listeners)
		{
			listener.groupMoved(uuid);
		}
	}
	
	public static boolean dq_add(long uuid)
	{
		return delete_groups.add(uuid);
	}
	
	
	
	
	// Does nothing right now
	public static void setup()
	{
		groupdb.clear();
//		pieMenuButtons.clear();
		delete_groups.clear();
		
//		registerPieMenuButton(calico.components.piemenu.groups.GroupDropButton.class);
//		registerPieMenuButton(calico.components.piemenu.groups.GroupSetPermanentButton.class);
//		registerPieMenuButton(calico.components.piemenu.groups.GroupShrinkToContentsButton.class);
//		registerPieMenuButton(calico.components.piemenu.groups.ListCreateButton.class);
//		registerPieMenuButton(calico.components.piemenu.groups.GroupMoveButton.class);
//		registerPieMenuButton(calico.components.piemenu.groups.GroupCopyDragButton.class);
//		registerPieMenuButton(calico.components.piemenu.groups.GroupRotateButton.class);
//		registerPieMenuButton(calico.components.piemenu.canvas.ArrowButton.class);
//		registerPieMenuButton(calico.components.piemenu.groups.GroupDeleteButton.class);
//		registerPieMenuButton(calico.components.piemenu.canvas.TextCreate.class);
		
		
//		registerPieMenuButton(calico.components.piemenu.groups.GroupCopyButton.class);
//		registerPieMenuButton(calico.components.piemenu.groups.GroupConvexHullButton.class);
//		registerPieMenuButton(calico.components.piemenu.groups.GroupIncreaseSizeButton.class);
	}
	
	public static boolean exists(long uuid)
	{
		return groupdb.containsKey(uuid);
	}
	
	
	public static long getCurrentUUID()
	{
		return currentGroupUUID;
	}
	public static void setCurrentUUID(long u)
	{
		currentGroupUUID = u;
	}
	
	public static void setLastCreatedGroupUUID(long u)
	{
		lastGroupUUID = u;
	}
	public static long getLastCreatedGroupUUID()
	{
		return lastGroupUUID;
	}
	
	
	
	/**
	 * This will make sure to remove whatever was the last temporary group that you created
	 */
	public static void checkToRemoveLastTempGroup()
	{
		checkToRemoveLastTempGroup(0L);
	}
	
	// actionUUID == the uuid we are working with (if it matches the group uuid, we abort)
	public static void checkToRemoveLastTempGroup(long actionUUID)
	{
		if(lastGroupUUID==0L || actionUUID==lastGroupUUID)
			return;
		
		if(!exists(lastGroupUUID))
		{
			lastGroupUUID = 0L;
			return;
		}
		
		if(!groupdb.get(lastGroupUUID).isPermanent())
		{
			drop(lastGroupUUID);
		}
	}
	
	public static boolean checkIfLastTempGroupExists()
	{
		if(!exists(lastGroupUUID))
		{
			return false;
		}
		
		if(!groupdb.get(lastGroupUUID).isPermanent())
		{
			return false;
		}
		
		return true;
	}
	
	
	
	
	public static boolean is_parented_to(long uuid, long puid)
	{
		if (groupdb.get(uuid) == null)
			return false;
		return (groupdb.get(uuid).getParentUUID()==puid);
	}
	
	
	public static void no_notify_add_arrow(long uuid, long auuid)
	{
		if(!exists(uuid)){return;}
		
		groupdb.get(uuid).addChildArrow(auuid);
	}
	
	public static void no_notify_add_connector(long uuid, long cuuid)
	{
		if(!exists(uuid)){return;}
		
		groupdb.get(uuid).addChildConnector(cuuid);
	}
	

	
	public static int get_signature(long uuid)
	{
		if(!exists(uuid)){return 0;}
		
		return groupdb.get(uuid).get_signature();
	}
	
	public static String get_signature_debug_output(long uuid)
	{
		if(!exists(uuid)){return "";}
		
		return groupdb.get(uuid).get_signature_debug_output();
	}
	
	
	
	/*
	 * TODO:setup some kind of nonotify_move functions to allow the server to not send (and not need an if/else)  
	 *
	 */
	
	public static void no_notify_start(long uuid, long cuid, long puid, boolean isperm)
	{
		if (!CCanvasController.exists(cuid))
			return;
		if(exists(uuid))
		{
			logger.debug("Need to delete group "+uuid);
			// WHOAA WE NEED TO DELETE THIS SHIT
			//CCanvasController.canvasdb.get(cuid).getLayer().removeChild(groupdb.get(uuid));
			CalicoDraw.removeChildFromNode(CCanvasController.canvasdb.get(cuid).getLayer(), groupdb.get(uuid));
//			CCanvasController.canvasdb.get(cuid).getCamera().repaint();
		}
		
		// Add to the GroupDB
		groupdb.put(uuid, new CGroup(uuid, cuid, puid, isperm));
		
		CCanvasController.canvasdb.get(cuid).addChildGroup(uuid);
		
		//CCanvasController.canvasdb.get(cuid).getLayer().addChild(groupdb.get(uuid));
		CalicoDraw.addChildToNode(CCanvasController.canvasdb.get(cuid).getLayer(), groupdb.get(uuid));
		
		groupdb.get(uuid).drawPermTemp(true);
		
		
		
		
		//CCanvasController.canvasdb.get(cuid).repaint();
	}
	
	public static void no_notify_append(long uuid, int x, int y)
	{
		// If we don't know wtf this UUID is for, then just eject
		if(!exists(uuid))
		{
			logger.warn("APPEND for non-existant group "+uuid);
			return;
		}
		
		groupdb.get(uuid).append(x, y);
	}
	
	public static void no_notify_append(long uuid, int[] x, int[] y)
	{
		// If we don't know wtf this UUID is for, then just eject
		if(!exists(uuid))
		{
			logger.warn("APPEND for non-existant group "+uuid);
			return;
		}
		
		groupdb.get(uuid).append(x, y);
	}
	
	public static void no_notify_move(long uuid, int x, int y)
	{
		// If we don't know wtf this UUID is for, then just eject
		if(!exists(uuid))
		{
			logger.warn("MOVE for non-existant group "+uuid);
			return;
		}
		
		groupdb.get(uuid).move(x, y);
		informListenersOfMove(uuid);
		if (BubbleMenu.isBubbleMenuActive() && BubbleMenu.activeUUID == uuid)
		{
			BubbleMenu.moveIconPositions(CGroupController.groupdb.get(uuid).getBounds());
		}
	}
	
	public static void no_notify_delete(final long uuid)
	{
		// If we don't know wtf this UUID is for, then just eject
		if(!exists(uuid))
		{
			logger.warn("DELETE for non-existant group "+uuid);
			return;
		}
		
		
		if (restoreOriginalStroke && originalStroke != null && uuid == lastGroupUUID)
		{
			//CStrokeController.unhideStroke(originalStroke);
			batchReceive(new CalicoPacket[]{originalStroke});
			Networking.send(originalStroke);
			originalStroke = null;
			restoreOriginalStroke = false;
		}
		/*else if (originalStroke != null)
		{
			//CStrokeController.delete(originalStroke);
			originalStroke = null;
		}*/
		
		//The purpose of this block is to achieve smoother drawing, but it is not thread safe
		/*groupdb.get(uuid).setTransparency(0f);
		if (groupdb.get(uuid).getBounds() != null)
		{
		int buffer = 5;
			Rectangle bounds = groupdb.get(uuid).getBounds().getBounds();
			Rectangle boundsWithBuffer =  new Rectangle(bounds.x - buffer, bounds.y - buffer, bounds.width + buffer * 2, bounds.height + buffer * 2);
			CCanvasController.canvasdb.get(groupdb.get(uuid).getCanvasUID()).repaint(boundsWithBuffer);
			groupdb.get(uuid).repaint();
		}*/
		
		// TODO: This should also delete the elements inside of the group first
		groupdb.get(uuid).delete();

		//CCanvasController.canvasdb.get(groupdb.get(uuid).getCanvasUID()).getLayer().removeChild(groupdb.get(uuid));
		
		//This ain't pretty, but its needed for thread safety.
		/*SwingUtilities.invokeLater(
			new Runnable() { 
				public void run() {*/

		if(!exists(uuid))
		{
			//Even if we get this warning it should be ok. 
			//It means the AWT eventqueue did not process this remove and a duplicate delete request came.
			//The rest of the code in this method should deal with it ok and the code in here deals with it through this warning
			logger.warn("DELETE for non-existant group "+uuid);
		}
		else
		{
			if(CCanvasController.canvas_has_child_group_node(groupdb.get(uuid).getCanvasUID(), uuid))
			{
				//groupdb.get(uuid).removeFromParent();
				CalicoDraw.removeNodeFromParent(groupdb.get(uuid));
				groupdb.remove(uuid);
	
				dq_add(uuid);
			}
			
			for (Listener listener : listeners)
			{
				listener.groupDeleted(uuid);
			}
		}
		
		if (BubbleMenu.isBubbleMenuActive() && BubbleMenu.activeUUID == uuid)
		{
			BubbleMenu.clearMenu();
		}
	
				//} } );
		
	}
	
	public static void no_notify_finish(long uuid, boolean captureChildren)
	{
		boolean checkParenting = true;
		no_notify_finish(uuid, captureChildren, checkParenting, true);
		
	}


	public static void no_notify_finish(long uuid, boolean captureChildren,
			boolean checkParenting, boolean fade) {
		// If we don't know wtf this UUID is for, then just eject
		if(!exists(uuid))
		{
			logger.warn("Error: FINISH for non-existant group "+uuid);
			return;
		}
		
		groupdb.get(uuid).finish(fade);
		if (checkParenting
				&& Networking.connectionState != Networking.ConnectionState.Connecting)
			recheck_parent(uuid);
		if (captureChildren
				&& Networking.connectionState != Networking.ConnectionState.Connecting)
			no_notify_calculate_parenting(uuid, true);
		
//		setLastCreatedGroupUUID(uuid);
	}
	
	public static void no_notify_calculate_parenting(final long uuid, final boolean includeStrokes)
	{
		if(!exists(uuid)){return;}
		
		Point2D mid = groupdb.get(uuid).getMidPoint();
		no_notify_calculate_parenting(uuid, includeStrokes, (int)mid.getX(), (int)mid.getY());
	}
	
	public static void no_notify_calculate_parenting(final long uuid, final boolean includeStrokes, int x, int y)
	{
		if(!exists(uuid)){return;}
		
		groupdb.get(uuid).calculateParenting(includeStrokes, x, y);
	}
	
	public static void recheck_parent(final long uuid)
	{
		if(!exists(uuid)){return;}// old one doesnt exist
		groupdb.get(uuid).recheckParentAfterMove();
	}
	
	public static void no_notify_bold(long uuid)
	{
	}
	public static void no_notify_unbold(long uuid)
	{
	}
	
	public static void no_notify_remove_child_group(final long uuid, final long cguuid)
	{
		if(!exists(uuid)){return;}
		
		groupdb.get(uuid).deleteChildGroup(cguuid);
	}
	
	
	public static void no_notify_drop(long uuid)
	{
		// If we don't know wtf this UUID is for, then just eject
		if(!exists(uuid))
		{
			logger.warn("DROP for non-existant group "+uuid);
			return;
		}
		
		if (BubbleMenu.isBubbleMenuActive() && BubbleMenu.activeUUID == uuid)
		{
			BubbleMenu.clearMenu();
		}
		
		CGroup group = groupdb.get(uuid);
		long[] child_strokes = group.getChildStrokes();
		long[] child_groups = group.getChildGroups();
		long[] child_arrows = group.getChildArrows();
		long[] child_connectors = group.getChildConnectors();
		
		group.unparentAllChildren();
		
		no_notify_delete(uuid);
		// what is it's current parent?

		// Remove from the canvas
//		CCanvasController.no_notify_delete_child_group(group.getCanvasUID(), uuid);
//		groupdb.remove(uuid);
		
		
		// Reparent any strokes
		if(child_strokes.length>0)
		{
			for(int i=0;i<child_strokes.length;i++)
			{
				CStrokeController.recalculateParent(child_strokes[i]);
//				CStrokeController.strokes.get(child_strokes[i]).calculateParent();
			}
		}
		
		// Reparent any groups
		if(child_groups.length>0)
		{
			for(int i=0;i<child_groups.length;i++)
			{
				if (groupdb.containsKey(child_groups[i]))
					groupdb.get(child_groups[i]).recheckParentAfterMove();
				else
					System.err.println("Invalid key found for child group while parenting! Key: " + child_groups[i]);
			}
		}
		
		// Reparent any arrows
		if(child_arrows.length>0)
		{
			for(int i=0;i<child_arrows.length;i++)
			{
				CArrowController.recheck_parent(child_arrows[i]);
//				CArrowController.arrows.get(child_arrows[i]).calculateParent();
			}
		}
		
		// Convert connectors to strokes
		if(child_connectors.length>0)
		{
			for(int i=0;i<child_connectors.length;i++)
			{
				CConnectorController.make_stroke(child_connectors[i]);
			}
		}
	
		
		CalicoInputManager.unlockHandlerIfMatch(uuid);
		// XXXXXXXXXXXXXXXX
//		CCanvasController.canvasdb.get(group.getCanvasUID()).repaint();
	}
	
	public static void no_notify_set_parent(long uuid, long bguuid)
	{
		if(!exists(uuid)){return;}
		
		long curpuid = groupdb.get(uuid).getParentUUID();
		long undecoratedParent = getDecoratedGroup(bguuid); 
		
		if (curpuid == undecoratedParent)
			return;
		
		if (curpuid != 0L && groupdb.get(curpuid) instanceof CGroupDecorator)
			return;
		
		if(curpuid!=0L && exists(curpuid))
		{
			// We should update the current parent
			no_notify_delete_child_group(curpuid, uuid);
		}
		
		groupdb.get(uuid).setParentUUID(undecoratedParent);
		
		if(bguuid!=0L && exists(undecoratedParent))
		{
			long decoratedParent = getDecoratorParent(bguuid);
			Point2D midPoint = CGroupController.groupdb.get(uuid).getMidPoint();
			no_notify_add_child_group(decoratedParent, uuid, (int)midPoint.getX(), (int)midPoint.getY());
		}
	}
	
	public static boolean hasChildGroup(long uuid, long cuuid)
	{
		return groupdb.get(uuid).hasChildGroup(cuuid);
	}
	
	public static void no_notify_set_children(long uuid, long[] bgeuuids, long[] grpuuids)
	{
		// If we don't know wtf this UUID is for, then just eject
		if(!exists(uuid))
		{
			logger.warn("SET_CHILDREN for non-existant group "+uuid);
			return;
		}
		
		groupdb.get(uuid).setChildStrokes(bgeuuids);
		groupdb.get(uuid).setChildGroups(grpuuids);
	}
	
	public static void no_notify_add_child_bge(long uuid, long childUUID)
	{
		// If we don't know wtf this UUID is for, then just eject
		if(!groupdb.containsKey(uuid))
		{
			logger.warn("ADD_CHILD_BGE for non-existant group "+uuid);
			return;
		}
		
		groupdb.get(uuid).addChildStroke(childUUID);
	}
	public static void no_notify_add_child_grp(long uuid, long childUUID, int x, int y)
	{
		// If we don't know wtf this UUID is for, then just eject
		if(!groupdb.containsKey(uuid))
		{
			logger.warn("ADD_CHILD_GROUP for non-existant group "+uuid);
			return;
		}
		
		groupdb.get(uuid).addChildGroup(childUUID, x, y);
	}
	
	public static void no_notify_set_permanent(long uuid, boolean isperm)
	{
		if(!exists(uuid))
		{
			logger.warn("GROUP_SET_PERM for non-existant group "+uuid);
			return;
		}
		
		groupdb.get(uuid).setPermanent(isperm);
		recheck_parent(uuid);
		
		if (BubbleMenu.isBubbleMenuActive() && BubbleMenu.activeUUID == uuid)
		{
			CGroupController.show_group_bubblemenu(uuid, false);
		}
	}
	
	public static void no_notify_set_text(long uuid, String text)
	{
		if(!exists(uuid))
		{
			logger.warn("GROUP_SET_TEXT for non-existant group"+uuid);
			return;
		}
		
		groupdb.get(uuid).setText(text);
	}
	
	public static void no_notify_rectify(long uuid)
	{
		if(!exists(uuid))
		{
			logger.warn("rectify for non-existant group "+uuid);
			return;
		}
		
		groupdb.get(uuid).rectify();
	}
	public static void no_notify_circlify(long uuid)
	{
		if(!exists(uuid))
		{
			logger.warn("circlify for non-existant group "+uuid);
			return;
		}
		
		groupdb.get(uuid).circlify();
	}
	
	public static void no_notify_set_children_color(long uuid, Color col)
	{
	}
	
	
	public static void no_notify_reload_remove(long uuid)
	{
	}
	public static void no_notify_reload_start(long uuid, long cuid, long puid, boolean isPerm)
	{
	}
	public static void no_notify_reload_finish(long uuid)
	{
	}
	public static void no_notify_reload_coords(long uuid, int x, int y)
	{
	}
	public static void no_notify_reload_coords(long uuid, int[] x, int[] y)
	{
	}
	
	
	
	
	/*
	 * THESE VERSIONS WILL SEND OUT PACKETS TO NOTIFY OF MANIPULATION
	 * 
	 */
	public static void append(long uuid, int x, int y)
	{
		no_notify_append(uuid, x, y);
		
//		CalicoPacket p = new CalicoPacket( ByteUtils.SIZE_OF_INT + ByteUtils.SIZE_OF_LONG + ByteUtils.SIZE_OF_SHORT + ByteUtils.SIZE_OF_SHORT );
//		p.putInt(NetworkCommand.GROUP_APPEND);
//		p.putLong(uuid);
//		p.putInt(x);
//		p.putInt(y);
//		Networking.send(p);
	}
	
	public static long copy_to_canvas(final long uuid)
	{
		Long2ReferenceArrayMap<Long> UUIDMappings = new Long2ReferenceArrayMap<Long>();
		
		long new_uuid = Calico.uuid();
		UUIDMappings.put(uuid, new Long(new_uuid));
		UUIDMappings.putAll(getMappings(uuid, true));

		no_notify_copy(uuid, 0l, UUIDMappings, true);

		Networking.send(getCopyMappingPackets(uuid, UUIDMappings));

		return new_uuid;
	}
	
	 public static CalicoPacket getCopyMappingPackets(long uuid, Long2ReferenceArrayMap<Long> UUIDMappings) {

			int packetSize = ByteUtils.SIZE_OF_INT
					+ ByteUtils.SIZE_OF_LONG + ByteUtils.SIZE_OF_BYTE
					+ ByteUtils.SIZE_OF_SHORT
					+ (2 * UUIDMappings.size() * ByteUtils.SIZE_OF_LONG);

			CalicoPacket packet = new CalicoPacket(packetSize);
			// UUID CUID PUID <COLOR> <NUMCOORDS> x1 y1
			packet.putInt(NetworkCommand.GROUP_COPY_WITH_MAPPINGS);
			packet.putLong(uuid);
			packet.putInt(UUIDMappings.size());
			LongIterator iterator = UUIDMappings.keySet().iterator();
			while (iterator.hasNext()) {
				long key = iterator.nextLong();
				packet.putLong(key);
				packet.putLong(UUIDMappings.get(key).longValue());
			}

			return packet;
		}
	
	public static void no_notify_copy(final long uuid, final long new_puuid, Long2ReferenceArrayMap<Long> UUIDMappings, boolean isRoot)
	{
		if(!exists(uuid)){return;}// old one doesnt exist
		
		//Since this operation can potentially take a long time, we don't want the client
		//trying to resync during it
		Networking.ignoreConsistencyCheck = true;
		
		CGroup temp = groupdb.get(uuid);
		long new_uuid = UUIDMappings.get(uuid).longValue();
		long canvasuuid = temp.getCanvasUID();
		CalicoPacket[] packets;
		
		if (temp instanceof CGroupDecorator)
		{
			long old_decoratorChildUUID = ((CGroupDecorator)temp).getDecoratedUUID();
			if (UUIDMappings.containsKey(old_decoratorChildUUID))
			{
				long new_decoratorChildUUID = UUIDMappings.get(old_decoratorChildUUID).longValue();
				no_notify_copy(old_decoratorChildUUID, new_uuid, UUIDMappings, false);
				
				ArrayList<Long> subGroups = getSubGroups(old_decoratorChildUUID);
				Long2ReferenceArrayMap<Long> subGroupMappings = new Long2ReferenceArrayMap<Long>();
				for (Long sub_uuid: subGroups)
				{
					if (UUIDMappings.containsKey(sub_uuid))
					{
						subGroupMappings.put(sub_uuid, UUIDMappings.get(sub_uuid));
					}
				}
				packets = ((CGroupDecorator)temp).getDecoratorUpdatePackets(new_uuid, canvasuuid, new_puuid, new_decoratorChildUUID, subGroupMappings);
				batchReceive(packets);
				
				CGroupController.groupdb.get(new_uuid).setChildGroups(new long[] { new_decoratorChildUUID } );

			}
		}
		else
		{
			packets = groupdb.get(uuid).getUpdatePackets(new_uuid, canvasuuid, new_puuid, 0, 0, false);
		
			batchReceive(packets);
			
			CGroup tempNew = groupdb.get(new_uuid);
			
			// DEAL WITH THE CHILDREN
			
			// Child stroke elements
			long[] bge_uuids = temp.getChildStrokes();
			long[] new_bge_uuids = new long[bge_uuids.length];
			
			if(bge_uuids.length>0)
			{
				for(int i=0;i<bge_uuids.length;i++)
				{
					if ((UUIDMappings.containsKey(bge_uuids[i])))
					{
						new_bge_uuids[i] = UUIDMappings.get(bge_uuids[i]).longValue();
						CStrokeController.no_notify_copy(bge_uuids[i], new_bge_uuids[i], new_uuid, canvasuuid, 0, 0);
					}
				}
				tempNew.clearChildStrokes();
				tempNew.setChildStrokes(new_bge_uuids);
	//			for(int i = 0; i < new_bge_uuids.length; i++)
	//			{
	//				tempNew.addChildStroke(new_bge_uuids[i]);
	//			}
			}
			
			//Child group elements
			long[] grp_uuids = temp.getChildGroups();
			long[] new_grp_uuids = new long[grp_uuids.length];
			
			if(grp_uuids.length>0)
			{
				for(int i=0;i<grp_uuids.length;i++)
				{
					if ((UUIDMappings.containsKey(grp_uuids[i])))
					{
						new_grp_uuids[i] = UUIDMappings.get(grp_uuids[i]).longValue();
						no_notify_copy(grp_uuids[i], new_uuid, UUIDMappings, false);
					}
				}
				tempNew.setChildGroups(new_grp_uuids);
			}
			
			//Child arrow elements
			/*long[] arrow_uuids = temp.getChildArrows();
			long[] new_arw_uuids = new long[arrow_uuids.length];
			
			if(arrow_uuids.length>0)
			{
				for(int i=0;i<arrow_uuids.length;i++)
				{				
					CArrow tempA = CArrowController.arrows.get(arrow_uuids[i]);
					if(tempA.getAnchorA().getUUID()==uuid && tempA.getAnchorB().getUUID()==uuid){	
						if ((UUIDMappings.containsKey(arrow_uuids[i])))
						{
							new_arw_uuids[i] = UUIDMappings.get(arrow_uuids[i]).longValue();
											
							AnchorPoint anchorA = tempA.getAnchorA().clone();
							AnchorPoint anchorB = tempA.getAnchorB().clone();				

							if (anchorA.getUUID() == uuid)
							{
								anchorA.setUUID(new_uuid);
							}
							if (anchorB.getUUID() == uuid)
							{
								anchorB.setUUID(new_uuid);
							}
							CArrowController.no_notify_start(new_arw_uuids[i], canvasuuid, tempA.getColor(), tempA.getArrowType(), anchorA, anchorB);
						}
					}
				}
			}*/
		}	
		
		if (isRoot)
		{
			long[] arrow_uuids  = CCanvasController.canvasdb.get(canvasuuid).getChildArrows();
			for (int i = 0; i < arrow_uuids.length; i++)
			{
				CArrow tempArrow = CArrowController.arrows.get(arrow_uuids[i]);
				if ((UUIDMappings.containsKey(tempArrow.getAnchorA().getUUID()) || tempArrow.getAnchorA().getUUID() == uuid) && 
					(UUIDMappings.containsKey(tempArrow.getAnchorB().getUUID()) || tempArrow.getAnchorB().getUUID() == uuid))
				{
					long new_arrow_uuid = UUIDMappings.get(arrow_uuids[i]).longValue();
					
					AnchorPoint anchorA = tempArrow.getAnchorA().clone();
					AnchorPoint anchorB = tempArrow.getAnchorB().clone();
					
					if (UUIDMappings.containsKey(anchorA.getUUID()) && UUIDMappings.containsKey(anchorB.getUUID()))
					{
						anchorA.setUUID(UUIDMappings.get(anchorA.getUUID()).longValue());
						anchorB.setUUID(UUIDMappings.get(anchorB.getUUID()).longValue());
						CArrowController.no_notify_start(new_arrow_uuid, canvasuuid, tempArrow.getColor(), tempArrow.getArrowType(), anchorA, anchorB);
					}
				}
			}
		}
		
		//Connectors
		if (isRoot)
		{
			long[] connector_uuids  = CCanvasController.canvasdb.get(canvasuuid).getChildConnectors();
			for (int i = 0; i < connector_uuids.length; i++)
			{
				CConnector tempConnector = CConnectorController.connectors.get(connector_uuids[i]);
				if ((UUIDMappings.containsKey(tempConnector.getAnchorUUID(CConnector.TYPE_HEAD)) || tempConnector.getAnchorUUID(CConnector.TYPE_HEAD) == uuid) && 
					(UUIDMappings.containsKey(tempConnector.getAnchorUUID(CConnector.TYPE_TAIL)) || tempConnector.getAnchorUUID(CConnector.TYPE_TAIL) == uuid))
				{
					long new_connector_uuid = UUIDMappings.get(connector_uuids[i]).longValue();
					
					
					if (UUIDMappings.containsKey(tempConnector.getAnchorUUID(CConnector.TYPE_HEAD)) && 
						UUIDMappings.containsKey(tempConnector.getAnchorUUID(CConnector.TYPE_TAIL)))
					{
						Point head = (Point) tempConnector.getHead().clone();
						Point tail = (Point) tempConnector.getTail().clone();
						
						CConnectorController.no_notify_create(new_connector_uuid, canvasuuid, tempConnector.getColor(), tempConnector.getThickness(), head, tail,
								tempConnector.getOrthogonalDistance(), tempConnector.getTravelDistance(), 
								UUIDMappings.get(tempConnector.getAnchorUUID(CConnector.TYPE_HEAD)), UUIDMappings.get(tempConnector.getAnchorUUID(CConnector.TYPE_TAIL)));
						if (ComposableElementController.elementList.containsKey(connector_uuids[i]))
						{
							Long2ReferenceAVLTreeMap<ComposableElement> componentElements = ComposableElementController.elementList.get(connector_uuids[i]);
							for (Map.Entry<Long, ComposableElement> entry : componentElements.entrySet())
							{
								if (UUIDMappings.containsKey(entry.getKey()))
								{
									long new_element_uuid = UUIDMappings.get(entry.getKey()).longValue();
									packets = new CalicoPacket[]{entry.getValue().getPacket(new_element_uuid, new_connector_uuid)};
									batchReceive(packets);
								}
							}
						}
					}
				}
			}
		}
		
		Networking.ignoreConsistencyCheck = false;
	}//no_notify_copy
	
	private static ArrayList<Long> getSubGroups(long uuid)
	{
		if(!exists(uuid)){return null;}// doesn't exist
		
		ArrayList<Long> childGroups = new ArrayList<Long>();
		
		CGroup temp = groupdb.get(uuid);
					
		if (temp instanceof CGroupDecorator)
		{
			childGroups.addAll(getSubGroups(((CGroupDecorator)temp).getDecoratedUUID()));
		}
		else
		{						
			//Child group elements
			long[] grp_uuids = temp.getChildGroups();
			
			if(grp_uuids.length>0)
			{
				for(int i=0;i<grp_uuids.length;i++)
				{
					childGroups.add(grp_uuids[i]);
					childGroups.addAll(getSubGroups(grp_uuids[i]));
				}
			}
		}

		return childGroups;	
	}
	
	private static void batchReceive(CalicoPacket[] packets)
	{
		for (int i = 0; i < packets.length; i++)
		{
			if (packets[i] == null)
			{
				logger.warn("WARNING!!! BatchReceive received a null packet, something likely went wrong!");
				continue;
			}
			
			CalicoPacket p = new CalicoPacket(packets[i].getBuffer());
			PacketHandler.receive(p);
		}
	}
	
	public static Long2ReferenceArrayMap<Long> getMappings(final long uuid, boolean isRoot)
	{
		if(!exists(uuid)){return null;}// doesn't exist
		
		Long2ReferenceArrayMap<Long> UUIDMappings = new Long2ReferenceArrayMap<Long>();
		
		CGroup temp = groupdb.get(uuid);
		long canvasuuid = temp.getCanvasUID();
		
		if (temp instanceof CGroupDecorator)
		{
			long new_decoratorChildUUID = Calico.uuid();
			long old_decoratorChildUUID = ((CGroupDecorator)temp).getDecoratedUUID();
			UUIDMappings.put(old_decoratorChildUUID, new Long(new_decoratorChildUUID));
			
			Long2ReferenceArrayMap<Long> subGroupMappings = getMappings(old_decoratorChildUUID, false);
			if (subGroupMappings != null)
				UUIDMappings.putAll(subGroupMappings);
		}
		else
		{			
			// DEAL WITH THE CHILDREN
			
			// Child stroke elements
			long[] bge_uuids = temp.getChildStrokes();
			long[] new_bge_uuids = new long[bge_uuids.length];
			
			if(bge_uuids.length>0)
			{
				for(int i=0;i<bge_uuids.length;i++)
				{
					new_bge_uuids[i] = Calico.uuid();
					UUIDMappings.put(bge_uuids[i], new Long(new_bge_uuids[i]));
				}
			}
			
			//Child group elements
			long[] grp_uuids = temp.getChildGroups();
			long[] new_grp_uuids = new long[grp_uuids.length];
			
			if(grp_uuids.length>0)
			{
				for(int i=0;i<grp_uuids.length;i++)
				{
					new_grp_uuids[i] = Calico.uuid();
					UUIDMappings.put(grp_uuids[i], new Long(new_grp_uuids[i]));
					Long2ReferenceArrayMap<Long> subGroupMappings = getMappings(grp_uuids[i], false);

					if (subGroupMappings != null)
						UUIDMappings.putAll(subGroupMappings);
				}
			}
			
			//Child arrow elements
			/*long[] arrow_uuids = temp.getChildArrows();
			long[] new_arw_uuids = new long[arrow_uuids.length];
			
			if(arrow_uuids.length>0)
			{
				for(int i=0;i<arrow_uuids.length;i++)
				{					
					new_arw_uuids[i] = Calico.uuid();
					UUIDMappings.put(arrow_uuids[i], new Long(new_arw_uuids[i]));
				}
			}*/
		}
		
		//Arrows
		if (isRoot)
		{
			long[] arrow_uuids  = CCanvasController.canvasdb.get(canvasuuid).getChildArrows();
			for (int i = 0; i < arrow_uuids.length; i++)
			{
				CArrow tempArrow = CArrowController.arrows.get(arrow_uuids[i]);
				if ((UUIDMappings.containsKey(tempArrow.getAnchorA().getUUID()) || tempArrow.getAnchorA().getUUID() == uuid) && 
					(UUIDMappings.containsKey(tempArrow.getAnchorB().getUUID()) || tempArrow.getAnchorB().getUUID() == uuid))
				{
					long new_arw_uuids = Calico.uuid();
					UUIDMappings.put(arrow_uuids[i], new Long(new_arw_uuids));
				}
			}
		}
		
		//Connectors
		if (isRoot)
		{
			long[] connector_uuids  = CCanvasController.canvasdb.get(canvasuuid).getChildConnectors();
			for (int i = 0; i < connector_uuids.length; i++)
			{
				CConnector tempConnector = CConnectorController.connectors.get(connector_uuids[i]);
				if ((UUIDMappings.containsKey(tempConnector.getAnchorUUID(CConnector.TYPE_HEAD)) || tempConnector.getAnchorUUID(CConnector.TYPE_HEAD) == uuid) && 
					(UUIDMappings.containsKey(tempConnector.getAnchorUUID(CConnector.TYPE_TAIL)) || tempConnector.getAnchorUUID(CConnector.TYPE_TAIL) == uuid))
				{
					long new_ctr_uuids = Calico.uuid();
					UUIDMappings.put(connector_uuids[i], new Long(new_ctr_uuids));
					if (ComposableElementController.elementList.containsKey(connector_uuids[i]))
					{
						Long2ReferenceAVLTreeMap<ComposableElement> componentElements = ComposableElementController.elementList.get(connector_uuids[i]);
						for (Map.Entry<Long, ComposableElement> entry : componentElements.entrySet())
						{
							long new_element_uuid = Calico.uuid();
							UUIDMappings.put(entry.getKey(), new Long(new_element_uuid));
						}
					}
				}
			}
		}

		return UUIDMappings;	
	}
	
	public static void move(long uuid, int x, int y)
	{
		if (x == 0 && y == 0)
			return;
		
		no_notify_move(uuid, x, y);
		Networking.send(NetworkCommand.GROUP_MOVE, uuid, x, y);
	}

	public static void finish(long uuid, boolean captureChildren)
	{
		no_notify_finish(uuid, captureChildren);
//		Networking.send(NetworkCommand.GROUP_FINISH, uuid);
		loadGroup(uuid, captureChildren);
		setLastCreatedGroupUUID(uuid);
		
		
		//groupdb.get(uuid).extra_submitToDesignMinders();
		
	}
	
	public static void loadGroup(long guuid, boolean captureChildren)
	{
		if (!groupdb.containsKey(guuid))
		{
			System.err.println("Attempting to load a group that does not exist!");
			(new Exception()).printStackTrace();
			return;
		}

		CalicoPacket[] packets = groupdb.get(guuid).getUpdatePackets(captureChildren);
		
		for(int i=0;i<packets.length;i++)
		{
			Networking.send(packets[i]);
		}
	}
	
	
	
	public static void drop(long uuid)
	{
		no_notify_drop(uuid);
		Networking.send(NetworkCommand.GROUP_DROP, uuid);
	}
	public static void delete(long uuid)
	{
		// WE WAIT FOR THE SERVER TO SEND US A DELETE PACKET!
		no_notify_delete(uuid);
		Networking.send(NetworkCommand.GROUP_DELETE, uuid);
	}
	// TODO: Maybe remove the puid alltogether, since we will always force 0L
	
	/**
	 * @deprecated
	 * @see #start(long, long, long, boolean)
	 */
	public static void start(long uuid, long cuid, long puid, int x, int y)
	{
		puid = 0L;//forcing - the server manaages the parents/children
		start(uuid, cuid, puid, false);
		append(uuid, x, y);
	}
	
	public static void start(long uuid, long cuid, long puid, boolean isperm)
	{
		no_notify_start(uuid, cuid, puid, isperm);
//		Networking.send(NetworkCommand.GROUP_START, uuid, cuid, puid, (isperm ? 1 : 0));
	}
	
	
	public static void set_parent(long uuid, long newparent)
	{
		no_notify_set_parent(uuid,newparent);
		Networking.send(NetworkCommand.GROUP_SET_PARENT, uuid, newparent);
	}
	public static void rectify(long uuid)
	{
		no_notify_rectify(uuid);
		Networking.send(NetworkCommand.GROUP_RECTIFY, uuid);
	}
	public static void circlify(long uuid)
	{
		no_notify_circlify(uuid);
		Networking.send(NetworkCommand.GROUP_CIRCLIFY, uuid);
	}
	
	public static void set_permanent(long uuid, boolean isperm)
	{
		no_notify_set_permanent(uuid,isperm);
		Networking.send(NetworkCommand.GROUP_SET_PERM, uuid, (isperm ? 1 : 0) );
	}
	
	public static void set_children_color(long uuid, Color col)
	{
		no_notify_set_children_color(uuid,col);
		Networking.send(NetworkCommand.GROUP_CHILDREN_COLOR, uuid, col.getRed(), col.getGreen(), col.getBlue());
	}

	public static void rotate(long uuid, double theta) {
		no_notify_rotate(uuid, theta);
		Networking.send(NetworkCommand.GROUP_ROTATE, uuid, theta);
	}

	public static void scale(long uuid, double scaleX, double scaleY) {
		no_notify_scale(uuid, scaleX, scaleY);
		Networking.send(NetworkCommand.GROUP_SCALE, uuid, scaleX, scaleY);
	}
	
	public static void set_text(long uuid, String str) 
	{
		if(!exists(uuid)){return;}
		no_notify_set_text(uuid,str);
		Networking.send(NetworkCommand.GROUP_SET_TEXT, uuid, str);
	}
	
	public static void create_text_scrap(long uuid, long cuuid, String text, int x, int y)
	{
		no_notify_create_text_scrap(uuid, cuuid, text, x, y);
		CalicoPacket[] packets = CGroupController.groupdb.get(uuid).getUpdatePackets(false);
		for (int i = 0; i < packets.length; i++)
			Networking.send(packets[i]);
		//Networking.send(NetworkCommand.GROUP_CREATE_TEXT_GROUP, uuid, cuuid, text, x, y);
	}
	
	public static void no_notify_clear_child_strokes(long uuid)
	{
		if(!exists(uuid)){return;}
		
		groupdb.get(uuid).clearChildStrokes();
	}
	public static void no_notify_add_child_stroke(long uuid, long childuid)
	{
		if(!exists(uuid)){return;}
		
		groupdb.get(uuid).addChildStroke(childuid);
	}
	public static void no_notify_set_child_strokes(long uuid, long[] children)
	{
		if(!exists(uuid)){return;}
		
		
		groupdb.get(uuid).setChildStrokes(children);
		for(int i=0;i<children.length;i++)
		{
			if (CStrokeController.exists(children[i]))
			{
				if (CStrokeController.strokes.get(children[i]).getParentUUID() != uuid)
					CStrokeController.no_notify_set_parent(children[i], uuid);
			}
		}
	}
	
	
	public static void no_notify_set_child_groups(long uuid, long[] children)
	{
		if(!exists(uuid)){return;}
		
		groupdb.get(uuid).setChildGroups(children);
		
		groupdb.get(uuid).resetViewOrder();
//		for(int i=0;i<children.length;i++)
//		{
////			no_notify_add_child_group(uuid, children[i]);
//			if (groupdb.get(children[i]).getParentUUID() != uuid)
//				CGroupController.no_notify_set_parent(children[i], uuid);
//		}
	}
	public static void no_notify_clear_child_groups(long uuid)
	{
		if(!exists(uuid)){return;}
		
		groupdb.get(uuid).clearChildGroups();
	}
	public static void no_notify_add_child_group(long uuid, long childuid, int x, int y)
	{
		if(!exists(uuid)){return;}
		
		groupdb.get(uuid).addChildGroup(childuid, x, y);
	}
	public static void no_notify_delete_child_group(long uuid, long childuid)
	{
		if(!exists(uuid)){return;}
		
		long duuid = getDecoratorParent(uuid);
		
		groupdb.get(duuid).deleteChildGroup(childuid);
	}
	
	public static void no_notify_delete_child_stroke(long uuid, long childuid)
	{
		if(!exists(uuid)){return;}
		
		groupdb.get(uuid).deleteChildStroke(childuid);
	}
	public static void no_notify_delete_child_arrow(long uuid, long childuid)
	{
		if(!exists(uuid)){return;}
		
		groupdb.get(uuid).deleteChildArrow(childuid);
	}
	public static void no_notify_delete_child_connector(long uuid, long childuid)
	{
		if(!exists(uuid)){return;}
		
		groupdb.get(uuid).deleteChildConnector(childuid);
	}
	
	/**
	 * Returns the outermost decorator for the scrap uuid
	 * @param uuid
	 * @return
	 */
	public static long getDecoratorParent(long uuid)
	{
		if (!exists(uuid)) { return 0l; }
		
		if (CGroupController.exists(groupdb.get(uuid).getParentUUID())
			&& groupdb.get(groupdb.get(uuid).getParentUUID()) instanceof CGroupDecorator)
			return getDecoratorParent(groupdb.get(uuid).getParentUUID());
		else
			return uuid;
	}
	
	private static long getDecoratedGroup(long uuid)
	{
		if (!exists(uuid)) { return 0l; }
		
		if (groupdb.get(uuid) instanceof CGroupDecorator)
			return getDecoratedGroup(((CGroupDecorator)groupdb.get(uuid)).getDecoratedUUID());
		else
			return uuid;
	}
	
	
	public static void no_notify_clear_child_arrows(long uuid)
	{
		if(!exists(uuid)){return;}
		
		groupdb.get(uuid).clearChildArrows();
	}
	public static void no_notify_set_child_arrows(long uuid, long[] children)
	{
		if(!exists(uuid)){return;}
		
		no_notify_clear_child_arrows(uuid);
		for(int i=0;i<children.length;i++)
		{
			no_notify_add_child_arrow(uuid, children[i]);
		}
	}
	public static void no_notify_add_child_arrow(long uuid, long childuid)
	{
		if(!exists(uuid)){return;}
		
		groupdb.get(uuid).addChildArrow(childuid);
	}
	
	
	public static void verify_hash(long uuid, byte[] hash)
	{
		if(!exists(uuid)){return;}
		
		byte[] cur_hash = groupdb.get(uuid).getHashCode();
		
		if(Arrays.equals(hash, cur_hash))
		{
			logger.debug("GROUP "+uuid+" PASSED HASH CHECK");
		}
		else
		{
			logger.warn("GROUP "+uuid+" FAILED HASH CHECK");
		}
	}
	
	public static void show_group_piemenu(long uuid, Point point)
	{
		show_group_piemenu(uuid, point, PieMenuButton.SHOWON_SCRAP_MENU);
	}
	
	public static void show_group_piemenu(long uuid, Point point, int showfilter)
	{
		//Class<?> pieMenuClass = calico.components.piemenu.PieMenu.class;
		if (!exists(uuid))
			return;
		
		ObjectArrayList<Class<?>> pieMenuButtons = CGroupController.groupdb.get(uuid).getPieMenuButtons();
		
		
		int curPos = 0;
		int totalButtons = 0;
		int[] bitmasks = new int[pieMenuButtons.size()];
		
		
		
		if(pieMenuButtons.size()>0)
		{
			ArrayList<PieMenuButton> buttons = new ArrayList<PieMenuButton>();
			
			for(int i=0;i<pieMenuButtons.size();i++)
			{
				try
				{
//					if (pieMenuButtons.get(i).getName().compareTo("GroupRotateButton") == 0
//							&& CGroupController.groupdb.get(i).getText().length() > 0)
//						continue;
					bitmasks[i] = pieMenuButtons.get(i).getField("SHOWON").getInt(null);
					if( ( bitmasks[i] & showfilter) == showfilter)
					{
						if (pieMenuButtons.get(i).getName().compareTo("calico.components.piemenu.groups.GroupShrinkToContentsButton") == 0
								&& groupdb.get(uuid).getBoundsOfContents().isEmpty()
								
							|| pieMenuButtons.get(i).getName().compareTo("calico.components.piemenu.groups.ListCreateButton") == 0
								&& groupdb.get(uuid).getChildGroups().length == 0
							|| pieMenuButtons.get(i).getName().compareTo("calico.components.piemenu.groups.GroupTextButton") == 0
									&& groupdb.get(uuid).getText().length() > 0)
						{
							buttons.add(new PieMenuButton(new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB)));
							continue;
						}
						
						buttons.add((PieMenuButton) pieMenuButtons.get(i).getConstructor(long.class).newInstance(uuid));
//						buttons[curPos++] = (PieMenuButton) pieMenuButtons.get(i).getConstructor(long.class).newInstance(uuid);
					}
					else
						buttons.add(new PieMenuButton(new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB)));
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			
//			for(int i=0;i<pieMenuButtons.size();i++)
//			{
//				try
//				{
////					if (pieMenuButtons.get(i).getName().compareTo("calico.components.piemenu.groups.GroupRotateButton") == 0
////							&& CGroupController.groupdb.get(uuid).getText().length() > 0)
////						continue;
//					bitmasks[i] = pieMenuButtons.get(i).getField("SHOWON").getInt(null);
//					if( (bitmasks[i] & showfilter) == showfilter)
//					{
//						totalButtons++;
//						//buttons[curPos++] = (PieMenuButton) pieMenuButtons.get(i).getConstructor(long.class).newInstance(uuid);
//					}
//				}
//				catch (Exception e)
//				{
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//			PieMenuButton[] buttons = new PieMenuButton[totalButtons];
//			
//			for(int i=0;i<pieMenuButtons.size();i++)
//			{
//				try
//				{
////					if (pieMenuButtons.get(i).getName().compareTo("GroupRotateButton") == 0
////							&& CGroupController.groupdb.get(i).getText().length() > 0)
////						continue;
//					if( ( bitmasks[i] & showfilter) == showfilter)
//					{
//						buttons[curPos++] = (PieMenuButton) pieMenuButtons.get(i).getConstructor(long.class).newInstance(uuid);
//					}
//				}
//				catch (Exception e)
//				{
//					e.printStackTrace();
//				}
//			}

			PieMenu.displayPieMenuArray(point, buttons.toArray(new PieMenuButton[buttons.size()]));
			//BubbleMenu.displayBubbleMenu(point, CGroupController.groupdb.get(uuid).getBounds(),buttons.toArray(new PieMenuButton[buttons.size()]));
			CGroupController.groupdb.get(uuid).highlight_on();
			
		}
		
		
	}
	
	public static void show_group_bubblemenu(long uuid, boolean fade)
	{
		show_group_bubblemenu(uuid, PieMenuButton.SHOWON_SCRAP_MENU, fade);
	}
	
	public static void show_group_bubblemenu(long uuid)
	{
		show_group_bubblemenu(uuid, PieMenuButton.SHOWON_SCRAP_MENU, true);
	}
	
	public static void show_group_bubblemenu(long uuid, int showfilter, boolean fade)
	{
		//Class<?> pieMenuClass = calico.components.piemenu.PieMenu.class;
		if (!exists(uuid))
			return;

		ObjectArrayList<Class<?>> pieMenuButtons = CGroupController.groupdb.get(uuid).getBubbleMenuButtons();
		
		int curPos = 0;
		int totalButtons = 0;
		int[] bitmasks = new int[pieMenuButtons.size()];
		
		
		
		if(pieMenuButtons.size()>0)
		{
			ArrayList<PieMenuButton> buttons = new ArrayList<PieMenuButton>();
			
			for(int i=0;i<pieMenuButtons.size();i++)
			{
				try
				{
//					if (pieMenuButtons.get(i).getName().compareTo("GroupRotateButton") == 0
//							&& CGroupController.groupdb.get(i).getText().length() > 0)
//						continue;
					bitmasks[i] = pieMenuButtons.get(i).getField("SHOWON").getInt(null);
					if( ( bitmasks[i] & showfilter) == showfilter)
					{
						if (pieMenuButtons.get(i).getName().compareTo("calico.components.piemenu.groups.GroupShrinkToContentsButton") == 0
								&& groupdb.get(uuid).getBoundsOfContents().isEmpty()
								
							|| pieMenuButtons.get(i).getName().compareTo("calico.components.piemenu.groups.ListCreateButton") == 0
								&& groupdb.get(uuid).getChildGroups().length == 0
							|| pieMenuButtons.get(i).getName().compareTo("calico.components.piemenu.groups.GroupTextButton") == 0
								&& groupdb.get(uuid).getText().length() == 0
							|| pieMenuButtons.get(i).getName().compareTo("calico.components.piemenu.groups.GroupDropButton") == 0
								&& groupdb.get(uuid).getText().length() > 0) //12)
						{
							buttons.add(new PieMenuButton(new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB)));
							continue;
						}
						
						buttons.add((PieMenuButton) pieMenuButtons.get(i).getConstructor(long.class).newInstance(uuid));
//						buttons[curPos++] = (PieMenuButton) pieMenuButtons.get(i).getConstructor(long.class).newInstance(uuid);
					}
					else
						buttons.add(new PieMenuButton(new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB)));
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}

			BubbleMenu.displayBubbleMenu(uuid,fade,BubbleMenu.TYPE_GROUP,buttons.toArray(new PieMenuButton[buttons.size()]));
			
			
		}
		
		
	}
	
	public static boolean group_contains_stroke(final long containerUUID, final long checkUUID)
	{
		if (!CStrokeController.exists(checkUUID))
		{
//			logger.warn("CGroupController.group_contains_stroke: Stroke " + checkUUID + " doesn't exist!");
			return false;
		}
		else if (!exists(containerUUID))
		{
//			logger.warn("CGroupController.group_contains_stroke: Group " + containerUUID + " doesn't exist!");
			return false;
		}
		else if (CStrokeController.strokes.get(checkUUID).isTempInk())
		{
			return false;
		}
		else
			return groupdb.get(containerUUID).containsShape(CStrokeController.strokes.get(checkUUID).getPathReference());
			//return CStrokeController.strokes.get(checkUUID).isContainedInPath(CGroupController.groupdb.get(containerUUID).getPathReference());
	}
	
	public static boolean group_contains_group(final long containerUUID, final long checkUUID)
	{
		if(!groupdb.containsKey(containerUUID) || !groupdb.containsKey(checkUUID)){return false;}
		
		return CGroupController.groupdb.get(containerUUID).containsShape(groupdb.get(checkUUID).getPathReference());
//		return group_contains_path(containerUUID, CGroupController.groupdb.get(checkUUID).getPathReference() );
	}
	
	public static boolean group_contains_shape(final long containerUUID, Shape shape)
	{
		if(!groupdb.containsKey(containerUUID)){return false;}
		
		return CGroupController.groupdb.get(containerUUID).containsShape(shape);
//		return group_contains_path(containerUUID, CGroupController.groups.get(checkUUID).getPathReference() );
	}
	
	/**
	 * Check to see if the requested group contains the entirety of the polygon
	 * @param containerUUID
	 * @param polygon
	 * @return
	 */
//	public static boolean group_contains_path(final long containerUUID, GeneralPath path)
//	{
//		if(!groupdb.containsKey(containerUUID)){return false;}
//		
//		Polygon polygon = Geometry.getPolyFromPath(path.getPathIterator(null));
//		GeneralPath containerGroup = CGroupController.groupdb.get(containerUUID).getPathReference();
//		for(int i=0;i<polygon.npoints;i++)
//		{
//			if (!containerGroup.contains(new Point(polygon.xpoints[i], polygon.ypoints[i])))
//			{
//				return false;
//			}
//		}
//		return true;
//	}
	
	/**
	 * Check to see if the requested group contains the entirety of the polygon
	 * @param containerUUID
	 * @param polygon
	 * @return
	 */
//	public static boolean group_contains_polygon(final long containerUUID, Polygon polygon)
//	{
//		if(!groupdb.containsKey(containerUUID)){return false;}
//		
//		
//		Polygon containerGroupPoints = CGroupController.groupdb.get(containerUUID).getPolygon();
//		for(int i=0;i<polygon.npoints;i++)
//		{
//			if(!PolygonUtils.insidePoly(containerGroupPoints, new Point(polygon.xpoints[i], polygon.ypoints[i])))
//			{
//				return false;
//			}
//		}
//		return true;
//	}


	public static void makeRectangle(long guuid, int x, int y, int width, int height) {
		no_notify_make_rectangle(guuid, x, y, width, height);
		Networking.send(NetworkCommand.GROUP_MAKE_RECTANGLE, guuid, x, y, width, height);
	}
	
	public static void no_notify_make_rectangle(long guuid, int x, int y, int width, int height) {
		if (!exists(guuid))
			return;
		
		CGroup group = CGroupController.groupdb.get(guuid);
		if (!group.isPermanent())
			CGroupController.no_notify_set_permanent(guuid, true);
		
//		Rectangle rect = group.getBoundsOfContents();
		Rectangle rect = new Rectangle(x, y, width, height);
		
		group.setShapeToRoundedRectangle(rect);
		
		//group.repaint();
		CalicoDraw.repaint(group);

		if (BubbleMenu.isBubbleMenuActive() && BubbleMenu.activeUUID == guuid)
		{
			BubbleMenu.moveIconPositions(group.getBounds());
		}
	}
	
//	public static void shrink_to_contents(long guuid) {
//		no_notify_set_permanent(guuid, true);
//		no_notify_shrink_to_contents(guuid);
////		Networking.send(NetworkCommand.GROUP_SHRINK_TO_CONTENTS, guuid);
//	}
//	
//	public static void no_notify_shrink_to_contents(long guuid)
//	{
//		if (groupdb.containsKey(guuid))
//		{
//		CGroupController.no_notify_set_permanent(guuid, true);
//		CGroupController.groupdb.get(guuid).shrinkToContents();
//		CGroupController.groupdb.get(guuid).repaint();
//		}
//		else
//			logger.warn("Attempting to shrink to contents on non-existing scrap: " + guuid + " !");
//	}

	public static void shrinkToConvexHull(long guuid) {
		no_notify_shrink_to_convex_hull(guuid);
		//Networking.
		
	}
	
	public static void no_notify_shrink_to_convex_hull(long guuid)
	{
		CGroupController.groupdb.get(guuid).shrinkToConvexHull();
	}

	public static void create_image_group(long uuid, long cuid, long puid, String imgURL, int port, String localPath, int imgX, int imgY, int imgW, int imgH)
	{
		
		no_notify_create_image_group(uuid, cuid, puid, imgURL, port, localPath, imgX, imgY, imgW, imgH);
		
		Networking.send(CalicoPacket.getPacket(NetworkCommand.GROUP_IMAGE_LOAD, uuid, cuid, puid, imgURL, port, localPath, imgX, imgY, imgW, imgH, true, false, 0.0d, 1.0d, 1.0d));
	}

	public static void no_notify_create_image_group(long uuid, long cuid, long puid, String imgURL, int port, String localPath, int imgX, int imgY, int imgW, int imgH) {
		// TODO Auto-generated method stub
		//taken from start(...)
		
		if (groupdb.containsKey(uuid))
			no_notify_delete(uuid);
		
		groupdb.put(uuid, new CGroupImage(uuid, cuid, puid, imgURL, port, localPath, imgX, imgY, imgW, imgH));		
		CCanvasController.canvasdb.get(cuid).addChildGroup(uuid);		
		//CCanvasController.canvasdb.get(cuid).getLayer().addChild(groupdb.get(uuid));	
		CalicoDraw.addChildToNode(CCanvasController.canvasdb.get(cuid).getLayer(), groupdb.get(uuid));
		groupdb.get(uuid).drawPermTemp(true);
		CGroupController.no_notify_finish(uuid, false);
		
		//set to the same size as the screen
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		double scale = (dim.getHeight() - 20)/imgH;
//		groupdb.get(uuid).scale(scale, scale);
		
		//move to origin
//		this.moveTo(0, 0);
		
		
	}



	
	public static void no_notify_rotate(long uuid, double theta) {
		// If we don't know wtf this UUID is for, then just eject
		if(!exists(uuid))
		{
			logger.warn("ROTATE for non-existant group "+uuid);
			return;
		}
		
		groupdb.get(uuid).rotate(theta);
		
		if (BubbleMenu.isBubbleMenuActive() && BubbleMenu.activeUUID == uuid)
		{
			BubbleMenu.moveIconPositions(CGroupController.groupdb.get(uuid).getBounds());
		}
		
		informListenersOfMove(uuid);
	}

	public static void no_notify_scale(long uuid, double scaleX, double scaleY) 
	{
		// If we don't know wtf this UUID is for, then just eject
		if(!exists(uuid))
		{
			logger.warn("SCALE for non-existant group "+uuid);
			return;
		}
		
		groupdb.get(uuid).scale(scaleX, scaleY);
		
		informListenersOfMove(uuid);
		if (BubbleMenu.isBubbleMenuActive() && BubbleMenu.activeUUID == uuid)
		{
			BubbleMenu.moveIconPositions(CGroupController.groupdb.get(uuid).getBounds());
		}
	}
	
	public static void no_notify_create_text_scrap(long uuid, long cuuid, String text, int x, int y)
	{

		CGroupController.no_notify_start(uuid, cuuid, 0l, true);
		CGroupController.setCurrentUUID(uuid);
		CGroupController.no_notify_append(uuid, x, y);
		CGroupController.no_notify_set_text(uuid, text);
		CGroupController.no_notify_finish(uuid, false, false, true);
		//CGroupController.no_notify_set_permanent(uuid, true);
		Rectangle rect = groupdb.get(uuid).getBoundsOfContents();
		CGroupController.no_notify_make_rectangle(uuid, rect.x, rect.y, rect.width, rect.height);
		CGroupController.recheck_parent(uuid);
//		CGroupController.no_notify_shrink_to_contents(uuid);
	}
	
	public static long get_smallest_containing_group_for_point(long canvas_uuid, Point p)
	{
		long[] uuids = CCanvasController.canvasdb.get(canvas_uuid).getChildGroups();
		
		long group_uuid = 0L;
		double group_area = Double.MAX_VALUE;
		CGroup temp;
		
		if(uuids.length>0)
		{
			for(int i=0;i<uuids.length;i++)
			{
				temp = CGroupController.groupdb.get(uuids[i]);
				if (temp == null || temp.getPathReference() == null /*|| !CGroupController.groupdb.get(uuids[i]).isPermanent()*/)
					continue;
				if( (temp.getArea()< group_area) && temp.getPathReference().contains(p)
						&& (temp.getParentUUID() == 0l || !(CGroupController.groupdb.get(temp.getParentUUID()) instanceof CGroupDecorator)))
				{
					group_area = CGroupController.groupdb.get(uuids[i]).getArea();
					group_uuid = uuids[i];
				}
			}
		}
		return group_uuid;
	}
	
	public static boolean canParentChild(long potentialParent, long child, int x, int y)
	{
		if (!exists(potentialParent) 
				|| (!exists(child) && !CStrokeController.exists(child)))
			return false;
		
		return groupdb.get(potentialParent).canParentChild(child, x, y);
	}
	
	public static void no_notify_move_end(final long uuid, int x, int y)
	{
		if(!exists(uuid)){return;}
		
		groupdb.get(uuid).recheckParentAfterMove(x, y);
		
	}
	
	public static void move_end(long uuid, int x, int y) {		
		if(!exists(uuid)){return;}
		
		no_notify_move_end(uuid, x, y);
		CGroupController.groupdb.get(CGroupController.groupdb.get(uuid).getTopmostParent()).moveInFrontOf(null);
		//CalicoDraw.moveNodeInFrontOf(CGroupController.groupdb.get(CGroupController.groupdb.get(uuid).getTopmostParent()), null);
		Networking.send(CalicoPacket.getPacket(NetworkCommand.GROUP_MOVE_END, uuid, x, y));
	}


	public static void move_start(long guuid) {
		no_notify_move_start(guuid);
		Networking.send(CalicoPacket.getPacket(NetworkCommand.GROUP_MOVE_START, guuid));
	}
	
	public static void no_notify_move_start(long guuid) {
		no_notify_set_parent(guuid, 0);
		//CGroupController.groupdb.get(guuid).moveToFront();
		CalicoDraw.moveNodeToFront(CGroupController.groupdb.get(guuid));
		//CGroupController.groupdb.get(guuid).moveInFrontOf(null);
		CalicoDraw.moveNodeInFrontOf(CGroupController.groupdb.get(guuid), null);
		CalicoInputManager.group = guuid;
	}
	
	public static boolean group_is_ancestor_of(long ancestor, long group)
	{
		long uuid = group, parent;
		
		while (exists(uuid) && (parent = CGroupController.groupdb.get(uuid).getParentUUID()) != 0l)
		{
			 if (parent == ancestor)
				 return true;
			 uuid = parent;
		}
		
		return false;
	}
	
	/**
	 * 
	 * @param potentialParent
	 * @param child
	 * @return Returns true if potential parent can parent any ancestor of child.
	 */
	public static boolean group_can_parent_ancestor(long potentialParent, long child, int x, int y)
	{
		long uuid = child, parent = 0l;
		
		//get first ancestor
		if (CGroupController.exists(uuid))
			parent = groupdb.get(child).getParentUUID();
		else if (CStrokeController.exists(uuid))
			parent = CStrokeController.strokes.get(child).getParentUUID();
		
		//loop while we have a parent
		while (exists(parent))
		{
			//if we found an ancestor, return true
			if (groupdb.get(potentialParent).canParentChild(parent, x, y))
				return true;
			
			parent = CGroupController.groupdb.get(parent).getParentUUID();
		}
		
		//return false if no suitable ancestor is found
		return false;
	}
	
	public static void sendRandomTestPacket()
	{
		if (Calico.numUUIDs() < 1)
			return;
		
		Random r = new Random();
		
		int type = r.nextInt(100);
		
		long[] canvases = CCanvasController.getCanvasIDList();
		if (canvases.length < 10)
			return;
		long cuid = canvases[r.nextInt(canvases.length-1)];
		
		if (type < 10)
		{
//			PalettePlugin.sendRandomTestPacket();
		}
		else if (type < 60)
		{
			int x = r.nextInt(1500) + 100;
			int y = r.nextInt(1500) + 100;
			
			int width = r.nextInt(500) + 300;
			int height = r.nextInt(500) + 100;
			
			long uuid = Calico.uuid();
			
			
			no_notify_start(uuid, cuid, 0, true);
			no_notify_make_rectangle(uuid, x, y, width, height);
			finish(uuid, true);
		}
		else
		{
			long uuid = Calico.uuid();
			int x = r.nextInt(1500) + 100;
			int y = r.nextInt(1500) + 100;
			int x2 = r.nextInt(1500) + 100;
			int y2 = r.nextInt(1500) + 100;
			CStrokeController.no_notify_start(uuid, cuid, 0l, CalicoDataStore.PenColor, CalicoDataStore.PenThickness);
			CStrokeController.append(uuid, x, y);
			CStrokeController.append(uuid, x2, y2);
			CStrokeController.finish(uuid);
		}
		
	}
	
	public static void create_canvas_view_scrap(long uuid, long cuuid, long targetCanvas)
	{
		create_canvas_view_scrap(uuid, cuuid, targetCanvas, 200, 200, (int)CanvasViewScrap.getDefaultWidth(), (int)CanvasViewScrap.getDefaultHeight());
	}
	
	public static void create_canvas_view_scrap(long uuid, long cuuid, long targetCanvas, int x, int y, int width, int height)
	{
		no_notify_create_canvas_view_scrap(uuid, cuuid, targetCanvas, x, y, width, height);
		
		CalicoPacket[] packets = CGroupController.groupdb.get(uuid).getUpdatePackets(false);
		
		Networking.send(packets[0]);
	}
	
	public static void no_notify_create_canvas_view_scrap(long uuid, long cuuid, long targetCanvas, int x, int y, int width, int height)
	{	
		//initialize custom scrap
		CGroup group = new CanvasViewScrap(uuid, cuuid, targetCanvas);

		Rectangle bounds = new Rectangle(x, y, width, height);
		group.setShapeToRoundedRectangle(bounds, 0);

//		Polygon p = group.getRawPolygon();

		//create the scrap
		no_notify_create_custom_scrap_bootstrap(uuid, cuuid, group, "");

		CalicoDraw.repaint(group);
	}
	
	public static void no_notify_load_canvasview_scrap(long uuid, long cuid,
			long puid, boolean isperm, int[] xArr, int[] yArr,
			boolean captureChildren, double rotation, double scaleX,
			double scaleY, String text, long targetCanvas) {
		
		CGroup group = new CanvasViewScrap(uuid, cuid, targetCanvas);
		no_notify_start(uuid, cuid, puid, isperm, group);
		no_notify_append(uuid, xArr, yArr);
		groupdb.get(uuid).primative_rotate(rotation);
		groupdb.get(uuid).primative_scale(scaleX, scaleY);
		groupdb.get(uuid).setText(text);
		
		no_notify_finish(uuid, captureChildren, false, false);
	}

	/*************************************************
	 * UTILITY METHODS
	 *************************************************/		
	public static void no_notify_create_custom_scrap_bootstrap(long uuid, long cuuid, CGroup group, String optText){
		no_notify_start(uuid, cuuid, 0l, true, group);
		CGroupController.setCurrentUUID(uuid);
//		create_custom_shape(uuid, p);
		//Set the optional text to identify the scrap
		CGroupController.no_notify_set_text(uuid, optText);
		CGroupController.no_notify_finish(uuid, false, false, true);
		CGroupController.no_notify_set_permanent(uuid, true);
		CGroupController.recheck_parent(uuid);
	}	

	//Starts the creation of any of the activity diagram scrap
	public static void no_notify_start(long uuid, long cuid, long puid, boolean isperm, CGroup customScrap)
	{
		if (!CCanvasController.exists(cuid))
			return;
		if(CGroupController.exists(uuid))
		{
			CGroupController.logger.debug("Need to delete group "+uuid);
			//CCanvasController.canvasdb.get(cuid).getLayer().removeChild(groupdb.get(uuid));
			CalicoDraw.removeChildFromNode(CCanvasController.canvasdb.get(cuid).getLayer(), CGroupController.groupdb.get(uuid));
			//CCanvasController.canvasdb.get(cuid).getCamera().repaint();
		}
		customScrap.setPermanent(isperm);

		// Add to the GroupDB
		try {
			CGroupController.groupdb.put(uuid, customScrap);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		CCanvasController.canvasdb.get(cuid).addChildGroup(uuid);
		//CCanvasController.canvasdb.get(cuid).getLayer().addChild(groupdb.get(uuid));
		CalicoDraw.addChildToNode(CCanvasController.canvasdb.get(cuid).getLayer(), CGroupController.groupdb.get(uuid));
		CGroupController.groupdb.get(uuid).drawPermTemp(true);
		//CCanvasController.canvasdb.get(cuid).repaint();
	}	

	//Add the points defined in p to the scrap with id uuid
	public static void create_custom_shape(long uuid, Polygon p){
		for (int i = 0; i < p.npoints; i++)
		{
			CGroupController.no_notify_append(uuid, p.xpoints[i], p.ypoints[i]);
			CGroupController.no_notify_append(uuid, p.xpoints[i], p.ypoints[i]);
			CGroupController.no_notify_append(uuid, p.xpoints[i], p.ypoints[i]);
		}
	}



	
}
