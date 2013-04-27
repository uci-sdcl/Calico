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

import java.awt.*;

import calico.*;
import calico.networking.*;
import calico.networking.netstuff.*;
import calico.components.CGroup;
import calico.components.CStroke;
import calico.components.bubblemenu.BubbleMenu;
import calico.components.decorators.CListDecorator;
import calico.components.piemenu.PieMenuButton;
import calico.input.CInputMode;
import calico.modules.*;


import java.awt.geom.*;
import java.awt.geom.Line2D.Double;
import java.awt.image.BufferedImage;
import java.util.*;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import sun.reflect.ReflectionFactory.GetReflectionFactoryAction;

import edu.umd.cs.piccolo.*;
import edu.umd.cs.piccolo.activities.PActivity;
import edu.umd.cs.piccolo.event.*;
import edu.umd.cs.piccolo.nodes.*;
import edu.umd.cs.piccolo.util.*;
import edu.umd.cs.piccolox.nodes.*;

import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * This handles all start/append/finish requests for lines
 *
 * @author Mitch Dempsey
 */
public class CStrokeController
{
	/**
	 * Logger
	 */
	private static Logger logger = Logger.getLogger(CStrokeController.class.getName());
	

	/**
	 * We queue up strokes to be deleted, this way, we can have piccolo ALWAYS remove objects 
	 * (no errors for null pointers) and then we can clean up the arraylist later
	 */
	private static LongArraySet delete_strokes = new LongArraySet();
	
	/**
	 * This is the database of all the BGElements
	 */
	public static Long2ReferenceAVLTreeMap<CStroke> strokes = new Long2ReferenceAVLTreeMap<CStroke>();

	public static boolean isConsistencyCheck = false;
	
	private static long currentStrokeUUID = 0L;
	
	private static long mostRecentStroke = 0L;
	private static long secondMostRecentStroke = 0L;
	

	public static boolean dq_add(long uuid)
	{
		return delete_strokes.add(uuid);
	}
	
	
	// Does nothing right now
	public static void setup()
	{
		strokes.clear();
		delete_strokes.clear();
	}
	
	
	public static boolean exists(long uuid)
	{
		return strokes.containsKey(uuid);
	}

	

	public static long getCurrentUUID()
	{
		return currentStrokeUUID;
	}

	public static void setCurrentUUID(long u)
	{
		currentStrokeUUID = u;
	}
	
	private static long getStrokeCanvasUUID(long uuid)
	{
		return strokes.get(uuid).getCanvasUUID();
	}
	
	
	public static boolean is_parented_to(long uuid, long puid)
	{
		return (strokes.get(uuid).getParentUUID()==puid);
	}
	
	public static long makeScrap(long suuid, long new_guuid)
	{
		long ret = no_notify_makeScrap(suuid, new_guuid);
		Networking.send(NetworkCommand.STROKE_MAKE_SCRAP, suuid, new_guuid);
		
		return ret;
	}
	
	public static long makeShrunkScrap(long suuid, long new_guuid)
	{
		long ret = no_notify_makeShrunkScrap(suuid, new_guuid);
		Networking.send(NetworkCommand.STROKE_MAKE_SHRUNK_SCRAP, suuid, new_guuid);
		
		return ret;
	}
	
	public static void deleteArea(long suuid, long temp_guuid)
	{
		no_notify_deleteArea(suuid, temp_guuid);
		Networking.send(NetworkCommand.STROKE_DELETE_AREA, suuid, temp_guuid);
	}
	
	public static void loadStroke(long suuid)
	{	
		if (!strokes.containsKey(suuid))
		{
			logger.warn("Attempting to load stroke " + suuid + " which does not exist!");
			//System.err.println("Attempting to load a stroke that does not exist!");
			return;
		}
		CalicoPacket[] packets = strokes.get(suuid).getUpdatePackets();
		
		for(int i=0;i<packets.length;i++)
		{
			Networking.send(packets[i]);
		}
		
		if (CalicoDataStore.Mode == CInputMode.POINTER)
			setStrokeAsPointer(suuid);
	}
	
