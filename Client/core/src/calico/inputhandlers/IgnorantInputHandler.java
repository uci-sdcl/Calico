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

import edu.umd.cs.piccolo.event.*;

/**
 * This is a default input handler. It ignores all mouse events
 * This should be extended, to allow you to only extend the methods
 * that you actually need.
 * 
 * @author Mitch Dempsey
 * @deprecated
 * @see CalicoAbstractInputHandler
 */
public class IgnorantInputHandler extends PBasicInputEventHandler
{
	public void mouseDragged(InputEventInfo e){}
	public void mouseClicked(InputEventInfo e){}
	public void mousePressed(InputEventInfo e){}
	public void mouseReleased(InputEventInfo e){}
	//public void mouseDragged(InputEventInfo e){}
	//public void mouseDragged(InputEventInfo e){}
	
	public void mousePressed(PInputEvent e)
	{
		e.setHandled(true);
		super.mousePressed(e);
	}

	public void mouseClicked(PInputEvent e)
	{
		e.setHandled(true);
		super.mouseClicked(e);
	}
	public void mouseEntered(PInputEvent e)
	{
		e.setHandled(true);
		super.mouseEntered(e);
	}
	public void mouseDragged(PInputEvent e)
	{
		e.setHandled(true);
		super.mouseDragged(e);
	}
	public void mouseExited(PInputEvent e)
	{
		e.setHandled(true);
		super.mouseExited(e);
	}

	public void mouseReleased(PInputEvent e)
	{
		e.setHandled(true);
		super.mouseReleased(e);
	}

	public void mouseMoved(PInputEvent e)
	{
		e.setHandled(true);
		super.mouseMoved(e);
	}
	public void keyTyped(PInputEvent e)
	{
		e.setHandled(true);
		super.keyTyped(e);
	}
	
	
	
	
	//////
	public void mousePressed(PInputEvent e,boolean handled)
	{
		e.setHandled(handled);
		super.mousePressed(e);
	}

	public void mouseClicked(PInputEvent e,boolean handled)
	{
		e.setHandled(handled);
		super.mouseClicked(e);
	}
	public void mouseEntered(PInputEvent e,boolean handled)
	{
		e.setHandled(handled);
		super.mouseEntered(e);
	}
	public void mouseDragged(PInputEvent e,boolean handled)
	{
		e.setHandled(handled);
		super.mouseDragged(e);
	}
	public void mouseExited(PInputEvent e,boolean handled)
	{
		e.setHandled(handled);
		super.mouseExited(e);
	}

	public void mouseReleased(PInputEvent e,boolean handled)
	{
		e.setHandled(handled);
		super.mouseReleased(e);
	}

	public void mouseMoved(PInputEvent e,boolean handled)
	{
		e.setHandled(handled);
		super.mouseMoved(e);
	}
	
	public void keyTyped(PInputEvent e,boolean handled)
	{
		e.setHandled(handled);
		super.keyTyped(e);
	}


}
