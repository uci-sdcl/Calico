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
package calico.modules;

import calico.*;
import calico.components.*;

import edu.umd.cs.piccolo.nodes.*;
import edu.umd.cs.piccolo.*;

import java.awt.Font;
import java.awt.Color;

import java.util.*;
import java.util.concurrent.*;


import javax.swing.*;


/**
 * This class handles all processing of error messages
 * 
 * @author Mitch Dempsey
 */
public class ErrorMessage// extends Thread
{
	public ErrorMessage()
	{
		
	}
	
	public void run()
	{
		
	}
	
	/**
	 * This adds a message to the queue, it will displayed ASAP
	 * 
	 * @param msg the text of the message to be shown.
	 * @deprecated
	 * @see MessageObject#showError(String)
	 */
	public static void msg(String msg)
	{	
		Calico.logger.error(msg);
	}
	
	public static void popup(String msg)
	{
		Calico.logger.error(msg);
		JFrame errorFrame = new JFrame();
		JOptionPane.showMessageDialog(errorFrame,msg,"Client Message",JOptionPane.ERROR_MESSAGE);
	}
	
	public static void fatal(String msg)
	{
		Calico.logger.fatal(msg);
		popup(msg);
		System.exit(1);
	}
}
