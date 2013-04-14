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
package calico.inputhandlers.canvas;

import calico.*;

import calico.components.*;
import calico.components.menus.*;
import calico.components.piemenu.*;
import calico.controllers.CGroupController;
import calico.iconsets.CalicoIconManager;
import calico.inputhandlers.CCanvasInputHandler;
import calico.inputhandlers.CalicoAbstractInputHandler;
import calico.inputhandlers.CalicoInputManager;
import calico.inputhandlers.InputEventInfo;
import calico.networking.*;
import calico.networking.netstuff.*;

import java.awt.geom.*;
import java.awt.*;

import java.util.*;

import org.apache.log4j.*;

import edu.umd.cs.piccolo.event.*;
import edu.umd.cs.piccolo.nodes.PImage;

// implements PenListener
public class CCanvasScrapModeInputHandler extends CalicoAbstractInputHandler {
	public static Logger logger = Logger
			.getLogger(CCanvasScrapModeInputHandler.class.getName());

	public static final double CREATE_GROUP_MIN_DIST = 15.0;

	// private long canvas_uid = 0L;

	private boolean hasStartedGroup = false;
	private boolean hasStartedBge = false;

	private boolean hasBeenPressed = false;
	private boolean hasBeenMoved = false;

	private InputEventInfo lastEvent = null;

	private Polygon draggedCoords = null;

	private CCanvasInputHandler parentHandler = null;

	private int actionsSincePress = 0;
	private Point mouseDown = null;

	public CCanvasScrapModeInputHandler(long cuid, CCanvasInputHandler parent) {
		canvas_uid = cuid;
		parentHandler = parent;

		// this.setupModeIcon("mode.scrap");

	}

	public void actionPressed(InputEventInfo e) {
		CalicoInputManager.drawCursorImage(canvas_uid,
				CalicoIconManager.getIconImage("mode.scrap"), e.getPoint());
		// this.showModeIcon(e.getPoint());
		// logger.debug(this.getClass().getName()+" actionPressed()");

		actionsSincePress = 0;

		hasStartedGroup = false;

		/*
		 * if(e.isLeftButtonPressed()) { int x = e.getX(); int y = e.getY();
		 * long uuid = Calico.uuid(); // Lasso! //TODO edit for testing
		 * CGroupController.start(uuid, canvas_uid, 0L, x, y);
		 * CGroupController.setCurrentUUID(uuid); }
		 */
		lastEvent = e;
		mouseDown = e.getPoint();
	}

	public void actionDragged(InputEventInfo e) {
		// this.hideModeIcon(e.getPoint());

		// logger.debug(this.getClass().getName()+" actionDragged()");
		actionsSincePress++;
//		CalicoInputManager.lockInputHandler(canvas_uid);

		int x = e.getX();
		int y = e.getY();

		if (e.isLeftButtonPressed()) {
			if (!hasStartedGroup) {
				long uuid = Calico.uuid();
				CGroupController.start(uuid, canvas_uid, 0L, false);
				CGroupController.append(uuid, lastEvent.getX(),
						lastEvent.getY());
				CGroupController.setCurrentUUID(uuid);
				hasStartedGroup = true;
			} else {
				CGroupController
						.append(CGroupController.getCurrentUUID(), x, y);
			}
		}

		/*
		 * if( e.isMiddleButtonPressed() && draggedCoords.npoints>1) { long[]
		 * bgelist = CCanvasController.canvasdb.get(canvas_uid).getStrokes();
		 * if(bgelist.length>0) { for(int i=0;i<bgelist.length;i++) {
		 * if(BGElementController.exists(bgelist[i]) &&
		 * BGElement.countIntersections
		 * (BGElementController.bgelements.get(bgelist[i]).getPolygon(),
		 * draggedCoords )>0) { BGElementController.delete(bgelist[i]); } } } }
		 * else if(e.isLeftButtonPressed() && hasStartedBge) {
		 * BGElementController.append(BGElementController.getCurrentUUID(), x,
		 * y); //e.setHandled(true); } else if(e.isLeftButtonPressed() &&
		 * !hasStartedBge) { long uuid = Calico.uuid();
		 * BGElementController.setCurrentUUID(uuid);
		 * BGElementController.start(uuid, canvas_uid, 0L);
		 * BGElementController.append(uuid, x, y); hasStartedBge = true;
		 * //e.setHandled(true); } else if( e.isRightButtonPressed() &&
		 * hasStartedGroup ) {
		 * CGroupController.append(CGroupController.getCurrentUUID(), x, y);
		 * //e.setHandled(true); } else if( e.isRightButtonPressed() &&
		 * !hasStartedGroup ) { // we havent started anything, so lets do it
		 * if(getDragDistance
		 * (draggedCoords)>=CCanvasScrapModeInputHandler.CREATE_GROUP_MIN_DIST)
		 * { hasStartedGroup = true; long uuid = Calico.uuid(); // Lasso! //TODO
		 * edit for testing CGroupController.start(uuid, canvas_uid, 0L,
		 * draggedCoords.xpoints[0], draggedCoords.ypoints[0]);
		 * CGroupController.setCurrentUUID(uuid); for(int
		 * i=1;i<draggedCoords.npoints;i++) { CGroupController.append(uuid,
		 * draggedCoords.xpoints[i], draggedCoords.ypoints[i]); }
		 * //e.setHandled(true); } } else { //super.mouseDragged(e); }
		 */
		lastEvent = e;

	}

