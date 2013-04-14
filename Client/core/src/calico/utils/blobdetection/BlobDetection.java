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

import java.lang.reflect.*;

//==================================================
//class BlobDetection
//==================================================
public class BlobDetection extends EdgeDetection
{
	// Temp
	Object	parent;
	Method 	filterBlobMethod;
	// Temp
	
	public static int        	blobMaxNumber = 1000;
	public int        			blobNumber;
	public Blob[]     			blob;
	public boolean[]  			gridVisited;

	public int					blobWidthMin, blobHeightMin;
		
	//	--------------------------------------------
	// Constructor
	//	--------------------------------------------
	public BlobDetection(int imgWidth, int imgHeight)
	{
	  super(imgWidth, imgHeight);
	  
	  gridVisited   = new boolean[nbGridValue];
	  blob         	= new Blob[blobMaxNumber];
	  blobNumber   	= 0;
	  for (int i=0 ; i<blobMaxNumber;i++) 
	  		blob[i] = new Blob(this); 
	
	  blobWidthMin	= 0;
	  blobHeightMin	= 0;
	  
	  filterBlobMethod = null;
	}
	
	//--------------------------------------------
	// setBlobDimensionMin()
	//--------------------------------------------
	/*
	public void setBlobDimensionMin(int w, int h)
	{
	  if (w<0) w=0;
	  if (h<0) h=0;
	  if (w>imgWidth) w=imgWidth;
	  if (h>imgHeight) h=imgHeight;
	  
	  blobWidthMin	= w;
	  blobHeightMin	= h;
	}
	*/
	//--------------------------------------------
	// setNumberBlobMax()
	//--------------------------------------------
	public void setBlobMaxNumber(int nb)
	{
		blobMaxNumber = nb;
	}
	
	//--------------------------------------------
	// getBlob()
	//--------------------------------------------
	public Blob getBlob(int n)
	{
		Blob b=null;
		if (n<blobNumber)
			return blob[n];
		return b;
	}
	
	//--------------------------------------------
	// getBlobNb()
	//--------------------------------------------
	public int getBlobNb()
	{
		return blobNumber;
	}
	
	//--------------------------------------------
	// computeBlobs()
	//--------------------------------------------
	public void computeBlobs(int[] pixels)
	{
		// Image
		setImage(pixels);
		
	     // Clear gridVisited
	     for (int i=0 ; i<nbGridValue; i++) 
	     	gridVisited[i]=false;
		
	     // Compute Isovalue
		computeIsovalue();
		
	     // Get Lines indices
		int		x,y,squareIndex,n;
		int		iEdge;
		int		offx, offy, offAB;
		int		toCompute;
		int		offset;
		float	t;
		float	vx,vy;
	
		nbLineToDraw 	= 0;
		vx	     		= 0.0f;
	    blobNumber   	= 0;
		for (x=0 ; x<resx-1 ; x++)
		{
			vy = 0.0f;
			for (y=0 ; y<resy-1 ; y++)
			{
				 // > offset in the grid
				offset		= x + resx*y;
	
		         // > if we were already there, just go the next square!
	            if (gridVisited[offset] == true) continue;
	                     
	             // > squareIndex
				squareIndex     = getSquareIndex(x,y);
	
	             // >Found something
				 if (squareIndex > 0 && squareIndex < 15)
				 {
				 	if (blobNumber < blobMaxNumber)
				 	{
						   findBlob(blobNumber,x,y);
							 blobNumber++;
				 		
				 	}
				 }
				vy += stepy;
			}	// for y
			vx += stepx;
		}	// for x
		nbLineToDraw /= 2;
		//blobNumber+=1;
	}
	
	//--------------------------------------------
	// findBlob()
	//--------------------------------------------
	public void findBlob(int iBlob, int x, int y)
	{
		 // Reset Blob values
	
		blob[iBlob].id     = iBlob; 
		 blob[iBlob].xMin   =  1000.0f;
		 blob[iBlob].xMax   =  -1000.0f;
		 blob[iBlob].yMin   =  1000.0f;
		 blob[iBlob].yMax   =  -1000.0f;
		 blob[iBlob].nbLine = 0; 
	
		
		 // Find it !!    
		computeEdgeVertex(iBlob, x,y);
		{
		
		 // > This is just a temp patch (somtimes 'big' blobs are detected on the grid edges)
	
		 if (blob[iBlob].xMin>=1000.0f || blob[iBlob].xMax<=-1000.0f || blob[iBlob].yMin>=1000.0f || blob[iBlob].yMax<=-1000.0f)
		   blobNumber--;    
		 else  
		 {
			blob[iBlob].update();
	        // User Filter
			if (filterBlobMethod != null)
	        {
	        	try 
				{
	        		Boolean returnObj = (Boolean)(filterBlobMethod.invoke(parent, new Object[]{ blob[iBlob] } ));
	        		boolean returnValue = returnObj.booleanValue();
	        		if (returnValue == false) blobNumber--;    
				} 
	        	catch (Exception e) 
				{
					System.out.println("Disabling filterBlobMethod() because of an error.");
					filterBlobMethod = null;
				}
	        }
		  }
	
		}
	
	
	}
	
