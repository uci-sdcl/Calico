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
import calico.components.arrow.AnchorPoint;
import calico.controllers.CCanvasController;
import calico.controllers.CGroupController;
import calico.inputhandlers.CalicoInputManager;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;
import edu.umd.cs.piccolo.*;
import edu.umd.cs.piccolo.nodes.*;
import edu.umd.cs.piccolo.util.PPaintContext;
import edu.umd.cs.piccolox.*;
import edu.umd.cs.piccolox.nodes.*;

import java.awt.*;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

public class CArrow extends PComposite
{
	private static Logger logger = Logger.getLogger(CArrow.class.getName());
	
	
	
	
	public static final int TYPE_NORM_HEAD_A	= 1;
	public static final int TYPE_NORM_HEAD_B	= 2;
	public static final int TYPE_NORM_HEAD_AB	= 3;

	public static final int TYPE_CANVAS	= 4;
	public static final int TYPE_GROUP	= 5;

	public static final int REGION_TAIL		= 1 << 0;
	public static final int REGION_MIDDLE	= 1 << 1;
	public static final int REGION_HEAD		= 1 << 2;
	
	public static final int ANCHOR_A = 1 << 0;
	public static final int ANCHOR_B = 1 << 1;
	
	private static final long serialVersionUID = 1L;
	
	private long uuid = 0L;
	private long canvasUID = 0L;

	
	private AnchorPoint anchorA = null;
	private AnchorPoint anchorB = null;
	
	//private PPath arrowHeadA = null;
	//private PPath arrowHeadB = null;
	//private PLine arrowLine = null;

	private int arrowType = TYPE_NORM_HEAD_B;
	

	// This is used as a larger polygon, used to check if the mouse is touching the arrow.
	@Deprecated
	private Polygon arrowContainment = new Polygon();
	
	private Polygon pointPath = new Polygon();
	
	private Color color = Color.BLACK;
	

	private Polygon poly_anchorA = new Polygon();
	private Polygon poly_anchorB = new Polygon();
	private Polygon poly_line = new Polygon();
	
	
	
	public CArrow(long uid, long cuid)
	{
		this(uid, cuid, CalicoDataStore.PenColor, CArrow.TYPE_NORM_HEAD_B);
	}
	
	public CArrow(long newUUID, long cuid, int type)
	{
		this(newUUID, cuid, CalicoDataStore.PenColor, type);
	}
	public CArrow(long uuid, long cuid, Color color)
	{
		this(uuid, cuid, CalicoDataStore.PenColor, TYPE_NORM_HEAD_B);
	}
	
	
	public CArrow(long uuid, long cuid, Color color, int type)
	{
		this.uuid = uuid;
		this.canvasUID = cuid;
		this.color = color;
		this.arrowType = type;

	}
	
	
	public CArrow(long uuid, long cuid, Color color, int type, AnchorPoint anchorA, AnchorPoint anchorB)
	{
		this(uuid,cuid,color,type);
		
		setAnchorA(anchorA);
		setAnchorB(anchorB);
		
	}
	
	
	public long getUUID()
	{
		return this.uuid;
	}
	public long getCanvasUUID()
	{
		return this.canvasUID;
	}
	
	public AnchorPoint getAnchorA()
	{
		return this.anchorA;
	}
	public AnchorPoint getAnchorB()
	{
		return this.anchorB;
	}
	public Color getColor()
	{
		return this.color;
	}
	
	
	
	

	public int getArrowType()
	{
		return arrowType;
	}
	
	public void setArrowType(int type)
	{
		arrowType = type;
	}
	
	public void setColor(Color color)
	{
		this.color = color;
	}

