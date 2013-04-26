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
import calico.networking.*;
import calico.networking.netstuff.*;
import calico.utils.CalicoUtils;
import calico.utils.Geometry;
import calico.uuid.*;

import java.awt.*;
import java.util.*;

import org.apache.log4j.Logger;

import it.unimi.dsi.fastutil.longs.*;

/**
 * Controller class for all Strokes
 * @author mdempsey
 *
 */
public class CStrokeController
{
	public static Long2ReferenceAVLTreeMap<CStroke> strokes = new Long2ReferenceAVLTreeMap<CStroke>();
	private static Logger logger = Logger.getLogger(CStrokeController.class.getName());
	
	public static boolean exists(long uuid)
	{
		return strokes.containsKey(uuid);
	}
	
	
	public static void setup()
	{
	
	}
	
	public static long makeScrap(long suuid, long new_guuid)
	{
		long ret = no_notify_makeScrap(suuid, new_guuid);
		ClientManager.send(CalicoPacket.getPacket(NetworkCommand.STROKE_MAKE_SCRAP, suuid, new_guuid));
		
		return ret;
	}
	
	public static long makeShrunkScrap(long suuid, long new_guuid)
	{
		long ret = no_notify_makeShrunkScrap(suuid, new_guuid);
		ClientManager.send(CalicoPacket.getPacket(NetworkCommand.STROKE_MAKE_SHRUNK_SCRAP, suuid, new_guuid));
		
		return ret;
	}
	
	public static void deleteArea(long suuid, long temp_guuid)
	{
		no_notify_deleteArea(suuid, temp_guuid);
		ClientManager.send(CalicoPacket.getPacket(NetworkCommand.STROKE_DELETE_AREA, suuid, temp_guuid));
	}
	
	public static long no_notify_makeScrap(long suuid, long new_guuid)
	{
		if (!CStrokeController.exists(suuid)) {return 0L;}
		
		CStroke stroke = CStrokeController.strokes.get(suuid);
		
		long parent = CGroupController.get_smallest_containing_group_for_point(stroke.getCanvasUUID(), 
				Geometry.getMidPoint(stroke.getPolygon()));
		CGroupController.no_notify_start(new_guuid, stroke.getCanvasUUID(), parent, false);
		
		Polygon strokePoly = stroke.getPolygon();
		for (int i=0;i<strokePoly.npoints;i++)
		{
			CGroupController.no_notify_append(new_guuid, strokePoly.xpoints[i], strokePoly.ypoints[i]);
		}
		CGroupController.no_notify_finish(new_guuid, true);
//		CGroupController.no_notify_set_permanent(new_guuid, true);
		CStrokeController.no_notify_delete(suuid);
		return new_guuid;
	}

	public static long no_notify_makeShrunkScrap(long suuid, long new_guuid)
	{
		long scrapUUID = no_notify_makeScrap(suuid, new_guuid);
		CGroup scrap = CGroupController.groups.get(scrapUUID);
		scrap.shrinkToContents();
		return scrapUUID;
//		long[] children = scrap.getPossibleChildren();
//		if (children.length > 0)
//		{
//			Rectangle bounds = scrap.getBoundsOfObjects(children);
//			scrap.shrinkToContents(bounds);
//			return scrapUUID;
//		}
//		else
//		{
//			CGroupController.delete(scrapUUID);
//			return 0L;
//		}
	}
	
	public static void no_notify_deleteArea(long uuid, long temp_guuid)
	{
		long scrapToDelete = no_notify_makeScrap(uuid, temp_guuid);
		CGroupController.no_notify_delete(scrapToDelete);
	}
	
	public static void no_notify_start(long uuid, long cuid, long puid, Color color, float thickness)
	{
		CCanvasController.set_stroke_canvas(uuid, cuid);
		
		strokes.put(uuid, new CStroke(uuid, cuid, puid, color, thickness));
		
		CCanvasController.no_notify_add_child_stroke(cuid, uuid);
		
		// Add to the group
		if(puid!=0L)
		{
			CGroupController.no_notify_add_child_stroke(puid, uuid);
		}
	}
	
	// Aliases
	public static void no_notify_start(long uuid, long cuid, long puid){no_notify_start(uuid,cuid,puid,COptions.stroke.default_color, COptions.stroke.default_thickness);}
	public static void no_notify_start(long uuid, long cuid){no_notify_start(uuid,cuid,0L);}

	
	public static void no_notify_set_parent(long uuid, long puid)
	{
		if (!strokes.containsKey(uuid))
			return;
		
		long curpuid = strokes.get(uuid).getParentUUID();
		
		
		if(curpuid!=0L)
		{
			// We have a parent already, so we must notify them that we are leaving
			CGroupController.no_notify_remove_child_stroke(puid, uuid);
		}
		
		strokes.get(uuid).setParentUUID(puid);
		
		if(puid!=0L)
		{
			CGroupController.no_notify_add_child_stroke(puid, uuid);
		}	
	}
	public static void set_parent(long uuid, long puid)
	{
		if(!exists(uuid)){return;}
		
		no_notify_set_parent(uuid, puid);
		parent_update(uuid);
	}
	
