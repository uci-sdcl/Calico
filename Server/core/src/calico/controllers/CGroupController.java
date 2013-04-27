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

import calico.*;
import calico.clients.*;
import calico.components.*;
import calico.components.composable.ComposableElement;
import calico.components.composable.ComposableElementController;
import calico.components.decorators.CGroupDecorator;
import calico.components.decorators.CListDecorator;
import calico.networking.*;
import calico.networking.netstuff.*;
import calico.utils.CalicoUtils;
import calico.utils.Geometry;
import calico.uuid.*;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.*;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.log4j.*;

import it.unimi.dsi.fastutil.longs.*;

public class CGroupController
{
	public static Long2ReferenceArrayMap<CGroup> groups = new Long2ReferenceArrayMap<CGroup>();

	static Logger logger = Logger.getLogger(CGroupController.class.getName());

	
	public static void setup()
	{
	
	}
	
	public static boolean exists(long uuid)
	{
		return groups.containsKey(uuid);
	}
	
	
	public static void no_notify_start(final long uuid, final long cuid, final long puid, final boolean isPerm)
	{
		// Set the canvas for this group (this is just for lookup, nothing else)
		CCanvasController.set_group_canvas(uuid, cuid);
		
		if(exists(uuid))
		{
			no_notify_drop(uuid);
		}
		
		groups.put(uuid, new CGroup(uuid, cuid, puid, isPerm));
		
		// Add to the canvas
		CCanvasController.no_notify_add_child_group(cuid, uuid);
		
		if(puid!=0L)
		{
			CGroupController.no_notify_add_child_group(puid, uuid, 0, 0);
		}
	}
	
	// Alias
	public static void no_notify_start(final long uuid, final long cuid, final long puid){ no_notify_start(uuid, cuid, puid, false); }
	public static void no_notify_start(final long uuid, final long cuid){ no_notify_start(uuid, cuid, 0L, false); }
	
	
	public static void no_notify_append(final long uuid, final int x, final int y)
	{
		if(!exists(uuid)){return;}
		
		groups.get(uuid).addPoint(x, y);
	}
	
	public static void no_notify_append(long uuid, int[] x, int[] y)
	{
		// If we don't know wtf this UUID is for, then just eject
		if(!exists(uuid))
		{
			logger.warn("APPEND for non-existant group "+uuid);
			return;
		}
		
		groups.get(uuid).append(x, y);
	}
	
	
	public static void no_notify_delete(final long uuid)
	{
		if(!exists(uuid)){return;}
		
		
//		groups.get(uuid).delete();
		
//		CGroup group = groups.get(uuid);
		
		// what is it's current parent?
//		long curpuid = group.getParentUUID();
//		
//		long[] child_strokes = group.getChildStrokes();
//		long[] child_groups = group.getChildGroups();
//		long[] child_arrows = group.getChildArrows();
//		
//		
//		// Reparent any strokes
//		if(child_strokes.length>0)
//		{
//			for(int i=0;i<child_strokes.length;i++)
//			{
////				CStrokeController.delete(child_strokes[i]);
//				CStrokeController.no_notify_delete(child_strokes[i]);
//			}
//		}
//		
//		// Reparent any groups
//		if(child_groups.length>0)
//		{
//			for(int i=0;i<child_groups.length;i++)
//			{
////				CGroupController.delete(child_groups[i]);
//				CGroupController.no_notify_delete(child_groups[i]);
//			}
//		}
//		
//		// Reparent any arrows
//		// TODO: FINISH
//		// FOR NOW: WE just delete the arrows :(
//		if(child_arrows.length>0)
//		{
//			for(int i=0;i<child_arrows.length;i++)
//			{
////				CArrowController.delete(child_arrows[i]);
//				CArrowController.no_notify_delete(child_arrows[i]);
//			}
//		}
//		
//		if (exists(curpuid))
//		{
//			groups.get(curpuid).deleteChildGroup(uuid);
//		}
		
		
		// Remove from the canvas
		CCanvasController.no_notify_remove_child_group(groups.get(uuid).getCanvasUUID(), uuid);
		groups.get(uuid).delete();
		
		groups.remove(uuid);
	}
	
	public static void no_notify_finish(long uuid, boolean captureChildren)
	{
		boolean checkParenting = true;
		no_notify_finish(uuid, captureChildren, checkParenting);
	}
	
	/**
	 * This finishes a Group element
	 * @param uuid
	 */
	public static void no_notify_finish(final long uuid, boolean captureChildren, boolean checkParenting)
	{
		if(!exists(uuid)){return;}
		
		groups.get(uuid).finish();
		if (checkParenting)
			recheck_parent(uuid);
		if (captureChildren)
			no_notify_calculate_parenting(uuid, true);
		
	}
	
	public static void no_notify_calculate_parenting(final long uuid, final boolean includeStrokes)
	{
		Point2D mid = groups.get(uuid).getMidPoint();
		no_notify_calculate_parenting(uuid, (int)mid.getX(), (int)mid.getY(), includeStrokes);
	}
	
	public static void no_notify_calculate_parenting(final long uuid, int x, int y, final boolean includeStrokes)
	{
		if(!exists(uuid)){return;}
		
		groups.get(uuid).calculateParenting(includeStrokes, x, y);
	}
	