	public void setAnchor(int type, AnchorPoint anchor)
	{
		if(type==CArrow.ANCHOR_A)
		{
			setAnchorA(anchor);	
		}
		else if(type==CArrow.ANCHOR_B)
		{
			setAnchorB(anchor);
		}
	}
	
	
	public void setAnchorA(AnchorPoint anchor)
	{
		if (this.anchorA != null)
		{
			if (this.anchorA.getType() == CArrow.TYPE_GROUP)
			{
				CGroupController.groupdb.get(this.anchorA.getUUID()).deleteChildArrow(this.uuid);
			}
		}
		this.anchorA = anchor;
	}
	public void setAnchorB(AnchorPoint anchor)
	{
		if (this.anchorB != null)
		{
			if (this.anchorB.getType() == CArrow.TYPE_GROUP)
			{
				CGroupController.groupdb.get(this.anchorB.getUUID()).deleteChildArrow(this.uuid);
			}
		}
		this.anchorB = anchor;
	}

	
	/**
	 * @deprecated
	 * @see #setAnchorA(AnchorPoint)
	 * @param atype
	 * @param uid
	 * @param point
	 */
	public void setAnchorA(int atype, long uid, Point point)
	{
		setAnchorA(new AnchorPoint(atype, point, uid));
		/*anchorAType = atype;
		anchorAUUID = uid;
		anchorAPoint = point;
		if(atype==CArrow.TYPE_GROUP)
		{
			CGroupController.groupdb.get(uid).addArrow(uuid);
		}*/
	}
	
	/**
	 * @deprecated
	 * @see #setAnchorB(AnchorPoint)
	 * @param atype
	 * @param uid
	 * @param point
	 */
	public void setAnchorB(int atype, long uid, Point point)
	{
		setAnchorB(new AnchorPoint(atype, point, uid));
		/*anchorBType = atype;
		anchorBUUID = uid;
		anchorBPoint = point;
		if(atype==CArrow.TYPE_GROUP)
		{
			CGroupController.groupdb.get(uid).addArrow(uuid);
		}*/
	}
	
	public Polygon getPolygon()
	{
		return pointPath;
	}
	
	@Deprecated
	public boolean containsMousePoint(Point point)
	{
		return arrowContainment.contains(point);
	}
	
	public double getPointPercentage(Point point)
	{
		double dist = anchorA.getPoint().distance(anchorB.getPoint());
		double pointDist = anchorA.getPoint().distance(point);
		
		return (100.0 * (pointDist/dist));
	}
	
	
	public static int getPercentRegion(double percent)
	{
		if( percent<20.0)
		{
			return CArrow.REGION_TAIL;
		}
		else if(percent>80.0)
		{
			return CArrow.REGION_HEAD;
		}
		return CArrow.REGION_MIDDLE;
	}
	
	public Polygon getLinePolygon()
	{
		Polygon temp = new Polygon();
		temp.addPoint(anchorA.getPoint().x, anchorA.getPoint().y);
		temp.addPoint(anchorB.getPoint().x, anchorB.getPoint().y);
		temp.addPoint(anchorA.getPoint().x, anchorA.getPoint().y);
		return temp;
	}
	
	@Deprecated
	public void moveGroup(long uid, int x, int y)
	{
		if( (anchorA.getType()==CArrow.TYPE_GROUP && anchorA.getUUID()==uid) && (anchorB.getType()==CArrow.TYPE_GROUP && anchorB.getUUID()==uid))
		{
			// Both anchors are on the same group, so we just translate!
			anchorA.translate(x, y);
			anchorB.translate(x, y);
		}
		else if(anchorA.getType()==CArrow.TYPE_GROUP && anchorA.getUUID()==uid)
		{
			anchorA.translate(x, y);	
		}
		else if(anchorB.getType()==CArrow.TYPE_GROUP && anchorB.getUUID()==uid)
		{
			anchorB.translate(x, y);
		}
		redraw();
	}
	
	public AnchorPoint getAnchor(long uid)
	{
		if(anchorA.getType()==CArrow.TYPE_GROUP && anchorA.getUUID()==uid)
		{
			return anchorA;	
		}
		else if(anchorB.getType()==CArrow.TYPE_GROUP && anchorB.getUUID()==uid)
		{
			return anchorB;
		}
		return null;
	}
	
	
	public void delete()
	{
		if(anchorA.getType()==CArrow.TYPE_GROUP)
		{
			CGroupController.groupdb.get(anchorA.getUUID()).deleteChildArrow(uuid);
		}
		
		if(anchorB.getType()==CArrow.TYPE_GROUP)
		{
			CGroupController.groupdb.get(anchorB.getUUID()).deleteChildArrow(uuid);
		}

		CCanvasController.canvasdb.get(canvasUID).removeChildArrow(uuid);
		
		//This line is not thread safe so must invokeLater to prevent eraser artifacts.
		/*SwingUtilities.invokeLater(
				new Runnable() { public void run() { removeFromParent(); } }
		);*/
		CalicoDraw.removeNodeFromParent(this);
		//removeFromParent();
	}
	
