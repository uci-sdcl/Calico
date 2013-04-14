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
import java.text.*;
import java.util.*;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.http.*;
import org.apache.http.entity.*;
import org.apache.http.protocol.*;
import org.apache.http.util.*;
import org.apache.log4j.Logger;

import org.json.me.*;

import calico.*;
import calico.admin.*;
import calico.admin.exceptions.*;
import calico.utils.CalicoBackupHandler;

public class AdminBasicRequestHandler implements HttpRequestHandler
{
	public static final int METHOD_GET = 1 << 0;
	public static final int METHOD_POST = 1 << 1;
	public static final int METHOD_HEAD = 1 << 2;
	public static final int METHOD_PUT = 1 << 3;
	public static final int METHOD_DELETE = 1 << 4;
	
	
	
	
	private static Logger logger = Logger.getLogger(AdminServer.class.getName());
	
	public AdminBasicRequestHandler()
	{
	    super();
	}
	
	public int getAllowedMethods()
	{
		return (METHOD_GET);// | METHOD_POST | METHOD_HEAD | METHOD_PUT | METHOD_DELETE);
	}
	
	public void handle(final HttpRequest request, final HttpResponse response, final HttpContext context) throws HttpException, IOException
	{
		//response.addHeader("X-Calico-Server-Address",CalicoServer.listenHost.getHostAddress()+":"+CalicoServer.listenPort);
		long startTime = System.nanoTime();
		response.addHeader("X-Calico-Request-ID", Long.toString(startTime, 36));
		
		response.addHeader("X-Calico-Uptime", DurationFormatUtils.formatDuration((System.currentTimeMillis() - COptions.ServerStartTime), "H:m:s"));
		
		Properties urlparams = request2params(request);
		
		try
		{
			
			String method = getMethod(request);// request.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);
			
			if(method.equals("GET") && ((getAllowedMethods() & AdminBasicRequestHandler.METHOD_GET) == AdminBasicRequestHandler.METHOD_GET) )
			{
				// This is a valid GET
			}
			else if(method.equals("POST") && ((getAllowedMethods() & AdminBasicRequestHandler.METHOD_POST) == AdminBasicRequestHandler.METHOD_POST) )
			{
				// This is a valid POST
			}
			else if(method.equals("HEAD") && ((getAllowedMethods() & AdminBasicRequestHandler.METHOD_HEAD) == AdminBasicRequestHandler.METHOD_HEAD) )
			{
				// This is a valid HEAD	
			}
			else if(method.equals("PUT") && ((getAllowedMethods() & AdminBasicRequestHandler.METHOD_PUT) == AdminBasicRequestHandler.METHOD_PUT) )
			{
				// This is a valid PUT
			}
			else if(method.equals("DELETE") && ((getAllowedMethods() & AdminBasicRequestHandler.METHOD_DELETE) == AdminBasicRequestHandler.METHOD_DELETE) )
			{
				// This is a valid DELETE 
			}
			else
			{
				throw new MethodNotAllowedException();
			}
			
			
			
			

			logger.trace("API Request: "+request.getRequestLine().getUri());
			
			try
			{
			
				
				if (request instanceof HttpEntityEnclosingRequest)
				{
					HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
					byte[] entityContent = EntityUtils.toByteArray(entity);
					//String content = new String(entityContent);
					//System.out.println("Incoming entity content (bytes): " + entityContent.length);
					//System.out.println("Incoming entity content (bytes): " + content);
					if(entityContent.length>0)
					{
						handleRequest(request, response, entityContent);
					}
					else
					{
						handleRequest(request,response);
					}
				}
				else
				{
					handleRequest(request,response);
				}
			}
			catch(JSONException ex)
			{
				response.addHeader("X-JSON-Error", ex.getMessage());
				throw new CalicoAPIErrorException("JSONGeneratorFailure", "This probably isn't your fault");
			}
		}
		catch(RedirectException rex)
		{
			response.setStatusCode(HttpStatus.SC_MOVED_TEMPORARILY);
			response.addHeader("Location", rex.getURL());
			
			StringEntity body = new StringEntity( "Redirecting you to "+rex.getURL() );
			body.setContentType("text/html");
			response.setEntity(body);
		}
		catch(CalicoAPIErrorException ex)
		{
			response.setStatusCode(ex.code);
			final CalicoAPIErrorException tempex = ex;
			
			if(urlparams.getProperty("xml","0").equals("1"))
			{
				// PRINT AS XML PROPERTIES
				EntityTemplate body = new EntityTemplate(new ContentProducer() {
					public void writeTo(final OutputStream outstream) throws IOException {
						tempex.toProperties().storeToXML(outstream, tempex.comments);
					}
				});
				body.setContentType("text/xml");
				response.setEntity(body);
			}
			else if(urlparams.getProperty("json","0").equals("1"))
			{
				// PRINT AS JSON
				StringEntity body = new StringEntity( (new JSONObject(tempex.toProperties())).toString() );
				body.setContentType("application/json");
				response.setEntity(body);
			}
			else
			{
				// PRINT AS TEXT
				EntityTemplate body = new EntityTemplate(new ContentProducer() {
					public void writeTo(final OutputStream outstream) throws IOException {
						tempex.toProperties().store(outstream, tempex.comments);
					}
				});
				body.setContentType("text/plain");
				response.setEntity(body);
			}
			
		}
		
		double respTime = ((double)(System.nanoTime()-startTime))/1000000.0;
		
		response.addHeader("X-Calico-Response-Time", respTime+"ms");
		
	}//handle
	
	
	
