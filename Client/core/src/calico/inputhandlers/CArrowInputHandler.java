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

import java.awt.*;

import calico.*;
import calico.components.*;
import calico.components.arrow.CArrow;
import calico.components.piemenu.*;
import calico.components.piemenu.arrows.ArrowDeleteButton;
import calico.components.piemenu.arrows.ChangeArrowColorButton;
import calico.controllers.CArrowController;
import calico.modules.*;
import calico.networking.*;

import java.awt.Color;
import java.awt.geom.*;
import java.util.*;

import org.apache.log4j.Logger;

import edu.umd.cs.piccolo.*;
import edu.umd.cs.piccolo.event.*;
import edu.umd.cs.piccolo.nodes.*;
import edu.umd.cs.piccolo.util.*;
import edu.umd.cs.piccolox.nodes.PLine;

@Deprecated
public class CArrowInputHandler extends CalicoAbstractInputHandler
{
	public static Logger logger = Logger.getLogger(CArrowInputHandler.class.getName());
	
	
	private long uuid = 0L;

	
	public CArrowInputHandler(long u)
	{
		uuid = u;
	}
	
	private double getPercentage(InputEventInfo e)
	{
		return CArrowController.arrows.get(uuid).getPointPercentage(e.getPoint());
	}
	private int getRegion(InputEventInfo e)
	{
		return CArrow.getPercentRegion(getPercentage(e));
	}
	
	public void actionPressed(InputEventInfo e)
	{
		Calico.logger.debug("ARROW_PRESSED("+uuid+")");
		//Calico.log_debug(getPercentage(e)+"%|"+e.toString());
	}


	public void actionDragged(InputEventInfo e)
	{
		Calico.logger.debug("ARROW_DRAGGED("+uuid+")");
		//Calico.log_debug(getPercentage(e)+"%|"+e.toString());
	}


	public void actionReleased(InputEventInfo e)
	{
		int region = getRegion(e);
		if(region==CArrow.REGION_HEAD)
		{
			Calico.logger.debug("REGION: HEAD");
			
			
			
		}
		else if(region==CArrow.REGION_TAIL)
		{
			Calico.logger.debug("REGION: TAIL");
		}
		else if(region==CArrow.REGION_MIDDLE)
		{
			Calico.logger.debug("REGION: MIDDLE");
		}
		
		PieMenu.displayPieMenu(e.getGlobalPoint(), 
				new ArrowDeleteButton(uuid),
				new ChangeArrowColorButton(uuid)
		);
		
		Calico.logger.debug("ARROW_RELEASED("+uuid+")");
		//Calico.log_debug(getPercentage(e)+"%|"+e.toString());
		
	}

}
