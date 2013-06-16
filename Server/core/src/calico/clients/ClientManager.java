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
package calico.clients;


import java.nio.*;
import java.nio.channels.*;

import java.util.*;
import java.net.*;
import java.io.*;

import calico.*;
import calico.components.*;
import calico.controllers.*;
import calico.networking.*;
import calico.sessions.*;
import calico.uuid.UUIDAllocator;
import calico.networking.netstuff.*;
import calico.plugins.CalicoPluginManager;
import calico.plugins.CalicoStateElement;

import java.util.*;

import org.apache.log4j.Logger;

import it.unimi.dsi.fastutil.objects.*;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.longs.*;

public class ClientManager
{

	/**
	 * This maintains all the client thread objects
	 */
	private static Int2ReferenceAVLTreeMap<ClientThread> threads = new Int2ReferenceAVLTreeMap<ClientThread>();
	
	private static Object2IntArrayMap<InetSocketAddress> udpsockets2clientid = new Object2IntArrayMap<InetSocketAddress>();
	private static Long2IntArrayMap challenge2clientid = new Long2IntArrayMap();
	
	/**
	 * This keeps a list of the currently active client ids
	 */
	static IntArraySet clientids = new IntArraySet();
	
	/**
	 * This keeps a list of out of sync clients
	 */
	public static IntArraySet out_of_sync_clients = new IntArraySet();
	
	private static int nextClientID = 0;
	
	
	public static Logger logger = Logger.getLogger(ClientManager.class.getName());
	
	
	public static void setup()
	{
		challenge2clientid.defaultReturnValue(0);
		udpsockets2clientid.defaultReturnValue(0);
		ClientConsistencyListener.getInstance();
	}
	
	
	
	public static void setClientChallenge(int clientid, long challenge)
	{
		challenge2clientid.put(challenge, clientid);
	}
	public static int getClientFromChallenge(long challenge)
	{
		return challenge2clientid.get(challenge);
	}
	
	public static void setClientUDPSocket(InetSocketAddress sockAddress, int clientid)
	{
		udpsockets2clientid.put(sockAddress, clientid);
		
		getClientThread(clientid).setUDPSocketAddress(sockAddress);
	}
	
	
	public static Properties getClientProperties(int clientid, boolean withPrefix)
	{
		return getClientThread(clientid).toProperties(withPrefix);
	}
	public static Properties getClientProperties(int clientid)
	{
		return getClientProperties(clientid, false);
	}
	public static ClientThread getClientThread(int clientid) 
	{
		return threads.get(clientid);
	}

	/**
	 * This creates a new client thread, then adds the client to it.
	 * @param socket
	 */
	public static void newClientThread(Socket socket)
	{
//		int clientid = ++nextClientID;
		int clientid = new Long(UUIDAllocator.getUUID()).intValue();
		
		try
		{
			threads.put(clientid, new ClientThread(clientid, socket));
			getClientThread(clientid).start();
			clientids.add(clientid);
		}
		catch(IOException e)
		{
			clientids.remove(clientid);
			e.printStackTrace();
		}
		
		
	}//newClientThread
	
	
	
