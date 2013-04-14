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

import org.apache.http.*;
import java.util.*;

public class CalicoAPIErrorException extends Exception
{
	/**
	 * 
	 */
	protected static final long serialVersionUID = 1L;
	
	
	public String name = "UnknownError";
	public String message = "";
	public int code = HttpStatus.SC_BAD_REQUEST;
	public String comments = null;
	
	
	public CalicoAPIErrorException(int code, String n, String m)
	{
		this.code = code;
		this.name = n;
		this.message = m;
	}
	
	public CalicoAPIErrorException(){this("UnknownError");}
	public CalicoAPIErrorException(String n){this(n,"");}
	public CalicoAPIErrorException(int c, String n){this(c, n,"");}
	public CalicoAPIErrorException(String n, String m){this(HttpStatus.SC_BAD_REQUEST, n, m);}
	
	public String toJSON()
	{
		String resp = "{\"CalicoAPIError\":{";
		resp += "\"code\":\""+name+"\"";
		if(message.length()>0)
		{
			resp += ",\"message\":\""+message.replaceAll("\"", "\\\"")+"\"";
		}
		resp += "}}";
		return resp;
		//{"memory":{"max":532742144,"used":753424,"total":2031616,"free":1278192}}
	}
	
	public Properties toProperties()
	{
		Properties props = new Properties();
		props.setProperty("ErrorName", this.name);
		props.setProperty("ErrorMessage", this.message);
		props.setProperty("ErrorCode", Integer.toString(this.code));
		return props;
	}

}
