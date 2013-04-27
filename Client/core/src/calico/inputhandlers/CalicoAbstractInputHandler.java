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
package calico.inputhandlers;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.geom.Arc2D;

import calico.CalicoDraw;
import calico.CalicoOptions;
import calico.components.CStroke;
import calico.components.piemenu.PieMenuButton;
import calico.controllers.CCanvasController;
import calico.controllers.CConnectorController;
import calico.controllers.CGroupController;
import calico.controllers.CStrokeController;
import calico.iconsets.CalicoIconManager;
import calico.utils.Ticker;
import calico.utils.TickerTask;
import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolo.activities.PActivity;
import edu.umd.cs.piccolo.nodes.PImage;
import edu.umd.cs.piccolo.nodes.PPath;

/**
 * This is the default class for all the input handlers
 * 
 * @author mdempsey
 * 
 */
public abstract class CalicoAbstractInputHandler
{
	public static class MenuAnimation extends PActivity
	{
		private final PLayer layer;

		private final PPath arc;
		private final double maxDistance;
		private final Point p;
		private final double radius = CalicoOptions.pen.press_and_hold_menu_radius;

		protected long step = 0;

		public MenuAnimation(PLayer layer, long duration, double maxDistance, Point p)
		{
			super(duration);

			this.layer = layer;
			this.maxDistance = maxDistance;
			this.p = p;

			Arc2D.Double arcShape = new Arc2D.Double(p.x - radius, p.y - radius, radius * 2, radius * 2, -90d, 0, Arc2D.OPEN);
			arc = new PPath(arcShape);
			arc.setStroke(new BasicStroke(3.0f));
			arc.setStrokePaint(CalicoOptions.pen.press_and_hold_menu_animation_color);
			arc.setTransparency(0.7f);
		}

		public void start()
		{
			layer.addChild(arc);
			arc.getRoot().addActivity(this);
		}

		protected boolean animateStep(long time)
		{
			double arcLength = (step * 2) * 360d / 20 + 90;
			step++;
			if (arcLength > 360 - 2 * 360d / 20 + 90)
			{
				return true;
			}// arcLength = 360 - 2 * 360d/20 + 90;

			Arc2D.Double arcShape = new Arc2D.Double(p.x - radius, p.y - radius, radius * 2, radius * 2, arcLength, 10, Arc2D.OPEN);
			final PPath arcChild = new PPath(arcShape);
			arcChild.setStroke(new BasicStroke(3.0f));
			arcChild.setStrokePaint(Color.RED);
			arcChild.setTransparency(0.7f);
			arc.addChild(arcChild);
			arc.repaint();
//			System.out.println("Stepping through PActivity: " + arcLength + ", " + time);

			return false;
		}

		protected void cleanup()
		{
			layer.removeChild(arc);
		}
	}

	public static class MenuTimer extends TickerTask
	{
		long stroke;
		long checkInterval;
		double maxHoldDistance;
		int holdTime;
		Point previousPoint;
		long guuid;
		PressAndHoldAction handler;
		private PLayer canvasToPaintTo;
		boolean terminateMenuTimer = false;

		public MenuTimer(PressAndHoldAction h, long uuid, long interval, double maxDistance, int holdTime, Point startingPoint, long guuid, PLayer pLayer)
		{
			handler = h;
			stroke = uuid;
			checkInterval = interval;
			maxHoldDistance = maxDistance;
			this.holdTime = holdTime;
			this.previousPoint = startingPoint;
			this.guuid = guuid;
			canvasToPaintTo = pLayer;
		}

		public boolean runtask()
		{
			// This means that either mouse up has occurred or a new stroke is being drawn, so we immediately quit!
			if (handler.getLastAction() != stroke)
			{
				handler.pressAndHoldAbortedEarly();
				return false;
			}

			if (handler.getLastPoint().distance(previousPoint) < maxHoldDistance && handler.getLastAction() == stroke
					&& handler.getMouseDown().distance(handler.getLastPoint()) < 30 && handler.getDraggedDistance() < 30
			/*
			 * &&
			 * Geometry.getPolygonLength(CStrokeController.strokes.get(CStrokeController.getCurrentUUID()).getPolygon())
			 * < 30
			 */)
			{
				final double maxDistance = maxHoldDistance;
				final Point p = new Point(handler.getLastPoint());
				final double radius = CalicoOptions.pen.press_and_hold_menu_radius;
				final long suuid = stroke;
				Arc2D.Double arcShape = new Arc2D.Double(p.x - radius, p.y - radius, radius * 2, radius * 2, -90d, 0, Arc2D.OPEN);
				final PPath arc = new PPath(arcShape);
				arc.setStroke(new BasicStroke(3.0f));
				arc.setStrokePaint(CalicoOptions.pen.press_and_hold_menu_animation_color);
				arc.setTransparency(0.7f);

				MenuAnimation animation = new MenuAnimation(canvasToPaintTo, CalicoOptions.pen.press_and_hold_menu_animation_duration, maxHoldDistance,
						handler.getLastPoint()) {
					protected void activityStep(long time)
					{
						if (terminateMenuTimer)
							this.terminate();
						super.activityStep(time);
						if (handler.getLastAction() != suuid || handler.getLastPoint().distance(p) > maxDistance)
						{
							this.terminate();
						}
						else
						{
							if (animateStep(time))
							{
								terminate();
							}
						}
					}

					protected void activityFinished()
					{
						cleanup();
						if (step > 9)
						{
							try
							{
								handler.pressAndHoldCompleted();
							}
							catch (Exception e)
							{
								e.printStackTrace();
							}
							handler.openMenu(0l, guuid, handler.getLastPoint());
						}
						else
						{
							handler.pressAndHoldAbortedEarly();
						}
					}
				};


				animation.setStartTime(System.currentTimeMillis());
				animation.setStepRate(CalicoOptions.pen.press_and_hold_menu_animation_tick_rate);
				animation.start();
				
				return false;
			}

			PLayer layer = CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getLayer();
			MenuTimer menuTimer = new MenuTimer(handler, stroke, checkInterval, CalicoOptions.core.max_hold_distance, CalicoOptions.core.hold_time,
					handler.getLastPoint(), guuid, layer);
			Ticker.scheduleIn(CalicoOptions.core.hold_time, menuTimer);

			return false;
		}

