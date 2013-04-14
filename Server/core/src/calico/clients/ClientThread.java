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

import calico.*;
import calico.networking.*;
import calico.networking.netstuff.*;
import calico.plugins.CalicoPluginManager;
import calico.plugins.events.clients.ClientDisconnect;
import calico.admin.*;
import calico.clients.*;
import calico.uuid.*;
import calico.components.*;
import calico.controllers.CCanvasController;
import calico.controllers.CGroupController;
import calico.sessions.*;

import java.nio.*;
import java.nio.channels.*;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.net.*;
import java.io.*;

import org.apache.log4j.Logger;

import it.unimi.dsi.fastutil.objects.*;
import it.unimi.dsi.fastutil.io.*;


// TODO: What if we had it sleep only after a certain number of loops with no data (that way its not sleepign when there is stuff queued up)?




public class ClientThread extends Thread
{
	public static Logger logger = Logger.getLogger(ClientThread.class.getName());

	private Socket sock = null;
	
	
	private Client client = null;
	
	private OutputStream out = null;
	private InputStream in = null;
	
	private long lastHearbeat = 0L;
	private long lastHeartbeatToClient = 0L;
	
	private int clientid = 0;
	
	private String clientStr = null;
	
	private String username = "null";

	private long totalBytesSent = 0;
	private long totalBytesRecv = 0;
	
	private InetSocketAddress udpSocketAddress = null;
	
	private long currentCanvasUUID = 0L;
	
	private long tempScrapUUID = 0L;
	
	private BlockingQueue<CalicoPacket> outboundPackets = new LinkedBlockingQueue<CalicoPacket>();
//	private ObjectArrayList<CalicoPacket> outboundPackets = new ObjectArrayList<CalicoPacket>();
	//private ObjectLinkedOpenHashSet<CalicoPacket> outboundPackets = new ObjectLinkedOpenHashSet<CalicoPacket>();


	public ClientThread(int clientid, Socket socket) throws IOException
	{
		super("ClientThread-"+clientid);
		this.clientid = clientid;
		this.sock = socket;
		this.sock.setSoTimeout((int) COptions.client.network.timeout);
		this.sock.setTcpNoDelay(true);
		
		this.client = new Client( this.sock.getInetAddress(), this.sock.getPort(), this.clientid );
		
		
		this.out = this.sock.getOutputStream();

		this.in = this.sock.getInputStream();
		
		makeClientString();

		ClientManager.logger.debug("CLIENT "+this.clientid+" TIMEOUT: "+this.sock.getSoTimeout());
		ClientManager.logger.debug("CLIENT "+this.clientid+" KEEPALIVE: "+this.sock.getKeepAlive());
		ClientManager.logger.debug("CLIENT "+this.clientid+" NODELAY: "+this.sock.getTcpNoDelay());
		//ClientManager.logger.debug("CLIENT "+this.clientid+" TIMEOUT: "+this.sock.getSoTimeout());
		
		// Add to the client->id thing
	}
	
	/**
	 * This is meant to be run ONLY ONCE by the constructor.
	 * The purpose of this method is to steal the outbound queue for a client if that client thread disconnected, and resend all data.
	 * It does this by checking if a new connection matches the same mac address as an old connection.
	 * @param client
	 */
	public void checkIfClientExists()
	{
		int[] clientIds = ClientManager.get_clientids();
		
		for (int i = 0; i < clientIds.length; i++)
		{
			if (ClientManager.getClientThread(clientIds[i]) != null 
					&& ClientManager.getClientThread(clientIds[i]).getClient().equals(client))
				continue;
			
			if (ClientManager.getClientThread(clientIds[i]) != null
					&& ClientManager.getClientThread(clientIds[i]).getClient().getUsername().compareTo(client.getUsername()) == 0)
			{
				//boot the old client thread with an error message
				
				CalicoServer.logger.debug("Client name conflict - booting old client");
//				int origID = clientid;
//				int preExistingID = ClientManager.getClientThread(clientIds[i]).getClientID();
//				CalicoServer.logger.debug("IP Checked... old client connected");
//				this.outboundPackets = ClientManager.getClientThread(clientIds[i]).outboundPackets;
				ClientManager.kill(clientIds[i]);
				ClientManager.getClientThread(clientIds[i]).interrupt();
				
				
//				ClientManager.clientids.remove(clientIds[i]);
//				clientid = preExistingID;
//				client.updateID(clientid);
				
//				ClientManager.updateClientID(this, origID);
				
//				logger.debug("Client has reconnected, closing previous socket and opening new one...");
			}
			{
				CalicoServer.logger.debug("IP Checked... new client connected");
			}
		}
	}
	
