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

import calico.controllers.CStrokeController;
import calico.controllers.CCanvasController;
import calico.controllers.CGroupController;
import calico.input.CInputMode;
import calico.networking.netstuff.*;
import calico.utils.Geometry;

import java.awt.*;

import edu.umd.cs.piccolo.*;
import edu.umd.cs.piccolo.activities.PActivity;
import edu.umd.cs.piccolo.nodes.*;
import edu.umd.cs.piccolox.nodes.*;
import edu.umd.cs.piccolo.util.PAffineTransform;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PPaintContext;
import calico.*;

import java.awt.geom.*;
import java.util.Arrays;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import it.unimi.dsi.fastutil.objects.*;

public class CStroke extends PPath
{
	private static Logger logger = Logger.getLogger(CStroke.class.getName());
	
	private static final long serialVersionUID = 1L;
	
	private Polygon mousePoints = new Polygon();
	protected double scaleX = 1.0d, scaleY = 1.0d;
	protected double rotation = 0.0d;
	
	//See method applyAffineTransform() for explanation
//	private Polygon mousePointsOriginal;
	
	//See method applyAffineTransform() for explanation
//	ArrayList<AffineTransform> strokeTransforms;
	
	private long uuid = 0L;
	private long canvasUID = 0L;
	private long parentUID = 0L;
	
	private Color color = null;
	
	private boolean finished = false;
	private boolean drawTailTarget = false;
	
	//private ObjectArrayList<PLine> debugObjects = new ObjectArrayList<PLine>();
	public boolean isScrapPreview = false;
	
	Stroke stroke;
	Color strokePaint;
	float thickness;
	
	private boolean isTempInk = false;
	private boolean isHighlighted = false;

	private LinkedList<PNode> tempSegments = new LinkedList<PNode>();
	
	protected float transparency = CalicoOptions.stroke.transparency;
	
	public boolean hiding = false;
	
	public Point circlePoint = new Point(0,0);
	
	// This will hold the bubble menu buttons (Class<?>)
	private static ObjectArrayList<Class<?>> bubbleMenuButtons = new ObjectArrayList<Class<?>>(); 
	
