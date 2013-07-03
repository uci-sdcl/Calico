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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.StringTokenizer;

import org.apache.http.TokenIterator;

import calico.CalicoOptions;

public class Geometry {

    public static double angle(Point2D newMidPoint, Point2D newBoundsRightCorner) {
    	 
        double dx = newBoundsRightCorner.getX() - newMidPoint.getX();
        double dy = newBoundsRightCorner.getY() - newMidPoint.getY();
        double angle = 0.0d;
 
        if (dx == 0.0) {
            if(dy == 0.0)     angle = 0.0;
            else if(dy > 0.0) angle = Math.PI / 2.0;
            else              angle = (Math.PI * 3.0) / 2.0;
        }
        else if(dy == 0.0) {
            if(dx > 0.0)      angle = 0.0;
            else              angle = Math.PI;
        }
        else {
            if(dx < 0.0)      angle = Math.atan(dy/dx) + Math.PI;
            else if(dy < 0.0) angle = Math.atan(dy/dx) + (2*Math.PI);
            else              angle = Math.atan(dy/dx);
        }
        return angle;
    }	
    
	public static double angle(double xVertex, double yVertex, double xVector1, double yVector1, double xVector2, double yVector2)
	{
		double angle1 = Math.atan2(yVertex - yVector1, xVertex - xVector1);
		double angle2 = Math.atan2(yVertex - yVector2, xVertex - xVector2);
		return angle1 - angle2;
	}

	public static Point2D getMidPoint2D(Polygon p)
	{
		Point2D ret;
		if (p.getBounds() != null)
		{
			Rectangle bounds = p.getBounds();
			double xAvg = bounds.getCenterX();
			double yAvg = bounds.getCenterY();
			
			ret = new Point2D.Double(xAvg, yAvg);
		}
		else
		{
			long xSum = 0, ySum = 0;
			int totalPoints = p.npoints;
			for (int i = 0; i < p.npoints; i++)
			{
				if (p.xpoints[i] == 0 || p.ypoints[i] == 0)
				{
					totalPoints--;
					continue;
				}
				xSum += p.xpoints[i];
				ySum += p.ypoints[i];
			}
			double xAvg = ((double)xSum) / totalPoints;
			double yAvg = ((double)ySum) / totalPoints;
			
			ret = new Point2D.Double(xAvg, yAvg);
		}
		
		if (ret.getX() == 0 || ret.getY() == 0)
		{
//			System.err.println("A midpoint returned zero!");
//			(new Exception()).printStackTrace();
		}
		return ret;
	}
	
	public static Polygon getPolyFromPath(PathIterator it) {

		Polygon p = new Polygon();
		float[] point = new float[6];
		int type;
		while (!it.isDone()) {
			type = it.currentSegment(point);
			if (type == PathIterator.SEG_MOVETO
					|| type == PathIterator.SEG_LINETO
					|| type == PathIterator.SEG_CLOSE)
				p.addPoint(Math.round(point[0]), Math.round(point[1]));
			else if (type == PathIterator.SEG_QUADTO) {
				p.addPoint(Math.round(point[0]), Math.round(point[1]));
				p.addPoint(Math.round(point[2]), Math.round(point[3]));
			} else if (type == PathIterator.SEG_CUBICTO) {
				p.addPoint(Math.round(point[0]), Math.round(point[1]));
				p.addPoint(Math.round(point[2]), Math.round(point[3]));
				p.addPoint(Math.round(point[4]), Math.round(point[5]));
			}
			else
			{
				System.out.println("Missed a pointset from path! It is size: " + point.length + ", and type: " + type);
				(new Exception()).printStackTrace();
			}
			it.next();
		}
		return p;
	}
	
	public static GeneralPath getBezieredPoly(Polygon pts)
	{
		GeneralPath p = new GeneralPath();
		if (pts.npoints > 0)
		{
			p.moveTo(pts.xpoints[0], pts.ypoints[0]);
			if (pts.npoints >= 4)
			{
				for (int i = 1; i+3 < pts.npoints; i += 3)
				{
					p.curveTo(pts.xpoints[i], pts.ypoints[i], 
							pts.xpoints[i+1], pts.ypoints[i+1], 
							pts.xpoints[i+2], pts.ypoints[i+2]);
				}
			}
			else
			{
				for (int i = 1; i < pts.npoints; i++)
				{
					p.lineTo(pts.xpoints[i], pts.ypoints[i]);
				}
			}
		}
		return p;
	}
	
	public static Polygon getRoundedPolygon(Rectangle newBounds)
	{
		return getRoundedPolygon(newBounds, 0);
	}
	
