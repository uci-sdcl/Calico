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

import java.awt.Color;
import java.awt.Image;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Stack;

import javax.imageio.ImageIO;

import org.apache.commons.lang.ArrayUtils;

import calico.Calico;
import calico.CalicoDataStore;
import calico.CalicoOptions;
import calico.components.CGroup;
import calico.components.CStroke;
import calico.components.decorators.CGroupDecorator;
import calico.components.menus.CanvasMenuBar;
import calico.components.menus.CanvasStatusBar;
import calico.controllers.CCanvasController;
import calico.controllers.CGroupController;
import calico.controllers.CGroupDecoratorController;
import calico.controllers.CStrokeController;
import calico.events.CalicoEventHandler;
import calico.events.CalicoEventListener;
import calico.networking.Networking;
import calico.networking.PacketHandler;
import calico.networking.netstuff.ByteUtils;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;
import calico.plugins.CalicoPlugin;
import calico.plugins.palette.iconsets.CalicoIconManager;
import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PNode;


public class PalettePlugin extends CalicoPlugin
	implements CalicoEventListener
{
	public static final int PALETTE_ITEM_WIDTH = 30;
	public static final int PALETTE_ITEM_HEIGHT = 30;
	 
	public static Long2ReferenceOpenHashMap<Palette> palettes = new Long2ReferenceOpenHashMap<Palette>();
	public static long activePalette = 0;
	private static PaletteBar paletteBar;
	
	public PalettePlugin()
	{
		super();
		
		PluginInfo.name = "Palette";
//		paletteItems = new ArrayList<CalicoPacket>();

//		CalicoEventHandler.getInstance().addListenerForType("PALETTE", this, CalicoEventHandler.ACTION_PERFORMER_LISTENER);
		
//		CanvasMenuBar.addMenuButton(PaletteButton.class);
//		CGroup.registerPieMenuButton(SaveToPaletteButton.class);
//		CalicoEventHandler.getInstance().addListener(NetworkCommand.VIEWING_SINGLE_CANVAS, this, CalicoEventHandler.PASSIVE_LISTENER);
//		for (Integer event : this.getNetworkCommands())
//			CalicoEventHandler.getInstance().addListener(event.intValue(), this, CalicoEventHandler.ACTION_PERFORMER_LISTENER);
		
		CalicoIconManager.setIconTheme(this.getClass(), CalicoOptions.core.icontheme );
//		this.getClass().getClassLoader()
		
//		try {
//			System.out.println("~~~~~> Resource test:" + this.getClass().getResource("PalettePlugin.class").toString());
//			System.out.println("~~~~~> Resource test:" + this.getClass().getResource("/calico/iconsets/calico/icontheme.properties").toString());
//			CalicoDataStore.calicoObj.getClass().getResource("PalettePlugin.class");
//			InputStream in = this.getClass().getResourceAsStream("/calico/iconsets/calico/icontheme.properties");
//			
//			String inString = (in == null)?"NULL":in.toString();
//			System.out.println("The input handler for the icon set is: " + inString);
//			
////			CalicoIconManager.loadIconTheme(in);
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
	}
	
	
	public void onPluginStart()
	{
		//register for palette events
		CanvasStatusBar.addMenuButtonRightAligned(PaletteButton.class);
		CGroup.registerPieMenuButton(SaveToPaletteButton.class);
		CalicoEventHandler.getInstance().addListener(NetworkCommand.VIEWING_SINGLE_CANVAS, this, CalicoEventHandler.PASSIVE_LISTENER);
		for (Integer event : this.getNetworkCommands())
			CalicoEventHandler.getInstance().addListener(event.intValue(), this, CalicoEventHandler.ACTION_PERFORMER_LISTENER);
	}
	
	public static long getActivePaletteUUID()
	{
		
		if (palettes.size() == 0)
		{
			activePalette = Calico.uuid();
			palettes.put(activePalette, new Palette(activePalette));
			
		}
		else if (activePalette == 0)
		{
			activePalette = palettes.keySet().toLongArray()[0];
		}
		return activePalette;	
	}
	
	public static void setActivePalette(long activePaletteUUID)
	{
		long old = PalettePlugin.activePalette;
		PalettePlugin.activePalette = activePaletteUUID;
		CalicoEventHandler.getInstance().fireEvent(PaletteNetworkCommands.PALETTE_SWITCH_VISIBLE_PALETTE, CalicoPacket.getPacket(PaletteNetworkCommands.PALETTE_SWITCH_VISIBLE_PALETTE, activePaletteUUID, old));
	}
	
	public static void addPalette(long uuid)
	{
		palettes.put(uuid, new Palette(uuid));
	}
	
	public static void deletePalette(long uuid)
	{
		CalicoPacket deletePacket = CalicoPacket.getPacket(PaletteNetworkCommands.PALETTE_DELETE, uuid); 
		deletePacket.rewind();
		PacketHandler.receive(deletePacket);
		Networking.send(deletePacket);
	}


	private static void no_notify_deletePalette(long uuid) {
		shiftVisisblePaletteRight();
		palettes.remove(uuid);
		if (palettes.isEmpty())
		{
			activePalette = Calico.uuid();
			palettes.put(activePalette, new Palette(activePalette));
			setActivePalette(getActivePaletteUUID());
		}
	}
	
	public static long[] getPaletteUUIDs()
	{
		return palettes.keySet().toLongArray();
	}
	
	public static Palette getActivePalette()
	{
		return palettes.get(getActivePaletteUUID());
	}
	
	public static void pastePaletteItem(long paletteUUID, long paletteItemUUID, long canvasUUID, int xLoc, int yLoc)
	{
		long[] newUUIDs = getNewUUIDsForPaletteItem(paletteUUID, paletteItemUUID);
		
		int packetSize = ByteUtils.SIZE_OF_INT * 4 + ByteUtils.SIZE_OF_LONG * 3 + ByteUtils.SIZE_OF_LONG * newUUIDs.length;
		CalicoPacket pastePacket = new CalicoPacket(packetSize);
		pastePacket.putInt(PaletteNetworkCommands.PALETTE_PASTE);
		pastePacket.putLong(paletteUUID);
		pastePacket.putLong(paletteItemUUID);
		pastePacket.putLong(canvasUUID);
		pastePacket.putInt(xLoc);
		pastePacket.putInt(yLoc);
		pastePacket.putInt(newUUIDs.length);
		
		for (int i = 0; i < newUUIDs.length; i++)
		{
			pastePacket.putLong(newUUIDs[i]);
		}
		
//		CalicoPacket pastePacket = CalicoPacket.getPacket(NetworkCommand.PALETTE_PASTE, paletteUUID, paletteItemUUID, canvasUUID, xLoc, yLoc);
		
//		CalicoPacket pastePacket = getPastePacket(paletteUUID, paletteItemUUID, canvasUUID, xLoc, yLoc); 
		
		pastePacket.rewind();
		PacketHandler.receive(pastePacket);
		Networking.send(pastePacket);
	}
	
	public static void PALETTE_PASTE(CalicoPacket p)
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
		pastePacket.rewind();
		PacketHandler.receive(pastePacket);
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
	public static CalicoPacket getPastePacket(long paletteUUID, long paletteItemUUID, long canvasUUID, int xLoc, int yLoc, long[] newUUIDs)
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
	
	public static long[] getNewUUIDsForPaletteItem(long paletteUUID, long paletteItemUUID)
	{
		CalicoPacket[] subPackets = getSubPacketsFromPaletteItem(paletteUUID, paletteItemUUID);
		long[] newUUIDs = new long[subPackets.length];
		
		for (int i = 0; i < newUUIDs.length; i++)
		{
			newUUIDs[i] = Calico.uuid();
		}
		
		return newUUIDs;
	}
	
	public static void sendRandomTestPacket()
	{
		long[] palettes = getPaletteUUIDs();
		if (palettes.length < 1)
			return;
		
		Random r = new Random();
		int index = r.nextInt(palettes.length);
		
		int paletteItemSizeMax = PalettePlugin.palettes.get(palettes[index]).getPaletteItems().size();
		
		if (paletteItemSizeMax < 1)
			return;
		
		int index2 = r.nextInt(paletteItemSizeMax);
		CalicoPacket paletteItem = PalettePlugin.palettes.get(palettes[index]).getPaletteItems().get(index2);
		paletteItem.rewind();
		paletteItem.getInt();
		long paletteItemUUID = paletteItem.getLong();
		
//		CalicoPacket p = PalettePlugin.palettes.get(palettes[index]).getPaletteItems().get(index2);
		
		int x = r.nextInt(1500);
		int y = r.nextInt(1500);
		
		long[] canvases = CCanvasController.getCanvasIDList();
		long canvas = canvases[r.nextInt(canvases.length-1)];
		
		try
		{
			
			
			pastePaletteItem(palettes[index], paletteItemUUID, canvas, x, y);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
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
	
	/**
	 * This will create a palette item for a group, and all of its children.
	 * 
	 * A palette packet is formatted as follows:
	 * PALETTE_PACKET, paletteItemUUID, paletteItemImage, num of sub packets, {size of subpacket, subpacket} 
	 * 
	 * @param paletteItemUUID
	 * @param guuid
	 * @return
	 */
	public static CalicoPacket createPalettePacket(long paletteItemUUID, long paletteUUID, long guuid)
	{
		//initialize variables
		ArrayList<CalicoPacket> packets = new ArrayList<CalicoPacket>();
		CGroup group = CGroupController.groupdb.get(guuid);
		Point2D offset = group.getMidPoint();
		int xOffset = (int)offset.getX() * -1, yOffset = (int)offset.getY() * -1;
		
		//Iterate through a group and all of its children
		Stack<Long> groupChldrenStack = new Stack<Long>();
		groupChldrenStack.push(guuid);
		long temp;
		long[] children = new long[0];
		while (!groupChldrenStack.empty())
		{
			temp = groupChldrenStack.pop().longValue();
			if (CGroupController.exists(temp))
			{
				CGroup tempGroup = CGroupController.groupdb.get(temp);

				packets.addAll(0, Arrays.asList(tempGroup.getUpdatePackets(tempGroup.getUUID(), 0l, tempGroup.getParentUUID(), xOffset, yOffset, false)));
				packets.addAll(1, Arrays.asList(tempGroup.getParentingUpdatePackets()));
				
				if (tempGroup instanceof CGroupDecorator)
				{
					//append all information abou the children to the end of the packet array?
					CGroup decGroup = CGroupController.groupdb.get(((CGroupDecorator)tempGroup).getDecoratedUUID());
					packets.addAll(0, Arrays.asList(decGroup.getUpdatePackets(decGroup.getUUID(), 0l, decGroup.getParentUUID(), xOffset, yOffset, false)));
					packets.addAll(1, Arrays.asList(decGroup.getParentingUpdatePackets()));
				}
				
				children = tempGroup.getChildGroups();
				children = ArrayUtils.addAll(children, tempGroup.getChildStrokes());
			}
			else if (CStrokeController.exists(temp))
			{
				CStroke tempStroke = CStrokeController.strokes.get(temp);
				packets.add(0, tempStroke.getUpdatePackets(tempStroke.getUUID(), 0l, tempStroke.getParentUUID(), xOffset, yOffset)[0]);
				children = new long[0];
			}
			
			for (int i = children.length-1; i >= 0; i--)
				groupChldrenStack.push(new Long(children[i]));
		}
		
		//get image
		Image paletteItemImage = group.getFamilyPicture(new BufferedImage(PalettePlugin.PALETTE_ITEM_WIDTH, PalettePlugin.PALETTE_ITEM_HEIGHT, BufferedImage.TYPE_INT_ARGB), Color.white, PNode.FILL_STRATEGY_ASPECT_FIT);
		
		//first put in packets relevant to the palette
		CalicoPacket firstPacket = CalicoPacket.getPacket(PaletteNetworkCommands.PALETTE_PACKET, paletteItemUUID, paletteUUID, paletteItemImage, packets.size());
		
		//add the subpackets
		int packetSize = firstPacket.getLength();
		for (CalicoPacket p : packets)
			packetSize += ByteUtils.SIZE_OF_INT + p.getBufferSize();
		
		CalicoPacket finalPacket = new CalicoPacket(packetSize);
		
		finalPacket.putByte(firstPacket.getBuffer());
		for (CalicoPacket p : packets)
		{
			finalPacket.putInt(p.getBufferSize());
			finalPacket.putBytes(p.getBuffer());
		}
			
		
		return finalPacket;
	}


	@Override
	public void handleCalicoEvent(int event, CalicoPacket p) {
		
		switch (event)
		{
			case PaletteNetworkCommands.PALETTE_PASTE_ITEM:
				PALETTE_PASTE_ITEM(p);
				break;
			case NetworkCommand.VIEWING_SINGLE_CANVAS:
				VIEWING_SINGLE_CANVAS(p);
				break;
			case PaletteNetworkCommands.PALETTE_LOAD:
				PALETTE_LOAD(p);
				break;
			case PaletteNetworkCommands.PALETTE_PASTE:
				PALETTE_PASTE(p);
				break;
			case PaletteNetworkCommands.PALETTE_DELETE:
				PALETTE_DELETE(p);
				break;
			case PaletteNetworkCommands.PALETTE_PACKET:
				PALETTE_PACKET(p);
				break;
				
		}
		
	}
	
	private static void PALETTE_PASTE_ITEM(CalicoPacket p)
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
			PacketHandler.receive(subPackets[i]);
		}
		
		//translate them to the appropriate spot
		//	Note: we're moving just the bottom most. Everything else with move along with it because
		//		they're parented to it.
		long baseUUID;
		subPackets[0].rewind();
		subPackets[0].getInt(); //comm
		long temp = subPackets[0].getLong();
		if (CGroupController.exists(temp))
			baseUUID = CGroupController.groupdb.get(temp).getTopmostParent();
		else
		{
			int finalIndex = subPackets.length-1;
			subPackets[finalIndex].rewind();
			subPackets[finalIndex].getInt();
			temp = subPackets[finalIndex].getLong();
			baseUUID = CGroupController.groupdb.get(temp).getTopmostParent();
		}
		CGroupController.no_notify_move(baseUUID, xLoc, yLoc);
		CGroupController.no_notify_move_end(baseUUID, xLoc, yLoc);
		CGroupController.groupdb.get(baseUUID).repaint();
	}
	
	private static void VIEWING_SINGLE_CANVAS(CalicoPacket p)
	{
		p.rewind();
		p.getInt();
		long cuid = p.getLong();
		
//		CalicoInputManager.unregisterStickyItem(paletteBar);
		if (paletteBar == null)
		{
			paletteBar = new PaletteBar(PalettePlugin.getActivePalette());
			paletteBar.setVisible(false);
		}
		
		if (paletteBar.getParent() != null)
			paletteBar.getParent().removeChild(paletteBar);
		
		PCamera camera = CCanvasController.canvasdb.get(cuid).getCamera();
		camera.addChild(paletteBar);
		
		camera.repaintFrom(paletteBar.getBounds(), paletteBar);
	}
	
	private static void PALETTE_LOAD(CalicoPacket p)
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
		
//		setActivePalette(uuid);
		
	}
	
	private static void PALETTE_DELETE(CalicoPacket p)
	{
		p.rewind();
		p.getInt();
		long uuid = p.getLong();
		
		no_notify_deletePalette(uuid);
	}
	
	public static void togglePaletteBar()
	{
		
		if (!paletteBar.visible())
		{
			paletteBar.setVisible(true);
			paletteBar.repaint();
		}
		else
		{
			paletteBar.setVisible(false);
		}
	}
	
	private static int getActiveIndex()
	{
		long[] indices = PalettePlugin.getPaletteUUIDs();
		long active = PalettePlugin.getActivePaletteUUID();
		
		for (int i = 0; i < indices.length; i++)
			if (indices[i] == active)
				return i;
		return -1;
	}
	
	public static void shiftVisiblePaletteLeft()
	{
		long[] indices = PalettePlugin.getPaletteUUIDs();
		if (indices.length < 2)
			return;
		
		int currentIndex = getActiveIndex();
		long newIndex;
		
		if (currentIndex == 0)
			newIndex = indices[indices.length-1];
		else
			newIndex = indices[currentIndex-1];
		
		PalettePlugin.setActivePalette(newIndex);
	}
	
	public static void shiftVisisblePaletteRight()
	{
		long[] uuids = PalettePlugin.getPaletteUUIDs();
		if (uuids.length < 2)
			return;
		
		int currentIndex = getActiveIndex();
		long newIndex;
		
		if (currentIndex == uuids.length-1)
			newIndex = uuids[0];
		else
			newIndex = uuids[currentIndex+1];
		
		PalettePlugin.setActivePalette(newIndex);
	}
	
	public static void savePalette(File file)
	{
		CalicoPacket outputPacket = new CalicoPacket();
		
        for (CalicoPacket pi : getActivePalette().getPaletteItems())
        {
        	outputPacket.putInt(pi.getLength());
        	outputPacket.putBytes(pi.getBuffer());
        }
		
		try 
		{
	        FileOutputStream foStream = new FileOutputStream(file);

	        ByteArrayOutputStream oStream = new ByteArrayOutputStream();
	        oStream.write(outputPacket.getBuffer());
	        oStream.writeTo(foStream);
	        oStream.close();
	        foStream.close();
	        
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	public static void loadPalette(File file)
	{
		long newPalette = Calico.uuid();
		addPalette(newPalette);
		try 
		{
			FileInputStream fiStream = new FileInputStream(file);
			 
			 while (fiStream.available() > 0)
			 {
				 byte[] data = new byte[ByteUtils.SIZE_OF_INT];
				 fiStream.read(data);
				 
				 int size = ByteUtils.readInt(data, 0);
				 
				 CalicoPacket packet = new CalicoPacket(size);
				 fiStream.read(packet.getBuffer());
				 addPaletteItemToPalette(newPalette, packet);
			 }
			 
			 setActivePalette(newPalette);
			
			
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public static void addPaletteItemToPalette(long paletteUUID, CalicoPacket paletteItem)
	{
//		no_notify_addPaletteItemToPalette(paletteUUID, paletteItem);
		CalicoEventHandler.getInstance().fireEvent(PaletteNetworkCommands.PALETTE_PACKET, paletteItem);
		
		Networking.send(paletteItem);
	}
	
	public static void no_notify_addPaletteItemToPalette(long paletteUUID,
			CalicoPacket paletteItem) {
		if (!palettes.containsKey(paletteUUID))
			addPalette(paletteUUID);
		
		palettes.get(paletteUUID).addPaletteItemToPalette(paletteItem);
		
//		CalicoEventHandler.getInstance().fireEvent(PaletteNetworkCommands.PALETTE_PACKET, paletteItem);
	}
	
	public static void importImages(File[] images)
	{
		
		long paletteUUID = PalettePlugin.getActivePaletteUUID();
		CalicoPacket[] paletteItems = new CalicoPacket[images.length];
		boolean showProgressBar = true;
		
//		CalicoEventHandler.getInstance().fireEvent(NetworkCommand.STATUS_SENDING_LARGE_FILE_START, 
//				CalicoPacket.getPacket(NetworkCommand.STATUS_SENDING_LARGE_FILE_START, 0, 
//				images.length, "Loading images into palette... "));	
		
		String paletteStatusMessage = "Loading images into palette... ";
		
		if (showProgressBar)
			CalicoEventHandler.getInstance().fireEvent(NetworkCommand.STATUS_SENDING_LARGE_FILE_START, CalicoPacket.getPacket(NetworkCommand.STATUS_SENDING_LARGE_FILE_START, 0, 1, paletteStatusMessage));
		for (int i = 0; i < images.length; i++)
		{	
			Image image = null;
			try
			{
				image = ImageIO.read(images[i]);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
	        CalicoPacket firstPacket = CalicoPacket.getPacket(PaletteNetworkCommands.PALETTE_PACKET, Calico.uuid(), paletteUUID,
	        		image.getScaledInstance(PalettePlugin.PALETTE_ITEM_WIDTH, PALETTE_ITEM_HEIGHT, Image.SCALE_FAST), 1 );
	        
			CalicoPacket imagePacket = CalicoPacket.getPacket(NetworkCommand.GROUP_IMAGE_LOAD,
					0l,
					-50l,
					-100l,
					"",
					"",
					"",
					-1 * image.getWidth(null) / 2 + 10,
					-1 * image.getHeight(null) / 2 + 10,
					image.getWidth(null),
					image.getHeight(null),
					true,
					false,
					0d,
					1d,
					1d);
			imagePacket.putImage(image);
	        
//	        CalicoPacket imagePacket = CImageController.getImageTransferPacket(0l, -1l, -1 * image.getWidth(null) / 2, -1 * image.getHeight(null) / 2, images[i]);
	        
	        firstPacket.putInt(imagePacket.getBufferSize());
	        firstPacket.putBytes(imagePacket.getBuffer());
	        paletteItems[i] = firstPacket;
	        if (showProgressBar)
				CalicoEventHandler.getInstance().fireEvent(NetworkCommand.STATUS_SENDING_LARGE_FILE, 
						CalicoPacket.getPacket(NetworkCommand.STATUS_SENDING_LARGE_FILE, (double)i, 
						(double)images.length, paletteStatusMessage));	
		}
		
		
//		PalettePlugin.addPalette(paletteUUID);
		for (int i = 0; i < images.length; i++)
		{	
			PalettePlugin.no_notify_addPaletteItemToPalette(paletteUUID, paletteItems[i]);
		}
		setActivePalette(paletteUUID);
		
		if (showProgressBar)
			CalicoEventHandler.getInstance().fireEvent(NetworkCommand.STATUS_SENDING_LARGE_FILE_FINISHED, CalicoPacket.getPacket(NetworkCommand.STATUS_SENDING_LARGE_FILE_FINISHED, 100d, 100d, paletteStatusMessage));
		
		Networking.send(palettes.get(paletteUUID).getUpdatePacket());
	}
	
	public static long addGroupToPalette(long paletteUUID, long guuid)
	{
		long piuuid = Calico.uuid();
		CalicoPacket packet = PalettePlugin.createPalettePacket(piuuid, paletteUUID, guuid);
		addPaletteItemToPalette(paletteUUID, packet);
		
		return piuuid;
	}
	
	public Class<?> getNetworkCommandsClass()
	{
		return PaletteNetworkCommands.class;
	}
	
	public static void PALETTE_PACKET(CalicoPacket p)
	{
		p.rewind();
		p.getInt();
		long paletteItemUUID = p.getLong();
		long paletteUUID = p.getLong();
		Image img = p.getBufferedImage();
		
		PalettePlugin.no_notify_addPaletteItemToPalette(paletteUUID, p);

	}
	

}
