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
package calico.networking;

import it.unimi.dsi.Util;
import it.unimi.dsi.fastutil.longs.Long2ReferenceArrayMap;
import it.unimi.dsi.fastutil.longs.LongIterator;

import java.io.*;
import java.nio.*;
import java.net.*;
import java.util.*;

import calico.components.*;
import calico.components.arrow.AnchorPoint;
import calico.components.composable.Composable;
import calico.components.composable.ComposableElement;
import calico.components.composable.ComposableElementController;
import calico.components.grid.*;
import calico.components.menus.buttons.UndoButton;
import calico.controllers.*;
import calico.events.CalicoEventHandler;
import calico.inputhandlers.*;
import calico.modules.*;
import calico.networking.netstuff.*;
import calico.perspectives.CalicoPerspective;
import calico.perspectives.GridPerspective;
import calico.plugins.CalicoPluginManager;
import calico.plugins.events.CalicoEvent;
import calico.*;

import java.awt.*;
import java.awt.image.BufferedImage;

import java.util.concurrent.*;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;

import org.apache.log4j.Logger;


/**
 * This handles all the network packets when they are received.
 *
 * @author Mitch Dempsey
 */
public class PacketHandler
{

	public static Logger logger = Logger.getLogger(PacketHandler.class.getName());

	/**
	 * This takes a packet and decides what should be done with it
	 *
	 * @param packet this is the packet
	 */
	public static void receive(CalicoPacket packet)
	{
		
		
		int command = packet.getInt();

//		if (command != NetworkCommand.HEARTBEAT
//				&& command != NetworkCommand.CONSISTENCY_FAILED)
//			logger.debug("RX "+packet.toString());
		
		switch(command)
		{

			case NetworkCommand.STROKE_START:STROKE_START(packet);break;
			case NetworkCommand.STROKE_APPEND:STROKE_APPEND(packet);break;
			case NetworkCommand.STROKE_DELETE:STROKE_DELETE(packet);break;
			case NetworkCommand.STROKE_FINISH:STROKE_FINISH(packet);break;
			case NetworkCommand.STROKE_MOVE:STROKE_MOVE(packet);break;
			case NetworkCommand.STROKE_SET_COLOR:STROKE_SET_COLOR(packet);break;
			case NetworkCommand.STROKE_SET_PARENT:STROKE_SET_PARENT(packet);break;
			case NetworkCommand.STROKE_LOAD:STROKE_LOAD(packet);break;
			case NetworkCommand.STROKE_MAKE_SCRAP:STROKE_MAKE_SCRAP(packet);break;
			case NetworkCommand.STROKE_MAKE_SHRUNK_SCRAP:STROKE_MAKE_SHRUNK_SCRAP(packet);break;
			case NetworkCommand.STROKE_DELETE_AREA:STROKE_DELETE_AREA(packet);break;
			case NetworkCommand.STROKE_ROTATE:STROKE_ROTATE(packet);break;
			case NetworkCommand.STROKE_SCALE:STROKE_SCALE(packet);break;
			case NetworkCommand.STROKE_SET_AS_POINTER:STROKE_SET_AS_POINTER(packet);break;
			case NetworkCommand.STROKE_HIDE:STROKE_HIDE(packet);break;
			case NetworkCommand.STROKE_UNHIDE:STROKE_UNHIDE(packet);break;
			

			case NetworkCommand.STATUS_MESSAGE:STATUS_MESSAGE(packet);break;

			case NetworkCommand.GROUP_START:GROUP_START(packet);break;
			case NetworkCommand.GROUP_APPEND:GROUP_APPEND(packet);break;
			case NetworkCommand.GROUP_FINISH:GROUP_FINISH(packet);break;
			case NetworkCommand.GROUP_DELETE:GROUP_DELETE(packet);break;
			case NetworkCommand.GROUP_SET_PARENT:GROUP_SET_PARENT(packet);break;
			case NetworkCommand.GROUP_SET_CHILDREN:GROUP_SET_CHILDREN(packet);break;
			case NetworkCommand.GROUP_DROP:GROUP_DROP(packet);break;
			case NetworkCommand.GROUP_MOVE:GROUP_MOVE(packet);break;
			case NetworkCommand.GROUP_MOVE_END:GROUP_MOVE_END(packet);break;
			case NetworkCommand.GROUP_MOVE_START:GROUP_MOVE_START(packet);break;
			case NetworkCommand.GROUP_SET_PERM:GROUP_SET_PERM(packet);break;
			case NetworkCommand.GROUP_CHILDREN_COLOR:GROUP_CHILDREN_COLOR(packet);break;
//			case NetworkCommand.GROUP_RECTIFY:GROUP_RECTIFY(packet);break;
//			case NetworkCommand.GROUP_CIRCLIFY:GROUP_CIRCLIFY(packet);break;
			case NetworkCommand.GROUP_APPEND_CLUSTER:GROUP_APPEND_CLUSTER(packet);break;
			case NetworkCommand.GROUP_SET_CHILD_GROUPS:GROUP_SET_CHILD_GROUPS(packet);break;
			case NetworkCommand.GROUP_SET_CHILD_STROKES:GROUP_SET_CHILD_STROKES(packet);break;
			case NetworkCommand.GROUP_SET_CHILD_ARROWS:GROUP_SET_CHILD_ARROWS(packet);break;
			case NetworkCommand.GROUP_LOAD:GROUP_LOAD(packet);break;
			case NetworkCommand.GROUP_HASH_CHECK:GROUP_HASH_CHECK(packet);break;
			//case NetworkCommand.GROUP_RELOAD_START:GROUP_RELOAD_START(packet);break;
			//case NetworkCommand.GROUP_RELOAD_COORDS:GROUP_RELOAD_COORDS(packet);break;
			//case NetworkCommand.GROUP_RELOAD_FINISH:GROUP_RELOAD_FINISH(packet);break;
			//case NetworkCommand.GROUP_RELOAD_CHILDREN:GROUP_RELOAD_CHILDREN(packet);break;
			//case NetworkCommand.GROUP_RELOAD_POSITION:GROUP_RELOAD_POSITION(packet);break;
			//case NetworkCommand.GROUP_RELOAD_REMOVE:GROUP_RELOAD_REMOVE(packet);break;
			case NetworkCommand.GROUP_SET_TEXT:GROUP_SET_TEXT(packet);break;
//			case NetworkCommand.GROUP_SHRINK_TO_CONTENTS:GROUP_SHRINK_TO_CONTENTS(packet);break;
			case NetworkCommand.GROUP_IMAGE_LOAD:GROUP_IMAGE_LOAD(packet);break;
			case NetworkCommand.GROUP_ROTATE:GROUP_ROTATE(packet);break;
			case NetworkCommand.GROUP_SCALE:GROUP_SCALE(packet);break;
			case NetworkCommand.GROUP_CREATE_TEXT_GROUP:GROUP_CREATE_TEXT_GROUP(packet);break;
			case NetworkCommand.GROUP_MAKE_RECTANGLE:GROUP_MAKE_RECTANGLE(packet);break;
			case NetworkCommand.GROUP_COPY_WITH_MAPPINGS:GROUP_COPY_WITH_MAPPINGS(packet);break;
			
			case NetworkCommand.GRID_SIZE:GRID_SIZE(packet);break;
			case NetworkCommand.CONSISTENCY_FINISH:CONSISTENCY_FINISH(packet);break;
			case NetworkCommand.CONSISTENCY_FAILED:CONSISTENCY_FAILED(packet);break;
			case NetworkCommand.CONSISTENCY_DEBUG:CONSISTENCY_DEBUG(packet);break;
			case NetworkCommand.CONSISTENCY_RESYNCED:CONSISTENCY_RESYNCED(packet);break;
			case NetworkCommand.CONSISTENCY_CHECK:CONSISTENCY_CHECK(packet);break;
			
			case NetworkCommand.CANVAS_CREATE:CANVAS_CREATE(packet);break;
			case NetworkCommand.CANVAS_INFO:CANVAS_INFO(packet);break;
			case NetworkCommand.CANVAS_REDRAW:CANVAS_REDRAW(packet);break;
			case NetworkCommand.CANVAS_CLEAR:CANVAS_CLEAR(packet);break;
			//case NetworkCommand.CANVAS_RELOAD_FINISH:CANVAS_RELOAD_FINISH(packet);break;
			//case NetworkCommand.CANVAS_RELOAD_STROKES:CANVAS_RELOAD_STROKES(packet);break;
			//case NetworkCommand.CANVAS_RELOAD_GROUPS:CANVAS_RELOAD_GROUPS(packet);break;
			//case NetworkCommand.CANVAS_RELOAD_ARROWS:CANVAS_RELOAD_ARROWS(packet);break;
			case NetworkCommand.CANVAS_CLEAR_FOR_SC:CANVAS_CLEAR_FOR_SC(packet);break;
			case NetworkCommand.CANVAS_SC_FINISH:CANVAS_SC_FINISH(packet);break;
			case NetworkCommand.CANVAS_LOCK:CANVAS_LOCK(packet);break;
			case NetworkCommand.CANVAS_LOAD:CANVAS_LOAD(packet);break;
			case NetworkCommand.CANVAS_DELETE:CANVAS_DELETE(packet);break;
			case NetworkCommand.CANVAS_SET_DIMENSIONS:CANVAS_SET_DIMENSIONS(packet);break;
			case NetworkCommand.CANVAS_LOAD_PROGRESS:CANVAS_LOAD_PROGRESS(packet);break;

			case NetworkCommand.SESSION_INFO:SESSION_INFO(packet);break;
			case NetworkCommand.CHUNK_DATA:CHUNK_DATA(packet);break;
			

			case NetworkCommand.ARROW_CREATE:ARROW_CREATE(packet);break;
			case NetworkCommand.ARROW_DELETE:ARROW_DELETE(packet);break;
			case NetworkCommand.ARROW_SET_TYPE:ARROW_SET_TYPE(packet);break;
			case NetworkCommand.ARROW_SET_COLOR:ARROW_SET_COLOR(packet);break;
			
			case NetworkCommand.CONNECTOR_LOAD:CONNECTOR_LOAD(packet);break;
			case NetworkCommand.CONNECTOR_DELETE:CONNECTOR_DELETE(packet);break;
			case NetworkCommand.CONNECTOR_LINEARIZE:CONNECTOR_LINEARIZE(packet);break;
			case NetworkCommand.CONNECTOR_MOVE_ANCHOR:CONNECTOR_MOVE_ANCHOR(packet);break;
			case NetworkCommand.CONNECTOR_MOVE_ANCHOR_START:CONNECTOR_MOVE_ANCHOR_START(packet);break;
			case NetworkCommand.CONNECTOR_MOVE_ANCHOR_END:CONNECTOR_MOVE_ANCHOR_END(packet);break;
			
			case NetworkCommand.ELEMENT_ADD:ELEMENT_ADD(packet);break;
			case NetworkCommand.ELEMENT_REMOVE:ELEMENT_REMOVE(packet);break;
			
			case NetworkCommand.AUTH_OK:AUTH_OK(packet);break;
			case NetworkCommand.AUTH_FAIL:AUTH_FAIL(packet);break;
			
			case NetworkCommand.LEAVE:LEAVE(packet);break;
			
			case NetworkCommand.UUID_BLOCK:UUID_BLOCK(packet);break;

			case NetworkCommand.DEBUG_PACKETSIZE:DEBUG_PACKETSIZE(packet);break;
			
			case NetworkCommand.PLUGIN_EVENT:PLUGIN_EVENT(packet);break;
			
			case NetworkCommand.ACK:break;
			
			case NetworkCommand.CLIENT_INFO:CLIENT_INFO(packet);break;
			
			case 0:ErrorMessage.fatal("Lost connection to server!");break;
			
			case NetworkCommand.LIST_CREATE:LIST_CREATE(packet);break;
			case NetworkCommand.LIST_LOAD:LIST_LOAD(packet);break;
			case NetworkCommand.LIST_CHECK_SET:LIST_CHECK_SET(packet);break;
			case NetworkCommand.CANVASVIEW_SCRAP_LOAD:CANVASVIEW_SCRAP_LOAD(packet);break;
			
			case NetworkCommand.IMAGE_TRANSFER:IMAGE_TRANSFER(packet);break;

//			case NetworkCommand.PALETTE_PACKET:PALETTE_PACKET(packet);break;
			
			case NetworkCommand.PRESENCE_CANVAS_USERS:PRESENCE_CANVAS_USERS(packet);break;
			case NetworkCommand.DEFAULT_EMAIL:DEFAULT_EMAIL(packet);break;
			case NetworkCommand.SERVER_HTTP_PORT:SERVER_HTTP_PORT(packet);break;
			case NetworkCommand.SERVER_EMAIL_SETTINGS:SERVER_EMAIL_SETTINGS(packet);break;
			
		}//command switch
		
		CalicoEventHandler.getInstance().fireEvent(command, packet);

	}
	
