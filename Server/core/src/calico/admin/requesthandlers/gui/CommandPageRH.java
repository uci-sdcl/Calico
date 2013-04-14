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

import it.unimi.dsi.fastutil.io.FastByteArrayInputStream;

import java.io.IOException;

import java.io.StringWriter;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.*;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.json.me.JSONException;
import org.json.me.JSONObject;

import calico.*;
import calico.admin.CalicoAPIErrorException;
import calico.admin.exceptions.RedirectException;
import calico.admin.requesthandlers.AdminBasicRequestHandler;
import calico.clients.ClientManager;
import calico.controllers.CCanvasController;
import calico.controllers.CGroupController;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;
import calico.plugins.CalicoPluginManager;
import calico.plugins.PluginCommandParameters;
import calico.utils.CalicoBackupHandler;
import calico.utils.CalicoInvalidBackupException;
import calico.utils.CalicoUploadParser;
import calico.uuid.UUIDAllocator;

import org.apache.velocity.*;
import org.apache.velocity.app.*;

public class CommandPageRH extends AdminBasicRequestHandler
{

	public static Pattern commandregex = Pattern.compile("\"([^\"]+?)\"\\s?|([^\\s]+)\\s?|\\s");
	
	
	public int getAllowedMethods()
	{
		return (METHOD_GET | METHOD_POST | METHOD_PUT );
	}
	
	protected void handleRequest(final HttpRequest request, final HttpResponse response, byte[] data) throws HttpException, IOException, JSONException, CalicoAPIErrorException
	{
		String strData = new String(data);
		Properties params = parseURLParams(strData);

		handleRequestInternal(request, response, params);
	}
	
	protected void handleRequest(final HttpRequest request, final HttpResponse response) throws HttpException, IOException, JSONException, CalicoAPIErrorException
	{
		handleRequestInternal(request, response, new Properties());
	}
	