	public static long no_notify_makeScrap(long suuid, long new_guuid)
	{
		if (!strokeExists(suuid))
			return 0;
		
		CStroke stroke = CStrokeController.strokes.get(suuid);
		Point2D center = calico.utils.Geometry.getMidPoint2D(stroke.getPolygon());
		long parent = CGroupController.get_smallest_containing_group_for_point(stroke.getCanvasUUID(), new Point((int)center.getX(), (int)center.getY()));
		CGroupController.no_notify_start(new_guuid, stroke.getCanvasUUID(), parent, false);
		
		Polygon strokePoly = stroke.getPolygon();
		CGroupController.no_notify_append(new_guuid, strokePoly.xpoints, strokePoly.ypoints);
//		for (int i=0;i<strokePoly.npoints;i++)
//		{
//			CGroupController.no_notify_append(new_guuid, strokePoly.xpoints[i], strokePoly.ypoints[i]);
//			
//		}
		CGroupController.setCurrentUUID(new_guuid);
		CGroupController.no_notify_finish(new_guuid, true);
		CGroupController.setLastCreatedGroupUUID(CGroupController.getCurrentUUID());
//		CGroupController.no_notify_set_permanent(new_guuid, true);
		CStrokeController.no_notify_delete(suuid);
		return new_guuid;
	}

	public static long no_notify_makeShrunkScrap(long suuid, long new_guuid)
	{
		long scrapUUID = no_notify_makeScrap(suuid, new_guuid);
		CGroup scrap = CGroupController.groupdb.get(scrapUUID);
		scrap.shrinkToContents();
		return scrapUUID;
//		long[] children = scrap.getPossibleChildren();
//		if (children.length > 0)
//		{
//			Rectangle bounds = scrap.getBoundsOfObjects(children);
//			scrap.shrinkToContents(bounds);
//			return scrapUUID;
//		}
//		else
//		{
//			CGroupController.delete(scrapUUID);
//			return 0L;
//		}
	}
	
	public static void no_notify_deleteArea(long uuid, long temp_guuid)
	{
		long scrapToDelete = no_notify_makeScrap(uuid, temp_guuid);
		CGroupController.no_notify_delete(scrapToDelete);
	}
	
	/**
	 * This will start a Stroke, but not notify anybody
	 * @param uuid
	 * @param cuid
	 * @param puid
	 * @param color
	 */
	public static void no_notify_start(long uuid, long cuid, long puid, Color color, float thickness)
	{
		// does the element already exist?
		if(exists(uuid))
		{
			no_notify_delete(uuid);
		}
		
		// add to the DB
		strokes.put(uuid, new CStroke(uuid, cuid, puid, color, thickness));
		
		// Add to the canvas
		CCanvasController.no_notify_add_child_stroke(cuid, uuid, true);
		
		if(puid!=0L)
		{
			// Ok, add to the group
			CGroupController.no_notify_add_child_stroke(puid, uuid);
		}
		
		//for the double click
		secondMostRecentStroke = mostRecentStroke;
		mostRecentStroke = uuid;
	}
	
	private static boolean strokeExists(long suuid)
	{
		if(!exists(suuid))
		{
			logger.warn("Stroke "+suuid+" does not exist");
//			(new Exception()).printStackTrace();
			return false;
		}
		return true;
	}
	
	// Probably shouldnt do this so much?
	
	public static void no_notify_append(long uuid, int x, int y, boolean repaint)
	{
		if (!strokeExists(uuid))
			return;
		
		// Append
		strokes.get(uuid).append(x, y);
		
		
		
		// Repaint, but only if requested AND we are looking at that canvas
		if(repaint)
		{
			// MITCH TEST
			//no_notify_repaint(uuid);
		}
	}
	public static void no_notify_append(long uuid, int[] x, int[] y)
	{
		if (!strokeExists(uuid))
			return;
		
		// loop the points and append
		for(int i=0;i<x.length;i++)
		{
			no_notify_append(uuid, x[i], y[i], false);
		}
		
//		no_notify_repaint(uuid);
	}
	
