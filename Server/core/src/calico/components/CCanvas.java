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
package calico.components;

import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import calico.COptions;
import calico.clients.Client;
import calico.controllers.CArrowController;
import calico.controllers.CConnectorController;
import calico.controllers.CGridController;
import calico.controllers.CGroupController;
import calico.controllers.CStrokeController;
import calico.events.CalicoEventHandler;
import calico.events.CalicoEventListener;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;
import calico.uuid.UUIDAllocator;

public class CCanvas
	implements CalicoEventListener
{
	private static final AtomicInteger INDEX_COUNTER = new AtomicInteger(0);
	
	public static void clearState()
	{
		INDEX_COUNTER.set(0);
	}

	private long uuid = 0L;
	private int index = 0;
	private boolean lock_value = false;
	private String lock_last_set_by_user = "";
	private long lock_last_set_at_time = 0l;
		

	private LongArraySet strokes = new LongArraySet();
	private LongArraySet groups = new LongArraySet();
	private LongArraySet arrows = new LongArraySet();
	private LongArraySet lists = new LongArraySet();
	private LongArraySet connectors = new LongArraySet();
	
	public ObjectArrayList<Object> keypairs = new ObjectArrayList<Object>();
	
	private IntArraySet clients = new IntArraySet();

	
	private ObjectArrayList<CCanvasBackupState> snapshots = new ObjectArrayList<CCanvasBackupState>();
	private int snapshotIndex = 0;
	
	private ArrayList<Integer> eventsThatUpdateSignature;
	private int signature = 0;
	
	

	/**
	 * @deprecated
	 * @see #Canvas(long)
	 */
	public CCanvas()
	{
		// This means we should get a new UUID to use
		this(UUIDAllocator.getUUID());
	}
	
	/**
	 * @param u
	 */
	public CCanvas(long u)
	{
		this.uuid = u;
		this.index = INDEX_COUNTER.getAndIncrement();

		snapshots.add(0, getBackupState());
		
		setupEventListeners();
	}
	
	/**
	 * @deprecated
	 * @see #Canvas(long)
	 */
	public CCanvas(long u, int s)
	{
		this.uuid = u;

	}
	
	public int getIndex()
	{
		return index;
	}
	
	public CCanvasBackupState getBackupState()
	{
		return new CCanvasBackupState(this.uuid, getUpdatePackets());
	}
	
	
	// This will save a canvas revision to the undo history
	public void saveCurrentCanvasState()
	{
		// If we have gone over the size limit, then we should remove one
		if(snapshots.size()>=COptions.canvas.max_snapshots)//COptions.CanvasMaxSnapshots)
		{
			snapshots.remove(0);
		}
		else
		{
			// We only increment the pointer if we havent removed anything
			snapshotIndex++;
		}
		
		// We need to clear out any redo history
		if(snapshotIndex<snapshots.size())
		{
			snapshots.removeElements(snapshotIndex, snapshots.size());
		}
		
		// Add the current state to the snapshot
		snapshots.add(getBackupState());
	}
	
	public boolean removeMostRecentCanvasState()
	{
		try
		{
			if (snapshots.size() < 1)
				return false;
			
			snapshots.remove(snapshots.size()-1);
			snapshotIndex--;
			
			if(snapshotIndex<0)
			{
				throw new NoSuchElementException();
			}
			
//			getBackupState().updateToNewState(snapshots.get(snapshotIndex));
			
			
			return true;
		}
		catch(NoSuchElementException e)
		{
			snapshotIndex++;
			return false;
		}		
	}
	
	/**
	 * Runs the undo sequence
	 * @return true if we were able to undo
	 */
	public boolean performUndo()
	{
		try
		{
			snapshotIndex--;
			
			if(snapshotIndex<0)
			{
				throw new NoSuchElementException();
			}
			
			getBackupState().updateToNewState(snapshots.get(snapshotIndex));
			
			
			return true;
		}
		catch(NoSuchElementException e)
		{
			snapshotIndex++;
			return false;
		}
		
	}
	public boolean performRedo()
	{
		try
		{
			snapshotIndex++;
			
			if(snapshotIndex>=snapshots.size())
			{
				throw new NoSuchElementException();
			}
			
			getBackupState().updateToNewState(snapshots.get(snapshotIndex));
			
			return true;
		}
		catch(NoSuchElementException e)
		{
			snapshotIndex--;
			return false;
		}
	}
	
	public long getUUID()
	{
		return this.uuid;
	}
	
	public void addChildStroke(long s)
	{
		this.strokes.add(s);
	}
	
	public void addChildConnector(long s)
	{
		this.connectors.add(s);
	}
	
	public void addChildGroup(long s)
	{
		this.groups.add(s);
	}
	
	public void addChildList(long lUUID)
	{
		this.lists.add(lUUID);
	}
	
	public void deleteChildGroup(long s)
	{
		this.groups.remove(s);
	}
	
	public void deleteChildStroke(long s)
	{
		this.strokes.remove(s);
	}
	
	public void deleteChildConnector(long s)
	{
		this.connectors.remove(s);
	}
		
	public long[] getChildStrokes()
	{
		return this.strokes.toLongArray();
	}	
	
	public long[] getChildGroups()
	{
		return this.groups.toLongArray();
	}
	
	public long[] getChildLists()
	{
		return this.lists.toLongArray();
	}

	public long[] getChildConnectors()
	{
		return this.connectors.toLongArray();
	}

	
	
	public void addChildArrow(long uid)
	{
		this.arrows.add(uid);
	}
	public void deleteChildArrow(long uid)
	{
		this.arrows.remove(uid);
	}
	public long[] getChildArrows()
	{
		return this.arrows.toLongArray();
	}

	public CalicoPacket getInfoPacket()
	{
		return CalicoPacket.getPacket(
			NetworkCommand.CANVAS_INFO,
			this.uuid,
			this.index
		);
	}
	
	public void render(Graphics2D g)
	{
		Date todaysDate = new java.util.Date();
		SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss Z");
		String formattedDate = formatter.format(todaysDate);
		
		Font renderFont = new Font("Verdana",Font.BOLD, 12);
		g.setFont(renderFont);
		g.setColor(Color.BLACK);
		g.drawString("Calico Canvas ("+index+") - Rendered on "+formattedDate, 10, 14);
		g.translate(0, 14);

		long[] groupa = groups.toLongArray();
		for(int i=0;i<groupa.length;i++)
		{
			CGroupController.groups.get(groupa[i]).render(g);
		}
		
		
		long[] strokea = strokes.toLongArray();
		for(int i=0;i<strokea.length;i++)
		{
			CStrokeController.strokes.get(strokea[i]).render(g);
		}
		
		long[] connectora = connectors.toLongArray();
		for(int i=0;i<connectora.length;i++)
		{
			CConnectorController.connectors.get(connectora[i]).render(g);
		}
	}
	
	
	
	public CalicoPacket[] getUpdatePackets()
	{
		ObjectArrayList<CalicoPacket> packetlist = new ObjectArrayList<CalicoPacket>();
		
		//packetlist.add(getInfoPacket());	

		
		long[] grouparr = getChildGroups();
		long[] bgearr = getChildStrokes();
		long[] arlist = getChildArrows();
		long[] ctrlist = getChildConnectors();
		
		// GROUPS
		if(grouparr.length>0)
		{
			// Send Group Info
			for(int i=0;i<grouparr.length;i++)
			{
				//if (!CGroupController.groups.get(grouparr[i]).isPermanent)
				//	continue;
				// we only want to load root groups
				if(true /*CGroupController.groups.get(grouparr[i]).getParentUUID()==0L*/)
				{
					packetlist.addElements(packetlist.size(), CGroupController.groups.get(grouparr[i]).getUpdatePackets(false) );
					
					// Load the children of that group
//					long[] groupChildren = CGroupController.groups.get(grouparr[i]).getChildGroups();
//					if(groupChildren.length>0)
//					{
//						for(int cg=0;cg<groupChildren.length;cg++)
//						{
//							if(CGroupController.exists(groupChildren[cg]))
//							{
//								packetlist.addElements(packetlist.size(), CGroupController.groups.get(groupChildren[cg]).getUpdatePackets(false));
//							}
//						}
//					}
					
				}
			}
			
			// Parents
			for(int i=0;i<grouparr.length;i++)
			{
				//if (!CGroupController.groups.get(grouparr[i]).isPermanent)
				//	continue;
				
				CalicoPacket[] packets = CGroupController.groups.get(grouparr[i]).getParentingUpdatePackets();
				for(int x=0;x<packets.length;x++)
				{
					packetlist.add(packets[x]);
				}
			}
		}
		
		
		// ARROWS
		if(arlist.length>0)
		{
			for(int i=0;i<arlist.length;i++)
			{
				CalicoPacket[] packets = CArrowController.arrows.get(arlist[i]).getUpdatePackets();
				if(packets!=null && packets.length>0)
				{
					packetlist.addElements(packetlist.size(), packets);
				}
			}
		}
		
		// CONNECTORS
		if(ctrlist.length>0)
		{
			for(int i=0;i<ctrlist.length;i++)
			{
				CalicoPacket[] packets = CConnectorController.connectors.get(ctrlist[i]).getUpdatePackets();
				if(packets!=null && packets.length>0)
				{
					packetlist.addElements(packetlist.size(), packets);
				}
				packets = CConnectorController.connectors.get(ctrlist[i]).getComposableElements();
				if(packets!=null && packets.length>0)
				{
					packetlist.addElements(packetlist.size(), packets);
				}
			}
		}
		
		// Send the BGElement Parents
		if(bgearr.length>0)
		{
			for(int i=0;i<bgearr.length;i++)
			{
				CalicoPacket[] packets = CStrokeController.strokes.get(bgearr[i]).getUpdatePackets();
				if(packets!=null && packets.length>0)
				{
					packetlist.addElements(packetlist.size(), packets);
				}
			}
		}//
		
		packetlist.addElements(0, new CalicoPacket[] { CalicoPacket.getPacket(NetworkCommand.CANVAS_LOCK, this.uuid, lock_value, this.lock_last_set_by_user, this.lock_last_set_at_time)});
		
		return packetlist.toArray(new CalicoPacket[]{});
	}
	

	public Properties toProperties()
	{
		Properties props = new Properties();

		props.setProperty("uuid", ""+this.uuid);
		props.setProperty("grid.index", ""+this.index);

		props.setProperty("child.groups", Arrays.toString(getChildGroups()) );
		props.setProperty("child.strokes", Arrays.toString(getChildStrokes()) );
		props.setProperty("child.arrows", Arrays.toString(getChildArrows()) );
		props.setProperty("child.connectors", Arrays.toString(getChildConnectors()) );
		return props;
	}

	
	public void addClient(int clientid) {
		this.clients.add(clientid);
	}
	
	public void removeClient(int clientid) {
		this.clients.remove(clientid);
	}
	
	public int[] getClients() {
		return this.clients.toIntArray();
	}

	public void setCanvasLock(boolean lock, String user, long time)
	{
		this.lock_value = lock;
		this.lock_last_set_by_user = user;
		this.lock_last_set_at_time = time;
	}
	
	public boolean getLockValue()
	{
		return this.lock_value;
	}
	
	public String getLockedByUser()
	{
		return this.lock_last_set_by_user;
	}
	
	public long getLockedAtTime()
	{
		return this.lock_last_set_at_time;
	}
	
	public void resetLock() {
		this.lock_value = false;
		this.lock_last_set_by_user = "";
		this.lock_last_set_at_time = 0l;
	}
	
	private void setupEventListeners()
	{
		CalicoEventHandler.getInstance().addListenerForType("GROUP", this, CalicoEventHandler.PASSIVE_LISTENER);
		CalicoEventHandler.getInstance().addListenerForType("STROKE", this, CalicoEventHandler.PASSIVE_LISTENER);
		CalicoEventHandler.getInstance().addListenerForType("ARROW", this, CalicoEventHandler.PASSIVE_LISTENER);
		CalicoEventHandler.getInstance().addListenerForType("CONNECTOR", this, CalicoEventHandler.PASSIVE_LISTENER);
		CalicoEventHandler.getInstance().addListenerForType("CANVAS", this, CalicoEventHandler.PASSIVE_LISTENER);
		CalicoEventHandler.getInstance().addListenerForType("LIST", this, CalicoEventHandler.PASSIVE_LISTENER);
		CalicoEventHandler.getInstance().addListenerForType("PALETTE", this, CalicoEventHandler.PASSIVE_LISTENER);
	}

	@Override
	public void handleCalicoEvent(int event, CalicoPacket p, Client client) {
		if (event > 2200)
		{
//			System.out.println("awesome!");
		}
		try
		{
			p.rewind();
			p.getInt();
			p.getLong();
			long c = p.getLong();
			if (c != this.getUUID())
				return;
		}
		catch (Exception e)
		{
			//do nothing
		}
		updateSignature();
	}
	
	public int get_signature()
	{
		int sig = 0;

		long[] strokear = getChildStrokes();
		long[] groupar = getChildGroups();
		long[] arrar = getChildArrows();
		long[] ctrar = getChildConnectors();
		
		int stroke_sig = 0;
		for(int i=0;i<strokear.length;i++)
		{
			stroke_sig = stroke_sig + CStrokeController.get_signature(strokear[i]);
		}
		
		int group_sig = 0;
		for(int i=0;i<groupar.length;i++)
		{
			group_sig = group_sig + CGroupController.get_signature(groupar[i]);
		}
		
		int arrow_sig = 0;
		for (int i=0;i<arrar.length;i++)
		{
			arrow_sig = arrow_sig + CArrowController.get_signature(arrar[i]);
		}
		
		int connector_sig = 0;
		for (int i=0;i<ctrar.length;i++)
		{
			connector_sig = connector_sig + CConnectorController.get_signature(ctrar[i]);
		}
		
		return stroke_sig + group_sig + arrow_sig + connector_sig;
	}
	
	public CalicoPacket getConsistencyDebugPacket()
	{
//		int sig = this.strokes.size() + this.groups.size() + this.lists.size() + this.checkBoxes.size() + this.arrows.size();
		CalicoPacket p = new CalicoPacket();
		p.putInt(NetworkCommand.CONSISTENCY_DEBUG);
		

		long[] strokear = getChildStrokes();
		long[] groupar = getChildGroups();
		long[] connectorar = getChildConnectors();
		
		int stroke_sig = 0;
		for(int i=0;i<strokear.length;i++)
		{
			long stroke_uuid = strokear[i];
			stroke_sig = CStrokeController.get_signature(strokear[i]);
			p.putLong(strokear[i]);
			p.putInt(stroke_sig);
			p.putString(CStrokeController.get_signature_debug_output(strokear[i]));
			
		}
		
		int group_sig = 0;
		for(int i=0;i<groupar.length;i++)
		{
			long group_uuid = groupar[i];
			group_sig = CGroupController.get_signature(groupar[i]);
			p.putLong(groupar[i]);
			p.putInt(group_sig);
			p.putString(CGroupController.get_signature_debug_output(groupar[i]));
		}
		
		int connector_sig = 0;
		for (int i=0;i<connectorar.length;i++)
		{
			connector_sig = CConnectorController.get_signature(connectorar[i]);
			p.putLong(connectorar[i]);
			p.putInt(connector_sig);
			p.putString(CConnectorController.get_signature_debug_output(connectorar[i]));
		}
		
		return p;
	}
	
	public void updateSignature()
	{
		this.signature = get_signature();
	}
	
	public int getSignature()
	{
		return this.signature;
	}

}

