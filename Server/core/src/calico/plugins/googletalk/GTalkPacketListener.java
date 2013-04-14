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

import java.util.Vector;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.*;


import calico.controllers.CCanvasController;
import calico.controllers.CGroupController;
import calico.uuid.UUIDAllocator;

public class GTalkPacketListener implements PacketListener
{
	private static Logger logger = Logger.getLogger(CGroupController.class.getName());
	
	private String POST_TEXT = "cp:";
	
	private GoogleTalkPlugin parent = null;
	
	public GTalkPacketListener(GoogleTalkPlugin parent)
	{
		this.parent = parent;
	}
	
	public void processPacket(Packet p)
	{
		String message = ((Message)p).getBody();
		if (message != null && message.contains(POST_TEXT))
		{
			String groupText = message.substring(message.indexOf(POST_TEXT) + POST_TEXT.length());
			long uuid = UUIDAllocator.getUUID();
			long cuid = getCanvasUUID();
			long puid = 0L;
			CGroupController.start(uuid, cuid, puid, true);
			CGroupController.append(uuid, 300, 300);
			CGroupController.set_text(uuid, groupText);
			CGroupController.finish(uuid, false);
			
		}
		
//		parent.debug(p.getFrom() + ": " + p.toString());
//		if (p instanceof Message)
//		{
//			Message msg = (Message) p;
//			parent.debug(msg.getFrom() + ": " + msg.getBody());
//			if(msg.getBody()!=null)
//			{
//				this.parent.sendMessage(msg.getFrom(), "You said: \""+msg.getBody()+"\"");
//			}
//		}
	}
	
	private long getCanvasUUID() 
	{
		long[] uuids = CCanvasController.canvases.keySet().toLongArray();
		
		
		return uuids[0];
		
//		builder.append("UUID\tCoordinate\n");
//		for(int i=0;i<uuids.length;i++)
//		{
//			builder.append(uuids[i]+"\t"+CCanvasController.canvases.get(uuids[i]).getCoordText()+"\n");
//		}
	}
}
