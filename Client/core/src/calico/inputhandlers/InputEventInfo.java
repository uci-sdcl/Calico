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
import edu.umd.cs.piccolo.event.*;
import java.awt.geom.*;

import calico.*;
import calico.components.CCanvas;
import calico.components.CGroup;
import calico.controllers.CCanvasController;
import calico.input.*;
import calico.perspectives.CanvasPerspective;
import jpen.*;
import java.awt.event.*;
import javax.swing.*;


/**
 * This contains all information about the specified input event.
 * @author mdempsey
 *
 */
public class InputEventInfo
{
	public static final int TYPE_MOUSE	= 1;
	public static final int TYPE_PEN	= 2;

	// Buttons
	public static final int BUTTON_LEFT		= 1024;
	public static final int BUTTON_RIGHT	= 2048;
	public static final int BUTTON_MIDDLE	= 4096;
	public static final int BUTTON_NONE		= 0;

	// Mouse Things
	public static final int MOUSE_PRESSED	= MouseEvent.MOUSE_PRESSED;
	public static final int MOUSE_DRAGGED	= MouseEvent.MOUSE_DRAGGED;
	public static final int MOUSE_RELEASED	= MouseEvent.MOUSE_RELEASED;
	public static final int MOUSE_CLICKED	= MouseEvent.MOUSE_CLICKED;


	public static final int ACTION_PRESSED	= MouseEvent.MOUSE_PRESSED;
	public static final int ACTION_DRAGGED	= MouseEvent.MOUSE_DRAGGED;
	public static final int ACTION_RELEASED	= MouseEvent.MOUSE_RELEASED;
	public static final int ACTION_CLICKED	= MouseEvent.MOUSE_CLICKED;
	public static final int ACTION_SCROLL	= MouseEvent.MOUSE_WHEEL;

	public static final int SCROLL_UP	= 1;
	public static final int SCROLL_DOWN	= 2;

	public static final int PEN_PRESSED		= 1;
	public static final int PEN_DRAGGED		= 2;
	public static final int PEN_RELEASED	= 3;
	//public static final int MOUSE_CLICKED = 4;
	
	public static final int FLAG_NOFLAGS			= 0;
	public static final int FLAG_USERFLAG1			= 1 << 0;
	public static final int FLAG_USERFLAG2			= 1 << 1;
	public static final int FLAG_USERFLAG3			= 1 << 2;
	public static final int FLAG_USERFLAG4			= 1 << 3;

	public static final int FLAG_IS_FROM_PIEMENU	= 1 << 4;
	public static final int FLAG_REROUTED			= 1 << 5;
	
	private int flags = InputEventInfo.FLAG_NOFLAGS;

//	private PInputEvent inputEvent = null;
//	private MouseEvent mouseEvent = null;
	public boolean menuShown = false;
	public long group = 0l;

	
	private int xpos = 0;
	private int ypos = 0;
	
	private int globalxpos = 0;
	private int globalypos = 0;


	private int type = TYPE_MOUSE;

	private int button = BUTTON_NONE;
	private int buttonMask = 0;
	
	private int mouse_action = MOUSE_PRESSED;
	private int pen_action = PEN_PRESSED;
	private int inputAction = ACTION_PRESSED;

	private boolean ignoreEvent = false;
	
	private int scrollAmount = 0;
	private int scrollDirection = 0;


	public InputEventInfo()
	{

	}

	public boolean isIgnored()
	{
		return ignoreEvent;
	}
	public void stop()
	{
		ignoreEvent = true;
		//inputEvent.setHandled(true);
	}

	
	public boolean isButtonPressed(int test)
	{
		return ((buttonMask & test) == test);
	}
	public boolean isLeftButtonPressed()
	{
		return ((buttonMask & BUTTON_LEFT) == BUTTON_LEFT);
	}
	public boolean isMiddleButtonPressed()
	{
		return ((buttonMask & BUTTON_MIDDLE) == BUTTON_MIDDLE);
	}
	
	/**
	 * This is used to see if the right button is PRESSED (does not work on the released)
	 * @return
	 */
	public boolean isRightButtonPressed()
	{
		return ((buttonMask & BUTTON_RIGHT) == BUTTON_RIGHT);
	}
	
	public boolean isOnlyLeftButtonPressed()
	{
		return (isLeftButtonPressed() && !isMiddleButtonPressed() && !isRightButtonPressed());
	}
	public boolean isOnlyMiddleButtonPressed()
	{
		return (!isLeftButtonPressed() && isMiddleButtonPressed() && !isRightButtonPressed());
	}
	public boolean isOnlyRightButtonPressed()
	{
		return (!isLeftButtonPressed() && !isMiddleButtonPressed() && isRightButtonPressed());
	}
	
