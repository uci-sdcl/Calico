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
package calico.plugins.palette.menuitems;

import java.io.File;


import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import calico.plugins.palette.*;
import calico.plugins.palette.iconsets.CalicoIconManager;
import calico.inputhandlers.InputEventInfo;

public class SavePalette extends PaletteBarMenuItem {

	public SavePalette()
	{
		super();
		this.setImage(CalicoIconManager.getIconImage("palette.save"));
	}
	
	@Override
	public void onClick(InputEventInfo ev) {
		JFileChooser fc = new JFileChooser(new File(File.separator+"palette"));
		fc.setFileFilter(new FileNameExtensionFilter("Calico palette file (*.cpal)", "cpal"));
		fc.showSaveDialog(null);
		File selFile = fc.getSelectedFile();
		if (selFile != null && selFile.toString().lastIndexOf(".cpal") < 0)
		{
			selFile = new File(selFile.getAbsolutePath() + ".cpal");
		}
			
		if (selFile != null)
			PalettePlugin.savePalette(selFile);
	}
	
	class CustomFileFilter extends javax.swing.filechooser.FileFilter {
	    public boolean accept(File file) {
	        String filename = file.getName().toLowerCase();
	        return filename.endsWith(".cpal");
	    }
	    public String getDescription() {
	        return "*.cpal (Calico palette file)";
	    }
	}

}
