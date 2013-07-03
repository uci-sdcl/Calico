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

import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Transparency;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D.Double;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.StringTokenizer;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import org.apache.commons.lang.ArrayUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;

import calico.Calico;
import calico.CalicoDraw;
import calico.CalicoOptions;
import calico.CalicoUtils;
import calico.components.arrow.AnchorPoint;
import calico.components.arrow.CArrow;
import calico.components.bubblemenu.BubbleMenu;
import calico.components.decorators.CGroupDecorator;
import calico.components.piemenu.PieMenu;
import calico.components.piemenu.PieMenuButton;
import calico.controllers.CArrowController;
import calico.controllers.CCanvasController;
import calico.controllers.CConnectorController;
import calico.controllers.CGroupController;
import calico.controllers.CStrokeController;
import calico.inputhandlers.CalicoInputManager;
import calico.networking.Networking;
import calico.networking.netstuff.ByteUtils;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;
import calico.utils.Geometry;
import calico.utils.RoundPolygon;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.activities.PActivity;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.util.PAffineTransform;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PPaintContext;

public class CGroup extends PPath implements Serializable {
	private static Logger logger = Logger.getLogger(CGroup.class.getName());

	public static final int RIGHTCLICK_MODE_DRAWGROUP = 100;
	public static final int RIGHTCLICK_MODE_MOVE = 101;
	// public static final int RIGHTCLICK_MODE_D = 100;

	private static final long serialVersionUID = 1L;
	protected float dash1[] = { 10.0f };

	protected long uuid = 0L;
	protected long puid = 0L;
	protected long cuid = 0L;

	protected Polygon points = new Polygon();
//	protected GeneralPath smoothedPath;
//	protected PAffineTransform groupTransform;
//	protected Point2D origOrigin;
	protected double scaleX = 1.0d, scaleY = 1.0d;
	protected double rotation = 0.0d;
	
	//See method applyAffineTransform() for explanation
//	private Polygon coordsOriginal;
	
	//See method applyAffineTransform() for explanation
//	ArrayList<AffineTransform> groupTransforms; 	
	
	// These are all the child groups
	protected LongArraySet childGroups = new LongArraySet();

	// the child BGElements, this ARE NOT bgelements that are in child groups
	protected LongArraySet childStrokes = new LongArraySet();

	protected LongArraySet childArrows = new LongArraySet();
	
	protected LongArraySet childConnectors = new LongArraySet();

	protected String text = "";
	private boolean textSet = false;

	// When this is false, we do not check if a bigger group contains this
	// however, this can still have children.
	protected boolean isGrouped = true;

	protected boolean finished = false;

	protected double groupArea = 0.0;

	protected float transparency = CalicoOptions.group.background_transparency;
	
	protected int rightClickMode = 2;// 1=draw group, 2 = move it.
	protected int previous_rightClickMode = 2;
	protected boolean rightClickToggled = false;

	protected boolean isPermanent = false;
	
	//This allows multiple groups to be highlighted at the same time
	protected boolean isHighlighted = false;

	// used by convex hull algorithm
	ArrayList<Line2D> chkLns = new ArrayList<Line2D>();
	ArrayList<Line2D> tempLns = new ArrayList<Line2D>();
	ArrayList<Line2D> tempHull = new ArrayList<Line2D>();
	
	protected int networkLoadCommand = NetworkCommand.GROUP_LOAD;	

	// This will hold the pie menu buttons (Class<?>)
	private static ObjectArrayList<Class<?>> pieMenuButtons = new ObjectArrayList<Class<?>>(); 
	public static void registerPieMenuButton(Class<?> button)
	{
		if(!pieMenuButtons.contains(button))
		{
			pieMenuButtons.add(button);
			System.out.println("Registering to pie menu: " + button.getName());
		}
	}
	
	public CGroup(long uuid, long cuid, long puid, boolean isperm) {
		this.uuid = uuid;
		this.cuid = cuid;
		//setParentUUID(puid);
		this.puid = puid;
		this.isPermanent = isperm;
		
		setStroke(new BasicStroke(CalicoOptions.group.stroke_size,
				BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, dash1,
				0.0f));
		setStrokePaint(CalicoOptions.group.stroke_color);

		drawPermTemp(false);
		//this.setTransparency(0f);
		CalicoDraw.setNodeTransparency(this, 0f);
		
//		setPieMenuButtons();
	}
	

	
//	protected void setPieMenuButtons()
//	{
//		registerPieMenuButton(calico.components.piemenu.groups.GroupDropButton.class);
//		registerPieMenuButton(calico.components.piemenu.groups.GroupSetPermanentButton.class);
//		registerPieMenuButton(calico.components.piemenu.groups.GroupShrinkToContentsButton.class);
//		registerPieMenuButton(calico.components.piemenu.groups.ListCreateButton.class);
//		registerPieMenuButton(calico.components.piemenu.groups.GroupMoveButton.class);
//		registerPieMenuButton(calico.components.piemenu.groups.GroupCopyDragButton.class);
//		registerPieMenuButton(calico.components.piemenu.groups.GroupRotateButton.class);
//		registerPieMenuButton(calico.components.piemenu.canvas.ArrowButton.class);
//		registerPieMenuButton(calico.components.piemenu.groups.GroupDeleteButton.class);
//		registerPieMenuButton(calico.components.piemenu.canvas.TextCreate.class);
//	}
	
	public ObjectArrayList<Class<?>> getPieMenuButtons()
	{
		ObjectArrayList<Class<?>> pieMenuButtons = new ObjectArrayList<Class<?>>();
		pieMenuButtons.addAll(internal_getPieMenuButtons());
		pieMenuButtons.addAll(CGroup.pieMenuButtons);
		return pieMenuButtons;
	}
	
	protected ObjectArrayList<Class<?>> internal_getPieMenuButtons()
	{
		ObjectArrayList<Class<?>> pieMenuButtons = new ObjectArrayList<Class<?>>(); 
		pieMenuButtons.add(calico.components.piemenu.groups.GroupDropButton.class);
		pieMenuButtons.add(calico.components.piemenu.groups.GroupSetPermanentButton.class);
		pieMenuButtons.add(calico.components.piemenu.groups.GroupShrinkToContentsButton.class);
		pieMenuButtons.add(calico.components.piemenu.groups.ListCreateButton.class);
//		pieMenuButtons.add(calico.components.piemenu.groups.GroupMoveButton.class);
		pieMenuButtons.add(calico.components.piemenu.groups.GroupCopyDragButton.class);
		pieMenuButtons.add(calico.components.piemenu.groups.GroupRotateButton.class);
		pieMenuButtons.add(calico.components.piemenu.groups.GroupResizeButton.class); //7
		//pieMenuButtons.add(calico.components.piemenu.canvas.ArrowButton.class);
		pieMenuButtons.add(calico.components.piemenu.groups.GroupDeleteButton.class);
		//pieMenuButtons.add(calico.components.piemenu.canvas.ImageCreate.class);
		return pieMenuButtons;
	}
	
	public ObjectArrayList<Class<?>> getBubbleMenuButtons()
	{
		ObjectArrayList<Class<?>> pieMenuButtons = new ObjectArrayList<Class<?>>();
		pieMenuButtons.addAll(internal_getBubbleMenuButtons());
		pieMenuButtons.addAll(CGroup.pieMenuButtons); //5
		return pieMenuButtons;
	}
	
	protected ObjectArrayList<Class<?>> internal_getBubbleMenuButtons()
	{
		ObjectArrayList<Class<?>> pieMenuButtons = new ObjectArrayList<Class<?>>(); 
		pieMenuButtons.add(calico.components.piemenu.groups.GroupDropButton.class); //12
		pieMenuButtons.add(calico.components.piemenu.groups.GroupSetPermanentButton.class); //1
		pieMenuButtons.add(calico.components.piemenu.groups.GroupShrinkToContentsButton.class); //2
		pieMenuButtons.add(calico.components.piemenu.groups.ListCreateButton.class); //3
//		pieMenuButtons.add(calico.components.piemenu.groups.GroupMoveButton.class); //4
		pieMenuButtons.add(calico.components.piemenu.groups.GroupCopyDragButton.class); //6
		pieMenuButtons.add(calico.components.piemenu.groups.GroupRotateButton.class); //7
		pieMenuButtons.add(calico.components.piemenu.groups.GroupResizeButton.class); //7
		pieMenuButtons.add(calico.components.piemenu.groups.GroupTextButton.class); //10
		//pieMenuButtons.add(calico.components.piemenu.canvas.ArrowButton.class); //9
		pieMenuButtons.add(calico.components.piemenu.groups.GroupDeleteButton.class); //11
		//pieMenuButtons.add(calico.components.piemenu.canvas.ImageCreate.class);
		return pieMenuButtons;
	}

	public long getUUID() {
		return this.uuid;
	}
	
	public double getRotation()
	{
		return rotation;
	}
	
	public double getScale()
	{
		return scaleX;
	}
	
	public CGroup(long uuid, long cuid, long puid) {
		this(uuid, cuid, puid, false);
	}

	public CGroup(long uuid, long cuid) {
		this(uuid, cuid, 0L);
	}

	public byte[] getHashCode() {
		CalicoPacket pack = new CalicoPacket(18);
		pack.putInt(Arrays.hashCode(new long[] { this.uuid, this.cuid,
				this.puid }));
		pack.putInt(Arrays.hashCode(this.points.xpoints));
		pack.putInt(Arrays.hashCode(this.points.ypoints));
		pack.putInt(Arrays.hashCode(getChildStrokes()));
		pack.putInt(Arrays.hashCode(getChildGroups()));
		pack.putInt(Arrays.hashCode(getChildArrows()));
		pack.putInt(Arrays.hashCode(getChildConnectors()));

		return pack.getBuffer();
	}

	public long getCanvasUID() {
		return this.cuid;
	}

	public boolean isPermanent() {
		return this.isPermanent;
	}

	public void setPermanent(boolean temp) {
		this.isPermanent = temp;

		drawPermTemp();
		
		/*if (temp
				&& CStrokeController.exists(CGroupController.originalStroke))
		{
			CStrokeController.delete(CGroupController.originalStroke);
		}*/
	}

	public void drawPermTemp() {
		drawPermTemp(true);
	}