	public void actionScroll(InputEventInfo e) {
	}

	public void actionReleased(InputEventInfo e) {
		// this.hideModeIcon();
		// logger.debug(this.getClass().getName()+" actionReleased()");
		/*
		 * if(CArrowController.getOutstandingAnchorPoint()!=null) { Point start
		 * = CArrowController.getOutstandingAnchorPoint(); Point end =
		 * e.getPoint(); long startGroupUID =
		 * CalicoInputManager.getSmallestGroupAtPoint(start.x, start.y); long
		 * endGroupUID = CalicoInputManager.getSmallestGroupAtPoint(end.x,
		 * end.y); if(endGroupUID==startGroupUID && endGroupUID!=0L) { return; }
		 * 
		 * long curCanvasUID = CCanvasController.getCurrentUUID(); int startType
		 * = CArrow.TYPE_GROUP; int endType = CArrow.TYPE_GROUP;
		 * 
		 * if(startGroupUID==0L) { startGroupUID = curCanvasUID; startType =
		 * CArrow.TYPE_CANVAS; } if(endGroupUID==0L) { endGroupUID =
		 * curCanvasUID; endType = CArrow.TYPE_CANVAS; }
		 * 
		 * // ARROW! CArrowController.start(Calico.uuid(), curCanvasUID,
		 * startType, startGroupUID, start, endType, endGroupUID, end );
		 * 
		 * CArrowController.setOutstandingAnchorPoint(null);
		 * 
		 * 
		 * return; }
		 */

		CalicoInputManager.unlockHandlerIfMatch(canvas_uid);

		int x = e.getX();
		int y = e.getY();

		if (hasStartedGroup && e.getButton() == InputEventInfo.BUTTON_LEFT) {
			CGroupController.append(CGroupController.getCurrentUUID(), x, y);
			CGroupController.finish(CGroupController.getCurrentUUID(), true);

			CGroupController.setLastCreatedGroupUUID(CGroupController
					.getCurrentUUID());

			CGroupController.show_group_piemenu(
					CGroupController.getCurrentUUID(), e.getPoint(),
					PieMenuButton.SHOWON_SCRAP_CREATE);
			/*
			 * PieMenu.displayPieMenu(e.getPoint(), new
			 * GroupDeleteButton(CGroupController.getCurrentUUID()), new
			 * GroupSetPermanentButton(CGroupController.getCurrentUUID()), new
			 * ListCreateButton(canvas_uid, CGroupController.getCurrentUUID())
			 * //new
			 * GroupChangeChildrenColorButton(CGroupController.getCurrentUUID
			 * ()), //new GroupCopyButton(CGroupController.getCurrentUUID()) );
			 */
			hasStartedGroup = false;
		}
		// else if (e.getInputEvent().isRightMouseButton()
		// && !hasStartedGroup)
		// {
		// CCanvasStrokeModeInputHandler.clickMenu(0l, 0l, mouseDown, e);
		// }
		/*
		 * if(!hasBeenPressed && (e.getButton()==InputEventInfo.BUTTON_LEFT)) {
		 * long uuid = Calico.uuid(); BGElementController.setCurrentUUID(uuid);
		 * BGElementController.start(uuid, canvas_uid, 0L);
		 * BGElementController.append(uuid, x, y);
		 * BGElementController.finish(uuid); } else
		 * if((e.getButton()==InputEventInfo.BUTTON_LEFT) && hasStartedBge) {
		 * long bguid = BGElementController.getCurrentUUID();
		 * BGElementController.append(bguid, x, y);
		 * BGElementController.finish(bguid); hasStartedBge = false; } else
		 * if((e.getButton()==InputEventInfo.BUTTON_RIGHT) && hasStartedGroup )
		 * { CGroupController.append(CGroupController.getCurrentUUID(), x, y);
		 * CGroupController.finish(CGroupController.getCurrentUUID());
		 * hasStartedGroup = false;
		 * 
		 * } else if((e.getButton()==InputEventInfo.BUTTON_RIGHT) &&
		 * !hasStartedGroup ) { //arrow.create Calico.logger.debug("SHOW MENU");
		 * if(CGroupController.getCopyUUID()!=0L) {
		 * PieMenu.displayPieMenu(e.getPoint(), new ChangePenColorButton(), new
		 * ArrowSetAnchorButton(e.getPoint()), new
		 * CanvasGroupPasteButton(canvas_uid) ); } else {
		 * PieMenu.displayPieMenu(e.getPoint(), new ChangePenColorButton(), new
		 * ArrowSetAnchorButton(e.getPoint()) ); } } else {
		 * 
		 * }
		 */
		lastEvent = e;

		// super.mouseReleased(e);
	}
}