	@Deprecated
	public void moveAnchor(int anchor, int x, int y)
	{
		if(anchor==CArrow.ANCHOR_A)
		{
			anchorA.translate(x, y);	
		}
		else
		{
			anchorB.translate(x, y);
		}
		redraw();
	}
	
	
	
	@Deprecated
	public void redraw()
	{
		redraw(true);
	}
	@Deprecated
	public void redraw(boolean repaint)
	{
		//this.removeAllChildren();
		CalicoDraw.removeAllChildrenFromNode(this);
		
		final PPath arrowHeadA = new PPath();
		final PPath arrowHeadB = new PPath();
		final PLine arrowLine = new PLine();
		final Color arrowColor = this.color;
		
		if(arrowType==CArrow.TYPE_NORM_HEAD_AB || arrowType==CArrow.TYPE_NORM_HEAD_A)
		{
			final int[] apoints = Geometry.createArrow(
					anchorB.getPoint().x, anchorB.getPoint().y, 
					anchorA.getPoint().x, anchorA.getPoint().y,
					CalicoOptions.arrow.length, CalicoOptions.arrow.angle, CalicoOptions.arrow.inset);
			
			
			//arrowHeadA = new PPath();
			SwingUtilities.invokeLater(
					new Runnable() { public void run() { 
						arrowHeadA.moveTo((float)apoints[0], (float)apoints[1]);
						for(int i=2;i<apoints.length;i=i+2)
						{
							arrowHeadA.lineTo((float)apoints[i], (float)apoints[i+1]);
						}
						arrowHeadA.setStroke(new BasicStroke(CalicoOptions.arrow.stroke_size));
						arrowHeadA.setStrokePaint(arrowColor);
						arrowHeadA.setPaint(arrowColor);
					}});
			
			//this.addChild(0,arrowHeadA);
			CalicoDraw.addChildToNode(this, arrowHeadA, 0);
		}
		if(arrowType==CArrow.TYPE_NORM_HEAD_AB || arrowType==CArrow.TYPE_NORM_HEAD_B)
		{
			final int[] bpoints = Geometry.createArrow(
					anchorA.getPoint().x, anchorA.getPoint().y, 
					anchorB.getPoint().x, anchorB.getPoint().y,
					CalicoOptions.arrow.length, CalicoOptions.arrow.angle, CalicoOptions.arrow.inset);
			
			//arrowHeadB = new PPath();
			SwingUtilities.invokeLater(
					new Runnable() { public void run() { 
						arrowHeadB.moveTo((float)bpoints[0], (float)bpoints[1]);
						for(int i=2;i<bpoints.length;i=i+2)
						{
							arrowHeadB.lineTo((float)bpoints[i], (float)bpoints[i+1]);
						}
						arrowHeadB.setStroke(new BasicStroke(CalicoOptions.arrow.stroke_size));
						arrowHeadB.setStrokePaint(arrowColor);
						arrowHeadB.setPaint(arrowColor);
					}});
			//this.addChild(0,arrowHeadB);
			CalicoDraw.addChildToNode(this, arrowHeadB, 0);
		}
	
		//arrowLine = new PLine();
		SwingUtilities.invokeLater(
				new Runnable() { public void run() { 
					arrowLine.addPoint(0, anchorA.getPoint().x, anchorA.getPoint().y);
					arrowLine.addPoint(1, anchorB.getPoint().x, anchorB.getPoint().y);
					arrowLine.setStroke(new BasicStroke(CalicoOptions.arrow.stroke_size));
					arrowLine.setStrokePaint(arrowColor);
					arrowLine.setPaint(arrowColor);
				}});
		
		//this.addChild(0,arrowLine);
		//CalicoDraw.addChildToNode(this, arrowLine, 0);
		
		//this.repaint();
		//CalicoDraw.repaint(this);
		
		if(repaint)
		{
			//CalicoDraw.repaintNode(this);
			//this.setPaintInvalid(true);
			//CalicoDraw.setNodePaintInvalid(this, true);
			//CCanvasController.canvasdb.get(canvasUID).repaint();
		}
		//createPointPath();
	}
	
	
	
