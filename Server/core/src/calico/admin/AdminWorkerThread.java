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
package calico.admin;

import java.io.IOException;
import java.net.SocketTimeoutException;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpException;
import org.apache.http.HttpServerConnection;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpService;
import org.apache.log4j.Logger;

import calico.CalicoServer;

public class AdminWorkerThread extends Thread
{

	private static Logger logger = Logger.getLogger(AdminServer.class.getName());
	
	private final HttpService httpservice;
	private final HttpServerConnection conn;

	public AdminWorkerThread(final HttpService httpservice, final HttpServerConnection conn)
	{
		super();
		this.httpservice = httpservice;
		this.conn = conn;
	}

	public void run()
	{
		
		logger.trace("New connection thread");
		HttpContext context = new BasicHttpContext(null);
		try
		{
			while(!Thread.interrupted() && this.conn.isOpen())
			{
				this.httpservice.handleRequest(this.conn, context);
			}
		}
		catch(ConnectionClosedException ex)
		{
			logger.error("Client closed connection");
		}
		catch(SocketTimeoutException stoe)
		{
			// ignore
		}
		catch(IOException ex)
		{
			logger.error("I/O error: " + ex.toString());
		}
		catch(HttpException ex)
		{
			logger.error("Unrecoverable HTTP protocol violation: " + ex.getMessage());
		}
		finally
		{
			try
			{
				this.conn.shutdown();
			}
			catch(IOException ignore) {}
		}
	}

}