	public static void no_notify_batch_append(long uuid, int[] x, int[] y)
	{
		if (!strokeExists(uuid))
			return;
		
		strokes.get(uuid).batch_append(x, y);
	}
	
	public static void no_notify_finish(long uuid)
	{
		if (!strokeExists(uuid))
			return;
		
		strokes.get(uuid).finish();
		if (Networking.connectionState != Networking.ConnectionState.Connecting)
			strokes.get(uuid).calculateParent();
	}
	
	public static void no_notify_delete(long uuid)
	{
		if (!strokeExists(uuid))
			return;
		
		
		strokes.get(uuid).delete();
		strokes.remove(uuid);
		//CGroupController.originalStroke = null;
		//CGroupController.restoreOriginalStroke = false;

		dq_add(uuid);
		
		if (BubbleMenu.activeUUID == uuid)
		{
			BubbleMenu.clearMenu();
		}
	}
	
	
	public static void no_notify_set_color(long uuid, Color color)
	{
		if (!strokeExists(uuid))
			return;
	}
	
	public static void no_notify_set_parent(long uuid, long puid)
	{
		if (!strokeExists(uuid))
			return;
		
		long curpuid = strokes.get(uuid).getParentUUID();
		
		if(curpuid!=0L)
		{
			// We have a parent already, so we must notify them that we are leaving
			CGroupController.no_notify_delete_child_stroke(puid, uuid);
		}
		
		strokes.get(uuid).setParentUUID(puid);
		
		if(puid!=0L)
		{
			CGroupController.no_notify_add_child_stroke(puid, uuid);
		}	
		
	}
	
	public static void no_notify_move(long uuid, int x, int y)
	{
		if (!strokeExists(uuid))
			return;
		
		strokes.get(uuid).move(x, y);
	}
	
//	public static void no_notify_repaint(long uuid)
//	{
//		// we only repaint if we are actually on this canvas
//		long cuid = getStrokeCanvasUUID(uuid);
//		if(CCanvasController.getCurrentUUID()==cuid)
//		{
//			CCanvasController.canvasdb.get(cuid).repaint();
//		}
//	}

	

	/**
	 * @deprecated
	 * @see #no_notify_start(long, long, long, Color)
	 * @param uuid
	 * @param cuid
	 * @param puid
	 */
	public static void no_notify_start(long uuid, long cuid, long puid)
	{
		no_notify_start(uuid,cuid,puid, CalicoDataStore.PenColor, CalicoDataStore.PenThickness);
	}
	
	public static void no_notify_start(long uuid, long cuid, long puid, int red, int green, int blue, float thickness)
	{
		no_notify_start(uuid, cuid, puid, new Color(red, green, blue), thickness);
	}
	public static void no_notify_set_color(long uuid, int red, int green, int blue)
	{
		no_notify_set_color(uuid, new Color(red,green,blue));
	}
	public static void no_notify_set_thickness(long uuid, float thickness)
	{
		if (!exists(uuid))
			return;
		strokes.get(uuid).setThickness(thickness);
	}
	public static void no_notify_append(long uuid, int x, int y)
	{
		no_notify_append(uuid, x, y, true);
	}
	public static void no_notify_copy(long uuid, long new_uuid, long new_puuid, long new_canvasuuid, int shift_x, int shift_y)
	{
		if(!exists(uuid) || exists(new_uuid)){return;}

		CalicoPacket[] packets = strokes.get(uuid).getUpdatePackets(new_uuid, new_canvasuuid, new_puuid, shift_x, shift_y);
		batchReceive(packets);	
	}
	
	private static void batchReceive(CalicoPacket[] packets)
	{
		for (int i = 0; i < packets.length; i++)
		{
			CalicoPacket p = new CalicoPacket(packets[i].getBuffer());
			PacketHandler.receive(p);
		}
	}
	
	//////////////////////////////////////// NOTIFY ELEMENTS
	
	
	