	public static void finish(final long uuid, boolean captureChildren)
	{
		if(!exists(uuid)){return;}
		
//		recheck_parent(uuid);
		no_notify_finish(uuid, captureChildren);
//		ClientManager.send( groups.get(uuid).getParentingUpdatePackets() );
		
		
		// Send the finished packet
		ClientManager.send( CalicoPacket.getPacket(NetworkCommand.GROUP_FINISH, uuid, captureChildren));
		if (captureChildren)
			ClientManager.send( groups.get(uuid).getParentingUpdatePackets() );
		
	}
	
	public static void calculateParenting(final long uuid)
	{
		if(!exists(uuid)){return;}
		
		no_notify_calculate_parenting(uuid, true);
		ClientManager.send( groups.get(uuid).getParentingUpdatePackets() );
	}
	
	public static void delete(final long uuid)
	{
		if(!exists(uuid)){return;}
		
		no_notify_delete(uuid);
		ClientManager.send(CalicoPacket.getPacket(NetworkCommand.GROUP_DELETE, uuid));
	}
	
	public static void append(long uuid, int x, int y)
	{
		no_notify_append(uuid, x, y);
		
		CalicoPacket p = new CalicoPacket( ByteUtils.SIZE_OF_INT + ByteUtils.SIZE_OF_LONG + ByteUtils.SIZE_OF_SHORT + ByteUtils.SIZE_OF_SHORT );
		p.putInt(NetworkCommand.GROUP_APPEND);
		p.putLong(uuid);
		p.putInt(x);
		p.putInt(y);
		
		ClientManager.send( p );
	}
	
	public static void copy(final long uuid, final long new_uuid, final long new_canvasuuid, final int shift_x, final int shift_y)
	{
		copy(uuid, new_uuid, 0l, new_canvasuuid, shift_x, shift_y, true);
	}
	public static void copy(final long uuid, final long new_uuid, final long new_puuid, final long new_canvasuuid, final int shift_x, final int shift_y)
	{
		copy(uuid, new_uuid, new_puuid, new_canvasuuid, shift_x, shift_y, true);
	}
	public static void no_nofity_copy(final long uuid, final long new_uuid, final long new_puuid, final long new_canvasuuid, final int shift_x, final int shift_y)
	{
		copy(uuid, new_uuid, new_puuid, new_canvasuuid, shift_x, shift_y, false);
	}
	
	public static void copy_to_canvas(final long uuid, final long new_uuid, final long new_canvasuuid, final int shift_x, final int shift_y, final int final_x, final int final_y, boolean notify)
	{
		long cuidFrom = groups.get(uuid).getCanvasUUID();
		long[] arrows  = CCanvasController.canvases.get(cuidFrom).getChildArrows();
		Long2ReferenceArrayMap<Long> groupMappings = new Long2ReferenceArrayMap<Long>();
		
		groupMappings.put(uuid, new Long(new_uuid));
		groupMappings.putAll(copy(uuid, new_uuid, new_canvasuuid, shift_x, shift_y, true));

		if(arrows.length>0)
		{			
			for(int i=0;i<arrows.length;i++)
			{	
				CArrow temp = CArrowController.arrows.get(arrows[i]);
				if(groupMappings.containsKey(temp.getAnchorA().getUUID()) != groupMappings.containsKey(temp.getAnchorB().getUUID())
					&& (groupMappings.containsKey(temp.getAnchorA().getUUID()) || groupMappings.containsKey(temp.getAnchorB().getUUID())))
				{				
					long new_auuid = UUIDAllocator.getUUID();
					AnchorPoint anchorA = temp.getAnchorA().clone();
					AnchorPoint anchorB = temp.getAnchorB().clone();
					anchorA.translate(shift_x, shift_y);
					anchorB.translate(shift_x, shift_y);
					
					if(groupMappings.containsKey(temp.getAnchorA().getUUID())){				
						anchorA.setUUID(groupMappings.get(temp.getAnchorA().getUUID()).longValue());
					}else{
						anchorA.setUUID(new_canvasuuid);
					}
					if(groupMappings.containsKey(temp.getAnchorB().getUUID())){				
						anchorB.setUUID(groupMappings.get(temp.getAnchorB().getUUID()).longValue());
					}else{
						anchorB.setUUID(new_canvasuuid);
					}
					CArrowController.no_notify_start(new_auuid, new_canvasuuid, temp.getArrowType(), temp.getArrowColor(),anchorA, anchorB);
					CArrowController.reload(new_auuid);
				}				
			}
		}
		
		move_end(new_uuid, final_x, final_y);
	}
	
	public static Long2ReferenceArrayMap<Long> copy(final long uuid, final long new_uuid, final long new_canvasuuid, final int shift_x, final int shift_y, boolean notify)
	{
		return copy(uuid, new_uuid, 0l, new_canvasuuid, shift_x, shift_y, notify);
	}
	
	private static void batchReceive(CalicoPacket[] packets)
	{
		for (int i = 0; i < packets.length; i++)
		{
			if (packets[i] == null)
			{
				logger.warn("WARNING!!! BatchReceive received a null packet, something likely went wrong!");
				System.out.println("WARNING!!! BatchReceive received a null packet, something likely went wrong!");
				continue;
			}
			
			CalicoPacket p = new CalicoPacket(packets[i].getBuffer());
			ProcessQueue.receive(p.getInt(), null, p);
		}
	}
	
