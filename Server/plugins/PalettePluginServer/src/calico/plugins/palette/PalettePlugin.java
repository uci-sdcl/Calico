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
package calico.plugins.palette;

import it.unimi.dsi.fastutil.longs.Long2ReferenceArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.awt.Color;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

import org.apache.commons.lang.ArrayUtils;

import calico.ProcessQueue;
import calico.clients.Client;
import calico.clients.ClientManager;
import calico.components.CGroup;
import calico.components.CGroupImage;
import calico.components.CStroke;
import calico.components.decorators.CGroupDecorator;
import calico.controllers.CCanvasController;
import calico.controllers.CGroupController;
import calico.controllers.CGroupDecoratorController;
import calico.controllers.CImageController;
import calico.controllers.CStrokeController;
import calico.events.CalicoEventHandler;
import calico.events.CalicoEventListener;
import calico.networking.netstuff.ByteUtils;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;
import calico.plugins.AbstractCalicoPlugin;
import calico.plugins.CalicoPlugin;
import calico.plugins.CalicoPluginManager;
import calico.plugins.CalicoStateElement;
import calico.utils.CalicoBackupHandler;
import calico.uuid.UUIDAllocator;
import edu.umd.cs.piccolo.PNode;


public class PalettePlugin extends AbstractCalicoPlugin
	implements CalicoEventListener, CalicoStateElement
{
	public static final int PALETTE_ITEM_WIDTH = 30;
	public static final int PALETTE_ITEM_HEIGHT = 30;
	 
	public static Long2ReferenceOpenHashMap<Palette> palettes = new Long2ReferenceOpenHashMap<Palette>();
	public static long activePalette = 0;
	
	public PalettePlugin()
	{
		super();
		
		PluginInfo.name = "Palette";
//		paletteItems = new ArrayList<CalicoPacket>();
//		CalicoEventHandler.getInstance().addListenerForType("PALETTE", this, CalicoEventHandler.ACTION_PERFORMER_LISTENER);
//		for (Integer event : this.getNetworkCommands())
//		{
//			System.out.println("PalettePlugin: attempting to listen for " + event.intValue());
//			CalicoEventHandler.getInstance().addListener(event.intValue(), this, CalicoEventHandler.ACTION_PERFORMER_LISTENER);
//		}
//		
//		CalicoPluginManager.registerCalicoStateExtension(this);

		

		

	}
	
	
	public void onPluginStart()
	{		
		//register for palette events
		for (Integer event : this.getNetworkCommands())
		{
			System.out.println("PalettePlugin: attempting to listen for " + event.intValue());
			CalicoEventHandler.getInstance().addListener(event.intValue(), this, CalicoEventHandler.ACTION_PERFORMER_LISTENER);
			LongSet canvasKeys = CCanvasController.canvases.keySet();
			for (Long canvaskey : canvasKeys)
			{
				CalicoEventHandler.getInstance().addListener(event.intValue(), CCanvasController.canvases.get(canvaskey.longValue()), CalicoEventHandler.PASSIVE_LISTENER);
			}
		}
		CalicoPluginManager.registerCalicoStateExtension(this);
		
		addPalette(UUIDAllocator.getUUID());
	}
	
	
	@Override
	public void handleCalicoEvent(int event, CalicoPacket p, Client client) {
		
		switch (event)
		{
			case PaletteNetworkCommands.PALETTE_PASTE_ITEM:
				PALETTE_PASTE_ITEM(p, client);
				break;
			case PaletteNetworkCommands.PALETTE_PACKET:
				PALETTE_PACKET(p, client);
				break;
			case PaletteNetworkCommands.PALETTE_LOAD:
				PALETTE_LOAD(p, client);
				break;
			case PaletteNetworkCommands.PALETTE_PASTE:
				PALETTE_PASTE(p, client);
				break;
			case PaletteNetworkCommands.PALETTE_DELETE:
				PALETTE_DELETE(p, client);
				break;
		}
		
	}
	
	private static void addPaletteItemToPalette(long paletteUUID, CalicoPacket paletteItem)
	{
		no_notify_addPaletteItemToPalette(paletteUUID, paletteItem);
	}
	
	private static void addPalette(long uuid)
	{
		palettes.put(uuid, new Palette(uuid));
	}

	private static void no_notify_addPaletteItemToPalette(long paletteUUID,
			CalicoPacket paletteItem) {
		if (!palettes.containsKey(paletteUUID))
			addPalette(paletteUUID);
		
		palettes.get(paletteUUID).addPaletteItemToPalette(paletteItem);
		
	}
	
	private static void no_notify_deletePalette(long uuid) {
		palettes.remove(uuid);
	}
	
	/**
	 * 
	 * A paste packet is formated as follows:
	 * (NetworkCommand.PALETTE_PASTE_ITEM, xLoc, yLoc, numSubPackets, { subPacketSize, subPacket} )
	 * 
	 * @param paletteUUID
	 * @param paletteItemUUID
	 * @param canvasUUID
	 * @param xLoc
	 * @param yLoc
	 */
	private static CalicoPacket getPastePacket(long paletteUUID, long paletteItemUUID, long canvasUUID, int xLoc, int yLoc, long[] newUUIDs)
	{
		
		CalicoPacket[] subPackets = getSubPacketsFromPaletteItem(paletteUUID, paletteItemUUID);
		
		//update palette item with new information
		CalicoPacket[] newPackets = getSubItemsWithNewUUIDs(subPackets, canvasUUID, newUUIDs);
		
		int sizeOfSubPackets = 0;
		for (int i = 0; i < newPackets.length; i++)
			sizeOfSubPackets += newPackets[i].getBufferSize();
		
		//create new paste packet
		int packetSize = ByteUtils.SIZE_OF_INT * 4 
						+ ByteUtils.SIZE_OF_BYTE * sizeOfSubPackets;
		
		CalicoPacket pastePacket = new CalicoPacket(packetSize);
		pastePacket.putInt(PaletteNetworkCommands.PALETTE_PASTE_ITEM);
		pastePacket.putInt(xLoc);
		pastePacket.putInt(yLoc);
		pastePacket.putInt(newPackets.length);
		for (int i = 0; i < subPackets.length; i++)
		{
			pastePacket.putInt(newPackets[i].getBufferSize());
			pastePacket.putBytes(newPackets[i].getBuffer());
		}
		
		return pastePacket;
	}
	
	private static CalicoPacket[] getSubPacketsFromPaletteItem(
			long paletteUUID, long paletteItemUUID) {
		
		CalicoPacket paletteItem = palettes.get(paletteUUID).getItem(paletteItemUUID);
		
		//get information from the stored palette item
		paletteItem.rewind();
		paletteItem.getInt();
		paletteItem.getLong();
		paletteItem.getLong();
		paletteItem.getBufferedImage();
		int numSubPackets = paletteItem.getInt();
//		int sizeOfSubPackets = paletteItem.remaining();
		CalicoPacket[] subPackets = new CalicoPacket[numSubPackets];
		int subPacketLength = 0;
		for (int i = 0; i < subPackets.length; i++)
		{
			subPacketLength = paletteItem.getInt();
			subPackets[i] = new CalicoPacket(paletteItem.getByteArray(subPacketLength));
		}
		return subPackets;
	}
	
	/**
	 * Internal method used by getPastePacket
	 * @param subItems
	 * @param cuid
	 * @return
	 */
	private static CalicoPacket[] getSubItemsWithNewUUIDs(CalicoPacket[] subItems, long cuid, long[] newUUIDs)
	{
		Long2ReferenceArrayMap<Long> subItemMappings = new Long2ReferenceArrayMap<Long>();
		
		int newUUIDIndex = 0;
		
		for (int i = 0; i < subItems.length; i++)
		{
			subItems[i].rewind();
			subItems[i].getInt(); //comm
			long orig = subItems[i].getLong();
			subItemMappings.put(orig, new Long(newUUIDs[newUUIDIndex++]));
		}
		
		CalicoPacket[] newSubItems = new CalicoPacket[subItems.length];
		int comm;
		long oldUUID;
		long oldParent;
		long newParent;
		long oldDecoratedGroup = 0;
		long newDecoratedGroup = 0;
		for (int i = 0; i < subItems.length; i++)
		{
			newSubItems[i] = new CalicoPacket(subItems[i].getBuffer());
			//get old uuid
			subItems[i].rewind();
			comm = subItems[i].getInt();
			oldUUID = subItems[i].getLong();
			
			if (comm == NetworkCommand.GROUP_SET_CHILD_STROKES
					|| comm == NetworkCommand.GROUP_SET_CHILD_GROUPS
					|| comm == NetworkCommand.GROUP_SET_CHILD_ARROWS)
			{
				int numChildren = subItems[i].getCharInt();
				long[] children = new long[numChildren];
				for (int j = 0; j < children.length; j++)
				{
					children[j] = subItems[i].getLong();
				}
				
				newSubItems[i].rewind();
				newSubItems[i].getInt();
				newSubItems[i].putLong(subItemMappings.get(oldUUID).longValue());
				newSubItems[i].getCharInt();
				for (int j = 0; j < children.length; j++)
				{
					if (subItemMappings.get(children[j]) == null)
						newSubItems[i].putLong(0l);
					else
						newSubItems[i].putLong(subItemMappings.get(children[j]).longValue());
				}
			}
			else
			{

				subItems[i].getLong(); //old cuuid
				oldParent = subItems[i].getLong();
				if (CGroupDecoratorController.isGroupDecoratorCommand(comm))
					oldDecoratedGroup = subItems[i].getLong();
				
				//set new uuids
				newSubItems[i].rewind();
				newSubItems[i].getInt();
				newSubItems[i].putLong(subItemMappings.get(oldUUID).longValue());
				newSubItems[i].putLong(cuid);
				newParent = (subItemMappings.containsKey(oldParent)) ? subItemMappings.get(oldParent).longValue() : 0l;
				newSubItems[i].putLong(newParent);
				if (CGroupDecoratorController.isGroupDecoratorCommand(comm))
				{
					newDecoratedGroup = subItemMappings.get(oldDecoratedGroup);
					newSubItems[i].putLong(newDecoratedGroup);
				}
			}
		}
		
		return newSubItems;
	}
	
	private static void PALETTE_PASTE_ITEM(CalicoPacket p, Client client)
	{
//		(NetworkCommand.PALETTE_PASTE_ITEM, xLoc, yLoc, numSubPackets, { subPacketSize, subPacket} )
		
		//recieve the packet information
		p.rewind();
		int comm = p.getInt(); //NetworkCommand.PALETEE_PASTE_ITEM
		if (comm != PaletteNetworkCommands.PALETTE_PASTE_ITEM)
			return;
		int xLoc = p.getInt();
		int yLoc = p.getInt();
		int numSubPackets = p.getInt();
		CalicoPacket[] subPackets = new CalicoPacket[numSubPackets];
		int subPacketSize;
		for (int i = 0; i < subPackets.length; i++)
		{
			subPacketSize = p.getInt();
			subPackets[i] = new CalicoPacket(p.getByteArray(subPacketSize));
		}
		
		//create the new items
		for (int i = 0; i < subPackets.length; i++) 
		{
			subPackets[i].rewind();
			int subComm = subPackets[i].getInt();
			ProcessQueue.receive(subComm, client, subPackets[i]);
		}
		
		//translate them to the appropriate spot
		//	Note: we're moving just the bottom most. Everything else with move along with it because
		//		they're parented to it.
		long baseUUID;
		subPackets[0].rewind();
		subPackets[0].getInt(); //comm
		long temp = subPackets[0].getLong();
		if (CGroupController.exists(temp))
			baseUUID = CGroupController.groups.get(temp).getTopmostParent();
		else
		{
			int finalIndex = subPackets.length-1;
			subPackets[finalIndex].rewind();
			subPackets[finalIndex].getInt();
			temp = subPackets[finalIndex].getLong();
			baseUUID = CGroupController.groups.get(temp).getTopmostParent();
		}
		
		CGroupController.no_notify_move(baseUUID, xLoc, yLoc);
		CGroupController.no_notify_move_end(baseUUID, xLoc, yLoc);
		CCanvasController.canvases.get(CGroupController.groups.get(baseUUID).getCanvasUUID()).updateSignature();
		CCanvasController.snapshot(CGroupController.groups.get(baseUUID).getCanvasUUID());
	}
	
	private static void PALETTE_PACKET(CalicoPacket p, Client client)
	{
		p.rewind();
		p.getInt();
		long paletteItemUUID = p.getLong();
		long paletteUUID = p.getLong();
		Image img = p.getBufferedImage();
		
		PalettePlugin.addPaletteItemToPalette(paletteUUID, p);
		
		if (client != null)
		{
			ClientManager.send_except(client, p);
		}
	}

	long[] getPaletteUUIDs()
	{
		return palettes.keySet().toLongArray();
	}

	@Override
	public CalicoPacket[] getCalicoStateElementUpdatePackets() {
		ArrayList<CalicoPacket> paletteItems = new ArrayList<CalicoPacket>();
		long[] pKeys = getPaletteUUIDs();
		for (int i = 0; i < pKeys.length; i++)
		{
			paletteItems.addAll(palettes.get(pKeys[i]).getPaletteItems());
		}
		return paletteItems.toArray(new CalicoPacket[paletteItems.size()]);
		
		
//		CalicoPacket[] p = new CalicoPacket[palettes.size()];
//		long[] pKeys = getPaletteUUIDs();
//		for (int i = 0; i < pKeys.length; i++)
//		{
//			p[i] = palettes.get(pKeys[i]).getUpdatePacket();
//		}
//		
//		return p;
	}
	
	private static void PALETTE_LOAD(CalicoPacket p, Client client)
	{
		p.rewind();
		p.getInt();
		long uuid = p.getLong();
		int numPaletteItems = p.getInt();
		addPalette(uuid);
		
		int size;
		CalicoPacket paletteItem;
		
		for (int i = 0; i < numPaletteItems; i++)
		{
			size = p.getInt();
			paletteItem = new CalicoPacket(p.getByteArray(size));
			no_notify_addPaletteItemToPalette(uuid, paletteItem);
		}
		
		if (client != null)
		{
			ClientManager.send_except(client, p);
		}
		
//		setActivePalette(uuid);
		
	}
	
	public static void PALETTE_PASTE(CalicoPacket p, Client c)
	{
		p.rewind();
		p.getInt();
		long paletteUUID = p.getLong();
		long paletteItemUUID = p.getLong();
		long canvasUUID = p.getLong();
		int xLoc = p.getInt();
		int yLoc = p.getInt();
		int numUUIDs = p.getInt();
		long[] newUUIDs = new long[numUUIDs];
		
		for (int i = 0; i < numUUIDs; i++)
		{
			newUUIDs[i] = p.getLong();
		}
		
		CalicoPacket pastePacket = getPastePacket(paletteUUID, paletteItemUUID, canvasUUID, xLoc, yLoc, newUUIDs); 
		
//		PALETTE_PASTE_ITEM(pastePacket, c);
		ProcessQueue.receive(PaletteNetworkCommands.PALETTE_PASTE_ITEM, null, pastePacket);
		
		if (c != null)
		{
			ClientManager.send_except(c, p);
		}
	}
	
	public static void PALETTE_DELETE(CalicoPacket p, Client c)
	{
		p.rewind();
		p.getInt();
		long uuid = p.getLong();
		
		no_notify_deletePalette(uuid);
		
		if (c != null)
		{
			ClientManager.send_except(c, p);
		}
	}
	
	public Class<?> getNetworkCommandsClass()
	{
		return PaletteNetworkCommands.class;
	}
	
	
	

}