	public void setUDPSocketAddress(InetSocketAddress sockAddy)
	{
		this.udpSocketAddress = sockAddy;
	}
	
	private void makeClientString()
	{
		this.clientStr = this.username+this.client.toString();
	}
	
	public boolean setCurrentCanvasUUID(long uuid)
	{
		boolean changed = false;
		if(this.currentCanvasUUID!=uuid) 
		{
			changed = true;
		}
		this.currentCanvasUUID = uuid;
		
		return changed;
	}
	public long getCurrentCanvasUUID()
	{
		return this.currentCanvasUUID;
	}
	
	
	public Client getClient()
	{
		return this.client;
	}
	public int getClientID()
	{
		return this.clientid;
	}
	public String getUsername()
	{
		return this.username;
	}
	public void setUsername(String username)
	{
		this.username = username;
		makeClientString();
	}
	
	public void setTempScrapUUID(long uuid)
	{
		tempScrapUUID = uuid;
	}
	
	public String toString()
	{
		return this.clientStr;
	}
		
	
	public void drop(String message)
	{
		CalicoPluginManager.sendEventToPlugins(new ClientDisconnect(this.client));
		
		// Remove the client from the canvas they are in
		if( this.currentCanvasUUID != 0L )
		{
			CCanvasController.canvases.get(this.currentCanvasUUID).removeClient(this.clientid);
		}
		
		try
		{
			sendInternal(CalicoPacket.getPacket(NetworkCommand.LEAVE, message));
		}
		catch (Exception e)
		{
			
		}
		
		try
		{
			
			
			this.sock.close();
			this.out.close();
			this.in.close();
		}
		catch(Exception e)
		{
			
		}
	}
	
	
	public void send(CalicoPacket p)
	{
		// Add the packet to the send buffer
		long currTime = (new Date()).getTime();
		if (currTime - this.lastHearbeat > 900000)
		{
			ClientManager.kill(this.clientid);
		}
		this.outboundPackets.add(p);
	}
	
	private void sendInternal(CalicoPacket p) throws SocketException, IOException
	{
		// If it is null, then we don't even bother
		if(p==null)
		{
			return; 
		}

		// Packet Size
		byte[] sizeArray = new byte[ByteUtils.SIZE_OF_INT];
		ByteUtils.writeInt(sizeArray, p.getLength(), 0);
		BinIO.storeBytes(sizeArray, this.out);
		
		this.totalBytesSent = this.totalBytesSent + p.getLength() + ByteUtils.SIZE_OF_INT;
		
		// Send it
		BinIO.storeBytes(p.getBuffer(), this.out);
		
		// Need to update the hearbeat
		this.lastHearbeat = System.currentTimeMillis();
		
		// Log it
//		if(ProcessQueue.logger.isDebugEnabled())
//		{
//			ProcessQueue.logger.debug("TX \""+toString()+"\" "+p.toString());
//		}
	}

	private byte[] old_getPacket() throws IOException
	{
		// Get the packet size
		byte[] data = new byte[ByteUtils.SIZE_OF_INT];
		BinIO.loadBytes(this.in, data);
		
		
		int size = ByteUtils.readInt(data, 0);
		
		//CalicoServer.logger.debug("PACKET SIZE: "+size);
		
		// Load the packet
		byte[] data2 = new byte[size];
		BinIO.loadBytes(this.in,data2);

		return data2;
	}
	