	public static AnchorPoint getAnchorPoint(int type, Point point, long uuid)
	{
		return new AnchorPoint(type,point,uuid);
	}
	
	
	protected void paint(final PPaintContext paintContext)
	{
		//System.out.println("CGROUP PAINT()");
		//logger.trace("CArrow.paint() ["+this.uuid+"] - "+this.getPaintInvalid());
		super.paint(paintContext);
		
		/*final Graphics2D g2 = paintContext.getGraphics();
		
		
		
		//g2.setPaint(this.getStrokePaint());
		g2.setPaint(this.color);
		g2.setStroke(this.getStroke());
		g2.draw(this.getPathReference());
		*/
    }

	public void calculateParent() {
		if (anchorA.getType() == CArrow.TYPE_CANVAS)
		{
			long smallestUUID = CGroupController.get_smallest_containing_group_for_point(this.canvasUID, anchorA.getPoint());
			if (smallestUUID != 0l)
			{
				this.setAnchorA(new AnchorPoint(CArrow.TYPE_GROUP, anchorA.getPoint(), smallestUUID));
				CGroupController.groupdb.get(smallestUUID).addChildArrow(this.uuid);
			}
		}
		if (anchorB.getType() == CArrow.TYPE_CANVAS)
		{
			long smallestUUID = CGroupController.get_smallest_containing_group_for_point(this.canvasUID, anchorB.getPoint());
			if (smallestUUID != 0l)
			{
				this.setAnchorB(new AnchorPoint(CArrow.TYPE_GROUP, anchorB.getPoint(), smallestUUID));
				CGroupController.groupdb.get(smallestUUID).addChildArrow(this.uuid);
			}
		}
	}

