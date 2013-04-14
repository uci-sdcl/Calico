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
package calico.events;

import java.lang.reflect.*;

import java.util.ArrayList;

import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;

public class CalicoEventHandler {

	/**
	 * ACTION_PERFORMER_LISTENER are for listeners that "own" a particular event. The primary
	 * users of this listener are plugins do not have their method located in PacketHandler or ProcessQueue.
	 * 
	 * These listeners will be notified first
	 */
	public static final int ACTION_PERFORMER_LISTENER = 1;
	
	/**
	 * PASSIVE_LISTENER are for listeners that respond to some event, such as updating their local variables
	 * in response to a user entering a canvas.
	 */
	public static final int PASSIVE_LISTENER = 2;
	
	private static CalicoEventHandler instance = new CalicoEventHandler();
	
	private static Int2ReferenceOpenHashMap<ArrayList<CalicoEventListener>> eventListeners;
	
	public static CalicoEventHandler getInstance()
	{
		return instance;
	}
	
	public CalicoEventHandler()
	{
		eventListeners = new Int2ReferenceOpenHashMap<ArrayList<CalicoEventListener>>();
		registerEvents();
//		System.out.println("Instanciated the Calico Event Handler Class!");
	}
	
	
	
	private void registerEvents()
	{
		Class<?> rootClass = NetworkCommand.class;
		Field[] fields = rootClass.getFields();
		
		try
		{
			for (int i = 0; i < fields.length; i++)
			{
				if (fields[i].getType() == int.class)
				{
					fields[i].setAccessible(true);
					int value = fields[i].getInt(NetworkCommand.class);
					addEvent(value);
//					System.out.println("Registering event for: " + fields[i].getName() + ", value: " + fields[i].getInt(NetworkCommand.class));
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

	}
	
	public boolean addEvent(int value)
	{
		if (eventListeners.containsKey(value))
		{
			return false;
		}
		if (value >= 2200 && value < 2300)
			System.out.println("found a palette one!");
		
		eventListeners.put(value, new ArrayList<CalicoEventListener>());
		return true;
	}
	
	public void addListener(int event, CalicoEventListener listener, int listenerType)
	{
		if (!eventListeners.containsKey(event) || listener == null)
			return;
		
		if (listenerType == CalicoEventHandler.ACTION_PERFORMER_LISTENER)
			eventListeners.get(event).add(0, listener);
		else
			eventListeners.get(event).add(listener);
		
//		System.out.println("Added listener " + listener.getClass().getName() + " for value " + event);
	}
	
	public void removeListener(int event, CalicoEventListener listener)
	{
		synchronized (this)
		{
			eventListeners.get(event).remove(listener);
		}
	}
	
	public void addListenerForType(String type, CalicoEventListener listener, int listenerType)
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
						if (listenerType == CalicoEventHandler.ACTION_PERFORMER_LISTENER)
							eventListeners.get(command).add(0, listener);
						else
							eventListeners.get(command).add(listener);
//						eventListeners.get(command).add(listener);
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void fireEvent(int event, CalicoPacket p)
	{
		if (!eventListeners.containsKey(event))
			return;
		
		synchronized (this)
		{
			ArrayList<CalicoEventListener> listeners = eventListeners.get(event);
			
			//we create a clone because some listeners may want to stop listening, and if they remove themselves,
			//	that may cause a java.util.ConcurrentModificationException if we use the same arraylist
			ArrayList<CalicoEventListener> listenerClone = new ArrayList<CalicoEventListener>(listeners);
			
			for (CalicoEventListener listener : listenerClone)
				listener.handleCalicoEvent(event, p);
		}
	}
	
}
