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

import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.net.*;

import org.apache.log4j.Logger;

import calico.Calico;
import calico.CalicoDataStore;
import calico.controllers.CCanvasController;
import calico.controllers.CGroupController;
import calico.events.CalicoEventHandler;
import calico.modules.*;
import calico.networking.netstuff.*;

import it.unimi.dsi.fastutil.io.*;


public class SendPackets implements Runnable
{
	public static Logger logger = Logger.getLogger(SendPackets.class.getName());
	
	private OutputStream out = null;
	private DatagramSocket udp_sock = null;
	private InetSocketAddress serverAddress = null;
	
	public SendPackets()
	{
		openOutputStream();
	}

	private void openOutputStream() {
		try
		{
			out = Networking.socket.getOutputStream();
			this.serverAddress = new InetSocketAddress(CalicoDataStore.ServerHost, CalicoDataStore.ServerPort);
			udp_sock = new DatagramSocket();//this.serverAddress);
		}
		catch(Exception e)
		{
			logger.error("Could not get output stream");
		}
	}
	
	public void run()
	{
		byte[] packetSize = new byte[4];
		
		logger.debug("Starting send packets thread");
		
		while (true)
		{
			if (CalicoDataStore.RunStressTest && System.currentTimeMillis() - CalicoDataStore.timeLastStressPacketSent > CalicoDataStore.StressTestInterval)
			{
				CGroupController.sendRandomTestPacket();
				CalicoDataStore.timeLastStressPacketSent = System.currentTimeMillis();
			}
			
			try
			{
				if (!Networking.socket.isConnected())
				{
//					logger.debug("Socket is not connected?");
					throw new Exception();
				}
				Networking.sendingPacketsToServer = true;
				
				CalicoPacket packet = Networking.sendQueue.poll(2, TimeUnit.SECONDS);
				if(packet==null)
				{
					// Timed out, so we can send a heartbeat
					long currentCanvas = CCanvasController.getCurrentUUID();
					int canvasSignature = 0;
					if (currentCanvas != 0l)
						canvasSignature = CCanvasController.canvasdb.get(currentCanvas).getSignature();
					packet = CalicoPacket.getPacket(NetworkCommand.HEARTBEAT, currentCanvas, canvasSignature);
				}
				
				ByteUtils.writeInt(packetSize, packet.getLength(), 0);

				BinIO.storeBytes(packetSize, out);
				
				if (packet.getLength() < calico.CalicoOptions.network.cluster_size)
					BinIO.storeBytes(packet.getBuffer(), out);
				else
				{
					System.out.println();
					int offSet = 0;
					
					
					CalicoEventHandler.getInstance().fireEvent(NetworkCommand.STATUS_SENDING_LARGE_FILE_START, CalicoPacket.getPacket(NetworkCommand.STATUS_SENDING_LARGE_FILE_START, 0, 1, "Sending large file to server... "));
					for (offSet = 0; offSet < packet.getLength(); offSet += calico.CalicoOptions.network.cluster_size)
					{
						int length = (offSet + calico.CalicoOptions.network.cluster_size < packet.getLength())
										? calico.CalicoOptions.network.cluster_size
										: packet.getLength() - offSet;
						BinIO.storeBytes(packet.getBuffer(), offSet, length, out);
						CalicoEventHandler.getInstance().fireEvent(NetworkCommand.STATUS_SENDING_LARGE_FILE, 
								CalicoPacket.getPacket(NetworkCommand.STATUS_SENDING_LARGE_FILE, ((double)offSet + length), 
								((double)packet.getLength()), "Sending large file to server... "));						
					}
					CalicoEventHandler.getInstance().fireEvent(NetworkCommand.STATUS_SENDING_LARGE_FILE_FINISHED, CalicoPacket.getPacket(NetworkCommand.STATUS_SENDING_LARGE_FILE_FINISHED, 1, 1, "Sending large file to server... "));
				}
					
				
				byte[] ptemp = packet.exportWithSize();
				
				Networking.sendingPacketsToServer = false;
				
				//DatagramPacket dpacket = new DatagramPacket(ptemp, ptemp.length, this.serverAddress);
				//this.udp_sock.send(dpacket);
				
				
				//byte[] tmp = packet.exportWithSize();
				//BinIO.storeBytes(tmp, out);
			}
			catch(InterruptedException e) 
			{
				logger.error("SendPackets thread was interrupted.");
				try
				{
					this.out.close();
				}
				catch (IOException e1)
				{
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				break;
			}
			catch(Exception e)
			{
				//				logger.error("Output stream lost connection to the server! Attempting to reconnect...");
				//				e.printStackTrace();

				//					Networking.connectToServer();
				if (Networking.connectionState == Networking.ConnectionState.Connected)
				{
					Networking.connectionState = Networking.ConnectionState.Connecting;
					CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).drawMenuBars();
					ErrorMessage.popup("Warning, you have been disconnected!");
				}
				try {
					Thread.sleep(1000l);
					logger.debug("Lost connection to server! Attempting to connect in: ");
					logger.debug("5... ");
					Thread.sleep(1000l);
					logger.debug("4... ");
					Thread.sleep(1000l);
					logger.debug("3... ");
					Thread.sleep(1000l);
					logger.debug("2... ");
					Thread.sleep(1000l);
					logger.debug("1... ");
					Thread.sleep(1000l);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				Networking.connectToServer();
				//					Thread.sleep(1000l);
				if (!Networking.socket.isConnected())
					continue;

				openOutputStream();
				Networking.join();
				//Need to inform the server what canvas we were in so it uses the right thread.
				if (CCanvasController.getCurrentUUID() != 0l)
					Networking.send(NetworkCommand.PRESENCE_VIEW_CANVAS, CCanvasController.getCurrentUUID());

			}
		}
		
//		catch(SocketException se)
//		{
//			se.printStackTrace();
//			ErrorMessage.fatal("Connection to server was lost!");
//		}
//		catch(Exception e)
//		{
//			e.printStackTrace();
//			ErrorMessage.fatal("Connection to server was lost!");
//		}
	}
	
}