	private static void CLIENT_INFO(CalicoPacket p){
		int clientid = p.getInt();
		String username = p.getString();
		
		CalicoDataStore.clientInfo.put(clientid, username);
		
	}
	
	private static void PRESENCE_CANVAS_USERS(CalicoPacket p) {
		long cuid = p.getLong();
		int count = p.getInt();
		
		if( CCanvasController.exists(cuid)) {
			int origNumClients = CCanvasController.canvasdb.get(cuid).getClients().length;
			CCanvasController.canvasdb.get(cuid).clearClients();
			
			if(count>0) {
				for(int i=0;i<count;i++) {
					CCanvasController.canvasdb.get(cuid).addClient(p.getInt());
				}
			}
			
//			CCanvasController.updateClientsLabel(CCanvasController.getCurrentUUID());

//				CCanvasController.redrawToolbar_clients(cuid);
			
			/* // GridRemoval: 
			if (CCanvasController.exists(cuid) && CalicoDataStore.gridObject != null &&
					origNumClients != CCanvasController.canvasdb.get(cuid).getClients().length
					&& 0 == CCanvasController.getCurrentUUID())
				CalicoDataStore.gridObject.updateCell(cuid);
			*/ 
			
			if (cuid == CCanvasController.getCurrentUUID())
				CCanvasController.redrawMenuBars();
			
		}
		
	}
	
