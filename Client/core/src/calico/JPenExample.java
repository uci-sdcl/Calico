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
/* \begin{verbatim} */
package calico;

import javax.swing.JFrame;
import javax.swing.JLabel;
import jpen.event.PenListener;
import jpen.PButtonEvent;
import jpen.PLevel;
import jpen.PenManager;
import jpen.PenProvider;
import jpen.PKindEvent;
import jpen.PLevelEvent;
import jpen.provider.Utils;
import jpen.PScrollEvent;

public class JPenExample
	implements PenListener{

	public static void main(String... args) throws Throwable{
		new JPenExample();
	}

	JPenExample(){
		JLabel l=new JLabel("Move the pen or mouse over me!");
		PenManager pm=new PenManager(l);
		pm.pen.addListener(this);

		JFrame f=new JFrame("JPen Example");
		f.getContentPane().add(l);
		f.setSize(300, 300);
		f.setVisible(true);
	}

	//@Override
	public void penButtonEvent(PButtonEvent ev) {
		System.out.println(ev);
	}
	//@Override
	public void penKindEvent(PKindEvent ev) {
		System.out.println(ev);
	}
	//@Override
	public void penLevelEvent(PLevelEvent ev) {
		System.out.println(ev);
	}
	//@Override
	public void penScrollEvent(PScrollEvent ev) {
		System.out.println(ev);
	}
	//@Override
	public void penTock(long availableMillis) {
		System.out.println("TOCK - available period fraction: "+availableMillis);
	}
}
/* \end{verbatim} */
