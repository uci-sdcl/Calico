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
package calico.components.grid;

import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseListener;
import java.util.Date;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import sun.security.jca.GetInstance;

import calico.CalicoDataStore;
import calico.CalicoDraw;
import calico.CalicoOptions;
import calico.components.CCanvas;
import calico.components.menus.GridBottomMenuBar;
import calico.controllers.CCanvasController;
import calico.events.CalicoEventHandler;
import calico.events.CalicoEventListener;
import calico.input.CalicoMouseListener;
import calico.inputhandlers.CalicoInputManager;
import calico.inputhandlers.InputEventInfo;
import calico.networking.Networking;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;
import calico.perspectives.GridPerspective;
import calico.utils.Geometry;
import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.nodes.PImage;
import edu.umd.cs.piccolo.nodes.PText;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolox.nodes.PComposite;
import edu.umd.cs.piccolox.nodes.PLine;

/**
 * This is the grid system for calico
 *
 * @author Mitch Dempsey
 */
public class CGrid 
{
	private static final long serialVersionUID = 1L;
	
	public static final int GRID_EDGE_INSET = 30;
	
	//public static final int MODE_NONE = 0;
	//public static final int MODE_VIEWPORT = 1;
	
	private final ContainedCanvas canvas = new ContainedCanvas();

	private static Long2ReferenceOpenHashMap<CGridCell> cells = new Long2ReferenceOpenHashMap<CGridCell>();
	private PLayer cellLayer;

	public static int gwidth = 0;
	public static int gheight = 0;

	private int imgw = 0;
	private int imgh = 0;
	
	private GridBottomMenuBar menuBar = null;
	
	//public static int mode = MODE_NONE;

	private static CGrid instance = null;
	
	//attributes for dragging a cell to copy or move
	public static boolean draggingCell=false;
	private PImage pressedCellMainImage;
	private long cuidDraggedCanvas;
	//Copy canvas deletes from the destination cell and copies the contents of the selected cell (does not delete selected cell)
	public static final int COPY_CANVAS=1;	
	//Cut canvas removes the existing content from the selected cell
	public static final int CUT_CANVAS=2;
	
	
	public static final int NO_ACTION=0;
	public static int canvasAction=NO_ACTION;
	
	public static int moveDelta=1;
	public static int moveDelay=100;
	
	public static GridLoadListener gridLoadListener;
	
	public static int GridRows = 0;
	public static int GridCols = 0;
	
	public static int topHeaderIconHeight = 30;
	public static int leftHeaderIconWidth = 30;
	public static boolean render_zoom_canvas = false;
	public static Color item_border = Color.BLACK;
	public static Color viewport_background_color = new Color(235,235,235);//Color.WHITE;
	public static float viewport_background_transparency = 0.3f;
	
	public static synchronized CGrid getInstance(){
		if(instance==null){
			instance = new CGrid();
		}
		//instance.drawBottomToolbar();
//		instance.repaint();
		return instance;
	}
	
	public PCamera getCamera()
	{
		return canvas.getCamera();
	}
	
	public PLayer getLayer()
	{
		return canvas.getLayer();
	}
	
	public JComponent getComponent()
	{
		return canvas;
	}
	
	public void repaint()
	{
		canvas.repaint();
	}
	
	public void setBounds(int x, int y, int w, int h)
	{
		canvas.setBounds(x, y, w, h);
	}
	
