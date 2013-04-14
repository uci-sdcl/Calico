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
package calico.plugins;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import it.unimi.dsi.fastutil.objects.*;
import it.unimi.dsi.fastutil.ints.*;
import calico.COptions;
import calico.plugins.events.*;
import calico.plugins.events.scraps.*;
import calico.plugins.events.clients.*;
//import calico.plugins.events.*;

public class CalicoPluginManager
{
	// This is used to process the admin callbacks
	private static class AdminCallback
	{
		private Method method = null;
		private AbstractCalicoPlugin plugin = null;
		
		public AdminCallback(Method method, AbstractCalicoPlugin plugin)
		{
			this.method = method;
			this.plugin = plugin;
		}
		
		public void call(PluginCommandParameters params, StringBuilder output) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException
		{
			this.method.invoke(this.plugin, params, output);
		}
	}
	
	public static ArrayList<CalicoStateElement> calicoStateExtensions = new ArrayList<CalicoStateElement>();
	
	public static Logger logger = Logger.getLogger(CalicoPluginManager.class.getName());
	
	// this maps classnames to actual plugin objects
	private static Object2ReferenceArrayMap<Class<?>, AbstractCalicoPlugin> plugins = new Object2ReferenceArrayMap<Class<?>, AbstractCalicoPlugin>();
	
	// this just lists all the objects
	private static ReferenceArrayList<AbstractCalicoPlugin> pluginList = new ReferenceArrayList<AbstractCalicoPlugin>();
	
	// This is where we store the event registrations
	private static Object2ObjectOpenHashMap<String, Class<?>> eventRegistry = new Object2ObjectOpenHashMap<String, Class<?>>(); 
	
	// This stores the admin command callbacks
	private static Object2ObjectOpenHashMap<String, AdminCallback> adminCommandCallbacks = new Object2ObjectOpenHashMap<String, AdminCallback>();
	
	//////////////////////////////////////////////////////////////////////////////
	