	public static Long2ReferenceArrayMap<Long> copy(final long uuid, final long new_uuid, final long new_puuid, final long new_canvasuuid, final int shift_x, final int shift_y, boolean notify)
	{
		if(!exists(uuid)){return null;}// old one doesnt exist
		if(exists(new_uuid)){return null;}// new one already exists
		
		Long2ReferenceArrayMap<Long> groupMappings = new Long2ReferenceArrayMap<Long>();
		
		CGroup temp = groups.get(uuid);
		CalicoPacket[] packets;
		
		if (temp instanceof CGroupDecorator)
		{
			long new_decoratorChildUUID = UUIDAllocator.getUUID();
			long old_decoratorChildUUID = ((CGroupDecorator)temp).getDecoratedUUID();
			Long2ReferenceArrayMap<Long> subGroupMappings = copy(old_decoratorChildUUID, new_decoratorChildUUID, new_uuid, new_canvasuuid, shift_x, shift_y, notify);
			if (subGroupMappings != null)
				groupMappings.putAll(subGroupMappings);
			
			packets = ((CGroupDecorator)temp).getDecoratorUpdatePackets(new_uuid, new_canvasuuid, new_puuid, new_decoratorChildUUID, subGroupMappings);
			batchReceive(packets);
			System.out.println();
			CGroupController.groups.get(new_uuid).setChildGroups(new long[] { new_decoratorChildUUID } );
			
			if (notify)
			{
				ClientManager.send(packets);
			}
		}
		else
		{
			packets = groups.get(uuid).getUpdatePackets(new_uuid, new_canvasuuid, new_puuid, shift_x, shift_y, false);
		
			batchReceive(packets);
			
			if(notify)
			{
				ClientManager.send(packets);
			}
			
			CGroup tempNew = groups.get(new_uuid);
			
			// DEAL WITH THE CHILDREN
			
			// Child stroke elements
			long[] bge_uuids = temp.getChildStrokes();
			long[] new_bge_uuids = new long[bge_uuids.length];
			
			if(bge_uuids.length>0)
			{
				for(int i=0;i<bge_uuids.length;i++)
				{
					new_bge_uuids[i] = UUIDAllocator.getUUID();
					CStrokeController.copy(bge_uuids[i], new_bge_uuids[i], new_uuid, new_canvasuuid, shift_x, shift_y, notify);
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
					new_grp_uuids[i] = UUIDAllocator.getUUID();
					groupMappings.put(grp_uuids[i], new Long(new_grp_uuids[i]));
					Long2ReferenceArrayMap<Long> subGroupMappings = copy(grp_uuids[i], new_grp_uuids[i], new_uuid, new_canvasuuid, shift_x, shift_y, notify);
	//				CGroupController.groups.get(new_uuid).addChildGroup(new_grp_uuids[i]);
					//				recheck_parent(new_grp_uuids[i]);
					if (subGroupMappings != null)
						groupMappings.putAll(subGroupMappings);
				}
				tempNew.setChildGroups(new_grp_uuids);
	//			tempNew.clearChildGroups();
	//			for(int i = 0; i < new_grp_uuids.length; i++)
	//			{
	//				tempNew.addChildGroup(new_grp_uuids[i]);
	//			}
			}
			
			//Child arrow elements
			long[] arrow_uuids = temp.getChildArrows();
			long[] new_arw_uuids = new long[arrow_uuids.length];
			
			if(arrow_uuids.length>0)
			{
				for(int i=0;i<arrow_uuids.length;i++)
				{				
					CArrow tempA = CArrowController.arrows.get(arrow_uuids[i]);
					if(tempA.getAnchorA().getUUID()==uuid&&tempA.getAnchorB().getUUID()==uuid){				
						new_arw_uuids[i] = UUIDAllocator.getUUID();
										
						AnchorPoint anchorA = tempA.getAnchorA().clone();
						AnchorPoint anchorB = tempA.getAnchorB().clone();				
						anchorA.translate(shift_x, shift_y);
						anchorB.translate(shift_x, shift_y);
						anchorA.setUUID(new_uuid);
						anchorB.setUUID(new_uuid);
						CArrowController.no_notify_start(new_arw_uuids[i], new_canvasuuid, tempA.getArrowType(), tempA.getArrowColor(),anchorA, anchorB);
						CArrowController.reload(new_arw_uuids[i]);
					}
				}
			}
		}
		if (notify)
			ClientManager.send(CGroupController.groups.get(new_uuid).getParentingUpdatePackets());
		//if (new_puuid == 0l)
			//recheck_parent(new_uuid);
		ClientManager.send(CalicoPacket.getPacket(NetworkCommand.CANVAS_SC_FINISH, new_canvasuuid));
		return groupMappings;
		
		
	}//no_notify_copy
	
