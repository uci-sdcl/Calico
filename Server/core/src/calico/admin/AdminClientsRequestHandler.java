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

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.http.*;
import org.apache.http.entity.*;
import org.apache.http.protocol.*;
import org.apache.http.util.*;

import org.json.me.*;

public class AdminClientsRequestHandler implements HttpRequestHandler
{
        
    
    public AdminClientsRequestHandler()
    {
        super();
    }
    
    public void handle(final HttpRequest request, final HttpResponse response, final HttpContext context) throws HttpException, IOException
    {

        String method = request.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);
        if (!method.equals("GET") && !method.equals("HEAD") && !method.equals("POST"))
        {
            throw new MethodNotSupportedException(method + " method not supported"); 
        }
        final String target = request.getRequestLine().getUri();

        JSONObject jsondata = new JSONObject();
        
        if (request instanceof HttpEntityEnclosingRequest)
        {
            HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
            byte[] entityContent = EntityUtils.toByteArray(entity);
            String content = new String(entityContent);
            System.out.println("Incoming entity content (bytes): " + entityContent.length);
            System.out.println("Incoming entity content (bytes): " + content);
            try
            {
            	jsondata = new JSONObject(content);
            }
            catch(Exception e)
            {
            }
        }
    
        String apikey = request.getFirstHeader("X-Calico-APIKey").getValue();

        test(request, response, context);
        
        /*
        try
        {
        
        response.setStatusCode(HttpStatus.SC_OK);
        response.addHeader("X-Calico-RequestNumber", System.currentTimeMillis()+"");
        response.addHeader("X-Calico-Apikeygiven", apikey);
        StringEntity body = new StringEntity("You requested "+target+" file"+jsondata.getString("test"));
        body.setContentType("text/plain");
        
        response.setEntity(body);
        }
        catch(JSONException ex)
        {
        	
        }*/
   
    }
    
    public void test(final HttpRequest request, final HttpResponse response, final HttpContext context) throws HttpException, IOException
    {
    	 response.setStatusCode(HttpStatus.SC_OK);
         response.addHeader("X-Calico-RequestNumber", System.currentTimeMillis()+"");
         response.addHeader("X-Calico-Apikeygiven", "BLAH");
         StringEntity body = new StringEntity("You requested YAR file");
         body.setContentType("text/plain");
         
         response.setEntity(body);
    }
    
    
}