	public void drawPermTemp(boolean repaint) {
		if (isPermanent()) {
			setStroke(new BasicStroke(CalicoOptions.group.stroke_size, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			setStrokePaint(CalicoOptions.group.stroke_color);
			setPaint(CalicoOptions.group.background_color);
//			setTransparency(CalicoOptions.group.background_transparency);
		} else {
			setStroke(new BasicStroke(CalicoOptions.group.stroke_size,
					BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash1,
					0.0f));
			setStrokePaint(CalicoOptions.group.stroke_color);
			setPaint(CalicoOptions.group.temp_background_color);
//			setTransparency(CalicoOptions.group.background_transparency);
		}

		if (repaint && this.getBounds() != null && this.getBounds().width > 0 && this.getBounds().height > 0 
				&& CCanvasController.exists(cuid)) {
//			this.setPaintInvalid(true);
//			 CCanvasController.canvasdb.get(this.cuid).repaint(this.getBounds());
			
			//CCanvasController.canvasdb.get(cuid).getCamera().repaintFrom(this.getBounds(), this);
			if (CCanvasController.getCurrentUUID() == getCanvasUID())
				CalicoDraw.repaintNode(CCanvasController.canvasdb.get(cuid).getCamera(), this.getBounds(), this);
		}
	}

	public int getRightClickMode() {
		return rightClickMode;
	}

	public void setRightClickMode(int mode) {
		rightClickToggled = true;
		previous_rightClickMode = rightClickMode;
		rightClickMode = mode;
	}

	public void resetRightClickMode() {
		if (!rightClickToggled) {
			return;
		}
		int temp = rightClickMode;
		rightClickMode = previous_rightClickMode;
		previous_rightClickMode = temp;
		rightClickToggled = false;
	}

	public int get_signature() {
		if (!finished)
			return 0;
		int sig = this.points.npoints + this.points.xpoints[0]
				+ this.points.ypoints[0] + this.text.length() + (int)(this.rotation*10) + (int)(this.scaleX*10) + (int)(this.scaleY*10);
		if (isPermanent()) {
			sig++;
		}
//		System.out.println("Debug sig for group " + uuid + ": " + sig + ", 1) " + this.points.npoints + ", 2) " + isPermanent() + ", 3) " + this.points.xpoints[0] + ", 4) " + this.points.xpoints[0] + ", 5) " + this.points.ypoints[0] + ", 6) " + (int)(this.rotation*10) + ", 7) " + (int)(this.scaleX*10) + ", 8) " + (int)(this.scaleY*10));
		return sig;
	}
	
	public String get_signature_debug_output()
	{
		return "Debug sig for group " + uuid + ": 1) " + this.points.npoints + ", 2) " + isPermanent() + ", 3) " + this.text.length() + ", 4) " + this.points.xpoints[0] + ", 5) " + this.points.ypoints[0] + ", 6) " + (int)(this.rotation*10) + ", 7) " + (int)(this.scaleX*10) + ", 8) " + (int)(this.scaleY*10);
	}

//	public Polygon getPolygon() {
//		Polygon p = getPolyFromPath(getPathReference().getPathIterator(null));
//		logger.trace("CGroup.getPolygon called.\n" +
//				"Bounds: (" + p.getBounds().x + ", " + p.getBounds().y + ", " +
//							+ p.getBounds().width + ", " + p.getBounds().height + ")");
//		return p;
//	}
	
	public GeneralPath getPathReference()
	{
		return super.getPathReference();
	}
	
	public Polygon getRawPolygon() {
		return points;
	}

	public long getParentUUID() {
		return this.puid;
	}
	
	public void setParentUUID(long u) {
		if (u == this.puid)
			return;
		
		if (puid == this.uuid)
			puid = 0l;
		
		logger.trace("Changing parent for " + uuid + ": " + this.puid + " -> " + u);
		this.puid = u;
		if (CGroupController.exists(u))
		{
			CGroup parent = CGroupController.groupdb.get(u);
			if (!parent.hasChildGroup(this.uuid))
			{
				Point2D midPoint = getMidPoint();
				parent.addChildGroup(this.uuid, (int)midPoint.getX(), (int)midPoint.getY());
			}
			resetViewOrder();
		}

	}

	/*public void setParentUUID(long puid) {
		if (puid == this.puid)
			return;
		
		if (puid == this.uuid)
			puid = 0l;
		
//		if (puid == 0 && CGroupController.groupdb.get(this.puid) instanceof CGroupDecorator)
//			System.out.println("Setting parent to 0!");
		
		logger.trace("Changing parent for " + uuid + ": " + this.puid + " -> " + puid);
		this.puid = puid;
		if (CGroupController.exists(puid))
		{
			CGroup parent = CGroupController.groupdb.get(puid);
			if (!parent.hasChildGroup(this.uuid))
			{
				Point2D midPoint = getMidPoint();
				if (midPoint == null)
					midPoint = new Point2D.Double(0,0);
				parent.addChildGroup(this.uuid, (int)midPoint.getX(), (int)midPoint.getY());
			}
			resetViewOrder();
		}
	}*/

	public double getArea() {
		return this.groupArea;
	}

	public boolean isChild() {
		return (this.puid != 0);
	}

	public boolean isFinished() {
		return this.finished;
	}

	public void delete() {
		delete(true);
	}

	public void delete(boolean recurse) {
		PBounds bounds = this.getBounds();
		
		if (recurse) {

			long[] child_strokes = getChildStrokes();
			long[] child_groups = getChildGroups();
			long[] child_arrows = getChildArrows();
			long[] child_connectors = getChildConnectors();

			// Reparent any strokes
			if (child_strokes.length > 0) {
				for (int i = 0; i < child_strokes.length; i++) {
					CStrokeController.no_notify_delete(child_strokes[i]);
				}
			}

			// Reparent any groups
			if (child_groups.length > 0) {
				for (int i = 0; i < child_groups.length; i++) {
					CGroupController.no_notify_delete(child_groups[i]);
				}
			}

			if (child_arrows.length > 0) {
				for (int i = 0; i < child_arrows.length; i++) {
					CArrowController.no_notify_delete(child_arrows[i]);
				}
			}
			
			if (child_connectors.length > 0) {
				for (int i = 0; i < child_connectors.length; i++) {
					CConnectorController.no_notify_delete(child_connectors[i]);
				}
			}
		}

		// remove from parent
		if (this.puid != 0L) {
			CGroupController.no_notify_delete_child_group(this.puid, this.uuid);
		}

		// Remove from the canvas
		CCanvasController.no_notify_delete_child_group(this.cuid, this.uuid);
		
//		CCanvasController.canvasdb.get(this.cuid).repaint(bounds);
	}

	public void drop() {
		delete(false);
	}

	public boolean containsPoint(int x, int y) {
		return getPathReference().contains(x, y);
	}
	
	public boolean containsShape(Shape shape)
	{
		if (shape == null)
			return false;
		Polygon polygon = Geometry.getPolyFromPath(shape.getPathIterator(null));
		GeneralPath containerGroup = getPathReference();
		if (containerGroup == null)
			return false;
		int totalNotContained = 0;
		for(int i=0;i<polygon.npoints;i++)
		{
			if (!containerGroup.contains(new Point(polygon.xpoints[i], polygon.ypoints[i])))
			{
				totalNotContained++;
			}
			if (totalNotContained > polygon.npoints*.1)
				return false;
		}
		return true;
	}

	public void addChildArrow(long uid) {
		this.childArrows.add(uid);
	}

	public void deleteChildArrow(long uid) {
		this.childArrows.remove(uid);
	}

	public long[] getChildArrows() {
		return this.childArrows.toLongArray();
	}
	
	public void addChildConnector(long uid) {
		this.childConnectors.add(uid);
	}

	public void deleteChildConnector(long uid) {
		this.childConnectors.remove(uid);
	}

	public long[] getChildConnectors() {
		return this.childConnectors.toLongArray();
	}
	

	public void setChildStrokes(long[] bglist) {
		childStrokes.clear();
		for (int i = 0; i < bglist.length; i++)
			childStrokes.add(bglist[i]);
	}

	public void setChildGroups(long[] gplist) {
		long[] oldChildGroups = childGroups.toLongArray();
		for (int i = 0; i < oldChildGroups.length; i++)
			if (CGroupController.exists(oldChildGroups[i]))
				CGroupController.groupdb.get(oldChildGroups[i]).setParentUUID(0l);
		
		childGroups.clear();
		for (int i = 0; i < gplist.length; i++)
		{
			childGroups.add(gplist[i]);
			if (CGroupController.exists(gplist[i]))
			{
				CGroup child = CGroupController.groupdb.get(gplist[i]);
				if (child.getParentUUID() != this.uuid)
					child.setParentUUID(this.uuid);
			}
		}
	}
	
	public void setChildArrows(long[] arlist, int x, int y) {
		this.childArrows.clear();
		for (int i = 0; i < arlist.length; i++)
			childArrows.add(arlist[i]);
	}
	
	public void setChildConnectors(long[] ctlist, int x, int y) {
		this.childConnectors.clear();
		for (int i = 0; i < ctlist.length; i++)
			childConnectors.add(ctlist[i]);
	}

	public void addChildStroke(long bgeUUID) {
		this.childStrokes.add(bgeUUID);
	}

	public void addChildGroup(long grpUUID, int x, int y) {
		if (!this.childGroups.contains(grpUUID))
			this.childGroups.add(grpUUID);
		
		if (CGroupController.exists(grpUUID))
		{
			CGroup child = CGroupController.groupdb.get(grpUUID);
			if (child.getParentUUID() != this.uuid)
				child.setParentUUID(this.uuid);
		}
	}

	public void deleteChildStroke(long bgeuuid) {
		this.childStrokes.rem(bgeuuid);
	}

	public void deleteChildGroup(long guuid) {
		this.childGroups.rem(guuid);
	}

	public long[] getChildStrokes() {
		return this.childStrokes.toLongArray();
	}

	public long[] getChildGroups() {
		return this.childGroups.toLongArray();
	}
	
	public boolean hasChildGroup(long childuuid) {
		long[] children = getChildGroups();
		for (int i = 0; i < children.length; i++)
		{
			if (children[i] == childuuid)
				return true;
		}
		return false;
	}

	public void move(int x, int y) {
		
		if (childArrows.size() > 0) {
			long[] auid = childArrows.toLongArray();
			for (int i = 0; i < auid.length; i++) {
				CArrowController.no_notify_move_group_anchor(auid[i], uuid, x, y);
			}
		}
		
		if (childConnectors.size() > 0) {
			long[] cuid = childConnectors.toLongArray();
			for (int i = 0; i < cuid.length; i++) {
				CConnectorController.no_notify_move_group_anchor(cuid[i], uuid, x, y);
			}
		}
		
		for (long s : childStrokes)
		{
			if (CStrokeController.strokes.containsKey(s))
			{
				if (CStrokeController.is_parented_to(s, this.uuid))
					CStrokeController.no_notify_move(s, x, y);
				else {
					Calico.logger.error("GROUP " + this.uuid
							+ " is trying to move kidnapped stroke " + s);
					CGroupController.no_notify_delete_child_stroke(this.uuid,s);
				}
			}
		}
		
		for (long g : childGroups)
		{
			if (CGroupController.is_parented_to(g, this.uuid))
				CGroupController.no_notify_move(g, x, y);
			else {
				Calico.logger.error("GROUP " + this.uuid
						+ " is trying to move kidnapped group " + g);
				CGroupController.no_notify_delete_child_group(this.uuid, g);
			}
		}

		points.translate(x, y);
		applyAffineTransform();
	}

	public void append(int[] x, int[] y)
	{
		for (int i = 0; i < x.length; i++)
			this.points.addPoint(x[i], y[i]);
		
		redrawAll();
	}
	
	public void append(int x, int y) {
		this.points.addPoint(x, y);

		redrawAll();

//		this.setPaintInvalid(true);
//		 CCanvasController.canvasdb.get(this.cuid).repaint();

	}

	public void rectify() {
		Rectangle2D bound = points.getBounds2D();
		points = new Polygon();
		points = CalicoUtils.pathIterator2Polygon(bound.getPathIterator(null));
		redrawAll();
	}

	public void circlify() {
		Rectangle2D bound = points.getBounds2D();
		points = new Polygon();
		Ellipse2D elip = new Ellipse2D.Double(bound.getX(), bound.getY(), bound
				.getWidth(), bound.getHeight());
		points = CalicoUtils.pathIterator2Polygon(elip.getPathIterator(null));
		redrawAll();
	}

	public void redraw() {
		redraw(getCoordList());
	}

	public void redraw(Polygon poly) {
		if (poly.npoints < 1)
			return;
		

		GeneralPath bezieredPoly = Geometry.getBezieredPoly(poly);
		if (poly.npoints > 2)
			setPathTo(bezieredPoly);
//		CCanvasController.canvasdb.get(cuid).getCamera().repaintFrom(this.getBounds(), this);
	}

	public void redrawAll() {
		redrawAll(getCoordList());
	}

	public void redrawAll(Polygon poly) {
		redraw(poly);
//		this.setPaintInvalid(true);
		// CCanvasController.canvasdb.get(this.cuid).repaint(poly.getBounds());
	}

	public Polygon getCoordList() {
		return this.points;
	}

	public void finish(boolean fade) {
		if (this.finished) {
			return;
		}
		this.finished = true;

		this.groupArea = calico.Geometry.computePolygonArea(this.points);
//		CalicoInputManager.addGroupInputHandler(this.uuid);
		this.setInputHandler();
//		smoothedPath = Geometry.getBezieredPoly(points);
		
//		setPathTo(smoothedPath);
//		this.setPaintInvalid(true);
//		origOrigin = new Point2D.Double(smoothedPath.getBounds2D().getX(), smoothedPath.getBounds2D().getY());

		
		applyAffineTransform();
		if (fade
				&& Networking.connectionState != Networking.ConnectionState.Connecting)
		{
			PActivity flash = new PActivity(500,70, System.currentTimeMillis()) {
				long step = 0;
	      
			    protected void activityStep(long time) {
			            super.activityStep(time);
			            setTransparency(1.0f * step/5 * transparency);
			            //CalicoDraw.setNodeTransparency(CGroup.this, 1.0f * step/5 * transparency);
	//		            repaint();
			            step++;
			            
			            if (step > 5)
			            	terminate();
			    }
			    
			    protected void activityFinished() {
			    	if (getTransparency() != transparency)
			    		setTransparency(transparency);
			    		//CalicoDraw.setNodeTransparency(CGroup.this, transparency);
			    }
			};
			// Must schedule the activity with the root for it to run.
			//getRoot().addActivity(flash);
			CalicoDraw.addActivityToNode(this, flash);
		}
		else
			//setTransparency(transparency);
			CalicoDraw.setNodeTransparency(this, transparency);
	}
	
	public void setInputHandler()
	{
		CalicoInputManager.addGroupInputHandler(this.uuid);
	}

	public void setText(String text) {
		this.text = text;
		this.textSet = true;
	}

	protected void paint(final PPaintContext paintContext) {
		final Graphics2D g2 = paintContext.getGraphics();
		Shape border;
		border = (Shape)getPathReference().clone();
		((GeneralPath)border).closePath();
		
		//This draws the highlight
		Composite temp = g2.getComposite();
		if (CGroupController.exists(getParentUUID()) && !CGroupController.groupdb.get(getParentUUID()).isPermanent() || isHighlighted /*BubbleMenu.highlightedGroup == this.uuid*/)
		{
			if (CGroupController.exists(getParentUUID()))
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, CGroupController.groupdb.get(getParentUUID()).getTransparency()));
//			else
//				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
			g2.setStroke(new BasicStroke(CalicoOptions.pen.stroke_size + 8));
			g2.setPaint(Color.blue);
			g2.draw(border);

//			Color borderColor = Color.BLUE;
//			Point2D center = new Point2D.Float((float)border.getBounds().x + (float)border.getBounds().getWidth()/2, 
//					(float)border.getBounds().y + (float)border.getBounds().getHeight()/2);
//		     float radius = (float)border.getBounds().getWidth() + 25;
//		     float[] dist = {.2f, .5f, .7f};
//
//		     
//
//		     Stroke borderStroke =
//		    		 new BasicStroke(50f,
//		    				 BasicStroke.CAP_BUTT,
//		    				 BasicStroke.JOIN_MITER);
//		     Color highlightColor = new Color(borderColor.getRed(),borderColor.getGreen(),borderColor.getBlue(), 255);
//		     Color[] colors = {new Color(0,0,0,0),highlightColor, new Color(0,0,0,0)};
//		     RadialGradientPaint gradient =
//		    		 new RadialGradientPaint(center, radius, dist, colors);
//		     g2.setPaint(gradient);
////		     g2.fill(border);
////		     g2.drawRoundRect(0, 0, ((int) border.getBounds().width) - 1, ((int) border.getBounds().height) - 1, 10, 10);
//		     g2.setStroke(borderStroke);
//		     g2.draw(border);
//		     g2.fill(border);
//		     g2.drawRoundRect(0, 0, ((int) border.getBounds().width) - 1, ((int) border.getBounds().height) - 1, 10, 10);
		     



			g2.setPaint(this.getStrokePaint());
			g2.setStroke(new BasicStroke(CalicoOptions.group.stroke_size,
					BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash1,
					0.0f));
			
			g2.draw(border);
		}
		else
		{
			g2.setPaint(this.getStrokePaint());
			g2.setStroke(this.getStroke());
			
			g2.draw(border);
		}
		g2.setComposite(temp);
		
//		if (getTransparency() > CalicoOptions.group.background_transparency)
//			System.out.println("&&&&&&&&&&&&&&&&& TRANSPARENCY HAS BEEN SET TO: " + getTransparency() + " &&&&&&&&&&&&&&&&&&&&&&&&&");
		g2.setPaint(this.getPaint());
		g2.fill(border);
		



		
		//draw bounding box for debugging
//		g2.setColor(Color.red);
//		Rectangle rect = border.getBounds();
//		g2.drawRect(rect.x, rect.y, rect.width, rect.height);

		if (textSet) {
			PAffineTransform piccoloTextTransform = getPTransform();
			paintContext.pushTransform(piccoloTextTransform);
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
			g2.setColor(Color.BLACK);
			
			g2.setFont(CalicoOptions.group.font);
			
			String[] tokens = text.split("\n");
			int yTextOffset = 0;
			for (int i = 0; i < tokens.length; i++)
			{	
				String token = tokens[i];
				g2.drawString(token,
						(float) (points.getBounds().getX() + CalicoOptions.group.text_padding + CalicoOptions.group.padding), 
						(float) (points.getBounds().getY() + CalicoOptions.group.text_padding + CalicoOptions.group.padding*2.25 + yTextOffset));
				yTextOffset += Geometry.getTextBounds(token).height;
			}
			paintContext.popTransform(piccoloTextTransform);
			
		}
		
//		long currTime = (new Date()).getTime();
//		System.out.println("CGroup repaint @ time: " + currTime);

	}
	