	public CStroke(long u, long canvas, long puid)
	{
		this(u, canvas, puid, CalicoDataStore.PenColor, CalicoDataStore.PenThickness);
		
		
	}
	public CStroke(long uuid, long cuid, long puid, Color color, float thickness)
	{
		this.uuid = uuid;
		this.canvasUID = cuid;
		this.parentUID = puid;
		this.color = color;
		this.thickness = thickness;
		
//		stroke = new BasicStroke( thickness );
		stroke = new BasicStroke(thickness,
				BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		setStroke( stroke );
		strokePaint = this.color;
		setStrokePaint( strokePaint );
		resetBounds();
		
//		strokeTransforms = new ArrayList<AffineTransform>();

	//	setTransparency(1);
	}
	
	// This is just a quicky signature, it is by no means very accurate
	public int get_signature()
	{
		if (!finished || isTempInk())
			return 0;
		int sig = this.mousePoints.npoints + this.color.getRGB() + this.mousePoints.xpoints[0] + this.mousePoints.ypoints[0] + (int)(this.rotation*10) + (int)(this.scaleX*10) + (int)(this.scaleY*10);
//		System.out.println("Debug sig for stroke " + uuid + ": " + sig + ", 1) " + this.mousePoints.npoints + ", 2) " + this.color.getRGB() + ", 3) " + this.mousePoints.xpoints[0] + ", 4) " + this.mousePoints.xpoints[0] + ", 5) " + this.mousePoints.ypoints[0] + ", 6) " + (int)(this.rotation*10) + ", 7) " + (int)(this.scaleX*10) + ", 8) " + (int)(this.scaleY*10));
		return sig;
	}
	
	public String get_signature_debug_output()
	{
		return "Debug sig for stroke " + uuid + ": 1) " + this.mousePoints.npoints + ", 2) " + this.color.getRGB() + ", 3) " +0 + ", 4) " + this.mousePoints.xpoints[0] + ", 5) " + this.mousePoints.ypoints[0] + ", 6) " + (int)(this.rotation*10) + ", 7) " + (int)(this.scaleX*10) + ", 8) " + (int)(this.scaleY*10);
	}
	
	
	// This is far more accurate, and used to confirm that the client/server are in sync
	public byte[] getHashCode()
	{
		CalicoPacket pack = new CalicoPacket(16);
		pack.putInt(Arrays.hashCode(new long[]{this.uuid, this.canvasUID, this.parentUID}));
		pack.putInt(Arrays.hashCode(this.mousePoints.xpoints) );
		pack.putInt(Arrays.hashCode(this.mousePoints.ypoints) );
		pack.putInt(color.getRGB());
		pack.putFloat(thickness);
		return pack.getBuffer();
	}

	public long getUUID()
	{
		return this.uuid;
	}
	public long getParentUUID()
	{
		return this.parentUID;
	}
	public void setParentUUID(long p)
	{
		logger.trace("Changing parent for " + uuid + ": " + this.parentUID + " -> " + p);
		this.parentUID = p;
	}
	public long getCanvasUUID()
	{
		return this.canvasUID;
	}
	
	public GeneralPath getPathReference()
	{
		return super.getPathReference();
	}
	
	public boolean isChild()
	{
		return (parentUID!=0);
	}
	
	public void delete()
	{
		
		if (tempSegments.size() > 0)
		{
			final PLayer layer = CCanvasController.canvasdb.get(canvasUID).getLayer();
			for (final PNode path : tempSegments)
			{
				//layer.removeChild(path);
				/*SwingUtilities.invokeLater(
						new Runnable() { public void run() { layer.removeChild(path); } }
				);*/
				CalicoDraw.removeChildFromNode(layer, path);
			}
			tempSegments.clear();
		}
		
		// loose the debug marks
		eraseDebugMarks();
		
		
		// remove from canvas
		CCanvasController.no_notify_delete_child_stroke(this.canvasUID, this.uuid);
		
		if(getParentUUID()!=0L)
		{
			CGroupController.no_notify_delete_child_stroke(getParentUUID(), this.uuid);
		}
		
		if(CCanvasController.canvas_has_child_stroke_node(this.canvasUID, uuid))
		{
			//This line is not thread safe so must invokeLater to prevent eraser artifacts.
			/*SwingUtilities.invokeLater(
					new Runnable() { public void run() { removeFromParent(); } }
			);*/
			CalicoDraw.removeNodeFromParent(this);
			//removeFromParent();
		}
	}
	
	public boolean containsShape(Shape shape)
	{
		Polygon polygon = Geometry.getPolyFromPath(shape.getPathIterator(null));
		GeneralPath containerGroup = getPathReference();
		for(int i=0;i<polygon.npoints;i++)
		{
			if (!containerGroup.contains(new Point(polygon.xpoints[i], polygon.ypoints[i])))
			{
				return false;
			}
		}
		return true;
	}
	
	public void setColor(Color color)
	{
		this.color = color;
		setStrokePaint( color );
		//this.setPaintInvalid(true);
		CalicoDraw.setNodePaintInvalid(this, true);
	}
	
	public void setThickness(float t)
	{
		this.thickness = t;
		setStroke( new BasicStroke(t) );
		//this.setPaintInvalid(true);
		CalicoDraw.setNodePaintInvalid(this, true);
	}
	
	public void finish()
	{
		if(finished)
		{
			return;
		}
		finished = true;
		applyAffineTransform();
		
		final PLayer layer = CCanvasController.canvasdb.get(canvasUID).getLayer();
		for (final PNode path : tempSegments)
		{
			//path.setTransparency(0f);
			/*SwingUtilities.invokeLater(
					new Runnable() { public void run() { 
						path.setTransparency(0f);
						layer.removeChild(path); 
						} }
			);*/
			CalicoDraw.removeChildFromNode(layer, path);
			
		}
		tempSegments.clear();
		
		if (CCanvasController.getCurrentUUID() == getCanvasUUID())
			CalicoDraw.repaint(this);
		
//		this.repaintFrom(this.getBounds(), this);
		
//		setPolygon(new Polygon(mousePoints.xpoints, mousePoints.ypoints, mousePoints.npoints));
//		mousePointsOriginal = new Polygon(mousePoints.xpoints, mousePoints.ypoints, mousePoints.npoints);
		
		//redrawLast();
		
		//this.invalidatePaint();
		
		//CCanvasController.canvasdb.get(canvasUID).repaint();
		
		/*if(CalicoOptions.pen.strikethru.enabled)
		{
			if(hasStrikeoutHeight())
			{
				
				//int bh = getPolygon().getBounds().height;
				
				long[] bges = CCanvasController.canvasdb.get(canvasUID).getChildStrokes();
				for(int i=0;i<bges.length;i++)
				{
					if(bges[i]!=uuid)
					{
						//pen.strikethru.min_intersects
						int inter = numIntersectionsWith(CStrokeController.strokes.get(bges[i]));
						//Calico.log_debug("BGE ("+bh+") "+uuid+" INT "+bges[i]+" "+inter+" times.");
						
						if(inter>=CalicoOptions.pen.strikethru.min_intersects)
						{
							if(CalicoOptions.pen.strikethru.debug)
							{
								CStrokeController.strokes.get(bges[i]).setStrokePaint(Color.RED);
							}
							else
							{

								CStrokeController.delete(bges[i]);
								CStrokeController.delete(uuid);
								return;
							}
							//BGElementController.delete(uuid);
							//return;
						}
						
					}
				}
			}
		}*/
		
		//CalicoInputManager.addBGElementInputHandler(uuid);
		
		//addInputEventListener(new BGElementInputHandler(uuid));
	}
	
	public boolean hasStrikeoutHeight()
	{
		return (getPolygon().getBounds().height<=CalicoOptions.pen.strikethru.max_height);
	}
	
	
	public void redraw(Polygon poly)
	{	

		GeneralPath path = getBezieredPoly(poly);
		this.setPathTo(path);

	}


	public int getPointCount()
	{
		
		return mousePoints.npoints;
	}
	
	public double getLength()
	{
		Polygon p = getPolygon();
		double totalLen = 0f;
		if (p.npoints < 2)
			return 0;
		
		for (int i = 1; i < p.npoints; i++)
			totalLen += Point.distance(p.xpoints[i-1], p.ypoints[i-1], p.xpoints[i], p.ypoints[i]);
		
		
		return totalLen;
	}
	
	public void batch_append(int[] x, int[] y)
	{
		for(int i=0;i<x.length;i++)
		{
			mousePoints.addPoint(x[i], y[i]);
		}
		redraw(mousePoints);
	}
	
	public void append(int x, int y)
	{
		mousePoints.addPoint(x,y);
		if (mousePoints.npoints < 2)
			return;
		redraw(mousePoints);
		CalicoDraw.repaintNode(this);
		
//		PLine line = new PLine();
//		line.setStroke(stroke);
//		line.setStrokePaint(strokePaint);
//		line.addPoint(0, mousePoints.xpoints[mousePoints.npoints-2], mousePoints.ypoints[mousePoints.npoints-2]);
//		line.addPoint(1, x, y);
//		//CCanvasController.canvasdb.get(canvasUID).getLayer().addChild(line);
//		CalicoDraw.addChildToNode(CCanvasController.canvasdb.get(canvasUID).getLayer(), line);
//		this.tempSegments.add(line);
//		//line.repaintFrom(line.getBounds(), line);
//		CalicoDraw.repaintNode(line);
		
		if (!drawTailTarget && Geometry.getPolygonLength(mousePoints) >= CalicoOptions.stroke.min_create_scrap_length
				&& CalicoDataStore.Mode == CInputMode.EXPERT)
		{
			drawHitCircle();

		}
	}
	
	private void drawHitCircle() {
		double totalLen = 0f, previousLen = 0f;
		for (int i = 1; i < mousePoints.npoints; i++)
		{
			previousLen = totalLen;
			totalLen += Point.distance(mousePoints.xpoints[i-1], mousePoints.ypoints[i-1], mousePoints.xpoints[i], mousePoints.ypoints[i]);
			int offsetLength = 25;
			if (totalLen >= offsetLength)
			{
				double ratio = (offsetLength - previousLen) / ((offsetLength - previousLen) +(totalLen - offsetLength));
				circlePoint.x = (int) (((mousePoints.xpoints[i] - mousePoints.xpoints[i-1]) * ratio) + mousePoints.xpoints[i-1]);
				circlePoint.y = (int) (((mousePoints.ypoints[i] - mousePoints.ypoints[i-1]) * ratio) + mousePoints.ypoints[i-1]);
				break;
			}
		}
		
		
		double radius = CalicoOptions.stroke.max_head_to_heal_distance;
		Ellipse2D.Double hitTarget = new Ellipse2D.Double(circlePoint.x - radius, circlePoint.y - radius, radius*2, radius*2);
		//Ellipse2D.Double hitTarget = new Ellipse2D.Double(mousePoints.xpoints[0] - radius, mousePoints.ypoints[0] - radius, radius*2, radius*2);
		final PPath circle = new PPath(hitTarget);
		circle.setStrokePaint(Color.white);
		circle.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
		BasicStroke.JOIN_MITER, 10.0f, new float[] {10f}, 0.0f));
		//CCanvasController.canvasdb.get(canvasUID).getLayer().addChild(circle);
		CalicoDraw.addChildToNode(CCanvasController.canvasdb.get(canvasUID).getLayer(), circle);
		this.tempSegments.add(circle);
		//circle.invalidatePaint();
		CalicoDraw.invalidatePaint(circle);
		CCanvasController.canvasdb.get(canvasUID).getLayer().repaintFrom(circle.getBounds(), circle);
		drawTailTarget = true;
//			circle.repaintFrom(circle.getBounds(), circle);
		
		
		//fade in
		PActivity flash = new PActivity(500,10) {
			long step = 0;
      
		    protected void activityStep(long time) {
		            super.activityStep(time);
		            circle.setStrokePaint(new Color(0, 0, 0, 1.0f * step/50 * .7f));
		            if (step > 100)
		            	step++;
		            step++;
		    }
		};
		// Must schedule the activity with the root for it to run.
		//circle.getRoot().addActivity(flash);
		CalicoDraw.addActivityToNode(circle, flash);
	}
	
	
	public void drawDebugMark(int x, int y)
	{
		drawDebugMark(x,y,Color.RED);
	}
	public void drawDebugMark(int x, int y, Color color)
	{
		drawDebugMark(x,y,color,1);
	}
	public void drawDebugMark(int x, int y, Color color, int type)
	{
		PLine pline = new PLine();
		if(type==1)
		{
			pline.addPoint(0, x, y);
			pline.addPoint(1, x, y);
		}
		else if(type==2)
		{
			pline.addPoint(0, x, y-2);
			pline.addPoint(1, x, y+2);
		}
		else if(type==3)
		{
			pline.addPoint(0, x-2, y);
			pline.addPoint(1, x+2, y);
		}
		
//		pline.setStroke(new BasicStroke( CalicoOptions.pen.stroke_size ));
		pline.setStrokePaint(color);
		//debugObjects.add(pline);
		//CCanvasController.canvasdb.get(canvasUID).getLayer().addChild(pline);
		CalicoDraw.addChildToNode(CCanvasController.canvasdb.get(canvasUID).getLayer(), pline);
	}
	public void eraseDebugMarks()
	{
		/*if(debugObjects.size()>0)
		{
			PNode[] list = new PNode[debugObjects.size()];
			//debugObjects.toArray(a)
			debugObjects.toArray(list);
			for(int i=0;i<list.length;i++)
			{
				list[i].removeFromParent();
			}
			debugObjects.clear();
		}
		*/
	}
	
	public Polygon getPolygon() {
		if (finished)
			return Geometry.getPolyFromPath(getPathReference().getPathIterator(null));
		else
			return mousePoints;
	}
	
	public Polygon getRawPolygon() {
		return mousePoints;
	}
	
	public Color getColor()
	{
		return this.color;
	}
	
	public float getThickness()
	{
		return this.thickness;
	}
	
	public boolean isContainedInPath(GeneralPath path)
	{
		if (isTempInk)
			return false;
		
		Polygon p = Geometry.getPolyFromPath(getPathReference().getPathIterator(null));
		
		if (p.npoints == 0)
			return false;
		
		for(int i=0;i<p.npoints;i++)
		{
			Point point = new Point(p.xpoints[i], p.ypoints[i]);
			if(!path.contains(point))
			{
				return false;
			}
		}
		
		return true;
	}
	
	public void move(int x, int y)
	{
		mousePoints.translate(x, y);
		applyAffineTransform();
	}
	
	public boolean intersectsWith(CStroke bge)
	{
		return (numIntersectionsWith(bge)>0); 
	}
	public int numIntersectionsWith(CStroke bge)
	{
		return CStroke.countIntersections(getPolygon(), bge.getPolygon()); 
	}
	
	
	
	protected void paint(final PPaintContext paintContext)
	{
		final Graphics2D g2 = paintContext.getGraphics();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		if (isScrapPreview)
		{
			setStroke(new BasicStroke(CalicoOptions.group.stroke_size,
					BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, new float[] { 10.0f },
					0.0f));
			
			paintContext.pushTransparency(CalicoOptions.group.background_transparency);
			g2.setPaint(CalicoOptions.group.temp_background_color);
			g2.fill(this.getPathReference());

			g2.setPaint(CalicoOptions.group.stroke_color);
			g2.setStroke(this.getStroke());
			g2.draw(this.getPathReference());
			paintContext.popTransparency(CalicoOptions.group.background_transparency);
			
		}
		else
		{
			//This draws the highlight
			Composite temp = g2.getComposite();
			if (isHighlighted)
			{
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, CalicoOptions.stroke.background_transparency));
				g2.setStroke(new BasicStroke(CalicoOptions.pen.stroke_size + 8));
				g2.setPaint(Color.blue);
				g2.draw(getPathReference());
			}
			if (CGroupController.exists(getParentUUID()) && !CGroupController.groupdb.get(getParentUUID()).isPermanent() && !this.isTempInk())
			{
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, CGroupController.groupdb.get(getParentUUID()).getTransparency()));
				g2.setStroke(new BasicStroke(CalicoOptions.pen.stroke_size + 8));
				g2.setPaint(Color.blue);
				g2.draw(getPathReference());
			}
			if (CGroupController.exists(getParentUUID()) && CGroupController.groupdb.get(getParentUUID()).isPermanent())
			{
				float transparency = (CGroupController.groupdb.get(getParentUUID()).getTransparency() / CalicoOptions.group.background_transparency)
									* CalicoOptions.stroke.transparency;
				if (transparency > 1)
					transparency = 1.0f;
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, transparency));
			
			}
			g2.setPaint(strokePaint);
			g2.setStroke(stroke);

			g2.draw(getPathReference());
			g2.setComposite(temp);
			

		}
		
