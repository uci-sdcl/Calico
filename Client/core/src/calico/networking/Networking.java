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

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.net.*;
import java.util.*;

import calico.*;
import calico.components.*;
import calico.controllers.CCanvasController;
import calico.modules.*;
import calico.networking.netstuff.*;

import java.util.concurrent.*;
// Has methods to send to the server
// also is used to listen to all requests

import org.apache.log4j.Logger;

public class Networking
{
	public enum ConnectionState {Disconnected, Connecting, ReConnecting, Connected};
	
	public static final int MAX_PACKET_SIZE = 1400;
		
//	Queue buffer = 
	public static BlockingQueue<CalicoPacket> recvQueue = new LinkedBlockingQueue<CalicoPacket>();
	public static BlockingQueue<CalicoPacket> sendQueue = new LinkedBlockingQueue<CalicoPacket>();
//	public static ArrayBlockingQueue<CalicoPacket> recvQueue = new ArrayBlockingQueue<CalicoPacket>(1000);
//	public static ArrayBlockingQueue<CalicoPacket> sendQueue = new ArrayBlockingQueue<CalicoPacket>(1000);

	public static DatagramSocket udpSocket = null;
	public static DatagramChannel udpChannel = null;
	
	public static Socket socket = null;
	
	public static InetSocketAddress udpServerAddress = null;
	
	public static ThreadGroup networkThreadGroup = new ThreadGroup("Calico Network");
	
	public static Thread sendPacketThread = null;
	public static Thread receivePacketThread = null;
	
	
	
	public static long udpChallenge = 0;

	public static ConnectionState connectionState = ConnectionState.Disconnected;
	public static long timeConnected = 0l;
	public static long lastConnectionAttempt = 0l;
	
	public static boolean synchroized = true;
	public static int timesFailed = 0;
	public static boolean ignoreConsistencyCheck = false;
	public static long lastResync = 0l;

	public static boolean sendingPacketsToServer;
	public static boolean receivingPacketsFromServer;
	
	public static long ip2long(String ip)
	{
		//A.B.C.D = D + (C * 256) + (B * 256 * 256) + (A * 256 * 256 * 256)
		String[] addyparts = ip.split(".");
		
		long part1 = Long.parseLong(addyparts[0]);
		long part2 = Long.parseLong(addyparts[1]);
		long part3 = Long.parseLong(addyparts[2]);
		long part4 = Long.parseLong(addyparts[3]);
		
		return part4 + (part3*256) + (part2*256*256) + (part1*256*256*256);
	}
	
	
	/**
	 * This initiates the networking object that is used 
	 */
	public static void setup()
	{
		try
		{	
			
			
			if(receivePacketThread!=null && receivePacketThread.isAlive()) {
				receivePacketThread.interrupt();
			}
			if(sendPacketThread!=null && sendPacketThread.isAlive()) {
				sendPacketThread.interrupt();
			}
			connectToServer();
			//udpChannel = DatagramChannel.open();
			//udpChannel.connect();
			//udpSocket = udpChannel.socket();
			
			
			//networkThreadGroup = new ThreadGroup("Calico Network");
			
			// Start the heartbeat server (to let the server know we are alive!)
			//Heartbeat hb = new Heartbeat();
			//hb.start();
			
			// Gotta listen for those incoming packets and stuff
			receivePacketThread = null;
			receivePacketThread = new Thread(networkThreadGroup, new ListenServer(),"ReceivePackets");
			receivePacketThread.start();
			
			// gotta process them packets
			//ListenServerQueue lsq = new ListenServerQueue();
			//lsq.start();
			
			// Queue the outbound packets
			sendPacketThread = null;
			sendPacketThread = new Thread(networkThreadGroup, new SendPackets(),"SendPackets");
			sendPacketThread.start();
			
			
			
			//new Thread(networkThreadGroup, new UDPListenServer(), "UDP Listener").start();
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
			ErrorMessage.fatal("Network setup error.");
		}
	}

