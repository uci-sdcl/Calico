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
package calico;

import calico.networking.*;
import calico.networking.netstuff.*;
import calico.plugins.CalicoPluginManager;
import calico.plugins.events.CalicoEvent;
import calico.plugins.events.clients.ClientConnect;
import calico.components.*;
import calico.components.composable.ComposableElement;
import calico.components.composable.ComposableElementController;
import calico.controllers.*;
import calico.admin.*;
import calico.clients.*;
import calico.events.CalicoEventHandler;
import calico.uuid.*;
import calico.sessions.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.sql.*;

import java.awt.*;

import it.unimi.dsi.fastutil.longs.*;

import org.apache.log4j.*;

public class ProcessQueue
{

	public static Logger logger = Logger.getLogger(ProcessQueue.class.getName());
	
	public static void setup()//ProcessQueue()
	{
	
	}

	public static void receive(int command,Client client, CalicoPacket pdata)
	{
		try
		{
			// If the client is null, then we are on the server
			
			/*
			if(command!=NetworkCommand.JOIN && command!=NetworkCommand.SESSION_LIST)
			{
				throw new ClientNotAuthedException(client);
			}*/
			//System.out.println("Packet: "+CLogger.packet2string(pdata));
			
			if(logger.isDebugEnabled() && client!=null && command != NetworkCommand.HEARTBEAT)
			{
				logger.debug("RX \""+ClientManager.client2string(client)+"\" "+pdata.toString());
			}
			
			switch(command)
			{
				case NetworkCommand.JOIN:JOIN(pdata,client);break;
				case NetworkCommand.HEARTBEAT:HEARTBEAT(pdata,client);break;
				//case NetworkCommand.SESSION_LIST:SESSION_LIST(pdata,client);break;

				case NetworkCommand.UUID_GET_BLOCK:UUID_GET_BLOCK(pdata,client);break;
				
				case NetworkCommand.STROKE_START:STROKE_START(pdata,client);break;
				case NetworkCommand.STROKE_APPEND:STROKE_APPEND(pdata,client);break;
				case NetworkCommand.STROKE_DELETE:STROKE_DELETE(pdata,client);break;
				case NetworkCommand.STROKE_FINISH:STROKE_FINISH(pdata,client);break;
				case NetworkCommand.STROKE_MOVE:STROKE_MOVE(pdata,client);break;
				case NetworkCommand.STROKE_SET_COLOR:STROKE_SET_COLOR(pdata,client);break;
				case NetworkCommand.STROKE_SET_PARENT:STROKE_SET_PARENT(pdata,client);break;
				case NetworkCommand.STROKE_LOAD:STROKE_LOAD(pdata,client);break;
				case NetworkCommand.STROKE_REQUEST_HASH_CHECK:STROKE_REQUEST_HASH_CHECK(pdata,client);break;
				case NetworkCommand.STROKE_MAKE_SCRAP:STROKE_MAKE_SCRAP(pdata,client);break;
				case NetworkCommand.STROKE_MAKE_SHRUNK_SCRAP:STROKE_MAKE_SHRUNK_SCRAP(pdata,client);break;
				case NetworkCommand.STROKE_DELETE_AREA:STROKE_DELETE_AREA(pdata,client);break;
				case NetworkCommand.STROKE_ROTATE:STROKE_ROTATE(pdata, client);break;
				case NetworkCommand.STROKE_SCALE:STROKE_SCALE(pdata, client);break;
				case NetworkCommand.STROKE_SET_AS_POINTER:STROKE_SET_AS_POINTER(pdata, client);break;
				case NetworkCommand.STROKE_HIDE:STROKE_HIDE(pdata, client);break;
				case NetworkCommand.STROKE_UNHIDE:STROKE_UNHIDE(pdata, client);break;
				
				case NetworkCommand.ERASE_START:ERASE_START(pdata, client);break;
				case NetworkCommand.ERASE_END:ERASE_END(pdata, client);break;
				
				
				case NetworkCommand.GROUP_START:GROUP_START(pdata,client);break;
				case NetworkCommand.GROUP_APPEND:GROUP_APPEND(pdata,client);break;
				case NetworkCommand.GROUP_APPEND_CLUSTER:GROUP_APPEND_CLUSTER(pdata,client);break;
				case NetworkCommand.GROUP_FINISH:GROUP_FINISH(pdata,client);break;
				case NetworkCommand.GROUP_DROP:GROUP_DROP(pdata,client);break;
				case NetworkCommand.GROUP_DELETE:GROUP_DELETE(pdata,client);break;
				case NetworkCommand.GROUP_MOVE:GROUP_MOVE(pdata,client);break;
				case NetworkCommand.GROUP_MOVE_END:GROUP_MOVE_END(pdata,client);break;
				case NetworkCommand.GROUP_MOVE_START:GROUP_MOVE_START(pdata,client);break;
				case NetworkCommand.GROUP_SET_CHILD_GROUPS:GROUP_SET_CHILD_GROUPS(pdata,client);break;
				case NetworkCommand.GROUP_SET_CHILD_STROKES:GROUP_SET_CHILD_STROKES(pdata,client);break;
				case NetworkCommand.GROUP_SET_CHILD_ARROWS:GROUP_SET_CHILD_ARROWS(pdata,client);break;
				case NetworkCommand.GROUP_SET_PARENT:GROUP_SET_PARENT(pdata,client);break;
				case NetworkCommand.GROUP_SET_PERM:GROUP_SET_PERM(pdata,client);break;
				case NetworkCommand.GROUP_RECTIFY:GROUP_RECTIFY(pdata,client);break;
				case NetworkCommand.GROUP_CIRCLIFY:GROUP_CIRCLIFY(pdata,client);break;
				case NetworkCommand.GROUP_CHILDREN_COLOR:GROUP_CHILDREN_COLOR(pdata,client);break;
				case NetworkCommand.GROUP_LOAD:GROUP_LOAD(pdata,client);break;
				case NetworkCommand.GROUP_IMAGE_LOAD:GROUP_IMAGE_LOAD(pdata, client);break;
				case NetworkCommand.GROUP_IMAGE_DOWNLOAD:GROUP_IMAGE_DOWNLOAD(pdata, client);break;
				case NetworkCommand.GROUP_REQUEST_HASH_CHECK:GROUP_REQUEST_HASH_CHECK(pdata,client);break;
				case NetworkCommand.GROUP_COPY_TO_CANVAS:GROUP_COPY_TO_CANVAS(pdata,client);break;
				case NetworkCommand.GROUP_SET_TEXT:GROUP_SET_TEXT(pdata,client);break;
//				case NetworkCommand.GROUP_SHRINK_TO_CONTENTS:GROUP_SHRINK_TO_CONTENTS(pdata,client);break;
				case NetworkCommand.GROUP_ROTATE:GROUP_ROTATE(pdata,client);break;
				case NetworkCommand.GROUP_SCALE:GROUP_SCALE(pdata,client);break;
				case NetworkCommand.GROUP_CREATE_TEXT_GROUP:GROUP_CREATE_TEXT_GROUP(pdata,client);break;
				case NetworkCommand.GROUP_MAKE_RECTANGLE:GROUP_MAKE_RECTANGLE(pdata,client);break;
				case NetworkCommand.GROUP_COPY_WITH_MAPPINGS:GROUP_COPY_WITH_MAPPINGS(pdata,client);break;
				

				case NetworkCommand.ARROW_CREATE:ARROW_CREATE(pdata,client);break;
				case NetworkCommand.ARROW_DELETE:ARROW_DELETE(pdata,client);break;
				case NetworkCommand.ARROW_SET_TYPE:ARROW_SET_TYPE(pdata,client);break;
				case NetworkCommand.ARROW_SET_COLOR:ARROW_SET_COLOR(pdata,client);break;
				
				case NetworkCommand.CONNECTOR_LOAD:CONNECTOR_LOAD(pdata,client);break;
				case NetworkCommand.CONNECTOR_DELETE:CONNECTOR_DELETE(pdata,client);break;
				case NetworkCommand.CONNECTOR_LINEARIZE:CONNECTOR_LINEARIZE(pdata,client);break;
				case NetworkCommand.CONNECTOR_MOVE_ANCHOR:CONNECTOR_MOVE_ANCHOR(pdata,client);break;
				case NetworkCommand.CONNECTOR_MOVE_ANCHOR_START:CONNECTOR_MOVE_ANCHOR_START(pdata,client);break;
				case NetworkCommand.CONNECTOR_MOVE_ANCHOR_END:CONNECTOR_MOVE_ANCHOR_END(pdata,client);break;
				
				case NetworkCommand.ELEMENT_ADD:ELEMENT_ADD(pdata,client);break;
				case NetworkCommand.ELEMENT_REMOVE:ELEMENT_REMOVE(pdata,client);break;
				
				case NetworkCommand.UDP_CHALLENGE:UDP_CHALLENGE(pdata, client);break;
				
				
				case NetworkCommand.CANVAS_CREATE:CANVAS_CREATE(pdata,client);break;
				case NetworkCommand.CANVAS_INFO:CANVAS_INFO(pdata,client);break;
				case NetworkCommand.CANVAS_SET:CANVAS_SET(pdata,client);break;
				case NetworkCommand.CANVAS_LIST:CANVAS_LIST(pdata,client);break;
				case NetworkCommand.CANVAS_UNDO:CANVAS_UNDO(pdata,client);break;
				case NetworkCommand.CANVAS_REDO:CANVAS_REDO(pdata,client);break;
				case NetworkCommand.CANVAS_CLEAR:CANVAS_CLEAR(pdata,client);break;
				case NetworkCommand.CANVAS_COPY:CANVAS_COPY(pdata,client);break;
				case NetworkCommand.CANVAS_LOCK:CANVAS_LOCK(pdata,client);break;
				case NetworkCommand.CANVAS_LOAD:CANVAS_LOAD(pdata,client);break;
				case NetworkCommand.CANVAS_DELETE:CANVAS_DELETE(pdata,client);break;

				case NetworkCommand.CONSISTENCY_CHECK:CONSISTENCY_CHECK(pdata,client);break;
				case NetworkCommand.CONSISTENCY_RESYNC_CANVAS:CONSISTENCY_RESYNC_CANVAS(pdata, client);break;
				
				case NetworkCommand.RESTORE_START:RESTORE_START(pdata);break;

//				case NetworkCommand.GRID_SIZE:GRID_SIZE(pdata,client);break;
				case NetworkCommand.PLUGIN_EVENT:PLUGIN_EVENT(pdata,client);break;

				case NetworkCommand.LIST_CREATE:LIST_CREATE(pdata,client);break;
				case NetworkCommand.LIST_LOAD:LIST_LOAD(pdata,client);break;
				case NetworkCommand.LIST_CHECK_SET:LIST_CHECK_SET(pdata,client);break;
				case NetworkCommand.CANVASVIEW_SCRAP_LOAD:CANVASVIEW_SCRAP_LOAD(pdata,client);break;
				
				case NetworkCommand.IMAGE_TRANSFER:IMAGE_TRANSFER(pdata, client);break;
				case NetworkCommand.IMAGE_TRANSFER_FILE:IMAGE_TRANSFER_FILE(pdata, client);break;

				case NetworkCommand.PRESENCE_VIEW_CANVAS:PRESENCE_VIEW_CANVAS(pdata,client);break;
				case NetworkCommand.PRESENCE_LEAVE_CANVAS:PRESENCE_LEAVE_CANVAS(pdata,client);break;
				case NetworkCommand.PRESENCE_CANVAS_RESET:PRESENCE_CANVAS_RESET(pdata,client);break;
				case NetworkCommand.PRESENCE_CANVAS_USERS:PRESENCE_CANVAS_USERS(pdata,client);break;
				
				case NetworkCommand.DEFAULT_EMAIL:DEFAULT_EMAIL(pdata, client);break;
				
				default:
					break;
			}//switch
			
			CalicoEventHandler.getInstance().fireEvent(command, pdata, client);
			
		}
		/*catch(NoSessionsException nse)
		{
			ClientManager.send(client,CalicoPacket.getPacket(
					NetworkCommand.STATUS_MESSAGE, 
					"No sessions have been created on this server."
			));
		}*/
		/*catch(ClientNotAuthedException cnae)
		{
			// NOTHING!
		}*/
		catch(Exception e)
		{
			e.printStackTrace(System.out);
		}
	}
	