//		long currTime = (new Date()).getTime();
//		System.out.println("CStroke (" + this.uuid + ") repaint @ time: " + currTime);
		
    }
	
	
	public void highlight_on() {
		isHighlighted = true;
		highlight_repaint();
	}
	
	public void highlight_off() {
		isHighlighted = false;
		highlight_repaint();
	}

	public void highlight_repaint()
	{
		Rectangle bounds = getBounds().getBounds();
		double buffer = 30;
		PBounds bufferBounds = new PBounds(bounds.getX() - buffer, bounds.getY() - buffer, bounds.getWidth() + buffer * 2, bounds.getHeight() + buffer * 2);
		//CCanvasController.canvasdb.get(cuid).getLayer().repaintFrom(bufferBounds, this);
		CalicoDraw.repaintNode(CCanvasController.canvasdb.get(canvasUID).getLayer(), bufferBounds, this);
	}
	
	public static int countIntersections(Polygon path, Polygon testPath)
	{
		int intersects = 0;

		if( !path.getBounds().intersects(testPath.getBounds()) )
		{
			return 0;
		}
		
		int px1 = path.xpoints[0];
		int py1 = path.ypoints[0];
		
		for(int i=1;i<path.npoints;i++)
		{
			int cx1 = path.xpoints[i];
			int cy1 = path.ypoints[i];
			
			intersects = intersects + countLineIntersections(px1,py1, cx1, cy1, testPath);
			
			px1 = cx1;
			py1 = cy1;
			
		}
		
		return intersects;
	}
	
	public static int countLineIntersections(int x1, int y1, int x2, int y2, Polygon testPath)
	{
		int intersects = 0;
	
		
		int px1 = testPath.xpoints[0];
		int py1 = testPath.ypoints[0];
		
		for(int i=1;i<testPath.npoints;i++)
		{
			int cx1 = testPath.xpoints[i];
			int cy1 = testPath.ypoints[i];
						
			if(Line2D.linesIntersect(px1, py1, cx1, cy1, x1, y1,x2, y2))
			{
				intersects++;
			}
			
			px1 = cx1;
			py1 = cy1;
			
		}
		
		
		return intersects;
	}
	