	public String getText()
	{
		return text;
	}
	
//	public void calculateParenting(boolean includeStrokes) {
//		calculateParent((int)getMidPoint().getX(), (int)getMidPoint().getY());
//	}
	
	public void calculateParenting(boolean includeStrokes, int x, int y) {

		// Check the bounds for the other items on the canvas.
		
		
		long[] grouparr = CCanvasController.canvasdb.get(this.cuid)
				.getChildGroups();
		long[] bgearr = CCanvasController.canvasdb.get(this.cuid)
				.getChildStrokes();
		long[] ararr = CCanvasController.canvasdb.get(this.cuid)
		.getChildArrows();

		// Check to see if any groups are inside of this.
		if (grouparr.length > 0) {
			for (int i = 0; i < grouparr.length; i++) {
				if (CGroupController.canParentChild(this.uuid, grouparr[i], x, y)
						&& CGroupController.group_contains_group(this.uuid, grouparr[i])) 
				{
					// it is contained in the group, so set it's parent
					CGroupController.no_notify_set_parent(grouparr[i], this.uuid);
				}
			}
		}

		// Send the BGElement Parents
		if (includeStrokes && bgearr.length > 0) {
			for (int i = 0; i < bgearr.length; i++) {
				// We first check to make sure this element isnt parented to
				// something else
				// then we check to see if it contained in this group
				if (CGroupController.canParentChild(this.uuid, bgearr[i], x, y)
						&& CGroupController.group_contains_stroke(this.uuid, bgearr[i])) {
					// it is contained in the group, so set it's parent
					// CStrokeController.no_notify_set_parent(bgearr[i],
					// this.uuid);
					// changed by Nick
					CStrokeController.no_notify_set_parent(bgearr[i], this.uuid);
				} 
				else if (/*CStrokeController.strokes.get(bgearr[i])
						.getParentUUID() != 0L
						&&*/ CGroupController.group_contains_stroke(this.uuid, bgearr[i])) 
				{
					// Check to see if the current parent group is larger than
					// this one.
					long pguid = CStrokeController.strokes.get(bgearr[i])
							.getParentUUID();

					if (CGroupController.group_contains_group(pguid, this.uuid)
							|| !CGroupController.group_contains_stroke(pguid, bgearr[i])) {
						// it is contained in the group, so set it's parent
						CStrokeController.no_notify_set_parent(bgearr[i], this.uuid);
					}

				}

			}// for bgearr
		}
		
		//Check for arrows (and hey, why not use this cool flag that was already here)
		if (includeStrokes && ararr.length > 0)
		{
			CArrow arr;
			for (int i = 0; i < ararr.length; i++) {
				arr = CArrowController.arrows.get(ararr[i]);
				if (arr.getAnchorA().getType() == CArrow.TYPE_CANVAS
					|| CGroupController.group_contains_group(arr.getAnchorA().getUUID(),this.uuid)
					)
				{
					if (this.containsPoint(arr.getAnchorA().getPoint().x, arr.getAnchorA().getPoint().y))
					{
						arr.setAnchorA(new AnchorPoint(CArrow.TYPE_GROUP, arr.getAnchorA().getPoint(), this.uuid));
						addChildArrow(ararr[i]);
					}
				}
				if (arr.getAnchorB().getType() == CArrow.TYPE_CANVAS
						|| CGroupController.group_contains_group(arr.getAnchorB().getUUID(),this.uuid)
						)
				{
					if (this.containsPoint(arr.getAnchorB().getPoint().x, arr.getAnchorB().getPoint().y))
					{
						arr.setAnchorB(new AnchorPoint(CArrow.TYPE_GROUP, arr.getAnchorB().getPoint(), this.uuid));
						addChildArrow(ararr[i]);
					}
				}				
			}
		}
		
		//Connector: A connector shouldn't need to check its parents because it turns to a stroke when one of its parents is dropped.
		//It is deleted when one of its parents is deleted.
	}
	@Override
	public void repaint() {
		super.repaint();
//		Rectangle bounds = getPathReference().getBounds();
//		double padding = CalicoOptions.group.padding * 2.0d;
//		PBounds paddedBounds = new PBounds(bounds.x -  padding, bounds.y - padding, bounds.width + padding * 2, bounds.height + padding * 2);
////		this.invalidatePaint();
//		CCanvasController.canvasdb.get(cuid).getLayer().repaintFrom(paddedBounds, this);
	}

	public void clearChildGroups() {
		this.childGroups.clear();
		this.childGroups = new LongArraySet();
	}

	public void clearChildStrokes() {
		this.childStrokes.clear();
		this.childStrokes = new LongArraySet();
	}

