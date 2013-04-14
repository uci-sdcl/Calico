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

import edu.umd.cs.piccolo.nodes.PImage;

public abstract class MenuBarButtonCluster
{
	private CanvasGenericMenuBar menubar = null;
	protected long canvas_uuid = 0L;
	
	public MenuBarButtonCluster(CanvasGenericMenuBar menubar, long canvas_uuid)
	{
		this.menubar = menubar;
		this.canvas_uuid = canvas_uuid;
	} 
	
	public abstract void display();
	
	
	
	protected final void addSpacer(){this.menubar.addSpacer();}
	
	protected final Rectangle addIcon(){return this.menubar.addIcon(22);}
	
	protected final Rectangle addIcon(Rectangle rect){return this.menubar.addIcon(rect.width);}
	protected final Rectangle addIcon(int width){return this.menubar.addIcon(width);}
	
	protected final void addIcon(CanvasMenuButton icon){this.menubar.addIcon(icon);}
	
	
	protected final void addText(String text, Font font){this.menubar.addText(text,font);}
	protected final void addText(String text){this.menubar.addText(text);}
}