	public static void PLUGIN_EVENT(CalicoPacket p, Client client)
	{
		String eventname = p.getString();
		try
		{
			Class<?> pluginEvent = CalicoPluginManager.getEventClass(eventname);
			
			CalicoEvent eventObj = (CalicoEvent) pluginEvent.newInstance();
			eventObj.getPacketData(p, client);
			
			CalicoPluginManager.sendEventToPlugins(eventObj);
			
		}
		catch(Exception e)
		{
			
		}
		
	}
	
	public static void GROUP_REQUEST_HASH_CHECK(CalicoPacket p, Client client)
	{
		long uuid = p.getLong();
		
		if(uuid==0L)
		{
			CGroupController.send_hash_checks(client);
		}
		
	}
	public static void STROKE_REQUEST_HASH_CHECK(CalicoPacket p, Client client)
	{

	}
	
	public static void CANVAS_CREATE(CalicoPacket p, Client c)
	{
		long canvasId = p.getLong();
		
		CCanvas canvas = new CCanvas(canvasId);
		CCanvasController.canvases.put(canvasId, canvas);
		
		ClientManager.send(canvas.getInfoPacket());
	}
	
	public static void CANVASVIEW_SCRAP_LOAD(CalicoPacket p, Client client)
	{
		long uuid = p.getLong();
		long cuid = p.getLong();
		long puid = p.getLong();
		boolean isperm = p.getBoolean();
		int count = p.getCharInt();
		int x = 0;
		int y = 0;
		
		if(count<=0)
		{
			return;
		}
		
		
		int[] xArr = new int[count], yArr = new int[count];
		for(int i=0;i<count;i++)
		{
			xArr[i] = p.getInt();
			yArr[i] = p.getInt();
//			CGroupController.no_notify_append(uuid, x, y);
		}
		
		boolean captureChildren = p.getBoolean();
		double rotation = p.getDouble();
		double scaleX = p.getDouble();
		double scaleY = p.getDouble();
		String text = p.getString();
		
		long targetCanvas = p.getLong();
		
		
//		CGroupController.groupdb.get(uuid).finish();

		CGroupController.no_notify_load_canvasview_scrap(uuid, cuid, puid, isperm, xArr, yArr,
				captureChildren, rotation, scaleX, scaleY, text, targetCanvas);
	}
	
