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

import it.unimi.dsi.fastutil.ints.Int2ReferenceAVLTreeMap;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import calico.clients.Client;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;

public class CanvasThread extends Thread {
	
	private static ArrayBlockingQueue<CanvasPacket> packetQueue;
	
	private int sleepCount;
	private long canvasid;
	
	public CanvasThread(long canvasid) throws IOException
	{
		super("CanvasThread-"+canvasid);
		packetQueue = new ArrayBlockingQueue<CanvasPacket>(4096);
		
		this.canvasid = canvasid;
		sleepCount = 0;
		
		start();
	}
	
	public void addPacketToQueue(int command,Client client,CalicoPacket packet)
	{
		packetQueue.offer(new CanvasPacket(command, client, packet));
	}
	
	public void run()
	{
		while(true)
		{
			if (!packetQueue.isEmpty())
			{
				sleepCount = 0;
				try
				{
					CanvasPacket packet = packetQueue.poll();
					if (packet != null)
					{
						ProcessQueue.receive(packet.command, packet.client, packet.packet);
					}
				}
				//Catch possible concurrency issue.
				catch(Exception e)
				{
					e.printStackTrace();
					//If there is an error, we kill the thread, otherwise there may be an infinite loop of errors.
					//Of course this means none of the packets in the queue will be processed, but it is better than infinite loop.
					return;
				}
			
			}
			else
			{
				sleepCount++;
				
				//Kill the thread if no packets are queued for 5 seconds. 
				if (sleepCount >= COptions.canvas.max_sleep_count)
				{
					synchronized(CalicoServer.canvasThreads)
					{
						CalicoServer.canvasThreads.remove(canvasid);
					}
					return;
				}
				
				// CPU Limiter (This prevents the CPU from going bonkers)
				try {
					Thread.sleep(COptions.canvas.sleeptime);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public static Int2ReferenceAVLTreeMap<Object> getCanvasCommands()
	{
		Int2ReferenceAVLTreeMap<Object> commands = new Int2ReferenceAVLTreeMap<Object>();
		
		commands.put(NetworkCommand.UUID_GET_BLOCK, null);
		
		//Stroke commands
		commands.put(NetworkCommand.STROKE_START, null);
		commands.put(NetworkCommand.STROKE_APPEND, null);
		commands.put(NetworkCommand.STROKE_DELETE, null);
		commands.put(NetworkCommand.STROKE_FINISH, null);
		commands.put(NetworkCommand.STROKE_MOVE, null);
		commands.put(NetworkCommand.STROKE_SET_COLOR, null);
		commands.put(NetworkCommand.STROKE_SET_PARENT, null);
		commands.put(NetworkCommand.STROKE_LOAD, null);
		commands.put(NetworkCommand.STROKE_REQUEST_HASH_CHECK, null);
		commands.put(NetworkCommand.STROKE_MAKE_SCRAP, null);
		commands.put(NetworkCommand.STROKE_MAKE_SHRUNK_SCRAP, null);
		commands.put(NetworkCommand.STROKE_DELETE_AREA, null);
		commands.put(NetworkCommand.STROKE_ROTATE, null);
		commands.put(NetworkCommand.STROKE_SCALE, null);
		commands.put(NetworkCommand.STROKE_SET_AS_POINTER, null);
		commands.put(NetworkCommand.STROKE_HIDE, null);
		commands.put(NetworkCommand.STROKE_UNHIDE, null);
		
		//Erase Canvas commands
		commands.put(NetworkCommand.ERASE_START, null);
		commands.put(NetworkCommand.ERASE_END, null);
		
		//Group commands
		commands.put(NetworkCommand.GROUP_START, null);
		commands.put(NetworkCommand.GROUP_APPEND, null);
		commands.put(NetworkCommand.GROUP_APPEND_CLUSTER, null);
		commands.put(NetworkCommand.GROUP_FINISH, null);
		commands.put(NetworkCommand.GROUP_DROP, null);
		commands.put(NetworkCommand.GROUP_DELETE, null);
		commands.put(NetworkCommand.GROUP_MOVE, null);
		commands.put(NetworkCommand.GROUP_MOVE_END, null);
		commands.put(NetworkCommand.GROUP_MOVE_START, null);
		commands.put(NetworkCommand.GROUP_SET_CHILD_GROUPS, null);
		commands.put(NetworkCommand.GROUP_SET_CHILD_STROKES, null);
		commands.put(NetworkCommand.GROUP_SET_CHILD_ARROWS, null);
		commands.put(NetworkCommand.GROUP_SET_PARENT, null);
		commands.put(NetworkCommand.GROUP_SET_PERM, null);
		commands.put(NetworkCommand.GROUP_RECTIFY, null);
		commands.put(NetworkCommand.GROUP_CIRCLIFY, null);
		commands.put(NetworkCommand.GROUP_CHILDREN_COLOR, null);
		commands.put(NetworkCommand.GROUP_LOAD, null);
		commands.put(NetworkCommand.GROUP_IMAGE_LOAD, null);
		commands.put(NetworkCommand.GROUP_IMAGE_DOWNLOAD, null);
		commands.put(NetworkCommand.GROUP_REQUEST_HASH_CHECK, null);
		commands.put(NetworkCommand.GROUP_COPY_TO_CANVAS, null);
		commands.put(NetworkCommand.GROUP_SET_TEXT, null);
		commands.put(NetworkCommand.GROUP_ROTATE, null);
		commands.put(NetworkCommand.GROUP_SCALE, null);
		commands.put(NetworkCommand.GROUP_CREATE_TEXT_GROUP, null);
		commands.put(NetworkCommand.GROUP_MAKE_RECTANGLE, null);
		commands.put(NetworkCommand.GROUP_COPY_WITH_MAPPINGS, null);
		
		//Arrow commands
		commands.put(NetworkCommand.ARROW_CREATE, null);
		commands.put(NetworkCommand.ARROW_DELETE, null);
		commands.put(NetworkCommand.ARROW_SET_TYPE, null);
		commands.put(NetworkCommand.ARROW_SET_COLOR, null);
		
		//Connector commands
		commands.put(NetworkCommand.CONNECTOR_LOAD, null);
		commands.put(NetworkCommand.CONNECTOR_DELETE, null);
		commands.put(NetworkCommand.CONNECTOR_LINEARIZE, null);
		commands.put(NetworkCommand.CONNECTOR_MOVE_ANCHOR, null);
		commands.put(NetworkCommand.CONNECTOR_MOVE_ANCHOR_START, null);
		commands.put(NetworkCommand.CONNECTOR_MOVE_ANCHOR_END, null);
		
		//Canvas Commands
		commands.put(NetworkCommand.CANVAS_SET, null);
		commands.put(NetworkCommand.CANVAS_LIST, null);
		commands.put(NetworkCommand.CANVAS_UNDO, null);
		commands.put(NetworkCommand.CANVAS_REDO, null);
		commands.put(NetworkCommand.CANVAS_CLEAR, null);
		commands.put(NetworkCommand.CANVAS_COPY, null);
		commands.put(NetworkCommand.CANVAS_LOCK, null);
		commands.put(NetworkCommand.CANVAS_LOAD, null);
		
		commands.put(NetworkCommand.LIST_CREATE, null);
		commands.put(NetworkCommand.LIST_LOAD, null);
		commands.put(NetworkCommand.LIST_CHECK_SET, null);
		
		//commands.put(NetworkCommand.IMAGE_TRANSFER, null);
		
		return commands;
	}
	
	private class CanvasPacket{
		
		public CalicoPacket packet;
		public Integer command;
		public Client client;
		
		public CanvasPacket(int command, Client client, CalicoPacket packet)
		{
			this.packet = packet;
			this.command = command;
			this.client = client;
		}		
	}
}