	public static void no_notify_copy(final long uuid, final long new_puuid, Long2ReferenceArrayMap<Long> UUIDMappings, boolean isRoot)
	{
		if(!exists(uuid)){return;}// old one doesnt exist
		
		CGroup temp = groups.get(uuid);
		long new_uuid = UUIDMappings.get(uuid).longValue();
		long canvasuuid = temp.getCanvasUUID();
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
				
				CGroupController.groups.get(new_uuid).setChildGroups(new long[] { new_decoratorChildUUID } );

			}
		}
		else
		{
			packets = groups.get(uuid).getUpdatePackets(new_uuid, canvasuuid, new_puuid, 0, 0, false);
		
			batchReceive(packets);
			
			CGroup tempNew = groups.get(new_uuid);
			
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
					if(tempA.getAnchorA().getUUID()==uuid||tempA.getAnchorB().getUUID()==uuid){	
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
							CArrowController.no_notify_start(new_arw_uuids[i], canvasuuid, tempA.getArrowType(), tempA.getArrowColor(), anchorA, anchorB);
						}
					}
				}
			}*/
		}		
		
		if (isRoot)
		{
			long[] arrow_uuids  = CCanvasController.canvases.get(canvasuuid).getChildArrows();
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
						CArrowController.no_notify_start(new_arrow_uuid, canvasuuid, tempArrow.getArrowType(), tempArrow.getArrowColor(), anchorA, anchorB);
					}
				}
			}
		}
		
		//Connectors
		if (isRoot)
		{
			long[] connector_uuids  = CCanvasController.canvases.get(canvasuuid).getChildConnectors();
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
	}//no_notify_copy
	
	private static ArrayList<Long> getSubGroups(long uuid)
	{
		if(!exists(uuid)){return null;}// doesn't exist
		
		ArrayList<Long> childGroups = new ArrayList<Long>();
		
		CGroup temp = groups.get(uuid);
		
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
	
	
	public static void recheck_parent(final long uuid)
	{
		if(!exists(uuid)){return;}// old one doesnt exist
		
		Point2D mid = groups.get(uuid).getMidPoint();
		groups.get(uuid).recheckParentAfterMove((int)mid.getX(), (int)mid.getY(), true);
	}
	
	public static void recheck_parent(final long uuid, int x, int y)
	{
		if(!exists(uuid)){return;}// old one doesnt exist
		
		groups.get(uuid).recheckParentAfterMove(x, y, true);
	}
	
	
	
	public static void no_notify_move(final long uuid, final int x, final int y)
	{
		if(!exists(uuid)){return;}
		
		groups.get(uuid).move(x, y);
	}
	public static void no_notify_drop(final long uuid){no_notify_drop(uuid, false);}
	public static void no_notify_drop(final long uuid, boolean notifyClients)
	{
		if(!exists(uuid)){return;}

		CGroup group = groups.get(uuid);
		long[] child_strokes = group.getChildStrokes();
		long[] child_groups = group.getChildGroups();
		long[] child_arrows = group.getChildArrows();
		
		group.unparentAllChildren();
		no_notify_delete(uuid);
		// what is it's current parent?

		// Remove from the canvas
//		CCanvasController.no_notify_remove_child_group(group.getCanvasUUID(), uuid);
//		groups.remove(uuid);
		
		// Reparent any strokes
		if(child_strokes.length>0)
		{
			for(int i=0;i<child_strokes.length;i++)
			{
				if(notifyClients)
				{
					CStrokeController.recalculate_parent(child_strokes[i]);
//					CStrokeController.set_parent(child_strokes[i], curpuid);
				}
				else
				{
					CStrokeController.strokes.get(child_strokes[i]).calculateParent();
//					CStrokeController.no_notify_set_parent(child_strokes[i], curpuid);
				}
			}
		}
		
		// Reparent any groups
		if(child_groups.length>0)
		{
			for(int i=0;i<child_groups.length;i++)
			{
				if (groups.containsKey(child_groups[i]))
					groups.get(child_groups[i]).recheckParentAfterMove(notifyClients);
				else
					System.err.println("Invalid key found for child group while parenting! Key: " + child_groups[i]);
			}
		}
		
		// Reparent any arrows
		// TODO: FINISH
		// FOR NOW: WE just delete the arrows :(
		if(child_arrows.length>0)
		{
			for(int i=0;i<child_arrows.length;i++)
			{
				if(notifyClients)
				{
					CArrowController.recalculate_parent(child_arrows[i]);
//					CStrokeController.set_parent(child_strokes[i], curpuid);
				}
				else
				{
					CArrowController.arrows.get(child_arrows[i]).calculateParent();
//					CStrokeController.no_notify_set_parent(child_strokes[i], curpuid);
				}
				
			}
		}
		
		// Connectors: The client will turn the connector into a stroke and notify the server. Therefore
		// we don't need to reparent the connector here.
		
	}
	
	public static void drop(final long uuid)
	{
		if(!exists(uuid)){return;}
		
		no_notify_drop(uuid, true);
		ClientManager.send(CalicoPacket.getPacket( NetworkCommand.GROUP_DROP, uuid ) );
	}
	
	
	public static void no_notify_move_end(final long uuid, final int x, final int y)
	{
		if(!exists(uuid)){return;}
		
		groups.get(uuid).recheckParentAfterMove(x, y);
	}
	
	public static void move_end(final long uuid, final int x, final int y)
	{
		if(!exists(uuid)){return;}
		
		no_notify_move_end(uuid, x, y);
		ClientManager.send(CalicoPacket.getPacket(NetworkCommand.GROUP_MOVE_END, uuid, x, y));
		//ClientManager.send(groups.get(uuid).getParentingUpdatePackets());
	}
	


	public static void reload(final long uuid)
	{
		if(!exists(uuid)){return;}
		
		ClientManager.send(groups.get(uuid).getUpdatePackets(false));
	}
	public static void reload(final long uuid, final Client client)
	{
		if(!exists(uuid)){return;}
		
		ClientManager.send(client, groups.get(uuid).getUpdatePackets(false));
	}
	
	
	public static void reload_parenting(final long uuid)
	{
		if(!exists(uuid)){return;}
		
		ClientManager.send(groups.get(uuid).getParentingUpdatePackets());
	}
	public static void reload_parenting(final long uuid, final Client client)
	{
		if(!exists(uuid)){return;}
		
		ClientManager.send(client, groups.get(uuid).getParentingUpdatePackets());
	}
	
	
	
	
	public static void no_notify_set_permanent(final long uuid, final boolean isPerm)
	{
		if(!exists(uuid)){return;}
		
		groups.get(uuid).setPermanent(isPerm);
		recheck_parent(uuid);
	}
	public static void set_permanent(final long uuid, final boolean isPerm)
	{
		if(!exists(uuid)){return;}
		
		no_notify_set_permanent(uuid, isPerm);
		
		ClientManager.send( CalicoPacket.getPacket( NetworkCommand.GROUP_SET_PERM, uuid, (isPerm ? 1 : 0) ));
	}
	
	public static void start(long uuid, long cuid, long puid, boolean isperm)
	{
		no_notify_start(uuid, cuid, puid, isperm);
		ClientManager.send( CalicoPacket.getPacket( NetworkCommand.GROUP_START, uuid, cuid, puid, (isperm ? 1 : 0)) );
	}
	public static void move(long uuid, int x, int y)
	{
		no_notify_move(uuid, x, y);
		ClientManager.send(CalicoPacket.getPacket(NetworkCommand.GROUP_MOVE, uuid, x, y));
	}

	public static void set_parent(final long uuid, final long puid)
	{
		no_notify_set_parent(uuid, puid);
		ClientManager.send( CalicoPacket.getPacket(NetworkCommand.GROUP_SET_PARENT, uuid, puid ));
	}
	
	public static void rotate(long uuid, double theta) {
		no_notify_rotate(uuid, theta);
		ClientManager.send( CalicoPacket.getPacket(NetworkCommand.GROUP_ROTATE, uuid, theta ));
	}

	public static void scale(long uuid, double scaleX, double scaleY) {
		no_notify_scale(uuid, scaleX, scaleY);
		ClientManager.send( CalicoPacket.getPacket(NetworkCommand.GROUP_SCALE, uuid, scaleX, scaleY ));
	}
	
	public static void no_notify_set_parent(long uuid, long puid)
	{
		if(!exists(uuid)){return;}
		
		long curpuid = groups.get(uuid).getParentUUID();
		long undecoratedParent = getDecoratedGroup(puid); 
		
		if (curpuid == undecoratedParent)
			return;
		
		if (curpuid != 0L && groups.get(curpuid) instanceof CGroupDecorator)
			return;
		
		if(curpuid!=0L && exists(curpuid))
		{
			// We should update the current parent
			no_notify_remove_child_group(curpuid, uuid);
		}
		

		groups.get(uuid).setParentUUID(undecoratedParent);
		
		if(puid!=0L && exists(undecoratedParent))
		{
			long decoratedParent = getDecoratorParent(puid);
			Point2D midPoint = CGroupController.groups.get(uuid).getMidPoint();
			no_notify_add_child_group(decoratedParent, uuid, (int)midPoint.getX(), (int)midPoint.getY());
		}
	}
	
	public static boolean hasChildGroup(long uuid, long cuuid)
	{
		return groups.get(uuid).hasChildGroup(cuuid);
	}
	
	public static void no_notify_set_text(long uuid, String str) 
	{
		// TODO Auto-generated method stub
		if (!exists(uuid)){return;}
		
		groups.get(uuid).setText(str);
		
	}
	
	public static void set_text(long uuid, String str) 
	{
		if(!exists(uuid)){return;}
		no_notify_set_text(uuid,str);
		ClientManager.send( CalicoPacket.getPacket(NetworkCommand.GROUP_SET_TEXT, uuid, str));
	}
	
	public static void no_notify_remove_child_stroke(final long uuid, final long csuuid)
	{
		if(!exists(uuid)){return;}
		
		groups.get(uuid).deleteChildStroke(csuuid);
	}
	public static void no_notify_add_child_stroke(final long uuid, final long csuuid)
	{
		if(!exists(uuid)){return;}
		
		groups.get(uuid).addChildStroke(csuuid);
	}
	
	
	public static void no_notify_remove_child_group(final long uuid, final long cguuid)
	{
		if(!exists(uuid)){return;}
		
		long duuid = getDecoratorParent(uuid);
		
		groups.get(duuid).deleteChildGroup(cguuid);
	}
	public static void no_notify_add_child_group(final long uuid, final long cguuid, int x, int y)
	{
		if(!exists(uuid)){return;}
		
		groups.get(uuid).addChildGroup(cguuid, x, y);
	}
	
	public static long getDecoratorParent(long uuid)
	{
		if (!exists(uuid)) { return 0l; }
		
		if (CGroupController.exists(groups.get(uuid).getParentUUID())
			&& groups.get(groups.get(uuid).getParentUUID()) instanceof CGroupDecorator)
			return getDecoratorParent(groups.get(uuid).getParentUUID());
		else
			return uuid;
	}
	
	private static long getDecoratedGroup(long uuid)
	{
		if (!exists(uuid)) { return 0l; }
		
		if (groups.get(uuid) instanceof CGroupDecorator)
			return getDecoratedGroup(((CGroupDecorator)groups.get(uuid)).getDecoratedUUID());
		else
			return uuid;
	}
	
	
	public static void no_notify_remove_child_arrow(final long uuid, final long cguuid)
	{
		if(!exists(uuid)){return;}
		
		groups.get(uuid).deleteChildArrow(cguuid);
	}
	public static void no_notify_add_child_arrow(final long uuid, final long cguuid)
	{
		if(!exists(uuid)){return;}
		
		groups.get(uuid).addChildArrow(cguuid);
	}
	
	public static void no_notify_add_child_connector(long uuid, long cguuid)
	{
		if(!exists(uuid)){return;}
		
		groups.get(uuid).addChildConnector(cguuid);
	}
	
	public static void no_notify_remove_child_connector(long uuid, long cguuid)
	{
		if(!exists(uuid)){return;}
		
		groups.get(uuid).deleteChildConnector(cguuid);
	}
	
	public static boolean group_contains_group(final long containerUUID, final long checkUUID)
	{
		if(!groups.containsKey(containerUUID) || !groups.containsKey(checkUUID)){return false;}
		
		return CGroupController.groups.get(containerUUID).containsShape(groups.get(checkUUID).getPathReference());
//		return group_contains_path(containerUUID, CGroupController.groups.get(checkUUID).getPathReference() );
	}
	
	public static boolean group_contains_shape(final long containerUUID, Shape shape)
	{
		if(!groups.containsKey(containerUUID)){return false;}
		
		return CGroupController.groups.get(containerUUID).containsShape(shape);
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
//		if(!groups.containsKey(containerUUID)){return false;}
//		
//		Polygon polygon = Geometry.getPolyFromPath(path.getPathIterator(null));
//		GeneralPath containerGroup = CGroupController.groups.get(containerUUID).getPathReference();
////		Polygon containerGroup = CGroup.getPolyFromPath(CGroupController.groups.get(containerUUID).getPathReference().getPathIterator(null));
////		GeneralPath containerGroup = CGroupController.groups.get(containerUUID).getPathReference();
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
//		if(!groups.containsKey(containerUUID)){return false;}
//		
//		
//		Polygon containerGroupPoints = CGroupController.groups.get(containerUUID).getPolygon();
//		for(int i=0;i<polygon.npoints;i++)
//		{
//			if(!PolygonUtils.insidePoly(containerGroupPoints, new Point(polygon.xpoints[i], polygon.ypoints[i])))
//			{
//				return false;
//			}
//		}
//		return true;
//	}
	
	public static boolean group_contains_stroke(final long containerUUID, final long checkUUID)
	{
		if (!CStrokeController.exists(checkUUID))
		{
			logger.warn("CGroupController.group_contains_stroke: Stroke " + checkUUID + " doesn't exist!");
			return false;
		}
		else if (!exists(containerUUID))
		{
			logger.warn("CGroupController.group_contains_stroke: Group " + containerUUID + " doesn't exist!");
			return false;
		}
		else
			return groups.get(containerUUID).containsShape(CStrokeController.strokes.get(checkUUID).getPathReference());
			//return CStrokeController.strokes.get(checkUUID).isContainedInPath(CGroupController.groupdb.get(containerUUID).getPathReference());
	}
	
	
	public static CalicoPacket get_hash_check_packet(long uuid)
	{
		CalicoPacket packet = new CalicoPacket();
		packet.putInt(NetworkCommand.GROUP_HASH_CHECK);
		packet.putLong(uuid);
		byte[] temp = groups.get(uuid).getHashCode();
		packet.putCharInt(temp.length);
		packet.putBytes(temp);
		return packet;
	}
	
	public static void send_hash_checks()
	{
		// Loop thru, and send the hashchecks out to everyone
		long[] groupids = groups.keySet().toLongArray();
		
		for(int i=0;i<groupids.length;i++)
		{
			ClientManager.send(get_hash_check_packet(groupids[i]));
		}
	}
	
	public static void send_hash_checks(Client client)
	{
		// Loop thru, and send the hashchecks out to everyone
		long[] groupids = groups.keySet().toLongArray();
		
		for(int i=0;i<groupids.length;i++)
		{
			ClientManager.send(client, get_hash_check_packet(groupids[i]));
		}
	}
	
	public static void makeRectangle(long guuid, int x, int y, int width, int height) {
		no_notify_make_rectangle(guuid, x, y, width, height);
		ClientManager.send( CalicoPacket.getPacket( NetworkCommand.GROUP_MAKE_RECTANGLE, guuid, x, y, width, height ));
	}
	
	public static void no_notify_make_rectangle(long guuid, int x, int y, int width, int height) {
		if (!exists(guuid))
			return;
		
		CGroup group = CGroupController.groups.get(guuid);
		if (!group.isPermanent())
			CGroupController.no_notify_set_permanent(guuid, true);
		
//		Rectangle rect = group.getBoundsOfContents();
		Rectangle rect = new Rectangle(x, y, width, height);
		
		group.setShapeToRoundedRectangle(rect);

	}

//	public static void shrink_to_contents(long uuid) {
//		CGroupController.no_notify_set_permanent(uuid, true);
//		CGroupController.no_notify_shrink_to_contents(uuid);
//		
//	}
//
//	private static void no_notify_shrink_to_contents(long uuid) {
//		CGroupController.groups.get(uuid).shrinkToContents();
//	}
//	
	public static boolean createImageGroup(long uuid, long cuuid, String imageURL, int x, int y)
	{
		try{
			
			CImageController.download_image(uuid, imageURL);
//			imageURL = CImageController.getImageURL(uuid);
			//URL url= new URL(imageURL);
			//Image image = null;
			/*try
			{
				image = ImageIO.read(new File(CImageController.getImagePath(uuid)));
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}*/
			
			Dimension imageSize = CImageController.getImageSize(CImageController.getImagePath(uuid));
			
			if (imageSize.width > 1920 || imageSize.height > 1080)
			{
				return false;
			}
			else
			{
	//			Image image = Toolkit.getDefaultToolkit().createImage(url);
	//			Image image = Toolkit.getDefaultToolkit().createImage(imageURL);
				//this will run once we have the image ready
	//			image.getWidth(CImageController.getImageInitializer(uuid, cuuid, CImageController.getImageURL(uuid), x, y));
				CGroupController.createImageGroup(uuid, cuuid, 0L, imageURL, x, y, imageSize.width, imageSize.height);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			logger.error(e.getMessage());
		}
		return true;
	}

	public static void createImageGroup(long uuid, long cuid,
			long puid, String imgURL, int imgX, int imgY, int imageWidth, int imageHeight) {

		no_notify_create_image_group(uuid, cuid, puid, imgURL, imgX, imgY, imageWidth, imageHeight);
		ClientManager.send( CGroupController.groups.get(uuid).getUpdatePackets(false));
//		ClientManager.send( CalicoPacket.getPacket( NetworkCommand.GROUP_ADD_IMAGE, uuid, cuid, puid, imgURL, imageWidth, imageHeight) );
		
	}

	public static void no_notify_create_image_group(long uuid, long cuid,
			long puid, String imgURL, int imgX, int imgY, int imageWidth, int imageHeight) {
		// TODO Auto-generated method stub
		// Set the canvas for this group (this is just for lookup, nothing else)
		CCanvasController.set_group_canvas(uuid, cuid);
		
		if(exists(uuid))
		{
			no_notify_drop(uuid);
		}
		
		groups.put(uuid, new CGroupImage(uuid, cuid, puid, imgURL, imgX, imgY, imageWidth, imageHeight));
		
		// Add to the canvas
		CCanvasController.no_notify_add_child_group(cuid, uuid);
		
		if(puid!=0L)
		{
			Point2D midPoint = CGroupController.groups.get(uuid).getMidPoint();
			CGroupController.no_notify_add_child_group(puid, uuid, (int)midPoint.getX(), (int)midPoint.getY());
		}
		
	}
	
	public static void no_notify_rotate(long uuid, double theta) {
		// If we don't know wtf this UUID is for, then just eject
		if(!exists(uuid))
		{
			logger.warn("ROTATE for non-existant group "+uuid);
			return;
		}
		
		groups.get(uuid).rotate(theta);
	}
	


	public static void no_notify_scale(long uuid, double scaleX, double scaleY) 
	{
		// If we don't know wtf this UUID is for, then just eject
		if(!exists(uuid))
		{
			logger.warn("SCALE for non-existant group "+uuid);
			return;
		}
		
		groups.get(uuid).scale(scaleX, scaleY);
	}

	public static long get_smallest_containing_group_for_point(long canvas_uuid, Point p)
	{
		long[] uuids = CCanvasController.canvases.get(canvas_uuid).getChildGroups();
		
		long group_uuid = 0L;
		double group_area = Double.MAX_VALUE;
		
		if(uuids.length>0)
		{
			for(int i=0;i<uuids.length;i++)
			{
				if( (CGroupController.groups.get(uuids[i]).getArea()< group_area) && CGroupController.groups.get(uuids[i]).getPathReference().contains(p)
						&& CGroupController.groups.get(uuids[i]).isPermanent())
				{
					group_area = CGroupController.groups.get(uuids[i]).getArea();
					group_uuid = uuids[i];
				}
			}
		}
		return group_uuid;
	}
	
	public static void no_notify_create_text_scrap(long uuid, long cuuid, String text, int x, int y)
	{
//		long parent = get_smallest_containing_group_for_point(cuuid, new Point(x,y));
		CGroupController.no_notify_start(uuid, cuuid, 0l, true);
		CGroupController.no_notify_append(uuid, x, y);
		CGroupController.no_notify_set_text(uuid, text);
		CGroupController.no_notify_finish(uuid, false, false);
		//CGroupController.no_notify_set_permanent(uuid, true);
		Rectangle rect = groups.get(uuid).getBoundsOfContents();
		CGroupController.no_notify_make_rectangle(uuid, rect.x, rect.y, rect.width, rect.height);
		CGroupController.recheck_parent(uuid);
	}
	

	public static boolean canParentChild(long potentialParent, long child, int x, int y)
	{
		if (!exists(potentialParent) 
				|| (!exists(child) && !CStrokeController.exists(child)))
			return false;
		
		return groups.get(potentialParent).canParentChild(child, x, y);
	}
	
	public static boolean is_parented_to(long uuid, long puid)
	{
		if (groups.get(uuid) == null)
			return false;
		return (groups.get(uuid).getParentUUID()==puid);
	}
	

	
	public static void move_start(long guuid) {
		no_notify_move_start(guuid);
		ClientManager.send(CalicoPacket.getPacket(NetworkCommand.GROUP_MOVE_START, guuid));
	}
	
	public static void no_notify_move_start(long guuid) {
		no_notify_set_parent(guuid, 0);
		//set_parent(guuid, 0);
	}
	
	public static boolean group_is_ancestor_of(long ancestor, long group)
	{
		if (!exists(ancestor) || !exists(group))
			return false;
		
		long uuid = group, parent;
		
		while (exists(uuid) && (parent = CGroupController.groups.get(uuid).getParentUUID()) != 0l)
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
			parent = groups.get(child).getParentUUID();
		else if (CStrokeController.exists(uuid))
			parent = CStrokeController.strokes.get(child).getParentUUID();
		
		//loop while we have a parent
		while (exists(parent))
		{
			//if we found an ancestor, return true
			if (groups.get(potentialParent).canParentChild(parent, x, y))
				return true;
			
			parent = CGroupController.groups.get(parent).getParentUUID();
		}
		
		//return false if no suitable ancestor is found
		return false;
	}
	
	public static int get_signature(long uuid)
	{
		if(!exists(uuid)){return 0;}
		
		return groups.get(uuid).get_signature();
	}
	
	public static String get_signature_debug_output(long uuid)
	{
		if(!exists(uuid)){return "";}
		
		return groups.get(uuid).get_signature_debug_output();
	}
	
	public static boolean isPermanent(long guuid)
	{
		if (!exists(guuid))
			return false;
		
		return groups.get(guuid).isPermanent();
	}
	
	public static void no_notify_set_child_groups(long uuid, long[] children)
	{
		if(!exists(uuid)){return;}
		
		groups.get(uuid).setChildGroups(children);

	}
	
	public static void no_notify_set_child_strokes(long uuid, long[] children)
	{
		if(!exists(uuid)){return;}
		
		
		groups.get(uuid).setChildStrokes(children);
		for(int i=0;i<children.length;i++)
		{
			if (CStrokeController.exists(children[i]))
			{
				if (CStrokeController.strokes.get(children[i]).getParentUUID() != uuid)
					CStrokeController.no_notify_set_parent(children[i], uuid);
			}
		}
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
	
	public static void no_notify_clear_child_arrows(long uuid)
	{
		if(!exists(uuid)){return;}
		
		groups.get(uuid).clearChildArrows();
	}
	
	public static void no_notify_load_canvasview_scrap(long uuid, long cuid,
			long puid, boolean isperm, int[] xArr, int[] yArr,
			boolean captureChildren, double rotation, double scaleX,
			double scaleY, String text, long targetCanvas) {
		
		CGroup group = new CanvasViewScrap(uuid, cuid, targetCanvas);
		no_notify_start(uuid, cuid, puid, isperm, group);
		no_notify_append(uuid, xArr, yArr);
		groups.get(uuid).primative_rotate(rotation);
		groups.get(uuid).primative_scale(scaleX, scaleY);
		groups.get(uuid).setText(text);
		
		no_notify_finish(uuid, captureChildren, false);
	}
	
	/*************************************************
	 * UTILITY METHODS
	 *************************************************/		
	public static void no_notify_create_custom_scrap_bootstrap(long uuid, long cuuid, CGroup group, String optText){
		no_notify_start(uuid, cuuid, 0l, true, group);
//		CGroupController.setCurrentUUID(uuid);
//		create_custom_shape(uuid, p);
		//Set the optional text to identify the scrap
		CGroupController.no_notify_set_text(uuid, optText);
		CGroupController.no_notify_finish(uuid, false, false);
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
//			CalicoDraw.removeChildFromNode(CCanvasController.canvasdb.get(cuid).getLayer(), CGroupController.groupdb.get(uuid));
			//CCanvasController.canvasdb.get(cuid).getCamera().repaint();
		}

		// Add to the GroupDB
		try {
			CGroupController.groups.put(uuid, customScrap);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		CCanvasController.canvases.get(cuid).addChildGroup(uuid);
		//CCanvasController.canvasdb.get(cuid).getLayer().addChild(groupdb.get(uuid));
//		CalicoDraw.addChildToNode(CCanvasController.canvases.get(cuid).getLayer(), CGroupController.groupdb.get(uuid));
		CGroupController.groups.get(uuid).setPermanent(isperm);
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