	/**
	 * Creates a new isntance of the grid
	 */
	private CGrid()
	{
//		CGrid.exitButtonBounds = new Rectangle(CalicoDataStore.ScreenWidth-32,5,24,24);

		canvas.setPreferredSize(new Dimension(CalicoDataStore.ScreenWidth, CalicoDataStore.ScreenHeight));
		setBounds(0, 0, CalicoDataStore.ScreenWidth, CalicoDataStore.ScreenHeight);

		CalicoInputManager.addGridInputHandler();

		canvas.removeInputSources();

		canvas.addMouseListener(new CalicoMouseListener());
		canvas.addMouseMotionListener(new CalicoMouseListener());

		canvas.removeInputEventListener(canvas.getPanEventHandler());
		canvas.removeInputEventListener(canvas.getZoomEventHandler());
		this.gridLoadListener = new GridLoadListener();


//		PText pt = new PText(" Calico Grid ("+CalicoDataStore.Username+") ");
//		pt.setConstrainWidthToTextWidth(true);
//		pt.setFont(new Font("SansSerif", Font.BOLD, 20));
//		pt.setTextPaint(Color.BLACK);
//		pt.setBounds(5,5,500,29);
//		getLayer().addChild(0,pt);

		
		//create the exit image on top of the grid
//		try
//		{
//			PImage b = new PImage( new URL(CalicoIconManager.getIcon("exit")) );
//			b.setBounds(CGrid.exitButtonBounds);
//			getLayer().addChild(0,b);
//		}
//		catch(Exception e)
//		{
//		}



		repaint();

		drawBottomToolbar();
		
		int bottomMenuBarHeight = this.menuBar.getBounds().getBounds().height;

		int topHeaderIconHeight = CGrid.topHeaderIconHeight;
		int leftHeaderIconWidth = CGrid.leftHeaderIconWidth;
		
		try {
			while (CGrid.GridCols == 0 || CGrid.GridRows == 0)
				Thread.sleep(50);
			}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		imgw = (int) Math.ceil( (CalicoDataStore.ScreenWidth - leftHeaderIconWidth) / CGrid.GridCols );
		imgh = (int) Math.ceil( (CalicoDataStore.ScreenHeight - topHeaderIconHeight - bottomMenuBarHeight) / CGrid.GridRows );

		CGrid.gwidth = imgw;
		CGrid.gheight = imgh;
		
		

		//sets the initial sizes of the viewports in the viewport controller
		int cellindex = 0;

		long[] canvasuids = CCanvasController.getCanvasIDList();
		cellLayer = new PLayer();	

		/*for(int can=0;can<canvasuids.length;can++)
		{
			long canuuid = canvasuids[can];
			CGridCell img = new CGridCell(canuuid, cellindex,GRID_EDGE_INSET,GRID_EDGE_INSET,imgw,imgh);
			cellLayer.addChild(img);
			cells.put(canuuid, img);			
			cellindex++;
		}*/
		//getLayer().addChild(cellLayer);
		
		//Frankly, there is no need to add this one grid cell right now because they all get added later. 
		//Some other code above is useless but this takes care of what the user sees.
		//CalicoDraw.addChildToNode(getLayer(), cellLayer);
		repaint();
	}

	private void drawHeaderlabels() {
		int topHeaderIconHeight;
		int leftHeaderIconWidth;
		topHeaderIconHeight = CGrid.topHeaderIconHeight;
		leftHeaderIconWidth = CGrid.leftHeaderIconWidth;
		
		//add header labels... A... B... etc
		PText headerIcon;

		for (int i = 0; i < CGrid.GridCols; i++)
		{
			headerIcon = new PText(" " + ((char)('A' + i)) + " ");
			headerIcon.setConstrainWidthToTextWidth(true);
			headerIcon.setFont(new Font("SansSerif", Font.BOLD, 20));
			headerIcon.setTextPaint(Color.BLACK);
			Rectangle textBounds = Geometry.getTextBounds(headerIcon.getText(), headerIcon.getFont());
			headerIcon.setBounds(leftHeaderIconWidth + imgw*(i+1) - imgw/2 - textBounds.width/2,5,textBounds.width,textBounds.height);
			//getLayer().addChild(0,headerIcon);	
			CalicoDraw.addChildToNode(getLayer(), headerIcon, 0);
		}
		
		for (int i = 0; i < CGrid.GridRows; i++)
		{
			headerIcon = new PText(" " + ((char)('1' + i)) + " ");
			headerIcon.setConstrainWidthToTextWidth(true);
			headerIcon.setFont(new Font("SansSerif", Font.BOLD, 20));
			headerIcon.setTextPaint(Color.BLACK);
			Rectangle textBounds = Geometry.getTextBounds(headerIcon.getText(), headerIcon.getFont());
			headerIcon.setBounds(5,topHeaderIconHeight + imgh*(i+1) - imgh/2 - textBounds.width/2,textBounds.width,textBounds.height);
			//getLayer().addChild(0,headerIcon);	
			CalicoDraw.addChildToNode(getLayer(), headerIcon, 0);
		}
	}
	
//	asdfasfdasfdas
	public void refreshCells(){
		drawHeaderlabels();
		cells = new Long2ReferenceOpenHashMap<CGridCell>();
		//getLayer().removeChild(cellLayer);
		CalicoDraw.removeChildFromNode(getLayer(), cellLayer);
		cellLayer = new PLayer();
		int cellindex = 0;

		long[] canvasuids = CCanvasController.getCanvasIDList();
		for(int can=0;can<canvasuids.length;can++)
		{
			long canuuid = canvasuids[can];
			CGridCell img = new CGridCell(canuuid, cellindex,GRID_EDGE_INSET,GRID_EDGE_INSET,imgw,imgh);
			cellLayer.addChild(img);
			cells.put(canuuid, img);;
			cellindex++;
		}
		//getLayer().addChild(cellLayer);
		CalicoDraw.addChildToNode(getLayer(), cellLayer);
		//drawBottomToolbar();
		repaint();
	}
	