	public static void start(long uuid, long cuid, long puid, Color color, float thickness)
	{
		no_notify_start(uuid, cuid, puid, color, thickness);
		
//		Networking.send(NetworkCommand.STROKE_START,
//			uuid,
//			cuid,
//			puid,
//			color.getRed(),
//			color.getGreen(),
//			color.getBlue()
//		);
	}
	
	public static void delete(long uuid)
	{
		if (!strokeExists(uuid))
			return;
		
		no_notify_delete(uuid);
		
		Networking.send(NetworkCommand.STROKE_DELETE, uuid);
	}
	public static void finish(long uuid)
	{
		no_notify_finish(uuid);
		
//		Networking.send(NetworkCommand.STROKE_FINISH, uuid);
		loadStroke(uuid);
	}
	
	public static void deleteDoubleClickStrokes()
	{
		delete(secondMostRecentStroke);
		delete(mostRecentStroke);
		
		secondMostRecentStroke = 0L;
		mostRecentStroke = 0L;
	}
	
	public static void set_color(long uuid, Color color)
	{
		no_notify_set_color(uuid,color);
		
		Networking.send(NetworkCommand.STROKE_SET_COLOR, 
			uuid,
			color.getRed(),
			color.getGreen(),
			color.getBlue()
		);
	}
	public static void set_parent(long uuid, long puid)
	{
		no_notify_set_parent(uuid, puid);
		
		Networking.send(NetworkCommand.STROKE_SET_PARENT, uuid, puid);
	}
	public static void append(long uuid, int x, int y)
	{
		no_notify_append(uuid, x, y);
		
//		CalicoPacket p = new CalicoPacket( ByteUtils.SIZE_OF_INT + ByteUtils.SIZE_OF_LONG + (ByteUtils.SIZE_OF_SHORT*3) );
//		p.putInt(NetworkCommand.STROKE_APPEND);
//		p.putLong(uuid);
//		p.putCharInt(1);
//		p.putInt(x);
//		p.putInt(y);
//		
//		Networking.send(p);
	}
//	public static void move(long uuid, int x, int y)
//	{
//		no_notify_move(uuid, x, y);
//		
//		Networking.send(NetworkCommand.STROKE_MOVE, uuid, x, y);
//	}
	
	
	
	public static void start(long uuid, long cuid, long puid)
	{
		start(uuid, cuid, puid, CalicoDataStore.PenColor, CalicoDataStore.PenThickness);
	}
	
	
	
	public static int get_signature(long uuid)
	{
		if (!strokeExists(uuid))
			return 0;
		
		return strokes.get(uuid).get_signature();
	}
	
	public static String get_signature_debug_output(long uuid)
	{
		if (!strokeExists(uuid))
			return "";
		
		return strokes.get(uuid).get_signature_debug_output();
	}
	