	private static void SESSION_INFO(CalicoPacket p)
	{
		int sessionid = p.getInt();
		String name = p.getString();
		String host = p.getString();
		int port = p.getInt();
		
		if(sessionid==1) {
			CalicoDataStore.sessiondb = new ArrayList<CSession>();
		}
		CalicoDataStore.sessiondb.add(new CSession(name, host, port));
		
	}
	
	private static void CHUNK_DATA(CalicoPacket p)
	{
		p.rewind();
		p.getInt();
		
		int count = p.getInt();
		
		for (int i = 0; i < count; i++)
		{
			int length = p.getInt();
			byte[] bytes = p.getByteArray(length);
			CalicoPacket packet = new CalicoPacket(bytes);
			PacketHandler.receive(packet);
		}
		
	}
	
	
	
	private static void PLUGIN_EVENT(CalicoPacket p)
	{
		String eventname = p.getString();
		try
		{
			Class<?> pluginEvent = CalicoPluginManager.getEventClass(eventname);
			
			CalicoEvent eventObj = (CalicoEvent) pluginEvent.newInstance();
			eventObj.getPacketData(p);
			
			CalicoPluginManager.FireEvent(eventObj);
			
		}
		catch(Exception e)
		{
		}
	}
	
	private static void GROUP_HASH_CHECK(CalicoPacket p)
	{
		long uuid = p.getLong();
		int size = p.getCharInt();
		
		byte[] hash = p.getByteArray(size);
		
		CGroupController.verify_hash(uuid, hash);
		
	}
	
	private static void GROUP_SET_TEXT(CalicoPacket p)
	{
		long uuid = p.getLong();
		String text = p.getString();
		
		CGroupController.no_notify_set_text(uuid, text);
	}
	
//	private static void GROUP_SHRINK_TO_CONTENTS(CalicoPacket p)
//	{
//		long uuid = p.getLong();
//		
//		CGroupController.no_notify_shrink_to_contents(uuid);
//	}
	
//	private static void GROUP_ADD_IMAGE(CalicoPacket p)
//	{
//		long uuid = p.getLong();
//		long cuid = p.getLong();
//		long puid = p.getLong();
//		String url = p.getString();
//		int imgX = p.getInt();
//		int imgY = p.getInt();
//		int imgW = p.getInt();
//		int imgH = p.getInt();
//		boolean perm = p.getBoolean();
//		boolean captureChildren = p.getBoolean();
//		double rotation = p.getDouble();
//		double scaleX = p.getDouble();
//		double scaleY = p.getDouble();
//		
//		CGroupController.no_notify_create_image_group(uuid, cuid, puid, url, imgX, imgY, imgW, imgH);
//	}
	
	private static void GROUP_IMAGE_LOAD(CalicoPacket p)
	{
		//ClientManager.send( CalicoPacket.getPacket( NetworkCommand.GROUP_ADD_IMAGE, uuid, cuid, puid, imgURL, imageWidth, imageHeight) );
		p.rewind();
		p.getInt();
		long uuid = p.getLong();
		
		//This is pretty ugly. The proper way to handle it is to create a new packet. However this is the quick and dirty solution until we decide on exactly how to handle large images.
		if (uuid == 0l)
		{
			JOptionPane.showMessageDialog(CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getComponent(),
			"The maximum image size is 1920W x 1080H",
			"Image Error",
			JOptionPane.ERROR_MESSAGE);
			return;
		}
		long cuid = p.getLong();
		long puid = p.getLong();
		String url = p.getString();
		int port = p.getInt();
		String localPath = p.getString();
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
		
//		if (CGroupController.exists(uuid))
//			return;
		
		CGroupController.no_notify_create_image_group(uuid, cuid, puid, url, port, localPath, imgX, imgY, imgW, imgH);
		CGroupController.groupdb.get(uuid).primative_rotate(rotation);
		CGroupController.groupdb.get(uuid).primative_scale(scaleX, scaleY);
		
		
	}
	
	private static void GROUP_ROTATE(CalicoPacket p)
	{
		long guuid = p.getLong();
		double theta = p.getDouble();
		
		CGroupController.no_notify_rotate(guuid, theta);
	}
	
	private static void GROUP_SCALE(CalicoPacket p)
	{
		long guuid = p.getLong();
		double scaleX = p.getDouble();
		double scaleY = p.getDouble();
		
		CGroupController.no_notify_scale(guuid, scaleX, scaleY);
	}
	
