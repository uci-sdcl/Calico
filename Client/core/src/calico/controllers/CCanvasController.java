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
package calico.controllers;

import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import calico.Calico;
import calico.CalicoDataStore;
import calico.CalicoDraw;
import calico.CalicoOptions;
import calico.components.CCanvas;
import calico.components.CCanvas.ContentContributor;
import calico.components.CCanvasWatermark;
import calico.components.CConnector;
import calico.components.CGroup;
import calico.components.CGroupImage;
import calico.components.CStroke;
import calico.components.arrow.CArrow;
import calico.components.grid.CGrid;
import calico.components.piemenu.PieMenu;
import calico.components.piemenu.PieMenuButton;
import calico.controllers.CHistoryController.Frame;
import calico.events.CalicoEventHandler;
import calico.inputhandlers.CalicoInputManager;
import calico.modules.MessageObject;
import calico.networking.Networking;
import calico.networking.PacketHandler;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;
import calico.perspectives.CalicoPerspective;
import calico.perspectives.CanvasPerspective;
import calico.perspectives.CalicoPerspective.Active;
import calico.plugins.CalicoPluginManager;
import calico.utils.TaskBuffer;
import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PNode;

/**
 * This handles all canvas requests
 * 
 * @author Mitch Dempsey
 */
public class CCanvasController
{
	private static Logger logger = Logger.getLogger(CCanvasController.class.getName());

	public static Long2ReferenceOpenHashMap<CCanvas> canvasdb = new Long2ReferenceOpenHashMap<CCanvas>();

	public static Long2ReferenceOpenHashMap<PCamera> canvasCameras = new Long2ReferenceOpenHashMap<PCamera>();

	private static final List<CCanvas.ContentContributor> contentContributors = new ArrayList<CCanvas.ContentContributor>();

	private static final ContentContributionController contributionController = new ContentContributionController();

	public static CGroup currentGroup = null;

	private static long currentCanvasUUID = 0L;

	private static long lastActiveCanvasUUID = 0L;

	// If we are changing the state of the canvas, save the canvasID in case it changes
	// mid state change.
	private static long stateChangeCanvasUUID = 0;

	// Does nothing right now
	public static void setup()
	{
		canvasdb.clear();
		canvasCameras.clear();
	}

	public static boolean exists(long uuid)
	{
		return canvasdb.containsKey(uuid);
	}

	public static void no_notify_clear(long uuid)
	{
		if (!exists(uuid))
		{
			return;
		}

		contributionController.clearCanvas(uuid);
//		CalicoDataStore.gridObject.updateCell(uuid);
	}

	public static void clear(long uuid)
	{
		CalicoPacket p = CalicoPacket.getPacket(NetworkCommand.CANVAS_CLEAR, uuid);
		p.rewind();
		PacketHandler.receive(p);
		Networking.send(p);
	}

//	public static CCanvas getCanvasByIndex(int index)
//	{
//		for (CCanvas canvas : canvasdb.values())
//		{
//			if (canvas.getIndex() == index)
//			{
//				return canvas;
//			}
//		}
//		return null;
//	}

	public static Color getActiveCanvasBackgroundColor()
	{
		return CalicoOptions.canvas.background_color;
	}

	public static void canvasModeChanged()
	{
		CCanvasWatermark watermark = CCanvasWatermark.InputModeWatermarks.get(CalicoDataStore.Mode);
		for (CCanvas canvas : CCanvasController.canvasdb.values())
		{
			canvas.setWatermarkLayer(watermark);
		}
	}

