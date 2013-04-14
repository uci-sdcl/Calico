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
package calico.components.menus;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.*;

import calico.*;
import calico.components.*;
import calico.components.grid.*;
import calico.components.menus.buttons.MBColorButton;
import calico.controllers.CCanvasController;
import calico.iconsets.CalicoIconManager;
import calico.inputhandlers.*;
import calico.modules.*;
import calico.networking.*;

import edu.umd.cs.piccolo.*;
import edu.umd.cs.piccolo.util.*;
import edu.umd.cs.piccolo.nodes.*;
import edu.umd.cs.piccolox.nodes.*;
import edu.umd.cs.piccolox.pswing.*;

import java.net.*;

import edu.umd.cs.piccolo.event.*;



public class CanvasTopMenuBar extends CanvasGenericMenuBar
{
	private static final long serialVersionUID = 1L;
	
	private Rectangle rect_default = new Rectangle(0,0,16,16);
	
	private long cuid = 0L;
	
	
	
	
	public CanvasTopMenuBar(long c)
	{
		super(CanvasGenericMenuBar.POSITION_TOP, CCanvasController.canvasdb.get(c).getBounds());
		cuid = c;
		
		
		// GRID COORDINATES
//		String canvasIndex = String.valueOf(CCanvasController.canvasdb.get(cuid).getIndex());
		
		
		addCap(CanvasGenericMenuBar.ALIGN_START);
		
//		addText(canvasIndex, new Font("Verdana", Font.BOLD, 12));
		
		addSpacer();
		
//		addIcon(new MBColorButton(0L, CalicoDataStore.PenColor, rect_default));

		addSpacer();
		
				
		switch(CalicoDataStore.Mode)
		{
//			case ARROW:canvasIndex="Arrow Mode";break;
//			case DELETE:canvasIndex="Eraser Mode";break;
//			case EXPERT:canvasIndex="Expert Mode";break;
//			case SCRAP:canvasIndex="Scrap Mode";break;
//			case STROKE:canvasIndex="Stroke Mode";break;
		}
		
//		addText(canvasIndex, new Font("Verdana", Font.BOLD, 12));
		
		addCap(CanvasGenericMenuBar.ALIGN_END);
		

		//this.invalidatePaint();
		CalicoDraw.invalidatePaint(this);

	}
			
}
