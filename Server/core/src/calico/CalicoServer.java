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
package calico;

import calico.networking.*;
import calico.networking.netstuff.*;
import calico.plugins.*;
import calico.plugins.events.*;
import calico.plugins.events.scraps.ScrapCreate;
import calico.plugins.googletalk.GoogleTalkPlugin;
import calico.admin.*;
import calico.sessions.SessionManager;
import calico.clients.*;
import calico.components.CCanvas;
import calico.controllers.*;
import calico.events.CalicoEventHandler;
import calico.utils.CalicoBackupHandler;
import calico.utils.CalicoUtils;
import calico.utils.Ticker;
import calico.uuid.*;


import java.awt.Color;
import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.channels.*;
import java.nio.channels.FileChannel.MapMode;



import java.util.concurrent.*;

import org.apache.commons.vfs.FileContent;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.Selectors;
import org.apache.log4j.*;

import it.unimi.dsi.fastutil.ints.Int2ReferenceAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceAVLTreeMap;
import it.unimi.dsi.fastutil.objects.*;



/*
* Needs to handle all the clients
* maintain who is the master server
* Manage heartbeats
*/


public class CalicoServer
{
	public static int sentPackets = 0;
	
	public static Logger logger = Logger.getLogger(CalicoServer.class.getName());

	public static InetAddress listenHost;

	public static Object2ReferenceOpenHashMap<Client,ClientThread> clientThreads = new Object2ReferenceOpenHashMap<Client,ClientThread>();
	public static Long2ReferenceAVLTreeMap<CanvasThread> canvasThreads = new Long2ReferenceAVLTreeMap<CanvasThread>();
	public static Int2ReferenceAVLTreeMap<Object> canvasCommands = CanvasThread.getCanvasCommands();
	
	public static String[] args = null;


	public static void main(String[] args)
	{
		CalicoServer.args = args;
		PropertyConfigurator.configure(System.getProperty("log4j.configuration","log4j.properties"));
		
		CalicoConfig.setup();
		CalicoEventHandler.getInstance();
		
		COptions.ServerStartTime = System.currentTimeMillis();
		
		try
		{
			// Check for a backup that already exists.
			//FileObject backupFile = COptions.fsManager.resolveFile(COptions.fsCWD, COptions.server.backup.backup_file);
			FileObject backupFile = COptions.fs.resolveFile(COptions.server.backup.backup_file);
			if(backupFile.exists())
			{
				FileObject backupFileBkup = COptions.fs.resolveFile(COptions.server.backup.backup_file+".bkup");
				int counter = 1;
				while (backupFileBkup.exists())
				{
					backupFileBkup = COptions.fs.resolveFile(COptions.server.backup.backup_file+"_" + counter++ +".bkup");
				}
					
				backupFileBkup.copyFrom(backupFile, Selectors.SELECT_ALL);
				logger.warn("Backup file: "+COptions.server.backup.backup_file+" already exists, renaming to "+COptions.server.backup.backup_file+".bkup");
				backupFileBkup.close();
			}
			backupFile.close();
		}
		catch(FileSystemException fse)
		{
			fse.printStackTrace();
		}

		// Setup the UUID Allocator
		UUIDAllocator.setup();

		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run()
			{
				CalicoServer.logger.info("Shutting down server...");
				
				CalicoPluginManager.shutdownPlugins();
				
				COptions.fs.close();
				
				//Thread.sleep(1000L);
			}
		});
		
		
		

		NetworkCommand.getFormat(0);
		ProcessQueue.setup();
		ClientManager.setup();
		
		// run the setups
		CArrowController.setup();
		CCanvasController.setup();
		CStrokeController.setup();
		CGroupController.setup();
		CSessionController.setup();
		
		
		logger.info(Runtime.getRuntime().availableProcessors()+" available CPUs");
		logger.info(Runtime.getRuntime().freeMemory()+"/"+Runtime.getRuntime().totalMemory()+" ("+CalicoUtils.printByteSize(Runtime.getRuntime().maxMemory())+") memory");
		logger.info("Starting Calico3 Server...");
		try
		{
			listenHost = InetAddress.getByName(COptions.listen.host);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		
		logger.info("Listening for connections on "+COptions.listen.host+":"+COptions.listen.port);


		ServerSocket sock = null;
		boolean listening = true;

		
		try
		{
			Thread t = new AdminRequestListenerThread();
	        t.setDaemon(false);
	        t.start();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		

		Ticker pe = new Ticker();
		pe.start();
		
		Thread udprecv = new Thread(new UDPReceiveQueue());
		udprecv.start();



//		CCanvas initialCanvas = new CCanvas(UUIDAllocator.getUUID());
//		CCanvasController.canvases.put(initialCanvas.getUUID(), initialCanvas);

		CalicoPluginManager.setup();
		
		if (!CalicoPluginManager.hasPlugin(
				"calico.plugins.iip.IntentionalInterfacesClientPlugin"))
			calico.controllers.CGridController.getInstance().initialize();

		
		try
		{
			sock = new ServerSocket(COptions.listen.port,50,listenHost);
//			sock.setSoTimeout(60000);

			logger.info("Opening socket");
			while(listening)
			{
				ClientManager.newClientThread( sock.accept() );
				//new ClientThread( sock.accept() ).start();
			}

			sock.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

	}//main
	
}