	public static void no_notify_clear_for_state_change(long uuid)
	{
		// TODO: This should somehow cache the groups
		setStateChangeUUID(uuid);
		// RepaintManager.currentManager(canvasdb.get(uuid)).
		// canvasdb.get(uuid).getLayer().setVisible(false);
		// CalicoDraw.setVisible(canvasdb.get(uuid).getLayer(), false);
		// canvasdb.get(uuid).setEnabled(false);
		CalicoInputManager.setEnabled(false);
		canvasdb.get(uuid).setBuffering(true);

		// canvasdb.get(uuid).setEnabled(false);
		// canvasdb.get(uuid).setDoubleBuffered(true);

		// canvasdb.get(uuid).setInteracting(true);
		// canvasdb.get(uuid).setIgnoreRepaint(true);
		canvasdb.get(uuid).resetLock();

		long[] groups = canvasdb.get(uuid).getChildGroups();
		long[] strokes = canvasdb.get(uuid).getChildStrokes();
		long[] arrows = canvasdb.get(uuid).getChildArrows();
		long[] connectors = canvasdb.get(uuid).getChildConnectors();

		if (strokes.length > 0)
		{
			for (int i = 0; i < strokes.length; i++)
			{
				// CCanvasController.canvasdb.get(uuid).getLayer().removeChild(CStrokeController.strokes.get(strokes[i]));
				CalicoDraw.removeChildFromNode(CCanvasController.canvasdb.get(uuid).getLayer(), CStrokeController.strokes.get(strokes[i]));
				CStrokeController.no_notify_delete(strokes[i]);
			}
		}

		if (arrows.length > 0)
		{
			for (int i = 0; i < arrows.length; i++)
			{
				// CCanvasController.canvasdb.get(uuid).getLayer().removeChild(CArrowController.arrows.get(arrows[i]));
				CalicoDraw.removeChildFromNode(CCanvasController.canvasdb.get(uuid).getLayer(), CArrowController.arrows.get(arrows[i]));
				CArrowController.no_notify_delete(arrows[i]);
			}
		}

		if (connectors.length > 0)
		{
			for (int i = 0; i < connectors.length; i++)
			{
				// CCanvasController.canvasdb.get(uuid).getLayer().removeChild(CConnectorController.connectors.get(connectors[i]));
				CalicoDraw.removeChildFromNode(CCanvasController.canvasdb.get(uuid).getLayer(), CConnectorController.connectors.get(connectors[i]));
				CConnectorController.no_notify_delete(connectors[i]);
			}
		}

		if (groups.length > 0)
		{
			for (int i = groups.length - 1; i >= 0; i--)
			{
				CGroupController.no_notify_delete(groups[i]);
			}
		}

		// CCanvasController.canvasdb.get(uuid).repaint();
		// CalicoDraw.repaint(CCanvasController.canvasdb.get(uuid).getCamera());
		contributionController.contentChanged(uuid);
//		CalicoDataStore.gridObject.updateCell(uuid);

	}

