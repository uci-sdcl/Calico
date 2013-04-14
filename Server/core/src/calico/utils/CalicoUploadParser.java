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


import java.awt.Color;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.fileupload.MultipartStream;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpRequest;

// TODO: The parser should only look for the byte of '\r' and use that to get the boundary, instead of parsing the whole request


// Parse the uploads to the admin server. stupid java cant be bothered to easily do it on its own.
public class CalicoUploadParser
{
	public class Filedata
	{
		private String name = "";
		private String contentType = "application/octet-stream";
		private byte[] data = null;
		
		public Filedata(String name, String contentType, byte[] data)
		{
			this.contentType = contentType;
			this.data = data;
			this.name = name;
		}
		
		public String getName()
		{
			return this.name;
		}
		public String getContentType()
		{
			return this.contentType;
		}
		public byte[] getData()
		{
			return this.data;
		}
		public String toString()
		{
			return "File: "+this.name+" ["+this.contentType+"] "+this.data.length+" bytes";
		}
	}
	
	private HashMap<String, Filedata> fileFields = new HashMap<String, Filedata>();
		
	private byte[] data = null;
	private byte[] boundary = null;

	private Properties params = new Properties();
	private Properties fileInfo = new Properties();
	
	private byte[] fileData = null;
	

	// used to match color strings
	private static Pattern parse_param = Pattern.compile("Content-Disposition: form-data; name=\"([^\"]+)\"");
	private static Pattern parse_file_param = Pattern.compile("Content-Disposition: form-data; name=\"([^\"]+)\"; filename=\"([^\"]+)\"\\s+Content-Type: ([-a-z0-9/]+)");
	
	public CalicoUploadParser(byte[] data)
	{
		this.data = data;
	}
	public CalicoUploadParser(byte[] data, final HttpRequest request)
	{
		this.data = data;
		
		String contentType = request.getFirstHeader("Content-Type").getValue();
		int boundaryIndex = contentType.indexOf("boundary=");
		this.boundary = (contentType.substring(boundaryIndex + 9)).getBytes();
		
	}
	
	public Properties getParams()
	{
		return this.params;
	}
	
	
	public byte[] getFileData()
	{
		return this.fileData;
	}
	
	public Filedata getFile(String file)
	{
		return this.fileFields.get(file);
	}
	
	public void parse() throws IOException
	{
		
		ByteArrayInputStream input = new ByteArrayInputStream(this.data);
		MultipartStream multipartStream = new MultipartStream(input, this.boundary);
		
		boolean nextPart = multipartStream.skipPreamble();
		while(nextPart)
		{
			String headers = multipartStream.readHeaders().replaceAll("\r", " ").replaceAll("\n", " ");
			System.out.println("Headers: [" + headers+"]");
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			multipartStream.readBodyData(baos);
			
			Matcher param = parse_file_param.matcher(headers);
			if(param.find())
			{
				String pname = param.group(1);
				String filename = param.group(2);
				String contenttype = param.group(3);
				
				System.out.println("Pname="+pname+"|Filename="+filename+"|CType:"+contenttype);
				
				this.fileFields.put(pname, new Filedata(filename, contenttype, baos.toByteArray()));
				
			}
			else
			{
				
				Matcher param2 = parse_param.matcher(headers);
				if(param2.find())
				{
					this.params.setProperty(param2.group(1), new String(baos.toByteArray()) );
				}
				
			}
			  
			
			  
			//this.fileData = baos.toByteArray();
			  
			//System.out.println();
			
			nextPart = multipartStream.readBoundary();
		}
		
		/*
		-----------------------------9849436581144108930470211272
Content-Disposition: form-data; name="testvar"

testvalue
-----------------------------9849436581144108930470211272
Content-Disposition: form-data; name="Filedata"; filename="1TB_SAS_HDD.pdf"
Content-Type: application/pdf

%PDF-1.5
...
-----------------------------9849436581144108930470211272--

		 */
		/*
		String strData = new String(data);
		String eol = new String("\r\n");
		String eol2 = new String("\r\n\r\n");
		
		String[] boundaryarr = strData.split(eol, 2);
		
		String boundary = boundaryarr[0].trim();
		
		String[] parts = strData.split(boundary);
		
		// http://www.oreillynet.com/onjava/blog/2006/06/parsing_formdata_multiparts.html
		
		
		for(int i=0;i<parts.length;i++)
		{
			String[] subpart = parts[i].split(eol2,2);
			if(subpart[0].indexOf("Content-Type")!=-1)
			{
				Matcher param = parse_file_param.matcher(subpart[0]);
				if(param.find())
				{
					String pname = param.group(1);
					String value = param.group(2);//subpart[1].trim();
					String filedata = subpart[1];
					StringUtils.stripEnd(filedata, eol);
					this.fileData = filedata.getBytes();
					
					this.fileInfo.setProperty("type",param.group(3));
					this.fileInfo.setProperty("name",param.group(2));
					this.fileInfo.setProperty("size",Long.toString((long) this.fileData.length));
					
					System.out.println("NAME: "+pname+"="+value+"|"+param.group(3));
				}
			}
			else
			{
				
				Matcher param = parse_param.matcher(subpart[0]);
				if(param.find())
				{
					this.params.setProperty(param.group(1), subpart[1].trim());
				}
				
			}
			//strbuilder.append(parts[i]);
		}
		*/
	}
	
	public Properties getFileInfo()
	{
		return this.fileInfo;
	}
	
	
}