	// this event occurs on server restore
	public static void CANVAS_INFO(CalicoPacket p, Client c)
	{
		long canvasId = p.getLong();
		int index = p.getInt();

		CCanvas canvas = new CCanvas(canvasId);
		CCanvasController.canvases.put(canvasId, canvas);

		if (index != canvas.getIndex())
			System.out.println("Warning: canvas with uuid " + canvas.getUUID() + " received the wrong index " + canvas.getIndex() + ". It should be " + index + ".");
	}
	
	public static void CANVAS_CLEAR(CalicoPacket p, Client c)
	{
		long uuid = p.getLong();
		
		// erase the canvas
		CCanvasController.no_notify_clear(uuid);
		
		
		// SNAPSHOT
		CCanvasController.snapshot(uuid);
		
		// Resend to all (even the sender)
		ClientManager.send_except(c, p);
		
	}
	public static void CANVAS_COPY(CalicoPacket p, Client c)
	{		
		long cuid = p.getLong();
		long to_canvasuuid = p.getLong();
		
		CCanvasController.copy_canvas(cuid, to_canvasuuid);
		
		CCanvasController.snapshot(cuid);	
	}
	
	public static void CANVAS_LOCK(CalicoPacket p, Client c)
	{
		long canvas = p.getLong();
		boolean lock = p.getBoolean();
		String lockedBy = p.getString();
		long time = p.getLong();
		
		CCanvasController.no_notify_lock_canvas(canvas, lock, lockedBy, time);
		
		ClientManager.send_except(c, p);
	}
	
	public static void CANVAS_LOAD(CalicoPacket p, Client c)
	{
		//get info from packets
		p.rewind();
		p.getInt(); //command
		long cuuid = p.getLong();
		int numPackets = p.getInt();
		CalicoPacket[] packets = new CalicoPacket[numPackets];
		int packetSize;
		for (int i = 0; i < packets.length; i++)
		{
			packetSize = p.getInt();
			packets[i] = new CalicoPacket(p.getByteArray(packetSize));
		}
		
		//restore canvas
		CCanvasController.no_notify_clear_for_state_change(cuuid);
		
		for (int i = 0; i < packets.length; i++)
		{
			packets[i].rewind();
			int comm = packets[i].getInt();
			
			// As long as its not the canvas_info, we should just send it along
			if(comm!=NetworkCommand.CANVAS_INFO)
			{
				ProcessQueue.receive(comm, null, packets[i]);
			}
		}
		
		//Remove temp scraps after undo/redo
		long[] guuid = CCanvasController.canvases.get(cuuid).getChildGroups();
		for (int i = 0; i < guuid.length; i++)
		{
			if (!CGroupController.groups.get(guuid[i]).isPermanent())
			{
				CGroupController.drop(guuid[i]);
			}
		}
		
		CCanvasController.state_change_complete(cuuid);

	}

	public static void CANVAS_SET(CalicoPacket p,Client c)
	{
		long str = p.getLong();

		if( !CCanvasController.canvases.containsKey(str) )
		{
			CCanvasController.canvases.put(str, new CCanvas(str) );
		}
	}
	
	public static void CANVAS_LIST(CalicoPacket pNOTUSED,Client c)
	{
		ClientManager.sendCanvasList(c);
	}
	
	public static void CANVAS_UNDO(CalicoPacket p, Client c)
	{
		long uuid = p.getLong();
		if(!CCanvasController.undo(uuid))
		{
			ClientManager.send(c, CalicoPacket.getPacket(NetworkCommand.STATUS_MESSAGE, "No more undo history"));
		}
	}
	public static void CANVAS_REDO(CalicoPacket p, Client c)
	{
		long uuid = p.getLong();
		if(!CCanvasController.redo(uuid))
		{
			ClientManager.send(c, CalicoPacket.getPacket(NetworkCommand.STATUS_MESSAGE, "No more redo history"));
		}
	}
	
	public static void CANVAS_DELETE(CalicoPacket p, Client c)
	{
		long uuid = p.getLong();
		
		synchronized(CalicoServer.canvasThreads)
		{
			CanvasThread thread = CalicoServer.canvasThreads.remove(uuid);
			if (thread != null)
			{
				// seems like it should be stopped or something
			}
		}
		
		CCanvasController.no_notify_clear(uuid);
		CCanvasController.canvases.remove(uuid);
		ClientManager.send_except(c, p);
	}
	
	public static void CONSISTENCY_RESYNC_CANVAS(CalicoPacket p, Client c)
	{
		long uuid = p.getLong();
		if (CCanvasController.exists(uuid))
		{
			ClientManager.send(c, CalicoPacket.getPacket(NetworkCommand.CANVAS_CLEAR_FOR_SC, uuid));
			ClientManager.send(c, CCanvasController.canvases.get(uuid).getUpdatePackets());
			ClientManager.send(c, CalicoPacket.getPacket(NetworkCommand.CANVAS_SC_FINISH, uuid));
			if (ClientManager.out_of_sync_clients.contains(c.getClientID()))
			{
				ClientManager.out_of_sync_clients.remove(c.getClientID());
			}
		}
	}
	

	
	public static void GROUP_START(CalicoPacket p, Client client)
	{
		long uuid = p.getLong();
		long canvasuid = p.getLong();
		long parent_uid = p.getLong();
		
		int ispermint = p.getInt();
		boolean isperm = ispermint==1 ? true : false;
		
		CGroupController.no_notify_start(uuid, canvasuid, parent_uid, isperm);
				
		ClientManager.send_except(client, p);
	}
	public static void GROUP_APPEND(CalicoPacket p, Client client)
	{
		long uuid = p.getLong();
		int x = p.getInt();
		int y = p.getInt();
		CGroupController.no_notify_append(uuid, x, y);
		
		ClientManager.send_except(client, p);
	}
	public static void GROUP_APPEND_CLUSTER(CalicoPacket p, Client client)
	{
		long uuid = p.getLong();
		int count = p.getCharInt();
		
		for(int i=0;i<count;i++)
		{
			int x = p.getInt();
			int y = p.getInt();
			CGroupController.no_notify_append(uuid, x, y);
		}
		
		ClientManager.send_except(client, p);
	}
	
