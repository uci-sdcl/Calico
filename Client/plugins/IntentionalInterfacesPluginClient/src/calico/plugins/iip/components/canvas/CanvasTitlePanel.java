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
package calico.plugins.iip.components.canvas;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Shape;
import java.awt.font.TextAttribute;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.SwingUtilities;

import org.apache.commons.lang.ArrayUtils;

import calico.Calico;
import calico.CalicoDataStore;
import calico.CalicoDraw;
import calico.CalicoOptions;
import calico.components.CCanvas;
import calico.components.CanvasViewScrap;
import calico.controllers.CCanvasController;
import calico.controllers.CGroupController;
import calico.events.CalicoEventHandler;
import calico.events.CalicoEventListener;
import calico.inputhandlers.CalicoAbstractInputHandler;
import calico.inputhandlers.CalicoInputManager;
import calico.inputhandlers.InputEventInfo;
import calico.inputhandlers.PressAndHoldAction;
import calico.inputhandlers.StickyItem;
import calico.inputhandlers.CalicoAbstractInputHandler.MenuTimer;
import calico.networking.netstuff.CalicoPacket;
import calico.perspectives.CalicoPerspective;
import calico.perspectives.CalicoPerspective.PerspectiveChangeListener;
import calico.perspectives.CanvasPerspective;
import calico.plugins.iip.IntentionalInterfacesNetworkCommands;
import calico.plugins.iip.components.CIntentionCell;
import calico.plugins.iip.components.IntentionPanelLayout;
import calico.plugins.iip.components.canvas.CanvasTitleDialog.Action;
import calico.plugins.iip.components.graph.IntentionGraph;
import calico.plugins.iip.controllers.CIntentionCellController;
import calico.plugins.iip.controllers.IntentionCanvasController;
import calico.plugins.iip.perspectives.IntentionalInterfacesPerspective;
import calico.utils.Ticker;
import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.event.PInputEventListener;
import edu.umd.cs.piccolo.nodes.PImage;
import edu.umd.cs.piccolo.nodes.PText;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PPaintContext;
import edu.umd.cs.piccolox.nodes.PComposite;

/**
 * Simple panel containing the canvas title, which is attached to the upper left corner of the Canvas View. Tapping the
 * panel pops up the <code>CanvasTitleDialog</code>, and this panel acts as the controller for that dialog. Title
 * changes are applied via <code>CIntentionCellController</code>. There is only one instance of this panel, and it is
 * moved from canvas to canvas as the user navigates. When the title of the current canvas changes, this panel expects a
 * call to <code>refresh()</code> so it can update the display.
 * 
 * @author Byron Hawkins
 */
public class CanvasTitlePanel implements StickyItem, CalicoEventListener, PerspectiveChangeListener
{
	public static CanvasTitlePanel getInstance()
	{
		return INSTANCE;
	}

	private static CanvasTitlePanel INSTANCE = new CanvasTitlePanel();

	public static final double PANEL_COMPONENT_INSET = 5.0;

	public static final double ROW_HEIGHT = 30.0;
	public static final double ROW_TEXT_INSET = 1.0;

	private final CanvasTitleNodeContainer titleNodeContainer;

	private final long uuid;
	private long canvas_uuid;

	private IntentionPanelLayout layout;

	private boolean initialized = false;
	private PLayer layer;
	
	private CanvasTitleNode activeTitleNode;
	
	private CanvasTitlePanel()
	{
		uuid = Calico.uuid();
		this.canvas_uuid = 0L;

		CalicoInputManager.addCustomInputHandler(uuid, new InputHandler());

		titleNodeContainer = new CanvasTitleNodeContainer(0l, CanvasTitleNodeType.TITLE);

		titleNodeContainer.setPaint(Color.white);
		CalicoInputManager.registerStickyItem(this);
		CalicoEventHandler.getInstance().addListener(IntentionalInterfacesNetworkCommands.CLINK_CREATE, this, CalicoEventHandler.PASSIVE_LISTENER);
		CalicoEventHandler.getInstance().addListener(IntentionalInterfacesNetworkCommands.CLINK_MOVE_ANCHOR, this, CalicoEventHandler.PASSIVE_LISTENER);
		CalicoEventHandler.getInstance().addListener(IntentionalInterfacesNetworkCommands.II_PERSPECTIVE_ACTIVATED, this, CalicoEventHandler.PASSIVE_LISTENER);
		CalicoEventHandler.getInstance().addListener(IntentionalInterfacesNetworkCommands.CIC_TAG, this, CalicoEventHandler.PASSIVE_LISTENER);
		CalicoEventHandler.getInstance().addListener(IntentionalInterfacesNetworkCommands.CIC_UNTAG, this, CalicoEventHandler.PASSIVE_LISTENER);
		CalicoEventHandler.getInstance().addListener(IntentionalInterfacesNetworkCommands.CIC_SET_TITLE, this, CalicoEventHandler.PASSIVE_LISTENER);
		CalicoPerspective.addListener(this);
		
		
		initialized = true;
	}

	@Override
	public long getUUID()
	{
		return uuid;
	}

	@Override
	public boolean containsPoint(Point p)
	{	
		if (titleNodeContainer.getBounds().contains(p))
			return true;
		
		for (CanvasTitleNodeContainer ctnc : titles)
		{
			if (ctnc.getBounds().contains(p))
				return true;
		}
		
		return false;
//		return panel.getBounds().contains(p);
	}

