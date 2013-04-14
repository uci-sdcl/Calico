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
package calico.sessions;
import java.util.*;
import java.net.*;
import java.io.*;

import calico.networking.*;
import calico.admin.*;
import calico.clients.*;
import calico.uuid.*;
import calico.components.*;
import calico.networking.netstuff.*;


import it.unimi.dsi.fastutil.objects.*;

public class SessionInfo
{
	public int sessionid = 0;
	public String name = "";
	public int rows = 0;
	public int cols = 0;
	
	
	/**
	 * Creates a session Info class, this just has info on the session, no more
	 * @param id
	 * @param n
	 * @param r
	 * @param c
	 */
	public SessionInfo(int id, String n, int r, int c)
	{
		sessionid = id;
		name = n;
		rows = r;
		cols = c;
	}
	
	/**
	 * Returns the session id
	 * @return
	 */
	public int getSessionID()
	{
		return sessionid;
	}
	
	public int getRows()
	{
		return rows;
	}
	public int getCols()
	{
		return cols;
	}
	public String getName()
	{
		return name;
	}

	
}
