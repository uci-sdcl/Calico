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

import calico.*;
import calico.networking.*;
import calico.networking.netstuff.*;
import calico.admin.*;
import calico.clients.*;
import calico.controllers.CArrowController;
import calico.controllers.CCanvasController;
import calico.controllers.CGroupController;
//import calico.components.CArrowBackupState.AnchorPoint;
import calico.sessions.*;
import calico.utils.CalicoUtils;
import calico.uuid.*;



import java.util.*;

import java.awt.*;

import org.json.me.*;

public class CArrow 
{
	public static final int TYPE_NORM_HEAD_A = 1;
	public static final int TYPE_NORM_HEAD_B = 2;
	public static final int TYPE_NORM_HEAD_AB = 3;

	public static final int TYPE_CANVAS = 4;
	public static final int TYPE_GROUP = 5;
	
	private long uuid = 0L;
	private long canvasuid = 0L;
	
	private int arrowType = 0;
	

	private AnchorPoint anchorA = null;
	private AnchorPoint anchorB = null;
	
	private Color color = Color.BLACK;
	
	public CArrow(long uuid, long cuid, int type, Color color, AnchorPoint pointa, AnchorPoint pointb)
	{
		this.uuid = uuid;
		this.canvasuid = cuid;
		this.arrowType = type;
		this.color = color;
		
		anchorA = pointa;
		anchorB = pointb;
	}
	
	
	/**
	 * @deprecated
	 * @see #CArrow(long, long, int, Color, AnchorPoint, AnchorPoint)
	 * @param uid
	 * @param cuid
	 * @param type
	 */
	public CArrow(long uid, long cuid, int type)
	{
		this(uid, cuid, type, Color.BLACK, null, null);
	}

	
	public int getArrowType()
	{
		return this.arrowType;
	}
	
	public Color getArrowColor(){
		return this.color;
	}
	public void setArrowType(int type)
	{
		// TODO: This needs to redraw the arrow.
		this.arrowType = type;
	}
	
	
	public void setColor(int r, int g, int b)
	{
		setColor( new Color(r,g,b));
	}
	public void setColor(Color col)
	{
		this.color = col;
	}
	
	
	public long getCanvasUUID()
	{
		return this.canvasuid;
	}
	
	
	

	public long getUUID()
	{
		return this.uuid;
	}
	
	public void moveGroup(long uid, int x, int y)
	{
		if(this.anchorA.getType()==CArrow.TYPE_GROUP && this.anchorA.getUUID()==uid)
		{
			this.anchorA.translate(x, y);	
		}
		
		if(this.anchorB.getType()==CArrow.TYPE_GROUP && this.anchorB.getUUID()==uid)
		{
			this.anchorB.translate(x, y);
		}
	}
	
	
	public void delete()
	{
		// We dont need to do anything in this
	}
	
	public AnchorPoint getAnchorA()
	{
		return this.anchorA;
	}
	public AnchorPoint getAnchorB()
	{
		return this.anchorB;
	}
	
	public AnchorPoint getAnchor(long uid)
	{
		if(anchorA.getType()==CArrow.TYPE_GROUP && anchorA.getUUID()==uid)
		{
			return anchorA;	
		}
		else if(anchorB.getType()==CArrow.TYPE_GROUP && anchorB.getUUID()==uid)
		{
			return anchorB;
		}
		return null;
	}
	
	public void setAnchorA(AnchorPoint anchor)
	{
		if (this.anchorA != null)
		{
			if (this.anchorA.getType() == CArrow.TYPE_GROUP)
			{
				CGroupController.groups.get(this.anchorA.getUUID()).deleteChildArrow(this.uuid);
			}
		}
		this.anchorA = anchor;
	}
	public void setAnchorB(AnchorPoint anchor)
	{
		if (this.anchorB != null)
		{
			if (this.anchorB.getType() == CArrow.TYPE_GROUP)
			{
				CGroupController.groups.get(this.anchorB.getUUID()).deleteChildArrow(this.uuid);
			}
		}
		this.anchorB = anchor;
	}

	public CalicoPacket[] getUpdatePackets()
	{
		//UUID CANVASUID ARROW_TYPE ANCHOR_A_TYPE ANCHOR_A_UUID ANCHOR_A_X ANCHOR_A_Y   ANCHOR_B_TYPE ANCHOR_B_UUID ANCHOR_B_X ANCHOR_B_Y
		return new CalicoPacket[]{
				CalicoPacket.getPacket(
						NetworkCommand.ARROW_CREATE,
						this.uuid,
						this.canvasuid,
						this.arrowType, 

						//this.color.getRed(),
						//this.color.getGreen(),
						//this.color.getBlue(),
						this.color.getRGB(),
						
						this.anchorA.getType(),
						this.anchorA.getUUID(),
						this.anchorA.getX(),
						this.anchorA.getY(),
						
						this.anchorB.getType(),
						this.anchorB.getUUID(),
						this.anchorB.getX(),
						this.anchorB.getY()
				)
		};
	}
	
	
	

	// TODO: Finish this
	public Properties toProperties()
	{
		Properties props = new Properties();

		props.setProperty("uuid", ""+this.uuid);
		props.setProperty("cuid", ""+this.canvasuid);
		props.setProperty("color", ""+this.color.getRGB());
		props.setProperty("type", ""+this.arrowType);

		props.setProperty("anchor.A.type", ""+this.anchorA.getType());
		props.setProperty("anchor.A.uuid", ""+this.anchorA.getUUID());
		props.setProperty("anchor.A.x", ""+this.anchorA.getX());
		props.setProperty("anchor.A.y", ""+this.anchorA.getY());
		

		props.setProperty("anchor.B.type", ""+this.anchorB.getType());
		props.setProperty("anchor.B.uuid", ""+this.anchorB.getUUID());
		props.setProperty("anchor.B.x", ""+this.anchorB.getX());
		props.setProperty("anchor.B.y", ""+this.anchorB.getY());
		
		
		
		return props;
	}


	public void calculateParent() {
		if (anchorA.getType() == CArrow.TYPE_CANVAS)
		{
			long smallestUUID = CGroupController.get_smallest_containing_group_for_point(this.canvasuid, anchorA.getPoint());
			if (smallestUUID != 0l)
			{
				this.setAnchorA(new AnchorPoint(CArrow.TYPE_GROUP, anchorA.getPoint(), smallestUUID));
				CGroupController.groups.get(smallestUUID).addChildArrow(this.uuid);
			}
		}
		if (anchorB.getType() == CArrow.TYPE_CANVAS)
		{
			long smallestUUID = CGroupController.get_smallest_containing_group_for_point(this.canvasuid, anchorB.getPoint());
			if (smallestUUID != 0l)
			{
				this.setAnchorB(new AnchorPoint(CArrow.TYPE_GROUP, anchorB.getPoint(), smallestUUID));
				CGroupController.groups.get(smallestUUID).addChildArrow(this.uuid);
			}
		}
	} 
	
	public int get_signature() {
		
		// TODO Auto-generated method stub
		return anchorA.getPoint().x + anchorA.getPoint().y + anchorB.getPoint().x + anchorB.getPoint().y;
	} 
	
	

}