	public void moveTo(long canvas_uuid)
	{
		this.canvas_uuid = canvas_uuid;


		refresh();
//		rebuildTitleNodes();
//		CCanvasController.canvasdb.get(canvas_uuid).getCamera().addChild(panel);
//		CCanvasController.canvasdb.get(canvas_uuid).getCamera().addChild(titleNodeContainer);

	}

	public void refresh()
	{
//		if (!SwingUtilities.isEventDispatchThread())
//		{
			SwingUtilities.invokeLater(new Runnable() {
				public void run()
				{
					
//					refresh();
					updatePanelBounds();
					rebuildTitleNodes();
					updatePanelBounds();
					if (titleNodeContainer.getParent() != null && titleNodeContainer.getParent() != layer)
					{
						titleNodeContainer.getParent().removeChild(titleNodeContainer);
					}
					if (layer != null && IntentionGraph.getInstance().getFocus() != IntentionGraph.Focus.WALL
							/*&& titleNodeContainer.getParent() != layer*/)
						CalicoDraw.addChildToNode(layer,titleNodeContainer);
				}
			});
//			return;
//		}
//		else
//		{
//
////		panel.refresh();
//			
//			rebuildTitleNodes();
//			updatePanelBounds();
//			if (titleNodeContainer.getParent() != null && titleNodeContainer.getParent() != layer)
//			{
//				titleNodeContainer.getParent().removeChild(titleNodeContainer);
//			}
//			if (layer != null && IntentionGraph.getInstance().getFocus() != IntentionGraph.Focus.WALL
//					&& titleNodeContainer.getParent() != layer)
//				CalicoDraw.addChildToNode(layer,titleNodeContainer);
//		}
		
//		CalicoDraw.setVisible(panel, true);
//		panel.setVisible(true);
//		CalicoDraw.repaint(panel);
//		panel.repaint();
	}

	private void updatePanelBounds()
	{
//		double width = panel.calculateWidth();
//		double height = panel.calculateHeight();
//		layout.updateBounds(panel, width, height);
		layout.updateBounds(titleNodeContainer, (int)titleNodeContainer.calculateWidth(), (int)titleNodeContainer.calculateHeight());
		

//		CalicoDraw.repaint(panel);
		CalicoDraw.repaint(titleNodeContainer);
//		panel.repaint();
	}

	public void setLayout(IntentionPanelLayout layout)
	{
		this.layout = layout;
	}

	/**
	 * Represents the title panel in the Piccolo component hierarchy.
	 * 
	 * @author Byron Hawkins
	 */
	private class PanelNode extends PComposite
	{
		private final PText text = new PText();

		public PanelNode()
		{
			text.setConstrainWidthToTextWidth(true);
			text.setConstrainHeightToTextHeight(true);
			text.setFont(text.getFont().deriveFont(20f));

			CalicoDraw.addChildToNode(this, text);
//			addChild(text);
		}

		void tap(Point point)
		{
			CanvasTitleDialog.Action action = CanvasTitleDialog.getInstance().queryUserForLabel(
					CIntentionCellController.getInstance().getCellByCanvasId(canvas_uuid));

			if (action == Action.OK)
			{
				CIntentionCellController.getInstance().setCellTitle(CIntentionCellController.getInstance().getCellByCanvasId(canvas_uuid).getId(),
						CanvasTitleDialog.getInstance().getText(), false);
			}
		}

		double calculateWidth()
		{
			return text.getBounds().width + (2 * PANEL_COMPONENT_INSET);
		}

		double calculateHeight()
		{
			return text.getBounds().height;
		}

		void refresh()
		{
			if (canvas_uuid == 0L)
			{
				return;
			}
			
			CIntentionCell cell = CIntentionCellController.getInstance().getCellByCanvasId(canvas_uuid);
			
			String tag = "";
			if (cell.getIntentionTypeId() != -1)
				tag = " (" + IntentionCanvasController.getInstance().getIntentionType(cell.getIntentionTypeId()).getName() + ")";
			
			String titlePrefix = "";
			if (! CIntentionCellController.getInstance().isRootCanvas(canvas_uuid))
				titlePrefix = cell.getSiblingIndex() + ". ";
			
			String title = titlePrefix + cell.getTitle() + tag;
			
			long parentUUID = CIntentionCellController.getInstance().getCIntentionCellParent(canvas_uuid);
			
			while (parentUUID != 0l)
			{
				cell = CIntentionCellController.getInstance().getCellByCanvasId(parentUUID);
				tag = "";
				titlePrefix = "";
				if (! CIntentionCellController.getInstance().isRootCanvas(parentUUID))
					titlePrefix = cell.getSiblingIndex() + ". ";
				
				if (cell.getIntentionTypeId() != -1)
					tag = " (" + IntentionCanvasController.getInstance().getIntentionType(cell.getIntentionTypeId()).getName() +")";
				title = titlePrefix + cell.getTitle() + tag + " > " + title;
				
				parentUUID = CIntentionCellController.getInstance().getCIntentionCellParent(parentUUID);
			}

			text.setText("-----" + title);
		}

		@Override
		protected void layoutChildren()
		{
			if (!initialized)
			{
				return;
			}

			PBounds bounds = getBounds();

			text.recomputeLayout();
			PBounds textBounds = text.getBounds();
			text.setBounds(bounds.x + PANEL_COMPONENT_INSET, bounds.y + ROW_TEXT_INSET, textBounds.width, textBounds.getHeight());
		}
	}