	public void setButtonMask(int mask)
	{
		this.buttonMask = mask;
	}
	public void setButton(int button)
	{
		this.button = button;
	}
	
	public void setButtonAndMask(int button)
	{
		setButton(button);
		setButtonMask(button);
	}
	
	
	public boolean isRightButton()
	{
		return (this.button==BUTTON_RIGHT);
	}
	public boolean isLeftButton()
	{
		return (this.button==BUTTON_LEFT);
	}
	public boolean isMiddleButton()
	{
		return (this.button==BUTTON_MIDDLE);
	}
	
	public InputEventInfo(Point p, int scrollDir, int scrollType, int scrollAmount)
	{
//		mouseEvent = e;
		type = TYPE_MOUSE;
		
		xpos = p.x;
		ypos = p.y;
		
		inputAction = ACTION_SCROLL;
		
		scrollDirection = scrollDir;
		if(scrollType==MouseWheelEvent.WHEEL_UNIT_SCROLL)
		{
			scrollAmount = Math.abs(scrollDirection) * scrollAmount;
		}
		else
		{
			scrollAmount = 0;
		}
	}
	
	public int getScrollDirection()
	{
		if(scrollDirection>0)
		{
			return SCROLL_DOWN;
		}
		else
		{
			return SCROLL_UP;
		}
	}
	public int getScrollAmount()
	{
		return scrollAmount;
	}
	

	public InputEventInfo(Point p, int mouseButton, int modifiers, int mouseAction, int buttonMaskFromMouseInfo)
	{
		//Calico.log_debug(e.toString());
//		mouseEvent = e;
		type = TYPE_MOUSE;
		//inputAction = mouseAction;

		globalxpos = p.x;
		globalypos = p.y;
		if (CCanvasController.getCurrentUUID() != 0l && CanvasPerspective.getInstance().isActive())
		{
//			Point2D local = CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getLayer().globalToLocal(e.getPoint());
			double scale = CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getLayer().getScale();

			xpos = (int)(p.getX() * 1/scale);
			ypos = (int)(p.getY() * 1/scale);
		}
		else
		{
			xpos = p.x;
			ypos = p.y;
		}

		button = mouseButton;
//		setButtonFromMouseEvent(e);
		buttonMask = buttonMaskFromMouseInfo;
//		setButtonMaskFromMouseInfo(mouseInfo);
		

		if(mouseAction==MouseEvent.MOUSE_PRESSED)
		{
			inputAction = ACTION_PRESSED;
			//Calico.log_debug("MOUSE PRESSED ACTION");
		}
		else if(mouseAction==MouseEvent.MOUSE_RELEASED)
		{
			inputAction = ACTION_RELEASED;
			//Calico.log_debug("MOUSE MOUSE_RELEASED ACTION");
		}
		else if(mouseAction==MouseEvent.MOUSE_DRAGGED)
		{

			inputAction = ACTION_DRAGGED;
			int mods = modifiers;
			if( (mods & MouseEvent.BUTTON1_MASK) == MouseEvent.BUTTON1_MASK )
			{
				button = BUTTON_LEFT;
			}
			else if( (mods & MouseEvent.BUTTON2_MASK) == MouseEvent.BUTTON2_MASK )
			{
				button = BUTTON_MIDDLE;
			}
			else if( (mods & MouseEvent.BUTTON3_MASK) == MouseEvent.BUTTON3_MASK )
			{
				button = BUTTON_RIGHT;
			}
			//Calico.log_debug("MOUSE MOUSE_DRAGGED ACTION");
		}
		else
		{
			stop();
		}
		//Calico.log_debug(toString());
	}

//	private void setButtonFromMouseEvent(MouseEvent e)
//	{
//		button = 0;
//		if(e.getButton()==MouseEvent.BUTTON1)
//		{
//			button = BUTTON_LEFT;
//		}
//		else if(e.getButton()==MouseEvent.BUTTON2)
//		{
//			button = BUTTON_MIDDLE;
//		}
//		else if(e.getButton()==MouseEvent.BUTTON3)
//		{
//			button = BUTTON_RIGHT;
//		}
//	}
	
//	private void setButtonMaskFromMouseInfo(CalicoMouseListener.MouseInfo mouseInfo)
//	{
//		buttonMask = 0;
//		if(mouseInfo.leftPressed)
//		{
//			buttonMask = buttonMask | BUTTON_LEFT;
//		}
//		if(mouseInfo.rightPressed)
//		{
//			buttonMask = buttonMask | BUTTON_RIGHT;
//		}
//		if(mouseInfo.middlePressed)
//		{
//			buttonMask = buttonMask | BUTTON_MIDDLE;
//		}
//	}
	


