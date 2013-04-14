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

import it.unimi.dsi.fastutil.objects.*;

import java.io.IOException;
import java.net.*;

import calico.networking.netstuff.*;
import calico.*;

public class UDPReceiveQueue implements Runnable
{
	public static ObjectArrayList<CalicoPacket> receiveQueue = new ObjectArrayList<CalicoPacket>();
	
	private byte[] packetData = new byte[2048];
	private DatagramSocket socket = null;
	
	public UDPReceiveQueue()
	{
		try
		{
			socket = new DatagramSocket( new InetSocketAddress(COptions.listen.host, COptions.listen.port) );
		}
		catch (SocketException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void send(DatagramPacket packet)
	{
		try
		{
			this.socket.send(packet);
		}
		catch(IOException e){}
	}
	
	public void run()
	{
		try
		{
			while(true)
			{
			
				DatagramPacket receivePacket = new DatagramPacket(this.packetData, this.packetData.length);
				if (socket == null)
					continue;
				
                this.socket.receive(receivePacket);
                
                int psize = ByteUtils.readInt(this.packetData, 0);
                
                CalicoPacket packet = new CalicoPacket(this.packetData, ByteUtils.SIZE_OF_INT, psize);
                UDPPacketHandler.receive(packet, (InetSocketAddress) receivePacket.getSocketAddress());
                //CalicoServer.logger.debug("UDP RECEIVE: "+packet.toString());
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			try
			{
				Thread.sleep(20L);
			}
			catch(Exception e2)
			{
				
			}
			run();
		}
	}
}
