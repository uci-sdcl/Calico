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

import javax.swing.ProgressMonitor;

import org.apache.james.mime4j.util.StringArrayMap;

import calico.events.CalicoEventHandler;
import calico.events.CalicoEventListener;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;

public class StatusMessageHandler implements CalicoEventListener {

	private ProgressMonitor progressMonitor;
	
	private static StringArrayMap progressMonitors = new StringArrayMap();
	
	private static StatusMessageHandler instance = null;
	
	public static StatusMessageHandler getInstance()
	{
		if (instance == null)
			instance = new StatusMessageHandler();
		
		return instance;
	}
	
	private StatusMessageHandler()
	{
		CalicoEventHandler.getInstance().addListener(NetworkCommand.STATUS_SENDING_LARGE_FILE_START, this, CalicoEventHandler.PASSIVE_LISTENER);
		CalicoEventHandler.getInstance().addListener(NetworkCommand.STATUS_SENDING_LARGE_FILE, this, CalicoEventHandler.PASSIVE_LISTENER);
		CalicoEventHandler.getInstance().addListener(NetworkCommand.STATUS_SENDING_LARGE_FILE_FINISHED, this, CalicoEventHandler.PASSIVE_LISTENER);
	}
	
	@Override
	public void handleCalicoEvent(int event, CalicoPacket p) {
		if (event == NetworkCommand.STATUS_SENDING_LARGE_FILE_START)
		{

			
			if (progressMonitor == null)
			{
				p.rewind();
				p.getInt();
				int progress = (int) ( p.getDouble() / p.getDouble() * 100); 
				String message = p.getString();
				
				startProgressMonitor(message);
			}
		}
		else if (event == NetworkCommand.STATUS_SENDING_LARGE_FILE)
		{

			p.rewind();
			p.getInt();
			int progress = Math.abs((int) ( p.getDouble() / p.getDouble() * 100)); 
			String message = p.getString();

			if (progressMonitor == null)
				startProgressMonitor(message);
//			progressMonitor.setNote(message);
			progressMonitor.setProgress(progress);
		}
		else if (event == NetworkCommand.STATUS_SENDING_LARGE_FILE_FINISHED)
		{
			p.rewind();
			p.getInt();
			int progress = (int) ( p.getDouble() / p.getDouble() * 100); 
			String message = p.getString();
			
			if (progressMonitor == null /*|| progressMonitor.getNote().compareTo(message) != 0 */)
				return;
			
			progressMonitor.close();
			progressMonitor = null;
		}	
	}
	
	
	public void startProgressMonitor(String message)
	{
		if (progressMonitor != null)
			return;
		progressMonitor = new ProgressMonitor(null,
                message,
                "", 0, 100);
		progressMonitor.setProgress(0);
		progressMonitor.setMillisToPopup(1);
		progressMonitor.setMillisToDecideToPopup(1000);
	}

}
