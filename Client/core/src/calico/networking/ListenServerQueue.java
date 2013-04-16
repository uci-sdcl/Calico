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

import java.util.concurrent.TimeUnit;

import javax.swing.ProgressMonitor;

import calico.*;
import calico.components.*;
import calico.modules.*;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;

/**
 * This reads packets from the queue and then sends them to the handler
 *
 * @author Mitch Dempsey
 */
public class ListenServerQueue extends Thread
{
	private static ProgressMonitor progressMonitor;
	private int previousProgress = 0;
	
	public void start()
	{
		super.start();
		
		startProgressMonitor();
	}
	
	public void run()
	{
		CalicoPacket tpack;
		while(true)
		{
			try
			{
				while(true)
				{
					tpack = Networking.recvQueue.poll(2, TimeUnit.MILLISECONDS);
					if (tpack != null)
					{
						PacketHandler.receive(tpack);
						
						if (Networking.connectionState != Networking.ConnectionState.Connected)
						{
							if (progressMonitor == null)
							{
//								CalicoEventHandler.getInstance().fireEvent(NetworkCommand.STATUS_SENDING_LARGE_FILE_START, CalicoPacket.getPacket(NetworkCommand.STATUS_SENDING_LARGE_FILE_START, ((double)0), 
//										((double)100), "Synchronizing with server... "));
								startProgressMonitor();
							}
							else if (tpack.getCommand() == NetworkCommand.CONSISTENCY_FINISH)
							{
//								CalicoEventHandler.getInstance().fireEvent(NetworkCommand.STATUS_SENDING_LARGE_FILE_FINISHED, CalicoPacket.getPacket(NetworkCommand.STATUS_SENDING_LARGE_FILE_FINISHED, ((double)100), 
//										((double)100), "Synchronizing with server... "));
								progressMonitor.close();
								progressMonitor = null;
								Networking.connectionState = Networking.ConnectionState.Connected;
								continue;
							}
							else if (progressMonitor.isCanceled()) {
				                System.exit(0);
				            }
							double cuuid = new Long(tpack.getCUUID()).doubleValue();
							int progress = 1;
							if (tpack.getCommand() == NetworkCommand.CANVAS_LOAD_PROGRESS)
							{
							
							tpack.rewind();
							tpack.getInt();
							int canvasPos = tpack.getInt();
							int totalCanvases = tpack.getInt();
							
							progress = new Double(((canvasPos*100d) / (totalCanvases*100d)) * 100).intValue();
							}
							if (progress < previousProgress && progress < 101)
								progress = previousProgress;
//							int progress = 0;
							
//							CalicoEventHandler.getInstance().fireEvent(NetworkCommand.STATUS_SENDING_LARGE_FILE, 
//									CalicoPacket.getPacket(NetworkCommand.STATUS_SENDING_LARGE_FILE, ((double)progress), 
//									((double)100), "Synchronizing with server... "));
												
//							if (progress >= 0)
//							{
								progressMonitor.setProgress(progress);
					            String message =
					                String.format("Completed %d%%.\n", progress);
					            progressMonitor.setNote(message);
		
//							}
							previousProgress = progress;
						}
						else if (Networking.connectionState == Networking.connectionState.Connected && progressMonitor != null)
						{
							progressMonitor.close();
							progressMonitor = null;
						}
					}
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
//				ErrorMessage.fatal("Network RX Queue Error");
			}
		}
	}
	
	public static void startProgressMonitor()
	{
		progressMonitor = new ProgressMonitor(CalicoDataStore.calicoObj,
                "Synchronizing with server: " + CalicoDataStore.ServerHost,
                "", 0, 100);
		progressMonitor.setProgress(0);
		progressMonitor.setMillisToPopup(1);
		progressMonitor.setMillisToDecideToPopup(1);
	}
}
	