	public void drawBottomToolbar()
	{		
		GridBottomMenuBar temp = new GridBottomMenuBar(1L);
		//getCamera().addChild(temp);
		CalicoDraw.addChildToNode(getCamera(), temp);
		
		if(this.menuBar!=null)
		{
			//getCamera().removeChild(this.menuBar);
			CalicoDraw.removeChildFromNode(getCamera(), this.menuBar);
			this.menuBar = null;
		}
		
		this.menuBar = temp;
	}

	/**
	 * Updates the cells if they have changed
	 */
	public void updateCells()
	{
		for(int i=0;i<cells.size();i++)
		{
			if (cells.get(i) != null)
				
				cells.get(i).renderIfChanged();
				
		}
	}

	public boolean isPointOnMenuBar(Point point)
	{
		return (this.menuBar.isPointInside(point));
	}

	public void clickMenuBar(InputEventInfo event, Point point) {
		this.menuBar.clickMenu(event, point);	
	}
	
	/**
	 * gets the bounds of a cell 
	 * @param cuid
	 * @return
	 */
	public static PBounds getCellBounds(long cuid){
		CGridCell cell = cells.get(cuid);
		if(cell!= null){
			return cell.getBounds();		
		}
		return null;
	}
	
	public int getImgw() {
		return imgw;
	}

	public int getImgh() {
		return imgh;
	}
	
	public void addMouseListener(MouseListener listener)
	{
		canvas.addMouseListener(listener);
	}
	
	public void removeMouseListener(MouseListener listener)
	{
		canvas.removeMouseListener(listener);
	}
	
	/**
	 * Draws a slightly smaller, yellow background version of a cell in the given position
	 * @param cuid
	 * @param x
	 * @param y
	 */
	public void drawSelectedCell(long cuid, int x, int y){		
		if(!draggingCell){
			draggingCell=true;
			CCanvas canvas = CCanvasController.canvasdb.get(cuid);
			PCamera canvasCam =canvas.getContentCamera();		
//			canvasCam.removeChild(canvas.menuBar);
//			canvasCam.removeChild(canvas.topMenuBar);
			CCanvasController.loadCanvasImages(cuid);
			Image img = canvasCam.toImage(imgw-16, imgh-16, Color.YELLOW);	
			CCanvasController.unloadCanvasImages(cuid);
			
			pressedCellMainImage =  new PImage(img);
			
			pressedCellMainImage.setBounds(x-((imgw-24)/2), y-((imgh-24)/2), imgw-24, imgh-24);
			//pressedCellMainImage.setTransparency(CalicoOptions.group.background_transparency);
			CalicoDraw.setNodeTransparency(pressedCellMainImage, CalicoOptions.group.background_transparency);
			//getLayer().addChild(pressedCellMainImage);
			CalicoDraw.addChildToNode(getLayer(), pressedCellMainImage);
			cuidDraggedCanvas=cuid;			
		}
	}
	
	/**
	 * removes the dragged cell after copying or cutting
	 */
	public void removeDraggedCell(){
		if(pressedCellMainImage!=null){
			//getLayer().removeChild(pressedCellMainImage);
			CalicoDraw.removeChildFromNode(getLayer(), pressedCellMainImage);
			pressedCellMainImage=null;
			draggingCell=false;
			cuidDraggedCanvas=0l;
			canvasAction=NO_ACTION;
		}
	}
	
