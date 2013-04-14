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
package calico.plugins.designminders;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;

import calico.plugins.*;

public class DesignMindersPlugin extends AbstractCalicoPlugin implements CalicoPlugin
{
	public DesignMindersPlugin()
	{
		super();
		
		PluginInfo.name = "DesignMindersPlugin";
		PluginInfo.author = "mdempsey";
		PluginInfo.info = "DesignMinders Interface";
		PluginInfo.url = "http://google.com/";
	}
	
	public void onPluginStart()
	{
		RegisterPluginEvent(calico.plugins.designminders.DMScrap2Card.class);
	}
	
	public void onPluginEnd()
	{
		
	}
	
	public void onDMScrap2Card(DMScrap2Card event)
	{
		debug("RECEIVED EVENT DMSCRAP2CARD - "+event.uuid);
		
		try
		{
			HttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost(GetConfigString("plugin.designminders.posturl"));
			
			post.addHeader("X-Calico-Version", "3.0");
			post.addHeader("User-Agent", "Calico3Server HTTP API/3.0");
			
			MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
			

			reqEntity.addPart("name", new StringBody("Insert card name"));
			reqEntity.addPart("desc", new StringBody("Insert card description"));
			reqEntity.addPart("tags", new StringBody("tags,tag,thing"));
			reqEntity.addPart("color", new StringBody("ff0000"));
			
			
			
			Rectangle rect = GetScrap(event.uuid).getPathReference().getBounds();
			
			BufferedImage buf = new BufferedImage(rect.width+10, rect.height+10, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = (Graphics2D) buf.getGraphics();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.translate(-1*rect.x, -1*rect.y);
			GetScrap(event.uuid).render(g, true);
			//g.drawImage(image, 0,0,null);
			
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ImageIO.write(buf, "PNG", bos);
			
			byte[] imagedata = bos.toByteArray();
			
			reqEntity.addPart("image", new InputStreamKnownSizeBody(  
				        new ByteArrayInputStream(imagedata),
				        imagedata.length,
				        "image/png", "test.png"));  
			
			post.setEntity(reqEntity);
			
			HttpResponse resp = client.execute(post);
			HttpEntity respEnt = resp.getEntity();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
	}


	class InputStreamKnownSizeBody extends InputStreamBody
	{  
		private int lenght;  

		public InputStreamKnownSizeBody(final InputStream in, final int lenght,final String mimeType, final String filename)
		{  
			super(in, mimeType, filename);  
			this.lenght = lenght;  
		}  

		@Override  
		public long getContentLength()
		{  
			return this.lenght;  
		}  
	}


	@Override
	public Class<?> getNetworkCommandsClass() {
		// TODO Auto-generated method stub
		return null;
	}  

}