	public static void receivePluginEvent(Class<?> plugin, CalicoEvent event)
	{
		try
		{
			handlePluginEvent(event, plugin);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	
	public static void registerAdminCommandCallback(String command, Method callback, AbstractCalicoPlugin plugin)
	{
		if(adminCommandCallbacks.containsKey(command))
		{
			return;
		}
		
		adminCommandCallbacks.put(command, new AdminCallback(callback, plugin));
		
	}
	
	
	public static boolean isAdminCommandRegistered(String command)
	{
		return adminCommandCallbacks.containsKey(command);
	}
	public static void callAdminCommand(String command, PluginCommandParameters params, StringBuilder output)
	{
		if(!isAdminCommandRegistered(command))
		{
			return;
		}
		
		try
		{
			adminCommandCallbacks.get(command).call(params, output);
		}
		catch (IllegalArgumentException e)
		{
			e.printStackTrace();
		}
		catch (IllegalAccessException e)
		{
			e.printStackTrace();
		}
		catch (InvocationTargetException e)
		{
			e.printStackTrace();
		}
	}
	
	
	
	public static void sendEventToPlugins(CalicoEvent event)
	{
		
		if(event.getClass().getSimpleName().equals("CalicoEvent"))
		{
			
		}
		else if(pluginList.size()>0)
		{
			String newMethodName = "on"+event.getClass().getSimpleName();
			
			for(int i=0;i<pluginList.size();i++)
			{
				try
				{
					AbstractCalicoPlugin plugin = pluginList.get(i);
					
					Method method = plugin.getClass().getMethod(newMethodName, event.getClass());
					try
					{
						method.invoke(plugin, event);
					}
					catch(Exception e)
					{
						plugin.onException(e);
					}
				}
				catch(NoSuchMethodException e)
				{
					// we ignore this
				}
				catch(Exception e2)
				{
					e2.printStackTrace();
				}
			}
			
		}
	}
	
	
	
	
	
	
	public static void registerPlugin(Class<?> pluginClass)
	{
		if(plugins.containsKey(pluginClass))
		{
			logger.error("Plugin "+pluginClass.getCanonicalName()+" has already been registered");
			return;
		}
		
		try
		{
			AbstractCalicoPlugin classObj = (AbstractCalicoPlugin) pluginClass.newInstance();
			
			try
			{
				logger.debug("Registering plugin "+classObj.PluginInfo.name);
				if (!classObj.registerNetworkCommandEvents())
				{
					System.err.println("PLUGIN WARNING: There is a command conflict with the plugin " + pluginClass.getName());
				}
				classObj.onPluginStart();
			}
			catch(Exception e)
			{
				classObj.onException(e);
				e.printStackTrace();
			}
			
			plugins.put(pluginClass, classObj);
			pluginList.add(classObj);

		}
		catch (InstantiationException e)
		{
			e.printStackTrace();
		}
		catch (IllegalAccessException e)
		{
			e.printStackTrace();
		}
	}
	
	
	public static void shutdownPlugins()
	{
		logger.debug("Shutting down plugins");

		for(int i=0;i<pluginList.size();i++)
		{
			try
			{
				
				AbstractCalicoPlugin plugin = pluginList.get(i);
				logger.debug("Shutting down plugin "+plugin.PluginInfo.name);
				try
				{
					plugin.onPluginEnd();
				}
				catch(Exception e)
				{
					plugin.onException(e);
				}
			}
			catch(Exception e2)
			{
				e2.printStackTrace();
			}
		}
		logger.debug("All plugins shutdown");
		
	}
	
	
	
	
	
	public static void registerEvent(String eventName, Class<?> eventClass)
	{	
		eventRegistry.put(eventName, eventClass);
	}
	public static void registerEvent(Class<?> eventClass)
	{
		registerEvent(eventClass.getSimpleName(), eventClass);
	}
	public static Class<?> getEventClass(String eventName)
	{
		if(eventRegistry.containsKey(eventName))
		{
			return eventRegistry.get(eventName);
		}
		else
		{
			return null;
		}
	}
	public static boolean hasPlugin(String pluginClassName)
	{
		ObjectSet<Class<?>> keySet = plugins.keySet();
		
		for (Class<?> c : keySet)
		{
			if (c.getName().compareTo(pluginClassName) == 0)
				return true;
		}
		
		return false;
//		return plugins.containsKey(pluginClass);
	}
	
	
	
	public static void setup()
	{
		// Register the default events
		registerEvent(calico.plugins.events.clients.ClientConnect.class);
		registerEvent(calico.plugins.events.clients.ClientDisconnect.class);

		registerEvent(calico.plugins.events.scraps.ScrapCreate.class);
		registerEvent(calico.plugins.events.scraps.ScrapDelete.class);
		registerEvent(calico.plugins.events.scraps.ScrapReload.class);
		
		
		//System.out.println("LOAD PLUGINS: "+COptions.server.plugins);
		
		logger.debug("Loading plugins");
		
		try
		{
			PluginFinder pluginFinder = new PluginFinder();
			pluginFinder.search("plugins/");
			List<Class<?>> pluginCollection = pluginFinder.getPluginCollection();
			for (Class<?> plugin: pluginCollection)
			{
				System.out.println("Loading " + plugin.getName());
				registerPlugin(plugin);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		String[] pluginsToLoad = COptions.server.plugins.split(",");
		
//		if(pluginsToLoad.length>0)
//		{
//			for(int i=0;i<pluginsToLoad.length;i++)
//			{
//				try
//				{
//					Class<?> pluginClass = Class.forName(pluginsToLoad[i].trim());
//					registerPlugin(pluginClass);
//				}
//				catch(Exception e)
//				{
//					logger.error("Could not load plugin "+pluginsToLoad[i].trim());
//				}
//			}
//		}
		
	
	}
	//////////////////////////////////////////////////////////////////////
	
	
	private static void handlePluginEvent(CalicoEvent event, Class<?> plugin) throws Exception
	{
		/*
		if(event instanceof ScrapReload)
		{
			
		}
		*/
		
		event.execute(plugin);
		
		
	}///
	
	public static void registerCalicoStateExtension(CalicoStateElement element)
	{
		calicoStateExtensions.add(element);
	}



	
}/// end
