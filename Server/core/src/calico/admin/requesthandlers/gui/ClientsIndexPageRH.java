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
package calico.admin.requesthandlers.gui;

import java.io.IOException;

import java.io.StringWriter;
import java.net.InetAddress;
import java.util.*;
import calico.clients.*;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.json.me.JSONException;
import org.json.me.JSONObject;

import calico.*;
import calico.admin.CalicoAPIErrorException;
import calico.admin.requesthandlers.AdminBasicRequestHandler;

import org.apache.velocity.*;
import org.apache.velocity.app.*;

public class ClientsIndexPageRH extends AdminBasicRequestHandler
{
	
	public static class ClientInfo
	{
		public String host = "";
		public String port = "";
		public String clientid = "";
		public String username = "";
		public String getClientID(){return this.clientid;}
		public String getHost(){return this.host;}
		public String getPort(){return this.port;}
		public String getUsername(){return this.username;}
		public String getHostname()
		{
			try
			{
				return InetAddress.getByName(this.host).getCanonicalHostName();
			}
			catch(Exception e)
			{
				return "";
			}
		}
	}
	
	
	protected void handleRequest(final HttpRequest request, final HttpResponse response) throws HttpException, IOException, JSONException, CalicoAPIErrorException
	{
		try
		{
			GUITemplate gt = new GUITemplate("clients/index.vm");
			gt.setSection("clients");
			
			
			ArrayList<ClientInfo> clients = new ArrayList<ClientInfo>();
			int[] clientids = ClientManager.get_clientids();
			
			for(int i=0;i<clientids.length;i++)
			{
				ClientInfo temp = new ClientInfo();
				Properties props = ClientManager.getClientProperties(clientids[i]);
				temp.clientid = ""+clientids[i];
				temp.host = props.getProperty("tcp.host");
				temp.port = props.getProperty("tcp.port");
				temp.username = props.getProperty("username");
				clients.add(temp);
			}
			
			gt.put("clients", clients);
			
			//gt.put("test.param.yar", "this is a big test");
			gt.getOutput(response);
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