	public static void drop(int clientid, String message)
	{
//		clientids.remove(clientid);
		try
		{
			getClientThread(clientid).drop(message);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void kill(int clientid)
	{
		ClientManager.getClientThread(clientid).interrupt();
		drop(clientid, "Client with same name has connected from another location");
		
		if (out_of_sync_clients.contains(clientid))
			out_of_sync_clients.remove(clientid);
		
		clientids.remove(clientid);
	}
	
	public static void updateClientID(ClientThread client, int oldClientID)
	{
		int newID = client.getClientID();
		threads.remove(oldClientID);
		clientids.remove(oldClientID);
		
		if (threads.containsValue(client))
			threads.remove(client);
		
		threads.put(newID, client);
		clientids.add(newID);
	}
	
	
	
	
	public static void setUsername(int clientid, String username)
	{
		getClientThread(clientid).setUsername(username);
	}
	
	
	
	public static boolean checkAuth(String user, String pass)
	{
		// TODO We should probably implement this
		return true;
	}

	


	/**
	 * This will send a packet to all clients
	 * @param p
	 */
	public static void send(CalicoPacket p)
	{
		if(p==null)
			return;
		
		
		int clients[] = clientids.toIntArray().clone();
		
		for(int i=0;i<clients.length;i++)
		{
			send(clients[i], p);
		}
	}
	
	/**
	 * This will send a packet to all clients
	 * @param p
	 */
	public static void send(CalicoPacket[] p)
	{
		if(p==null)
			return;
		
		
		int clients[] = clientids.toIntArray().clone();
		
		for(int i=0;i<clients.length;i++)
		{
			send(clients[i], p);
		}
	}

	/**
	 * Sends a packet to a specific client
	 * @param c
	 * @param p
	 */
	public static void send(final Client c, CalicoPacket p)
	{
		send(c.getClientID(), p);
	}
	public static void send(final Client c, final CalicoPacket[] p)
	{
		send(c.getClientID(), p);
	}
	
	public static void send(final int clientid, final CalicoPacket p)
	{
		if(clientid==-1 || clientid==0 || p==null || !threads.containsKey(clientid))
			return;
		
		getClientThread(clientid).send(p);
	}
	
	public static void send(final int clientid, final CalicoPacket[] p)
	{
		if(clientid==-1 || clientid==0 || p==null || !threads.containsKey(clientid))
			return;
		
		for(int i=0;i<p.length;i++)
		{
			getClientThread(clientid).send(p[i]);
		}
	}

	public static void send_except(final Client c, final CalicoPacket p)
	{
		if(c==null)
			return;
		
		send_except( c.getClientID(), p );
	}
	
	
	public static void send_except(final int clientid, final CalicoPacket p)
	{
		if(p==null || clientid==-1 || clientid==0)
			return;
		
		int clients[] = clientids.toIntArray();
		
		for(int i=0;i<clients.length;i++)
		{
			if(clients[i]!=clientid)
			{
				send(clients[i], p);
			}
		}
	}
	
	
	/**
	 * @deprecated
	 * @see #get_client_string(int)
	 * @param c
	 * @return
	 */
	public static String client2string(int c)
	{
		return get_client_string(c);
	}
	
	public static String client2string(Client c)
	{
		return get_client_string(c.getClientID());
	}
	
	
	
	
	/**
	 * Add the client to the server, and send them an update.
	 * @param clientid
	 */
	public static void joinClient(int clientid)
	{
	
		// Notify the other users that a noob has joined
		ClientThread client = getClientThread(clientid);
		
		CalicoPacket resp2 = new CalicoPacket( );
		resp2.putInt(NetworkCommand.STATUS_MESSAGE);
		resp2.putString(client.getName()+" has joined the session!");
		
		// Send it to everyone else
		send_except(client.getClientID(), resp2);
		
		send_except(client.getClientID(), client.getClientInfoPacket());
		
		// TODO: Send the CLIENT_INFO Packet for this client (send to all) and send the new client all the other info
		
	}
	
	
	
	
	/**
	 * This sends the big consistency check to the client, to inform them of all things on the server
	 * @param client
	 */
	public static void sendConsistencyUpdate(Client client)
	{
		// Send away!
		ClientManager.send(client, CalicoPacket.command(NetworkCommand.CONSISTENCY_CHECK));
		
//		CalicoPacket packet = new CalicoPacket();
		
		/*
		 * - We want to send the list of canvases
		 * - The groups
		 * - The BGelements
		 * - the group parents
		 * - the BGelement parents
		 * - 
		 */
		
		//calico state elements
		for (CalicoStateElement element : CalicoPluginManager.calicoStateExtensions)
		{
			send(client, element.getCalicoStateElementUpdatePackets());
		}
		
		CalicoPacket default_email = CalicoPacket.getPacket(NetworkCommand.DEFAULT_EMAIL, COptions.server.default_email);
		ClientManager.send(client, default_email);
		ClientManager.send(client, CalicoPacket.getPacket(NetworkCommand.SERVER_HTTP_PORT, COptions.admin.listen.port));
		ClientManager.send(client, CalicoPacket.getPacket(NetworkCommand.SERVER_EMAIL_SETTINGS, 
				calico.COptions.server.email.smtpHost, 
				calico.COptions.server.email.smtpPort,
				calico.COptions.server.email.smtpsAuth,
				calico.COptions.server.email.replyToEmail,
				calico.COptions.server.email.username,
				calico.COptions.server.email.password));
		
		ClientManager.send(client, CalicoPacket.getPacket(NetworkCommand.CANVAS_SET_DIMENSIONS, COptions.canvas.width, COptions.canvas.height));
		
		
		long[] canvasids = CCanvasController.canvases.keySet().toLongArray();
		
		for(int j=0;j<canvasids.length;j++)
		{
			send(client, CalicoPacket.getPacket(NetworkCommand.CANVAS_LOAD_PROGRESS, j, canvasids.length));
			CCanvas can = CCanvasController.canvases.get(canvasids[j]);
			
			CalicoPacket[] packets = {};
			try
			{
				packets = can.getUpdatePackets();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			for(int i=0;i<packets.length;i++)
			{
				send(client, packets[i]);
			}
		}
		

		
		// Stuff!
		ClientManager.send(client, CalicoPacket.command(NetworkCommand.CONSISTENCY_FINISH));
		
		// Send the client list?
		int[] cidlist = clientids.toIntArray().clone();
		for(int i=0;i<cidlist.length;i++)
		{
			send(client, getClientThread(cidlist[i]).getClientInfoPacket());
		}
		
		
	}
	
	/**
	 * This sends the big consistency check to the client, to inform them of all things on the server
	 * @param client
	 */
	public static void sendConsistencyUpdate_continue(Client client, long lastSuccessfullySentUUID)
	{
		boolean foundUUID = false;
		// Send away!
//		ClientManager.send(client, CalicoPacket.command(NetworkCommand.CONSISTENCY_CHECK));
		ClientManager.send(client, CalicoPacket.getPacket(NetworkCommand.CONSISTENCY_CHECK_CONTINUE, lastSuccessfullySentUUID));
		
		/*
		 * TODO:
		 * - We want to send the list of canvases
		 * - The groups
		 * - The BGelements
		 * - the group parents
		 * - the BGelement parents
		 * - 
		 */
		
		long[] canvasids = CCanvasController.canvases.keySet().toLongArray();
		
		for(int j=0;j<canvasids.length;j++)
		{
		
			CCanvas can = CCanvasController.canvases.get(canvasids[j]);
			
			CalicoPacket[] packets = can.getUpdatePackets();
			for(int i=0;i<packets.length;i++)
			{
				if (!foundUUID)
				{
					if (packets[i].getUUID() != lastSuccessfullySentUUID)
						continue;
					else
						foundUUID = true;
				}
					
				send(client, packets[i]);
			}
		}
		
		
		// Stuff!
//		ClientManager.send(client, CalicoPacket.command(NetworkCommand.CONSISTENCY_FINISH));
		
		// Send the client list?
		int[] cidlist = clientids.toIntArray().clone();
		for(int i=0;i<cidlist.length;i++)
		{
			send(client, getClientThread(cidlist[i]).getClientInfoPacket());
		}
		
		
	}

	
	
	/**
	 * This sends the list of canvases to the client.
	 * THIS IS ONLY DONE AT THE START OF THE SESSION
	 * @param c
	 */
	public static void sendCanvasList(Client c)
	{
		long[] canvasids = CCanvasController.canvases.keySet().toLongArray();
		
		CalicoPacket p = new CalicoPacket();
		p.putInt(NetworkCommand.CHUNK_DATA);
		p.putInt(canvasids.length);
		
		for(int j=0;j<canvasids.length;j++)
		{
			byte[] bytes = CCanvasController.canvases.get(canvasids[j]).getInfoPacket().export();
			p.putInt(bytes.length);
			p.putByte(bytes);
//			ClientManager.send(c,CCanvasController.canvases.get(canvasids[j]).getInfoPacket());
		}
		ClientManager.send(c, p);
		//
	}
	
	
	
	public static String get_client_username(int clientid)
	{
		if (getClientThread(clientid) != null)
			return getClientThread(clientid).getUsername();
		
		return "";
	}
	
	public static String get_client_string(int clientid)
	{
		return getClientThread(clientid).toString();
	}
	
	public static String get_client_hostport(int clientid)
	{
		return getClientThread(clientid).getClient().getAddress().getHostAddress()+":"+getClientThread(clientid).getClient().getPort();
	}
	
	
	public static int[] get_clientids()
	{
		return clientids.toIntArray().clone();
	}
	
	
	
	public static void cleanup()
	{
		
		int clients[] = clientids.toIntArray();
		
		for(int i=0;i<clients.length;i++)
		{
			getClientThread(clients[i]).cleanup();
		}
	}
	
	

}//client