	/**
	 * Only tap input is recognized, so it is only necessary to track the pressed state.
	 * 
	 * @author Byron Hawkins
	 */
	private enum InputState
	{
		IDLE,
		PRESSED,
		DRAGGING
	}

	/**
	 * Recognizes a press as a tap if it is not held longer than the <code>tapDuration</code> and no drag extends beyond
	 * the <code>dragThreshold</code>. The <code>state</code> is voluntarily read/write locked under
	 * <code>stateLock</code>.
	 * 
	 * @author Byron Hawkins
	 */
	private class InputHandler extends CalicoAbstractInputHandler
		implements PressAndHoldAction
	{
		private final Object stateLock = new Object();

		private final long tapDuration = 500L;
		private final double dragThreshold = 10.0;

		private InputState state = InputState.IDLE;
		private long pressTime = 0L;
		private Point pressAnchor;
		private long lastAction = 0;
		
		Point lastPoint, mouseDown, mouseUp;
		PImage pressedCellMainImage;
		int imgw;// = CanvasViewScrap.getDefaultWidth()*.25;
		int imgh;// = CanvasViewScrap.getDefaultHeight()*.25;
		long cuidDraggedCanvas = 0l;
		boolean draggingCell;

		@Override
		public void actionReleased(InputEventInfo event)
		{
			lastAction = 1l;
			mouseUp = event.getGlobalPoint();
			lastPoint = event.getGlobalPoint();
			synchronized (stateLock)
			{
				if (event.isRightButton())
				{
					pressAndHoldCompleted();
				}
				else if ((state == InputState.PRESSED) && ((System.currentTimeMillis() - pressTime) < tapDuration))
				{
//					panel.tap(event.getPoint());
					Point p = event.getGlobalPoint();
					if (titleNodeContainer.getGlobalBounds().contains(p))
					{
						long tappedCanvas = titleNodeContainer.getCanvasAt(p);
						if (isChildContainerVisible(tappedCanvas) &&  tappedCanvas == IntentionGraph.WALL)
						{
							IntentionGraph.getInstance().setFocusToWall();
							IntentionalInterfacesPerspective.getInstance().displayPerspective(IntentionGraph.WALL);
//							if (CalicoPerspective.Active.getCurrentPerspective() instanceof IntentionalInterfacesPerspective)
//							{
//								
//								SwingUtilities.invokeLater(
//										new Runnable() { public void run() { 
//											refresh();
//										}});
//								
//							}
//							else
								
							
						}
						else if ((isChildContainerVisible(tappedCanvas) || !titleNodeContainer.canvasAtPointHasChildren(p)) 
								&&  CIntentionCellController.getInstance().isRootCanvas(tappedCanvas))
						{
							// fix problem of loading from within II if ()
							if (CalicoPerspective.Active.getCurrentPerspective() instanceof IntentionalInterfacesPerspective)
								IntentionGraph.getInstance().setFocusToCluster(CIntentionCellController.getInstance().getClusterRootCanvasId(
									tappedCanvas), false);
							else
								IntentionalInterfacesPerspective.getInstance().displayPerspective(CIntentionCellController.getInstance().getClusterRootCanvasId(
									tappedCanvas));
						}
						else if ((isChildContainerVisible(tappedCanvas) || !titleNodeContainer.canvasAtPointHasChildren(p)) 
								&& tappedCanvas != CCanvasController.getCurrentUUID())
							CCanvasController.loadCanvas(tappedCanvas);
						else
							titleNodeContainer.tap(p);
					}
					
					for (int i = 0; i < titles.size(); i++)
					{
						CanvasTitleNodeContainer ctnc = titles.get(i);
						if (ctnc.getBounds().contains(p))
						{
							long tappedCanvas = ctnc.getCanvasAt(p);
							if ((isChildContainerVisible(tappedCanvas) || !ctnc.canvasAtPointHasChildren(p)) 
									&&  CIntentionCellController.getInstance().isRootCanvas(tappedCanvas))
								// fix problem of loading from within II if ()
								if (CalicoPerspective.Active.getCurrentPerspective() instanceof IntentionalInterfacesPerspective)
									IntentionGraph.getInstance().setFocusToCluster(CIntentionCellController.getInstance().getClusterRootCanvasId(
										tappedCanvas), false);
								else
									IntentionalInterfacesPerspective.getInstance().displayPerspective(CIntentionCellController.getInstance().getClusterRootCanvasId(
										tappedCanvas));
							else if (isChildContainerVisible(tappedCanvas) || !ctnc.canvasAtPointHasChildren(p))
								CCanvasController.loadCanvas(tappedCanvas);
							else
								ctnc.tap(p);
						}
					}
				}
				else if (state == InputState.DRAGGING)
				{
					if (draggingCell)
						removeDraggedCell();
					Point p = pressAnchor;
					if (titleNodeContainer.getBounds().contains(p))
					{
						createCanvasViewScrap(p, titleNodeContainer);
						
					}
					
					for (int i = 0; i < titles.size(); i++)
					{
						CanvasTitleNodeContainer ctnc = titles.get(i);
						if (ctnc.getBounds().contains(p))
						{
							createCanvasViewScrap(p, ctnc);
						}
					}
				}

				state = InputState.IDLE;
			}

			pressTime = 0L;

			CalicoInputManager.unlockHandlerIfMatch(uuid);
			
			final CanvasTitleNode oldNode = activeTitleNode;
			if (oldNode != null)
			{
				SwingUtilities.invokeLater(
						new Runnable() { public void run() { 
							if (oldNode != null)
							{
								oldNode.setPaint(Color.white);
								oldNode.repaint();
							}
						}});
			}
		}

		public void createCanvasViewScrap(Point p, CanvasTitleNodeContainer ctnc) {
			long targetCanvas = ctnc.getCanvasAt(p);
			if (targetCanvas > 0l && targetCanvas != this.canvas_uid)
			{
				long uuid = Calico.uuid();
				int width = (int)CanvasViewScrap.getDefaultWidth();
				int height = (int)CanvasViewScrap.getDefaultHeight();
				CGroupController.create_canvas_view_scrap(uuid, CCanvasController.getCurrentUUID(), targetCanvas, mouseUp.x - width/2, mouseUp.y - height/2,
						width, height);
			}
		}

		@Override
		public void actionDragged(InputEventInfo event)
		{
			lastPoint = event.getPoint();
			if (pressAnchor.distance(event.getGlobalPoint()) < dragThreshold)
			{
				// not a drag, completely ignore this event
				return;
			}

			synchronized (stateLock)
			{
				/*
				if (state == InputState.DRAGGING)
				{
					Point p = new Point(lastPoint);
					long currentCanvas = CCanvasController.getCurrentUUID();
					CCanvasController.canvasdb.get(currentCanvas).getLayer().getLocalToGlobalTransform(null).transform(p, p);
					
					if (draggingCell)
						moveDraggedCell(p.x, p.y);
				}
				else if (state == InputState.PRESSED || state == InputState.IDLE)
				{
					state = InputState.DRAGGING;
					
					Point p = pressAnchor;
					if (titleNodeContainer.getBounds().contains(p))
					{
						long targetCanvas = titleNodeContainer.getCanvasAt(p);
						if (targetCanvas > 0l && targetCanvas != this.canvas_uid)
						{
							drawSelectedCell(targetCanvas, lastPoint.x, lastPoint.y);
						}
						
					}
					
					for (int i = 0; i < titles.size(); i++)
					{
						CanvasTitleNodeContainer ctnc = titles.get(i);
						if (ctnc.getBounds().contains(p))
						{
							long targetCanvas = ctnc.getCanvasAt(p);
							if (targetCanvas > 0l && targetCanvas != this.canvas_uid)
							{
								drawSelectedCell(targetCanvas, lastPoint.x, lastPoint.y);
							}
						}
					}
					
				}
				
				else */if (state == InputState.PRESSED)
				{
					state = InputState.IDLE;
					pressTime = 0L;
				}
				
			}
		}

		@Override
		public void actionPressed(InputEventInfo event)
		{
			lastAction = 0l;
			mouseDown = event.getGlobalPoint();
			lastPoint = event.getGlobalPoint();
			enableHighlight(event.getGlobalPoint());
			synchronized (stateLock)
			{
//				PLayer layer = CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getLayer();
				PLayer layer = CanvasTitlePanel.this.layer;
				MenuTimer menuTimer = new CalicoAbstractInputHandler.MenuTimer(this, 0l, 100l, CalicoOptions.core.max_hold_distance, 1000,
						mouseDown, 0l, layer);
				if (event.isLeftButton())
					Ticker.scheduleIn(250, menuTimer);
				state = InputState.PRESSED;

				pressTime = System.currentTimeMillis();
				pressAnchor = event.getGlobalPoint();
			}
		}
		
		@Override
		public long getLastAction() {
			return lastAction;
		}

		@Override
		public Point getMouseDown() {
			return mouseDown;
		}

		@Override
		public Point getMouseUp() {
			return mouseUp;
		}

		@Override
		public Point getLastPoint() {
			
			return lastPoint;
		}

		@Override
		public double getDraggedDistance() {
			// TODO Auto-generated method stub
			return mouseDown.distance(lastPoint);
		}

		@Override
		public void pressAndHoldCompleted() {
			Point p = new Point(getLastPoint());
			long currentCanvas = CCanvasController.getCurrentUUID();
//			CCanvasController.canvasdb.get(currentCanvas).getLayer().getLocalToGlobalTransform(null).transform(p, p);
			layer.getLocalToGlobalTransform(null).transform(p, p);
			if (titleNodeContainer.getBounds().contains(p))
			{
				long targetCanvas = titleNodeContainer.getCanvasAt(p);
				if (targetCanvas == CCanvasController.getCurrentUUID())
				{
					setCanvasTitleText(canvas_uuid);
//					CCanvasController.loadCanvas(canvas_uuid);
				}
				else if (targetCanvas > 0l)
					setCanvasTitleText(targetCanvas);
//					CCanvasController.loadCanvas(targetCanvas);
//				else if (targetCanvas == CanvasTitleNode.WALL)
//				{
//					IntentionalInterfacesPerspective.getInstance().displayPerspective(CCanvasController.getCurrentUUID());
//				}
			}
			
			for (int i = 0; i < titles.size(); i++)
			{
				CanvasTitleNodeContainer ctnc = titles.get(i);
				if (ctnc.getBounds().contains(p))
				{
					long targetCanvas = ctnc.getCanvasAt(p);
					if (targetCanvas != 0l)
						setCanvasTitleText(targetCanvas);
//						CCanvasController.loadCanvas(targetCanvas);
				}
			}
			
			final CanvasTitleNode oldNode = activeTitleNode;
			if (oldNode != null)
			{
				SwingUtilities.invokeLater(
						new Runnable() { public void run() { 
							if (oldNode != null)
							{
								oldNode.setPaint(Color.white);
								oldNode.repaint();
							}
						}});
			}
		}

		@Override
		public void pressAndHoldAbortedEarly() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void openMenu(long potScrap, long group, Point point) {
			// TODO Auto-generated method stub
			
		}
		
		/**
		 * Draws a slightly smaller, yellow background version of a cell in the given position
		 * @param cuid
		 * @param x
		 * @param y
		 */
		private void drawSelectedCell(long cuid, int x, int y){		
//			if(!draggingCell){
				draggingCell=true;
				imgw = (int)(CanvasViewScrap.getDefaultWidth()*.5);
				imgh = (int)(CanvasViewScrap.getDefaultHeight()*.5);
				CCanvas canvas = CCanvasController.canvasdb.get(cuid);
				PCamera canvasCam =canvas.getContentCamera();		
//				canvasCam.removeChild(canvas.menuBar);
//				canvasCam.removeChild(canvas.topMenuBar);
				CCanvasController.loadCanvasImages(cuid);
				Image img = canvasCam.toImage(imgw-16, imgh-16, Color.white);
				img.getGraphics().drawRect(1, 1, img.getWidth(null) - 2, img.getHeight(null) - 2);
				CCanvasController.unloadCanvasImages(cuid);
				
				pressedCellMainImage =  new PImage(img);
				
				pressedCellMainImage.setBounds(x-((imgw-24)/2), y-((imgh-24)/2), imgw-24, imgh-24);
				//pressedCellMainImage.setTransparency(CalicoOptions.group.background_transparency);
//				CalicoDraw.setNodeTransparency(pressedCellMainImage, CalicoOptions.group.background_transparency);
				//getLayer().addChild(pressedCellMainImage);
//				CalicoDraw.addChildToNode(CCanvasController.canvasdb.get(canvas_uuid).getCamera(), pressedCellMainImage);
				CalicoDraw.addChildToNode(layer, pressedCellMainImage);
				cuidDraggedCanvas=cuid;			
//			}
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
		 * removes the dragged cell after copying or cutting
		 */
		public void removeDraggedCell(){
			if(pressedCellMainImage!=null){
				//getLayer().removeChild(pressedCellMainImage);
				CalicoDraw.removeChildFromNode(CCanvasController.canvasdb.get(canvas_uuid).getCamera(), pressedCellMainImage);
				pressedCellMainImage=null;
				cuidDraggedCanvas=0l;
				draggingCell = false;
			}
		}
	}
	
	@Override
	public void handleCalicoEvent(int event, CalicoPacket p) {
		
		if (event == IntentionalInterfacesNetworkCommands.CLINK_CREATE
				|| event == IntentionalInterfacesNetworkCommands.CLINK_MOVE_ANCHOR
				|| event == IntentionalInterfacesNetworkCommands.CIC_TAG
				|| event == IntentionalInterfacesNetworkCommands.CIC_UNTAG
				|| event == IntentionalInterfacesNetworkCommands.CIC_SET_TITLE)
		{
			refresh();
		}
		
	}

	@Override
	public void perspectiveChanged(CalicoPerspective perspective) {
		clearDisplayedStack(null);
		if (perspective instanceof CanvasPerspective)
		{
			layer = CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getLayer(CCanvas.Layer.TOOLS);
			refresh();
//			rebuildTitleNodes();
			CalicoInputManager.registerStickyItem(this);
		}
		else if (perspective instanceof IntentionalInterfacesPerspective
				&& IntentionGraph.getInstance().getFocus() == IntentionGraph.Focus.CLUSTER)
		{
			layer = IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.TOOLS);
			CalicoInputManager.registerStickyItem(this);
			refresh();
		}
		else
		{
			if (titleNodeContainer.getParent() != null)
				CalicoDraw.removeChildFromNode(titleNodeContainer.getParent(), titleNodeContainer);
			layer = null;
//			refresh();
			
			CalicoInputManager.unregisterStickyItem(this);
			
		}
		
		
		
		
	}
	