	public static void GROUP_MOVE(CalicoPacket p, Client client)
	{
		long uuid = p.getLong();
		int x = p.getInt();
		int y = p.getInt();
		CGroupController.no_notify_move(uuid, x, y);
		
		ClientManager.send_except(client, p);
	}
	public static void GROUP_MOVE_START(CalicoPacket p, Client client)
	{
		long guuid = p.getLong();
		
		CGroupController.no_notify_move_start(guuid);
		
		ClientManager.send_except(client, p);
	}
	public static void GROUP_MOVE_END(CalicoPacket p, Client client)
	{
		long uuid = p.getLong();
		int x = p.getInt();
		int y = p.getInt();
		
		CGroupController.no_notify_move_end(uuid, x, y);
		
		ClientManager.send_except(client, p);
		CCanvasController.snapshot_group(uuid);	
	}
	public static void GROUP_DROP(CalicoPacket p, Client client)
	{
		long uuid = p.getLong();
		boolean wasPerm;
		if (CGroupController.exists(uuid))
			wasPerm = CGroupController.groups.get(uuid).isPermanent();
		else
			return;
		
		CGroupController.no_notify_drop(uuid, true);
		//CGroupController.drop(uuid);
		
		if(client!=null)
		{
			if (wasPerm)
				CCanvasController.snapshot_group(uuid);
			ClientManager.send_except(client, p);
		}
	}
	public static void GROUP_DELETE(CalicoPacket p, Client client)
	{
		long uuid = p.getLong();
		CGroupController.no_notify_delete(uuid);
		ClientManager.send_except(client, p);
		
		if(client!=null)// && CGroupController.groups.get(uuid).isPermanent())
		{
			CCanvasController.snapshot_group(uuid);
		}
	}
	public static void GROUP_FINISH(CalicoPacket p, Client client)
	{
		long uuid = p.getLong();
		boolean captureChildren = p.getBoolean();
		
		if(client!=null)
		{
			// This is a real client, so we should calculate the parenting for them
//			CGroupController.finish(uuid);
			CGroupController.no_notify_finish(uuid, captureChildren);
			ClientManager.send_except(client, p);
			
			if (captureChildren)
				ClientManager.send( CGroupController.groups.get(uuid).getParentingUpdatePackets() );
//				CGroupController.calculateParenting(uuid);
		}
		else
		{
			// this is the server, so we assume it knows whats up
			CGroupController.no_notify_finish(uuid, captureChildren);
		}
		
		// Only make snapshots for groups that are perm
		if(client!=null && CGroupController.groups.get(uuid).isPermanent())
		{
			//CGroupController.groups.get(uuid).recheckParentAfterMove();
			
			CCanvasController.snapshot_group(uuid);
		}
		
		//ClientManager.sendExcept(client, p);
	}
	
	public static void GROUP_COPY_TO_CANVAS(CalicoPacket p, Client client)
	{
		long uuid = p.getLong();
		long new_canvasuid = p.getLong();
		long new_uuid = p.getLong();
		
		int shift_x = p.getInt();
		int shift_y = p.getInt();
		
		int final_x = p.getInt();
		int final_y = p.getInt();
		
		if(new_uuid==0L)
		{
			new_uuid = UUIDAllocator.getUUID();
		}
		
		CGroupController.copy_to_canvas(uuid, new_uuid, new_canvasuid, shift_x, shift_y, final_x, final_y, true);
		//if (!CGroupController.groups.get(uuid).isPermanent())
		//	CGroupController.drop(new_uuid);
//		CGroupController.copy(uuid, new_uuid, new_canvasuid, shift_x, shift_y, true);

		CCanvasController.snapshot(new_canvasuid);
	}
	
	public static void GROUP_COPY_WITH_MAPPINGS(CalicoPacket p, Client client)
	{
		long guuid = p.getLong();
		
		Long2ReferenceArrayMap<Long> UUIDMappings = new Long2ReferenceArrayMap<Long>();
		int mappingSize = p.getInt();
		for (int i = 0; i < mappingSize; i++)
		{
			long key = p.getLong();
			long value = p.getLong();
			UUIDMappings.put(key, new Long(value));
		}
		
		CGroupController.no_notify_copy(guuid, 0l, UUIDMappings, true);
		ClientManager.send_except(client,p);
	}
	
	public static void GROUP_SET_TEXT(CalicoPacket p, Client client)
	{
		long uuid = p.getLong();
		String str = p.getString();
		
		CGroupController.no_notify_set_text(uuid, str);
		ClientManager.send_except(client, p);
		
	}
	
//	public static void GROUP_SHRINK_TO_CONTENTS(CalicoPacket p, Client client)
//	{
//		long uuid = p.getLong();
//		
//		CGroupController.shrink_to_contents(uuid);
//		
//		ClientManager.send_except(client, p);
//		
//		if(client!=null && CGroupController.groups.get(uuid).isPermanent())
//		{
//			//CGroupController.groups.get(uuid).recheckParentAfterMove();
//			
//			CCanvasController.snapshot_group(uuid);
//		}
//	}
	
	public static void GROUP_SET_PERM(CalicoPacket p, Client client)
	{
		long uuid = p.getLong();
		int perm = p.getInt();
		
		CGroupController.no_notify_set_permanent(uuid, (perm==1 ? true : false));

		// Only make snapshots for groups that are perm
		if(client!=null)// && perm==1)
		{
			CCanvasController.snapshot_group(uuid);
			
			ClientManager.send_except(client, p);
		}
	}
	
	
	public static void GROUP_SET_PARENT(CalicoPacket p, Client client)
	{
		long uuid = p.getLong();
		long parent_uid = p.getLong();
		
		CGroupController.no_notify_set_parent(uuid, parent_uid);
	}
	
	public static void GROUP_RECTIFY(CalicoPacket p, Client client)
	{
		ClientManager.send_except(client, p);
	}
	public static void GROUP_CIRCLIFY(CalicoPacket p, Client client)
	{
		ClientManager.send_except(client, p);
	}
	
	
	
	public static void GROUP_CHILDREN_COLOR(CalicoPacket p, Client client)
	{
	}
	
	private static void GROUP_SET_CHILD_STROKES(CalicoPacket p, Client client)
	{
		long uuid = p.getLong();
		
		int numpackets = p.getCharInt();
		
		if(numpackets>0)
		{
			long[] child_uuids = new long[numpackets];
			for(int i=0;i<numpackets;i++)
			{
				child_uuids[i] = p.getLong();
			}
			CGroupController.no_notify_set_child_strokes(uuid, child_uuids);
		}
	}
	private static void GROUP_SET_CHILD_ARROWS(CalicoPacket p, Client client)
	{
		long uuid = p.getLong();
		
		int numpackets = p.getCharInt();
		
		if(numpackets>0)
		{
			long[] child_uuids = new long[numpackets];
			for(int i=0;i<numpackets;i++)
			{
				child_uuids[i] = p.getLong();
			}
			CGroupController.no_notify_set_child_arrows(uuid, child_uuids);
		}
	}
	