	/*
	public static void no_notify_start(long uuid,long canvasuid, long parentuid)
	{
		// TODO: Should we check to see if one already exists (and delete it first)
		
		
		bgelements.put(uuid, new BGElement(uuid,canvasuid,parentuid));

	}

	public static void no_notify_append(long uuid, int x, int y)
	{
		if(!bgelements.containsKey(uuid))
		{
			logger.warn("BGE append on nonexistant "+uuid);
			return;
		}
		bgelements.get(uuid).append( x, y );
	}

	public static void no_notify_finish(long uuid)
	{

		if(!bgelements.containsKey(uuid))
		{
			logger.warn("BGE finish on nonexistant "+uuid);
			return;
		}
		bgelements.get(uuid).finish();
	}

	public static void no_notify_delete(long uuid)
	{

		if(!bgelements.containsKey(uuid))
		{
			logger.warn("BGE delete on nonexistant "+uuid);
			return;
		}
		
		BGElement elm = bgelements.get(uuid);
		elm.delete();
	}
	
	
	public static void no_notify_color(long uuid, Color color)
	{
		if(!bgelements.containsKey(uuid))
		{
			logger.warn("BGE delete on nonexistant "+uuid);
			return;
		}
		bgelements.get(uuid).setColor(color);
	}
	public static void no_notify_color(long uuid, int r, int g, int b)
	{
		no_notify_color(uuid,new Color(r,g,b));
	}
	
	
	
	public static void no_notify_OLD_reload_start(long uuid)
	{
		if(!bgelements.containsKey(uuid))
		{
			logger.warn("BGE reload_start on nonexistant "+uuid);
			return;
		}
		bgelements.get(uuid).setReloading();
	}
	public static void no_notify_OLD_reload_coords(long uuid, Polygon points)
	{
		if(!bgelements.containsKey(uuid))
		{
			logger.warn("BGE reload_coords on nonexistant "+uuid);
			return;
		}
		bgelements.get(uuid).reload(points);
	}
	public static void no_notify_OLD_reload_finish(long uuid)
	{
		if(!bgelements.containsKey(uuid))
		{
			logger.warn("BGE reload_finish on nonexistant "+uuid);
			return;
		}
		bgelements.get(uuid).doneReloading();
	}
	
	public static void no_notify_parent(long uuid, long newparent)
	{

		if(!bgelements.containsKey(uuid))
		{
			logger.warn("BGE parent on nonexistant "+uuid);
			return;
		}
		bgelements.get(uuid).setParentUUID(newparent);
	}
	
	public static void no_notify_move(long uuid, int x, int y)
	{

		if(!bgelements.containsKey(uuid))
		{
			logger.warn("BGE move on nonexistant "+uuid);
			return;
		}
		
		bgelements.get(uuid).move(x, y);
	}
	
	
	public static void start(long uuid,long canvasuid, long parentuid)
	{
		no_notify_start(uuid, canvasuid, parentuid);
		Networking.send(NetworkCommand.BGE_START, uuid, canvasuid, parentuid);
		color(uuid, CalicoDataStore.PenColor);
		//append(uuid,x,y);
	}

	public static void append(long uuid, int x, int y)
	{
		no_notify_append(uuid, x, y);
		Networking.bge_append(uuid, x, y);
	}

	public static void finish(long uuid)
	{
		no_notify_finish(uuid);
		Networking.send(NetworkCommand.BGE_FINISH, uuid);
	}

	public static void delete(long uuid)
	{
		no_notify_delete(uuid);
		Networking.send(NetworkCommand.BGE_DELETE, uuid);
	}
	
	public static void color(long uuid, Color color)
	{
		no_notify_color(uuid,color);
		Networking.send(NetworkCommand.BGE_COLOR, uuid, color.getRed(), color.getGreen(), color.getBlue());
	}
	public static void color(long uuid, int r, int g, int b)
	{
		color(uuid,new Color(r,g,b));
	}

	public static void parent(long uuid, long newparent)
	{
		no_notify_parent(uuid, newparent);
	}
	
	public static void move(long uuid, int x, int y)
	{
		no_notify_move(uuid,x,y);
	}
	*/

	
	
	
	
	
	
	
	