	/**
	 * moves a cell that is beeign dragged to cut or copy a canvas
	 * @param x the new x point to drag to
	 * @param y the new y point to drag to
	 */
	public void moveDraggedCell(int x, int y){
		//pressedCellMainImage.setBounds(x-((imgw-24)/2), y-((imgh-24)/2), imgw-24, imgh-24);		
		CalicoDraw.setNodeBounds(pressedCellMainImage, x-((imgw-24)/2), y-((imgh-24)/2), imgw-24, imgh-24);
	}


	/**
	 * deletes the contents of a canvas
	 * prompts the user to confirm before deleting
	 * @param cuid he canvas id to delete
	 */
	public void deleteCanvas(long cuid) {
		//send the message to delete the contents of the canvas
		/*Networking.send(CalicoPacket.getPacket(NetworkCommand.CANVAS_CLEAR, 
				cuid				
			));*/
		CCanvasController.clear(cuid);
	}
	/**
	 * executes the action to copy or cut a canvas from the dragged canvas id to the destination canvas in the point signaled by the event 
	 * @param ev the event where the button was released
	 */
	public void execActionCanvas( InputEventInfo ev){
		long cuidDest = CGrid.getCanvasAtPoint( ev.getPoint() );
		if(cuidDest!=0l){

			//send package to add the contents from the source to dest cell
			Networking.send(CalicoPacket.getPacket(NetworkCommand.CANVAS_COPY, 
					cuidDraggedCanvas, 
					cuidDest				
				));
			if(canvasAction==CUT_CANVAS){
				//if cut was the action removes the contents of the source canvas
				//send package to delete contents from source cell
				CCanvasController.clear(cuidDraggedCanvas);
				/*Networking.send(CalicoPacket.getPacket(NetworkCommand.CANVAS_CLEAR, 
						cuidDraggedCanvas				
					));*/
				if (CCanvasController.canvasdb.get(cuidDraggedCanvas).getLockValue() == true)
				{
					CCanvasController.lock_canvas(cuidDraggedCanvas, false, "clean move action", (new Date()).getTime());
					CCanvasController.lock_canvas(cuidDest, true, "clean move action", (new Date()).getTime());
				}
			}		
		}
	}
	
	private PLine drawDashedBorderLine(double x,double y, double x2, double y2)
	{
		PLine pline = new PLine();
		pline.addPoint(0, x, y);
		pline.addPoint(1, x2, y2);
		float[] dashFloat = {5f, 10f};
		pline.setStroke(new BasicStroke( 5.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER, 10.0f, dashFloat ,0f));
		pline.setStrokePaint( CGrid.item_border );
		return pline;
	}
	

	private PComposite clientListPopup = null;
	
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
		str.append("Clients on this server:");
		
