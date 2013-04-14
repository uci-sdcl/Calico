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
import java.net.*;
import java.nio.ByteBuffer;

import calico.Calico;
import calico.modules.*;
import calico.networking.netstuff.CalicoPacket;


/**
 * This reads packets from the network and then adds them to the packet queue
 *
 * @author Mitch Dempsey
 */
public class UDPListenServer implements Runnable
{
	public UDPListenServer()
	{
		
	}
	
	public void run()
	{
		try
		{
			ByteBuffer buffer = ByteBuffer.allocateDirect(4096);
			
			while(true)
			{
				buffer.clear();
				
				int bytesRead = Networking.udpChannel.read(buffer);

				if(bytesRead==0)
				{
					// none
					//return null;
				}
				else if(bytesRead==-1)
				{
					// discon
					//return null;
				}
				else
				{
					//System.out.println("Bytes Read: "+bytesRead);
					byte[] data = new byte[bytesRead];
					System.arraycopy(buffer.array(), 0, data, 0, data.length);
					//CalicoPacket pack = new CalicoPacket(data);
					PacketHandler.receive( new CalicoPacket(data) );
					
				}
				// TESTING
				//PacketHandler.receive(new CalicoPacket(data2));
				
				
			}//while
		}
		catch(IllegalStateException ise)
		{
			Calico.logger.fatal("The receive queue has exploded.");
			ise.printStackTrace();
		}
		catch(Exception e)
		{
			//Networking.recvQueue.put(new CalicoPacket(rdata));
			e.printStackTrace();
		}
	}
}


