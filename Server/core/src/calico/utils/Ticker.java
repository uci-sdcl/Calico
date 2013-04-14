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
package calico.utils;

import calico.COptions;
import calico.CalicoServer;
import calico.networking.*;
import calico.networking.netstuff.*;
import calico.COptions.server;
import calico.admin.*;
import calico.clients.*;
import calico.uuid.*;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;

import java.net.*;

import org.apache.log4j.Logger;

public class Ticker extends Thread
{
	public static Logger logger = Logger.getLogger(Ticker.class.getName());

	private ReferenceArrayList<TickerTask> ticker_tasks_time = new ReferenceArrayList<TickerTask>();
	private ReferenceArrayList<TickerTask> ticker_tasks_ticks = new ReferenceArrayList<TickerTask>();
	
	public static Ticker ticker = null;

	private static int TICKRATE = 66;
	private static int TICKRATEx2 = 66;
	private static int TICKRATEx3 = 66;
	private static int TICKRATEx4 = 66;
	private static int TICKRATEx5 = 66;
	
	private static long ticker_sleeptime = 66L;
	
	
	
	private long tickcount = 0;
	private long ticker_starttime = 0;
	private int loop_tickcount = 0;

	
	
	public Ticker()
	{
		Ticker.ticker = this;

		Ticker.TICKRATE = COptions.server.tickrate;
		Ticker.TICKRATEx2 = COptions.server.tickrate * 2;
		Ticker.TICKRATEx3 = COptions.server.tickrate * 3;
		Ticker.TICKRATEx4 = COptions.server.tickrate * 4;
		Ticker.TICKRATEx5 = COptions.server.tickrate * 5;
		
		Ticker.ticker_sleeptime = (long) (1000.0/COptions.server.tickrate);
		
		
		//// CLEANUP of client queues
		Ticker.scheduleOnTick(Ticker.TICKRATEx3, new TickerTask(){
			public boolean runtask()
			{
				ClientManager.cleanup();
				return true;
			}
		});
		
		// Garbage collection
		Ticker.scheduleOnTick(Ticker.TICKRATE * 100, new TickerTask(){
			public boolean runtask()
			{
				System.gc();
				return true;
			}
		});
		
		/// AUTO BACKUP
		if(COptions.server.backup.enable_autobackup)
		{
			Ticker.scheduleOnTick(Ticker.TICKRATE * COptions.server.backup.write_on_tick, new TickerTask(){
				public boolean runtask()
				{
					try
					{
						CalicoBackupHandler.writeAutoBackupFile(COptions.server.backup.backup_file);
					}
					catch(Exception e)
					{
						CalicoServer.logger.error("Error: could not write auto-backup!");
						e.printStackTrace();
					}
					return true;
				}
			});
		}//enable_autobackup
	}
	
	public void run()
	{
		ticker_starttime = System.currentTimeMillis();
		while(true)
		{
			if(this.loop_tickcount>=COptions.server.tickrate)
			{
				this.loop_tickcount = 0;
			}
			
			this.loop_tickcount++;
			
			this.tickcount++;
			
			// DO A BUNCH OF STUFF HERE
			if(this.ticker_tasks_time.size()>0)
			{
				try
				{
					long curtime = System.currentTimeMillis();
					for(int i=(this.ticker_tasks_time.size()-1);i>=0;i--)
					{
						// do we want to run it?
						if(this.ticker_tasks_time.get(i).run_after<curtime)
						{
							// Run it
							if(!this.ticker_tasks_time.get(i).runtask())
							{
								this.ticker_tasks_time.remove(i);
							}
						}
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
			
			
			///// LOOP THRU TASKS TO RUN ON THE TICKRATE
			if(this.ticker_tasks_ticks.size()>0)
			{
				try
				{
					for(int i=(this.ticker_tasks_ticks.size()-1);i>=0;i--)
					{
						// do we want to run it?
						if(onTick(this.ticker_tasks_ticks.get(i).run_ontick))
						{
							try
							{
								// Run it
								if(!this.ticker_tasks_ticks.get(i).runtask())
								{
									this.ticker_tasks_ticks.remove(i);
								}
							}
							catch(Exception e)
							{
								e.printStackTrace();
							}
						}
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
			
			
			
			
			// TODO: Scan thru the stroke/group/arrow arrays, and remove things that are not being referenced anywhere.
			
			// TODO: Autosave the backup
			
			// TODO: Garbage collection
			
			// TODO: Make sure the arrays are all trimmed to whatever size they hold
			
			// ^^^^^^^^^^^^^^^^^^^^^^^^^
			try
			{
				Thread.sleep( Ticker.ticker_sleeptime );
			}
			catch(InterruptedException ie)
			{
				
			}
			
		}
	}
	

	private boolean onTick(int tick)
	{
		return ((this.tickcount%tick)==0);
	}
	
	
	public double getAverageTickrate()
	{
		double actual_tickrate = 0.0;
		
		double seconds = ((System.currentTimeMillis() - ticker_starttime)/1000.0);
		
		actual_tickrate = this.tickcount / seconds;
		
		return actual_tickrate;
	}
	
	
	
	public static void schedule(long whatTime, TickerTask task)
	{
		task.run_after = whatTime;
		task.tasktype = TickerTask.TASKTYPE_TIME;
		Ticker.ticker.ticker_tasks_time.add(task);
	}
	
	// miliseconds
	public static void scheduleIn(int inWhatTime, TickerTask task)
	{
		schedule(System.currentTimeMillis() + inWhatTime, task);
	}
	
	// miliseconds
	public static void scheduleOnTick(int onTicks, TickerTask task)
	{
		task.tasktype = TickerTask.TASKTYPE_TICK;
		task.run_ontick = onTicks;
		Ticker.ticker.ticker_tasks_ticks.add(task);
	}
	
	
}