	private ArrayList<CanvasTitleNodeContainer> titles = new ArrayList<CanvasTitleNodeContainer>();

	private Point lastPoint;
	
	private void rebuildTitleNodes()
	{
		long title_canvas_uuid = canvas_uuid;
		if (CalicoPerspective.Active.getCurrentPerspective() instanceof IntentionalInterfacesPerspective
				&& IntentionGraph.getInstance().getFocus() == IntentionGraph.Focus.CLUSTER)
			title_canvas_uuid = IntentionGraph.getInstance().getClusterInFocus();
		
		if (title_canvas_uuid == 0l)
			return;
		
		//remove old nodes from canvas
		titleNodeContainer.removeAllChildren();

		if (titles != null)
		{
			clearDisplayedStack(null);
		}
		
		//iterate and build array of title nodes

		
		ArrayList<PText> titleNodes = new ArrayList<PText>();
		CanvasTitleNode ctNode = new CanvasTitleNode(title_canvas_uuid, CanvasTitleNodeType.TITLE);
		titleNodes.add(0, ctNode);
		
		long parentUUID = CIntentionCellController.getInstance().getCIntentionCellParent(title_canvas_uuid);
		while (parentUUID != 0l)
		{
			if (CIntentionCellController.getInstance().isRootCanvas(parentUUID)
					|| CIntentionCellController.getInstance().isRootCanvas(
							CIntentionCellController.getInstance().getCIntentionCellParent(parentUUID)))
			{
				titleNodes.add(0, getTitleNodeSpacer());
				ctNode = new CanvasTitleNode(parentUUID, CanvasTitleNodeType.TITLE);
				titleNodes.add(0, ctNode);
			}
			parentUUID = CIntentionCellController.getInstance().getCIntentionCellParent(parentUUID);
		}
		
		titleNodes.add(0, getTitleNodeSpacer());
		ctNode = new CanvasTitleNode(IntentionGraph.WALL, CanvasTitleNodeType.TITLE);
		titleNodes.add(0, ctNode);
		

		//lay them out from left to right
		//get their width and height
//		titleNodeContainer.setBounds(PANEL_COMPONENT_INSET, ROW_TEXT_INSET, width, maxHeight);
		
		int xPos = (int)titleNodeContainer.getBounds().getX();
		int yPos = (int)titleNodeContainer.getBounds().getY();
		for (PText n : titleNodes)
		{
			n.setX(xPos);
			n.setY(yPos);
			titleNodeContainer.addChild(n);
			xPos += n.getWidth() + CanvasTitleNodeContainer.CTNODE_SPACING;
		}
		
//		CalicoDraw.addChildToNode(CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getLayer(CCanvas.Layer.TOOLS), 
//				titleNodeContainer);
		if (layer != null)
		{
			CalicoDraw.addChildToNode(layer, titleNodeContainer);
		}
//		titles = new ArrayList<CanvasTitleNodeContainer>();
//		titles.add(titleContainer);
		
	}
	
