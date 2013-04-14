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


import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.batik.dom.*;
import org.apache.batik.svggen.*;
import org.apache.commons.vfs.*;
import org.apache.http.*;
import org.apache.http.entity.*;
import org.apache.http.protocol.*;
import org.apache.http.util.*;

import org.apache.http.message.*;
import org.json.me.*;
import org.w3c.dom.*;

import calico.admin.*;
import calico.admin.exceptions.SuccessException;
import calico.clients.*;
import calico.controllers.*;
import calico.utils.*;
import calico.uuid.*;
import calico.*;

import java.util.zip.*;

public class BackupGenerateRequestHandler extends AdminBasicRequestHandler
{
	/*
	public class GzipCompressingEntity extends HttpEntityWrapper
	{
	    
	    private static final String GZIP_CODEC = "gzip";

	    public GzipCompressingEntity(final HttpEntity entity)
	    {
	        super(entity);
	    }

	    public Header getContentEncoding()
	    {
	        return new BasicHeader(HTTP.CONTENT_ENCODING, GZIP_CODEC);
	    }

	    public long getContentLength()
	    {
	        return -1;
	    }

	    public boolean isChunked()
	    {
	        // force content chunking
	        return true;
	    }

	    public void writeTo(final OutputStream outstream) throws IOException
	    {
	        if (outstream == null)
	        {
	            throw new IllegalArgumentException("Output stream may not be null");
	        }
	        GZIPOutputStream gzip = new GZIPOutputStream(outstream);
	        InputStream in = wrappedEntity.getContent();
	        byte[] tmp = new byte[2048];
	        int l;
	        while ((l = in.read(tmp)) != -1)
	        {
	            gzip.write(tmp, 0, l);
	        }
	        gzip.close();
	    }

	} // class GzipCompressingEntity
	*/
	
	
	protected void handleRequest(final HttpRequest request, final HttpResponse response) throws HttpException, IOException, JSONException, CalicoAPIErrorException
	{
		URL requestURL = uri2url(request.getRequestLine().getUri());
		
		Properties params = urlQuery2Properties(requestURL);
		
		
		String saveto = params.getProperty("saveto","BROWSER").toUpperCase();
		
		FileObject tempstore = COptions.fs.resolveFile("ram://tempstore");
		tempstore.createFile();

		CalicoBackupHandler.writeBackupStream(tempstore.getContent().getOutputStream());
		tempstore.getContent().close();
		
		if(saveto.equals("BROWSER"))
		{
			ByteArrayEntity bae = new ByteArrayEntity(FileUtil.getContent(tempstore));	
			bae.setContentType("application/calico-backup-state");
			response.setEntity(bae);
			response.addHeader("Content-Disposition", "attachment; filename=calico3server_backup.csb");
			tempstore.close();
		}
		else if(saveto.equals("FILE"))
		{
			String filename = params.getProperty("file","www_backup.csb");
			
			FileObject saveToFile = COptions.fs.resolveFile(filename);
			saveToFile.copyFrom(tempstore, Selectors.SELECT_ALL);

			tempstore.close();
			Properties props = new Properties();
			props.setProperty("Status", "OK");
			props.setProperty("SaveLocation", saveToFile.getURL().toString());
			
			
			throw new SuccessException(props);
		}
		
	}
	
	
}
