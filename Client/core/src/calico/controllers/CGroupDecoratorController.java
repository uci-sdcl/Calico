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

import java.awt.Rectangle;
import java.lang.reflect.Field;
import java.util.ArrayList;

import it.unimi.dsi.fastutil.longs.Long2ReferenceArrayMap;
import calico.CalicoDraw;
import calico.CalicoOptions;
import calico.components.CGroup;
import calico.components.decorators.CListDecorator;
import calico.inputhandlers.CalicoInputManager;
import calico.networking.Networking;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;
import edu.umd.cs.piccolo.nodes.PImage;

public class CGroupDecoratorController {
	
	public static Long2ReferenceArrayMap<Boolean> groupCheckValues = new Long2ReferenceArrayMap<Boolean>();
	public static ArrayList<Integer> groupDecoratorCommands = new ArrayList<Integer>();
	
//	public static Long2ReferenceArrayMap<PImage> groupImages = new Long2ReferenceArrayMap<PImage>();

	private static void no_notify_decorator_create(long guuid, long uuid, CGroup groupToCreate) {
		long cuid = CGroupController.groupdb.get(guuid).getCanvasUID();
		no_notify_decorator_load(uuid, cuid, groupToCreate);
	}

	private static void no_notify_decorator_load(long uuid, long cuuid, CGroup groupToLoad) {
		if(CGroupController.exists(uuid))
		{
			CGroupController.logger.debug("Need to delete group "+uuid);
			// WHOAA WE NEED TO DELETE THIS SHIT
			//CCanvasController.canvasdb.get(cuuid).getLayer().removeChild(CGroupController.groupdb.get(uuid));
			CalicoDraw.removeChildFromNode(CCanvasController.canvasdb.get(cuuid).getLayer(), CGroupController.groupdb.get(uuid));
//			CCanvasController.canvasdb.get(cuuid).getCamera().repaint();
		}
		
		// Add to the GroupDB
		CGroupController.groupdb.put(uuid, groupToLoad);
		
		CCanvasController.canvasdb.get(cuuid).addChildGroup(groupToLoad.getUUID());
		
		//CCanvasController.canvasdb.get(cuuid).getLayer().addChild(CGroupController.groupdb.get(groupToLoad.getUUID()));
		CalicoDraw.addChildToNode(CCanvasController.canvasdb.get(cuuid).getLayer(), CGroupController.groupdb.get(groupToLoad.getUUID()));
		
		CalicoInputManager.addGroupInputHandler(uuid);
		
		CGroupController.groupdb.get(uuid).drawPermTemp(true);
		
		CGroupController.groupdb.get(uuid).resetViewOrder();
	}
	
	public static void list_create(long guuid, long uuid)
	{
		no_notify_list_create(guuid, uuid);
		Networking.send(CalicoPacket.getPacket(NetworkCommand.LIST_CREATE, guuid, uuid));

	}
	
	public static void no_notify_list_create(long guuid, long uuid)
	{
		if (!CGroupController.groupdb.get(guuid).isPermanent())
			CGroupController.no_notify_set_permanent(guuid, true);
		
		long[] cstrokes = CGroupController.groupdb.get(guuid).getChildStrokes(); 
		if (cstrokes.length > 0)
		{
			for (int i = cstrokes.length - 1; i >= 0; i--)
			{
				CGroupController.groupdb.get(guuid).deleteChildStroke(cstrokes[i]);
				CStrokeController.strokes.get(cstrokes[i]).setParentUUID(0);
			}
		}
		
		Rectangle bounds = CGroupController.groupdb.get(guuid).getBoundsOfContents();
		CGroupController.no_notify_make_rectangle(guuid, bounds.x, bounds.y, bounds.width, bounds.height);
		CGroup groupToCreate = new CListDecorator(guuid, uuid);
		no_notify_decorator_create(guuid, uuid, groupToCreate);
		((CListDecorator)CGroupController.groupdb.get(uuid)).resetListElementPositions(true);
		groupToCreate.recomputeBounds();
		
		if (cstrokes.length > 0)
		{
			for (int i = cstrokes.length - 1; i >= 0; i--)
			{
				CStrokeController.strokes.get(cstrokes[i]).calculateParent();
			}
		}
		
		CGroupController.groupdb.get(uuid).resetViewOrder();
	}
	
	public static void no_notify_list_load(long guuid, long uuid, long cuuid, long puuid)
	{
		CGroup groupToLoad =  new CListDecorator(guuid, uuid, cuuid, puuid);
		no_notify_decorator_load(uuid, cuuid, groupToLoad);
	}
	
	public static void list_set_check(long luuid, long cuid, long puid, long guuid, boolean value)
	{
		no_notify_list_set_check(luuid, cuid, puid, guuid, value);
		Networking.send(CalicoPacket.getPacket(NetworkCommand.LIST_CHECK_SET, luuid, cuid, puid, guuid, value));
	}
	
	public static void no_notify_list_set_check(long luuid, long cuid, long puid, long guuid, boolean value)
	{
		if (!CGroupController.exists(luuid)) { return; }
		
		CListDecorator list = (CListDecorator)CGroupController.groupdb.get(luuid);
		list.setCheck(guuid, value);
		list.recomputeValues();
	}
	
	public static void registerGroupDecoratorCommands(String type)
	{
		Class<?> rootClass = NetworkCommand.class;
		Field[] fields = rootClass.getFields();
		
		try
		{
			for (int i = 0; i < fields.length; i++)
			{
				if (fields[i].getType() == int.class)
				{
					if (fields[i].getName().length() >= type.length() && fields[i].getName().substring(0, type.length()).compareTo(type) == 0)
					{
						fields[i].setAccessible(true);
						int command = fields[i].getInt(NetworkCommand.class);
						groupDecoratorCommands.add(new Integer(command));
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static boolean isGroupDecoratorCommand(int comm)
	{
		for (Integer i : groupDecoratorCommands)
			if (i.intValue() == comm)
				return true;
		return false;
	}
	

}