	public int get_signature() {
		
		// TODO Auto-generated method stub
		return anchorA.getPoint().x + anchorA.getPoint().y + anchorB.getPoint().x + anchorB.getPoint().y;
	} 
	
	
	
	
	/*

	public void createPointPath()
	{
		redraw();
		
		int[] crosslinebase = CArrow.getArrowHeadLine(anchorA.getPoint().x, anchorA.getPoint().y, anchorB.getPoint().x, anchorB.getPoint().y, CalicoOptions.arrow.headsize);
		int[] headbase = CArrow.getArrowHeadLine(anchorA.getPoint().x, anchorA.getPoint().y, anchorB.getPoint().x, anchorB.getPoint().y, CalicoOptions.arrow.headsize - CalicoOptions.arrow.difference);
		int[] crossline = CArrow.getArrowHeadCrossLine(crosslinebase[0], crosslinebase[1], anchorB.getPoint().x, anchorB.getPoint().y, CalicoOptions.arrow.factor);//this is the bottom of the arrow spur

		
		
		
		
		pointPath = new Polygon();
		pointPath.addPoint(headbase[0], headbase[1]);
		pointPath.addPoint(crossline[0], crossline[1]); // HEIGHT
		pointPath.addPoint(anchorB.getPoint().x, anchorB.getPoint().y);
		pointPath.addPoint(crossline[2], crossline[3]); // HEIGHT
		pointPath.addPoint(headbase[0], headbase[1]);
		pointPath.addPoint(anchorA.getPoint().x, anchorA.getPoint().y);
		
		arrowContainment = makeArrowContainmentPolygon();
		
		
		
		float[] xpoints = new float[pointPath.npoints];
		float[] ypoints = new float[pointPath.npoints];
			
		for(int i=0;i<pointPath.npoints;i++)
		{
			xpoints[i] = (float) pointPath.xpoints[i];
			ypoints[i] = (float) pointPath.ypoints[i];
		}
			
		//setPathToPolyline(xpoints, ypoints);
		CCanvasController.canvasdb.get(canvasUID).repaint(pointPath.getBounds());
		
	}

	private Polygon makeArrowContainmentPolygon()
	{
		Polygon temp = new Polygon();
		
		int[] crosslinebase = CArrow.getArrowHeadLine(anchorA.getPoint().x, anchorA.getPoint().y, anchorB.getPoint().x, anchorB.getPoint().y, CalicoOptions.arrow.headsize);
		int[] crossline = CArrow.getArrowHeadCrossLine(crosslinebase[0], crosslinebase[1], anchorB.getPoint().x, anchorB.getPoint().y, CalicoOptions.arrow.factor);//this is the bottom of the arrow spur

		int[] crosslinebase2 = CArrow.getArrowHeadLine(anchorB.getPoint().x, anchorB.getPoint().y, anchorA.getPoint().x, anchorA.getPoint().y, CalicoOptions.arrow.headsize);
		int[] crossline2 = CArrow.getArrowHeadCrossLine(crosslinebase2[0], crosslinebase2[1], anchorA.getPoint().x, anchorA.getPoint().y, 1.0);//CArrow.FACTOR);//this is the bottom of the arrow spur
			
		temp.addPoint(crossline[0], crossline[1]); // HEIGHT
		temp.addPoint(anchorB.getPoint().x, anchorB.getPoint().y);
		temp.addPoint(crossline[2], crossline[3]); // HEIGHT
		
		temp.addPoint(crossline2[0], crossline2[1]); // HEIGHT
		temp.addPoint(anchorA.getPoint().x, anchorA.getPoint().y);
		temp.addPoint(crossline2[2], crossline2[3]); // HEIGHT
		temp.addPoint(crossline[0], crossline[1]); // HEIGHT
		return temp;
	}
	
	private static int[] getArrowHeadLine(int xsource, int ysource,int xdest,int ydest, int distance)
	{
		int[] arrowhead = new int[2];
		int headsize = distance;

		double stretchfactor = 0;
		stretchfactor = 1 - (headsize/(Math.sqrt(((xdest-xsource)*(xdest-xsource))+((ydest-ysource)*(ydest-ysource)))));

		arrowhead[0] = (int) (stretchfactor*(xdest-xsource))+xsource;
		arrowhead[1] = (int) (stretchfactor*(ydest-ysource))+ysource;

		return arrowhead;
	}

	private static int[] getArrowHeadCrossLine(int x1, int x2, int b1, int b2, double factor)
	{
		int [] crossline = new int[4];

		int x_dest = (int) (((b1-x1)*factor)+x1);
		int y_dest = (int) (((b2-x2)*factor)+x2);

		crossline[0] = (int) ((x1+x2-y_dest));
		crossline[1] = (int) ((x2+x_dest-x1));
		crossline[2] = crossline[0]+(x1-crossline[0])*2;
		crossline[3] = crossline[1]+(x2-crossline[1])*2;
		return crossline;
	}
	*/
	
	public CalicoPacket[] getUpdatePackets()
	{
		//UUID CANVASUID ARROW_TYPE ANCHOR_A_TYPE ANCHOR_A_UUID ANCHOR_A_X ANCHOR_A_Y   ANCHOR_B_TYPE ANCHOR_B_UUID ANCHOR_B_X ANCHOR_B_Y
		return new CalicoPacket[]{
				CalicoPacket.getPacket(
						NetworkCommand.ARROW_CREATE,
						this.uuid,
						this.canvasUID,
						this.arrowType, 

						//this.color.getRed(),
						//this.color.getGreen(),
						//this.color.getBlue(),
						this.color.getRGB(),
						
						this.anchorA.getType(),
						this.anchorA.getUUID(),
						this.anchorA.getPoint().x,
						this.anchorA.getPoint().y,
						
						this.anchorB.getType(),
						this.anchorB.getUUID(),
						this.anchorB.getPoint().x,
						this.anchorB.getPoint().y
				)
		};
	}
	
}