	public static Polygon getRoundedPolygon(Rectangle newBounds, int padding) {
		Polygon coords = new Polygon();

		// The below was created by playing around with the applet from this
		// page: http://www.cse.unsw.edu.au/~lambert/splines/Bezier.html
		coords.addPoint(newBounds.x - padding, newBounds.y - padding + 5);
		coords.addPoint(newBounds.x - padding, newBounds.y - padding + 5);
		coords.addPoint(newBounds.x - padding, newBounds.y - padding); // upper
																		// left
																		// corner
		coords.addPoint(newBounds.x - padding + 5, newBounds.y - padding);
		coords.addPoint(newBounds.x - padding + 5, newBounds.y - padding);
		coords.addPoint(newBounds.x - padding + 5, newBounds.y - padding);

		coords.addPoint(newBounds.x + newBounds.width + padding - 5,
				newBounds.y - padding);
		coords.addPoint(newBounds.x + newBounds.width + padding - 5,
				newBounds.y - padding);
		coords.addPoint(newBounds.x + newBounds.width + padding, newBounds.y
				- padding); // upper right corner
		coords.addPoint(newBounds.x + newBounds.width + padding, newBounds.y
				- padding + 5);
		coords.addPoint(newBounds.x + newBounds.width + padding, newBounds.y
				- padding + 5);
		coords.addPoint(newBounds.x + newBounds.width + padding, newBounds.y
				- padding + 5);

		coords.addPoint(newBounds.x + newBounds.width + padding, newBounds.y
				+ newBounds.height + padding - 5);
		coords.addPoint(newBounds.x + newBounds.width + padding, newBounds.y
				+ newBounds.height + padding - 5);
		coords.addPoint(newBounds.x + newBounds.width + padding, newBounds.y
				+ newBounds.height + padding); // bottom right corner
		coords.addPoint(newBounds.x + newBounds.width + padding - 5,
				newBounds.y + newBounds.height + padding);
		coords.addPoint(newBounds.x + newBounds.width + padding - 5,
				newBounds.y + newBounds.height + padding);
		coords.addPoint(newBounds.x + newBounds.width + padding - 5,
				newBounds.y + newBounds.height + padding);

		coords.addPoint(newBounds.x - padding + 5, newBounds.y
				+ newBounds.height + padding);
		coords.addPoint(newBounds.x - padding + 5, newBounds.y
				+ newBounds.height + padding);
		coords.addPoint(newBounds.x - padding, newBounds.y + newBounds.height
				+ padding); // bottom left corner
		coords.addPoint(newBounds.x - padding, newBounds.y + newBounds.height
				+ padding - 5);
		coords.addPoint(newBounds.x - padding, newBounds.y + newBounds.height
				+ padding - 5);
		coords.addPoint(newBounds.x - padding, newBounds.y + newBounds.height
				+ padding - 5);

		coords.addPoint(newBounds.x - padding, newBounds.y - padding + 5); // connect
																			// back
																			// to
																			// top

		return coords;
	}
	
	public static Rectangle getTextBounds(String t) {
		Font f = CalicoOptions.group.font;
		return getTextBounds(t, f);
	}
	
	public static Rectangle getTextBounds(String t, Font f) {
		Graphics2D g2d = (Graphics2D) new BufferedImage(16, 16,
				BufferedImage.TYPE_INT_RGB).getGraphics();
		FontRenderContext frc = g2d.getFontRenderContext();
		
		Rectangle fontBounds = new Rectangle(0,0,0,0);
		String[] tokens = t.split("\n");
		for (int i = 0; i < tokens.length; i++)
		{
			Rectangle tempD = f.getStringBounds(tokens[i], frc).getBounds();
			fontBounds.setSize(Math.max(tempD.width, fontBounds.width), fontBounds.height + tempD.height);
		}
		return fontBounds;
	}

	public static Rectangle2D getCombinedBounds(Rectangle[] rects)
	{
		double lowX = Double.MAX_VALUE, lowY = Double.MAX_VALUE, highX = Double.MIN_VALUE, highY = Double.MIN_VALUE;
		
		Rectangle bounds;
		for (int i = 0; i < rects.length; i++)
		{
			bounds = rects[i];
			if (bounds.width == 0 || bounds.height == 0)
				continue;
			
			if (lowX > bounds.x)
				lowX = bounds.x;
			if (lowY > bounds.y)
				lowY = bounds.y;
			if (highX < bounds.x + bounds.width)
				highX = bounds.x + bounds.width;
			if (highY < bounds.y + bounds.height)
				highY = bounds.y + bounds.height;
		}
		
		return new Rectangle2D.Double(lowX, lowY, highX - lowX, highY - lowY);
	}
	
	public static double getPolygonLength(Polygon p)
	{
		double totalLen = 0f;
		if (p.npoints < 2)
			return 0;
		
		for (int i = 1; i < p.npoints; i++)
			totalLen += Point.distance(p.xpoints[i-1], p.ypoints[i-1], p.xpoints[i], p.ypoints[i]);
		
		
		return totalLen;
	}
	
}
