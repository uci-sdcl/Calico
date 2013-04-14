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
package calico.components;

import calico.*;
import calico.networking.*;
import calico.networking.netstuff.*;
import calico.admin.*;
import calico.clients.*;
import calico.uuid.*;

import java.util.*;
import java.io.*;
import java.awt.Point;
import java.awt.Polygon;

public class Scrap implements Serializable
{
	private String uuid;
	private String puid;
	private String cuid;
	private static final long serialVersionUID = 42L;

	//Vector<Point> points;
	Polygon points;

	public Vector<String> children_scraps;
	public Vector<String> children_bges;
	Vector<String> arrows;


	public Scrap(String u, String c)
	{
		uuid = u;
		cuid = c;
		puid = "";

		points = new Polygon();

		children_scraps = new Vector<String>();
		children_bges = new Vector<String>();
		arrows = new Vector<String>();
	}

	public void addPoint(int x, int y)
	{
		points.addPoint(x,y);
	}

	public void addScrap(String c)
	{
		if(!children_scraps.contains(c))
		{
			children_scraps.add(c);
		}
	}
	public void clearScraps()
	{
		children_scraps.clear();
	}

	public void addBGElement(String c)
	{
		if(!children_bges.contains(c))
		{
			children_bges.add(c);
		}
	}
	public void clearBGElements()
	{
		children_bges.clear();
	}

	public void addArrow(String c)
	{
		arrows.add(c);
	}

	public void delete()
	{
		/*for(int i=0;i<children_bges.size();i++)
		{
			BGElement bge = ProcessQueue.bgElements.get( children_bges.get(i) );
			bge.delete();
		}
		children_bges.clear();

		// delete the arrows
		for(int i=0;i<arrows.size();i++)
		{
			ProcessQueue.arrows.get(arrows.get(i)).delete();
		}
		arrows.clear();

		for(int i=0;i<children_scraps.size();i++)
		{
			Scrap scrap = ProcessQueue.scraps.get( children_scraps.get(i) );
			scrap.delete();
		}
		children_scraps.clear();

		points.reset();

		ProcessQueue.scraps.remove( uuid );*/
	}

	public ArrayList<CalicoPacket> getPackets()
	{
		ArrayList<CalicoPacket> pk = new ArrayList<CalicoPacket>();

		if(points.npoints==0)
		{
			return pk;
		}

		CalicoPacket s = new CalicoPacket( NetworkCommand.SCRAP_START );
		s.putString(uuid);
		s.putString(cuid);
		s.putInt(points.xpoints[0]);
		s.putInt(points.ypoints[0]);
		pk.add(s);

		// points
		for(int i=1;i<points.npoints;i++)
		{
			CalicoPacket p = new CalicoPacket( NetworkCommand.SCRAP_APPEND );
			p.putString(uuid);
			p.putInt(points.xpoints[i]);
			p.putInt(points.ypoints[i]);
			pk.add(p);
		}


		// end packet
		CalicoPacket e = new CalicoPacket( NetworkCommand.SCRAP_FINISH );
		e.putString(uuid);
		pk.add(e);

		if(puid.length()>0)
		{
			CalicoPacket p = new CalicoPacket( NetworkCommand.SCRAP_PARENT);
			p.putString(uuid);
			p.putString(puid);
			pk.add(p);
		}


		return pk;
	}

	public void move(int x, int y)
	{
		points.translate(x,y);
	}//


}


