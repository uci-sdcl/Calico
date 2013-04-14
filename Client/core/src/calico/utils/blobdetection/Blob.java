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
package calico.utils.blobdetection;

//==================================================
//class Blob
//==================================================
public class Blob
{
	public BlobDetection parent;
	
	public int   	id;
	public float 	x,y;   // position of its center
	public float 	w,h;   // width & height
	public float 	xMin, xMax, yMin, yMax;

	public int[] 	line;  
	public int 		nbLine;
	
	public static int MAX_NBLINE = 4000;    

	public Blob(BlobDetection parent)
	{
		this.parent = parent;
		line = new int[MAX_NBLINE];  // stack of index
		nbLine = 0;
	}
	
	public EdgeVertex getEdgeVertexA(int iEdge)
	{
		if (iEdge*2<parent.nbLineToDraw*2)
			return parent.getEdgeVertex(line[iEdge*2]);
		else
			return null;
	}
	
	public EdgeVertex getEdgeVertexB(int iEdge)
	{
		if ((iEdge*2+1)<parent.nbLineToDraw*2)
			return parent.getEdgeVertex(line[iEdge*2+1]);
		else
			return null;
	}
	
	
	public int getEdgeNb()
	{
		return nbLine;
	}
	
	public void update()
	{
		w = (xMax-xMin);
		h = (yMax-yMin);
		x = 0.5f*(xMax+xMin);
		y = 0.5f*(yMax+yMin);
	
		nbLine /= 2;  
	}


}