		if(CCanvasController.getGlobalClientCount()==0)
		{
			str.append("\nNo clients are on a canvas");
		}
		else
		{
		
			long[] cuids = CCanvasController.canvasdb.keySet().toLongArray();
			for(int x=0; x < cuids.length; x++)
			{
				int[] clients = CCanvasController.canvasdb.get(cuids[x]).getClients();
				if(clients.length>0) {
					for(int i=0;i<clients.length;i++) {
						if(CalicoDataStore.clientInfo.containsKey(clients[i])) {
							str.append("\n"+CalicoDataStore.clientInfo.get(clients[i])+" ("+CGrid.getCanvasCoord(cuids[x])+")");
						} else {
//							str.append("\nUnknown_"+clients[i]+" ("+CCanvasController.canvasdb.get(cuids[x]).getGridCoordTxt()+")");
						}
						
					}
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
		CalicoDataStore.calicoObj.getContentPane().getComponent(0).repaint();
		//((PCanvas)CalicoDataStore.calicoObj.getContentPane().getComponent(0)).getCamera().repaint();
		CalicoDraw.repaint(((PCanvas)CalicoDataStore.calicoObj.getContentPane().getComponent(0)).getCamera());
		
		Rectangle newBounds = new Rectangle(bounds.x, bounds.y, bounds.width, bounds.height+padding);
	}
	
	public static int getCanvasRow(long cuid)
	{
		//Formula: Index - Row * NumColumns
		return CCanvasController.canvasdb.get(cuid).getIndex() - getCanvasColumn(cuid) * CGrid.GridCols;
	}
	
	public static int getCanvasColumn(long cuid)
	{
		return (int) Math.floor(CCanvasController.canvasdb.get(cuid).getIndex() / CGrid.GridCols);
	}
	
	public static String getCanvasCoord(long cuid)
	{
		int x = getCanvasRow(cuid);
		int y = getCanvasColumn(cuid);
		return (Character.valueOf( (char) (x+65)) ).toString()+""+y;
	}



	public void updateCell(long canvas) {
		if (cells.containsKey(canvas))
			cells.get(canvas).refreshImage();
	}
	
	public void updateCellPresenceText(long canvas) {
		cells.get(canvas).updatePresenceText();
	}
	
	public static void loadGrid()
	{
		CCanvasController.unloadCanvasImages(CCanvasController.getCurrentUUID());
		Networking.send(NetworkCommand.PRESENCE_LEAVE_CANVAS, CCanvasController.getCurrentUUID(), CCanvasController.getCurrentUUID());
		CCanvasController.setCurrentUUID(0l);
		
//		CalicoDataStore.gridObject = CGrid.getInstance();
		//CalicoDataStore.gridObject.refreshCells();
		CalicoDataStore.calicoObj.getContentPane().removeAll();
		
		Component[] comps = CalicoDataStore.calicoObj.getContentPane().getComponents();
		
		CGrid.getInstance().drawBottomToolbar();
		CalicoDataStore.calicoObj.getContentPane().add( CGrid.getInstance().getComponent() );
		
		for (int i = 0; i < comps.length; i++)
			CalicoDataStore.calicoObj.getContentPane().remove(comps[i]);
		
		CalicoDataStore.calicoObj.pack();
		CalicoDataStore.calicoObj.setVisible(true);
		CalicoDataStore.calicoObj.repaint();
		GridPerspective.getInstance().activate();
	}

	private class ContainedCanvas extends PCanvas
	{
		@Override
		protected void removeInputSources()
		{
			super.removeInputSources();
		}
	}
	
	public static long getCanvasAtPos(int x, int y)
	{
		// This will give the UUID for the canvas at the specific X/Y pos

		long[] cuids = CCanvasController.getCanvasIDList();
		for (int i = 0; i < cuids.length; i++)
		{
			int xpos = CGrid.getCanvasColumn(cuids[i]);
			int ypos = CGrid.getCanvasRow(cuids[i]);			
//			int r = getCanvasRow(cuids[i]);
//			int c = getCanvasColumn(cuids[i]);
			if (x == xpos && y == ypos)
			{
				return cuids[i];
			}
		}
		return 0L;
	}
	
	 public static long getCanvasAtPoint(Point point)
	 {
		 // This will give the UUID for the canvas at the specific X/Y pos
	
		 long[] cuids = CCanvasController.getCanvasIDList();
		 for (int i = 0; i < cuids.length; i++)
		 {
			 if (getCellBounds(cuids[i]).contains(point))
			 {
				 return cuids[i];
			 }
		 }
		 return 0L;
	 }
	

	
}//CGrid

class GridLoadListener implements CalicoEventListener
{
	private long prevStrokeCanvas = 0l;
	
	public GridLoadListener()
	{
		CalicoEventHandler.getInstance().addListener(NetworkCommand.STROKE_LOAD, this, CalicoEventHandler.PASSIVE_LISTENER);
		CalicoEventHandler.getInstance().addListener(NetworkCommand.CONSISTENCY_FINISH, this, CalicoEventHandler.PASSIVE_LISTENER);
	}

	@Override
	public void handleCalicoEvent(int event, CalicoPacket p) {
		// TODO Auto-generated method stub
		switch (event)
		{
			case NetworkCommand.STROKE_LOAD:
				p.rewind();
				p.getInt();
				p.getLong();
				long canvas = p.getLong();
				if (prevStrokeCanvas != canvas && prevStrokeCanvas != 0l)
					CGrid.getInstance().updateCell(prevStrokeCanvas);
				prevStrokeCanvas = canvas;
				break;
			case NetworkCommand.CONSISTENCY_FINISH:
				if (prevStrokeCanvas != 0l)
					CGrid.getInstance().updateCell(prevStrokeCanvas);
				SwingUtilities.invokeLater(
						new Runnable() { public void run() { 
							CalicoEventHandler.getInstance().removeListener(NetworkCommand.STROKE_LOAD, CGrid.gridLoadListener);
							CalicoEventHandler.getInstance().removeListener(NetworkCommand.CONSISTENCY_FINISH, CGrid.gridLoadListener);
							CGrid.gridLoadListener = null;
						}});

				break;	
		}
	}
	
}
