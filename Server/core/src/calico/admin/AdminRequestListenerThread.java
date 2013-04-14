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
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.http.*;
import org.apache.http.impl.*;
import org.apache.http.params.*;
import org.apache.http.protocol.*;
import org.apache.log4j.Logger;

import calico.COptions;
import calico.admin.requesthandlers.*;
import calico.admin.requesthandlers.gui.*;

public class AdminRequestListenerThread extends Thread
{
	private static Logger logger = Logger.getLogger(AdminServer.class.getName());

	
	private final HttpParams params; 
	private final HttpService httpService;
	private static HttpRequestHandlerRegistry reqistry;

	public AdminRequestListenerThread() throws IOException
	{
		// 50 = backlog
		COptions.admin.serversocket = new ServerSocket(COptions.admin.listen.port, 50, InetAddress.getByName(COptions.admin.listen.host));

		this.params = new BasicHttpParams();
		this.params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, COptions.admin.listen.timeout);
		this.params.setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, COptions.admin.listen.buffer);
		this.params.setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, COptions.admin.listen.stale_conn_check);
		this.params.setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, COptions.admin.listen.tcp_nodelay);
		this.params.setParameter(CoreProtocolPNames.ORIGIN_SERVER, COptions.admin.server_signature);

		// Set up the HTTP protocol processor
		BasicHttpProcessor httpproc = new BasicHttpProcessor();//new HttpResponseInterceptor[] {
		httpproc.addInterceptor(new ResponseDate());
		httpproc.addInterceptor(new ResponseServer());
		httpproc.addInterceptor(new ResponseContent());
		httpproc.addInterceptor(new ResponseConnControl());
		
		// Set up request handlers
		reqistry = new HttpRequestHandlerRegistry();

		// Clients
		reqistry.register("/client/list*", new ClientListRequestHandler());
		reqistry.register("/client/kick*", new NotImplementedRequestHandler());
		reqistry.register("/client/get*", new NotImplementedRequestHandler());

		
		// Session Stuff///////////

		reqistry.register("/backup/generate*", new BackupGenerateRequestHandler());
		reqistry.register("/backup/restore*", new BackupRestoreRequestHandler());
		
		reqistry.register("/stroke/list*", new StrokeListRequestHandler());
		reqistry.register("/stroke/get*", new StrokeGetRequestHandler());
		
		reqistry.register("/group/list*", new GroupListRequestHandler());
		reqistry.register("/group/get*", new GroupGetRequestHandler());

		reqistry.register("/canvas/list*", new CanvasListRequestHandler());
		reqistry.register("/canvas/getimage*", new CanvasGetImageRequestHandler());
		reqistry.register("/canvas/get*", new CanvasGetRequestHandler());
		reqistry.register("/canvas/getactions*", new CanvasGetActionHistoryRequestHandler());
		

		reqistry.register("/arrow/list*", new NotImplementedRequestHandler());
		reqistry.register("/arrow/get*", new NotImplementedRequestHandler());
		
		///// END SESSION
		
		reqistry.register("/chat*", new ChatRequestHandler());
		
		// Config
		
		// Server
		reqistry.register("/server/shutdown*", new NotImplementedRequestHandler());
		reqistry.register("/server/gc*", new ServerGCRequestHandler());
		reqistry.register("/stats*", new StatsRequestHandler());
		
		
		// Debugging and stuff
		reqistry.register("/debug/unittest*", new UnitTestRequestHandler());

		reqistry.register("/gui/", new IndexPage());
		reqistry.register("/gui/backup/", new BackupIndexPageRH());
		reqistry.register("/gui/images/*", new GuiImageLoaderRH());
		reqistry.register("/gui/config/", new ConfigIndexRH());
		reqistry.register("/gui/command/help", new CommandHelpPageRH());
		reqistry.register("/gui/command*", new CommandPageRH());
		reqistry.register("/gui/clients/*", new ClientsIndexPageRH());
		
		reqistry.register("/gui/imagemgr/upload*", new ImageUploadRH());
		
		reqistry.register("/gui/chat*", new ChatPageRH());
		
		reqistry.register("/gui", new RedirectRequestHandler("/gui/"));
		
		reqistry.register("/uploads/*", new UploadedFilesRH());

		reqistry.register("/", new RedirectRequestHandler("/gui/"));
		
		// Default?
		reqistry.register("*", new NotFoundRequestHandler());

		// Set up the HTTP service
		this.httpService = new HttpService(
			httpproc, 
			new DefaultConnectionReuseStrategy(), 
			new DefaultHttpResponseFactory()
		);
		this.httpService.setParams(this.params);
		this.httpService.setHandlerResolver(reqistry);
	}
	
	/**
	 * Allows a plugin to register a new handler. See one of the *.vm page for an example of
	 * a template page. Also, if you want to add a tab, make sure to use {@link=GUITemplate.setSection(String)} 
	 * @param page The string of the page. Example: "/gui/clients/*". The * implies any following character.
	 * @param handler
	 */
	public static void registerPageHandler(String page, AdminBasicRequestHandler handler)
	{
		reqistry.register(page, handler);
	}

	public void run()
	{
		logger.info("Listening on port " + COptions.admin.serversocket.getLocalPort());
		while(!Thread.interrupted())
		{
			try
			{
				// Set up HTTP connection
				Socket socket = COptions.admin.serversocket.accept();
				DefaultHttpServerConnection conn = new DefaultHttpServerConnection();
				logger.trace("Incoming connection from " + socket.getInetAddress());
				conn.bind(socket, this.params);

				// Start worker thread
				Thread t = new AdminWorkerThread(this.httpService, conn);
				t.setDaemon(true);
				t.start();
			}
			catch(InterruptedIOException ex)
			{
				break;
			}
			catch (IOException e)
			{
				logger.error("I/O error initialising connection thread: " + e.getMessage());
				break;
			}
		}
	}
}