	//--------------------------------------------
	// computeEdgeVertex()
	//--------------------------------------------
	void computeEdgeVertex(int iBlob, int x, int y)
	{
		 // offset
		 int offset = x+resx*y;
		 
		 // Mark voxel as visited
		 if (gridVisited[offset] == true) return;
		 gridVisited[offset]=true;    
		
		 // 
		 int   iEdge, offx, offy, offAB;
		 int[] edgeOffsetInfo;
		 int   squareIndex     = getSquareIndex(x,y);
		 float vx = (float)x*stepx;
		 float vy = (float)y*stepy;
		 
		 int   n = 0;
		 while ( (iEdge = MetaballsTable.edgeCut[squareIndex][n++]) != -1)
		 {
			edgeOffsetInfo  = MetaballsTable.edgeOffsetInfo[iEdge];
			offx			= edgeOffsetInfo[0];
			offy			= edgeOffsetInfo[1];
			offAB			= edgeOffsetInfo[2];
		
		    if (blob[iBlob].nbLine < Blob.MAX_NBLINE) 
		    	lineToDraw[nbLineToDraw++] = blob[iBlob].line[blob[iBlob].nbLine++] = voxel[(x+offx) + resx*(y+offy)] + offAB;
		    else
		    	return;
		 }
		 
		 int   toCompute = MetaballsTable.edgeToCompute[squareIndex];
		 float t = 0.0f;
		 float value = 0.0f;
		 if (toCompute>0)
		 {
		   if ( (toCompute & 1) > 0) // Edge 0
		   {
		   		t	= (isovalue - gridValue[offset]) / (gridValue[offset+1] - gridValue[offset]); 
				value   = vx*(1.0f-t) + t*(vx+stepx); 
				edgeVrt[voxel[offset]].x = value;
		     
				if (value < blob[iBlob].xMin ) blob[iBlob].xMin = value;
				if (value > blob[iBlob].xMax ) blob[iBlob].xMax = value;
		      
		   }
		   if ( (toCompute & 2) > 0) // Edge 3
		   {
		   		t	= (isovalue - gridValue[offset]) / (gridValue[offset+resx] - gridValue[offset]); 
		   		value   = vy*(1.0f-t) + t*(vy+stepy);
		   		edgeVrt[voxel[offset]+1].y = value;
		     
		   		if (value < blob[iBlob].yMin ) blob[iBlob].yMin = value;
		   		if (value > blob[iBlob].yMax ) blob[iBlob].yMax = value;
		
		   }
					
		 } // toCompute
		
		 // Propagate to neightbors : use of Metaballs.neighborsTable 
		 byte neighborVoxel = MetaballsTable.neightborVoxel[squareIndex];    
		 if (x<resx-2   && (neighborVoxel & (1<<0))>0)        computeEdgeVertex(iBlob, x+1,y);
		 if (x>0        && (neighborVoxel & (1<<1))>0)        computeEdgeVertex(iBlob, x-1,y);
		 if (y<resy-2   && (neighborVoxel & (1<<2))>0)        computeEdgeVertex(iBlob, x,y+1);
		 if (y>0        && (neighborVoxel & (1<<3))>0)        computeEdgeVertex(iBlob, x,y-1);
	 
	}
	
	//--------------------------------------------
	// filterBlob()
	//--------------------------------------------
	/*
	public boolean acceptBlob(Blob b)
	{
		if ( (b.w*imgWidth>=blobWidthMin) || (b.h*imgHeight>=blobHeightMin) )
			return true;
		return false;
	}
	*/
	//--------------------------------------------
	// activeCustomFilter
	//--------------------------------------------
	public void activeCustomFilter(Object parent)
	{
		this.parent = parent;
		  try 
		  {
		  	filterBlobMethod = parent.getClass().getMethod("newBlobDetectedEvent", new Class[] { Blob.class });
	      	//System.out.println("newBlobDetectedEvent found!");
	      } catch (Exception e) 
		  {
	      	//System.out.println("no such metho or error");
	        // no such method, or an error.. which is fine, just ignore
	      }
	}

}