	public static void connectToServer() {
		try
		{
			if (System.currentTimeMillis() - lastConnectionAttempt < 1000)
			{
				try 
				{
					Thread.sleep(1000l);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			
			if(socket!=null && socket.isConnected())
			{
				socket.close();
			}
			
			if (Networking.connectionState == Networking.ConnectionState.Connected)
			{
				Networking.connectionState = Networking.ConnectionState.Connecting;
				CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).drawMenuBars();
			}
			
			Random rand = new Random();
			udpChallenge = rand.nextLong();
			
			socket = new Socket();
			socket.setReceiveBufferSize(10240000);
			socket.connect(new InetSocketAddress(CalicoDataStore.ServerHost, CalicoDataStore.ServerPort));
			socket.setSoLinger(false,0);
			socket.setTcpNoDelay(true);
			socket.setSoTimeout(0);
			//socket = new Socket(CalicoDataStore.ServerHost, CalicoDataStore.ServerPort);
			
			Calico.logger.debug("SOCKET RBUF: "+socket.getReceiveBufferSize());
			Calico.logger.debug("SOCKET SBUF: "+socket.getSendBufferSize());
			Calico.logger.debug("SOCKET LINGER: "+socket.getSoLinger());
			Calico.logger.debug("SOCKET TIMEOUT: "+socket.getSoTimeout());
			Calico.logger.debug("SOCKET KEEPALIVE: "+socket.getKeepAlive());
			
			
			udpServerAddress = new InetSocketAddress(InetAddress.getByName(CalicoDataStore.ServerHost), CalicoDataStore.ServerPort);
			
			
			udpSocket = new DatagramSocket();
			
			lastConnectionAttempt = System.currentTimeMillis();
		}
		catch (IOException e)
		{
			Logger.getLogger(ListenServer.class.getName()).debug("Unable to connect to server!");
//			e.printStackTrace(System.err);
		}
	}
	
	//////////// COMMANDS/////////////
	
	public static boolean join()
	{
		return send(CalicoPacket.getPacket(NetworkCommand.JOIN, 
				CalicoDataStore.Username,
				CalicoDataStore.Password
		));
	}
	
	public static boolean consistency_check()
	{
		return send( CalicoPacket.command(NetworkCommand.CONSISTENCY_CHECK) );
	}
	
	public static boolean leave()
	{
		return send(CalicoPacket.command(NetworkCommand.LEAVE) );
	}

	
	//////////// END COMMANDS //////////
	
	
	public static int packetSize(int com, Object... parts)
	{
		int size = 4;
		
		for(int i=0;i<parts.length;i++)
		{
			if( parts[i] instanceof Integer)
			{
				size = size + 4;
			}
			else if ( parts[i] instanceof String )
			{
				size = size + (2 * ((String)parts[i]).toCharArray().length );
			}
			else if ( parts[i] instanceof Float )
			{
				size = size + 4;
			}
			else if ( parts[i] instanceof Double )
			{
				size = size + 8;
			}
			else if ( parts[i] instanceof Long )
			{
				size = size + 8;
			}
			else if ( parts[i] instanceof Short )
			{
				size = size + 2;
			}
			else if ( parts[i] instanceof Character )
			{
				size = size + 2;
			}
			else if ( parts[i] instanceof Byte )
			{
				size = size + 1;
			}
		}
		return size;
	}
	
	
	public static boolean send(int com, Object... parts)
	{
		
		
		CalicoPacket p = new CalicoPacket();
		p.putInt(com);
		
		//long start = System.nanoTime();
		
		for(int i=0;i<parts.length;i++)
		{
			if( parts[i] instanceof Integer)
			{
				p.putInt( ((Integer)parts[i]).intValue() );
			}
			else if ( parts[i] instanceof String )
			{
				p.putString( (String) parts[i] );
			}
			else if ( parts[i] instanceof Float )
			{
				p.putFloat( ((Float)parts[i]).floatValue() );
			}
			else if ( parts[i] instanceof Double )
			{
				p.putDouble( ((Double)parts[i]).doubleValue() );
			}
			else if ( parts[i] instanceof Long )
			{
				p.putLong( ((Long) parts[i]).longValue() );
			}
			else if ( parts[i] instanceof Byte )
			{
				p.putByte( ((Byte) parts[i]).byteValue() );
			}
		}
		return send(p);
	}

	
	public static boolean send(CalicoPacket pkt)
	{
		try
		{
			sendQueue.offer(pkt);
			return true;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	public static void udpSend(CalicoPacket pkt)
	{
		try
		{
			byte[] data = pkt.exportWithSize();
			DatagramPacket dpack = new DatagramPacket(data, data.length, udpServerAddress  );
			udpSocket.send(dpack);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	
}//Networking
