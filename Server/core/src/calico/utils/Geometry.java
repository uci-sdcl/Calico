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

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;

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
			System.err.println("A midpoint returned zero!");
//			(new Exception()).printStackTrace();
		}
		return ret;
	}
	
	public static Point getMidPoint(Polygon p)
	{
		Point2D point2d = getMidPoint2D(p);
		return new Point((int)point2d.getX(), (int)point2d.getY());
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
	
	public static Polygon getRoundedPolygon(Rectangle newBounds) {
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
	
	/**
	 * Find the point on the line defined by x0,y0,x1,y1 a given fraction
	 * from x0,y0. 2D version of method above..
	 * 
	 * @param  x0, y0         First point defining the line
	 * @param  x1, y1         Second point defining the line
	 * @param  fractionFrom0  Distance from (x0,y0)
	 * @return x, y           Coordinate of point we are looking for
	 */
	public static double[] computePointOnLine (double x0, double y0,
			double x1, double y1,
			double fractionFrom0)
	{
		double[] p0 = {x0, y0, 0.0};
		double[] p1 = {x1, y1, 0.0};

		double[] p = Geometry.computePointOnLine (p0, p1, fractionFrom0);

		double[] r = {p[0], p[1]};
		return r;
	}
	
	/**
	 * Find the point on the line p0,p1 [x,y,z] a given fraction from p0.
	 * Fraction of 0.0 whould give back p0, 1.0 give back p1, 0.5 returns
	 * midpoint of line p0,p1 and so on. F
	 * raction can be >1 and it can be negative to return any point on the
	 * line specified by p0,p1.
	 * 
	 * @param p0              First coordinale of line [x,y,z].
	 * @param p0              Second coordinale of line [x,y,z].   
	 * @param fractionFromP0  Point we are looking for coordinates of
	 * @param p               Coordinate of point we are looking for
	 */
	public static double[] computePointOnLine (double[] p0, double[] p1, 
			double fractionFromP0)
	{
		double[] p = new double[3];

		p[0] = p0[0] + fractionFromP0 * (p1[0] - p0[0]);
		p[1] = p0[1] + fractionFromP0 * (p1[1] - p0[1]);
		p[2] = p0[2] + fractionFromP0 * (p1[2] - p0[2]);  

		return p;
	}
	
	/**
	 * Compute the intersection between two line segments, or two lines
	 * of infinite length.
	 * 
	 * @param  x0              X coordinate first end point first line segment.
	 * @param  y0              Y coordinate first end point first line segment.
	 * @param  x1              X coordinate second end point first line segment.
	 * @param  y1              Y coordinate second end point first line segment.
	 * @param  x2              X coordinate first end point second line segment.
	 * @param  y2              Y coordinate first end point second line segment.
	 * @param  x3              X coordinate second end point second line segment.
	 * @param  y3              Y coordinate second end point second line segment.
	 * @param  intersection[2] Preallocated by caller to double[2]
	 * @return -1 if lines are parallel (x,y unset),
	 *         -2 if lines are parallel and overlapping (x, y center)
	 *          0 if intesrection outside segments (x,y set)
	 *         +1 if segments intersect (x,y set)
	 */
	public static int findLineSegmentIntersection (double x0, double y0,
			double x1, double y1,
			double x2, double y2,
			double x3, double y3,
			double[] intersection)
	{
		// TODO: Make limit depend on input domain
		final double LIMIT    = 1e-5;
		final double INFINITY = 1e10;

		double x, y;

		//
		// Convert the lines to the form y = ax + b
		//

		// Slope of the two lines
		double a0 = Geometry.equals (x0, x1, LIMIT) ?
				INFINITY : (y0 - y1) / (x0 - x1);
		double a1 = Geometry.equals (x2, x3, LIMIT) ?
				INFINITY : (y2 - y3) / (x2 - x3);

		double b0 = y0 - a0 * x0;
		double b1 = y2 - a1 * x2;

		// Check if lines are parallel
		if (Geometry.equals (a0, a1)) {
			if (!Geometry.equals (b0, b1))
				return -1; // Parallell non-overlapping

			else {
				if (Geometry.equals (x0, x1)) {
					if (Math.min (y0, y1) < Math.max (y2, y3) ||
							Math.max (y0, y1) > Math.min (y2, y3)) {
						double twoMiddle = y0 + y1 + y2 + y3 -
						Geometry.min (y0, y1, y2, y3) -
						Geometry.max (y0, y1, y2, y3);
						y = (twoMiddle) / 2.0;
						x = (y - b0) / a0;
					}
					else return -1;  // Parallell non-overlapping
				}
				else {
					if (Math.min (x0, x1) < Math.max (x2, x3) ||
							Math.max (x0, x1) > Math.min (x2, x3)) {
						double twoMiddle = x0 + x1 + x2 + x3 -
						Geometry.min (x0, x1, x2, x3) -
						Geometry.max (x0, x1, x2, x3);
						x = (twoMiddle) / 2.0;
						y = a0 * x + b0;
					}
					else return -1;
				}

				intersection[0] = x;
				intersection[1] = y;
				return -2;
			}
		}

		// Find correct intersection point
		if (Geometry.equals (a0, INFINITY)) {
			x = x0;
			y = a1 * x + b1;
		}
		else if (Geometry.equals (a1, INFINITY)) {
			x = x2;
			y = a0 * x + b0;
		}
		else {
			x = - (b0 - b1) / (a0 - a1);
			y = a0 * x + b0; 
		}

		intersection[0] = x;
		intersection[1] = y;

		// Then check if intersection is within line segments
		double distanceFrom1;
		if (Geometry.equals (x0, x1)) {
			if (y0 < y1)
				distanceFrom1 = y < y0 ? Geometry.length (x, y, x0, y0) :
					y > y1 ? Geometry.length (x, y, x1, y1) : 0.0;
					else
						distanceFrom1 = y < y1 ? Geometry.length (x, y, x1, y1) :
							y > y0 ? Geometry.length (x, y, x0, y0) : 0.0;
		}
		else {
			if (x0 < x1)
				distanceFrom1 = x < x0 ? Geometry.length (x, y, x0, y0) :
					x > x1 ? Geometry.length (x, y, x1, y1) : 0.0;
					else
						distanceFrom1 = x < x1 ? Geometry.length (x, y, x1, y1) :
							x > x0 ? Geometry.length (x, y, x0, y0) : 0.0;
		}

		double distanceFrom2;
		if (Geometry.equals (x2, x3)) {
			if (y2 < y3)
				distanceFrom2 = y < y2 ? Geometry.length (x, y, x2, y2) :
					y > y3 ? Geometry.length (x, y, x3, y3) : 0.0;
					else
						distanceFrom2 = y < y3 ? Geometry.length (x, y, x3, y3) :
							y > y2 ? Geometry.length (x, y, x2, y2) : 0.0;
		}
		else {
			if (x2 < x3)
				distanceFrom2 = x < x2 ? Geometry.length (x, y, x2, y2) :
					x > x3 ? Geometry.length (x, y, x3, y3) : 0.0;
					else
						distanceFrom2 = x < x3 ? Geometry.length (x, y, x3, y3) :
							x > x2 ? Geometry.length (x, y, x2, y2) : 0.0;
		}

		return Geometry.equals (distanceFrom1, 0.0) &&
		Geometry.equals (distanceFrom2, 0.0) ? 1 : 0;
	}
	
	/**
	 * Check if two double precision numbers are "equal", i.e. close enough
	 * to a given limit.
	 * 
	 * @param a      First number to check
	 * @param b      Second number to check   
	 * @param limit  The definition of "equal".
	 * @return       True if the twho numbers are "equal", false otherwise
	 */
	private static boolean equals (double a, double b, double limit)
	{
		return Math.abs (a - b) < limit;
	}



	/**
	 * Check if two double precision numbers are "equal", i.e. close enough
	 * to a prespecified limit.
	 * 
	 * @param a  First number to check
	 * @param b  Second number to check   
	 * @return   True if the twho numbers are "equal", false otherwise
	 */
	private static boolean equals (double a, double b)
	{
		return equals (a, b, 1.0e-5);
	}
	
	/**
	 * Return the length of a vector.
	 * 
	 * @param v  Vector to compute length of [x,y,z].
	 * @return   Length of vector.
	 */
	public static double length (double[] v)
	{
		return Math.sqrt (v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
	}



	/**
	 * Compute distance between two points.
	 * 
	 * @param p0, p1  Points to compute distance between [x,y,z].
	 * @return        Distance between points.
	 */
	public static double length (double[] p0, double[] p1)
	{
		double[] v = Geometry.createVector (p0, p1);
		return length (v);
	}



	/**
	 * Compute the length of the line from (x0,y0) to (x1,y1)
	 * 
	 * @param x0, y0  First line end point.
	 * @param x1, y1  Second line end point.
	 * @return        Length of line from (x0,y0) to (x1,y1).
	 */
	public static double length (int x0, int y0, int x1, int y1)
	{
		return Geometry.length ((double) x0, (double) y0,
				(double) x1, (double) y1);
	}



	/**
	 * Compute the length of the line from (x0,y0) to (x1,y1)
	 * 
	 * @param x0, y0  First line end point.
	 * @param x1, y1  Second line end point.
	 * @return        Length of line from (x0,y0) to (x1,y1).
	 */
	public static double length (double x0, double y0, double x1, double y1)
	{
		double dx = x1 - x0;
		double dy = y1 - y0;

		return Math.sqrt (dx*dx + dy*dy);
	}



	/**
	 * Compute the length of a polyline.
	 * 
	 * @param x, y     Arrays of x,y coordinates
	 * @param nPoints  Number of elements in the above.
	 * @param isClosed True if this is a closed polygon, false otherwise
	 * @return         Length of polyline defined by x, y and nPoints.
	 */
	public static double length (int[] x, int[] y, boolean isClosed)
	{
		double length = 0.0;

		int nPoints = x.length;
		for (int i = 0; i < nPoints-1; i++)
			length += Geometry.length (x[i], y[i], x[i+1], y[i+1]);

		// Add last leg if this is a polygon
		if (isClosed && nPoints > 1)
			length += Geometry.length (x[nPoints-1], y[nPoints-1], x[0], y[0]);

		return length;
	}
	
	/**
	 * Return smallest of four numbers.
	 * 
	 * @param a  First number to find smallest among.
	 * @param b  Second number to find smallest among.
	 * @param c  Third number to find smallest among.
	 * @param d  Fourth number to find smallest among.   
	 * @return   Smallest of a, b, c and d.
	 */
	private static double min (double a, double b, double c, double d)
	{
		return Math.min (Math.min (a, b), Math.min (c, d));
	}
	
	/**
	 * Return largest of four numbers.
	 * 
	 * @param a  First number to find largest among.
	 * @param b  Second number to find largest among.
	 * @param c  Third number to find largest among.
	 * @param d  Fourth number to find largest among.   
	 * @return   Largest of a, b, c and d.
	 */
	private static double max (double a, double b, double c, double d)
	{
		return Math.max (Math.max (a, b), Math.max (c, d));
	}
	
	/**
	 * Construct the vector specified by two points.
	 * 
	 * @param  p0, p1  Points the construct vector between [x,y,z].
	 * @return v       Vector from p0 to p1 [x,y,z].
	 */
	public static double[] createVector (double[] p0, double[] p1)
	{
		double v[] = {p1[0] - p0[0], p1[1] - p0[1], p1[2] - p0[2]};
		return v;
	}
	
	/**
	 * Compute the area of the specfied polygon.
	 * 
	 * @param x  X coordinates of polygon.
	 * @param y  Y coordinates of polygon.   
	 * @return   Area of specified polygon.
	 */
	public static double computePolygonArea (double[] x, double[] y)
	{
		int n = x.length;

		double area = 0.0;
		for (int i = 0; i < n - 1; i++)
			area += (x[i] * y[i+1]) - (x[i+1] * y[i]);
		area += (x[n-1] * y[0]) - (x[0] * y[n-1]);    

		area *= 0.5;

		return Math.abs(area);
	}
	
	public static double computePolygonArea (Polygon poly)
	{
		double[] x = new double[poly.npoints];
		double[] y = new double[poly.npoints];
		for (int i = 0;i < poly.npoints; i++)
		{
			x[i] = poly.xpoints[i];
			y[i] = poly.ypoints[i];
		}
		
		return computePolygonArea(x, y);
	}



	/**
	 * Compute the area of the specfied polygon.
	 * 
	 * @param xy  Geometry of polygon [x,y,...]
	 * @return    Area of specified polygon.
	 */
	public static double computePolygonArea (double[] xy)
	{
		int n = xy.length;

		double area = 0.0;
		for (int i = 0; i < n - 2; i += 2)
			area += (xy[i] * xy[i+3]) - (xy[i+2] * xy[i+1]);
		area += (xy[xy.length-2] * xy[1]) - (xy[0] * xy[xy.length-1]);    

		area *= 0.5;

		return area;
	}
}