	public static void delete(final long uuid)
	{
		if(!exists(uuid)){return;}
		
		no_notify_delete(uuid);
		ClientManager.send(CalicoPacket.getPacket(NetworkCommand.STROKE_DELETE, uuid));
	}
	
	public static void no_notify_set_color(long uuid, Color color)
	{
		if(!exists(uuid)){return;}
		
		strokes.get(uuid).setColor(color);
	}
	
	
	public static void no_notify_append(long uuid, int x, int y)
	{
		if(!exists(uuid)){return;}
		
		strokes.get(uuid).append(x, y);
	}
	
	public static void no_notify_batch_append(long uuid, int[] x, int[] y)
	{
		if(!exists(uuid))
		{
			logger.warn("Stroke "+uuid+" does not exist");
			return;
		}
		
		strokes.get(uuid).batch_append(x, y);
	}
	
	
	public static void no_notify_delete(long uuid)
	{
		if(!exists(uuid)){return;}
		
		long cuid = CCanvasController.get_stroke_canvas(uuid);
		
		// Get the canvas, and remove it
		CCanvasController.no_notify_remove_child_stroke(cuid, uuid);
		
		// get any groups, and remove it
		long puid = strokes.get(uuid).getParentUUID();
		if(puid!=0L)
		{
			CGroupController.no_notify_remove_child_stroke(puid, uuid);
		}
		
		strokes.get(uuid).delete();
		
		// delete the object
		strokes.remove(uuid);
	}
	
	
	public static void no_notify_finish(long uuid)
	{
		if(!exists(uuid)){return;}
		
		strokes.get(uuid).finish();
		//always send out parenting packets when a new parent is calculated.
		//Wayne: NOPE
		//ClientManager.send(strokes.get(uuid).calculateParent());
//		strokes.get(uuid).calculateParent();
	}

	
	public static void no_notify_recalculate_parent(long uuid)
	{
		if(!exists(uuid)){return;}
		
		strokes.get(uuid).calculateParent();
	}
	public static void recalculate_parent(long uuid)
	{
		if(!exists(uuid)){return;}
		
		ClientManager.send(strokes.get(uuid).calculateParent());
	}
	
	public static void parent_update(long uuid)
	{
		if(!exists(uuid)){return;}
		// Resend to all
		ClientManager.send(CalicoPacket.getPacket(NetworkCommand.STROKE_SET_PARENT, uuid, strokes.get(uuid).getParentUUID()	));
	}
	
	public static void copy(long uuid, long new_uuid, long new_puuid, long new_canvasuuid, int shift_x, int shift_y)
	{
		copy(uuid, new_uuid, new_puuid, new_canvasuuid, shift_x, shift_y, true);
	}
	public static void copy(long uuid, long new_uuid, long new_puuid, long new_canvasuuid, int shift_x, int shift_y, boolean notify)
	{
		if(!exists(uuid) || exists(new_uuid)){return;}

		CalicoPacket[] packets = strokes.get(uuid).getUpdatePackets(new_uuid, new_canvasuuid, new_puuid, shift_x, shift_y);
		batchReceive(packets);
		
		if(notify)
		{
			ClientManager.send(packets);
		}
		
	}
	public static void no_notify_copy(long uuid, long new_uuid, long new_puuid, long new_canvasuuid, int shift_x, int shift_y)
	{
		copy(uuid, new_uuid, new_puuid, new_canvasuuid, shift_x, shift_y, false);
	}
	
	private static void batchReceive(CalicoPacket[] packets)
	{
		for (int i = 0; i < packets.length; i++)
		{
			CalicoPacket p = new CalicoPacket(packets[i].getBuffer());
			ProcessQueue.receive(p.getInt(), null, p);
		}
	}
	
	/**
	 * This will notify everyone on the server about the stroke
	 * @param uuid
	 */
	public static void reload(long uuid)
	{
		if(!exists(uuid)){return;}
		
		CalicoPacket[] packets = strokes.get(uuid).getUpdatePackets();
		
		for(int i=0;i<packets.length;i++)
		{
			ClientManager.send(packets[i]);
		}
	}
	
	
	/**
	 * This will reload a stroke for the client
	 * @param uuid
	 * @param client
	 */
	public static void reload(long uuid, Client client)
	{
		if(!exists(uuid)){return;}
		
		CalicoPacket[] packets = strokes.get(uuid).getUpdatePackets();
		
		for(int i=0;i<packets.length;i++)
		{
			ClientManager.send(client, packets[i]);
		}
	}
	
	
	
	public static void no_notify_move(long uuid, int x, int y)
	{
		if(!exists(uuid)){return;}
		
		strokes.get(uuid).move(x, y);
	}
	
	public static void move(long uuid, int x, int y)
	{
		if(!exists(uuid)){return;}
		
		no_notify_move(uuid, x, y);
	}
	
	public static boolean is_parented_to(long uuid, long puid)
	{
		return (strokes.get(uuid).getParentUUID()==puid);
	}
	
	public static int get_signature(long uuid)
	{
		if (!exists(uuid))
			return 0;
		
		return strokes.get(uuid).get_signature();
	}
	
	public static String get_signature_debug_output(long uuid)
	{
		if (!exists(uuid))
			return "";
		
		return strokes.get(uuid).get_signature_debug_output();
	}
	
}
