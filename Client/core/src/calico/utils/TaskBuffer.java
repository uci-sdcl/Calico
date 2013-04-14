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

import javax.swing.SwingUtilities;

/**
 * Buffers repetitive tasks such that they are executed no more frequently than the specified
 * <code>minExecutionInterval</code>. The <code>client</code> notifies this buffer for each new task, and this buffer in
 * turn notifies the client when it is time to execute the tasks. The definition and execution of the tasks remain in
 * the hands of the client, so this buffer serves only as a scheduling mechanism. Client tasks are invoked on the AWT
 * EventDispatchThread for simplicity of thread coordination.
 * 
 * @author Byron Hawkins
 */
public class TaskBuffer
{
	public interface Client
	{
		void executeTasks();
	}

	private final Client client;
	private final long minExecutionInterval;

	private final Daemon daemon = new Daemon();
	private final ClientTask clientTask = new ClientTask();

	public TaskBuffer(Client client, long minExecutionInterval)
	{
		this.client = client;
		this.minExecutionInterval = minExecutionInterval;
	}

	public void start()
	{
		daemon.start();
	}

	public void taskPending()
	{
		daemon.wakeUp();
	}
	
	private String getThreadName()
	{
		return getClass().getSimpleName();
	}

	private class Daemon extends Thread
	{
		private long lastBroadcast = 0L;
		private boolean taskPending = false;

		Daemon()
		{
			super(getThreadName());

			setDaemon(true);
		}

		void wakeUp()
		{
			synchronized (this)
			{
				taskPending = true;
				notify();
			}
		}

		@Override
		public void run()
		{
			while (true)
			{
				try
				{
					synchronized (this)
					{
						if (taskPending)
						{
							long bufferingPause;
							while ((bufferingPause = minExecutionInterval - (System.currentTimeMillis() - lastBroadcast)) > 0L)
							{
								try
								{
									wait(bufferingPause);
								}
								catch (InterruptedException wakeUpCall)
								{
								}
							}
							invokeTasks();
						}
						else
						{
							try
							{
								wait();
							}
							catch (InterruptedException wakeUpCall)
							{
							}

							if (taskPending && ((System.currentTimeMillis() - lastBroadcast) > minExecutionInterval))
							{
								invokeTasks();
							}
						}
					}
				}
				catch (Throwable t)
				{
					t.printStackTrace();
				}
			}
		}

		private synchronized void invokeTasks()
		{
			taskPending = false;
			lastBroadcast = System.currentTimeMillis();
			SwingUtilities.invokeLater(clientTask);
		}
	}

	private class ClientTask implements Runnable
	{
		@Override
		public void run()
		{
			client.executeTasks();
		}
	}
}