	public static void no_notify_state_change_complete(long uuid)
	{
		// just repaint it
		// canvasdb.get(uuid).validate();
		Networking.timesFailed = 0;
		Networking.synchroized = true;
		if (CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()) != null)
			CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).drawMenuBars();
		// canvasdb.get(uuid).setEnabled(true);
		// canvasdb.get(uuid).setInteracting(false);
		// canvasdb.get(uuid).setIgnoreRepaint(false);
		// canvasdb.get(uuid).setDoubleBuffered(false);
		CalicoInputManager.setEnabled(true);
		canvasdb.get(uuid).setBuffering(false);
		// canvasdb.get(uuid).setEnabled(true);
		setStateChangeUUID(0l);
		// canvasdb.get(uuid).getLayer().setVisible(true);
		// CalicoDraw.setVisible(canvasdb.get(uuid).getLayer(), true);

		// canvasdb.get(uuid).repaint();
		CalicoDraw.repaint(CCanvasController.canvasdb.get(uuid).getCamera());

		CalicoEventHandler.getInstance().fireEvent(NetworkCommand.STATUS_SENDING_LARGE_FILE_FINISHED,
				CalicoPacket.getPacket(NetworkCommand.STATUS_SENDING_LARGE_FILE_FINISHED, 1d, 1d, ""));
	}

	public static long[] getCanvasIDList()
	{
		return canvasdb.keySet().toLongArray();
	}

	public static long getCurrentUUID()
	{
		return currentCanvasUUID;
	}

	public static void setCurrentUUID(long u)
	{
		currentCanvasUUID = u;
	}

	public static long getLastActiveUUID()
	{
		return lastActiveCanvasUUID;
	}

	public static void setLastActiveUUID(long u)
	{
		lastActiveCanvasUUID = u;
	}

	public static long getStateChangeUUID()
	{
		return stateChangeCanvasUUID;
	}

	public static void setStateChangeUUID(long u)
	{
		stateChangeCanvasUUID = u;
	}

	public static void windowResized()
	{
		// processese the resize of the windows
		redrawMenuBars();
	}

	public static void redrawMenuBars()
	{
		redrawMenuBars(getCurrentUUID());
	}

	public static void redrawMenuBars(long uuid)
	{
		if (!canvasdb.containsKey(uuid))
		{
			System.out.println("Warning, attempting to draw menu bar on a canvas that doesn't exist! Key: " + uuid);
			return;
		}

		canvasdb.get(uuid).drawMenuBars();

		// TODO do this also for grid and for viewport views

	}

	public static void redrawToolbar_clients(long uuid)
	{
		if (!canvasdb.containsKey(uuid))
		{
			System.out.println("Warning, attempting to draw menu bar on a canvas that doesn't exist! Key: " + uuid);
			return;
		}

		canvasdb.get(uuid).redrawToolbar_clients();
	}

	public static int getGlobalClientCount()
	{
		int totalClients = 0;

		long[] cuids = CCanvasController.canvasdb.keySet().toLongArray();
		for (int x = 0; x < cuids.length; x++)
		{
			totalClients = totalClients + CCanvasController.canvasdb.get(cuids[x]).getClients().length;
		}
		return totalClients;
	}

	/**
	 * Reloads the top menu bar (this shows state changes and stuff)
	 */
	public static void redrawTopMenubar()
	{
		canvasdb.get(getCurrentUUID()).drawMenuBars();
		// TODO do this also for grid and for viewport views
	}
	
	public static void loadCanvas(long uuid)
	{
		CHistoryController.getInstance().push(new HistoryFrame(uuid));

		// if (CCanvasController.getCurrentUUID() == uuid)
		// return;
		//
		// CCanvasController.setCurrentUUID(uuid);
		// restore scale of old canvas
		if (CCanvasController.currentCanvasUUID != 0l)
			CCanvasController.canvasdb.get(CCanvasController.currentCanvasUUID).getLayer().setScale(1.0d);

		Calico cal = CalicoDataStore.calicoObj;

		// cal.getContentPane().removeAll();

		Component[] comps = CalicoDataStore.calicoObj.getContentPane().getComponents();

		// This code here is to fix the bug where the viewport messes up the
		// cameras of the canvas
		CCanvas canvas = CCanvasController.canvasdb.get(uuid);
		canvas.drawMenuBars();

		//get ratio of screen dimensions to server screen dimensions
		Rectangle boundsOfCanvas = CCanvasController.canvasdb.get(uuid).getBounds();
		double widthRatio = (double)boundsOfCanvas.width / CalicoDataStore.serverScreenWidth;
		double heightRatio = (double)boundsOfCanvas.height / CalicoDataStore.serverScreenHeight;
		
		double ratio = Math.min(widthRatio, heightRatio);
		CCanvasController.canvasdb.get(uuid).getLayer().setScale(ratio);
		
		// //get bounds of contents on canvas
		// Rectangle boundsOfChildren =
		// CCanvasController.canvasdb.get(uuid).getLayer().getUnionOfChildrenBounds(null).getBounds();
		// Rectangle boundsOfCanvas = CCanvasController.canvasdb.get(uuid).getBounds();
		//
		// //if bounds of contents on canvas is larger than screen, then zoom out.
		// if ((boundsOfCanvas.width < boundsOfChildren.width || boundsOfCanvas.height < boundsOfChildren.height) &&
		// !CalicoDataStore.isInViewPort)
		// {
		// double widthScale = (double)boundsOfCanvas.width / (double)boundsOfChildren.width;
		// double heightScale = (double)boundsOfCanvas.height / (double)boundsOfChildren.height;
		//
		// double scale = (widthScale < heightScale)?widthScale:heightScale;
		// scale *= .95d;
		//
		// CCanvasController.canvasdb.get(uuid).getLayer().setScale(scale);
		// }

		if (CCanvasController.canvasCameras.containsKey(uuid))
		{
			canvas.setCamera(CCanvasController.canvasCameras.get(uuid));
		}
		// end of bug fix

		for (int i = 0; i < comps.length; i++)
			CalicoDataStore.calicoObj.getContentPane().remove(comps[i]);

		cal.getContentPane().add(canvas.getComponent());

		cal.setJMenuBar(null);
		cal.pack();
		loadCanvasImages(uuid);
		initializeCanvas(uuid);
		cal.setVisible(true);
		CanvasPerspective.getInstance().activate();
		cal.repaint();

		// initializeCanvas(uuid);

		cal.requestFocus();
	}

	public static void removeCanvas(final long uuid)
	{
		CHistoryController.getInstance().purgeFrames(new CHistoryController.FrameSelector() {
			@Override
			public boolean match(Frame frame)
			{
				return (frame instanceof HistoryFrame) && (((HistoryFrame) frame).canvasId == uuid);
			}
		});

		canvasdb.remove(uuid);

		if (currentCanvasUUID == uuid)
		{
			// randomly choose a canvas to be current, so things don't crash
			currentCanvasUUID = CCanvasController.canvasdb.values().iterator().next().uuid;

			if (CanvasPerspective.getInstance().isActive())
				CalicoPerspective.Registry.activateNavigationPerspective();
		}
	}

	public static void initializeCanvas(long uuid)
	{
		// Networking.send(NetworkCommand.CLICK_CANVAS, cellid);
		if (CCanvasController.getCurrentUUID() != 0L)
		{

			Networking.send(NetworkCommand.PRESENCE_LEAVE_CANVAS, CCanvasController.getCurrentUUID(), uuid);
		}

		long tempUUID = getLastActiveUUID();
		CCanvasController.setLastActiveUUID(uuid);
		if (tempUUID != 0L)
		{
//			CGrid.getInstance().updateCell(tempUUID);      
		}
		CCanvasController.setCurrentUUID(uuid);

		Networking.send(NetworkCommand.PRESENCE_VIEW_CANVAS, uuid);

		// Why was this even here? -Wayne
		// Networking.send(NetworkCommand.PRESENCE_CANVAS_USERS, uuid);

		CArrowController.setOutstandingAnchorPoint(null);
		// calico.events.CalicoEventHandler.getInstance().fireEvent(NetworkCommand.PRESENCE_CANVAS_USERS,
		// CalicoPacket.getPacket(NetworkCommand.PRESENCE_CANVAS_USERS, uuid));
		calico.events.CalicoEventHandler.getInstance().fireEvent(NetworkCommand.VIEWING_SINGLE_CANVAS,
				CalicoPacket.getPacket(NetworkCommand.VIEWING_SINGLE_CANVAS, uuid));
		CalicoPluginManager.FireEvent(new calico.plugins.events.ui.ViewSingleCanvas(uuid));

		// Make sure the Menu bar has all been redrawn and updated
		// canvas.drawToolbar();
		// canvas.menuBar.invalidateFullBounds();

//		MessageObject.showNotice("Viewing canvas " + CGrid.getCanvasCoord(uuid));
	}

	// Load all images in the canvas to memory to they are visible
	public static void loadCanvasImages(long uuid)
	{
		if (Networking.connectionState == Networking.ConnectionState.Connecting)
			return;
		
		// System.out.println("loading canvas: " + uuid);
		if (uuid != 0)
		{
			long[] groups = CCanvasController.canvasdb.get(uuid).getChildGroups();
			for (int i = 0; i < groups.length; i++)
			{
				CGroup temp = CGroupController.groupdb.get(groups[i]);
				if (temp instanceof CGroupImage)
				{
					((CGroupImage) temp).setImage();
				}
			}
		}
	}

	// Remove all image in the canvas from memory as they are not needed right now
	public static void unloadCanvasImages(long uuid)
	{
		// System.out.println("unloading canvas: " + uuid);
		if (uuid != 0)
		{
			long[] groups = CCanvasController.canvasdb.get(uuid).getChildGroups();
			for (int i = 0; i < groups.length; i++)
			{
				CGroup temp = CGroupController.groupdb.get(groups[i]);
				if (temp instanceof CGroupImage)
				{
					((CGroupImage) temp).unloadImage();
				}
			}
		}
	}

	/**
	 * @deprecated
	 * @param uuid
	 */
	public static void drawCanvasMenubars(long uuid)
	{

	}

	public static Image image(long uuid)
	{
		return canvasdb.get(uuid).toImage();
	}
	
	public static Image image(long uuid, int x, int y)
	{
		return canvasdb.get(uuid).toImage(x,y);
	}

	public static int get_signature(long uuid)
	{
		return canvasdb.get(uuid).getSignature();
	}

	public static void no_notify_add_child_stroke(long cuid, long uuid, boolean addToPiccolo)
	{
		if (!canvasdb.containsKey(cuid))
		{
			logger.warn("Attempting to add a stroke to non-existing canvas: " + cuid + " !!");
			return;
		}

		canvasdb.get(cuid).addChildStroke(uuid);

		// add to the painter
		if (addToPiccolo)
		{
			// canvasdb.get(cuid).getLayer().addChild(CStrokeController.strokes.get(uuid));
			CalicoDraw.addChildToNode(canvasdb.get(cuid).getLayer(), CStrokeController.strokes.get(uuid));
		}
	}

	public static void no_notify_add_child_connector(long cuid, long uuid)
	{
		if (!canvasdb.containsKey(cuid))
		{
			logger.warn("Attempting to add a connector to non-existing canvas: " + cuid + " !!");
			return;
		}

		canvasdb.get(cuid).addChildConnector(uuid);

		// canvasdb.get(cuid).getLayer().addChild(CStrokeController.strokes.get(uuid));
		CalicoDraw.addChildToNode(canvasdb.get(cuid).getLayer(), CConnectorController.connectors.get(uuid));
	}

	public static void no_notify_add_child_stroke(long cuid, long uuid)
	{
		no_notify_add_child_stroke(cuid, uuid, true);
	}

	public static void no_notify_delete_child_group(long cuid, long uuid)
	{
		canvasdb.get(cuid).deleteChildGroup(uuid);
	}

	public static void no_notify_delete_child_stroke(long cuid, long uuid)
	{
		canvasdb.get(cuid).deleteChildStroke(uuid);
	}

	public static void no_notify_delete_child_list(long cuid, long uuid)
	{
		canvasdb.get(cuid).deleteChildList(uuid);
	}

	public static void no_notify_delete_child_connector(long cuid, long uuid)
	{
		canvasdb.get(cuid).deleteChildConnector(uuid);
	}

	public static void no_notify_flush_dead_objects()
	{
		long[] cuids = canvasdb.keySet().toLongArray();

		for (int i = 0; i < cuids.length; i++)
		{

			int children = canvasdb.get(cuids[i]).getLayer().getChildrenCount();
			for (int c = children - 1; c >= 0; c--)
			{
				PNode childobj = canvasdb.get(cuids[i]).getLayer().getChild(c);

				if (childobj instanceof CStroke)
				{
					if (!canvasdb.get(cuids[i]).hasChildStroke(((CStroke) childobj).getUUID()))
					{
						// canvasdb.get(cuids[i]).getLayer().removeChild(c);
						CalicoDraw.removeChildFromNode(canvasdb.get(cuids[i]).getLayer(), c);
					}
				}
				else if (childobj instanceof CGroup)
				{
					if (!canvasdb.get(cuids[i]).hasChildGroup(((CGroup) childobj).getUUID()))
					{
						// canvasdb.get(cuids[i]).getLayer().removeChild(c);
						CalicoDraw.removeChildFromNode(canvasdb.get(cuids[i]).getLayer(), c);
					}
				}
				else if (childobj instanceof CArrow)
				{
					if (!canvasdb.get(cuids[i]).hasChildArrow(((CArrow) childobj).getUUID()))
					{
						// canvasdb.get(cuids[i]).getLayer().removeChild(c);
						CalicoDraw.removeChildFromNode(canvasdb.get(cuids[i]).getLayer(), c);
					}
				}
				else if (childobj instanceof CConnector)
				{
					if (!canvasdb.get(cuids[i]).hasChildConnector(((CConnector) childobj).getUUID()))
					{
						// canvasdb.get(cuids[i]).getLayer().removeChild(c);
						CalicoDraw.removeChildFromNode(canvasdb.get(cuids[i]).getLayer(), c);
					}
				}

			}

		}// for cuids
	}

	public static boolean canvas_has_child_group_node(long cuid, long uuid)
	{
		if (!CGroupController.exists(uuid))
		{
			return false;
		}

		return (canvasdb.get(cuid).getLayer().indexOfChild(CGroupController.groupdb.get(uuid)) != -1);
	}

	public static boolean canvas_has_child_stroke_node(long cuid, long uuid)
	{
		if (!CStrokeController.exists(uuid))
		{
			return false;
		}

		return (canvasdb.get(cuid).getLayer().indexOfChild(CStrokeController.strokes.get(uuid)) != -1);
	}

	public static boolean canvas_has_child_connector_node(long cuid, long uuid)
	{
		if (!CConnectorController.exists(uuid))
		{
			return false;
		}

		return (canvasdb.get(cuid).getLayer().indexOfChild(CConnectorController.connectors.get(uuid)) != -1);
	}

	public static void lock_canvas(long canvas, boolean lock, String lockedBy, long time)
	{
		no_notify_lock_canvas(canvas, lock, lockedBy, time);

		Networking.send(CalicoPacket.getPacket(NetworkCommand.CANVAS_LOCK, canvas, lock, lockedBy, time));
	}

	public static void no_notify_lock_canvas(long canvas, boolean lock, String lockedBy, long time)
	{
		if (!exists(canvas))
		{
			return;
		}

		canvasdb.get(canvas).setCanvasLock(lock, lockedBy, time);
		
//		if (CalicoDataStore.gridObject != null) CalicoDataStore.gridObject.updateCell(canvas);
		contributionController.contentChanged(canvas);

//		canvasdb.get(canvas).drawMenuBars();
	}
	
	/**
	   * Query whether the canvas <code>cuid</code> has any content within its CCanvas instance or from any
	   * <code>ContentContributor</code>. To query exclusively for CCanvas content, use
	   * <code>CCanvasController.canvasdb.get(cuid).isEmpty()</code>.
	   */