	private static void GROUP_CREATE_TEXT_GROUP(CalicoPacket p)
	{
		long guuid = p.getLong();
		long cuuid = p.getLong();
		String text = p.getString();
		int x = p.getInt();
		int y = p.getInt();
		
		CGroupController.no_notify_create_text_scrap(guuid, cuuid, text, x, y);
	}
	
	public static void GROUP_MAKE_RECTANGLE(CalicoPacket p)
	{
		long guuid = p.getLong();
		int x = p.getInt();
		int y = p.getInt();
		int width = p.getInt();
		int height = p.getInt();
		
		CGroupController.no_notify_make_rectangle(guuid, x, y, width, height);
		
	}
	
	public static void GROUP_COPY_WITH_MAPPINGS(CalicoPacket p)
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
	}
	
	private static void GROUP_START(CalicoPacket p)
	{
		long uuid = p.getLong();
		long cuid = p.getLong();
		long puid = p.getLong();
		int ispermINT = p.getInt();
		boolean isperm = ispermINT==1 ? true : false;
		CGroupController.no_notify_start(uuid, cuid, puid, isperm);
	}
	private static void GROUP_FINISH(CalicoPacket p)
	{
		long uuid = p.getLong();
		boolean captureChildren = p.getBoolean();
		CGroupController.no_notify_finish(uuid, captureChildren);
	}
	private static void GROUP_APPEND(CalicoPacket p)
	{
		long uuid = p.getLong();
		int x = p.getInt();
		int y = p.getInt();
		
		CGroupController.no_notify_append(uuid, x, y);
	}
	private static void GROUP_DELETE(CalicoPacket p)
	{
		long uuid = p.getLong();
		CGroupController.no_notify_delete(uuid);
	}
	private static void GROUP_MOVE(CalicoPacket p)
	{
		long uuid = p.getLong();
		int x = p.getInt();
		int y = p.getInt();
		CGroupController.no_notify_move(uuid, x, y);
	}
	
	public static void GROUP_MOVE_START(CalicoPacket p)
	{
		long uuid = p.getLong();

		CGroupController.no_notify_move_start(uuid);
	}
	
	public static void GROUP_MOVE_END(CalicoPacket p)
	{
		long uuid = p.getLong();
		int x = p.getInt();
		int y = p.getInt();
		CGroupController.no_notify_move_end(uuid, x, y);
	}
	
	private static void GROUP_DROP(CalicoPacket p)
	{
		long uuid = p.getLong();
		CGroupController.no_notify_drop(uuid);
	}
	private static void GROUP_SET_PARENT(CalicoPacket p)
	{
		long uuid = p.getLong();
		long newParent = p.getLong();
		CGroupController.no_notify_set_parent(uuid, newParent);
	}
	private static void GROUP_APPEND_CLUSTER(CalicoPacket p)
	{
		long uuid = p.getLong();
		int count = p.getCharInt();
		
		for(int i=0;i<count;i++)
		{
			int x = p.getInt();
			int y = p.getInt();
			CGroupController.no_notify_append(uuid, x, y);
		}
	}
	private static void GROUP_SET_CHILDREN(CalicoPacket p)
	{
		long uuid = p.getLong();
		int numBGEs = p.getInt();
		int numGrps = p.getInt();
		
		
		long[] grplist = null;
		long[] bgelist = null;
		
		if(numBGEs>0)
		{
			bgelist = new long[numBGEs];
			for(int i=0;i<numBGEs;i++)
			{
				bgelist[i] = p.getLong();
			}
		}
		
		
		if(numGrps>0)
		{
			grplist = new long[numGrps];
			for(int i=0;i<numGrps;i++)
			{
				grplist[i] = p.getLong();
			}
		}
		
		
		// Now loop thru it.
		CGroupController.no_notify_set_children(uuid, bgelist, grplist);
		
	}
	private static void GROUP_SET_PERM(CalicoPacket p)
	{
		long uuid = p.getLong();
		int isPerm = p.getInt();
		
		CGroupController.no_notify_set_permanent(uuid, (isPerm==1));
		
	}
	private static void GROUP_CHILDREN_COLOR(CalicoPacket p)
	{
		long uuid = p.getLong();
		int red = p.getInt();
		int green = p.getInt();
		int blue = p.getInt();
		
		CGroupController.no_notify_set_children_color(uuid, new Color(red,green,blue));
	}
	
//	private static void GROUP_RECTIFY(CalicoPacket p)
//	{
//		long uuid = p.getLong();
//		CGroupController.no_notify_rectify(uuid);
//	}
//	
//	private static void GROUP_CIRCLIFY(CalicoPacket p)
//	{
//		long uuid = p.getLong();
//		CGroupController.no_notify_circlify(uuid);
//	}
	
	private static void GROUP_SET_CHILD_STROKES(CalicoPacket p)
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
	private static void GROUP_SET_CHILD_ARROWS(CalicoPacket p)
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
	
	private static void GROUP_SET_CHILD_GROUPS(CalicoPacket p)
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

	private static void GROUP_LOAD(CalicoPacket p)
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
		
		CGroupController.no_notify_start(uuid, cuid, puid, isperm);
		int[] xArr = new int[count], yArr = new int[count];
		for(int i=0;i<count;i++)
		{
			xArr[i] = p.getInt();
			yArr[i] = p.getInt();
//			CGroupController.no_notify_append(uuid, x, y);
		}
		CGroupController.no_notify_append(uuid, xArr, yArr);
		boolean captureChildren = p.getBoolean();
		double rotation = p.getDouble();
		double scaleX = p.getDouble();
		double scaleY = p.getDouble();
		String text = p.getString();
		
		
//		CGroupController.groupdb.get(uuid).finish();
		CGroupController.groupdb.get(uuid).primative_rotate(rotation);
		CGroupController.groupdb.get(uuid).primative_scale(scaleX, scaleY);
		CGroupController.groupdb.get(uuid).setText(text);
		
		CGroupController.no_notify_finish(uuid, captureChildren, false, false);
		
