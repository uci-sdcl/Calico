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
package calico.plugins.googletalk;


import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;

import calico.ProcessQueue;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;
import calico.plugins.*;
import calico.plugins.events.*;
import calico.plugins.events.clients.ClientConnect;
import calico.plugins.events.scraps.*;

public class GoogleTalkPlugin extends AbstractCalicoPlugin implements CalicoPlugin
{

	
	private XMPPConnection connection = null;
	private Presence presence = null;
	
	public GoogleTalkPlugin()
	{
		super();

		PluginInfo.name = "GoogleTalkPlugin";
		PluginInfo.author = "mdempsey";
		PluginInfo.info = "Google Talk Interface";
		PluginInfo.url = "http://google.com/";
		
	}//
	
	
	void sendMessage(String username, String message)
	{
		Message msg = new Message(username, Message.Type.chat);
		msg.setBody(message);
		connection.sendPacket(msg);
		
		// Make the packet
//		CalicoPacket packet = new CalicoPacket(null);
		// HANDLE THE PACKET
//		ProcessQueue.receive("command", null, packet);

		
		debug("Message sent");
	}
	
	
	public void onPluginStart()
	{
		RegisterAdminCommand("gtalk_send","receiveChatCommand");
		try
		{
			
			// connect to gtalk server
			ConnectionConfiguration connConfig = new ConnectionConfiguration(GetConfigString("plugin.googletalk.server"), GetConfigInt("plugin.googletalk.port"), GetConfigString("plugin.googletalk.service"));
			connection = new XMPPConnection(connConfig);
			connection.connect();
			
			// login with username and password
			connection.login(GetConfigString("plugin.googletalk.username"), GetConfigString("plugin.googletalk.password"));
			
			// set presence status info
			presence = new Presence(Presence.Type.available);
			connection.sendPacket(presence);
			
			FirePluginEvent(new CalicoEvent());
			
			// Send a message
			//sendMessage("mrdempsey@gmail.com", "Calico GTalk Plugin Active");
			
			GTalkPacketListener packetListener = new GTalkPacketListener(this);
			connection.addPacketListener(packetListener, null);
		}
		catch(Exception e)
		{
			System.out.println("GTALK EXCEPTION");
			e.printStackTrace();
		}
	}
	
	public void onPluginEnd()
	{
		System.out.println("GTALK SHUTDOWN");
		// set presence status to unavailable
		presence = new Presence(Presence.Type.unavailable);
		connection.sendPacket(presence);
	}
	
	public void onException(Exception e)
	{
		
	}
	
	
	
	public void receiveChatCommand(PluginCommandParameters params, StringBuilder output)
	{
		//FireEvent(new ScrapReload(1L));
		sendMessage("mrdempsey@gmail.com", params.getString(0));
	}
	
	
	
	public void onClientConnect(ClientConnect event)
	{
		//sendMessage("mrdempsey@gmail.com", "Client has joined");
	}
	
	public void onScrapCreate(ScrapCreate event)
	{
		System.out.println("RECEIVED: "+event.getClass().getCanonicalName());
	}


	@Override
	public Class<?> getNetworkCommandsClass() {
		// TODO Auto-generated method stub
		return null;
	}

}