	protected void handleRequest(final HttpRequest request, final HttpResponse response, byte[] bytes) throws HttpException, IOException, CalicoAPIErrorException, JSONException
	{
		handleRequest(request,response, new String(bytes));
	}
	
	protected void handleRequest(final HttpRequest request, final HttpResponse response, JSONObject requestContent) throws HttpException, IOException, CalicoAPIErrorException, JSONException
	{
		handleRequest(request,response);
	}

	
	protected void handleRequest(final HttpRequest request, final HttpResponse response, String requestContent) throws HttpException, IOException, CalicoAPIErrorException, JSONException
	{
		handleRequest(request,response);
	}
	
	
	protected void handleRequest(final HttpRequest request, final HttpResponse response) throws HttpException, IOException, CalicoAPIErrorException, JSONException
	{
		throw new CalicoAPIErrorException("RequestNotSupported");
	}
	

	protected Properties request2params(final HttpRequest request)
	{
		return urlQuery2Properties(uri2url(request.getRequestLine().getUri()));
	}
	

	protected URL uri2url(String uri)
	{
		try
		{
			return new URL("http://"+COptions.admin.listen.host+":"+COptions.admin.listen.port+uri);
		}
		catch(MalformedURLException e)
		{
			return null;
		}
	}
	
	protected Properties urlQuery2Properties(URL url)
	{
		return parseURLParams(url.getQuery());
	}
	
	protected Properties parseURLParams(String q)
	{
		Properties props = new Properties();
		if (q == null || q.length() == 0)
		{
			return new Properties();
		}
		for (StringTokenizer iter = new StringTokenizer(q, "&"); iter.hasMoreElements();/*-*/)
		{
			String pair = (String) iter.nextToken();
			int split = pair.indexOf('=');
			if (split <= 0)
			{
				throw new RuntimeException("Invalid pair [" + pair + "] in query string [" + q + "]");
			}
			else
			{
				String key = pair.substring(0, split);
				String value = pair.substring(split + 1);
				try
				{
					key = URLDecoder.decode(key, "UTF-8");
					value = URLDecoder.decode(value, "UTF-8");
				}
				catch (UnsupportedEncodingException e)
				{
					throw new RuntimeException("Invalid encoding in [" + pair + "] in query string [" + q + "]", e);
				}
				props.setProperty(key, value);
			}
		}
		return props;
	}
	
	protected Properties getURLParams(final HttpRequest request)
	{
		return urlQuery2Properties(uri2url(request.getRequestLine().getUri()));
	}
		
	
	
	protected String getDate()
	{
		//EEE, d MMM yyyy HH:mm:ss Z
		Date todaysDate = new java.util.Date();
		SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss");
		String formattedDate = formatter.format(todaysDate);
		return formattedDate;
	}
	

	protected String getMethod(final HttpRequest request)
	{
		return request.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);
	}
	
	

}