//	protected void addTransform(AffineTransform at)
//	{
//		smoothedPath.transform(at);
////		strokeTransforms.add(at);
//	}
	
	public void rotate(double radians, Point2D pivotPoint)
	{
		AffineTransform rotateAboutPivot = AffineTransform.getRotateInstance(radians, pivotPoint.getX(), pivotPoint.getY());
		
		//1) compute mid point translation
		Point2D oldMidPoint = Geometry.getMidPoint2D(mousePoints);
		Point2D newMidPoint = null;
		newMidPoint = rotateAboutPivot.transform(oldMidPoint, newMidPoint);
		int deltaX = new java.lang.Double(newMidPoint.getX() - oldMidPoint.getX()).intValue();
		int deltaY = new java.lang.Double(newMidPoint.getY() - oldMidPoint.getY()).intValue();
		mousePoints.translate(deltaX, deltaY);
		
		//2) compute actual rotation change
		Rectangle2D bounds = mousePoints.getBounds2D();
		Point2D oldBoundsRightCorner = new Point2D.Double(bounds.getX() + bounds.getWidth(), bounds.getY());
		Point2D newBoundsRightCorner = null;
		newBoundsRightCorner = rotateAboutPivot.transform(oldBoundsRightCorner, newBoundsRightCorner);
		
		double angleOld = calico.utils.Geometry.angle(oldMidPoint, oldBoundsRightCorner);
		double angleNew = calico.utils.Geometry.angle(newMidPoint, newBoundsRightCorner);
		double actualRotation = angleNew - angleOld;
		primative_rotate(actualRotation + rotation);
	}
	
	public void primative_rotate(double actualRotation) {
		rotation = actualRotation;
		applyAffineTransform();
	}
	
	public void scale(double scaleX, double scaleY, Point2D pivotPoint) {
		
		AffineTransform scaleAboutPivot1 = AffineTransform.getTranslateInstance(-1 * pivotPoint.getX(), -1 * pivotPoint.getY());
		AffineTransform scaleAboutPivot2 = AffineTransform.getScaleInstance(scaleX, scaleY);
		AffineTransform scaleAboutPivot3 = AffineTransform.getTranslateInstance(pivotPoint.getX(), pivotPoint.getY());
		
		//1) compute mid point translation
		Point2D oldMidPoint = Geometry.getMidPoint2D(mousePoints);
		Point2D newMidPoint = null;
		newMidPoint = scaleAboutPivot1.transform(oldMidPoint, newMidPoint);
		newMidPoint = scaleAboutPivot2.transform(newMidPoint, null);
		newMidPoint = scaleAboutPivot3.transform(newMidPoint, null);
		int deltaX = new java.lang.Double(newMidPoint.getX() - oldMidPoint.getX()).intValue();
		int deltaY = new java.lang.Double(newMidPoint.getY() - oldMidPoint.getY()).intValue();
		mousePoints.translate(deltaX, deltaY);
		
		//2) assign actual scale
		primative_scale(this.scaleX * scaleX, this.scaleY * scaleY);
	}
	public void primative_scale(double scaleX, double scaleY) {
		this.scaleX = scaleX;
		this.scaleY = scaleY;
		applyAffineTransform();
	}
	
	/**
	 * See CGroup.applyAffineTransform for explanation
	 * 
	 * @param maintainCenter
	 */
	protected void applyAffineTransform()
	{
		Rectangle oldBounds = getBounds().getBounds();
		
		PAffineTransform piccoloTextTransform = getPTransform();
		GeneralPath p = (GeneralPath) getBezieredPoly(mousePoints).createTransformedShape(piccoloTextTransform);
		this.setPathTo(p);
//		if (p.getBounds().width == 0 || p.getBounds().height == 0)
//		{
//			this.setBounds(new java.awt.geom.Rectangle2D.Double(p.getBounds2D().getX(), p.getBounds2D().getY(), 1d, 1d));
//		}
//		else
//		{
//			this.setBounds(p.getBounds());
//		}
//		this.repaintFrom(this.getBounds(), this);
//		invalidatePaint();
		
//		CCanvasController.canvasdb.get(canvasUID).getCamera().validateFullPaint();

		//CCanvasController.canvasdb.get(canvasUID).getCamera().repaintFrom(new PBounds(Geometry.getCombinedBounds(new Rectangle[] {oldBounds, this.getBounds().getBounds()})), this);
		if (CCanvasController.getCurrentUUID() == getCanvasUUID())
			CalicoDraw.repaintNode(CCanvasController.canvasdb.get(canvasUID).getCamera(), new PBounds(Geometry.getCombinedBounds(new Rectangle[] {oldBounds, this.getBounds().getBounds()})), this);
	}
	
	public PAffineTransform getPTransform() {
		PAffineTransform piccoloTextTransform = new PAffineTransform();
		Point2D midPoint = Geometry.getMidPoint2D(mousePoints);
		piccoloTextTransform.rotate(rotation, midPoint.getX(), midPoint.getY());
		piccoloTextTransform.scaleAboutPoint(scaleX, midPoint.getX(), midPoint.getY());
		return piccoloTextTransform;
	}
	
	public GeneralPath getBezieredPoly(Polygon pts)
	{
		GeneralPath p = new GeneralPath();
		if (pts.npoints > 0)
		{
			p.moveTo(pts.xpoints[0], pts.ypoints[0]);
			if (pts.npoints >= 4)
			{
				int counter = 1;
				for (int i = 1; i+2 < pts.npoints; i += 3)
				{
					p.curveTo(pts.xpoints[i], pts.ypoints[i], 
							pts.xpoints[i+1], pts.ypoints[i+1], 
							pts.xpoints[i+2], pts.ypoints[i+2]);
					counter += 3;
				}
				while (counter < pts.npoints)
				{
					p.lineTo(pts.xpoints[counter], pts.ypoints[counter]);
					counter++;
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
	public long createTemporaryScrapPreview(final boolean delete) {
		
		long tempUUID = Calico.uuid();
		CGroupController.no_notify_start(tempUUID, this.canvasUID, 0l, false);

		int[] x = new int[mousePoints.npoints];
		int[] y = new int[mousePoints.npoints];
		for (int i = 0; i < mousePoints.npoints; i++)
		{
			x[i] = mousePoints.xpoints[i];
			y[i] = mousePoints.ypoints[i];
		}
		
		CGroupController.no_notify_append(tempUUID, x, y);
		CGroupController.no_notify_finish(tempUUID, true, false, true);
		
//		System.out.println("Creating temporary group: " + tempUUID);
		
		CGroupController.loadGroup(tempUUID, true);
		
		CGroupController.setCurrentUUID(tempUUID);
		CGroupController.setLastCreatedGroupUUID(tempUUID);

		//CStrokeController.hideStroke(this.uuid, delete);
		
		
		CGroupController.restoreOriginalStroke = true;
		CGroupController.originalStroke = getUpdatePackets()[0];
		
		CStrokeController.delete(this.uuid);
		
		return tempUUID;
	}
	
	public CalicoPacket[] getUpdatePackets(long uuid, long cuid, long puid, int dx, int dy)
	{			
		int packetSize = ByteUtils.SIZE_OF_INT + (3 * ByteUtils.SIZE_OF_LONG) + ByteUtils.SIZE_OF_INT + ByteUtils.SIZE_OF_SHORT + (2 * this.mousePoints.npoints * ByteUtils.SIZE_OF_SHORT);
		
		CalicoPacket packet = new CalicoPacket(packetSize);
		//UUID CUID PUID <COLOR> <NUMCOORDS> x1 y1
		packet.putInt(NetworkCommand.STROKE_LOAD);
		packet.putLong(uuid);
		packet.putLong(cuid);
		packet.putLong(puid);
		packet.putColor(new Color(this.getColor().getRed(), this.getColor().getGreen(), this.getColor().getBlue()));
		packet.putFloat(this.thickness);
		packet.putCharInt(this.mousePoints.npoints);
		for(int j=0;j<this.mousePoints.npoints;j++)
		{
			packet.putInt(this.mousePoints.xpoints[j] + dx);
			packet.putInt(this.mousePoints.ypoints[j] + dy);
		}
		packet.putDouble(this.rotation);
		packet.putDouble(this.scaleX);
		packet.putDouble(this.scaleY);
		
		return new CalicoPacket[]{packet};

	}
	
	public CalicoPacket[] getUpdatePackets()
	{
		return getUpdatePackets(this.uuid, this.canvasUID, this.parentUID, 0, 0);
	}
	
//	public CalicoPacket[] getUpdatePackets()
//	{
//		if(!finished)
//		{
//			return null;
//		}
//		
//		if(CalicoOptions.network.cluster_size>=this.mousePoints.npoints)
//		{
//			// WE CAN SEND A STROKE_LOAD SINGLE PACKET
//			
//			int packetSize = ByteUtils.SIZE_OF_INT + (3 * ByteUtils.SIZE_OF_LONG) + ByteUtils.SIZE_OF_INT + ByteUtils.SIZE_OF_SHORT + (2 * this.mousePoints.npoints * ByteUtils.SIZE_OF_SHORT);
//			
//			CalicoPacket packet = new CalicoPacket(packetSize);
//			//UUID CUID PUID <COLOR> <NUMCOORDS> x1 y1
//			packet.putInt(NetworkCommand.STROKE_LOAD);
//			packet.putLong(this.uuid);
//			packet.putLong(this.canvasUID);
//			packet.putLong(this.parentUID);
//			packet.putColor(new Color(this.getColor().getRed(), this.getColor().getGreen(), this.getColor().getBlue()));
//			packet.putFloat(this.thickness);
//			packet.putCharInt(this.mousePoints.npoints);
//			for(int j=0;j<this.mousePoints.npoints;j++)
//			{
//				packet.putInt(this.mousePoints.xpoints[j]);
//				packet.putInt(this.mousePoints.ypoints[j]);
//			}
//			packet.putDouble(this.rotation);
//			packet.putDouble(this.scaleX);
//			packet.putDouble(this.scaleY);
//			
//			return new CalicoPacket[]{packet};
//		}
//		else
//		{
//		
//			// how many packets are we going to need?
//			int numPackets = 4;// START + END
//			for(int i=0;i<this.mousePoints.npoints;i=i+CalicoOptions.network.cluster_size)
//			{
//				numPackets++;// APPENDs
//			}
//			
//			CalicoPacket[] packets = new CalicoPacket[numPackets];
//			
//			packets[0] = CalicoPacket.getPacket(NetworkCommand.STROKE_START, 
//				this.uuid, 
//				this.canvasUID, 
//				this.parentUID, 
//				this.getColor().getRed(), this.getColor().getGreen(), this.getColor().getBlue(),
//				this.getThickness()
//			);
//			
//			int packetIndex = 1;
//			
//			for(int i=0;i<this.mousePoints.npoints;i=i+CalicoOptions.network.cluster_size)
//			{
//				// determine how many coordinates will be in the next cluster
//				int numingroup = (i+CalicoOptions.network.cluster_size) > this.mousePoints.npoints ? (this.mousePoints.npoints-i) : CalicoOptions.network.cluster_size;
//				
//				packets[packetIndex] = new CalicoPacket(2 * numingroup * ByteUtils.SIZE_OF_SHORT);
//				packets[packetIndex].putInt(NetworkCommand.STROKE_APPEND);
//				packets[packetIndex].putLong(this.uuid);
//				packets[packetIndex].putCharInt(numingroup);
//				
//				for(int j=0;j<numingroup;j++)
//				{
//					packets[packetIndex].putInt(this.mousePoints.xpoints[i+j]);
//					packets[packetIndex].putInt(this.mousePoints.ypoints[i+j]);
//				}
//				
//				packetIndex++;
//			}
//			packets[packetIndex++] = CalicoPacket.getPacket(NetworkCommand.STROKE_FINISH, this.uuid);
//			packets[packetIndex++] = CalicoPacket.getPacket(
//					NetworkCommand.STROKE_ROTATE, this.uuid, this.rotation);
//			packets[packetIndex] = CalicoPacket.getPacket(
//					NetworkCommand.STROKE_SCALE, this.uuid, this.scaleX, this.scaleY);
//			
//			return packets;
//		}
//	}
	
	public void calculateParent()
	{
		
		double smallestArea = java.lang.Double.MAX_VALUE;
		long smallestGroupUUID = 0L;
		
		long[] groupList = CCanvasController.canvasdb.get(this.canvasUID).getChildGroups();
		
		if(groupList.length>0)
		{
			for(int i=0;i<groupList.length;i++)
			{
				if (!CGroupController.exists(groupList[i]))
					continue;
				
				if(smallestArea > CGroupController.groupdb.get(groupList[i]).getArea() 
					&& CGroupController.groupdb.get(groupList[i]).isPermanent()
					//&& isContainedInPath(CGroupController.groupdb.get(groupList[i]).getPathReference()
					&& CGroupController.groupdb.get(groupList[i]).canParentChild(this.uuid, (int)Geometry.getMidPoint2D(mousePoints).getX(), (int)Geometry.getMidPoint2D(mousePoints).getY())
					)
				{
					smallestArea = CGroupController.groupdb.get(groupList[i]).getArea();
					smallestGroupUUID = groupList[i];
				}//
			}
		}
		
		// Only run this if we actually found a parent
		if(smallestGroupUUID!=0L)
		{
			// We have a parent!
			CStrokeController.no_notify_set_parent(this.uuid, smallestGroupUUID);
			setParentUUID(smallestGroupUUID);
		}

	}
	public boolean intersects(double x, double y, double x2, double y2) {
		Polygon p = Geometry.getPolyFromPath(getPathReference().getPathIterator(null));
		boolean intersectionFound = false;
		
		for (int i = 1; i < p.npoints; i++)
		{
			if (Line2D.linesIntersect(p.xpoints[i-1], p.ypoints[i-1], p.xpoints[i], p.ypoints[i], 
									x, y, x2, y2) )
				intersectionFound = true;
		}
		
		return intersectionFound;
		
	}
	
	@Override
	public PBounds getBounds()
	{
		if (finished)
			return super.getBounds();
		else
			return new PBounds(mousePoints.getBounds2D());
	}

	public void setIsTempInk(boolean b)
	{
		isTempInk = b;
	}
	
	public boolean isTempInk()
	{
		return isTempInk;
	}
	
	public void setTransparency(float f)
	{
		if (f > 1.0f)
			f = 1.0f;
		else if (f < 0.0f)
			f = 0.0f;
		
		if (getTransparency() == f)
			return;
		
		super.setTransparency(f);
	}
	
	
	public ObjectArrayList<Class<?>> getBubbleMenuButtons()
	{
		ObjectArrayList<Class<?>> bubbleMenuButtons = new ObjectArrayList<Class<?>>();
		bubbleMenuButtons.addAll(internal_getBubbleMenuButtons());
		//bubbleMenuButtons.addAll(CStroke.bubbleMenuButtons); //5
		return bubbleMenuButtons;
	}
	
	protected ObjectArrayList<Class<?>> internal_getBubbleMenuButtons()
	{
		ObjectArrayList<Class<?>> bubbleMenuButtons = new ObjectArrayList<Class<?>>(); 
		bubbleMenuButtons.add(calico.components.bubblemenu.strokes.StrokeMakeConnectorButton.class); //12
		return bubbleMenuButtons;
	}
	
	

}