//		if (captureChildren)
//			CGroupController.no_notify_calculate_parenting(uuid, true);
//		CGroupController.no_notify_finish(uuid, captureChildren);
	}
	
	private static void CANVASVIEW_SCRAP_LOAD(CalicoPacket p)
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

	private static void CANVAS_CREATE(CalicoPacket p)
	{
	}
	
	private static void CANVAS_INFO(CalicoPacket p)
	{
		// UUID "COORDS" XPOSongrid YPOSongrid
		long uuid = p.getLong();
		int index = p.getInt();
		
		CCanvas canvas = new CCanvas(uuid, index);
		CCanvasController.canvasdb.put(uuid, canvas);
//		CCanvasController.canvasdb.get(uuid).drawMenuBars();
		
		if (!CalicoDataStore.initialScreenDisplayed)
		{
			CalicoDataStore.initialScreenDisplayed = true;
			CalicoPerspective.Registry.activateNavigationPerspective();
			
			Networking.consistency_check();
		}

		CCanvasController.Factory.getInstance().canvasCreated(canvas);
	}
	
	
	private static void UUID_BLOCK(CalicoPacket p)
	{
		//uuidlist
		int count = p.getInt();
		for(int i=0;i<count;i++)
		{
			Calico.uuidlist.add( p.getLong() );
		}
		Calico.isAllocating = false;
	}
	
	private static void AUTH_OK(CalicoPacket p)
	{
		if (Networking.connectionState == Networking.ConnectionState.Connecting)
		{
			Networking.connectionState = Networking.ConnectionState.Connected;
			CCanvasController.redrawMenuBars(CCanvasController.getCurrentUUID());
		}
		else if (Networking.connectionState != Networking.ConnectionState.Connected)
		{
			Networking.send(CalicoPacket.getPacket(NetworkCommand.UDP_CHALLENGE, Networking.udpChallenge ));
			// SUCCESS

			if (Calico.uuidlist.isEmpty())
			{
    			Networking.send(CalicoPacket.command(NetworkCommand.UUID_GET_BLOCK));
    			Networking.send(CalicoPacket.command(NetworkCommand.UUID_GET_BLOCK));
			}

			// Now we request a consistency check
			Networking.send(CalicoPacket.command(NetworkCommand.CANVAS_LIST));
			Networking.connectionState = Networking.ConnectionState.Connecting;
		}		
		
		
//		if (CalicoDataStore.GridRows == 0 && CalicoDataStore.GridCols == 0
//			&& Networking.connectionState != Networking.ConnectionState.Connected)
//		{
//			Networking.send(CalicoPacket.getPacket(NetworkCommand.UDP_CHALLENGE, Networking.udpChallenge ));
//			// SUCCESS
//			Networking.grid_size();
//		}
//		else if (Networking.connectionState != Networking.ConnectionState.Connected)
//		{
//			Networking.connectionState = Networking.ConnectionState.Connected;
//			CCanvasController.redrawMenuBars(CCanvasController.getCurrentUUID());
//		}
	}
	private static void AUTH_FAIL(CalicoPacket p)
	{
		// Error Message
		ErrorMessage.fatal("Authentication Failure");
	}
	
	private static void LEAVE(CalicoPacket p)
	{
		String msg = p.getString();
		Networking.receivePacketThread.interrupt();
		Networking.sendPacketThread.interrupt();
		
		// Error Message
		ErrorMessage.fatal("Kicked by server: " + msg);
	}
	

	private static void CONSISTENCY_FINISH(CalicoPacket p)
	{
		Networking.send(CalicoPacket.getPacket(NetworkCommand.PRESENCE_VIEW_CANVAS, 1l));
		Networking.send(CalicoPacket.getPacket(NetworkCommand.PRESENCE_LEAVE_CANVAS, 1l));
		Networking.send(NetworkCommand.PRESENCE_CANVAS_USERS);
		
		Networking.udpSend(CalicoPacket.getPacket(NetworkCommand.UDP_CHALLENGE, Networking.udpChallenge ));


	}
	
	private static void CONSISTENCY_FAILED(CalicoPacket p)
	{
		if (!Networking.ignoreConsistencyCheck)
		{
			Networking.timesFailed++;
			System.out.println("Consistency Failed " + Networking.timesFailed + " times.");
			
			if (Networking.timesFailed == 1)
			{
				Networking.synchroized = false;
				CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).drawMenuBars();
			}
			
			//Resync automatically if 3 or more consistency failed packets have arrived in a row.
			//A resync cannot occur more than twice in 30 seconds
			if (Networking.timesFailed >= 3 && (System.currentTimeMillis() - Networking.lastResync > 20000))
			{
				System.out.println("Resynching with server");
				Networking.lastResync = System.currentTimeMillis();
				Networking.send(NetworkCommand.CONSISTENCY_RESYNC_CANVAS, CCanvasController.getCurrentUUID());
			}
		}
	}
	
	private static void CONSISTENCY_RESYNCED(CalicoPacket p)
	{
		Networking.timesFailed = 0;
		Networking.synchroized = true;
		CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).drawMenuBars();
	}
	
	private static void CONSISTENCY_CHECK(CalicoPacket p)
	{
		if( GridPerspective.getInstance().isActive() && Calico.isGridLoading)
		{
			Calico cal = CalicoDataStore.calicoObj;
			
			cal.getContentPane().removeAll();
			cal.getContentPane().add( CGrid.getInstance().getComponent() );
			CGrid.getInstance().refreshCells();
	        cal.pack();
	        cal.setVisible(true);
			cal.repaint();
		}	
	}
	

	
	private static void CONSISTENCY_DEBUG(CalicoPacket p)
	{
		boolean foundConflict = false;
		while (p.remaining() > 0)
		{
			long uuid = p.getLong();
			int sig = p.getInt();
			String debugMsg = p.getString();
			
			boolean conflict = false;
			int local_sig = 0;
			String local_debugMsg = "";
			
			if (CGroupController.exists(uuid))
			{
				if (CGroupController.get_signature(uuid) != sig)
				{
					conflict = true;
					local_sig = CGroupController.get_signature(uuid);
					local_debugMsg = CGroupController.get_signature_debug_output(uuid);
				}
			}
			else if (CStrokeController.exists(uuid))
			{
				if (CStrokeController.get_signature(uuid) != sig)
				{
					conflict = true;
					local_sig = CStrokeController.get_signature(uuid);
					local_debugMsg = CStrokeController.get_signature_debug_output(uuid);
				}
			}
			else if (CConnectorController.exists(uuid))
			{
				if (CConnectorController.get_signature(uuid) != sig)
				{
					conflict = true;
					local_sig = CConnectorController.get_signature(uuid);
					local_debugMsg = CConnectorController.get_signature_debug_output(uuid);
				}
			}			
			
			if (conflict)
			{
				foundConflict = true;
				System.out.println("[!=== FOUND MISMATCH ===!] Server: " + sig + ", Client: " + local_sig);
				System.out.println("SERVER: " + debugMsg);
				System.out.println("CLIENT: " + local_debugMsg);
			}
			
			conflict = false;
		}
		
		if (Networking.synchroized == true && foundConflict == false)
		{
			Networking.synchroized = true;
			CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).drawMenuBars();
		}
		
	}
	
	private static void CANVAS_REDRAW(CalicoPacket p)
	{
		//p.getByte();
		//long uid = p.getLong();

	//	Logger.logc("CANVAS_REDRAW", uid);

		//CCanvasController.render(uid);
		
		
		//test 
		if( GridPerspective.getInstance().isActive() )
		{
			Calico cal = CalicoDataStore.calicoObj;

			cal.getContentPane().removeAll();
			cal.getContentPane().add( CGrid.getInstance().getComponent() );
			CGrid.getInstance().refreshCells();
	        cal.pack();
	        cal.setVisible(true);
			cal.repaint();
		}
	}
	
	private static void CANVAS_CLEAR(CalicoPacket p)
	{
		long uuid = p.getLong();
		
		CCanvasController.no_notify_clear(uuid);
		
	}
	
	private static void CANVAS_CLEAR_FOR_SC(CalicoPacket p)
	{
		long uuid = p.getLong();
		
		// We must clear it out
		CCanvasController.no_notify_clear_for_state_change(uuid);
	}
	
	private static void CANVAS_SC_FINISH(CalicoPacket p)
	{
		long uuid = p.getLong();
		
		CCanvasController.no_notify_state_change_complete(uuid);
		

	}

	//@Deprecated
	private static void GRID_SIZE(CalicoPacket p)
	{ 
		CGrid.GridRows = p.getInt();
		CGrid.GridCols = p.getInt();
		
//		Networking.send(CalicoPacket.command(NetworkCommand.UUID_GET_BLOCK));
//		Networking.send(CalicoPacket.command(NetworkCommand.UUID_GET_BLOCK));
//		
//
//		// Now we request a consistency check
//		Networking.send(CalicoPacket.command(NetworkCommand.CANVAS_LIST));
//		Networking.connectionState = Networking.ConnectionState.Connecting;
	}

	private static void STATUS_MESSAGE(CalicoPacket p)
	{
		String msg = p.getString();
		MessageObject.showNotice(msg);
		
		//This is required because when there are no undo steps left, this event would never be fired and the progress monitor would linger.
		CalicoEventHandler.getInstance().fireEvent(NetworkCommand.STATUS_SENDING_LARGE_FILE_FINISHED, CalicoPacket.getPacket(NetworkCommand.STATUS_SENDING_LARGE_FILE, 0.1d, 1d, ""));
	}

	
	
	private static void ARROW_CREATE(CalicoPacket p)
	{
		//UUID CANVASUID ARROW_TYPE ANCHOR_A_TYPE ANCHOR_A_UUID ANCHOR_A_X ANCHOR_A_Y   ANCHOR_B_TYPE ANCHOR_B_UUID ANCHOR_B_X ANCHOR_B_Y
		long uid = p.getLong();
		long cuid = p.getLong();
		int arrowType = p.getInt();


		Color color = p.getColor();
		
		int aType = p.getInt();
		long aUUID = p.getLong();
		int ax = p.getInt();
		int ay = p.getInt();
		
		int bType = p.getInt();
		long bUUID = p.getLong();
		int bx = p.getInt();
		int by = p.getInt();
		

		//long uuid,long canvasuid,int type,   int anchorAType, long anchorAUUID, Point anchorAPoint, int anchorBType, long anchorBUUID, Point anchorBPoint 
		
		CArrowController.no_notify_start(uid, cuid, color, arrowType, 
			new AnchorPoint(aType, new Point(ax,ay),aUUID),
			new AnchorPoint(bType, new Point(bx,by),bUUID)
		);
		
	}
	private static void ARROW_DELETE(CalicoPacket p)
	{
		long u = p.getLong();

		CArrowController.no_notify_delete(u);

	}
	private static void ARROW_SET_TYPE(CalicoPacket p)
	{
		//long u = p.getLong();

		//int type = p.getInt();
		

		//CArrowController.no_notify_type(u, type);

	}
	private static void ARROW_SET_COLOR(CalicoPacket p)
	{
		//long u = p.getLong();
		//int r = p.getInt();
		//int g = p.getInt();
		//int b = p.getInt();

	//	CArrowController.no_notify_color(u, r, g, b);
	}
	
	
	public static void CONNECTOR_LOAD(CalicoPacket p)
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
		
	}
	
	public static void CONNECTOR_DELETE(CalicoPacket p)
	{
		long uuid = p.getLong();
		CConnectorController.no_notify_delete(uuid);
	}
	
	public static void CONNECTOR_LINEARIZE(CalicoPacket p)
	{
		long uuid = p.getLong();
		CConnectorController.no_notify_linearize(uuid);
	}
	
	public static void CONNECTOR_MOVE_ANCHOR(CalicoPacket p)
	{
		long uuid = p.getLong();
		int type = p.getInt();
		int x = p.getInt();
		int y = p.getInt();
		
		CConnectorController.no_notify_move_group_anchor(uuid, type, x, y);

	}
	
	public static void CONNECTOR_MOVE_ANCHOR_START(CalicoPacket p)
	{
		long uuid = p.getLong();
		int type = p.getInt();
		
		CConnectorController.no_notify_move_group_anchor_start(uuid, type);
	}
	
	public static void CONNECTOR_MOVE_ANCHOR_END(CalicoPacket p)
	{
		long uuid = p.getLong();
		int type = p.getInt();
		
		CConnectorController.no_notify_move_group_anchor_end(uuid, type);

	}
	
	public static void ELEMENT_ADD(CalicoPacket p)
	{
		ComposableElement element = ComposableElementController.getElementFromPacket(p);
	
		if (element != null)
		{
			ComposableElementController.no_notify_addElement(element);
		}
	}
	
	public static void ELEMENT_REMOVE(CalicoPacket p)
	{
		long euuid = p.getLong();
		long cuuid = p.getLong();
		
		ComposableElementController.no_notify_removeElement(euuid, cuuid);
	}
	
	/*
	private static void STROKE_RELOAD_REMOVE(CalicoPacket p)
	{
		long uuid = p.getLong();
		CStrokeController.no_notify_reload_remove(uuid);
	}
	private static void STROKE_RELOAD_POSITION(CalicoPacket p)
	{
		long uuid = p.getLong();
		//BGElementController.no_notify_OLD_reload_finish(uuid);
	}
	private static void STROKE_RELOAD_START(CalicoPacket p)
	{
		long uuid = p.getLong();
		long cuid = p.getLong();
		long puid = p.getLong();

		int r = p.getInt();
		int g = p.getInt();
		int b = p.getInt();
		
		CStrokeController.no_notify_reload_start(uuid, cuid, puid, new Color(r,g,b));
	}
	private static void STROKE_RELOAD_FINISH(CalicoPacket p)
	{
		long uuid = p.getLong();
		CStrokeController.no_notify_reload_finish(uuid);
	}
	private static void STROKE_RELOAD_COORDS(CalicoPacket p)
	{
		long uuid = p.getLong();
		int pointcount = p.getInt();
		int[] xp = new int[pointcount];
		int[] yp = new int[pointcount];
		
		for(int i=0;i<pointcount;i++)
		{
			xp[i] = p.getInt();
			yp[i] = p.getInt();
		}
		
		CStrokeController.no_notify_reload_coords(uuid, xp, yp);
	}
	*/

	
	private static void STROKE_START(CalicoPacket p)
	{
		long uuid = p.getLong();
		long cuid = p.getLong();
		long puid = p.getLong();
		int red = p.getInt();
		int green = p.getInt();
		int blue = p.getInt();
		float thickness = p.getFloat();
		
		CStrokeController.no_notify_start(uuid,cuid,puid,red,green,blue,thickness);
	}
	private static void STROKE_APPEND(CalicoPacket p)
	{
		long uuid = p.getLong();
		int numpoints = p.getCharInt();

		if(numpoints==1)
		{
			int xp = p.getInt();
			int yp = p.getInt();
			CStrokeController.no_notify_append(uuid, xp, yp);
		}
		else if(numpoints>1)
		{
			int x[] = new int[numpoints];
			int y[] = new int[numpoints];
			
			for(int i=0;i<numpoints;i++)
			{
				x[i] = p.getInt();
				y[i] = p.getInt();
			}
			CStrokeController.no_notify_batch_append(uuid, x, y);
		}
		
		
	}
	private static void STROKE_FINISH(CalicoPacket p)
	{
		long uuid = p.getLong();
		
		CStrokeController.no_notify_finish(uuid);
	}
	private static void STROKE_SET_COLOR(CalicoPacket p)
	{
		long uuid = p.getLong();
		int red = p.getInt();
		int green = p.getInt();
		int blue = p.getInt();
		CStrokeController.no_notify_set_color(uuid, red, green, blue);
	}
	private static void STROKE_SET_PARENT(CalicoPacket p)
	{
		long uuid = p.getLong();
		long puid = p.getLong();
		CStrokeController.no_notify_set_parent(uuid, puid);
	}
	
	//TODO: remove this command
	private static void STROKE_MOVE(CalicoPacket p)
	{
		long uuid = p.getLong();
		int x = p.getInt();
		int y = p.getInt();

//		CStrokeController.no_notify_move(uuid, x, y);
	}
	private static void STROKE_DELETE(CalicoPacket p)
	{
		long uuid = p.getLong();
		CStrokeController.no_notify_delete(uuid);
	}
	
	private static void STROKE_LOAD(CalicoPacket p)
	{
		long time = System.currentTimeMillis();
		long newTime = 0;
		long uuid = p.getLong();
		long cuid = p.getLong();
		long puid = p.getLong();
		
		Color color = p.getColor();
		
		float thickness = p.getFloat();
		

		int numpoints = p.getCharInt();
		
		if(numpoints==0)
		{
			return;
		}

		CStrokeController.no_notify_start(uuid, cuid, puid, color, thickness);
		
//		newTime = System.currentTimeMillis(); System.out.println("~~~ P1 Delta time: " + (newTime - time)); newTime = time;
		
		int[] x = new int[numpoints];
		int[] y = new int[numpoints];
		
		for(int i=0;i<numpoints;i++)
		{
			x[i] = p.getInt();
			y[i] = p.getInt();
		}
		CStrokeController.no_notify_batch_append(uuid, x, y);
		
//		newTime = System.currentTimeMillis(); System.out.println("~~~ P2 Delta time: " + (newTime - time)); newTime = time;
		
		double rotation = p.getDouble();
		double scaleX = p.getDouble();
		double scaleY = p.getDouble();
		
		CStrokeController.strokes.get(uuid).primative_rotate(rotation);
		CStrokeController.strokes.get(uuid).primative_scale(scaleX, scaleY);
		
		CStrokeController.strokes.get(uuid).finish();
		
//		CStrokeController.no_notify_finish(uuid);
		
//		newTime = System.currentTimeMillis(); System.out.println("~~~ P3 Delta time: " + (newTime - time)); newTime = time;
	}
	
	private static void STROKE_MAKE_SCRAP(CalicoPacket p)
	{
		long suuid = p.getLong();
		long new_guuid = p.getLong();
		
		CStrokeController.no_notify_makeScrap(suuid, new_guuid);
	}
	
	private static void STROKE_MAKE_SHRUNK_SCRAP(CalicoPacket p)
	{
		long suuid = p.getLong();
		long new_guuid = p.getLong();
		
		CStrokeController.no_notify_makeShrunkScrap(suuid, new_guuid);
	}
	
	private static void STROKE_DELETE_AREA(CalicoPacket p)
	{
		long suuid = p.getLong();
		long temp_guuid = p.getLong();
		
		CStrokeController.no_notify_deleteArea(suuid, temp_guuid);
	}
	public static void STROKE_ROTATE(CalicoPacket p)
	{
		long uuid = p.getLong();
		double r = p.getDouble();

		if (CStrokeController.strokes.containsKey(uuid))
		{
			CStrokeController.strokes.get(uuid).primative_rotate(r);
		}
	}
	
	public static void STROKE_SCALE(CalicoPacket p)
	{
		long uuid = p.getLong();
		double sX = p.getDouble();
		double sY = p.getDouble();

		if (CStrokeController.strokes.containsKey(uuid))
		{
			CStrokeController.strokes.get(uuid).primative_scale(sX, sY);
		}
	}
	
	public static void STROKE_SET_AS_POINTER(CalicoPacket p)
	{
		long uuid = p.getLong();
		
		if (CStrokeController.strokes.containsKey(uuid))
		{
			CStrokeController.no_notify_set_stroke_as_pointer(uuid);
		}
	}
	
	public static void STROKE_HIDE(CalicoPacket p)
	{
		long uuid = p.getLong();
		boolean delete = p.getBoolean();
		
		CStrokeController.no_notify_hide_stroke(uuid, delete);
	}
	
	public static void STROKE_UNHIDE(CalicoPacket p)
	{
		long uuid = p.getLong();
		
		CStrokeController.no_notify_unhide_stroke(uuid);
	}

	private static void DEBUG_PACKETSIZE(CalicoPacket p)
	{
		int numlongs = p.getInt();
		long[] temp = new long[numlongs];
		
		for(int i=0;i<numlongs;i++)
		{
			temp[i] = p.getLong();
		}
		Calico.logger.debug("PACKETSIZE OK: "+numlongs+" longs loaded ("+Util.formatSize(ByteUtils.SIZE_OF_LONG*numlongs)+")");
	}
	
	public static void LIST_CREATE(CalicoPacket p)
	{
		long guuid = p.getLong();
		long luuid = p.getLong();
		
		CGroupDecoratorController.no_notify_list_create(guuid, luuid);
	}
	
	public static void LIST_LOAD(CalicoPacket p)
	{
		long luuid = p.getLong();
		long cuuid = p.getLong();
		long puuid = p.getLong();
		long guuid = p.getLong();
		
		CGroupDecoratorController.no_notify_list_load(guuid, luuid, cuuid, puuid);
	}
	
	public static void LIST_CHECK_SET(CalicoPacket p)
	{
		long luuid = p.getLong();
		long cuid = p.getLong();
		long puid = p.getLong();
		long guuid = p.getLong();
		boolean value = p.getBoolean();
		
		CGroupDecoratorController.no_notify_list_set_check(luuid, cuid, puid, guuid, value);
	}
	
	public static void IMAGE_TRANSFER(CalicoPacket p)
	{
		p.rewind();
		p.getInt();
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
			Image image = null;
			try
			{
				image = ImageIO.read(new File(CImageController.getImagePath(uuid)));
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
//			Image image = Toolkit.getDefaultToolkit().createImage(CImageController.getImagePath(uuid));
			CGroupController.no_notify_create_image_group(uuid, cuuid, 0l, "", 0, "", x, y, image.getWidth(null), image.getHeight(null));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void IMAGE_TRANSFER_FILE(CalicoPacket p)
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
	
//	public static void PALETTE_PACKET(CalicoPacket p)
//	{
//		long paletteItemUUID = p.getLong();
//		long paletteUUID = p.getLong();
//		Image img = p.getBufferedImage();
//		
//		PalettePlugin.no_notify_addPaletteItemToPalette(paletteUUID, p);
//		
//	}
	
	public static void CANVAS_LOCK(CalicoPacket p)
	{
		long canvas = p.getLong();
		boolean lock = p.getBoolean();
		String lockedBy = p.getString();
		long time = p.getLong();
		
		CCanvasController.no_notify_lock_canvas(canvas, lock, lockedBy, time);
	}
	
	public static void CANVAS_LOAD(CalicoPacket p)
	{
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
			if(comm!=NetworkCommand.CANVAS_INFO
					&& comm >= 200)
			{
				packets[i].rewind();
				receive(packets[i]);
			}
		}

		//Remove temp scraps after undo/redo
		long[] guuid = CCanvasController.canvasdb.get(cuuid).getChildGroups();
		for (int i = 0; i < guuid.length; i++)
		{
			if (!CGroupController.groupdb.get(guuid[i]).isPermanent())
			{
				CGroupController.drop(guuid[i]);
			}
		}

		CCanvasController.no_notify_state_change_complete(cuuid);
//		UndoButton.progressMonitor.close();
//		UndoButton.progressMonitor = null;
//		CalicoEventHandler.getInstance().fireEvent(NetworkCommand.STATUS_SENDING_LARGE_FILE_FINISHED, CalicoPacket.getPacket(NetworkCommand.STATUS_SENDING_LARGE_FILE_FINISHED, 1, 1, UndoButton.undoMessage));
	}
	
	public static void CANVAS_DELETE(CalicoPacket p)
	{
		p.rewind();
		p.getInt();
		long canvasId = p.getLong();
		
		CCanvasController.no_notify_clear(canvasId);
//		CCanvasController.removeCanvas(canvasId);
	}
	
	public static void CANVAS_SET_DIMENSIONS(CalicoPacket p)
	{
		p.rewind();
		p.getInt();
		int width = p.getInt();
		int height = p.getInt();
		
		CalicoDataStore.serverScreenWidth = width;
		CalicoDataStore.serverScreenHeight = height;
	}
	
	
	public static void CANVAS_LOAD_PROGRESS(CalicoPacket p)
	{
		p.rewind();
		p.getInt();
//		long num = p.getLong();
//		long total = p.getLong();
		
	}
	
	public static void DEFAULT_EMAIL(CalicoPacket p)
	{
		p.rewind();
		p.getInt();
		
		CalicoDataStore.default_email = p.getString();
	}
	
	public static void SERVER_HTTP_PORT(CalicoPacket p)
	{
		p.rewind();
		p.getInt();
		
		CalicoDataStore.ServerHTTPPort = p.getInt();
	}
	
	public static void SERVER_EMAIL_SETTINGS(CalicoPacket p)
	{
		p.rewind();
		p.getInt();
		
		CalicoDataStore.email.smtpHost = p.getString();
		CalicoDataStore.email.smtpPort = p.getInt();
		CalicoDataStore.email.smtpsAuth = p.getString();
		CalicoDataStore.email.replyToEmail = p.getString();
		CalicoDataStore.email.username = p.getString();
		CalicoDataStore.email.password = p.getString();

	}
}