	protected void handleRequestInternal(final HttpRequest request, final HttpResponse response, Properties params) throws HttpException, IOException, JSONException, CalicoAPIErrorException
	{
		try
		{
			GUITemplate gt = new GUITemplate("command.vm");
			gt.setSection("console");
		
			
			String command = params.getProperty("command", "NONE");

			gt.put("lastcommand", "");
			if(!command.equals("NONE"))
			{
				gt.put("hasoutput", "1");
				gt.put("lastcommand", StringEscapeUtils.escapeHtml(command));
				
				
				try
				{
					String output = parseCommand(command);
					if(output.length()==0)
					{
						gt.put("commandoutput", "This command has no output.");
					}
					else
					{
						output = output.trim();
						//output = StringEscapeUtils.escapeHtml(output);
						//output = output.replaceAll("\n", "<br/>\n");
						gt.put("commandoutput", output);
					}					
				}
				catch(Exception e)
				{					
					gt.put("commandoutput", "Exception: "+e.getMessage()+"\n\nSee the help page for more info.\n");
				}
			}
			gt.getOutput(response);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	
	
	private String parseCommand(String command) throws Exception
	{
		StringBuilder output = new StringBuilder();
		
		command = command.trim();
		
		
		Vector<String> theCmdArray = new Vector<String>(0);
		Matcher m = commandregex.matcher(command);
		while (m.find())
		{
			theCmdArray.add( m.group().trim() );
		}
		
		for(int i=0;i<theCmdArray.size();i++)
		{
			System.out.println("CommandPart: "+theCmdArray.get(i));
		}
		
		String justCommand = theCmdArray.remove(0).toLowerCase();
		
		if(justCommand.equals("canvas_list")){command_canvas_list(output, theCmdArray);}
		else if(justCommand.equals("canvas_contents")){command_canvas_contents(output, theCmdArray);}
		else if(justCommand.equals("canvas_clear")){command_canvas_clear(output, theCmdArray);}
		else if(justCommand.equals("group_delete")){command_group_delete(output, theCmdArray);}
		else if(justCommand.equals("stroke_delete")){command_stroke_delete(output, theCmdArray);}
		else if(justCommand.equals("group_copy")){command_group_copy(output, theCmdArray);}
		
		else if(justCommand.equals("set")){command_set(output, theCmdArray);}
		else if(justCommand.equals("get")){command_get(output, theCmdArray);}
		else if(CalicoPluginManager.isAdminCommandRegistered(justCommand))
		{
			CalicoPluginManager.callAdminCommand(justCommand, new PluginCommandParameters(theCmdArray), output);
		}
		else
		{
			output.append("Error: \""+justCommand+"\" is not a valid command.\n");
			output.append("See the help page for more info\n");
		}
		
		return output.toString();
	}
	
	
	private void command_canvas_list(StringBuilder builder, Vector<String> params) throws Exception
	{
		long[] uuids = CCanvasController.canvases.keySet().toLongArray();
		
		builder.append("UUID\tCoordinate\n");
		for(int i=0;i<uuids.length;i++)
		{
			builder.append("<a href=\"http://"
					+InetAddress.getLocalHost().getHostAddress()+":"+COptions.admin.serversocket.getLocalPort()
					+"/canvas/getimage?uuid="+uuids[i]+"\">");
			builder.append(uuids[i]+"\t"+CCanvasController.canvases.get(uuids[i]).getIndex());
			builder.append("</a>\n");
		}

	}
	
	private void command_canvas_contents(StringBuilder builder, Vector<String> params) throws Exception
	{
		long uuid = Long.decode(params.get(0));
		
		builder.setLength(60);
		builder.append(CCanvasController.canvases.get(uuid).toProperties());
	}
	

	
	private void command_canvas_clear(StringBuilder builder, Vector<String> params) throws Exception
	{
		long uuid = Long.decode(params.get(0));
		//CCanvasController.no_notify_clear(uuid);
		CalicoPacket packet = CalicoPacket.getPacket(NetworkCommand.CANVAS_CLEAR, uuid);
		packet.rewind();
		packet.getInt();
		ProcessQueue.receive(NetworkCommand.CANVAS_CLEAR, null, packet);
		ClientManager.send(packet);
		
		builder.append("Canvas "+uuid+" ("+CCanvasController.canvases.get(uuid).getIndex()+") cleared.\n");  	
		
	}
	
	private void command_group_delete(StringBuilder builder, Vector<String> params) throws Exception
	{
		long uuid = Long.decode(params.get(0));
		//CCanvasController.no_notify_clear(uuid);
		CalicoPacket packet = CalicoPacket.getPacket(NetworkCommand.GROUP_DELETE, uuid);
		packet.rewind();
		packet.getInt();
		ProcessQueue.receive(NetworkCommand.GROUP_DELETE, null, packet);
		ClientManager.send(packet);
		
		builder.append("Group "+uuid+" cleared.\n");  
	}
	
	private void command_stroke_delete(StringBuilder builder, Vector<String> params) throws Exception
	{
		long uuid = Long.decode(params.get(0));
		//CCanvasController.no_notify_clear(uuid);
		CalicoPacket packet = CalicoPacket.getPacket(NetworkCommand.STROKE_DELETE, uuid);
		packet.rewind();
		packet.getInt();
		ProcessQueue.receive(NetworkCommand.STROKE_DELETE, null, packet);
		ClientManager.send(packet);
		
		builder.append("Stroke "+uuid+" cleared.\n");  
	}
	
	private void command_set(StringBuilder builder, Vector<String> params) throws Exception
	{
		String cname = params.get(0);
		String value = params.get(1);
		try
		{
			CalicoConfig.setConfigVariable(cname, value);
		
			builder.append("Variable '"+cname+"' set to '"+value+"'\n");	
		}
		catch(NoSuchFieldException e)
		{
			builder.append("Variable '"+cname+"' does not exist.\n");
		}
	}
	
	private void command_get(StringBuilder builder, Vector<String> params) throws Exception
	{
		String cname = params.get(0);
		try
		{
			Field field = CalicoConfig.getConfigField(cname);
		
			builder.append(field.get(null).toString());	
		}
		catch(NoSuchFieldException e)
		{
			builder.append("Variable '"+cname+"' does not exist.\n");
		}
	}
	
	
	private void command_group_copy(StringBuilder builder, Vector<String> params) throws Exception
	{
		//public static void no_nofity_copy(final long uuid, final long new_uuid, final long new_canvasuuid, final int shift_x, final int shift_y)
		if(params.size()!=2 && params.size()!=4)
		{
			throw new Exception("Invalid Parameters");
		}
		
		long uuid = Long.parseLong(params.get(0));
		long canvas_uuid = Long.parseLong(params.get(1));
		
		int shiftx = 0;
		int shifty = 0;
		
		if(params.size()==4)
		{
			shiftx = Integer.parseInt(params.get(2));
			shifty = Integer.parseInt(params.get(3));
		}
		
		long new_uuid = UUIDAllocator.getUUID();
		
		CGroupController.copy(uuid, new_uuid, canvas_uuid, shiftx, shifty, true);
		builder.append("Group '"+uuid+"' copied to canvas '"+canvas_uuid+"'.\n");
	}
	
	
	
	
}