	public void clearChildArrows() {
		this.childArrows.clear();
		this.childArrows = new LongArraySet();
	}
	
	public void clearChildConnectors() {
		this.childConnectors.clear();
		this.childConnectors = new LongArraySet();
	}

	public void extra_submitToDesignMinders() {
		try {
			HttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost(
					"http://128.195.20.32:8080/designminders/Notecards");

			post.addHeader("X-Calico-Version", "3.0");
			post.addHeader("User-Agent", "Calico3Client HTTP API/3.0");

			MultipartEntity reqEntity = new MultipartEntity(
					HttpMultipartMode.BROWSER_COMPATIBLE);

			reqEntity.addPart("name", new StringBody("Insert card name"));
			reqEntity
					.addPart("desc", new StringBody("Insert card description"));
			reqEntity.addPart("tags", new StringBody("tags,tag,thing"));
			reqEntity.addPart("color", new StringBody("ff0000"));

			Image image = this.toImage();

			BufferedImage buf = new BufferedImage(image.getWidth(null), image
					.getHeight(null), BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = (Graphics2D) buf.getGraphics();
			g.drawImage(image, 0, 0, null);

			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ImageIO.write(buf, "PNG", bos);

			byte[] imagedata = bos.toByteArray();

			reqEntity.addPart("image", new InputStreamKnownSizeBody(
					new ByteArrayInputStream(imagedata), imagedata.length,
					"image/png", "test.png"));

			post.setEntity(reqEntity);

			HttpResponse resp = client.execute(post);
			HttpEntity respEnt = resp.getEntity();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	class InputStreamKnownSizeBody extends InputStreamBody {
		private int lenght;

		public InputStreamKnownSizeBody(final InputStream in, final int lenght,
				final String mimeType, final String filename) {
			super(in, mimeType, filename);
			this.lenght = lenght;
		}

		@Override
		public long getContentLength() {
			return this.lenght;
		}
	}
	
	public long[] getPossibleChildren()
	{
		
		ArrayList<Long> possibleChildren = new ArrayList<Long>();
		// Check the bounds for the other items on the canvas.
		
		long[] grouparr = CCanvasController.canvasdb.get(this.cuid).getChildGroups();
		long[] bgearr = CCanvasController.canvasdb.get(this.cuid).getChildStrokes();

		// Check to see if any groups are inside of this.
		if(grouparr.length>0)
		{
			for(int i=0;i<grouparr.length;i++)
			{
				if(CGroupController.groupdb.get(grouparr[i]).getParentUUID()==0L && CGroupController.group_contains_group(this.uuid, grouparr[i]))
				{
					// it is contained in the group, so set it's parent
					possibleChildren.add(new Long(CGroupController.groupdb.get(grouparr[i]).uuid));
				}
			}
		}
		
		// Send the BGElement Parents
		if(bgearr.length>0)
		{
			for(int i=0;i<bgearr.length;i++)
			{
				// We first check to make sure this element isnt parented to something else
				// then we check to see if it contained in this group
				if(CGroupController.group_contains_stroke(this.uuid, bgearr[i]))
				{
					// it is contained in the group, so set it's parent
//					CStrokeController.no_notify_set_parent(bgearr[i], this.uuid);
					//changed by Nick
					possibleChildren.add(new Long(CStrokeController.strokes.get(bgearr[i]).getUUID()));
				}
				
			}//for bgearr
		}
		
		long[] ret = new long[possibleChildren.size()];
		for (int i = 0; i < possibleChildren.size(); i++)
			ret[i] = possibleChildren.get(i);
		return ret;
	}
	
	public Rectangle getBoundsOfObjects(long[] children)
	{
		if (children.length == 0)
			return new Rectangle(0,0,0,0);
		
		int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
		Rectangle bounds;
		
		// iterate through child strokes
		for (int i = 0; i < children.length; i++) {
			if (CStrokeController.strokes.containsKey(children[i])) {
				if (CStrokeController.strokes.get(children[i]) == null
						|| CStrokeController.strokes.get(children[i]).getPathReference().getBounds().x == 0)
					continue;
				bounds = CStrokeController.strokes.get(children[i]).getPathReference().getBounds();
//				printBounds("Stroke " + children[i], bounds);
			}
			else if (CGroupController.groupdb.containsKey(children[i])) 
			{
				bounds = CGroupController.groupdb.get(children[i]).getPathReference().getBounds();
//				printBounds("Group " + children[i], bounds);
			}
			else
				continue;
			if (bounds.x < minX)
				minX = bounds.x;
			if (bounds.y < minY)
				minY = bounds.y;
			if (bounds.x + bounds.width > maxX)
				maxX = bounds.x + bounds.width;
			if (bounds.y + bounds.height > maxY)
				maxY = bounds.y + bounds.height;
		}
		
		return new Rectangle(minX, minY, maxX - minX, maxY - minY);
	}
	
	public Rectangle getBoundsOfObjects(Rectangle[] objects)
	{
		if (objects.length == 0)
			return new Rectangle(0,0,0,0);
		
		int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
		
		Rectangle bounds;
		// iterate through child strokes
		for (int i = 0; i < objects.length; i++) {
			bounds = objects[i];
			if (bounds.x < minX)
				minX = bounds.x;
			if (bounds.y < minY)
				minY = bounds.y;
			if (bounds.x + bounds.width > maxX)
				maxX = bounds.x + bounds.width;
			if (bounds.y + bounds.height > maxY)
				maxY = bounds.y + bounds.height;
		}
		
		return new Rectangle(minX, minY, maxX - minX, maxY - minY);
	}

	public Rectangle getBoundsOfContents()
	{
		ArrayList<Rectangle> boundsOfContainedElements = new ArrayList<Rectangle>();
		
		long[] strokearr = this.getChildStrokes();
		long[] grouparr = this.getChildGroups();		
		long[] childIndices = ArrayUtils.addAll(strokearr, grouparr);
		
		Rectangle childrenBounds = getBoundsOfObjects(childIndices);
		if (childrenBounds.width < 1 || childrenBounds.height < 1)
			childrenBounds = this.getPathReference().getBounds();
		boundsOfContainedElements.add(childrenBounds);
		
		Rectangle textDimensions = Geometry.getTextBounds(this.text);
		Rectangle textBounds;
		textBounds = new Rectangle(childrenBounds.x, childrenBounds.y, textDimensions.width + CalicoOptions.group.text_padding*2, textDimensions.height + CalicoOptions.group.text_padding*2);
		if (textBounds.width > 0 && textBounds.height > 0)
			boundsOfContainedElements.add(textBounds);
		
		return getBoundsOfObjects(boundsOfContainedElements.toArray(new Rectangle[] { }));
	}
	
	public void printBounds()
	{
		printBounds(new Long(uuid).toString(), getPathReference().getBounds());
	}
	
	private void printBounds(String id, Rectangle bounds)
	{
		System.out.println("\n\t\t" + id + ": (" + bounds.x + ", " + bounds.y + ", " + 
				bounds.width + ", " + bounds.height + ")");
	}
	
	public void shrinkToContents() {
		setShapeToRoundedRectangle(getBoundsOfContents());	
	}
	
	public void setShapeToRoundedRectangle(Rectangle newBounds) {
		setShapeToRoundedRectangle(newBounds, CalicoOptions.group.padding);
	}
	
	public void setShapeToRoundedRectangle(Rectangle newBounds, int padding) {
		if (newBounds.width == 0 && newBounds.height == 0)
			return;
		
		if (points != null)
			points.reset();
		points = Geometry.getRoundedPolygon(newBounds, padding);

		this.groupArea = calico.Geometry.computePolygonArea(this.points);
		GeneralPath bezieredPoly = Geometry.getBezieredPoly(points);
		setPathTo(bezieredPoly);
		
		scaleX = 1.0d;
		scaleY = 1.0d;
		rotation = 0.0d;
		
		//this.setPaintInvalid(true);
		CalicoDraw.setNodePaintInvalid(this, true);
	}

	// public void shrinkToConvexHull() {
	// // draw the convex hull of the particles in the system
	// if (a.length>1) {
	// int i;
	// // find highest particle
	// double miny=a[0].pos.y;
	// int mini = 0;
	// for (i=1;i maxcos) {
	// nxti = i;
	// maxcos = thiscos;
	//
	// }
	//
	// DLine(a[curi].pos.x,a[curi].pos.y,a[nxti].pos.x,a[nxti].pos.y);
	// if (a[nxti].pos.diff(a[curi].pos).norm() != 0.0) {
	// dir = a[nxti].pos.diff(a[curi].pos);
	// dir = dir.scale(1/dir.norm());
	// }
	// curi = nxti;
	// }
	// }

	public void shrinkToConvexHull() {
//		ArrayList<Point> points = new ArrayList<Point>();
//		int inflation = 10;
//
//		// ArrayList<Point> childPoints = new ArrayList<Point>();
//		int sumX = 0, sumY = 0, pointCount = 0;
//		long[] strokearr = this.getChildStrokes();
//		Polygon strokePoly;
//		for (int i = 0; i < strokearr.length; i++) {
//			strokePoly = CStrokeController.strokes.get(strokearr[i])
//					.getPolygon();
//			for (int j = 0; j < strokePoly.npoints; j++) {
//				points.add(new Point(strokePoly.xpoints[j],
//						strokePoly.ypoints[j]));
//				sumX += strokePoly.xpoints[j];
//				sumY += strokePoly.ypoints[j];
//				pointCount++;
//			}
//		}
//		Point midPoint = new Point(sumX / pointCount, sumY / pointCount);
//
//		ArrayList<Point> P1 = new ArrayList<Point>();
//		ArrayList<Point> P2 = new ArrayList<Point>();
//		Point l = points.get(0);
//		Point r = points.get(0);
//		int minX = l.x;
//		int maxX = l.x;
//		int minAt = 0;
//		int maxAt = 0;
//		Point currPt;
//
//		chkLns.clear();
//		tempLns.clear();
//		tempHull.clear();
//
//		// for (int i=0;i<coords.npoints;i++)
//		// points.add(new Point(coords.xpoints[i], coords.ypoints[i]));
//
//		/* find the max and min x-coord point */
//
//		for (int i = 1; i < points.size(); i++) {
//			currPt = points.get(i);
//			if (points.get(i).x > maxX) {
//				r = points.get(i);
//				maxX = (points.get(i)).x;
//				maxAt = i;
//			}
//			;
//
//			if (points.get(i).x < minX) {
//				l = points.get(i);
//				minX = points.get(i).x;
//				minAt = i;
//			}
//			;
//			// repaint();
//			// try { Thread.sleep(speed); } catch (InterruptedException e) {}
//
//		}
//
//		Line2D lr = new Line2D.Double(l, r);
//		tempLns.add(new Line2D.Double(points.get(maxAt), points.get(minAt)));
//		chkLns.add(new Line2D.Double(points.get(maxAt), points.get(minAt)));
//		// repaint();
//		// try { Thread.sleep(speed); } catch (InterruptedException e) {};
//
//		/*
//		 * find out each point is over or under the line formed by the two
//		 * points
//		 */
//		/*
//		 * with min and max x-coord, and put them in 2 group according to
//		 * whether
//		 */
//		/* they are above or under */
//		for (int i = 0; i < points.size(); i++) {
//			if ((i != maxAt) && (i != minAt)) {
//				currPt = points.get(i);
//
//				if (this.onLeft(lr, points.get(i))) {
//					P1.add(new Point((points.get(i)).x, (points.get(i)).y));
//				} else {
//					P2.add(new Point((points.get(i)).x, (points.get(i)).y));
//				}
//				// repaint();
//				// try { Thread.sleep(speed); } catch (InterruptedException e)
//				// {}
//			}
//
//		}
//		;
//
//		/* put the max and min x-cord points in each group */
//		P1.add(new Point(l.x, l.y));
//		P1.add(new Point(r.x, r.y));
//
//		P2.add(new Point(l.x, l.y));
//		P2.add(new Point(r.x, r.y));
//
//		/* calculate the upper hull */
//		quick(P1, l, r, 0);
//
//		points.reset();
//		/* put the upper hull result in final result */
//		Point p;
//		for (int k = 0; k < tempHull.size(); k++) {
//			p = getInflated(getPoint(tempHull.get(k).getP1()), midPoint,
//					inflation);
//			points.addPoint(p.x, p.y);
//			p = getInflated(getPoint(tempHull.get(k).getP2()), midPoint,
//					inflation);
//			points.addPoint(p.x, p.y);
//			// hull.add(new Line2D.Double((tempHull.get(k)).getP1(),
//			// (tempHull.get(k)).getP2()));
//		}
//		chkLns.clear();
//		tempLns.clear();
//		tempHull.clear();
//
//		/* calculate the lower hull */
//		quick(P2, l, r, 1);
//
//		/* append the result from lower hull to final result */
//		for (int k = tempHull.size() - 1; k >= 0; k--) {
//			p = getInflated(getPoint(tempHull.get(k).getP1()), midPoint,
//					inflation);
//			points.addPoint(p.x, p.y);
//			p = getInflated(getPoint(tempHull.get(k).getP2()), midPoint,
//					inflation);
//			points.addPoint(p.x, p.y);
//			// hull.add(new Line2D.Double((tempHull.get(k)).getP1(),
//			// (tempHull.get(k)).getP2()));
//		}
//		points.addPoint(points.xpoints[0], points.ypoints[0]);
//
//		this.groupArea = PolygonUtils.PolygonArea(points);
//		redrawAll();

	}
	
//	public void setPolygon(Polygon p)
//	{
//
////		coords = p;
//		this.groupArea = PolygonUtils.PolygonArea(this.coords);
//		
////		coordsOriginal = new Polygon(p.xpoints, p.ypoints, p.npoints);
////		groupTransforms.clear();
////		redrawAll();
//	}

	private Point getInflated(Point p, Point midPoint, int inflateAmount) {
		int deltaX = 0, deltaY = 0;
		// quadrant I
		if (p.x > midPoint.x && p.y > midPoint.y) {
			deltaX = inflateAmount * 1;
			deltaY = inflateAmount * 1;
		}
		// quadrant II
		else if (p.x < midPoint.x && p.y > midPoint.y) {
			deltaX = inflateAmount * -1;
			deltaY = inflateAmount * 1;
		}
		// quadrant III
		else if (p.x < midPoint.x && p.y < midPoint.y) {
			deltaX = inflateAmount * -1;
			deltaY = inflateAmount * -1;
		}
		// quadrant IV
		else if (p.x > midPoint.x && p.y < midPoint.y) {
			deltaX = inflateAmount * 1;
			deltaY = inflateAmount * -1;
		}

		return new Point(p.x + deltaX, p.y + deltaY);
	}

	/**
	 * Recursive method to find out the Hull. faceDir is 0 if we are calculating
	 * the upper hull. faceDir is 1 if we are calculating the lower hull.
	 */
	public synchronized void quick(ArrayList<Point> P, Point l, Point r,
			int faceDir) {
		Point currPt;

		if (P.size() == 2) {
			tempHull.add(new Line2D.Double(P.get(0), P.get(1)));
			return;
		} else {
			int hAt = splitAt(P, l, r);
			Line2D lh = new Line2D.Double(l, P.get(hAt));
			Line2D hr = new Line2D.Double(P.get(hAt), r);
			ArrayList<Point> P1 = new ArrayList<Point>();
			ArrayList<Point> P2 = new ArrayList<Point>();

			for (int i = 0; i < (P.size() - 2); i++) {
				if (i != hAt) {
					currPt = P.get(i);
					if (faceDir == 0) {
						if (this.onLeft(lh, P.get(i))) {
							P1.add(new Point(P.get(i).x, P.get(i).y));
						}

						if ((this.onLeft(hr, P.get(i)))) {
							P2.add(new Point(P.get(i).x, P.get(i).y));
						}
					} else {
						if (!(onLeft(lh, P.get(i)))) {
							P1.add(new Point((P.get(i)).x, P.get(i).y));
						}
						;

						if (!(this.onLeft(hr, P.get(i)))) {
							P2.add(new Point(P.get(i).x, P.get(i).y));
						}
						;
					}
					;
				}
			}

			P1.add(new Point(l.x, l.y));
			P1.add(new Point(P.get(hAt).x, P.get(hAt).y));

			P2.add(new Point((P.get(hAt)).x, (P.get(hAt)).y));
			P2.add(new Point(r.x, r.y));

			Point h = new Point(P.get(hAt).x, P.get(hAt).y);

			tempLns.add(new Line2D.Double(l, h));
			tempLns.add(new Line2D.Double(h, r));

			if (faceDir == 0) {
				quick(P1, l, h, 0);
				quick(P2, h, r, 0);
			} else {
				quick(P1, l, h, 1);
				quick(P2, h, r, 1);
			}
			return;
		}
	}

	private Point getPoint(Point2D p) {

		return new Point((int) p.getX(), (int) p.getY());
	}

	/**
	 * Given a Check point and determine if this check point is lying on the
	 * left side or right side of the first point of the line.
	 */
	private boolean onLeft(Line2D line, Point chkpt) {
		Point point1 = getPoint(line.getP1());
		Point point2 = getPoint(line.getP2());
		boolean slopeUndefine = false;
		float slope = 0f;

		if (point1.x == point2.x)
			slopeUndefine = true;
		else {
			if (point2.y == point1.y)
				slope = (float) 0;
			else
				slope = (float) (point2.y - point1.y) / (point2.x - point1.x);
			slopeUndefine = false;
		}

		if (slopeUndefine) {
			if (chkpt.x < point1.x)
				return true;
			else {
				if (chkpt.x == point1.x) {
					if (((chkpt.y > point1.y) && (chkpt.y < point2.y))
							|| ((chkpt.y > point2.y) && (chkpt.y < point1.y)))
						return true;
					else
						return false;
				} else
					return false;
			}
		} else {
			/* multiply the result to avoid the rounding error */
			int x3 = (int) (((chkpt.x + slope
					* (slope * point1.x - point1.y + chkpt.y)) / (1 + slope
					* slope)) * 10000);
			int y3 = (int) ((slope * (x3 / 10000 - point1.x) + point1.y) * 10000);

			if (slope == (float) 0) {
				if ((chkpt.y * 10000) > y3)
					return true;
				else
					return false;
			} else {
				if (slope > (float) 0) {
					if (x3 > (chkpt.x * 10000))
						return true;
					else
						return false;
				} else {
					if ((chkpt.x * 10000) > x3)
						return true;
					else
						return false;
				}
			}
		}
	}

	/**
	 * Find out a point which is in the Hull for sure among a group of points
	 * Since all the point are on the same side of the line formed by l and r,
	 * so the point with the longest distance perpendicular to this line is the
	 * point we are lokking for. Return the index of this point in the Vector/
	 */
	private synchronized int splitAt(ArrayList<Point> P, Point l, Point r) {
		double maxDist = 0;

		int x3 = 0, y3 = 0;
		double distance = 0;
		int farPt = 0;

		boolean slopeUndefine = false;
		float slope = 0f;

		if (l == r)
			slopeUndefine = true;
		else {
			if (r.y == l.y)
				slope = (float) 0;
			else
				slope = (float) (r.y - l.y) / (r.x - l.x);
			slopeUndefine = false;
		}

		for (int i = 0; i < (P.size() - 2); i++) {
			if (slopeUndefine) {
				x3 = l.x;
				y3 = (P.get(i)).y;
			} else {
				if (r.y == l.y) {
					x3 = (P.get(i)).x;
					y3 = l.y;
				} else {
					x3 = (int) ((((P.get(i)).x + slope
							* (slope * l.x - l.y + (P.get(i)).y)) / (1 + slope
							* slope)));
					y3 = (int) ((slope * (x3 - l.x) + l.y));
				}
			}
			int x1 = (P.get(i)).x;
			int y1 = (P.get(i)).y;
			distance = Math.sqrt(Math.pow((y1 - y3), 2)
					+ Math.pow((x1 - x3), 2));

			if (distance > maxDist) {
				maxDist = distance;
				farPt = i;
			}
		}
		return farPt;
	}

	public void rotate(double radians) {
		rotate(radians, Geometry.getMidPoint2D(points));
	}

	/**
	 * Rotating about a pivot is somewhat tricky if we want the rotation to be applicable recursively to children.
	 * In order to accomplish that, we perform the following operation:
	 * 	1) Compute the translation of the new mid point after pivoting around the given point.
	 *  2) Compute the actual change in rotation (locally) after this group rotates about the pivot.
	 * 
	 * @param radians
	 * @param pivotPoint
	 */
	public void rotate(double radians, Point2D pivotPoint) {
		System.out.println("Performing rotate on (" + this.uuid + ") at point (" + pivotPoint.getX() + ", " + pivotPoint.getY() + " with scale  " + rotation);
		
		AffineTransform rotateAboutPivot = AffineTransform.getRotateInstance(radians, pivotPoint.getX(), pivotPoint.getY());
		
		//1) compute mid point translation
		Point2D oldMidPoint = Geometry.getMidPoint2D(points);
		Point2D newMidPoint = null;
		newMidPoint = rotateAboutPivot.transform(oldMidPoint, newMidPoint);
		int deltaX = (int)(newMidPoint.getX() - oldMidPoint.getX());
		int deltaY = (int)(newMidPoint.getY() - oldMidPoint.getY());
		points.translate(deltaX, deltaY);
//		smoothedPath.transform(AffineTransform.getTranslateInstance(deltaX, deltaY));
		
		//2) compute actual rotation change
		Rectangle2D bounds = points.getBounds2D();
		Point2D oldBoundsRightCorner = new Point2D.Double(bounds.getX() + bounds.getWidth(), bounds.getY());
		Point2D newBoundsRightCorner = null;
		newBoundsRightCorner = rotateAboutPivot.transform(oldBoundsRightCorner, newBoundsRightCorner);
		
		double angleOld = calico.utils.Geometry.angle(oldMidPoint, oldBoundsRightCorner);
		double angleNew = calico.utils.Geometry.angle(newMidPoint, newBoundsRightCorner);
		double actualRotation = angleNew - angleOld;
		primative_rotate(actualRotation + rotation);
		
		if (childArrows.size() > 0) {
			long[] auid = childArrows.toLongArray();
			Point2D ptSrc = new Point2D.Double();
			Point2D ptDst = new Point2D.Double();
			for (int i = 0; i < auid.length; i++) {
				ptSrc = new Point(CArrowController.arrows.get(auid[i]).getAnchor(uuid).getPoint());
				rotateAboutPivot.transform(ptSrc, ptDst);
				CArrowController.no_notify_move_group_anchor(auid[i], uuid, Math.round((float)ptDst.getX() - (float)ptSrc.getX()),
						Math.round((float)ptDst.getY() - (float)ptSrc.getY()));
			}
		}
		
		if (childConnectors.size() > 0) {
			long[] cuid = childConnectors.toLongArray();
			Point2D ptSrc = new Point2D.Double();
			Point2D ptDst = new Point2D.Double();
			for (int i = 0; i < cuid.length; i++) {
				if (CConnectorController.connectors.get(cuid[i]).getAnchorUUID(CConnector.TYPE_HEAD) == uuid)
				{
					ptSrc = new Point(CConnectorController.connectors.get(cuid[i]).getHead());
					rotateAboutPivot.transform(ptSrc, ptDst);
					CConnectorController.no_notify_move_group_anchor(cuid[i], CConnector.TYPE_HEAD, Math.round((float)ptDst.getX() - (float)ptSrc.getX()),
							Math.round((float)ptDst.getY() - (float)ptSrc.getY()));
				}
				if (CConnectorController.connectors.get(cuid[i]).getAnchorUUID(CConnector.TYPE_TAIL) == uuid)
				{
					ptSrc = new Point(CConnectorController.connectors.get(cuid[i]).getTail());
					rotateAboutPivot.transform(ptSrc, ptDst);
					CConnectorController.no_notify_move_group_anchor(cuid[i], CConnector.TYPE_TAIL, Math.round((float)ptDst.getX() - (float)ptSrc.getX()),
							Math.round((float)ptDst.getY() - (float)ptSrc.getY()));
				}
			}
		}
		
		for (long g : childGroups)
		{
			if (CGroupController.is_parented_to(g, this.uuid))
				CGroupController.groupdb.get(g).rotate(radians, pivotPoint);
			else {
				Calico.logger.error("GROUP " + this.uuid
						+ " is trying to translate kidnapped group " + g);
				CGroupController.no_notify_delete_child_group(this.uuid, g);
			}
		}
		
		for (long s : childStrokes)
		{
			if (CStrokeController.strokes.containsKey(s))
			{
				if (CStrokeController.is_parented_to(s, this.uuid))
					CStrokeController.strokes.get(s).rotate(radians, pivotPoint);
				else {
					Calico.logger.error("GROUP " + this.uuid
							+ " is trying to trasnlate kidnapped stroke " + s);
					CGroupController.no_notify_delete_child_stroke(this.uuid,s);
				}
			}
		}
		
		recomputeBounds();
	}

	public void primative_rotate(double actualRotation) {
		rotation = actualRotation;
		applyAffineTransform();
	}
	
	public void scale(double scaleX, double scaleY)
	{
		scale(scaleX,scaleY, Geometry.getMidPoint2D(points));
	}
	
	public void scale(double scaleX, double scaleY, Point2D pivotPoint)
	{
		System.out.println("Performing scale on (" + this.uuid + ") at point (" + pivotPoint.getX() + ", " + pivotPoint.getY() + " with scale " + scaleX);
		
		AffineTransform scaleAboutPivot1 = AffineTransform.getTranslateInstance(-1 * pivotPoint.getX(), -1 * pivotPoint.getY());
		AffineTransform scaleAboutPivot2 = AffineTransform.getScaleInstance(scaleX, scaleY);
		AffineTransform scaleAboutPivot3 = AffineTransform.getTranslateInstance(pivotPoint.getX(), pivotPoint.getY());
		
		//1) compute mid point translation
		Point2D oldMidPoint = Geometry.getMidPoint2D(points);
		Point2D newMidPoint = null;
		newMidPoint = scaleAboutPivot1.transform(oldMidPoint, newMidPoint);
		newMidPoint = scaleAboutPivot2.transform(newMidPoint, null);
		newMidPoint = scaleAboutPivot3.transform(newMidPoint, null);
		int deltaX = new java.lang.Double(newMidPoint.getX() - oldMidPoint.getX()).intValue();
		int deltaY = new java.lang.Double(newMidPoint.getY() - oldMidPoint.getY()).intValue();
		points.translate(deltaX, deltaY);
//		smoothedPath.transform(AffineTransform.getTranslateInstance(deltaX, deltaY));
		
		//2) assign actual scale
		primative_scale(this.scaleX * scaleX, this.scaleY * scaleY);
		
		if (childArrows.size() > 0) {
			long[] auid = childArrows.toLongArray();
			Point2D ptSrc = new Point2D.Double();
			Point2D ptDst = new Point2D.Double();
			for (int i = 0; i < auid.length; i++) {
				ptSrc = new Point(CArrowController.arrows.get(auid[i]).getAnchor(uuid).getPoint());
				ptDst = scaleAboutPivot1.transform(ptSrc, ptDst);
				ptDst = scaleAboutPivot2.transform(ptDst, null);
				ptDst = scaleAboutPivot3.transform(ptDst, null);
				CArrowController.no_notify_move_group_anchor(auid[i], uuid, Math.round((float)ptDst.getX() - (float)ptSrc.getX()),
						Math.round((float)ptDst.getY() - (float)ptSrc.getY()));
			}
		}
		
		if (childConnectors.size() > 0) {
			long[] cuid = childConnectors.toLongArray();
			Point2D ptSrc = new Point2D.Double();
			Point2D ptDst = new Point2D.Double();
			for (int i = 0; i < cuid.length; i++) {
				if (CConnectorController.connectors.get(cuid[i]).getAnchorUUID(CConnector.TYPE_HEAD) == uuid)
				{
					ptSrc = new Point(CConnectorController.connectors.get(cuid[i]).getHead());
					ptDst = scaleAboutPivot1.transform(ptSrc, ptDst);
					ptDst = scaleAboutPivot2.transform(ptDst, null);
					ptDst = scaleAboutPivot3.transform(ptDst, null);
					CConnectorController.no_notify_move_group_anchor(cuid[i], CConnector.TYPE_HEAD, Math.round((float)ptDst.getX() - (float)ptSrc.getX()),
							Math.round((float)ptDst.getY() - (float)ptSrc.getY()));
				}
				if (CConnectorController.connectors.get(cuid[i]).getAnchorUUID(CConnector.TYPE_TAIL) == uuid)
				{
					ptSrc = new Point(CConnectorController.connectors.get(cuid[i]).getTail());
					ptDst = scaleAboutPivot1.transform(ptSrc, ptDst);
					ptDst = scaleAboutPivot2.transform(ptDst, null);
					ptDst = scaleAboutPivot3.transform(ptDst, null);
					CConnectorController.no_notify_move_group_anchor(cuid[i], CConnector.TYPE_TAIL, Math.round((float)ptDst.getX() - (float)ptSrc.getX()),
							Math.round((float)ptDst.getY() - (float)ptSrc.getY()));
				}
			}
		}
		
		for (long g : childGroups)
		{
			if (CGroupController.is_parented_to(g, this.uuid))
				CGroupController.groupdb.get(g).scale(scaleX, scaleY, pivotPoint);
			else {
				logger.error("GROUP " + this.uuid
						+ " is trying to translate kidnapped group " + g);
				CGroupController.no_notify_delete_child_group(this.uuid, g);
			}
		}
		
		for (long s : childStrokes)
		{
			if (CStrokeController.strokes.containsKey(s))
			{
				if (CStrokeController.is_parented_to(s, this.uuid))
					CStrokeController.strokes.get(s).scale(scaleX, scaleY, pivotPoint);
				else {
					logger.error("GROUP " + this.uuid
							+ " is trying to trasnlate kidnapped stroke " + s);
					CGroupController.no_notify_delete_child_stroke(this.uuid,s);
				}
			}
		}
		
		recomputeBounds();
	}

	public void primative_scale(double scaleX, double scaleY) {
		this.scaleX = scaleX;
		this.scaleY = scaleY;
		applyAffineTransform();
	}
	
	private void applyAffineTransform()
	{
		Rectangle oldBounds = getBounds().getBounds();
		
		PAffineTransform piccoloTextTransform = getPTransform();
		GeneralPath p = (GeneralPath) Geometry.getBezieredPoly(points).createTransformedShape(piccoloTextTransform);
		this.setPathTo(p);
		if (p.getBounds().width == 0 || p.getBounds().height == 0)
		{
			this.setBounds(new java.awt.geom.Rectangle2D.Double(p.getBounds2D().getX(), p.getBounds2D().getY(), 1d, 1d));
			//CalicoDraw.setNodeBounds(this, new java.awt.geom.Rectangle2D.Double(p.getBounds2D().getX(), p.getBounds2D().getY(), 1d, 1d));
		}
		else
		{
//			this.setBounds(p.getBounds());
		}
		this.groupArea = calico.Geometry.computePolygonArea(Geometry.getPolyFromPath(p.getPathIterator(null)));
		
//		CCanvasController.canvasdb.get(cuid).getCamera().validateFullPaint();
		/*a*/
		//CCanvasController.canvasdb.get(cuid).getCamera().repaintFrom(new PBounds(Geometry.getCombinedBounds(new Rectangle[] {oldBounds, this.getBounds().getBounds()})), this);
		//this.repaint();
		if (CCanvasController.getCurrentUUID() == getCanvasUID())
			CalicoDraw.repaintNode(this);
	}
	
	public PAffineTransform getPTransform() {
		PAffineTransform piccoloTextTransform = new PAffineTransform();
		Point2D midPoint = Geometry.getMidPoint2D(points);
		piccoloTextTransform.rotate(rotation, midPoint.getX(), midPoint.getY());
		piccoloTextTransform.scaleAboutPoint(scaleX, midPoint.getX(), midPoint.getY());
		return piccoloTextTransform;
	}
	
	public Point2D getMidPoint()
	{
		return Geometry.getMidPoint2D(points);
	}
	
	public Image getFamilyPicture()
	{
        final PBounds b = getFullBoundsReference();
        int width = (int) Math.ceil(b.getWidth());
        int height = (int) Math.ceil(b.getHeight());
        
        BufferedImage result;

        if (GraphicsEnvironment.isHeadless()) {
            result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }
        else {
            final GraphicsConfiguration graphicsConfiguration = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDefaultConfiguration();
            result = graphicsConfiguration.createCompatibleImage(width, height, Transparency.TRANSLUCENT);
        }
        
        return getFamilyPicture(result, null, FILL_STRATEGY_ASPECT_FIT);
        
	}
	
    public Image getFamilyPicture(final BufferedImage image, final Paint backGroundPaint, final int fillStrategy) {
        final int imageWidth = image.getWidth();
        final int imageHeight = image.getHeight();
        final Graphics2D g2 = image.createGraphics();

        if (backGroundPaint != null) {
            g2.setPaint(backGroundPaint);
            g2.fillRect(0, 0, imageWidth, imageHeight);
        }
        g2.setClip(0, 0, imageWidth, imageHeight);

        final PBounds nodeBounds = getFullBounds();
        nodeBounds.expandNearestIntegerDimensions();

        final double nodeWidth = nodeBounds.getWidth();
        final double nodeHeight = nodeBounds.getHeight();

        double imageRatio = imageWidth / (imageHeight * 1.0);
        double nodeRatio = nodeWidth / nodeHeight;
        double scale;
        switch (fillStrategy) {
            case FILL_STRATEGY_ASPECT_FIT:
                // scale the graphics so node's full bounds fit in the imageable
                // bounds but aspect ration is retained

                if (nodeRatio <= imageRatio) {
                    scale = image.getHeight() / nodeHeight;
                }
                else {
                    scale = image.getWidth() / nodeWidth;
                }
                g2.scale(scale, scale);
                g2.translate(-nodeBounds.x, -nodeBounds.y);
                break;
            case FILL_STRATEGY_ASPECT_COVER:
                // scale the graphics so node completely covers the imageable
                // area, but retains its aspect ratio.
                if (nodeRatio <= imageRatio) {
                    scale = image.getWidth() / nodeWidth;
                }
                else {
                    scale = image.getHeight() / nodeHeight;
                }
                g2.scale(scale, scale);
                break;
            case FILL_STRATEGY_EXACT_FIT:
                // scale the node so that it covers then entire image,
                // distorting it if necessary.
                g2.scale(image.getWidth() / nodeWidth, image.getHeight() / nodeHeight);
                g2.translate(-nodeBounds.x, -nodeBounds.y);
                break;
            default:
                throw new IllegalArgumentException("Fill strategy provided is invalid");
        }

        final PPaintContext pc = new PPaintContext(g2);
        pc.setRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING);
        fullPaintWithChildren(pc);
        return image;
    }
    
    private void fullPaintWithChildren(final PPaintContext paintContext) {
    	
        if (getVisible() && fullIntersects(paintContext.getLocalClip())) {
            paintContext.pushTransform(getTransform());
            paintContext.pushTransparency(getTransparency());

            if (!getOccluded()) {
                paint(paintContext);
            }

            for (long s : childStrokes)
            {
            	if (CStrokeController.strokes.get(s) != null)
            		CStrokeController.strokes.get(s).fullPaint(paintContext);
            }
            for (long g : childGroups)
            {
            	if (CGroupController.groupdb.get(g) != null)
            		CGroupController.groupdb.get(g).fullPaintWithChildren(paintContext);
            }
//            final int count = getChildrenCount();
//            for (int i = 0; i < count; i++) {
//                final PNode each = (PNode) children.get(i);
//                each.fullPaint(paintContext);
//            }

            paintAfterChildren(paintContext);

            paintContext.popTransparency(getTransparency());
            paintContext.popTransform(getTransform());
        }
    }
    
    public CalicoPacket[] getUpdatePackets(long uuid, long cuid, long puid, int dx, int dy, boolean captureChildren) {

		int packetSize = ByteUtils.SIZE_OF_INT
				+ (3 * ByteUtils.SIZE_OF_LONG) + ByteUtils.SIZE_OF_BYTE
				+ ByteUtils.SIZE_OF_SHORT
				+ (2 * this.points.npoints * ByteUtils.SIZE_OF_SHORT)
				+ CalicoPacket.getSizeOfString(this.text);

		CalicoPacket packet = new CalicoPacket(packetSize);
		// UUID CUID PUID <COLOR> <NUMCOORDS> x1 y1
		packet.putInt(networkLoadCommand);
		packet.putLong(uuid);
		packet.putLong(cuid);
		packet.putLong(puid);
		packet.putBoolean(this.isPermanent);
		packet.putCharInt(this.points.npoints);
		for (int j = 0; j < this.points.npoints; j++) {
			packet.putInt(this.points.xpoints[j] + dx);
			packet.putInt(this.points.ypoints[j] + dy);
		}
		packet.putBoolean(captureChildren);
		packet.putDouble(this.rotation);
		packet.putDouble(this.scaleX);
		packet.putDouble(this.scaleY);
		packet.putString(this.text);

		return new CalicoPacket[] {packet};
	}
	
	public CalicoPacket[] getUpdatePackets(boolean captureChildren)
	{
		return getUpdatePackets(this.uuid, this.cuid, this.puid, 0, 0, captureChildren);
	}
    
//	public CalicoPacket[] getUpdatePackets(boolean captureChildren) {
//		
//		if (CalicoOptions.network.cluster_size >= this.points.npoints) {
//			// WE CAN SEND A STROKE_LOAD SINGLE PACKET
//
//			int packetSize = ByteUtils.SIZE_OF_INT
//					+ (3 * ByteUtils.SIZE_OF_LONG) + ByteUtils.SIZE_OF_BYTE
//					+ ByteUtils.SIZE_OF_SHORT
//					+ (2 * this.points.npoints * ByteUtils.SIZE_OF_SHORT);
//
//			CalicoPacket packet = new CalicoPacket(packetSize);
//			// UUID CUID PUID <COLOR> <NUMCOORDS> x1 y1
//			packet.putInt(networkLoadCommand);
//			packet.putLong(this.uuid);
//			packet.putLong(this.cuid);
//			packet.putLong(this.puid);
//			packet.putBoolean(this.isPermanent);
//			packet.putCharInt(this.points.npoints);
//			for (int j = 0; j < this.points.npoints; j++) {
//				packet.putInt(this.points.xpoints[j]);
//				packet.putInt(this.points.ypoints[j]);
//			}
//			packet.putBoolean(captureChildren);
//			packet.putDouble(this.rotation);
//			packet.putDouble(this.scaleX);
//			packet.putDouble(this.scaleY);
//
//			// TODO: FIX THIS
//			if (!(getText().length() > 0))
//				return new CalicoPacket[] { packet };
//			else
//				return new CalicoPacket[] {
//						packet,
//						CalicoPacket.getPacket(NetworkCommand.GROUP_SET_TEXT,
//								this.uuid, this.text) };
//		} else {
//
//			Polygon pointtemp = CalicoUtils.clonePolygon(this.points);
//
//			int numPackets = 5;
//			if (textSet)
//				numPackets++;
//
//			for (int i = 0; i < pointtemp.npoints; i = i
//					+ CalicoOptions.network.cluster_size) {
//				numPackets++;
//			}
//
//			CalicoPacket[] packets = new CalicoPacket[numPackets];
//
//			packets[0] = CalicoPacket
//					.getPacket(NetworkCommand.GROUP_START, this.uuid,
//							this.cuid, this.puid, (this.isPermanent ? 1 : 0));
//			int packetIndex = 1;
//
//			for (int i = 0; i < pointtemp.npoints; i = i
//					+ CalicoOptions.network.cluster_size) {
//				int numingroup = (i + CalicoOptions.network.cluster_size) > pointtemp.npoints ? (pointtemp.npoints - i)
//						: CalicoOptions.network.cluster_size;
//
//				packets[packetIndex] = new CalicoPacket(
//						(2 * numingroup * ByteUtils.SIZE_OF_CHAR)
//								+ ByteUtils.SIZE_OF_INT
//								+ ByteUtils.SIZE_OF_CHAR
//								+ ByteUtils.SIZE_OF_LONG);
//				packets[packetIndex]
//						.putInt(NetworkCommand.GROUP_APPEND_CLUSTER);
//				packets[packetIndex].putLong(this.uuid);
//				packets[packetIndex].putCharInt(numingroup);
//
//				for (int j = 0; j < numingroup; j++) {
//					packets[packetIndex].putInt(pointtemp.xpoints[i + j]);
//					packets[packetIndex].putInt(pointtemp.ypoints[i + j]);
//				}
//				packetIndex++;
//			}
//
//			packets[packetIndex++] = CalicoPacket.getPacket(
//					NetworkCommand.GROUP_FINISH, this.uuid, captureChildren);
//			packets[packetIndex++] = CalicoPacket.getPacket(
//					NetworkCommand.GROUP_ROTATE, this.uuid, this.rotation);
//			packets[packetIndex++] = CalicoPacket.getPacket(
//					NetworkCommand.GROUP_SCALE, this.uuid, this.scaleX, this.scaleY);
//			packets[packetIndex] = CalicoPacket.getPacket(
//						NetworkCommand.GROUP_SET_TEXT, this.uuid, this.text);
//
//			return packets;
//		}
//	}
	
	public CalicoPacket[] getParentingUpdatePackets() {
		CalicoPacket[] packets = new CalicoPacket[3];

		// SET PARENT
//		packets[0] = CalicoPacket.getPacket(NetworkCommand.GROUP_SET_PARENT,
//				this.uuid, this.puid);

		int basePacketSize = ByteUtils.SIZE_OF_INT + ByteUtils.SIZE_OF_LONG
				+ ByteUtils.SIZE_OF_SHORT;

		// / Child
		long[] bgelist = getChildStrokes();

		packets[0] = new CalicoPacket(basePacketSize
				+ (bgelist.length * ByteUtils.SIZE_OF_LONG));
		packets[0].putInt(NetworkCommand.GROUP_SET_CHILD_STROKES);
		packets[0].putLong(this.uuid);
		packets[0].putCharInt(bgelist.length);
		if (bgelist.length > 0) {
			for (int i = 0; i < bgelist.length; i++) {
				packets[0].putLong(bgelist[i]);
			}
		}

		long[] grplist = getChildGroups();
		packets[1] = new CalicoPacket(basePacketSize
				+ (grplist.length * ByteUtils.SIZE_OF_LONG));
		packets[1].putInt(NetworkCommand.GROUP_SET_CHILD_GROUPS);
		packets[1].putLong(this.uuid);
		packets[1].putCharInt(grplist.length);
		if (grplist.length > 0) {
			for (int i = 0; i < grplist.length; i++) {
				packets[1].putLong(grplist[i]);
			}
		}
		// end child

		long[] arlist = getChildArrows();
		packets[2] = new CalicoPacket(basePacketSize
				+ (arlist.length * ByteUtils.SIZE_OF_LONG));
		packets[2].putInt(NetworkCommand.GROUP_SET_CHILD_ARROWS);
		packets[2].putLong(this.uuid);
		packets[2].putCharInt(arlist.length);
		if (arlist.length > 0) {
			for (int i = 0; i < arlist.length; i++) {
				packets[2].putLong(arlist[i]);
			}
		}
		
		ArrayList<CalicoPacket> totalPackets = new ArrayList<CalicoPacket>(Arrays.asList(packets));
//		for (int i = 0; i < bgelist.length; i++)
//			totalPackets.add(CalicoPacket.getPacket(NetworkCommand.STROKE_SET_PARENT, bgelist[i], this.uuid));
//		for (int i = 0; i < grplist.length; i++)
//			totalPackets.add(CalicoPacket.getPacket(NetworkCommand.GROUP_SET_PARENT, grplist[i], this.uuid));

		return totalPackets.toArray(new CalicoPacket[0]);
	}
	
	public void unparentAllChildren()
	{
		for (int i = getChildGroups().length-1; i >= 0; i--)
			CGroupController.no_notify_set_parent(getChildGroups()[i], 0l);
		for (int i = getChildStrokes().length-1; i >= 0; i--)
			CStrokeController.no_notify_set_parent(getChildStrokes()[i], 0l);
		for (long childArrow : getChildArrows())
		{
			CArrow tempArrow = CArrowController.arrows.get(childArrow);
			if (tempArrow.getAnchorA().getUUID() == this.uuid)
				tempArrow.setAnchorA(new AnchorPoint(CArrow.TYPE_CANVAS, tempArrow.getAnchorA().getPoint(), this.uuid));
			if (tempArrow.getAnchorB().getUUID() == this.uuid)
				tempArrow.setAnchorB(new AnchorPoint(CArrow.TYPE_CANVAS, tempArrow.getAnchorB().getPoint(), this.uuid));
		}
		for (long childConnector : getChildConnectors())
		{
			CConnector tempConnector = CConnectorController.connectors.get(childConnector);
			if (tempConnector.getAnchorUUID(CConnector.TYPE_HEAD) == this.uuid)
				tempConnector.setAnchorUUID(0l, CConnector.TYPE_HEAD);
			if (tempConnector.getAnchorUUID(CConnector.TYPE_TAIL) == this.uuid)
				tempConnector.setAnchorUUID(0l, CConnector.TYPE_TAIL);
		}
		
		childGroups.clear();
		childStrokes.clear();
		childArrows.clear();
		childConnectors.clear();
	}
	
	public long calculateParent(int x, int y) {
		long smallestGUID = 0L;
		double smallestGroupArea = java.lang.Double.MAX_VALUE;

		// Now, we check all other groups.
		long[] grouparr = CCanvasController.canvasdb.get(cuid).getChildGroups();
		if (grouparr.length > 0) {
			for (int i = 0; i < grouparr.length; i++) {
				if (!CGroupController.exists(grouparr[i]) || !CGroupController.groupdb.get(grouparr[i]).isPermanent() || !this.isPermanent())
					continue;
				if (grouparr[i] != this.uuid
						&& smallestGroupArea > CGroupController.groupdb.get(grouparr[i]).getArea()
						&& CGroupController.canParentChild(grouparr[i], this.uuid, x, y)) 
				{
					smallestGroupArea = CGroupController.groupdb.get(grouparr[i]).getArea();
					smallestGUID = grouparr[i];
				}
			}
		}
		return smallestGUID;
	}
	
	/**
	 * @param potentialParent
	 * @param child
	 * @return
	 */
	public boolean canParentChild(long child, int x, int y)
	{
		//A child cannot be parented to nothing or itself
		if (child == 0l || child == this.uuid)
			return false;
		
		//The child cannot be already be parented to a decorator because
		//	this causes crazy things to happen with things like the list.
		if (CGroupController.exists(child)
				&& CGroupController.groupdb.get(child).getParentUUID() != 0l
				&& CGroupController.groupdb.get(CGroupController.getDecoratorParent(
						CGroupController.groupdb.get(child).getParentUUID()))
					instanceof CGroupDecorator)
			return false;
		
		//The parent must exist, and cannot be parented to a decorator
		if (CGroupController.exists(getParentUUID())
				&& CGroupController.groupdb.get(getParentUUID()) instanceof CGroupDecorator)
			return false;
		
		if (CStrokeController.strokes.containsKey(child))
		{
			 if (!CGroupController.group_contains_stroke(this.uuid, child))
				 return false;
		}
		else if (CGroupController.groupdb.containsKey(child))
		{
			CGroup childGroup = CGroupController.groupdb.get(child);
			//the area of the child must be smaller than the area of the parent
			 if (getArea() < childGroup.getArea())
				 return false;
			 //the parent must completely contain the child
			 if (!CGroupController.group_contains_group(this.uuid, childGroup.uuid))
				 return false;		 
			 //The child should not be a temp scrap
			 if (!childGroup.isPermanent())
				 return false;
		}
		
		
		//We don't want to parent something if we can parent its ancestor. I.e., if 
		//	we select a scrap, we don't want to select all of the content on top of 
		//	that scrap too
		if (CGroupController.group_can_parent_ancestor(this.uuid, child,x,y))
			return false;
			
		return true;
	}

	public void recheckParentAfterMove() {
		recheckParentAfterMove((int)getMidPoint().getX(), (int)getMidPoint().getY());
	}

	public void recheckParentAfterMove(int x, int y) {
		// This checks to make sure we havent moved the group In
		long oldParentUUID = getParentUUID();
		
		//decorators will remove children manually
		if (CGroupController.groupdb.get(oldParentUUID) instanceof CGroupDecorator)
			return;
		
		long smallestGUID = calculateParent(x, y);
		
		if (oldParentUUID == smallestGUID
			|| smallestGUID == 0l)
			return;

		logger.trace("GROUP " + this.uuid + " WILL PARENT TO " + smallestGUID);

		if (oldParentUUID != 0L) {
			CGroupController.no_notify_remove_child_group(oldParentUUID,
					this.uuid);
		}
		
		CGroupController.no_notify_remove_child_group(oldParentUUID, this.uuid);
			
		CGroupController.no_notify_add_child_group(smallestGUID, this.uuid, x, y);

		CGroupController.no_notify_set_parent(this.uuid, smallestGUID);
	}
	
	public void moveGroupInFrontOf(PNode node)
	{
		super.moveInFrontOf(node);
	}
	
	@Override
	public void moveInFrontOf(PNode node)
	{
		if (node != null)
			//super.moveInFrontOf(node);
			CalicoDraw.moveGroupInFrontOf(this, node);
			
		
		moveInFrontOfInternal();
		
		long[] cgroups = this.childGroups.toLongArray();
		
		for (int i = 0; i < childGroups.size(); i++)
		{
			if (CGroupController.exists(cgroups[i]))
			{
				CGroupController.groupdb.get(cgroups[i]).moveInFrontOf(this);
			}
		}
		
		long[] cstrokes = this.childStrokes.toLongArray();
		for (int i = 0; i < childStrokes.size(); i++)
		{
			if (CStrokeController.exists(cstrokes[i]))
			{
				//CStrokeController.strokes.get(cstrokes[i]).moveInFrontOf(this);
				CalicoDraw.moveNodeInFrontOf(CStrokeController.strokes.get(cstrokes[i]), this);
			}
		}
		
		long[] carrows = this.childArrows.toLongArray();
		for (int i = 0; i < childArrows.size(); i++)
		{
			if (CArrowController.exists(carrows[i]))
			{
				//CArrowController.arrows.get(carrows[i]).moveInFrontOf(this);
				CalicoDraw.moveNodeInFrontOf(CArrowController.arrows.get(carrows[i]), this);
			}
		}
		
		long[] cconnectors = this.childConnectors.toLongArray();
		for (int i = 0; i < childConnectors.size(); i++)
		{
			if (CConnectorController.exists(cconnectors[i]))
			{
				CalicoDraw.moveNodeInFrontOf(CConnectorController.connectors.get(cconnectors[i]), this);
			}
		}
	}
	
	/**
	 * Subclasses should override this
	 */
	protected void moveInFrontOfInternal()
	{
		
	}
	
	public void resetViewOrder()
	{
		long topUUID = getTopmostParent();
		if (CGroupController.exists(topUUID))
			CGroupController.groupdb.get(topUUID).moveInFrontOf(null);
			//CalicoDraw.moveNodeInFrontOf(CGroupController.groupdb.get(topUUID), null);
	}
	
	public long getTopmostParent()
	{
		long uuid = this.uuid;
		long parentUUID = this.puid;
		
		while (CGroupController.exists(parentUUID))
		{
			uuid = parentUUID;
			parentUUID = CGroupController.groupdb.get(uuid).getParentUUID();
		}
		
		return uuid;
	}

	public void highlight_off() {
		//BubbleMenu.highlightedGroup = 0l;
		isHighlighted = false;
		this.drawPermTemp(true);
		
		/*Rectangle bounds = getBounds().getBounds();
		double buffer = 10;
		PBounds bufferBounds = new PBounds(bounds.getX() - buffer, bounds.getY() - buffer, bounds.getWidth() + buffer * 2, bounds.getHeight() + buffer * 2);
		CCanvasController.canvasdb.get(cuid).getLayer().repaintFrom(bufferBounds, this);*/
		
	}
	
	//highlight_on does not require repaint because it is sometimes faster when the area will be repainted anyway
	//highlight_off does not auto repaint to keep consistency with highlight_on
	//Therefore you must call highlight_repaint manually when needed for both off and on
	public void highlight_repaint()
	{
		Rectangle bounds = getBounds().getBounds();
		double buffer = 20;
		PBounds bufferBounds = new PBounds(bounds.getX() - buffer, bounds.getY() - buffer, bounds.getWidth() + buffer * 2, bounds.getHeight() + buffer * 2);
		//CCanvasController.canvasdb.get(cuid).getLayer().repaintFrom(bufferBounds, this);
		if (CCanvasController.getCurrentUUID() == getCanvasUID())
			CalicoDraw.repaintNode(CCanvasController.canvasdb.get(cuid).getLayer(), bufferBounds, this);
	}
	
	public void highlight_on() {
		isHighlighted = true;
		
		//BubbleMenu.activeGroup = this.uuid;
		
//		if (isPermanent)
//		{
//			this.setStrokePaint(Color.blue);
//			this.setStroke(new BasicStroke(CalicoOptions.group.stroke_size * 2));
//			this.repaintFrom(this.getBounds(), this);
//		}
	}
	
	public boolean canParent(Shape s, double area)
	{
		if (area < -1)
			area = calico.Geometry.computePolygonArea(Geometry.getPolyFromPath(s.getPathIterator(null)));
		if (this.containsShape(s) && this.groupArea > area)
			return true;
		
		return false;
	}
	
	public boolean canParent(Shape s)
	{
		return canParent(s, -1);
	}
	
	public void recomputeBounds()
	{
//		applyAffineTransform();
		
		if (CGroupController.exists(puid))
			CGroupController.groupdb.get(puid).recomputeBounds();
	}
	
	public void recomputeValues()
	{
		if (CGroupController.exists(puid))
			CGroupController.groupdb.get(puid).recomputeValues();
	}
	
//	@Override
	public void setHierarchyTransparency(float t)
	{
		long[] cgroups = getChildGroups();
		for (int i = 0; i < cgroups.length; i++)
			if (CGroupController.exists(cgroups[i]))
				//CGroupController.groupdb.get(cgroups[i]).setTransparency(t);
				CalicoDraw.setNodeTransparency(CGroupController.groupdb.get(cgroups[i]), t);
		
		//Scraps will always be slightly more transparent than the strokes on them, so strokes need to be compensated for a little bit
		long[] cstrokes = getChildStrokes();
		for (int i = 0; i < cstrokes.length; i++)
			if (CStrokeController.exists(cstrokes[i]))
			{
				float trans = 1 / CalicoOptions.group.background_transparency * t;
				if (trans > 1f)
					trans = 1f;
				//CStrokeController.strokes.get(cstrokes[i]).setTransparency(trans);
				CalicoDraw.setNodeTransparency(CStrokeController.strokes.get(cstrokes[i]), trans);
			}
		
		if (t > CalicoOptions.group.background_transparency)
			//super.setTransparency(CalicoOptions.group.background_transparency);
			CalicoDraw.setNodeTransparency((PNode) super.clone(), CalicoOptions.group.background_transparency);
		else
			//super.setTransparency(t);
			CalicoDraw.setNodeTransparency(this, t);
		
	}
	
	@Override
	public int getChildrenCount()
	{
		return 0;
	}
	
	@Override
	public void setTransparency(float t)
	{
//		System.out.println("~~~~~~~~~~~~~~~ SETTING TRANSPARENCY TO: " + t +", wasL " + getTransparency());
		super.setTransparency(t);
	}

}
