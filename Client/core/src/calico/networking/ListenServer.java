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

import it.unimi.dsi.fastutil.longs.LongIterator;

import java.io.InputStream;

import javax.swing.ProgressMonitor;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import calico.CalicoDataStore;
import calico.CalicoOptions;
import calico.controllers.CCanvasController;
import calico.events.CalicoEventHandler;
import calico.networking.netstuff.ByteUtils;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;


/**
 * This reads packets from the network and then adds them to the packet queue
 *
 * @author Mitch Dempsey
 */
public class ListenServer implements Runnable
{

	public static Logger logger = Logger.getLogger(ListenServer.class.getName());
	
	private InputStream in = null;

	
	private long lastHeartBeatFromServer;
	
	public ListenServer()
	{
		openInputStream();
		(new ListenServerQueue()).start();
		
		
//			CalicoEventHandler.getInstance().fireEvent(NetworkCommand.STATUS_SENDING_LARGE_FILE_START, CalicoPacket.getPacket(NetworkCommand.STATUS_SENDING_LARGE_FILE_START, ((double)0), 
//					((double)100), "Synchronizing with server... "));
		
	}

	private void openInputStream() {
		try
		{
			in = Networking.socket.getInputStream();
		}
		catch(Exception e)
		{
			logger.error("Could not get input stream");
		}
	}
	
	public void run()
	{
		logger.debug("ListenServer thread is starting.");
//		try
//		{
		
			while(true)
			{
				lastHeartBeatFromServer = System.currentTimeMillis();
				try {
					if (!Networking.socket.isConnected())
					{
						throw new CalicoLostServerConnectionException();
					}
					else if((System.currentTimeMillis()-this.lastHeartBeatFromServer)>=CalicoOptions.network.timeout)
					{
						logger.debug("Server timeout: " + (System.currentTimeMillis()-this.lastHeartBeatFromServer));
						throw new CalicoLostServerConnectionException();
					}
					byte[] data = new byte[ByteUtils.SIZE_OF_INT];
					
					Networking.receivingPacketsFromServer = true;
					int available = in.available();
					while (available < 4)
					{
	//					logger.info("Input stream doesn't have enough byte, blocking for 10ms. (Need: " + 4 + ", has: " + available);
						Thread.sleep(1l);
						available = in.available();
						
						if((System.currentTimeMillis()-this.lastHeartBeatFromServer)>=CalicoOptions.network.timeout)
						{
							logger.debug("Server timeout: " + (System.currentTimeMillis()-this.lastHeartBeatFromServer));
							throw new CalicoLostServerConnectionException();
						}
						if (Networking.sendingPacketsToServer)
							lastHeartBeatFromServer = System.currentTimeMillis();
						
					}
					in.read(data);
					
					int size = ByteUtils.readInt(data, 0);
					
					if(size<=0)
					{
						logger.debug("PACKET SIZE: "+size);
						throw new CalicoLostServerConnectionException();
					}
					
					CalicoPacket tpack = new CalicoPacket(size);
					available = in.available();
					
					int offset = 0;
					
					boolean showProgressBar = false; //size > CalicoOptions.network.cluster_size;
					String msg = "Downloading large message from the server, please wait...";
					
					if (showProgressBar)
						CalicoEventHandler.getInstance().fireEvent(NetworkCommand.STATUS_SENDING_LARGE_FILE_START, CalicoPacket.getPacket(NetworkCommand.STATUS_SENDING_LARGE_FILE_START, 0, 1, msg));
					while (offset < size)
					{
			//			logger.info("Input stream doesn't have enough byte, blocking for 10ms. (Need: " + 4 + ", has: " + available);
						Thread.sleep(1l);
						available = in.available();
						if (offset + available > size)
							available = size - offset;
						if (tpack.getBufferSize() > 0)
							in.read(tpack.getBuffer(), offset, available);
						offset += available;

				        if (showProgressBar)
							CalicoEventHandler.getInstance().fireEvent(NetworkCommand.STATUS_SENDING_LARGE_FILE, 
									CalicoPacket.getPacket(NetworkCommand.STATUS_SENDING_LARGE_FILE, (double)offset, 
									(double)size, msg));
					}
					if (showProgressBar)
						CalicoEventHandler.getInstance().fireEvent(NetworkCommand.STATUS_SENDING_LARGE_FILE_FINISHED, CalicoPacket.getPacket(NetworkCommand.STATUS_SENDING_LARGE_FILE_FINISHED, 1d, 1d, msg));
					
					
					if(logger.isTraceEnabled() && tpack.getBufferSize() > 0)
					{
						logger.trace("rx " + tpack.toString());
					}
					
					// Catching this here so that it doesn't disconnect the client
					try
					{
						if (tpack.getBufferSize() > 0)
							Networking.recvQueue.offer(tpack);
//						PacketHandler.receive(tpack);
					}
					catch (Exception e)
					{
						logger.warn("Failed to process packet " + tpack.toString());
						e.printStackTrace();
					}
					Networking.receivingPacketsFromServer = false;

				}
				catch (InterruptedException e1) {
					e1.printStackTrace();
					logger.debug("ListenServer thread was interrupted.");
					break;
				}
				catch(Exception e)
				{

					try {
						Thread.sleep(1000l);

					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					if (!Networking.socket.isConnected())
						continue;

					openInputStream();
				}
				
			}//while
	}
	

}