	private static void GROUP_SET_CHILD_GROUPS(CalicoPacket p, Client client)
	{
		long uuid = p.getLong();
		
		int numpackets = p.getCharInt();
		
		if(numpackets>0)
		{
			long[] child_uuids = new long[numpackets];
			for(int i=0;i<numpackets;i++)
			{
				child_uuids[i] = p.getLong();
			}
			CGroupController.no_notify_set_child_groups(uuid, child_uuids);
		}
	}
	
	public static void GROUP_LOAD(CalicoPacket p, Client client)
	{
		long uuid = p.getLong();
		long cuid = p.getLong();
		long puid = p.getLong();
		boolean isperm = p.getBoolean();
		int count = p.getCharInt();
		int x = 0;
		int y = 0;
		
		CGroupController.no_notify_start(uuid, cuid, puid, isperm);

		for(int i=0;i<count;i++)
		{
			x = p.getInt();
			y = p.getInt();
			CGroupController.no_notify_append(uuid, x, y);
		}
		

		boolean captureChildren = false;
		double rotation;
		double scaleX;
		double scaleY;
		String text;
		captureChildren = p.getBoolean();
		rotation = p.getDouble();
		scaleX = p.getDouble();
		scaleY = p.getDouble();
		text = p.getString();

//		CGroupController.groups.get(uuid).finish();
		CGroupController.groups.get(uuid).primative_rotate(rotation);
		CGroupController.groups.get(uuid).primative_scale(scaleX, scaleY);
		CGroupController.groups.get(uuid).setText(text);
		
		CGroupController.no_notify_finish(uuid, captureChildren, false);

		if(client!=null)
		{
			ClientManager.send_except(client, p);
			if (isperm)
				CCanvasController.snapshot_group(uuid);
			else
			{
				ClientManager.getClientThread(client.getClientID()).setTempScrapUUID(uuid);
			}
		}
		
		//if (captureChildren)
			//ClientManager.send( CGroupController.groups.get(uuid).getParentingUpdatePackets() );
		
	}
	
