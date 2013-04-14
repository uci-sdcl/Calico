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
package calico.clients;

import calico.events.CalicoEventHandler;
import calico.events.CalicoEventListener;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;

public class ClientConsistencyListener implements CalicoEventListener {

	private static ClientConsistencyListener instance;
	
	public static boolean ignoreConsistencyCheck = false;
	
	public ClientConsistencyListener()
	{
		CalicoEventHandler.getInstance().addListener(NetworkCommand.GROUP_MOVE_START, this, CalicoEventHandler.PASSIVE_LISTENER);
		CalicoEventHandler.getInstance().addListener(NetworkCommand.GROUP_MOVE_END, this, CalicoEventHandler.PASSIVE_LISTENER);
		CalicoEventHandler.getInstance().addListener(NetworkCommand.ERASE_START, this, CalicoEventHandler.PASSIVE_LISTENER);
		CalicoEventHandler.getInstance().addListener(NetworkCommand.ERASE_END, this, CalicoEventHandler.PASSIVE_LISTENER);
		CalicoEventHandler.getInstance().addListener(NetworkCommand.CONNECTOR_MOVE_ANCHOR_START, this, CalicoEventHandler.PASSIVE_LISTENER);
		CalicoEventHandler.getInstance().addListener(NetworkCommand.CONNECTOR_MOVE_ANCHOR_END, this, CalicoEventHandler.PASSIVE_LISTENER);
		
//		System.out.println("~~~~~~~~~~~~ Instanciated Consisteny listener!! ~~~~~~~~~~~~");
	}
	
	public static ClientConsistencyListener getInstance()
	{
		if (instance == null)
			 instance = new ClientConsistencyListener();
		
		return instance;
	}
	
	@Override
	public void handleCalicoEvent(int event, CalicoPacket p, Client client) {
		
		switch (event)
		{
			case NetworkCommand.GROUP_MOVE_START:
				ignoreConsistencyCheck = true;
				break;
			case NetworkCommand.GROUP_MOVE_END:
				ignoreConsistencyCheck = false;
				break;
			case NetworkCommand.ERASE_START:
				ignoreConsistencyCheck = true;
				break;
			case NetworkCommand.ERASE_END:
				ignoreConsistencyCheck = false;
				break;
			case NetworkCommand.CONNECTOR_MOVE_ANCHOR_START:
				ignoreConsistencyCheck = true;
				break;
			case NetworkCommand.CONNECTOR_MOVE_ANCHOR_END:
				ignoreConsistencyCheck = false;
				break;
			default:
				break;
		}

	}

}
