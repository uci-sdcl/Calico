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

import java.util.*;

import org.json.me.*;

import java.awt.*;
import java.awt.color.*;
import java.awt.geom.PathIterator;

/**
 * Various utilities and what nots
 * @author mdempsey
 *
 */
public class CalicoUtils
{
	
	public static JSONObject point2JSON(Point point) throws JSONException
	{
		return point2JSON(point.x, point.y);
	}
	public static JSONObject point2JSON(int x, int y) throws JSONException
	{
		JSONObject obj = new JSONObject();

		obj.put("x", x);
		obj.put("y", y);
		
		return obj;
	}
	
	public static JSONObject color2JSON(Color color) throws JSONException
	{
		JSONObject colObj = new JSONObject();
		colObj.put("b", color.getBlue());
		colObj.put("g", color.getGreen());
		colObj.put("r", color.getRed());
		return colObj;
	}
	
	
	public static Point json2point(JSONObject obj) throws JSONException
	{
		return new Point( obj.getInt("x"), obj.getInt("y") );
	}
	
	public static Color json2color(JSONObject obj) throws JSONException
	{
		return new Color( obj.getInt("r"), obj.getInt("g"), obj.getInt("b") );
	}
	
	/**
	 * Clones a polygon
	 * @param poly
	 * @return
	 */
	public static Polygon copyPolygon(Polygon poly)
	{
		Polygon temp = new Polygon();
		for(int i=0;i<poly.npoints;i++)
		{
			temp.addPoint(poly.xpoints[i], poly.ypoints[i]);
		}
		return temp;
	}
	
	
	public static boolean polygonsEqual(Polygon poly1, Polygon poly2)
	{
		
		return (
			(Arrays.equals(poly1.xpoints, poly2.xpoints))
			&&
			(Arrays.equals(poly1.ypoints, poly2.ypoints))
		);
		
	}
	
	public static Polygon pathIterator2Polygon(PathIterator path)
	{
		Polygon poly = new Polygon();
		
		double[] coordinates = new double[6];
		while (path.isDone() == false)
		{
			//int type = path.currentSegment(coordinates);
			
			poly.addPoint((int)coordinates[0], (int)coordinates[1]);
			
			path.next();
		}
		
		return poly;
	}
	
	
	public static Point polygonDelta(Polygon p1, Polygon p2)
	{
		int xdelta = p2.xpoints[0] - p1.xpoints[0];
		int ydelta = p2.ypoints[0] - p1.ypoints[0];
		
		return new Point(xdelta, ydelta);
	}
	

	public static String printByteSize(long bytes)
	{
		if (bytes >= 1073741824)
	    {
			return ""+(Math.round(bytes/ 1073741824 * 100) / 100)+ "G";
	    }
	    else if (bytes >= 1048576)
	    {
	    	return ""+(Math.round(bytes / 1048576 * 100) / 100) + "M";
	    }
	    else if (bytes >= 1024)
	    {
	    	return ""+(Math.round(bytes / 1024 * 100) / 100) + "K";
	    }
	    else
	    {
	    	return ""+bytes+"B";
	    }
	}
	
	
	
	public static String properties2string(Properties props)
	{
		StringBuffer responseStr = new StringBuffer();
		
		for (Enumeration<?> e = props.propertyNames(); e.hasMoreElements();)
		{
			String keyStr = (String) e.nextElement();
			responseStr.append(keyStr+"="+props.getProperty(keyStr)+"\n");
		}
		return responseStr.toString();
	}
	
	
	public static String cleanFilename(String filename)
	{
		filename = filename.toLowerCase();
		filename.replaceAll("/[ _a-z0-9^]/", "");
		filename.replaceAll(" ", "_");
		
		return filename;
	}
	
	
	
}