	/*
	public static void no_notify_reload_start(long uuid, long cuid, long puid, Color color)
	{
		no_notify_reload_remove(uuid);
		
		BGElement bge = new BGElement(uuid, cuid, puid);
		bge.setColor(color);
		bgelements.put(uuid, bge);
	}
	public static void no_notify_reload_coords(long uuid, int[] xpoints, int[] ypoints)
	{
		if(!bgelements.containsKey(uuid))
		{
			logger.warn("BGE reload_coords on nonexistant "+uuid);
			return;
		}
		bgelements.get(uuid).reload_coords(xpoints, ypoints);
	}
	public static void no_notify_reload_finish(long uuid)
	{
		if(!bgelements.containsKey(uuid))
		{
			logger.warn("BGE reload_finish on nonexistant "+uuid);
			return;
		}
		bgelements.get(uuid).reload_finish();
	}
	public static void no_notify_reload_remove(long uuid)
	{
		if(!bgelements.containsKey(uuid))
		{
			logger.warn("BGE reload_REMOVE on nonexistant "+uuid);
			return;
		}

		logger.debug("BEFORE BGE REMOVE PARENT");
		bgelements.get(uuid).removeFromParent();
		logger.debug("AFTER BGE REMOVE PARENT^^^^^");
	}
	*/
	
	
	public static boolean isPotentialScrap(long strokeUUID)
	{
		float DISTANCE_PERCENT = 0.025f;//0.0125f;
		
		int maxDistance = (int) (CalicoDataStore.ScreenWidth * DISTANCE_PERCENT);//10;
		CStroke stroke = CStrokeController.strokes.get(strokeUUID);
	
		if (stroke == null)
			return false;
	
		
		Rectangle bounds = stroke.getRawPolygon().getBounds();
		if (bounds.width < 50 || bounds.height < 50)
			return false;
		
		Polygon strokePoly = stroke.getRawPolygon();

//		Polygon strokePoly = stroke.getPolygon();
		double headTailDistance = Point2D.distance(stroke.circlePoint.x, stroke.circlePoint.y, 
				stroke.getPolygon().xpoints[strokePoly.npoints-1], stroke.getPolygon().ypoints[strokePoly.npoints-1]);
		
//		logger.debug("Stroke length: " + stroke.getLength());
		if (stroke.getLength() < CalicoOptions.stroke.min_create_scrap_length ||
				headTailDistance > CalicoOptions.stroke.max_head_to_heal_distance)
			return false;
			
//		if (getPossibleChildren(strokeUUID).length == 0)
//			return false;
		
		return true;
	}
	