	/**
	 * Defines if the children of CanvasTitleNode should go downward or rightward.
	 * @author nfmangano
	 *
	 */
	public enum CanvasTitleNodeType {
		TITLE(0), DROPDOWN(1);

		public final int id;

		private CanvasTitleNodeType(int id) {
			this.id = id;
		}
	}
	
	private class CanvasTitleNodeContainer extends PComposite
	{
		private long parentCanvas = 0l;
		CanvasTitleNodeType type;
		public static final int CTNODE_SPACING = 5;
		final static int CTNODE_VERTICAL_SPACING = 0;
		
		public CanvasTitleNodeContainer(long parentCanvas, CanvasTitleNodeType type)
		{
			this.parentCanvas = parentCanvas;
			this.type = type;
			setPaint(Color.white);
		}
		
		public long getParentCanvas()
		{
			return parentCanvas;
		}
		
		public void tap(Point p) {
			//Get which node was tapped
			int childIndex = getChildIndex(p);
			
			//show its children
			if (childIndex != -1)
			{
				boolean childContainerVisible = false;
				
				CanvasTitleNode child = (CanvasTitleNode)getChild(childIndex);
				for (CanvasTitleNodeContainer ctnc : titles)
					if (child.getCanvasId() == ctnc.getParentCanvas())
						childContainerVisible = true;
				
				clearDisplayedStack(this);
				if (!childContainerVisible)
					child.showChildren();
			}
		}
		