	public CalicoPacket getClientInfoPacket() 
	{
		return CalicoPacket.getPacket(NetworkCommand.CLIENT_INFO, this.clientid, this.username);
	}
	
	private CalicoPacket getPacket() throws IOException
	{
		// Get the packet size
		byte[] data = new byte[ByteUtils.SIZE_OF_INT];
		BinIO.loadBytes(this.in, data);
		
		
		int size = ByteUtils.readInt(data, 0);
		
		//CalicoServer.logger.debug("PACKET SIZE: "+size);
		CalicoPacket packet = new CalicoPacket( size );
		
		int available = in.available();
		int offset = 0;
		
		try
		{
			while (offset < size)
			{
	//			logger.info("Input stream doesn't have enough byte, blocking for 10ms. (Need: " + 4 + ", has: " + available);
				Thread.sleep(COptions.client.threadopts.sleeptime);
				available = in.available();
				if (offset + available > size)
					available = size - offset;
				BinIO.loadBytes(this.in, packet.getBuffer(), offset, available);
				offset += available;
				if (size > COptions.client.network.cluster_size)
				{
					System.out.println("Loading large packet (" + size + "), " + ((double)offset)/((double)size) * 100 + " percent complete");
				}
			}
		}
		catch (Exception e)
		{
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		
		
//		BinIO.loadBytes(this.in, packet.getBuffer());
		

		this.totalBytesRecv = this.totalBytesRecv + size + ByteUtils.SIZE_OF_INT;

		return packet;
	}
	
	private void sendPacketQueue() throws IOException
	{
		sendPacketQueue(0);
	}
	
	private void sendPacketQueue(int depth) throws IOException
	{
		if(!this.outboundPackets.isEmpty())
		{
			try {
				sendInternal(this.outboundPackets.take());
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(depth<20)
			{
				sendPacketQueue(++depth);
			}
		}
	}
	
	
	public void run()
	{
		try
		{
			// we need to setup a first heartbeat (otherwise they will always timeout)
			this.lastHearbeat = System.currentTimeMillis();
			
			// Loop forever
			while(true)
			{
				
				// Are they disconnected?
				if(!this.sock.isConnected() || this.sock.isClosed() || this.sock.isInputShutdown() || this.sock.isOutputShutdown())
				{
					throw new ClientTimedOutException();
				}
				else if((System.currentTimeMillis()-this.lastHearbeat)>=COptions.client.network.timeout)
				{
					throw new ClientTimedOutException();
				}
				
			
				// do we have any outbound things to send?
				if(!this.outboundPackets.isEmpty())
				{
					// we have things to send!
					sendPacketQueue();
					//sendInternal(this.outboundPackets.remove(0));
					//ClientManager.logger.debug("CLIENT "+toString()+" SEND QUEUE SIZE IS "+this.outboundPackets.size());
				}
				else if (System.currentTimeMillis() - lastHeartbeatToClient > 2000)
				{
					send(CalicoPacket.getPacket(NetworkCommand.HEARTBEAT));
					sendPacketQueue();
					lastHeartbeatToClient = System.currentTimeMillis();
				}
				
				// Do we have anything to read?
				int availableInboundBytes = this.in.available();
				if(availableInboundBytes>=4)
				{
					// Ok, read the packet
					CalicoPacket packet = getPacket();//new CalicoPacket( getPacket() );
					
					// Update the hearbeat
					
	
					// What kind is it? (We can ignore the heartbeat packets)
					int com = packet.getInt();
//					if(com==NetworkCommand.HEARTBEAT)
//					{
////						System.out.println("Heart beat");
//						// We can just ignore this
//					}
//					else
//					{
						try
						{
							//Only canvas specific commands are sent to a canvas thread. 
							//if (com >= 200 && com <= 3000 && currentCanvasUUID != 0l && com != 1200)
							if (currentCanvasUUID != 0l && CalicoServer.canvasCommands.containsKey(com))
							{
								synchronized(CalicoServer.canvasThreads)
								{
									if (!CalicoServer.canvasThreads.containsKey(currentCanvasUUID))
									{
										CalicoServer.canvasThreads.put(currentCanvasUUID, new CanvasThread(currentCanvasUUID));
									}
									CalicoServer.canvasThreads.get(currentCanvasUUID).addPacketToQueue(com, this.client, packet);
								}
							}
							else
								ProcessQueue.receive(com, this.client, packet);
						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
//					}
					this.lastHearbeat = System.currentTimeMillis();
				}
				
				try
				{
					// CPU Limiter (This prevents the CPU from going bonkers)
					Thread.sleep(COptions.client.threadopts.sleeptime);
				}
				catch(InterruptedException e)
				{
					logger.warn("ClientThread " + clientid + " interrupted");
				}
				
				
				
			}
		}
		catch(ClientTimedOutException e)
		{
			ClientManager.logger.info("\""+toString()+"\" has timed out");
			
			CalicoPacket resp2 = new CalicoPacket();
			resp2.putInt(NetworkCommand.STATUS_MESSAGE);
			resp2.putString(getUsername()+" has timed out");
			// Send it to everyone else
			ClientManager.send_except(this.clientid, resp2);
		}
		catch (SocketException e)
		{
			if (e.getMessage().startsWith("Connection reset by peer"))
			{
				System.out.println("Client \"" + toString() + "\" closed the socket. Service thread exiting.");
			}
			else
			{
				e.printStackTrace();
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		finally
		{
			if (tempScrapUUID != 0l && CGroupController.exists(tempScrapUUID)
					&& !CGroupController.groups.get(tempScrapUUID).isPermanent())
			{
				CGroupController.drop(tempScrapUUID);
				tempScrapUUID = 0L;
			}
			//System.out.println("CALLING THE FINALLY");
			ClientManager.drop(clientid, "");
		}
	}
	
	
	public void cleanup()
	{
		if(this.outboundPackets.isEmpty())
		{
//			this.outboundPackets.trim();
		}
	}
	
	
	public Properties toProperties()
	{
		return toProperties(false);
	}
	
	public Properties toProperties(boolean withPrefix)
	{
		Properties props = new Properties();
		String prefix = "";
		if(withPrefix)
		{
			prefix = "client."+this.clientid+".";
		}
		
		props.setProperty(prefix+"clientid", ""+this.clientid);
		props.setProperty(prefix+"username", this.username);
		props.setProperty(prefix+"heartbeat", ""+(System.currentTimeMillis() - this.lastHearbeat));
		
		props.setProperty(prefix+"bytes_sent", ""+this.totalBytesSent);
		props.setProperty(prefix+"bytes_recv", ""+this.totalBytesRecv);
		
		props.setProperty(prefix+"tcp.host", ""+this.sock.getInetAddress().getHostAddress());
		props.setProperty(prefix+"tcp.port", ""+this.sock.getPort());
		try
		{
			props.setProperty(prefix+"tcp.timeout", ""+this.sock.getSoTimeout());
			props.setProperty(prefix+"tcp.linger", ""+this.sock.getSoLinger());
			props.setProperty(prefix+"tcp.keepalive", ""+this.sock.getKeepAlive());
			props.setProperty(prefix+"tcp.oobinline", ""+this.sock.getOOBInline());
			props.setProperty(prefix+"tcp.recvbuffer", ""+this.sock.getReceiveBufferSize());
			props.setProperty(prefix+"tcp.sendbuffer", ""+this.sock.getSendBufferSize());
			props.setProperty(prefix+"tcp.nodelay", ""+this.sock.getTcpNoDelay());
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		try
		{
			props.setProperty(prefix+"udp.host", ""+this.udpSocketAddress.getHostName());
			props.setProperty(prefix+"udp.port", ""+this.udpSocketAddress.getPort());
		}
		catch(Exception e)
		{
			props.setProperty(prefix+"udp.host", "null");
			props.setProperty(prefix+"udp.port", "0");
		}
		
		return props;
	}
	
}


