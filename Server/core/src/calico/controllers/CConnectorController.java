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

import java.awt.Color;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import it.unimi.dsi.fastutil.longs.Long2ReferenceAVLTreeMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import org.apache.log4j.Logger;

import calico.clients.ClientManager;
import calico.components.CConnector;
import calico.components.CStroke;
import calico.networking.netstuff.NetworkCommand;

public class CConnectorController {
	/**
	 * Logger
	 */
	private static Logger logger = Logger.getLogger(CConnectorController.class.getName());
	
	/**
	 * This is the database of all the BGElements
	 */
	public static Long2ReferenceAVLTreeMap<CConnector> connectors = new Long2ReferenceAVLTreeMap<CConnector>();
	
	public static boolean exists(long uuid)
	{
		return connectors.containsKey(uuid);
	}
	
	private static long getConnectorCanvasUUID(long uuid)
	{
		return connectors.get(uuid).getCanvasUUID();
	}
	
	public static void create(long uuid, long cuid, Color color, float thickness, Point head, Point tail, double[] orthogonalDistance, 
			double[] travelDistance, long anchorHead, long anchorTail)
	{
		no_notify_create(uuid, cuid, color, thickness, head, tail, orthogonalDistance, travelDistance, anchorHead, anchorTail);
		
		ClientManager.send(connectors.get(uuid).getUpdatePackets()[0]);
	}
	
	/**
	 * This will create a Connector, but not notify anybody
	 * @param uuid
	 * @param cuid
	 * @param puid
	 * @param color
	 */
	public static void no_notify_create(long uuid, long cuid, Color color, float thickness, Point head, Point tail, double[] orthogonalDistance, 
			double[] travelDistance, long anchorHead, long anchorTail)
	{
		// does the element already exist?
		if(exists(uuid))
		{
			no_notify_delete(uuid);
		}
		// add to the DB
		connectors.put(uuid, new CConnector(uuid, cuid, color, thickness, head, tail, orthogonalDistance, travelDistance, anchorHead, anchorTail));
		
		// Add to the canvas
		CCanvasController.no_notify_add_child_connector(cuid, uuid);
		
		// We need to notify the groups 
		CGroupController.no_notify_add_child_connector(connectors.get(uuid).getAnchorUUID(CConnector.TYPE_HEAD), uuid);
		CGroupController.no_notify_add_child_connector(connectors.get(uuid).getAnchorUUID(CConnector.TYPE_TAIL), uuid);	
	}
	
	public static void no_notify_delete(long uuid)
	{
		if (!exists(uuid))
			return;
			
		connectors.get(uuid).delete();
		connectors.remove(uuid);
	}
	
	public static void no_notify_linearize(long uuid)
	{
		if (!exists(uuid))
			return;
		
		connectors.get(uuid).linearize();
	}
	
	public static void no_notify_move_group_anchor_start(long uuid, int type)
	{
		CConnector tempConnector = CConnectorController.connectors.get(uuid);
		tempConnector.savePosition(type);
		if (tempConnector.getAnchorUUID(CConnector.TYPE_HEAD) != tempConnector.getAnchorUUID(CConnector.TYPE_TAIL))
		{
			CGroupController.no_notify_remove_child_connector(tempConnector.getAnchorUUID(type), uuid);
		}
	}
	
	public static void move_group_anchor(long uuid, int type, int x, int y)
	{
		if (!exists(uuid))
			return;
		
		no_notify_move_group_anchor(uuid, type, x, y);
		
		//SEND PACKET HERE
	}
	
	public static void no_notify_move_group_anchor(long uuid, int type, int x, int y)
	{
		if (!exists(uuid))
			return;
		
		connectors.get(uuid).moveAnchor(type, x, y);
	}
	
	public static void no_notify_move_group_anchor(long uuid, long guuid, int x, int y)
	{
		if (!exists(uuid))
			return;
		
		connectors.get(uuid).moveAnchor(guuid, x, y);
	}
	
	public static void no_notify_move_group_anchor_end(long uuid, int type)
	{
		CConnector tempConnector = CConnectorController.connectors.get(uuid);
		
		Point p;
		if (type == CConnector.TYPE_HEAD)
			p = tempConnector.getHead();
		else if (type == CConnector.TYPE_TAIL)
			p = tempConnector.getTail();
		else return;
		
		long guuid = CGroupController.get_smallest_containing_group_for_point(tempConnector.getCanvasUUID(), p);
		if (guuid == 0l)
		{
			tempConnector.loadPosition(type);
		}
		else
		{
			tempConnector.setAnchorUUID(guuid, type);
			CGroupController.no_notify_add_child_connector(guuid, uuid);
		}
	}
	
	
	public static void delete(long uuid)
	{
		no_notify_delete(uuid);
		
		//Networking.send(NetworkCommand.CONNECTOR_DELETE, uuid);
		
	}
	
	public static int get_signature(long l) {
		if (!exists(l))
			return 0;
		
		return connectors.get(l).get_signature();
	}
	
	public static String get_signature_debug_output(long uuid)
	{
		if (!exists(uuid))
			return "";
		
		return connectors.get(uuid).get_signature_debug_output();
	}	

}