	public static long[] getPossibleChildren(long strokeUUID)
	{
		long cuid = CStrokeController.strokes.get(strokeUUID).getCanvasUUID();
		
		ArrayList<Long> possibleChildren = new ArrayList<Long>();
		// Check the bounds for the other items on the canvas.
		
		long[] grouparr = CCanvasController.canvasdb.get(cuid).getChildGroups();
		long[] bgearr = CCanvasController.canvasdb.get(cuid).getChildStrokes();

		// Check to see if any groups are inside of this.
		if(grouparr.length>0)
		{
			for(int i=0;i<grouparr.length;i++)
			{
				if(CGroupController.groupdb.get(grouparr[i]).getParentUUID()==0L && CStrokeController.stroke_contains_shape(strokeUUID, CGroupController.groupdb.get(grouparr[i]).getPathReference()))
				{
					possibleChildren.add(new Long(CGroupController.groupdb.get(grouparr[i]).getUUID()));
				}
			}
		}
		
		// Send the BGElement Parents
		if(bgearr.length>0)
		{
			for(int i=0;i<bgearr.length;i++)
			{
				if(CStrokeController.stroke_contains_shape(strokeUUID, CStrokeController.strokes.get(bgearr[i]).getPathReference()))
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
	
	public static boolean stroke_contains_shape(final long containerUUID, Shape shape)
	{
		if(!strokes.containsKey(containerUUID)){return false;}
		
		return CStrokeController.strokes.get(containerUUID).containsShape(shape);
//		return group_contains_path(containerUUID, CGroupController.groups.get(checkUUID).getPathReference() );
	}
	
	public static long getPotentialScrap(Point p)
	{
		if (p == null)
			return 0l;
		
		long[] strokes = CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getChildStrokes();
		
		long smallestStroke = 0l;
		double strokeArea = java.lang.Double.MAX_VALUE;
		Polygon temp;
		double tempArea;
		for (int i = 0; i < strokes.length; i++)
		{
			temp = CStrokeController.strokes.get(strokes[i]).getPolygon();
			tempArea = Geometry.computePolygonArea(temp);
			if (temp.contains(p) 
					&& tempArea < strokeArea
					&& new Point(temp.xpoints[0],temp.ypoints[0])
						.distance(new Point(temp.xpoints[temp.npoints-1],temp.ypoints[temp.npoints-1])) 
						< calico.CalicoOptions.pen.press_and_hold_menu_radius * 4)
			{
				smallestStroke = strokes[i];
				strokeArea = tempArea;
			}
		}
		return smallestStroke;
	}
	
	public static long getPotentialConnector(Point p, int maxDistance)
	{
		if (p == null)
			return 0l;
		
		long[] strokes = CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getChildStrokes();
		
		long closestStroke = 0l;
		double minStrokeDistance = java.lang.Double.MAX_VALUE;
		Polygon temp;
		for (int i = 0; i < strokes.length; i++)
		{
			temp = CStrokeController.strokes.get(strokes[i]).getPolygon();

			long tailUUID = CGroupController.get_smallest_containing_group_for_point(CCanvasController.getCurrentUUID(), new Point(temp.xpoints[0], temp.ypoints[0]));
			long headUUID = CGroupController.get_smallest_containing_group_for_point(CCanvasController.getCurrentUUID(), new Point(temp.xpoints[temp.npoints - 1], temp.ypoints[temp.npoints - 1]));
			
			if (tailUUID != 0l && headUUID != 0l 
					&& !(tailUUID == headUUID && CGroupController.groupdb.get(headUUID).containsShape(temp))
					&& !(CGroupController.groupdb.get(tailUUID) instanceof CListDecorator) && !(CGroupController.groupdb.get(headUUID) instanceof CListDecorator))
			{
				double minSegmentDistance = java.lang.Double.MAX_VALUE;
				for (int j = 0; j < temp.npoints - 1; j++)
				{
					double[] intersectPoint = Geometry.computeIntersectingPoint(temp.xpoints[j], temp.ypoints[j], temp.xpoints[j+1], temp.ypoints[j+1], p.x, p.y);
					double AtoB = Geometry.length(temp.xpoints[j], temp.ypoints[j], temp.xpoints[j+1], temp.ypoints[j+1]);
					double AtoI = Geometry.length(temp.xpoints[j], temp.ypoints[j], intersectPoint[0], intersectPoint[1]);
					double ItoB = Geometry.length(intersectPoint[0], intersectPoint[1], temp.xpoints[j+1], temp.ypoints[j+1]);
					double actualDistance;
					
					//The intersecting point is not on the segment
					if (AtoI > AtoB || ItoB > AtoB)
					{
						actualDistance = Math.min(Geometry.length(temp.xpoints[j], temp.ypoints[j], p.x, p.y),
									Geometry.length(p.x, p.y, temp.xpoints[j+1], temp.ypoints[j+1]));
					}
					//The intersecting line is on the segment
					else
					{
						actualDistance = Geometry.length(intersectPoint[0], intersectPoint[1], p.x, p.y);
					}
					
					if (actualDistance < minSegmentDistance)
						minSegmentDistance = actualDistance;
				}
				
				if (minSegmentDistance < maxDistance && minSegmentDistance < minStrokeDistance)
				{
					minStrokeDistance = minSegmentDistance;
					closestStroke = strokes[i];
				}
		
			}
		}
		return closestStroke;
	}
	
	public static boolean intersectsCircle(long suuid, Point center, double radius)
	{
		Polygon stroke = strokes.get(suuid).getPolygon();

		for (int i = 1; i < stroke.npoints; i++)
		{
			double circleDist = Line2D.ptSegDist(stroke.xpoints[i-1], stroke.ypoints[i-1], stroke.xpoints[i], stroke.ypoints[i], center.getX(), center.getY());
			if (circleDist < radius)
				return true;
		}
		return false;
	}
	
	public static void setStrokeAsPointer(long uuid)
	{
		Networking.send(NetworkCommand.STROKE_SET_AS_POINTER, uuid);
		
		no_notify_set_stroke_as_pointer(uuid);
	}


	public static void no_notify_set_stroke_as_pointer(long uuid) {
		// TODO Auto-generated method stub
		if (!exists(uuid))
			return;
		
		strokes.get(uuid).setIsTempInk(true);
		
		final long suuid = uuid;
		long fadeDelay = 3000;
		
		PActivity flash = new PActivity(5000,10, System.currentTimeMillis()+fadeDelay) {
			long step = 0;
      
		    protected void activityStep(long time) {
		            super.activityStep(time);
		            if (!CStrokeController.exists(suuid))
		            {
		            	this.terminate();
		            	return;
		            }
		            strokes.get(suuid).setTransparency(1.0f - 1.0f * step/50);
		            if (step > 100)
		            	step++;
		            step++;
		    }
		    
		    protected void activityFinished() {
		    	CStrokeController.no_notify_delete(suuid);
		    }
		};
		// Must schedule the activity with the root for it to run.
		//strokes.get(uuid).getRoot().addActivity(flash);
		CalicoDraw.addActivityToNode(strokes.get(uuid), flash);
		
	}
	
	@Deprecated
	public static void hideStroke(long uuid, boolean delete)
	{
		if (!exists(uuid))
		{
			return;
		}
		
		Networking.send(NetworkCommand.STROKE_HIDE, uuid);
		no_notify_hide_stroke(uuid, delete);
	}
	
	@Deprecated
	public static void no_notify_hide_stroke(final long uuid, final boolean delete)
	{
		if (!exists(uuid))
			return;
		
		final CStroke stroke = strokes.get(uuid);
		
		PActivity flash = new PActivity(/*500,70,*/50,10, System.currentTimeMillis()) {
			long step = 0;
      
		    protected void activityStep(long time) {
		    	super.activityStep(time);
		    	
		    	
		    	if (stroke.hiding == false)
		    		terminate();
		    	
		            
		            float t = 1.0f - 1.0f * step/5;
		            stroke.setTransparency(t);
		            step++;
		            if (t <= 0)
		            	terminate();
		    }
		    
		    protected void activityStarted() {
		    	stroke.hiding = true;
		    }
		    
		    
		    protected void activityFinished() {
		    	if (stroke.hiding)
		    		stroke.setTransparency(0f);
		    	stroke.hiding = false;
		    	if (delete)
		    	{
		    		CStrokeController.no_notify_delete(uuid);
		    	}
		    	
		    }
		};
		// Must schedule the activity with the root for it to run.
		if (stroke.getRoot() != null)
			//stroke.getRoot().addActivity(flash);
			CalicoDraw.addActivityToNode(stroke, flash);
	}
	
	public static void show_stroke_bubblemenu(long uuid, boolean fade)
	{
		//Class<?> pieMenuClass = calico.components.piemenu.PieMenu.class;
		if (!exists(uuid))
			return;

		ObjectArrayList<Class<?>> pieMenuButtons = CStrokeController.strokes.get(uuid).getBubbleMenuButtons();
		
		int[] bitmasks = new int[pieMenuButtons.size()];
		
		
		
		if(pieMenuButtons.size()>0)
		{
			ArrayList<PieMenuButton> buttons = new ArrayList<PieMenuButton>();
			
			for(int i=0;i<pieMenuButtons.size();i++)
			{
				try
				{
					buttons.add((PieMenuButton) pieMenuButtons.get(i).getConstructor(long.class).newInstance(uuid));
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}

			BubbleMenu.displayBubbleMenu(uuid,fade,BubbleMenu.TYPE_STROKE,buttons.toArray(new PieMenuButton[buttons.size()]));
			
			
		}
		
		
	}
	
	@Deprecated
	public static void unhideStroke(long uuid)
	{
		if (!exists(uuid))
			return;
		
		
		
		no_notify_unhide_stroke(uuid);
		Networking.send(NetworkCommand.STROKE_UNHIDE, uuid);
	}

	@Deprecated
	public static void no_notify_unhide_stroke(long uuid) {
		if (!exists(uuid))
			return;
		
		final long tempUUID = uuid;
		
		strokes.get(tempUUID).hiding = false;
		//This line is not thread safe so must invokeLater to prevent exceptions.
		/*SwingUtilities.invokeLater(
				new Runnable() { public void run() { 
					
					strokes.get(tempUUID).setTransparency(1.0f);
					} }
		);*/
		CalicoDraw.setNodeTransparency(strokes.get(tempUUID), 1.0f);
		
		//strokes.get(uuid).hiding = false;
		//strokes.get(uuid).setTransparency(1.0f);
	}
	
	public static void recalculateParent(long stroke)
	{
		if (!exists(stroke))
			return;
		
		CStrokeController.strokes.get(stroke).calculateParent();
	}

}
