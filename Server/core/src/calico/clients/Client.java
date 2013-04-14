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

import java.net.*;


public class Client 
{
	private InetAddress address;
	private int port;

	private int clientid = -1;
	private byte[] mac;

	public Client(InetAddress address, int port, int clientid)
	{
		this.port = port;
		this.address = address;
		this.clientid = clientid;
		
//		this.mac = address.getAddress();
//		NetworkInterface ni;
//		try {
//			ni = NetworkInterface.getByInetAddress(this.address);
//			
//			if (ni != null)
//			{
////				this.mac = ni.getHardwareAddress();
//				this.mac = address.getAddress();
//			}
//		} catch (SocketException e) {
//			e.printStackTrace();
//		}
	}
	public Client(InetAddress address, int port)
	{
		this(address, port, -1);
	}
	
	public int getClientID()
	{
		return this.clientid;
	}
	
	public InetAddress getAddress()
	{
		return this.address;
	}
	public int getPort()
	{
		return this.port;
	}
	
	public String toString()
	{
		return "<"+this.clientid+"><"+this.address.getHostAddress()+":"+this.port+">";
	}
	
	public String getUsername()
	{
		return ClientManager.get_client_username(this.clientid);
	}

	public boolean equals(Client c)
	{
		if( c.getAddress().equals(this.address) && c.getPort()==this.port)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	public void updateID(int clientid2) {
		clientid = clientid2;
		
	}
	
//	public boolean sameMacAs(Client c)
//	{
//		if (c.mac.length == this.mac.length)
//		{
//			for (int i = 0; i < this.mac.length; i++)
//				if (c.mac[i] != this.mac[i])
//					return false;
//			return true;
//		}
//		
//		return false;
//	}

}//client