		public long getCanvasAt(Point p)
		{
			int childIndex = getChildIndex(p);
			
			if (childIndex != -1 
					&& getChild(childIndex) instanceof CanvasTitleNode)
			{
				return ((CanvasTitleNode)getChild(childIndex)).canvasId;
			}
			
			return 0;
		}
		
		public boolean canvasAtPointHasChildren(Point p)
		{
			int childIndex = getChildIndex(p);
			
			if (childIndex != -1 
					&& getChild(childIndex) instanceof CanvasTitleNode)
			{
				return ((CanvasTitleNode)getChild(childIndex)).hasChildren();
			}
			
			return false;
		}
		
		public void highlightTitleNode(Point p)
		{
			int childIndex = getChildIndex(p);
			
			if (childIndex != -1 
					&& getChild(childIndex) instanceof CanvasTitleNode)
			{
				final CanvasTitleNode oldNode = activeTitleNode;
				if (oldNode != null)
				{
					SwingUtilities.invokeLater(
							new Runnable() { public void run() { 
								if (oldNode != null)
								{
									oldNode.setPaint(Color.white);
									oldNode.repaint();
								}
							}});
				}
				
				activeTitleNode = ((CanvasTitleNode)getChild(childIndex));
				final CanvasTitleNode highlightedNode =  activeTitleNode;
				SwingUtilities.invokeLater(
						new Runnable() { public void run() {
							highlightedNode.setPaint(new Color(255,186,100));
							highlightedNode.repaint();
						}});

			}
			
		}

		public int getChildIndex(Point p) {
			int childIndex = -1;
			for (int i = 0; i < getChildrenCount(); i++)
			{
				if (getChild(i).getBounds().contains(p))
					if (getChild(i) instanceof CanvasTitleNode)
						childIndex = i;
			}
			return childIndex;
		}

		public double calculateWidth()
		{
			if (type == CanvasTitleNodeType.TITLE)
			{
				if (getChildrenCount() == 0)
					return 0;
				
				double width = getChild(0).getWidth();
				for (int i = 1; i < getChildrenCount(); i++)
				{
					width += CTNODE_SPACING + getChild(i).getWidth();	
				}
				
				return width;
				
			}
			else if (type == CanvasTitleNodeType.DROPDOWN)
			{
				if (getChildrenCount() == 0)
					return 0;
				
				double width = getChild(0).getWidth();
				for (int i = 1; i < getChildrenCount(); i++)
				{
					if (getChild(i).getWidth() > width)
						width = getChild(i).getWidth();	
				}
				
				return width;
			}
			
			return 0;
		}
		