	private static void GROUP_IMAGE_LOAD(CalicoPacket p, Client client)
	{
		//ClientManager.send( CalicoPacket.getPacket( NetworkCommand.GROUP_ADD_IMAGE, uuid, cuid, puid, imgURL, imageWidth, imageHeight) );
		
		long uuid = p.getLong();
		long cuid = p.getLong();
		long puid = p.getLong();
		String url = p.getString();
		int port = p.getInt(); //Not used on the server
		String localPath = p.getString(); //Not used on the server
		int imgX = p.getInt();
		int imgY = p.getInt();
		int imgW = p.getInt();
		int imgH = p.getInt();
		boolean perm = p.getBoolean();
		boolean captureChildren = p.getBoolean();
		double rotation = p.getDouble();
		double scaleX = p.getDouble();
		double scaleY = p.getDouble();
		
		if (p.remaining() > 0)
		{
			int len = p.getInt();
			
			byte[] imageByteArray = p.getByteArray(len);
			try
			{
				CImageController.save_to_disk(uuid, uuid + ".png", imageByteArray);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			try {
				CImageController.download_image(uuid, url);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			url = CImageController.getImageURL(uuid);
		}
		
		CGroupController.no_notify_create_image_group(uuid, cuid, puid, url, imgX, imgY, imgW, imgH);
		CGroupController.groups.get(uuid).primative_rotate(rotation);
		CGroupController.groups.get(uuid).primative_scale(scaleX, scaleY);

		if (client != null)
		{
			ClientManager.send_except(client, p);
		}
	}
	
	private static void GROUP_IMAGE_DOWNLOAD(CalicoPacket p, Client client)
	{	
		long uuid = p.getLong();
		long cuuid = p.getLong();
		String imageURL = p.getString();
		int x = p.getInt();
		int y = p.getInt();
		
		if (!CGroupController.createImageGroup(uuid, cuuid, imageURL, x, y))
			ClientManager.send(client, CalicoPacket.getPacket(NetworkCommand.GROUP_IMAGE_LOAD,0l));

		//Don't broadcast it to the clients... the server will download the image to have it locally and then broadcast the link to that.
	}
	
	public static void GROUP_ROTATE(CalicoPacket p, Client client)
	{
		long guuid = p.getLong();
		double theta = p.getDouble();
		
		CGroupController.no_notify_rotate(guuid, theta);
		
		if (client != null)
		{
			ClientManager.send_except(client, p);
//			CCanvasController.snapshot_group(guuid);
		}
	}
	
	public static void GROUP_SCALE(CalicoPacket p, Client client)
	{
		long guuid = p.getLong();
		double scaleX = p.getDouble();
		double scaleY = p.getDouble();
		
		CGroupController.no_notify_scale(guuid, scaleX, scaleY);
//		CGroupController.recheck_parent(guuid);
		
		if (client != null)
		{
			ClientManager.send_except(client, p);
			CCanvasController.snapshot_group(guuid);
		}
	}
	
	public static void GROUP_CREATE_TEXT_GROUP(CalicoPacket p, Client client)
	{
		long guuid = p.getLong();
		long cuuid = p.getLong();
		String text = p.getString();
		int x = p.getInt();
		int y = p.getInt();
		
		CGroupController.no_notify_create_text_scrap(guuid, cuuid, text, x, y);
		if (client != null)
		{
			ClientManager.send_except(client, p);
			CCanvasController.snapshot_group(guuid);
		}
	}
	
	public static void GROUP_MAKE_RECTANGLE(CalicoPacket p , Client client)
	{
		long guuid = p.getLong();
		int x = p.getInt();
		int y = p.getInt();
		int width = p.getInt();
		int height = p.getInt();
		
		CGroupController.no_notify_make_rectangle(guuid, x, y, width, height);
		
		if (client != null)
		{
			ClientManager.send_except(client, p);
		}
		
		if(client!=null && CGroupController.isPermanent(guuid))
		{
			//CGroupController.groups.get(uuid).recheckParentAfterMove();
			
			CCanvasController.snapshot_group(guuid);
		}
	}
	
	
	
	public static void JOIN(CalicoPacket p, Client client)
	{
		String username = p.getString();
		String password = p.getString();

		// check auth
		if( ClientManager.checkAuth(username, password) )
		{
			// Set their username
			ClientManager.setUsername(client.getClientID(), username);
			//logger.debug("Setting client "+clientid+"'s username to "+username);
			
			//int clientid = ClientManager.getClientID(client);
			ClientManager.getClientThread(client.getClientID()).checkIfClientExists();

			// Join them to the server
			ClientManager.joinClient(client.getClientID());
			
			// Respond with AUTH OK
			ClientManager.send(client, CalicoPacket.command(NetworkCommand.AUTH_OK) );
			
			CalicoPluginManager.sendEventToPlugins(new ClientConnect(client));
			
		}
		else
		{
			// FAILZOR!
		}
	}
	
	public static void HEARTBEAT(CalicoPacket p, Client client)
	{
		long canvas = p.getLong();
		int sig = p.getInt();
		
		if (CCanvasController.getCanvasSignature(canvas) != sig
			&& !ClientConsistencyListener.ignoreConsistencyCheck
			&& CCanvasController.canvases.get(canvas).get_signature() != sig)
		{
			ClientManager.out_of_sync_clients.add(client.getClientID());
			ClientManager.send(client, CalicoPacket.command(NetworkCommand.CONSISTENCY_FAILED));
			if (COptions.debug.consistency_debug_enabled)
			{
				ClientManager.send(client, CCanvasController.getCanvasConsistencyDebugPacket(canvas));
			}
		}
		else
		{
			if (ClientManager.out_of_sync_clients.contains(client.getClientID()))
			{
				ClientManager.send(client, CalicoPacket.command(NetworkCommand.CONSISTENCY_RESYNCED));
				ClientManager.out_of_sync_clients.remove(client.getClientID());
			}
		}
	}

	public static void CONSISTENCY_CHECK(CalicoPacket pNOTUSED,Client client)
	{
		ClientManager.sendConsistencyUpdate(client);
	}
	
	public static void CONSISTENCY_CHECK_CONTINUE(CalicoPacket p,Client client)
	{
		long lastSuccessfullySentUUID = p.getLong();
		
		ClientManager.sendConsistencyUpdate_continue(client, lastSuccessfullySentUUID);
	}


	public static void UUID_GET_BLOCK(CalicoPacket p, Client client)
	{
		ClientManager.send( client, UUIDAllocator.getClientUUIDBlock() );
	}
	
	
	
	public static void ARROW_CREATE(CalicoPacket p, Client client)
	{
		//UUID CANVASUID ARROW_TYPE ANCHOR_A_TYPE ANCHOR_A_UUID ANCHOR_A_X ANCHOR_A_Y   ANCHOR_B_TYPE ANCHOR_B_UUID ANCHOR_B_X ANCHOR_B_Y
		long uid = p.getLong();
		long cuid = p.getLong();
		int arrowType = p.getInt();
		

		//int red = p.getInt();
		//int green = p.getInt();
		//int blue = p.getInt();

		Color color = p.getColor();//new Color(red, green, blue);

		int aType = p.getInt();
		long aUUID = p.getLong();
		int ax = p.getInt();
		int ay = p.getInt();
		
		int bType = p.getInt();
		long bUUID = p.getLong();
		int bx = p.getInt();
		int by = p.getInt();
		
		
		CArrowController.no_notify_start(uid, cuid, arrowType, color, 
			new AnchorPoint(aType, aUUID, ax, ay),
			new AnchorPoint(bType, bUUID, bx, by)
		);
		

		ClientManager.send_except(client, p);
		
		if(client!=null)
		{
			CCanvasController.snapshot_arrow(uid);
		}
		
		
	}
	public static void ARROW_DELETE(CalicoPacket p, Client client)
	{
		long u = p.getLong();

		CArrowController.no_notify_delete(u);

		ClientManager.send_except(client, p);

		if(client!=null)
		{
			CCanvasController.snapshot_arrow(u);
		}

	}
	public static void ARROW_SET_TYPE(CalicoPacket p, Client client)
	{
		long u = p.getLong();

		int type = p.getInt();
		
		if(!CArrowController.arrows.containsKey(u))
		{
			// Invalid delete request
			return;
		}

		CArrowController.arrows.get(u).setArrowType(type);
		
		ClientManager.send_except(client, p);
	}
	public static void ARROW_SET_COLOR(CalicoPacket p, Client client)
	{
		long u = p.getLong();
		int r = p.getInt();
		int g = p.getInt();
		int b = p.getInt();

		if(!CArrowController.arrows.containsKey(u))
		{
			// Invalid delete request
			return;
		}

		CArrowController.arrows.get(u).setColor(r, g, b);

		ClientManager.send_except(client, p);
	}
	
	public static void CONNECTOR_LOAD(CalicoPacket p, Client client)
	{
		long uuid = p.getLong();
		long cuid = p.getLong();
		Color color = p.getColor();
		float thickness = p.getFloat();
		
		Point head = new Point(p.getInt(), p.getInt());
		Point tail = new Point(p.getInt(), p.getInt());
		
		int nPoints = p.getInt();
		double[] orthogonalDistance = new double[nPoints];
		double[] travelDistance = new double[nPoints];
		for (int i = 0; i < nPoints; i++)
		{
			orthogonalDistance[i] = p.getDouble();
			travelDistance[i] = p.getDouble();
		}
		
		long anchorHead = p.getLong();
		long anchorTail = p.getLong();
		
		CConnectorController.no_notify_create(uuid, cuid, color, thickness, head, tail, orthogonalDistance, travelDistance, anchorHead, anchorTail);
		
		ClientManager.send_except(client, p);
		
		if(client!=null)
		{
			CCanvasController.snapshot(cuid);
		}
	}
	
	public static void CONNECTOR_DELETE(CalicoPacket p, Client client)
	{
		long uuid = p.getLong();
		CConnectorController.no_notify_delete(uuid);

		ClientManager.send_except(client, p);
		
		if(client!=null)
		{
			CCanvasController.snapshot_connector(uuid);
		}
	}
	
	public static void CONNECTOR_LINEARIZE(CalicoPacket p, Client client)
	{
		long uuid = p.getLong();
		CConnectorController.no_notify_linearize(uuid);

		ClientManager.send_except(client, p);
		
		if(client!=null)
		{
			CCanvasController.snapshot_connector(uuid);
		}
	}
	
	public static void CONNECTOR_MOVE_ANCHOR(CalicoPacket p, Client client)
	{
		long uuid = p.getLong();
		int type = p.getInt();
		int x = p.getInt();
		int y = p.getInt();
		
		CConnectorController.no_notify_move_group_anchor(uuid, type, x, y);

		ClientManager.send_except(client, p);
		
		if(client!=null)
		{
			CCanvasController.snapshot_connector(uuid);
		}
	}
	
	public static void CONNECTOR_MOVE_ANCHOR_START(CalicoPacket p, Client client)
	{
		long uuid = p.getLong();
		int type = p.getInt();
		
		CConnectorController.no_notify_move_group_anchor_start(uuid, type);

		ClientManager.send_except(client, p);

	}
	
	public static void CONNECTOR_MOVE_ANCHOR_END(CalicoPacket p, Client client)
	{
		long uuid = p.getLong();
		int type = p.getInt();
		
		CConnectorController.no_notify_move_group_anchor_end(uuid, type);

		ClientManager.send_except(client, p);
		
		if(client!=null)
		{
			CCanvasController.snapshot_connector(uuid);
		}
	}
	
	public static void ELEMENT_ADD(CalicoPacket p, Client client)
	{
		ComposableElement element = ComposableElementController.getElementFromPacket(p);
		
		if (element != null)
		{
			ComposableElementController.no_notify_addElement(element);
		}
		
		ClientManager.send_except(client, p);
		
		if (client != null)
		{
			ComposableElement elem = ComposableElementController.elementList.get(element.getComponentUUID()).get(element.getElementUUID());
				
		}
	}
	
	public static void ELEMENT_REMOVE(CalicoPacket p, Client client)
	{
		long euuid = p.getLong();
		long cuuid = p.getLong();
		
		ComposableElementController.no_notify_removeElement(euuid, cuuid);
		
		ClientManager.send_except(client, p);
	}
	

	public static void STROKE_START(CalicoPacket p, Client client)
	{
		long uuid = p.getLong();
		long cuid = p.getLong();
		long puid = p.getLong();
		
		int red = p.getInt();
		int green = p.getInt();
		int blue = p.getInt();
		
		float thickness = p.getFloat();
		
		CStrokeController.no_notify_start(uuid, cuid, puid, new Color(red, green, blue), thickness);

		ClientManager.send_except(client, p);
	}
	public static void STROKE_APPEND(CalicoPacket p, Client client)
	{
		long uuid = p.getLong();
		int numpoints = p.getCharInt();
		
		int x = 0;
		int y = 0;
		
		for(int i=0;i<numpoints;i++)
		{
			x = p.getInt();
			y = p.getInt();

			CStrokeController.no_notify_append(uuid, x, y);
		}
		
		ClientManager.send_except(client, p);
		
	}
	public static void STROKE_FINISH(CalicoPacket p, Client client)
	{
		long uuid = p.getLong();

		CStrokeController.no_notify_finish(uuid);
		ClientManager.send_except(client, p);
		
		// Make a snapshot
		if(client!=null)
		{
//			if (CStrokeController.strokes.get(uuid).getPoints().npoints > 2)
				CCanvasController.snapshot_stroke(uuid);
//			else
//				CStrokeController.delete(uuid);
		}
	}
	public static void STROKE_SET_COLOR(CalicoPacket p, Client client)
	{
		long uuid = p.getLong();
		int red = p.getInt();
		int green = p.getInt();
		int blue = p.getInt();
		
		CStrokeController.no_notify_set_color(uuid, new Color(red, green, blue));
		
		ClientManager.send_except(client, p);
	}
	public static void STROKE_SET_PARENT(CalicoPacket p, Client client)
	{
		long uuid = p.getLong();
		long puid = p.getLong();
		
		if (CStrokeController.exists(uuid))
		{
			CStrokeController.strokes.get(uuid).setParentUUID(puid);
			ClientManager.send_except(client, p);
		}
	}
	public static void STROKE_MOVE(CalicoPacket p, Client client)
	{
		long uuid = p.getLong();
		int x = p.getInt();
		int y = p.getInt();

		CStrokeController.no_notify_move(uuid, x, y);
		
		ClientManager.send_except(client, p);
	}
	public static void STROKE_DELETE(CalicoPacket p, Client client)
	{
		long uuid = p.getLong();
		CStrokeController.no_notify_delete(uuid);

		ClientManager.send_except(client, p);

//		if(client!=null)
//		{
//			CCanvasController.snapshot_stroke(uuid);
//		}
		
	}
	
	public static void STROKE_LOAD(CalicoPacket p, Client client)
	{
		long uuid = p.getLong();
		long cuid = p.getLong();
		long puid = p.getLong();
		
		if (!CCanvasController.canvases.containsKey(cuid))
		{
			// canvas has been deleted
			return;
		}
		
		Color color = p.getColor();
		
		float thickness = p.getFloat();
		
		CStrokeController.no_notify_start(uuid, cuid, puid, color, thickness);

		int numpoints = p.getCharInt();
		
		int[] x = new int[numpoints];
		int[] y = new int[numpoints];
		
		for(int i=0;i<numpoints;i++)
		{
			x[i] = p.getInt();
			y[i] = p.getInt();
		}
		CStrokeController.no_notify_batch_append(uuid, x, y);
		
		double rotation;
		double scaleX;
		double scaleY;

		rotation = p.getDouble();
		scaleX = p.getDouble();
		scaleY = p.getDouble();

		
		CStrokeController.strokes.get(uuid).primative_rotate(rotation);
		CStrokeController.strokes.get(uuid).primative_scale(scaleX, scaleY);
		
		CStrokeController.no_notify_finish(uuid);

		ClientManager.send_except(client, p);
		if (client != null)
			CCanvasController.snapshot_stroke(uuid);
	}
	
	public static void STROKE_ROTATE(CalicoPacket p, Client client)
	{
		long uuid = p.getLong();
		double r = p.getDouble();

		if (CStrokeController.strokes.containsKey(uuid))
		{
			CStrokeController.strokes.get(uuid).rotate(r);
			ClientManager.send_except(client, p);
		}
	}
	
	public static void STROKE_SCALE(CalicoPacket p, Client client)
	{
		long uuid = p.getLong();
		double sX = p.getDouble();
		double sY = p.getDouble();

		if (CStrokeController.strokes.containsKey(uuid))
		{
			CStrokeController.strokes.get(uuid).scale(sX, sY);
			ClientManager.send_except(client, p);
		}
	}
	
	public static void STROKE_SET_AS_POINTER(CalicoPacket p, Client client)
	{
		long uuid = p.getLong();
		
		if (CStrokeController.strokes.containsKey(uuid))
		{
			CStrokeController.no_notify_delete(uuid);
			ClientManager.send_except(client, p);
			long canvas = CCanvasController.get_stroke_canvas(uuid);
			CCanvasController.snapshot_remove_most_recent_undo(canvas);
		}
		
	}
	
	public static void STROKE_HIDE(CalicoPacket p, Client client)
	{
		long uuid = p.getLong();
		boolean delete = p.getBoolean();

		if (CStrokeController.strokes.containsKey(uuid))
		{
			ClientManager.send_except(client, p);
		}
	}
	
	public static void STROKE_UNHIDE(CalicoPacket p, Client client)
	{
		long uuid = p.getLong();
		
		if (CStrokeController.strokes.containsKey(uuid))
		{
			ClientManager.send_except(client, p);
		}
	}
	
	public static void ERASE_START(CalicoPacket p, Client client)
	{
		long canvas = p.getLong();
	}
	
	public static void ERASE_END(CalicoPacket p, Client client)
	{
		long canvas = p.getLong();
		boolean erasedSomething = p.getBoolean();
		
		if (erasedSomething)
			CCanvasController.snapshot_stroke(canvas);
	}
	
	public static void STROKE_MAKE_SCRAP(CalicoPacket p, Client client)
	{
		long suuid = p.getLong();
		long new_guuid = p.getLong();
		
		long guuid = CStrokeController.no_notify_makeScrap(suuid, new_guuid);
		
		ClientManager.send_except(client, p);
		
		if(client!=null && CGroupController.groups.get(guuid).isPermanent())
		{
			//CGroupController.groups.get(uuid).recheckParentAfterMove();
			
			CCanvasController.snapshot_group(guuid);
		}
	}
	
	public static void STROKE_MAKE_SHRUNK_SCRAP(CalicoPacket p, Client client)
	{
		long suuid = p.getLong();
		long new_guuid = p.getLong();
		
		long guuid = CStrokeController.no_notify_makeShrunkScrap(suuid, new_guuid);
		
		ClientManager.send_except(client, p);
		
		if(client!=null && CGroupController.groups.get(guuid).isPermanent())
		{
			//CGroupController.groups.get(uuid).recheckParentAfterMove();
			
			CCanvasController.snapshot_group(guuid);
		}
	}
	
	public static void STROKE_DELETE_AREA(CalicoPacket p, Client client)
	{
		long suuid = p.getLong();
		long temp_guuid = p.getLong();
		
		CStrokeController.no_notify_deleteArea(suuid, temp_guuid);
		
		ClientManager.send_except(client, p);
		
		if(client!=null)
		{
			CCanvasController.snapshot_stroke(suuid);
		}
	}
	
	
	public static void UDP_CHALLENGE(CalicoPacket p, Client client)
	{
		long challenge = p.getLong();
		
		ClientManager.setClientChallenge(client.getClientID(), challenge);
		
	}
	
	public static void RESTORE_START(CalicoPacket p)
	{
		CCanvasController.canvases.clear();
		CCanvas.clearState();
	}
	
	public static void LIST_CREATE(CalicoPacket p, Client client)
	{
		long guuid = p.getLong();
		long luuid = p.getLong();
		
		CGroupDecoratorController.no_notify_list_create(guuid, luuid);
		
		if (client != null)
		{
			ClientManager.send_except(client, p);
			CCanvasController.snapshot_group(luuid);
		}
	}
	
	public static void LIST_LOAD(CalicoPacket p, Client client)
	{

		long luuid = p.getLong();
		long cuuid = p.getLong();
		long puuid = p.getLong();
		long guuid = p.getLong();
		
		CGroupDecoratorController.no_notify_list_load(guuid, luuid, cuuid, puuid);
		
		if (client != null)
		{
			ClientManager.send_except(client, p);
			CCanvasController.snapshot_group(luuid);
		}
	}
	
	public static void LIST_CHECK_SET(CalicoPacket p, Client client)
	{
		long luuid = p.getLong();
		long cuid = p.getLong();
		long puid = p.getLong();
		long guuid = p.getLong();
		boolean value = p.getBoolean();
		
		CGroupDecoratorController.no_notify_list_set_check(luuid, cuid, puid, guuid, value);
		
		if (client != null)
		{
			ClientManager.send_except(client, p);
			CCanvasController.snapshot(CGroupController.groups.get(luuid).getCanvasUUID());
		}
	}
	
	public static void IMAGE_TRANSFER(CalicoPacket p, Client client)
	{
		long uuid = p.getLong();
		long cuuid = p.getLong();
		long puid = p.getLong();
		int x = p.getInt();
		int y = p.getInt();
		String name = p.getString();
		int byteArraySize = p.getInt();
		byte[] bytes = new byte[byteArraySize];
		bytes = p.getByteArray(byteArraySize);

		try
		{
			CImageController.save_to_disk(uuid, name, bytes);
			if (!CGroupController.createImageGroup(uuid, cuuid, CImageController.getImageURL(uuid), x, y))
				ClientManager.send(client, CalicoPacket.getPacket(NetworkCommand.GROUP_IMAGE_LOAD,0l));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		//TODO: Finish
	}
	
	public static void IMAGE_TRANSFER_FILE(CalicoPacket p, Client client)
	{
		long uuid = p.getLong();
		long cuuid = p.getLong();
		long puid = p.getLong();
		String name = p.getString();
		int byteArraySize = p.getInt();
		byte[] bytes = new byte[byteArraySize];
		bytes = p.getByteArray(byteArraySize);
		
		try
		{
			CImageController.save_to_disk(uuid, name, bytes);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	

	
	/*
	 * public static final int PRESENCE_VIEW_CANVAS = 3001; // CUID
	public static final int PRESENCE_LEAVE_CANVAS = 3002; // CUID
	public static final int PRESENCE_CANVAS_RESET = 3003; // cuid
	public static final int PRESENCE_CANVAS_USERS = 3004;// CUID, NUMUSERS, USERID... USERIDn
	 */
	public static void PRESENCE_VIEW_CANVAS(CalicoPacket p, Client client) 
	{
		// when the user loads the canvas
		long cuid = p.getLong();
		int clientid = client.getClientID();
		
		long oldCUID = ClientManager.getClientThread(clientid).getCurrentCanvasUUID();
		
		if(!CCanvasController.canvases.containsKey(cuid)) {
			return;
		}
		
		// Remove them from the old canvas
		if(CCanvasController.canvases.containsKey(oldCUID)) {
			CCanvasController.canvases.get(cuid).removeClient(clientid);
		}
		
		ClientManager.getClientThread(clientid).setCurrentCanvasUUID(cuid);
		
		CCanvasController.canvases.get(cuid).addClient(clientid);
		
		int[] clientIds = CCanvasController.canvases.get(cuid).getClients();
		CalicoPacket outPacket = CalicoPacket.getPacket(NetworkCommand.PRESENCE_CANVAS_USERS, cuid, clientIds.length);
		if(clientIds.length>0) {
			for(int j=0;j<clientIds.length;j++) {
				outPacket.putInt(clientIds[j]);
			}
		}
		ClientManager.send(outPacket);
		
	}
	public static void PRESENCE_LEAVE_CANVAS(CalicoPacket p, Client client) 
	{
		long cuid = p.getLong();
		int clientid = client.getClientID();
		
		if(!CCanvasController.canvases.containsKey(cuid)) {
			return;
		}
		CCanvasController.canvases.get(cuid).removeClient(clientid);
		
		int[] clientIds = CCanvasController.canvases.get(cuid).getClients();
		CalicoPacket outPacket = CalicoPacket.getPacket(NetworkCommand.PRESENCE_CANVAS_USERS, cuid, clientIds.length);
		if(clientIds.length>0) {
			for(int j=0;j<clientIds.length;j++) {
				outPacket.putInt(clientIds[j]);
			}
		}
		ClientManager.send(outPacket);
		
	}
	public static void PRESENCE_CANVAS_RESET(CalicoPacket p, Client client) 
	{
		// not used
	}
	public static void PRESENCE_CANVAS_USERS(CalicoPacket p, Client client) 
	{
		// this is a request, but resent with the same packet
		
		long[] uuids = CCanvasController.canvases.keySet().toLongArray();
		if(uuids.length>0) {
			for(int i=0;i<uuids.length;i++) {
				int[] clientIds = CCanvasController.canvases.get(uuids[i]).getClients();
				CalicoPacket outPacket = CalicoPacket.getPacket(NetworkCommand.PRESENCE_CANVAS_USERS, uuids[i], clientIds.length);
				if(clientIds.length>0) {
					for(int j=0;j<clientIds.length;j++) {
						outPacket.putInt(clientIds[j]);
					}
				}
				ClientManager.send(client, outPacket);
			}
		}
	}
	
	public static void DEFAULT_EMAIL(CalicoPacket p, Client client)
	{
		p.rewind();
		p.getInt();
		
		COptions.server.default_email = p.getString();
		
		if (client != null)
			ClientManager.send_except(client, p);
	}


}// PROCESS QUEUE


