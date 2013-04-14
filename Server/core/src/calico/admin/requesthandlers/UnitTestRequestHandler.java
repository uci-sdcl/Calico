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
package calico.admin.requesthandlers;


import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.geom.PathIterator;
import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.batik.dom.*;
import org.apache.batik.svggen.*;
import org.apache.http.*;
import org.apache.http.entity.*;
import org.apache.http.protocol.*;
import org.apache.http.util.*;

import org.apache.http.message.*;
import org.json.me.*;
import org.w3c.dom.*;

import calico.admin.*;
import calico.admin.exceptions.*;
import calico.clients.*;
import calico.controllers.*;
import calico.networking.netstuff.ByteUtils;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;
import calico.utils.*;
import calico.uuid.*;
import calico.*;

import java.util.zip.*;

public class UnitTestRequestHandler extends AdminBasicRequestHandler
{
	
	
	protected void handleRequest(final HttpRequest request, final HttpResponse response) throws HttpException, IOException, JSONException, CalicoAPIErrorException
	{
		Properties resp = new Properties();

		// SEE COptions.debug.unittests
		
		
		Properties params = request2params(request);
		
		String testToRun = params.getProperty("test","NONE").toUpperCase();
		

		
		resp.setProperty("Status", "OK");
		
		if(testToRun.equals("STROKES"))
		{
			test_strokes();
		}
		else if(testToRun.equals("BIGGESTPACKET"))
		{
			test_biggestpacket();
		}
		else if(testToRun.equals("PACKETSIZE"))
		{
			test_packetsize(Integer.valueOf(params.getProperty("size","1000")));
		}
		else
		{
			resp.setProperty("Status", "NO TEST SPECIFIED");
		}
		
		
		//// DRA GROUPS ////////////////////////////////
		/*for(int canv=0;canv<canvasUIDS.length;canv++)
		{
			long uuid = UUIDAllocator.getUUID();
			Rectangle rect = new Rectangle(0,0, 50+rand.nextInt(300), 50+rand.nextInt(300));
			rect.translate(50 + rand.nextInt(COptions.debug.unittests.bound_width-50), 50 + rand.nextInt(COptions.debug.unittests.bound_height-50));
			sendGroupShape(uuid, canvasUIDS[canv], rect.getPathIterator(null));
		}
		for(int canv=0;canv<canvasUIDS.length;canv++)
		{
			long uuid = UUIDAllocator.getUUID();
			Ellipse2D rect = new Ellipse2D.Float(	rand.nextInt(COptions.debug.unittests.bound_width/2), rand.nextInt(COptions.debug.unittests.bound_height/2), 
					50.0f+rand.nextInt(300), 50.0f+rand.nextInt(300));
			sendGroupShape(uuid, canvasUIDS[canv], rect.getPathIterator(null));
		}
		*/
		
		
		
		
		
		
		throw new SuccessException(resp);
	}
	
	
	private void sendGroupShape(long uuid, long cuid, PathIterator path)
	{
		double[] coordinates = new double[6];
		int coordnum = 0;
		while (path.isDone() == false)
		{
			//int type = path.currentSegment(coordinates);
			
			if(coordnum==0)
			{
				//CGroupController.start(uuid, cuid, 0L, true);//(int)coordinates[0], (int)coordinates[1]);
			}
			//CGroupController.append(uuid, (int)coordinates[0], (int)coordinates[1]);
			
			path.next();
			coordnum++;
		}
		//Networking.send(NetworkCommand.GROUP_SET_PERM,uuid,1);
		
		//CGroupController.finish(uuid);
		
	}
	
	private void test_biggestpacket()
	{
		for(int i=100;i<5000;i=i+100)
		{
			CalicoPacket packet = new CalicoPacket( ByteUtils.SIZE_OF_INT + ByteUtils.SIZE_OF_INT + (i*ByteUtils.SIZE_OF_LONG) );
			packet.putInt(NetworkCommand.DEBUG_PACKETSIZE);
			packet.putInt(i);
			for(int temp=0;temp<i;temp++)
			{
				packet.putLong(9289183918230L);
			}
			ClientManager.send(packet);
		}
	}
	private void test_packetsize(int size)
	{
		
		
		for(int i=0;i<1000;i++)
		{
			CalicoPacket packet = new CalicoPacket( ByteUtils.SIZE_OF_INT + ByteUtils.SIZE_OF_INT + (size*ByteUtils.SIZE_OF_LONG) );
			packet.putInt(NetworkCommand.DEBUG_PACKETSIZE);
			packet.putInt(size);
			for(int temp=0;temp<size;temp++)
			{
				packet.putLong(9289183918230L);
			}
			ClientManager.send(packet);
		}
	}
	
	private void test_strokes()
	{
		// Allocate a huge pool of UUIDs
		for(int i=0;i<100;i++)
		{
			UUIDAllocator.allocateMore();
		}
		
		
		
		long[] canvasUIDS = CCanvasController.canvases.keySet().toLongArray();
		Color[] colorList = {Color.RED, Color.BLUE, Color.BLACK, Color.GREEN, Color.PINK};
		
		Random rand = new Random();
		
		
		// Draw the star things
		for(int canv=0;canv<canvasUIDS.length;canv++)
		{
			long uuid = UUIDAllocator.getUUID();
			Color rcolor = colorList[ rand.nextInt(colorList.length) ];
			CStrokeController.no_notify_start(uuid, canvasUIDS[canv], 0L, rcolor, COptions.stroke.default_thickness);
			int offsetx = 50 + rand.nextInt(COptions.debug.unittests.bound_width-50);
			int offsety = 50 + rand.nextInt(COptions.debug.unittests.bound_height-50);
			
			int multip = 25+rand.nextInt(100);
			//theta=theta+1.0
			for(double theta=0.0;theta<=90;theta=theta+0.5)
			{
				double r = multip * Math.sin(4.0 * theta);
	
				int x = (int) (Math.round(r * Math.cos(theta)) + offsetx);
				int y = (int) (Math.round(r * Math.sin(theta)) + offsety);
				
				CStrokeController.no_notify_append(uuid, x, y);
			}
			CStrokeController.no_notify_finish(uuid);
			CStrokeController.reload(uuid);
		}
		
		// Disk with lines coming out
		for(int canv=0;canv<canvasUIDS.length;canv++)
		{
			
			int offsetx = 50 + rand.nextInt(COptions.debug.unittests.bound_width-50);
			int offsety = 50 + rand.nextInt(COptions.debug.unittests.bound_height-50);
			
			for(double theta=0.0;theta<360.0;theta++)
			{
				double r = 25.0;
				double r2 = 75.0;

				int x = (int) (Math.round(r * Math.cos(theta)) + offsetx);
				int y = (int) (Math.round(r * Math.sin(theta)) + offsety);

				int x2 = (int) (Math.round(r2 * Math.cos(theta)) + offsetx);
				int y2 = (int) (Math.round(r2 * Math.sin(theta)) + offsety);
				
				long uuid = UUIDAllocator.getUUID();
				CStrokeController.no_notify_start(uuid, canvasUIDS[canv], 0L, colorList[ rand.nextInt(colorList.length) ], COptions.stroke.default_thickness);
				CStrokeController.no_notify_append(uuid, x, y);
				CStrokeController.no_notify_append(uuid, x2, y2);
				CStrokeController.no_notify_finish(uuid);
				CStrokeController.reload(uuid);
				
			}
		}	
	}
	
	
	
}