		public void terminate()
		{
			terminateMenuTimer = true;
		}
	}

	protected PImage modeIcon = null;
	protected boolean isModeIconShowing = false;
	protected long canvas_uid = 0L;
	protected Point modeIconLocation = null;

	protected void setupModeIcon(String iconName)
	{
		// setup the icon
		try
		{
			this.modeIcon = new PImage();
			this.modeIcon.setImage(CalicoIconManager.getIconImage(iconName));
		}
		catch (Exception e)
		{
			// Dunno
		}

	}

	protected void showModeIcon(Point showLocation)
	{
		if (this.isModeIconShowing)
		{
			return;
		}

		try
		{
			this.modeIconLocation = new Point(showLocation.x, showLocation.y); 
			this.modeIcon.setBounds(showLocation.getX() - 16, showLocation.getY() - 16, 16, 16);
			this.modeIcon.setPaintInvalid(true);
			//CCanvasController.canvasdb.get(this.canvas_uid).getLayer().addChild(this.modeIcon);
			CalicoDraw.addChildToNode(CCanvasController.canvasdb.get(this.canvas_uid).getLayer(), this.modeIcon);
//			CCanvasController.canvasdb.get(this.canvas_uid).getLayer().repaint();

			this.isModeIconShowing = true;
		}
		catch (Exception e)
		{

		}
	}

	// this just forces it to close
	protected void hideModeIcon()
	{
		if (!this.isModeIconShowing)
		{
			return;
		}	
		//this.modeIcon.removeFromParent();
		CalicoDraw.removeNodeFromParent(this.modeIcon);
		this.isModeIconShowing = false;
	}

	// TODO: check to see if we are out of the bounds
	protected void hideModeIcon(Point mouseCoords)
	{
		if (this.modeIconLocation != null && mouseCoords.distance(this.modeIconLocation) < CalicoOptions.menu.icon_tooltip_dist_threshold)
		{
			return;
		}
		this.hideModeIcon();// new Point(-1*this.modeIconLocation.x, -1*this.modeIconLocation.y));
	}

	/**
	 * This determines the action taken upon receiving a PRESSED action
	 * 
	 * @param ev
	 */
	public abstract void actionPressed(InputEventInfo ev);

	/**
	 * This determines the action taken upon receiving a RELEASED action
	 * 
	 * @param ev
	 */
	public abstract void actionReleased(InputEventInfo ev);

	/**
	 * This determines the action taken upon receiving a DRAGGED action
	 * 
	 * @param ev
	 */
	public abstract void actionDragged(InputEventInfo ev);

	/**
	 * This determines the action taken upon receiving a CLICKED action
	 * 
	 * @deprecated We shouldnt use this really, all events should rely on RELEASED instead
	 * @see #actionReleased(InputEventInfo)
	 * @param ev
	 */
	public void actionClicked(InputEventInfo ev)
	{
		actionReleased(ev);
	}

	/**
	 * Used for scrolling events
	 * 
	 * @param ev
	 */
	public void actionScroll(InputEventInfo ev)
	{
		// Ignore
	}

	public double getDragDistance(Polygon poly)
	{
		if (poly.npoints <= 1)
		{
			return 0.0;
		}
		Point p1 = new Point(poly.xpoints[0], poly.ypoints[0]);
		Point p2 = new Point(poly.xpoints[poly.npoints - 1], poly.ypoints[poly.npoints - 1]);

		return p1.distance(p2);
	}

	public static void clickMenu(long potScrap, long group, Point point)
	{
		long potentialScrap = potScrap;

		boolean deleteStroke = false;
		if (potentialScrap == 0l)
			// deleteStroke = true;
			// else
			potentialScrap = CStrokeController.getPotentialScrap(point);

		long g = CGroupController.get_smallest_containing_group_for_point(CCanvasController.getCurrentUUID(), point);
		
		if (CStrokeController.exists(potentialScrap) && CGroupController.group_contains_stroke(g, potentialScrap))
			g = 0;
		
		if (CGroupController.exists(g))
		{
			
		}
		else if (potentialScrap > 0l || (potentialScrap) > 0l)
		{
			CStroke stroke = CStrokeController.strokes.get(potentialScrap);
			// if (!CGroupController.checkIfLastTempGroupExists())
			// {
			long previewScrap = stroke.createTemporaryScrapPreview(deleteStroke);
			CGroupController.show_group_bubblemenu(previewScrap, PieMenuButton.SHOWON_SCRAP_CREATE, true);
			// }

		}
		else if (CConnectorController.exists(group))
		{
			CConnectorController.show_stroke_bubblemenu(group, false);
		}
		else if (CStrokeController.exists(group))
		{
			CStrokeController.show_stroke_bubblemenu(group, false);
		}
		else if (group != 0l // the group must exist
				&& !CGroupController.group_contains_stroke(group, potentialScrap)) // and the group must not contain a
																				   // potential scrap
		{
			CGroupController.show_group_bubblemenu(group);
		}
		
		else
		{
			// CCanvasController.show_canvas_piemenu(point);
			// PieMenu.displayPieMenu(point, new TextCreate(), new ArrowButton());
		}

	}

}