//	  public static boolean hasContent(long cuid)
//	  {
//	    if (!exists(cuid))
//	      return false;
//	
//	    for (CCanvas.ContentContributor contributor : contentContributors)
//	    {
//	      if (contributor.hasContent(cuid))
//	      {
//	        return true;
//	      }
//	    }
//	    return false;
//	  }

	/**
	 * Register <code>contributor</code> as a contributor of content to canvases.
	 */
	public static void addContentContributor(CCanvas.ContentContributor contributor)
	{
		contentContributors.add(contributor);
	}

	/**
	 * Unregister <code>contributor</code> as a contributor of content to canvases.
	 */
	public static void removeContentContributor(CCanvas.ContentContributor contributor)
	{
		contentContributors.remove(contributor);
	}

	/**
	 * Convenience method for the exclusive use of CCanvas. Implementations of ContentContributor must identify
	 * themselves using the notifyContentChanged(ContentContributor, long).
	 * 
	 * @param canvasId
	 *            the canvas for which content has changed.
	 */
	public static void notifyContentChanged(long canvasId)
	{
		contributionController.notifyContentChanged(contributionController, canvasId);
	}

	/**
	 * Each ContentContributor is responsible for calling this method when its content on the canvas has changed.
	 * 
	 * @param changeContributor
	 *            the contributor which is reporting a content change.
	 * @param canvasId
	 *            the canvas for which the contributor's content has changed.
	 */
	public static void notifyContentChanged(ContentContributor changeContributor, long canvasId)
	{
		contributionController.notifyContentChanged(changeContributor, canvasId);
	}

	public static void show_canvas_piemenu(Point point)
	{
		CGroup group = new CGroup(0l, 0l, 0l, false);
		ObjectArrayList<Class<?>> pieMenuButtons = group.getPieMenuButtons();

		ArrayList<PieMenuButton> buttons = new ArrayList<PieMenuButton>();
		try
		{

			for (int i = 0; i < pieMenuButtons.size(); i++)
			{
				if (pieMenuButtons.get(i).getName().compareTo("calico.components.piemenu.canvas.ArrowButton") == 0
						|| pieMenuButtons.get(i).getName().compareTo("calico.components.piemenu.canvas.ImageCreate") == 0)
				{
					buttons.add((PieMenuButton) pieMenuButtons.get(i).getConstructor(long.class).newInstance(0l));
				}
				else
				{
					buttons.add(new PieMenuButton(new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB)));
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		PieMenu.displayPieMenuArray(point, buttons.toArray(new PieMenuButton[buttons.size()]));

	}

	public static CCanvas getRestoredCanvas(CalicoPacket[] p)
	{
		CCanvas canvas = new CCanvas(-1, -1);

		for (int i = 0; i < p.length; i++)
		{
			p[i].rewind();
			int comm = p[i].getInt();

			if (comm == NetworkCommand.ARROW_CREATE)
			{

			}
			else if (comm == NetworkCommand.GROUP_LOAD)
			{

			}
			else if (comm == NetworkCommand.STROKE_LOAD)
			{

			}

		}

		return null;
	}

	/**
	 * Coordinates canvas contributors. When the content of a canvas changes, all contributors will be notified. This
	 * class also acts as the "central contributor", reporting the direct content of the CCanvas itself (scraps, arrows,
	 * etc.) The CCanvas class notifies this controller via CCanvasController.notifyContentChange() when its contents
	 * change.
	 * 
	 * @author Byron Hawkins
	 */
	private static class ContentContributionController implements CCanvas.ContentContributor, TaskBuffer.Client
	{
		private final TaskBuffer notificationSpool;

		private final Set<Long> changedCanvasIds = new HashSet<Long>();

		public ContentContributionController()
		{
			addContentContributor(this);

			notificationSpool = new TaskBuffer(this, 100L);
			notificationSpool.start();
		}


		 
//		@Override
//		public boolean hasContent(long canvas_uuid)
//		{
//		  CCanvas canvas = CCanvasController.canvasdb.get(canvas_uuid);
//		  if (canvas == null)
//		  {
//		    return false;
//		  }
//		  return !canvas.isEmpty();
//		}
		
		void clearCanvas(long canvas_uuid)
		{
			for (CCanvas.ContentContributor contributor : contentContributors)
			{
				contributor.clearContent(canvas_uuid);
			}
		}

		void notifyContentChanged(ContentContributor changeContributor, long canvas_uuid)
		{
			if (!canvasdb.containsKey(canvas_uuid))
			{
				// this canvas is being deleted, so stop notifying about it
				return;
			}

			synchronized (changedCanvasIds)
			{
				changedCanvasIds.add(canvas_uuid);
			}

			notificationSpool.taskPending();
		}

		@Override
		public void executeTasks()
		{
			synchronized (changedCanvasIds)
			{
				for (Long canvasId : changedCanvasIds)
				{
					for (CCanvas.ContentContributor contributor : contentContributors)
					{
						contributor.contentChanged(canvasId);
					}
				}
				changedCanvasIds.clear();
			}
		}

		@Override
		public void contentChanged(long canvas_uuid)
		{
			// no response required for this instance
		}

		@Override
		public void clearContent(long canvas_uuid)
		{
			canvasdb.get(canvas_uuid).clear();
		}
	}

	public static class Factory
	{
		private static final Factory INSTANCE = new Factory();

		public static Factory getInstance()
		{
			return INSTANCE;
		}

		private final Long2ReferenceOpenHashMap<PendingCanvas> pendingCanvases = new Long2ReferenceOpenHashMap<PendingCanvas>();

		public CCanvas createNewCanvas(long originatingCanvasId)
		{
			long canvasId = Calico.uuid();

			PendingCanvas pendingCanvas = new PendingCanvas();
			pendingCanvases.put(canvasId, pendingCanvas);

			CalicoPacket packet = new CalicoPacket();
			packet.putInt(NetworkCommand.CANVAS_CREATE);
			packet.putLong(canvasId);
			packet.putLong(originatingCanvasId);
			packet.rewind();
			Networking.send(packet);

			return pendingCanvas.waitForCanvas();
		}

		public void canvasCreated(CCanvas canvas)
		{
			PendingCanvas pendingCanvas = pendingCanvases.get(canvas.uuid);
			if (pendingCanvas != null)
			{
				pendingCanvas.canvasArrived(canvas);
			}
		}

		private class PendingCanvas
		{
			private CCanvas canvas = null;

			synchronized void canvasArrived(CCanvas canvas)
			{
				this.canvas = canvas;
				notify();
			}

			synchronized CCanvas waitForCanvas()
			{
				while (canvas == null)
				{
					try
					{
						wait();
					}
					catch (InterruptedException ok)
					{
					}
				}

				pendingCanvases.remove(canvas.uuid);
				return canvas;
			}
		}
	}

	public static class HistoryFrame extends CHistoryController.Frame
	{
		long canvasId;

		public HistoryFrame(long canvasId)
		{
			this.canvasId = canvasId;
		}

		@Override
		protected void restore()
		{
			CalicoPerspective.Active.displayPerspective(canvasId);
//			loadCanvas(canvasId);
		}
		
		public long getCanvasId()
		{
			return canvasId;
		}
	}
}