	// Pen Things
	public InputEventInfo(PButtonEvent penb)
	{
		type = TYPE_PEN;
		if(penb.button.value)
		{
			inputAction = PEN_PRESSED;
		}
		else
		{
			inputAction = PEN_RELEASED;
		}


		if(penb.button.getType()==PButton.Type.LEFT)
		{
			button = BUTTON_LEFT;
		}
		else if(penb.button.getType()==PButton.Type.RIGHT)
		{
			button = BUTTON_RIGHT;
		}
		else if(penb.button.getType()==PButton.Type.CENTER)
		{
			button = BUTTON_MIDDLE;
		}


		setPenPosition(penb.pen);
	}
	public InputEventInfo(PLevelEvent penl)
	{
		//Calico.log_debug(penl.toString());
		type = TYPE_PEN;
		if(penl.pen.hasPressedButtons())
		{
			inputAction = PEN_DRAGGED;
			setPenButton(penl.pen);
			setPenPosition(penl.pen);
		}
		else
		{
			// Ignore?
			stop();
		}
	}
	private void setPenPosition(Pen pen)
	{
		xpos = (int) pen.getLevelValue(PLevel.Type.X);
		ypos = (int) pen.getLevelValue(PLevel.Type.Y);
	}
	private void setPenButton(Pen pen)
	{
		if(pen.hasPressedButtons())
		{
			if(pen.getButtonValue(PButton.Type.LEFT))
			{
				button = BUTTON_LEFT;
			}
			else if(pen.getButtonValue(PButton.Type.RIGHT))
			{
				button = BUTTON_RIGHT;
			}
			else if(pen.getButtonValue(PButton.Type.CENTER))
			{
				button = BUTTON_MIDDLE;
			}
		}
	}



	public int getX()
	{
		return xpos;
	}
	public int getY()
	{
		return ypos;
	}
	public Point getPoint()
	{
		return new Point(xpos,ypos);
	}
	
	public Point getGlobalPoint()
	{
		return new Point(globalxpos,globalypos);
	}
	
	public void setPoint(Point p)
	{
		xpos=(int)p.getX();
		ypos=(int)p.getY();
	}

	public int getButton()
	{
		return button;
	}

	/**
	 * @deprecated
	 * @see #getAction()
	 * @return
	 */
	public int getMouseAction()
	{
		return mouse_action;
	}
	/**
	 * @deprecated
	 * @see #getAction()
	 * @return
	 */
	public int getPenAction()
	{
		return pen_action;
	}
	
	
	public int getAction()
	{
		return inputAction;
	}

	public int getType()
	{
		return type;
	}

	public Point getDelta(InputEventInfo lastEvent)
	{
		int xdel = xpos - lastEvent.getX();
		int ydel = ypos - lastEvent.getY();

		return new Point(xdel,ydel);
	}
	
	
	public boolean isPenEvent()
	{
		return (type==TYPE_PEN);
	}
	public boolean isMouseEvent()
	{
		return (type==TYPE_MOUSE);
	}

	public String toString()
	{
		StringBuffer str = new StringBuffer();
		//Point percPoint = Calico.pointToPercent(getPoint());
		str.append("InputEventInfo(");
		str.append("button="+getButton());
		str.append(",point=("+getPoint().toString()+")");
		//str.append(",percent=("+percPoint.toString()+")");
		//str.append(",convert=("+Calico.percentToPoint(percPoint).toString()+")");

		str.append(")");

		return str.toString();
	}
	
	
	
	
	public boolean hasFlag(int checkFlag)
	{
		return ((flags & checkFlag) == checkFlag);
	}
	public void setFlag(int setFlag)
	{
		flags = flags & setFlag;
	}
	public void unsetFlag(int unsetFlag)
	{
		setFlag(~unsetFlag);
		//flags = flags & ~unsetFlag;
	}
	
	
//	public MouseEvent getMouseEvent()
//	{
//		return mouseEvent;
//	}
//	public PInputEvent getInputEvent()
//	{
//		return inputEvent;
//	}
	
	public void setAction(int action){
		this.inputAction=action;
	}

}
