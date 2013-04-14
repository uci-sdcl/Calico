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

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import calico.Calico;
import calico.CalicoDataStore;
import calico.CalicoOptions;
import calico.controllers.CCanvasController;
import calico.perspectives.CalicoPerspective;
import calico.perspectives.CanvasPerspective;
import calico.perspectives.GridPerspective;

public class Ticker extends Thread
{
	private ReferenceArrayList<TickerTask> ticker_tasks = new ReferenceArrayList<TickerTask>();
	
	public static Ticker ticker = null;
	
	private long tickcount = 0;
	private long ticker_starttime = 0;
	
	public Ticker()
	{
		Ticker.ticker = this;
	}
	
	public void run()
	{
		System.out.println("TICKET");
		ticker_starttime = System.currentTimeMillis();
		while(true)
		{
			this.tickcount++;
			
			// DO A BUNCH OF STUFF HERE
			
			// Check for tasks
			if(this.ticker_tasks.size()>0)
			{
				try
				{
					long curtime = System.currentTimeMillis();
					for(int i=(this.ticker_tasks.size()-1);i>=0;i--)
					{
						// do we want to run it?
						if(this.ticker_tasks.get(i).run_after<curtime)
						{
							// Run it
							if(!this.ticker_tasks.get(i).runtask())
							{
								this.ticker_tasks.remove(i);
							}
						}
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
			

			if(CalicoPerspective.Active.getCurrentPerspective()!=null && onTick(66) && CalicoPerspective.Active.getCurrentPerspective().isNavigationPerspective() && !Calico.isGridLoading )
			{
				CalicoPerspective.Active.getCurrentPerspective().tickerUpdate();
			}
			
			// TODO: maybe record when the last input was, and only run this after some time has passed
			try {
				if(onTick(66) && CCanvasController.getCurrentUUID()!=0L) {//
					//System.out.println(CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).isValid());
					
					//XXXXXXXX REMOVING THIS LINE TO SPEED THINGS UP XXXXXXX 
//					CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getLayer().repaint();
					
					//System.out.println(CalicoDataStore.calicoObj.isActive());
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
			
			
			if(onTick(CalicoOptions.core.tickrate*100) && !Calico.isGridLoading)
			{
				CCanvasController.no_notify_flush_dead_objects();
			}
			
			/*if(onTick(CalicoOptions.core.tickrate*CalicoOptions.core.hash_check_request_interval))
			{
				Networking.send(CalicoPacket.getPacket(NetworkCommand.GROUP_REQUEST_HASH_CHECK, 0L));
			}*/
			
			//if(onTick(66))
			//{
			//	Calico.logger.debug("TICKRATE: "+getAverageTickrate());
			//}
			
			// ^^^^^^^^^^^^^^^^^^^^^^^^^
			try
			{
				Thread.sleep( (long) (1000.0/CalicoOptions.core.tickrate) );
			}
			catch(InterruptedException ie)
			{
				
			}
		}//while
	}//run
	
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
		Ticker.ticker.ticker_tasks.add(task);
	}
	
	// miliseconds
	public static void scheduleIn(int inWhatTime, TickerTask task)
	{
		schedule(System.currentTimeMillis() + inWhatTime, task);
	}
	
	
}


