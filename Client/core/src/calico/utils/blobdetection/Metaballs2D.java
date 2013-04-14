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
//class Metaballs2D
//==================================================
public class Metaballs2D
{
	// Isovalue
	// ------------------
	protected float isovalue;
	
	// Grid
	// ------------------
	protected int  resx,resy;
	protected float stepx,stepy;
	protected float[] gridValue;
	protected int nbGridValue;  
	
	// Voxels      
	// ------------------
	protected int[] voxel;
	protected int nbVoxel;
	
	// EdgeVertex
	// ------------------
	protected EdgeVertex[] edgeVrt;
	protected int nbEdgeVrt;  
	
	// Lines
	// what we pass to the renderer
	// ------------------
	protected int[] lineToDraw;  
	protected int nbLineToDraw;
	
	// Constructor
	// ------------------
	public Metaballs2D(){}
	
	
	// init(int, int)
	// ------------------
	public void init(int resx, int resy)
	{
		this.resx = resx;
		this.resy = resy;
	
		this.stepx = 1.0f/( (float)(resx-1));
		this.stepy = 1.0f/( (float)(resy-1));
	
		// Allocate gridValue
		nbGridValue	= resx*resy;
		gridValue	= new float[nbGridValue];
	
		// Allocate voxels
		nbVoxel		= nbGridValue;
		voxel		= new int[nbVoxel];
	
		// Allocate EdgeVertices
	     edgeVrt 	= new EdgeVertex [2*nbVoxel];
	     nbEdgeVrt 	= 2*nbVoxel;
	
		// Allocate Lines
		lineToDraw 		= new int[2*nbVoxel];
	     nbLineToDraw 	= 0;
		
	     // Precompute some values
		int x,y,n,index;
		n = 0;
		for (x=0 ; x<resx ; x++)
			for (y=0 ; y<resy ; y++)
			{
				index = 2*n;
				// index to edgeVrt
	                     voxel[x+resx*y] = index;
				// values
	                     edgeVrt[index]   = new EdgeVertex(x*stepx, y*stepy);
	                     edgeVrt[index+1] = new EdgeVertex(x*stepx, y*stepy);
	
				// Next!
				n++;
			}
		
	}
	
	// computeIsovalue()
	// ------------------
	public void computeIsovalue()
	{
	
	 // A simple test : put a metaball on center of the screen
	/*
		float	ballx = 0.5f;
		float	bally = 0.5f;
		float	vx,vy;
		float	dist;
	
		int x,y;
		vx = 0.0f;
		for (x=0 ; x<resx; x++)
		{	
			vy = 0.0f;
			for (y=0 ; y<resy; y++)
			{
				dist = (float)sqrt((vx-ballx)*(vx-ballx) + (vy-bally)*(vy-bally));
				gridValue[x+resx*y] = 10.0f/(dist*dist+0.001f);
				vy+=stepy;
			}
			vx+=stepx;
		}
	*/
	 
	         
	}
	
	
	// computeMesh()
	// ------------------
	public void computeMesh()
	{
		// Compute IsoValue
		computeIsovalue();
		// Get Lines indices
	
		int					x,y,squareIndex,n;
		int					iEdge;
		int					offx, offy, offAB;
		int					toCompute;
		int					offset;
		float					t;
		float					vx,vy;
		int[]                                   edgeOffsetInfo;
	
		nbLineToDraw = 0;
		vx	     = 0.0f;
		for (x=0 ; x<resx-1 ; x++)
		{
			vy = 0.0f;
			for (y=0 ; y<resy-1 ; y++)
			{
				offset		= x + resx*y;
				squareIndex = getSquareIndex(x,y);
	
				n	        = 0;
				while ( (iEdge = MetaballsTable.edgeCut[squareIndex][n++]) != -1)
				{
					edgeOffsetInfo          = MetaballsTable.edgeOffsetInfo[iEdge];
					offx			= edgeOffsetInfo[0];
					offy			= edgeOffsetInfo[1];
					offAB			= edgeOffsetInfo[2];
	
	                            lineToDraw[nbLineToDraw++] = voxel[(x+offx) + resx*(y+offy)] + offAB;
				}
	
	            toCompute = MetaballsTable.edgeToCompute[squareIndex];
				if (toCompute>0)
				{
	                if ( (toCompute & 1) > 0) // Edge 0
					{
						t	= (isovalue - gridValue[offset]) / (gridValue[offset+1] - gridValue[offset]); 
						edgeVrt[voxel[offset]].x = vx*(1.0f-t) + t*(vx+stepx);
	                             }
					if ( (toCompute & 2) > 0) // Edge 3
					{
						t	= (isovalue - gridValue[offset]) / (gridValue[offset+resx] - gridValue[offset]); 
						edgeVrt[voxel[offset]+1].y = vy*(1.0f-t) + t*(vy+stepy);
					}
				
				} // toCompute
				vy += stepy;
			}	// for y
	
			vx += stepx;
		}	// for x
	
		nbLineToDraw /= 2;
	 
	}
	
	// getSquareIndex(int,int)
	// ------------------
	protected int getSquareIndex(int x, int y)
	{
		int squareIndex = 0;
	    int offy  = resx*y;
	    int offy1 = resx*(y+1);
		if (gridValue[x+offy]		< isovalue) squareIndex |= 1;
		if (gridValue[x+1+offy]		< isovalue) squareIndex |= 2;
		if (gridValue[x+1+offy1]	< isovalue) squareIndex |= 4;
		if (gridValue[x+offy1]		< isovalue) squareIndex |= 8;
		return squareIndex;
	}
	
	// setIsoValue(float)
	// ------------------
	public void setIsovalue(float iso){this.isovalue = iso;}
	


}