		public double calculateHeight()
		{
			if (type == CanvasTitleNodeType.TITLE)
			{
				if (getChildrenCount() == 0)
					return 0;
				
				double height = getChild(0).getHeight();
				for (int i = 1; i < getChildrenCount(); i++)
				{
					if (getChild(i).getHeight() > height)
						height = getChild(i).getHeight();	
				}
				
				return height;
			}
			else if (type == CanvasTitleNodeType.DROPDOWN)
			{
				if (getChildrenCount() == 0)
					return 0;
				
				double height = getChild(0).getHeight();
				for (int i = 1; i < getChildrenCount(); i++)
				{
					height += CTNODE_VERTICAL_SPACING + getChild(i).getHeight();	
				}
				
				return height;
			}
			
			return 0;
		}
		
	}
	
	private class CanvasTitleNode extends PText
	{
		private boolean childrenVisible = false;
		
		private long canvasId;
		CanvasTitleNodeType type;
		
		public CanvasTitleNode(long canvasId, CanvasTitleNodeType t)
		{
			this.canvasId = canvasId;
			this.type = t;
			
			refresh();
		}
		
		public long getCanvasId()
		{
			return canvasId;
		}
		
		public void refresh()
		{
			CIntentionCell cell = CIntentionCellController.getInstance().getCellByCanvasId(this.canvasId);
			if (cell == null && canvasId != IntentionGraph.WALL)
			{
				System.out.println("Warning: cell is null in calico.plugins.iip.components.canvas.CanvasTitlePanel.CanvasTitleNode.refresh(), canvasId is " + canvasId);
				return;
			}
			
			String title = "";
			
			if (this.canvasId == IntentionGraph.WALL)
				title = "Wall";
			else
			{
				//get title prefix
				String titlePrefix = "";
				if (!CIntentionCellController.getInstance().isRootCanvas(this.canvasId))
					titlePrefix = cell.getTitlePrefix();
				else if (type == CanvasTitleNodeType.DROPDOWN)
					titlePrefix = " * ";
				
				//get tag name
				String tag = "";
				if (this.canvasId != IntentionGraph.WALL
						&& cell.getIntentionTypeId() != -1
						&& IntentionCanvasController.getInstance().getIntentionType(cell.getIntentionTypeId()) != null
						&& IntentionCanvasController.getInstance().getIntentionType(cell.getIntentionTypeId()).getName().compareTo("no tag") != 0)
					
					tag = " (" + IntentionCanvasController.getInstance().getIntentionType(cell.getIntentionTypeId()).getName() + ")";
				
				//get number of children
				String numChildren = "";
				if (this.canvasId != IntentionGraph.WALL
						&& type == CanvasTitleNodeType.DROPDOWN)
				{	
					int num = CIntentionCellController.getInstance().getCIntentionCellChildren(this.canvasId).length;
					if (num > 0
							&& CIntentionCellController.getInstance().isRootCanvas(
									CIntentionCellController.getInstance().getCIntentionCellParent(canvasId)))
						numChildren = " (" + getCICChildren().length + ")";	
				}
				
				if (CIntentionCellController.getInstance().isRootCanvas(this.canvasId) 
						&& type == CanvasTitleNodeType.DROPDOWN)
					title = titlePrefix + cell.getTitleWithoutPrefix() + tag + numChildren;
				else
					title = titlePrefix + cell.getTitle() + tag + numChildren;
			}
			

			/*
			String titlePrefix = "";
			if (! CIntentionCellController.getInstance().isRootCanvas(this.canvasId)
					&& this.canvasId != CanvasTitleNode.WALL)
				titlePrefix = cell.getSiblingIndex() + ". ";
			else if (this.canvasId != CanvasTitleNode.WALL)
			{
				int clusterIndex = cell.getClusterIndex();
				if (clusterIndex != -1)
					titlePrefix = "C" + clusterIndex + ". ";
				else
					titlePrefix = "C#. ";
			}
			*/

			
			this.setText(title);
			this.setConstrainWidthToTextWidth(true);
			this.setConstrainHeightToTextHeight(true);
			this.setFont(this.getFont().deriveFont(20f));
//			Map<TextAttribute, Object> fontAttributes = new HashMap<TextAttribute, Object>();
//			fontAttributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
//			this.setFont(this.getFont().deriveFont(fontAttributes));
			this.recomputeLayout();
		}
		
		public boolean isChildContainerVisible()
		{
			 for (CanvasTitleNodeContainer c : titles)
			 {
				 if (c.getParentCanvas() == canvasId)
					 return true;
			 }
			 return false;
		}
		
		public boolean hasChildren()
		{
			long[] children;
			if (this.canvasId == IntentionGraph.WALL)
				children = IntentionGraph.getInstance().getRootsOfAllClusters();
			else if (CIntentionCellController.getInstance().isRootCanvas(
									CIntentionCellController.getInstance().getCIntentionCellParent(canvasId)))
				children = getCICChildren();
			else
				children = new long[0];
			
			return children.length > 0;
		}
		
		public void showChildren()
		{
			//initialize container
			CanvasTitleNodeContainer container = new CanvasTitleNodeContainer(this.canvasId, CanvasTitleNodeType.DROPDOWN);
			
			//create children immediately below this node
			layoutChildren(container);
			
			//add to layer
//			CCanvasController.canvasdb.get(canvas_uuid).getCamera().addChild(container);
			CalicoDraw.addChildToNode(layer, container);
			titles.add(container);
			CalicoDraw.repaint(container);
			childrenVisible = true;
		}

