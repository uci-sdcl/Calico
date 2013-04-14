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

import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.DateFormat;
import java.util.Date;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.ProgressMonitor;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import calico.*;
import calico.components.grid.CGrid;
import calico.components.menus.CanvasMenuBar;
import calico.components.menus.CanvasGenericMenuBar;
import calico.components.menus.CanvasStatusBar;
import calico.components.menus.CanvasTopMenuBar;
import calico.controllers.*;
import calico.events.CalicoEventHandler;
import calico.events.CalicoEventListener;
import calico.input.CalicoKeyListener;
import calico.input.CalicoMouseListener;
import calico.inputhandlers.CalicoInputManager;
import calico.inputhandlers.InputEventInfo;
import calico.modules.MessageObject;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;
import calico.utils.blobdetection.*;
import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.nodes.*;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PPaintContext;
import edu.umd.cs.piccolox.nodes.PComposite;
import edu.umd.cs.piccolox.nodes.PLine;


public class CCanvas 
	implements CalicoEventListener
{
	private static Logger logger = Logger.getLogger(CCanvas.class.getName());
	
	private static final long serialVersionUID = 1L;
	
	public enum Layer
	{
		CONTENT(1),
		WATERMARK(0),
		TOOLS(2);
		
		final int id;

		private Layer(int id)
		{
			this.id = id;
		}
	}
	
	public interface ContentContributor
	{
//		boolean hasContent(long canvas_uuid);
		
		void contentChanged(long canvas_uuid);
		
		void clearContent(long canvas_uuid);
	}
	
	public static final int ROUNDED_RECTANGLE_OVERFLOW = 4;
	public static final int CELL_MARGIN = 6;

	private final ContainedCanvas canvas = new ContainedCanvas();
	
	private static final PLayer WATERMARK_PLACEHOLDER = new PLayer();
	private PLayer watermarkLayer = null;
	private final PLayer toolLayer = new PLayer();
	
	private final PCamera contentCamera = new PCamera();

	// All the BGelements, arrows, groups, objects that are on the canvas
	private LongArraySet strokes = new LongArraySet();

	private LongArraySet groups = new LongArraySet();
	
	private LongArraySet arrows = new LongArraySet();
	
	private LongArraySet lists = new LongArraySet();
	
	private LongArraySet connectors = new LongArraySet();
	
	private LongArraySet checkBoxes = new LongArraySet();
	
	private IntArraySet clients = new IntArraySet();

//	private String cell_coord = "A1";

//	private Rectangle grid_thumb_coords = new Rectangle();
	
	public CanvasMenuBar menuBarLeft = null;
	public CanvasMenuBar menuBarRight = null;
	public CanvasStatusBar statusBar = null;
	public CanvasTopMenuBar topMenuBar = null;

	private PComposite clientListPopup = null;
	
	private int index;

	public long uuid = 0L;
	
	private boolean lock_value = false;
	private String lock_last_set_by_user = "";
	private long lock_last_set_at_time = 0l;

	public CCanvas(long uuid, int index)
	{
		PLayer contentLayer = canvas.getCamera().removeLayer(0);
		toolLayer.setParent(contentLayer.getParent());
		canvas.getCamera().addLayer(Layer.WATERMARK.id, WATERMARK_PLACEHOLDER);
		canvas.getCamera().addLayer(Layer.CONTENT.id, contentLayer);
		canvas.getCamera().addLayer(Layer.TOOLS.id, toolLayer);
		contentCamera.addLayer(contentLayer);
		
		this.uuid = uuid;
		this.index = index;

		canvas.setCursor(Calico.getDrawCursor());
		
		canvas.setDefaultRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING);
		canvas.setInteractingRenderQuality(PPaintContext.LOW_QUALITY_RENDERING);
		Calico.logger.debug("setting canvas sizes ");
		canvas.setPreferredSize(new Dimension(CalicoDataStore.ScreenWidth, CalicoDataStore.ScreenHeight));
		setBounds(0, 0, CalicoDataStore.ScreenWidth, CalicoDataStore.ScreenHeight);
		canvas.setBackground( CCanvasController.getActiveCanvasBackgroundColor() );

		// We are using our own input listener, we dont want piccolo's
		canvas.removeInputSources();

		CalicoMouseListener mouseListener = new CalicoMouseListener();
		CalicoKeyListener keyListener = new CalicoKeyListener();
		
		canvas.addMouseListener(mouseListener);
		canvas.addMouseMotionListener(mouseListener);
		canvas.addMouseWheelListener(mouseListener);
		canvas.addKeyListener(keyListener);

		canvas.removeInputEventListener(canvas.getPanEventHandler());
		canvas.removeInputEventListener(canvas.getZoomEventHandler());


		CalicoInputManager.addCanvasInputHandler(this.uuid);
		
		// This makes a border, so that we see the ENTIRE canvas
		if(!CGrid.render_zoom_canvas)
		{
			/*getLayer(Layer.CONTENT).addChild(drawBorderLine(0,0, CalicoDataStore.ScreenWidth,0));//top
			getLayer(Layer.CONTENT).addChild(drawBorderLine(CalicoDataStore.ScreenWidth,0, CalicoDataStore.ScreenWidth, CalicoDataStore.ScreenHeight));//right
			getLayer(Layer.CONTENT).addChild(drawBorderLine(0,0, 0, CalicoDataStore.ScreenHeight));//left
			getLayer(Layer.CONTENT).addChild(drawBorderLine(0,CalicoDataStore.ScreenHeight, CalicoDataStore.ScreenWidth,CalicoDataStore.ScreenHeight));//bottom
			*/
			CalicoDraw.addChildToNode(getLayer(Layer.CONTENT), drawBorderLine(0,0, CalicoDataStore.ScreenWidth,0));//top
			CalicoDraw.addChildToNode(getLayer(Layer.CONTENT), drawBorderLine(CalicoDataStore.ScreenWidth,0, CalicoDataStore.ScreenWidth, CalicoDataStore.ScreenHeight));//right
			CalicoDraw.addChildToNode(getLayer(Layer.CONTENT), drawBorderLine(0,0, 0, CalicoDataStore.ScreenHeight));//left
			CalicoDraw.addChildToNode(getLayer(Layer.CONTENT), drawBorderLine(0,CalicoDataStore.ScreenHeight, CalicoDataStore.ScreenWidth,CalicoDataStore.ScreenHeight));//bottom
		}
		canvas.repaint();
	}
	
	public JComponent getComponent()
	{
		return canvas;
	}
	
	public PLayer getLayer(Layer layer)
	{
		return canvas.getCamera().getLayer(layer.id);
	}
	
	public PLayer getLayer()
	{
		return canvas.getCamera().getLayer(Layer.CONTENT.id);
	}
	
	public PCamera getCamera()
	{
		return canvas.getCamera();
	}
	
	public void setCamera(PCamera camera)
	{
		canvas.setCamera(camera);
	}
	
	public PCamera getContentCamera()
	{
		return contentCamera;
	}
	
	public boolean hasWatermarkLayer()
	{
		return watermarkLayer != null;
	}
	
	public PLayer getWatermarkLayer()
	{
		return watermarkLayer;
	}
	
	public void setWatermarkLayer(PLayer watermarkLayer)
	{
		removeWatermarkLayer();
		installWatermarkLayer(watermarkLayer);
	}
	
	private void installWatermarkLayer(PLayer watermarkLayer)
	{
		this.watermarkLayer = watermarkLayer;
		if (this.watermarkLayer != null)
		{
			canvas.getCamera().removeLayer(Layer.WATERMARK.id);
			canvas.getCamera().addLayer(Layer.WATERMARK.id, this.watermarkLayer);
			this.watermarkLayer.setBounds(getLayer(Layer.CONTENT).computeFullBounds(null)); 
			canvas.repaint();
		}
	}
	
	public void removeWatermarkLayer()
	{
		if (this.watermarkLayer != null)
		{
			canvas.getCamera().removeLayer(Layer.WATERMARK.id);
			canvas.getCamera().addLayer(Layer.WATERMARK.id, WATERMARK_PLACEHOLDER);
			canvas.repaint();
		}
	}

	private PLine drawBorderLine(int x,int y, int x2, int y2)
	{
		PLine pline = new PLine();
		pline.addPoint(0, x, y);
		pline.addPoint(1, x2, y2);
		pline.setStroke(new BasicStroke( 1.0f ));
		pline.setStrokePaint( CCanvasController.getActiveCanvasBackgroundColor() );
		return pline;
	}

	public int getIndex()
	{
		return index;
	}
	
	public int getSignature()
	{
//		int sig = this.strokes.size() + this.groups.size() + this.lists.size() + this.checkBoxes.size() + this.arrows.size();
		int sig = 0;

		long[] strokear = getChildStrokes();
		long[] groupar = getChildGroups();
		long[] arrar = getChildArrows();
		long[] ctrar = getChildConnectors();
		
		int stroke_sig = 0;
		for(int i=0;i<strokear.length;i++)
		{
			stroke_sig = stroke_sig + CStrokeController.get_signature(strokear[i]);
		}
		
		int group_sig = 0;
		for(int i=0;i<groupar.length;i++)
		{
			group_sig = group_sig + CGroupController.get_signature(groupar[i]);
		}
		
		int arrow_sig = 0;
		for (int i=0;i<arrar.length;i++)
		{
			arrow_sig = arrow_sig + CArrowController.get_signature(arrar[i]);
		}
		
		int connector_sig = 0;
		for (int i=0;i<ctrar.length;i++)
		{
			connector_sig = connector_sig + CConnectorController.get_signature(ctrar[i]);
		}
		
		return stroke_sig + group_sig + arrow_sig + connector_sig;
	}
	
	public CalicoPacket getConsistencyDebugPacket()
	{
//		int sig = this.strokes.size() + this.groups.size() + this.lists.size() + this.checkBoxes.size() + this.arrows.size();
		CalicoPacket p = new CalicoPacket();
		

		long[] strokear = getChildStrokes();
		long[] groupar = getChildGroups();
//		long[] arrar = getChildArrows();
		
		int stroke_sig = 0;
		for(int i=0;i<strokear.length;i++)
		{
			stroke_sig = stroke_sig + CStrokeController.get_signature(strokear[i]);
			p.putLong(strokear[i]);
			p.putInt(stroke_sig);
			p.putString(CStrokeController.get_signature_debug_output(strokear[i]));
			
		}
		
		int group_sig = 0;
		for(int i=0;i<groupar.length;i++)
		{
			group_sig = group_sig + CGroupController.get_signature(groupar[i]);
			p.putLong(groupar[i]);
			p.putInt(group_sig);
			p.putString(CGroupController.get_signature_debug_output(groupar[i]));
		}
		
//		int arrow_sig = 0;
//		for (int i=0;i<arrar.length;i++)
//		{
//			arrow_sig = arrow_sig + CArrowController.get_signature(arrar[i]);
//		}
		
		return p;
	}
	
	
	public void clickMenuBar(InputEventInfo event, Point point)
	{
		if(this.menuBarLeft.isPointInside(point))
		{
			this.menuBarLeft.clickMenu(event, point);
		}
		if(this.menuBarRight != null && this.menuBarRight.isPointInside(point))
		{
			this.menuBarRight.clickMenu(event, point);
		}
		if(this.statusBar.isPointInside(point))
		{
			this.statusBar.clickMenu(event, point);
		}
		else if(this.topMenuBar!=null && this.topMenuBar.isPointInside(point))
		{
			this.topMenuBar.clickMenu(event, point);	
		}
	}
	
	public boolean isPointOnMenuBar(Point point)
	{
		if (this.menuBarLeft != null
			&& this.menuBarLeft.isPointInside(point))
			return true;
		
		if (this.menuBarRight != null
				&& this.menuBarRight.isPointInside(point))
				return true;
		
		if (this.statusBar != null
				&& this.statusBar.isPointInside(point))
				return true;
		
		if (this.topMenuBar != null
			&& this.topMenuBar.isPointInside(point))
			return true;
		
		return false;
	}

	public void addChildStroke(long uid)
	{
		this.strokes.add(uid);
		CCanvasController.notifyContentChanged(uuid);
	}
	public int getNumStrokes()
	{
		return this.strokes.size();
	}
	
	public void addChildConnector(long uid)
	{
		this.connectors.add(uid);
		CCanvasController.notifyContentChanged(uuid);
	}
	public int getNumConnectors()
	{
		return this.connectors.size();
	}
	
	/**
	 * @deprecated
	 * @see #getChildStrokes()
	 * @return
	 */
	public long[] getStrokes()
	{
		return getChildStrokes();
	}
	public void deleteChildStroke(long uid)
	{
		this.strokes.remove(uid);
		CCanvasController.notifyContentChanged(uuid);
	}
	
	public void deleteChildConnector(long uid)
	{
		this.connectors.remove(uid);
		CCanvasController.notifyContentChanged(uuid);
	}

	public void addChildGroup(long gUUID)
	{
		this.groups.add(gUUID);
		CCanvasController.notifyContentChanged(uuid);
	}
	
	public void addChildList(long lUUID)
	{
		this.lists.add(lUUID);
		CCanvasController.notifyContentChanged(uuid);
	}
	
	public void addChildCheckBox(long cUUID)
	{
		this.checkBoxes.add(cUUID);
		CCanvasController.notifyContentChanged(uuid);
	}

	/**
	 * @deprecated
	 * @see #getChildGroups()
	 * @return
	 */
	public long[] getGroupElements()
	{
		return getChildGroups();
	}

	/**
	 * Removes a group from this canvas (but not the painted canvas)
	 * @param gUUID
	 */
	public void deleteChildGroup(long gUUID)
	{
		this.groups.remove(gUUID);
		CCanvasController.notifyContentChanged(uuid);
	}
	
	public void deleteChildList(long gUUID)
	{
		this.lists.remove(gUUID);
		CCanvasController.notifyContentChanged(uuid);
	}
	
	public void deleteChildCheckBox(long cUUID)
	{
		this.checkBoxes.remove(cUUID);
		CCanvasController.notifyContentChanged(uuid);
	}

	/**
	 * @deprecated
	 * @see #getChildArrows()
	 * @return
	 */
	public long[] listArrows()
	{
		return getChildArrows();
	}
	public void addChildArrow(long uid)
	{
		this.arrows.add(uid);
		CCanvasController.notifyContentChanged(uuid);
	}
	public void removeChildArrow(long uid)
	{
		this.arrows.remove(uid);
		CCanvasController.notifyContentChanged(uuid);
	}
	
	public Rectangle getBounds()
	{
		return canvas.getBounds();
	}
	
	public void setBounds(int x, int y, int w, int h)
	{
		canvas.setBounds(x, y, w, h);
	}

	private void drawTopMenuBar()
	{
		if(topMenuBar!=null)
		{
			//toolLayer.removeChild(topMenuBar);
			CalicoDraw.removeChildFromNode(toolLayer, topMenuBar);
			topMenuBar = null;
		}
		topMenuBar = new CanvasTopMenuBar(uuid);
		//toolLayer.addChild(topMenuBar);
		CalicoDraw.addChildToNode(toolLayer, topMenuBar);
	}
	
	private void drawMenuBar()
	{
		CanvasMenuBar tempLeft = new CanvasMenuBar(this.uuid, CanvasGenericMenuBar.POSITION_LEFT);
		//toolLayer.addChild(tempLeft);
		CalicoDraw.addChildToNode(toolLayer, tempLeft);
		
		CanvasMenuBar tempRight = new CanvasMenuBar(this.uuid, CanvasGenericMenuBar.POSITION_RIGHT);
		//toolLayer.addChild(tempRight);
		CalicoDraw.addChildToNode(toolLayer, tempRight);
		
		if(this.menuBarLeft!=null)
		{
			//canvas.getCamera().removeChild(this.menuBarLeft);
			//toolLayer.removeChild(this.menuBarLeft);
			CalicoDraw.removeChildFromNode(toolLayer, this.menuBarLeft);
			this.menuBarLeft = null;
		}
		if(this.menuBarRight!=null)
		{
			//toolLayer.removeChild(this.menuBarRight);
			CalicoDraw.removeChildFromNode(toolLayer, this.menuBarRight);
			this.menuBarRight = null;
		}
		
		this.menuBarLeft = tempLeft;
		this.menuBarRight = tempRight;
		if (this.menuBarLeft != null)
			//this.menuBarLeft.repaint();
			CalicoDraw.repaint(this.menuBarLeft);
		if (this.menuBarRight != null)
			//this.menuBarRight.repaint();
			CalicoDraw.repaint(this.menuBarRight);
	}
	
	private void drawStatusBar()
	{
		CanvasStatusBar temp = new CanvasStatusBar(this.uuid);
		//toolLayer.addChild(temp);
		CalicoDraw.addChildToNode(toolLayer, temp);
		
		if(this.statusBar!=null)
		{
			//toolLayer.removeChild(this.statusBar);
			CalicoDraw.removeChildFromNode(toolLayer, this.statusBar);
			this.statusBar = null;
		}
		
		this.statusBar = temp;
		//this.statusBar.repaint();
		CalicoDraw.repaint(this.statusBar);
	}
	
	public void drawMenuBars()
	{		
//		drawTopMenuBar();
		drawMenuBar();
		drawStatusBar();
	}
	
	public void removeTopMenuBar(){
		if(topMenuBar!=null)
		{
			//toolLayer.removeChild(topMenuBar);
			CalicoDraw.removeChildFromNode(toolLayer, topMenuBar);
			topMenuBar = null;
		}
	}
	
	public void redrawToolbar_clients()
	{
//		if (this.menuBar != null)
//			this.menuBar.redrawClients();
	}

	public void setAsCurrent()
	{
		CCanvasController.setCurrentUUID(this.uuid);
	}
	
	public boolean isEmpty()
	{
		return (strokes.isEmpty() && groups.isEmpty() && lists.isEmpty() && checkBoxes.isEmpty() && arrows.isEmpty());
	}

	public Image toImage()
	{
		Color backgroundColor;
//		if (CCanvasController.getLastActiveUUID() == this.uuid)
//			backgroundColor = new Color(200,200,200);
//		else
			backgroundColor = CCanvasController.getActiveCanvasBackgroundColor();
		
		if (!isEmpty())
		{
			//logger.debug("Canvas "+cell_coord+" render image");
//			getCamera().removeChild(menuBarLeft);
//			getCamera().removeChild(menuBarRight);
//			getCamera().removeChild(statusBar);
			//getCamera().removeChild(topMenuBar);
			if (uuid != CCanvasController.getCurrentUUID())
				CCanvasController.loadCanvasImages(uuid);
			
			Rectangle boundsOfCanvas = CCanvasController.canvasdb.get(uuid).getBounds();
			double widthRatio = (double)boundsOfCanvas.width / CalicoDataStore.serverScreenWidth;
			double heightRatio = (double)boundsOfCanvas.height / CalicoDataStore.serverScreenHeight;
			
			double ratio = Math.min(widthRatio, heightRatio);
			CCanvasController.canvasdb.get(uuid).getLayer().setScale(ratio);
			
//			Image img = contentCamera.toImage(CGrid.gwidth, CGrid.gheight, backgroundColor);
			Image img = contentCamera.toImage(CalicoDataStore.CanvasSnapshotSize.width, CalicoDataStore.CanvasSnapshotSize.height, backgroundColor);
			//Image img = contentCamera.toImage(CGrid.gwidth - 5, CGrid.gheight, Color.lightGray);
			if (uuid != CCanvasController.getCurrentUUID())
				CCanvasController.unloadCanvasImages(uuid);
			
//			getCamera().addChild(menuBarLeft);
//			if (menuBarRight != null)
//				getCamera().addChild(menuBarRight);
//			getCamera().addChild(statusBar);
			//getCamera().addChild(topMenuBar);
			
			return img;
		}
		else
		{
			// This makes a solid white image for the canvas
//			BufferedImage bimg = new BufferedImage(CGrid.gwidth, CGrid.gheight, BufferedImage.TYPE_INT_ARGB);
			BufferedImage bimg = new BufferedImage(CalicoDataStore.CanvasSnapshotSize.width, CalicoDataStore.CanvasSnapshotSize.height, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = (Graphics2D)bimg.createGraphics();
			g.setComposite(AlphaComposite.Src);
			g.setColor(backgroundColor);
			g.fill(new Rectangle(0,0,CGrid.gwidth, CGrid.gheight));
			g.draw(new Rectangle(0,0,CGrid.gwidth, CGrid.gheight));
			g.fill(new Rectangle(0,0,CalicoDataStore.CanvasSnapshotSize.width, CalicoDataStore.CanvasSnapshotSize.height));
			g.draw(new Rectangle(0,0,CalicoDataStore.CanvasSnapshotSize.width, CalicoDataStore.CanvasSnapshotSize.height));
			g.dispose();
			
			return bimg;
		}
	}
	
	public Image toImage(int width, int height)
	{
		Color backgroundColor;
		if (CCanvasController.getLastActiveUUID() == this.uuid)
			backgroundColor = new Color(200,200,200);
		else
			backgroundColor = CCanvasController.getActiveCanvasBackgroundColor();

		if (!isEmpty())
		{
			//logger.debug("Canvas "+cell_coord+" render image");
//			getCamera().removeChild(menuBarLeft);
//			getCamera().removeChild(menuBarRight);
//			getCamera().removeChild(statusBar);
			//getCamera().removeChild(topMenuBar);
			if (uuid != CCanvasController.getCurrentUUID())
				CCanvasController.loadCanvasImages(uuid);

			Rectangle boundsOfCanvas = CCanvasController.canvasdb.get(uuid).getBounds();
			double widthRatio = (double)boundsOfCanvas.width / CalicoDataStore.ScreenWidth;
			double heightRatio = (double)boundsOfCanvas.height / CalicoDataStore.ScreenHeight;

			double ratio = Math.min(widthRatio, heightRatio);
			CCanvasController.canvasdb.get(uuid).getLayer().setScale(ratio);

			Image img = contentCamera.toImage(width, height, backgroundColor);
//			Image img = contentCamera.toImage(CalicoDataStore.CanvasSnapshotSize.width, CalicoDataStore.CanvasSnapshotSize.height, backgroundColor);
			//Image img = contentCamera.toImage(CGrid.gwidth - 5, CGrid.gheight, Color.lightGray);
			if (uuid != CCanvasController.getCurrentUUID())
				CCanvasController.unloadCanvasImages(uuid);

//			getCamera().addChild(menuBarLeft);
//			if (menuBarRight != null)
//				getCamera().addChild(menuBarRight);
//			getCamera().addChild(statusBar);
			//getCamera().addChild(topMenuBar);

			return img;
		}
		else
		{
			// This makes a solid white image for the canvas
			BufferedImage bimg = new BufferedImage(CGrid.gwidth, CGrid.gheight, BufferedImage.TYPE_INT_ARGB);
//			BufferedImage bimg = new BufferedImage(CalicoDataStore.CanvasSnapshotSize.width, CalicoDataStore.CanvasSnapshotSize.height, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = (Graphics2D)bimg.createGraphics();
			g.setComposite(AlphaComposite.Src);
			g.setColor(backgroundColor);
			g.fill(new Rectangle(0,0,CGrid.gwidth, CGrid.gheight));
			g.draw(new Rectangle(0,0,CGrid.gwidth, CGrid.gheight));
			g.fill(new Rectangle(0,0,CalicoDataStore.CanvasSnapshotSize.width, CalicoDataStore.CanvasSnapshotSize.height));
			g.draw(new Rectangle(0,0,CalicoDataStore.CanvasSnapshotSize.width, CalicoDataStore.CanvasSnapshotSize.height));
			g.dispose();

			return bimg;
		}
	}
	
	
	
	public void getBlobs()
	{
//		getCamera().removeChild(menuBarLeft);
//		getCamera().removeChild(menuBarRight);
//		getCamera().removeChild(statusBar);
		//getCamera().removeChild(topMenuBar);
		
		Image img = canvas.getCamera().toImage(CalicoDataStore.ScreenWidth, CalicoDataStore.ScreenHeight, CCanvasController.getActiveCanvasBackgroundColor());
//		getCamera().addChild(menuBarLeft);
//		if (menuBarRight != null)
//			getCamera().addChild(menuBarRight);
//		getCamera().addChild(statusBar);
		//getCamera().addChild(topMenuBar);
		
		int imgwidth = img.getWidth(null);
		int imgheight = img.getHeight(null);
		
		/*
		 * BufferedImage bimg = new BufferedImage(tBounds.width, tBounds.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D)bimg.createGraphics();
		g.setComposite(AlphaComposite.Src);
		g.drawImage(img, 2, 2, tBounds.width - 4, tBounds.height - 4,null);
		g.setColor(Color.green);
		g.fill(border);
		g.setColor(new Color(100,100,100));
		g.draw(rRect);
		g.setColor(Color.black);
		g.drawRoundRect(	2,//new Double(getBounds().x).intValue() + 2,
							2,//new Double(getBounds().y).intValue() + 2,
							tBounds.width - 4,
							tBounds.height - 4,
							10,
							10);
		g.dispose();
		 */
		/*
		BufferedImage bimg = new BufferedImage(CalicoDataStore.ScreenWidth, CalicoDataStore.ScreenHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D)bimg.createGraphics();
		g.setComposite(AlphaComposite.Src);
		g.drawImage(img, 1, 1, CalicoDataStore.ScreenWidth-2, CalicoDataStore.ScreenHeight-2, null);
		g.setColor(Color.green);
		
		g.dispose();
		*/
		
		BufferedImage bimg = PImage.toBufferedImage(img, true);

		try
		{
			ImageIO.write(bimg, "PNG", new File("img.png"));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}


		int white = Color.white.getRGB();
		int black = Color.black.getRGB();
		
		int[] pixels = new int[(imgwidth*imgheight)+1];
		int count = 0;
		
			for(int h=0;h<imgheight;h++)
			{
				for(int w=0;w<imgwidth;w++)
				{
				//pixels[(h*imgwidth)+(w+1)] = bimg.getRGB(w,h);
				pixels[count] = white;
				if(bimg.getRGB(w,h)!=white)
				{
					pixels[count] = black;//bimg.getRGB(w,h);
				}
				count++;
			}
		}
		
		
		
		Graphics2D g = (Graphics2D)bimg.createGraphics();
		g.setComposite(AlphaComposite.Src);
		
		
		g.setColor(Color.RED);
		
		
		
		
		
		logger.debug("STARTING BLOB DETECT");
		
		BlobDetection theBlobDetection = null;
		theBlobDetection = new BlobDetection(imgwidth, imgheight);
		theBlobDetection.setPosDiscrimination(false);
		theBlobDetection.setThreshold(0.38f);
		theBlobDetection.computeBlobs(pixels);
		
		
		Blob b;
		EdgeVertex eA,eB;
		boolean drawEdges = true;
		boolean drawBlobs = true;

		float width = (float) (CalicoDataStore.ScreenWidth);
		float height = (float) (CalicoDataStore.ScreenHeight);
		
		logger.debug("FOUND "+theBlobDetection.getBlobNb()+" blobs");
		
		for (int n=0 ; n<theBlobDetection.getBlobNb() ; n++)
		{
			b=theBlobDetection.getBlob(n);
			if (b!=null)
			{
				logger.debug("BLOB FOUND"); 
				// Edges
				if (drawEdges)
				{
					//strokeWeight(3);
					//stroke(0,255,0);
					PPath linetemp = new PPath();
					linetemp.setStroke(new BasicStroke(CalicoOptions.arrow.stroke_size));
					linetemp.setStrokePaint(Color.RED);
					linetemp.setPaint(Color.RED);
					
					for (int m=0;m<b.getEdgeNb();m++)
					{
						eA = b.getEdgeVertexA(m);
						eB = b.getEdgeVertexB(m);
						if (eA !=null && eB !=null)
						{
							logger.debug("BLOB EDGE: ("+eA.x*width+","+eA.y*height+"),("+eB.x*width+","+eB.y*height+")");
							if(m==0)
							{
								linetemp.moveTo(eA.x*width, eA.y*height);
							}
							else
							{
								linetemp.lineTo(eA.x*width, eA.y*height);
								
							}
							//g.drawLine((int)(eA.x*width), (int)(eA.y*height), (int)(eB.x*width), (int)(eB.y*height));
							
							linetemp.lineTo(eB.x*width, eB.y*height);
							
							/*line(
									eA.x*width, eA.y*height, 
									eB.x*width, eB.y*height
									);*/	
						}
							
					}
					//this.getLayer(Layer.CONTENT).addChild(linetemp);
					CalicoDraw.addChildToNode(this.getLayer(Layer.CONTENT), linetemp);
					//linetemp.repaint();
					CalicoDraw.repaint(linetemp);
				}

				// Blobs
				if (drawBlobs)
				{
					//strokeWeight(1);
					//stroke(255,0,0);
					logger.debug("BLOB RECT: ("+b.xMin*width+","+b.yMin*height+"),(w="+b.w*width+",h="+b.h*height+")");
					g.drawRect((int)(b.xMin*width),(int)(b.yMin*height),
						(int)(b.w*width),(int)(b.h*height));
					/*rect(
						b.xMin*width,b.yMin*height,
						b.w*width,b.h*height
						);*/
				}

			}

	      }
		
		g.dispose();
		
		try
		{
			ImageIO.write(bimg, "PNG", new File("img2.png"));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		
		
	}//getBlobs
	
	
	

	/**
	 * @deprecated
	 */
	public void render()
	{
		setBounds(0,0,CalicoDataStore.ScreenWidth, CalicoDataStore.ScreenHeight);
		canvas.repaint();
	}


	//////

	@Deprecated
	public void reload_start()
	{
		arrows.clear();
		strokes.clear();
		groups.clear();
		CCanvasController.notifyContentChanged(uuid);
	}
	@Deprecated
	public void reload_finish()
	{
		
	}
	@Deprecated
	public void reload_groups(long[] uuids)
	{
		for(int i=0;i<uuids.length;i++)
		{
			groups.add(uuids[i]);
		}
		CCanvasController.notifyContentChanged(uuid);
	}
	@Deprecated
	public void reload_strokes(long[] uuids)
	{
		for(int i=0;i<uuids.length;i++)
		{
			strokes.add(uuids[i]);
		}
		CCanvasController.notifyContentChanged(uuid);
	}
	@Deprecated
	public void reload_arrows(long[] uuids)
	{
		for(int i=0;i<uuids.length;i++)
		{
			arrows.add(uuids[i]);
		}	
		CCanvasController.notifyContentChanged(uuid);
	}
	
	public void repaint()
	{
		canvas.repaint();
	}
	
	public void repaint(final PBounds bounds)
	{
		canvas.repaint(bounds);
	}
	
	public void repaint(final Rectangle bounds)
	{
		canvas.repaint(bounds);
	}
	
	public void setEnabled(boolean b)
	{
		canvas.setEnabled(b);
	}
	
	public void setInteracting(boolean b)
	{
		canvas.setInteracting(b);
	}
	
	public void validate()
	{
		canvas.validate();
	}
	
	public void addMouseListener(MouseListener listener)
	{
		canvas.addMouseListener(listener);
	}
	
	public void addMouseMotionListener(MouseMotionListener listener)
	{
		canvas.addMouseMotionListener(listener);
	}
	
	public void removeMouseListener(MouseListener listener)
	{
		canvas.removeMouseListener(listener);
	}
	
	public void removeMouseMotionListener(MouseMotionListener listener)
	{
		canvas.removeMouseMotionListener(listener);
	}
	
	public long[] getChildGroups()
	{
		return this.groups.toLongArray();
	}
	public long[] getChildLists()
	{
		return this.lists.toLongArray();
	}
	public long[] getChildCheckBoxes()
	{
		return this.checkBoxes.toLongArray();
	}
	public long[] getChildStrokes()
	{
		return this.strokes.toLongArray();
	}
	public long[] getChildArrows()
	{
		return this.arrows.toLongArray();
	}
	public long[] getChildConnectors()
	{
		return this.connectors.toLongArray();
	}
	public boolean hasChildStroke(long cuuid)
	{
		return this.strokes.contains(cuuid);
	}
	public boolean hasChildGroup(long cuuid)
	{
		return this.groups.contains(cuuid);
	}
	public boolean hasChildList(long cuuid)
	{
		return this.lists.contains(cuuid);
	}
	public boolean hasChildCheckBox(long cuuid)
	{
		return this.checkBoxes.contains(cuuid);
	}
	public boolean hasChildArrow(long cuuid)
	{
		return this.arrows.contains(cuuid);
	}
	public boolean hasChildConnector(long cuuid)
	{
		return this.connectors.contains(cuuid);
	}
	
	/**
	 * This clears the canvas and all its contents.
	 */
	public void clear()
	{
		CGroupController.restoreOriginalStroke = false;
		
		if(this.groups.size()>0)
		{
			long guuids[] = this.groups.toLongArray();
			for(int i=guuids.length-1;i>=0;i--)
			{
				CGroupController.no_notify_delete(guuids[i]);
			}
		}
		
		if(this.strokes.size()>0)
		{
			long suuids[] = this.strokes.toLongArray();
			for(int i=suuids.length-1;i>=0;i--)
			{
				CStrokeController.no_notify_delete(suuids[i]);
			}
		}
		
		if(this.arrows.size()>0)
		{
			long auuids[] = this.arrows.toLongArray();
			for(int i=auuids.length-1;i>=0;i--)
			{
				CArrowController.no_notify_delete(auuids[i]);
			}
		}
		
		if(this.connectors.size()>0)
		{
			long cuuids[] = this.connectors.toLongArray();
			for(int i=cuuids.length-1;i>=0;i--)
			{
				CConnectorController.no_notify_delete(cuuids[i]);
			}
		}

		this.groups = new LongArraySet();
		this.strokes = new LongArraySet();
		this.arrows = new LongArraySet();
		this.connectors = new LongArraySet();
		this.lists = new LongArraySet();
		this.checkBoxes = new LongArraySet();
		
//		this.getLayer().removeAllChildren();
		
		//removing this line because it seems to incorrectly remove the borders and the repaint below seems to do the same thing.
		//clearEverythingExceptMenu();
		
		drawMenuBars();
		//this.getLayer(Layer.CONTENT).repaint();
		CalicoDraw.repaint(this.getLayer(Layer.CONTENT));
	}
	
	@Deprecated
	private void clearEverythingExceptMenu()
	{
		for (int i = getLayer(Layer.CONTENT).getChildrenCount() - 1; i >= 0; i--)
		{
			if (getLayer(Layer.CONTENT).getChild(i) instanceof CanvasGenericMenuBar)
				continue;
			else
				//getLayer(Layer.CONTENT).removeChild(i);
				CalicoDraw.removeChildFromNode(getLayer(Layer.CONTENT), i);
		}
	}
	

	public void addClient(int clientid) {
		this.clients.add(clientid);
	}
	
	public void removeClient(int clientid) {
		this.clients.remove(clientid);
	}
	public void clearClients() {
		this.clients.clear();
	}
	
	public int[] getClients() {
		return this.clients.toIntArray();
	}
	
	
	public void drawClientList(Rectangle boundingBox) {
		
		if(this.clientListPopup!=null) {
			//this.clientListPopup.removeFromParent();
			CalicoDraw.removeNodeFromParent(this.clientListPopup);
			CalicoDataStore.calicoObj.getContentPane().getComponent(0).repaint();
			this.clientListPopup = null;
			return;
		}
		
		this.clientListPopup = new PComposite();
		
		StringBuilder str = new StringBuilder();
		str.append("Clients on this canvas:");
		if(this.clients.size()==0) {
			str.append("\nNo clients are on this canvas");
		}
		else {
			int[] clients = this.getClients();
			for(int i=0;i<clients.length;i++) {
				if(CalicoDataStore.clientInfo.containsKey(clients[i])) {
					str.append("\n"+CalicoDataStore.clientInfo.get(clients[i]));
				} else {
					//str.append("\nUnknown ("+clients[i]+")");
				}
				
			}
		}
		
		PText text = new PText(str.toString());
		text.setConstrainWidthToTextWidth(true);
		text.setFont(new Font(CalicoOptions.messagebox.font.name, Font.BOLD, CalicoOptions.messagebox.font.size));
		text.setConstrainHeightToTextHeight(true);
		text.setConstrainWidthToTextWidth(true);
		text.setTextPaint(CalicoOptions.messagebox.color.text);
		
		text.recomputeLayout();
		Rectangle ntextbounds = text.getBounds().getBounds();
		

		int padding = CalicoOptions.messagebox.padding;
		int padadd = padding*2;
		
		
		int lastVertPos = 30;
		
		
		
		
		//int vertpos = CCanvasController.canvasdb.get(canvas_uid).messageBoxOffset;
		

		Rectangle bounds = new Rectangle(16,lastVertPos,ntextbounds.width+padadd,ntextbounds.height+padadd);
		
		bounds.setLocation(boundingBox.x, boundingBox.y-ntextbounds.height-30);

		Rectangle textbounds = new Rectangle(bounds.x+padding,bounds.y+padding,ntextbounds.width,ntextbounds.height);

		PNode bgnode = new PNode();
		bgnode.setPaint(CalicoOptions.messagebox.color.notice);
		
		bgnode.setBounds(bounds);
		
		
		text.setBounds(textbounds);

		/*this.clientListPopup.addChild(0,bgnode);
		this.clientListPopup.addChild(1,text);
		this.clientListPopup.setBounds(bounds);*/
		CalicoDraw.addChildToNode(this.clientListPopup, bgnode, 0);
		CalicoDraw.addChildToNode(this.clientListPopup, text, 1);
		CalicoDraw.setNodeBounds(this.clientListPopup, bounds);
		

		//((PCanvas)CalicoDataStore.calicoObj.getContentPane().getComponent(0)).getCamera().addChild(0, this.clientListPopup);
		CalicoDraw.addChildToNode(((PCanvas)CalicoDataStore.calicoObj.getContentPane().getComponent(0)).getCamera(), this.clientListPopup, 0);
		CalicoDataStore.calicoObj.getContentPane().getComponent(0).validate();
		((PCanvas)CalicoDataStore.calicoObj.getContentPane().getComponent(0)).getCamera().validateFullPaint();
//		CalicoDataStore.calicoObj.getContentPane().getComponent(0).repaint();
//		((PCanvas)CalicoDataStore.calicoObj.getContentPane().getComponent(0)).getCamera().repaint();
		
		Rectangle newBounds = new Rectangle(bounds.x, bounds.y, bounds.width, bounds.height+padding);
	}
	
	public void drawLockInfo(Rectangle boundingBox) {
		
		if(this.clientListPopup!=null) {
			//this.clientListPopup.removeFromParent();
			CalicoDraw.removeNodeFromParent(this.clientListPopup);
			CalicoDataStore.calicoObj.getContentPane().getComponent(0).validate();
			this.clientListPopup = null;
			return;
		}
		
		this.clientListPopup = new PComposite();
		
		StringBuilder str = new StringBuilder();
		Date epoch = new Date(lock_last_set_at_time);
		str.append("Status: " + ((lock_value)?"Locked":"Not locked"));
		if (lock_last_set_at_time != 0l)
		{
		DateFormat df2 = DateFormat.getDateInstance(DateFormat.MEDIUM);
		DateFormat df3 = DateFormat.getTimeInstance(DateFormat.MEDIUM);
		String s2 = df2.format(epoch);
		String s3 = df3.format(epoch);
		str.append("\n- Last set by: " + lock_last_set_by_user);
		str.append("\n- Set on: " + s2);
		str.append("\n- At time: " + s3);
		}
		
		PText text = new PText(str.toString());
		text.setConstrainWidthToTextWidth(true);
		text.setFont(new Font(CalicoOptions.messagebox.font.name, Font.BOLD, CalicoOptions.messagebox.font.size));
		text.setConstrainHeightToTextHeight(true);
		text.setConstrainWidthToTextWidth(true);
		text.setTextPaint(CalicoOptions.messagebox.color.text);
		
		text.recomputeLayout();
		Rectangle ntextbounds = text.getBounds().getBounds();
		

		int padding = CalicoOptions.messagebox.padding;
		int padadd = padding*2;
		
		
		int lastVertPos = 30;
		
		
		
		
		//int vertpos = CCanvasController.canvasdb.get(canvas_uid).messageBoxOffset;
		

		Rectangle bounds = new Rectangle(16,lastVertPos,ntextbounds.width+padadd,ntextbounds.height+padadd);
		
		bounds.setLocation(boundingBox.x, boundingBox.y-ntextbounds.height-30);

		Rectangle textbounds = new Rectangle(bounds.x+padding,bounds.y+padding,ntextbounds.width,ntextbounds.height);

		PNode bgnode = new PNode();
		bgnode.setPaint(CalicoOptions.messagebox.color.notice);
		
		bgnode.setBounds(bounds);
		
		
		text.setBounds(textbounds);

		/*this.clientListPopup.addChild(0,bgnode);
		this.clientListPopup.addChild(1,text);
		this.clientListPopup.setBounds(bounds);*/
		CalicoDraw.addChildToNode(this.clientListPopup, bgnode, 0);
		CalicoDraw.addChildToNode(this.clientListPopup, text, 1);
		CalicoDraw.setNodeBounds(this.clientListPopup, bounds);
		

		
		//((PCanvas)CalicoDataStore.calicoObj.getContentPane().getComponent(0)).getCamera().addChild(0, this.clientListPopup);
		CalicoDraw.addChildToNode(((PCanvas)CalicoDataStore.calicoObj.getContentPane().getComponent(0)).getCamera(), this.clientListPopup, 0);
		canvas.repaint(menuBarLeft.getBounds());
		if (menuBarRight != null)
			canvas.repaint(menuBarRight.getBounds());
		CalicoDataStore.calicoObj.getContentPane().getComponent(0).validate();
		((PCanvas)CalicoDataStore.calicoObj.getContentPane().getComponent(0)).getCamera().validateFullPaint();
		
//		CalicoDataStore.calicoObj.getContentPane().getComponent(0).repaint();
//		((PCanvas)CalicoDataStore.calicoObj.getContentPane().getComponent(0)).getCamera().repaint();
		
		Rectangle newBounds = new Rectangle(bounds.x, bounds.y, bounds.width, bounds.height+padding);
	}
	
	public void setCanvasLock(boolean lock, String user, long time)
	{
		this.lock_value = lock;
		this.lock_last_set_by_user = user;
		this.lock_last_set_at_time = time;
	}
	
	public boolean getLockValue()
	{
		return this.lock_value;
	}
	
	public String getLockedByUser()
	{
		return this.lock_last_set_by_user;
	}
	
	public long getLockedAtTime()
	{
		return this.lock_last_set_at_time;
	}

	public void resetLock() {
		this.lock_value = false;
		this.lock_last_set_by_user = "";
		this.lock_last_set_at_time = 0l;
	}
	
	public Point getScaledPoint(Point location)
	{
		double scale = getLayer(Layer.CONTENT).getScale();
		
		int xpos = (int)(location.getX() * scale);
		int ypos = (int)(location.getY() * scale);
		
		return new Point(xpos, ypos);		
	}
	
	public Point getUnscaledPoint(Point location)
	{
		double scale = canvas.getCamera().getScale();
		
		int xpos = (int)(location.getX() * 1/scale);
		int ypos = (int)(location.getY() * 1/scale);
		
		return new Point(xpos, ypos);
	}
	
	@Override
	public void handleCalicoEvent(int event, CalicoPacket p) {
	}
	
	public void setBuffering(boolean bufferImage)
	{
		canvas.setBuffering(bufferImage);
	}
	
	public CalicoPacket getInfoPacket()
	{
		return CalicoPacket.getPacket(
			NetworkCommand.CANVAS_INFO,
			this.uuid,
			this.index
		);
	}
	
	public CalicoPacket[] getUpdatePackets()
	{
		ObjectArrayList<CalicoPacket> packetlist = new ObjectArrayList<CalicoPacket>();
		
		//packetlist.add(getInfoPacket());	

		
		long[] grouparr = getChildGroups();
		long[] bgearr = getChildStrokes();
		long[] arlist = getChildArrows();
		long[] ctrlist = getChildConnectors();
		
		// GROUPS
		if(grouparr.length>0)
		{
			// Send Group Info
			for(int i=0;i<grouparr.length;i++)
			{
				if (!CGroupController.groupdb.get(grouparr[i]).isPermanent)
					continue;
				// we only want to load root groups
				if(true /*CGroupController.groups.get(grouparr[i]).getParentUUID()==0L*/)
				{
					packetlist.addElements(packetlist.size(), CGroupController.groupdb.get(grouparr[i]).getUpdatePackets(false) );
					
					// Load the children of that group
//					long[] groupChildren = CGroupController.groups.get(grouparr[i]).getChildGroups();
//					if(groupChildren.length>0)
//					{
//						for(int cg=0;cg<groupChildren.length;cg++)
//						{
//							if(CGroupController.exists(groupChildren[cg]))
//							{
//								packetlist.addElements(packetlist.size(), CGroupController.groups.get(groupChildren[cg]).getUpdatePackets(false));
//							}
//						}
//					}
					
				}
			}
			
			// Parents
			for(int i=0;i<grouparr.length;i++)
			{
				if (!CGroupController.groupdb.get(grouparr[i]).isPermanent)
					continue;
				
				CalicoPacket[] packets = CGroupController.groupdb.get(grouparr[i]).getParentingUpdatePackets();
				for(int x=0;x<packets.length;x++)
				{
					packetlist.add(packets[x]);
				}
			}
		}
		
		
		// ARROWS
		if(arlist.length>0)
		{
			for(int i=0;i<arlist.length;i++)
			{
				CalicoPacket[] packets = CArrowController.arrows.get(arlist[i]).getUpdatePackets();
				if(packets!=null && packets.length>0)
				{
					packetlist.addElements(packetlist.size(), packets);
				}
			}
		}
		
		// CONNECTORS
		if(ctrlist.length>0)
		{
			for(int i=0;i<ctrlist.length;i++)
			{
				CalicoPacket[] packets = CConnectorController.connectors.get(ctrlist[i]).getUpdatePackets();
				if(packets!=null && packets.length>0)
				{
					packetlist.addElements(packetlist.size(), packets);
				}
			}
		}
		
		// Send the BGElement Parents
		if(bgearr.length>0)
		{
			for(int i=0;i<bgearr.length;i++)
			{
				CalicoPacket[] packets = CStrokeController.strokes.get(bgearr[i]).getUpdatePackets();
				if(packets!=null && packets.length>0)
				{
					packetlist.addElements(packetlist.size(), packets);
				}
			}
		}//
		
		packetlist.addElements(0, new CalicoPacket[] { CalicoPacket.getPacket(NetworkCommand.CANVAS_LOCK, this.uuid, lock_value, this.lock_last_set_by_user, this.lock_last_set_at_time)});
		
		return packetlist.toArray(new CalicoPacket[]{});
	}
	
	private class ContainedCanvas extends PCanvas
	{
		private boolean useBuffer = false;

		@Override
		protected void removeInputSources()
		{
			super.removeInputSources();
		}

		protected void sendInputEventToInputManager(InputEvent e, int type) 
		{
			if (type == MouseEvent.MOUSE_PRESSED || type == MouseEvent.MOUSE_DRAGGED || type == MouseEvent.MOUSE_RELEASED)
				return;
			
			//getRoot().getDefaultInputManager().processEventFromCamera(e, type, getCamera());
			CalicoDraw.processEventFromCamera(getRoot(), e, type, getCamera());
		}

		public void paintComponent(final Graphics g) {

			//Using the buffer simply means we don't actually update the canvas anymore
			if (!useBuffer)
			{
				super.paintComponent(g);
			}

		}
		
		public void setBuffering(final boolean bufferImage)
		{
			useBuffer = bufferImage;
		}

		public void setBounds(int x, int y, int w, int h)
		{
			if (!useBuffer)
			{
				//logger.debug("SET BOUNDS ("+x+","+y+","+w+","+h+")");
				CalicoDataStore.ScreenWidth = w;
				CalicoDataStore.ScreenHeight = h;
				super.setPreferredSize(new Dimension(w,h));
				super.setBounds(x,y,w,h); 
				//getCamera().getLayer(Layer.CONTENT.id).setBounds(x, y, w, h);
				CalicoDraw.setNodeBounds(getCamera().getLayer(Layer.CONTENT.id),x, y, w, h);
				
				//toolLayer.setBounds(x, y, w , h);
				CalicoDraw.setNodeBounds(toolLayer, x, y, w , h);
				//contentCamera.setBounds(x, y, w, h);
				CalicoDraw.setNodeBounds(contentCamera, x, y, w , h);
				if (CCanvas.this.watermarkLayer != null)
				{
					//CCanvas.this.watermarkLayer.setBounds(x, y, w, h);
					CalicoDraw.setNodeBounds(CCanvas.this.watermarkLayer, x, y, w , h);
				}
			}
		}

		public void repaint(final PBounds bounds)
		{
			//System.out.println("CALLING CCANVAS REPAINT");
			if (!useBuffer)
			{
			super.repaint(bounds);
			}
		}
	}
}//CCanvas
