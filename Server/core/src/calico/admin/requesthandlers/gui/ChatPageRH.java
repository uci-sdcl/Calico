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

public class ChatPageRH extends AdminBasicRequestHandler
{

	
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
			GUITemplate gt = new GUITemplate("chat.vm");
			gt.setSection("chat");
			gt.getOutput(response);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	
	
	
}