		public void layoutChildren(CanvasTitleNodeContainer container) {
			if (type == CanvasTitleNodeType.TITLE)
			{
				long[] children;
				if (this.canvasId == IntentionGraph.WALL)
					children = IntentionGraph.getInstance().getRootsOfAllClusters();
				else
					children = getCICChildren();
				
				CanvasTitleNode[] titleNodes = new CanvasTitleNode[children.length];
				int xPos = (int)getX();
				int yPosOriginal = (int)getY() + (int)getHeight() + CanvasTitleNodeContainer.CTNODE_VERTICAL_SPACING; 
				int yPos = yPosOriginal;
				for (int i = 0; i < titleNodes.length; i++)
				{	
					titleNodes[i] = new CanvasTitleNode(children[i], CanvasTitleNodeType.DROPDOWN);
					titleNodes[i].setX(xPos);
					titleNodes[i].setY(yPos);
					container.addChild(titleNodes[i]);
					
					yPos += titleNodes[i].getHeight() + CanvasTitleNodeContainer.CTNODE_VERTICAL_SPACING;
				}
				container.setBounds(getX(), yPosOriginal, container.calculateWidth(), container.calculateHeight());
			}
			else if (type == CanvasTitleNodeType.DROPDOWN)
			{
				long[] children = getCICChildren();
//				long[] children = CIntentionCellController.getInstance().getCIntentionCellChildren(this.canvasId);
				
				int width = (int)getWidth();
				if (getParent() instanceof CanvasTitleNodeContainer)
					width = (int)(((CanvasTitleNodeContainer)getParent()).calculateWidth());
				
				CanvasTitleNode[] titleNodes = new CanvasTitleNode[children.length];
				int xPosOriginal = (int)getX() + width + CanvasTitleNodeContainer.CTNODE_SPACING;
				int yPosOriginal = (int)getY();
				int xPos = xPosOriginal;
				int yPos = yPosOriginal;
				
				PText spacer = getTitleNodeSpacer();
				spacer.setX(xPos);
				spacer.setY(yPos);
				container.addChild(spacer);
				xPos += spacer.getWidth();
				
				for (int i = 0; i < titleNodes.length; i++)
				{	
					titleNodes[i] = new CanvasTitleNode(children[i], CanvasTitleNodeType.DROPDOWN);
					titleNodes[i].setX(xPos);
					titleNodes[i].setY(yPos);
					container.addChild(titleNodes[i]);
					
					yPos += titleNodes[i].getHeight() + CanvasTitleNodeContainer.CTNODE_VERTICAL_SPACING;
				}
				container.setBounds(xPosOriginal, yPosOriginal, container.calculateWidth() + spacer.getWidth(), container.calculateHeight());
			}
		}

		private long[] getCICChildren() {
			if (CIntentionCellController.getInstance().isRootCanvas(this.canvasId))
			{
				return CIntentionCellController.getInstance().getCIntentionCellChildren(this.canvasId);
			}
			ArrayList<Long> children = new ArrayList<Long>();
			long[] traversalChildren = CIntentionCellController.getInstance().getCIntentionCellChildren(this.canvasId);
			while (traversalChildren.length > 0)
			{
				children.add(new Long(traversalChildren[0]));
				traversalChildren = CIntentionCellController.getInstance().getCIntentionCellChildren(traversalChildren[0]);
			}
			
			return ArrayUtils.toPrimitive(children.toArray(new Long[0]));
		}
	}
	
	private void clearDisplayedStack(CanvasTitleNodeContainer upToThisContainer)
	{
		for (int i = titles.size()-1; i >= 0; i--)
		{
			if (upToThisContainer == null 
					|| titles.get(i).getParentCanvas() != upToThisContainer.getParentCanvas())
			{
				CalicoDraw.setVisible(titles.get(i), false);
				if (titles.size() > i)
					titles.get(i).getParent().removeChild(titles.get(i));
//				CalicoDraw.removeChildFromNode(CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getLayer(CCanvas.Layer.TOOLS),
//						titles.get(i));
				if (titles.size() > i)
					titles.remove(i);
			}
			else
				break;
		}
	}
	
	public boolean isChildContainerVisible(long parentCanvasId)
	{
		 for (CanvasTitleNodeContainer c : titles)
		 {
			 if (c.getParentCanvas() == parentCanvasId)
				 return true;
		 }
		 return false;
	}
	
	private PText getTitleNodeSpacer()
	{
		PText spacer = new PText();
		spacer.setText(" > ");
		spacer.setConstrainWidthToTextWidth(true);
		spacer.setConstrainHeightToTextHeight(true);
		spacer.setFont(spacer.getFont().deriveFont(20f));
		spacer.recomputeLayout();
		return spacer;
		
	}

	public static void setCanvasTitleText(long canvasId) {
		CanvasTitleDialog.Action action = CanvasTitleDialog.getInstance().queryUserForLabel(
				CIntentionCellController.getInstance().getCellByCanvasId(canvasId));

		if (action == Action.OK)
		{
			CIntentionCellController.getInstance().setCellTitle(CIntentionCellController.getInstance().getCellByCanvasId(canvasId).getId(),
					CanvasTitleDialog.getInstance().getText(), false);
		}
	}
	
	private void enableHighlight(Point p)
	{
		if (titleNodeContainer.getBounds().contains(p))
		{
			titleNodeContainer.highlightTitleNode(p);
		}
		
		for (int i = 0; i < titles.size(); i++)
		{
			CanvasTitleNodeContainer ctnc = titles.get(i);
			if (ctnc.getBounds().contains(p))
			{
				ctnc.highlightTitleNode(p);
			}
		}
	}


}
