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
package calico.admin.requesthandlers;


import it.unimi.dsi.fastutil.io.FastByteArrayInputStream;

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.http.*;
import org.apache.http.entity.*;
import org.apache.http.protocol.*;
import org.apache.http.util.*;

import org.apache.http.message.*;
import org.json.me.*;

import calico.admin.*;
import calico.admin.exceptions.*;
import calico.clients.*;
import calico.*;

import calico.components.*;
import calico.controllers.CArrowController;
import calico.controllers.CCanvasController;
import calico.controllers.CGroupController;
import calico.controllers.CStrokeController;
import calico.utils.CalicoBackupHandler;
import calico.utils.CalicoInvalidBackupException;
import calico.uuid.UUIDAllocator;

import java.util.zip.*;

public class BackupRestoreRequestHandler extends AdminBasicRequestHandler
{
	public int getAllowedMethods()
	{
		return (METHOD_GET | METHOD_POST | METHOD_PUT );
	}
	
	protected void handleRequest(final HttpRequest request, final HttpResponse response) throws HttpException, IOException, JSONException, CalicoAPIErrorException
	{
		handleRequest(request, response, new byte[]{(byte)0});
	}
	
	protected void handleRequest(final HttpRequest request, final HttpResponse response, byte[] bytes) throws HttpException, IOException, JSONException, CalicoAPIErrorException
	{

		Properties params = this.getURLParams(request);
		
		String source = params.getProperty("source","FILE").toUpperCase();
		
		try
		{
			if(source.equals("FILE"))
			{
				String filename = params.getProperty("file","./backup_auto.csb");
			
			
				Properties props = CalicoBackupHandler.getBackupFileInfo(filename);
				props.setProperty("Restored", "OK");
				
				CalicoBackupHandler.restoreBackupFile(filename);
				
				throw new SuccessException(props);
			}
			else if(source.equals("UPLOAD"))
			{
				FastByteArrayInputStream bais = new FastByteArrayInputStream(bytes);
				CalicoBackupHandler.restoreBackupStream(bais);
				
				Properties props = new Properties();
				props.setProperty("Restored", "OK");
				
				
				throw new SuccessException(props);
			}
			

		}
		catch(CalicoInvalidBackupException e)
		{
			throw new CalicoAPIErrorException("Invalid Backup");
		}
		
	}
		
	
	
}
