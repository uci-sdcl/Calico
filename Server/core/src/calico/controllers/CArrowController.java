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
import calico.uuid.*;

import java.awt.*;
import java.util.*;

import it.unimi.dsi.fastutil.longs.*;

public class CArrowController
{
	public static Long2ReferenceArrayMap<CArrow> arrows = new Long2ReferenceArrayMap<CArrow>();
	
	
	public static void setup()
	{
	
	}
	
	public static boolean exists(long uuid)
	{
		return arrows.containsKey(uuid);
	}
	
	public static void no_notify_start(long uuid, long cuid, int type, Color color, AnchorPoint pointa, AnchorPoint pointb)
	{
		CCanvasController.set_arrow_canvas(uuid, cuid);
		
		if (exists(uuid))
			no_notify_delete(uuid);
		// create the object
		arrows.put(uuid, new CArrow(uuid, cuid, type, color, pointa, pointb));

		// Add to the canvas
		CCanvasController.no_notify_add_child_arrow(cuid, uuid);
		
		if(pointa.getType()==CArrow.TYPE_GROUP)
		{
			CGroupController.no_notify_add_child_arrow(pointa.getUUID(), uuid);
		}
		
		if(pointb.getType()==CArrow.TYPE_GROUP)
		{
			CGroupController.no_notify_add_child_arrow(pointb.getUUID(), uuid);
		}
		
	}

	
	public static void no_notify_delete(long uuid)
	{
		if(!exists(uuid)){return;}
		
		// Clear the anchor A
		if(arrows.get(uuid).getAnchorA().getType()==CArrow.TYPE_GROUP)
		{
			CGroupController.no_notify_remove_child_arrow(arrows.get(uuid).getAnchorA().getUUID(), uuid);
		}
		
		// Clear anchor B
		if(arrows.get(uuid).getAnchorB().getType()==CArrow.TYPE_GROUP)
		{
			CGroupController.no_notify_remove_child_arrow(arrows.get(uuid).getAnchorB().getUUID(), uuid);
		}
		
		// remove from the canvas
		CCanvasController.no_notify_remove_child_arrow( CCanvasController.get_arrow_canvas(uuid), uuid);
		
		// Call the arrow delete, 
		arrows.get(uuid).delete();
		
		
		// remove from the DB
		arrows.remove( uuid );
	}


	public static void no_notify_move_group(long uuid, long groupuuid, int x, int y)
	{
		if(!exists(uuid)){return;}
		
		arrows.get(uuid).moveGroup(groupuuid, x, y);
	}

	
	public static void delete(long uuid)
	{
		if(!exists(uuid)){return;}
		
		no_notify_delete(uuid);
		
		// Notify
		ClientManager.send(CalicoPacket.getPacket( NetworkCommand.ARROW_DELETE, uuid ));
	}
	
	
	// THIS IS DIFFERENT FROM SET_PARENT ON GROUP/STROKES - It only changes the parent of the anchor node IF THE ANCHOR IS SET TO THE CANVAS
	public static void no_notify_parented_to_group(long uuid, long parent_uuid)
	{
		if(!exists(uuid)){return;}
		
		// TODO: Finish
	}
	
	public static void reload(final long uuid)
	{
		if(!exists(uuid)){return;}
		
		ClientManager.send(arrows.get(uuid).getUpdatePackets());
	}
	
	public static void no_notify_move_group_anchor(long uuid, long guuid, int x, int y)
	{
		arrows.get(uuid).moveGroup(guuid, x, y);
	}

	public static void recalculate_parent(long uuid) {
		CArrowController.arrows.get(uuid).calculateParent();
		reload(uuid);
	}
	
	public static int get_signature(long l) {
		if (!exists(l))
			return 0;
		
		return arrows.get(l).get_signature();
	}	
	
}
